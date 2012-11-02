package edi;

import java.util.Iterator;

import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.PublishResult
import org.openedit.event.WebEvent;

import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.page.Page
import com.openedit.page.manage.PageManager

public class PreprocessDownloads extends EnterMediaObject {

	//check all download files, identify type, copy to invoice, ASN, etc.
	//if we found any, fire "processdownloads"

	public PublishResult processFiles() {

		PublishResult result = new PublishResult();
		result.setComplete(false);

		String catalogid = getMediaArchive().getCatalogId();
		MediaArchive archive = context.getPageValue("mediaarchive");

		String downloadFolder = "/WEB-INF/data/${catalogid}/uploads/";
		String invoiceFolder = "/WEB-INF/data/${catalogid}/incoming/invoices/";
		String asnFolder = "/WEB-INF/data/${catalogid}/incoming/asn/";

		PageManager pageManager = archive.getPageManager();
		
		List dirList = pageManager.getChildrenPaths(downloadFolder);
		log.info("Initial directory size: " + dirList.size().toString());
		
		def int iterCounter = 0;
		for (Iterator iterator = dirList.iterator(); iterator.hasNext();) {
			Page xmlFile = pageManager.getPage(iterator.next());
			log.info("Processing " + xmlFile.getName());
			if (xmlFile != null) {

				//Create the XMLSlurper Object
				try{
					
					def xmlType = new XmlSlurper().parse(xmlFile.getReader());

					//Get the INVOICENUMBER details
					def String invoiceFileType = xmlType.InvoiceGroup.InvoiceHeader.InvoiceNumber.text();
					if (!invoiceFileType.isEmpty()) {
						//THIS IS AN INVOICE - Copy to invoice folder
						log.info("Valid Invoice file detected: ${xmlFile.getName()}");
						String invoiceFile = invoiceFolder + xmlFile.getName()
						Page destination = pageManager.getPage(invoiceFile);
						pageManager.movePage(xmlFile, destination);
						iterCounter++;
						continue;
					}
					
					def String asnFileType = xmlType.ASNGroup.ASNHeader.Attributes.TblAddress.find {it.AddressType == "ST"}.AddressName1.text();
					if (!asnFileType.isEmpty()) {
						//THIS IS AN ASN - Copy to ASN folder
						log.info("Valid ASN file detected: ${xmlFile.getName()}");
						String asnFile = asnFolder + xmlFile.getName()
						Page destination = pageManager.getPage(asnFile);
						pageManager.movePage(xmlFile, destination);
						iterCounter++;
						continue;
					}

				} catch (Exception e) {
					log.info("invalid XML file detected: ${xmlFile.getName()}");
					Page target = pageManager.getPage("/WEB-INF/data/${catalogid}/incoming/invalid/${xmlFile.getName()}");
					pageManager.movePage(xmlFile, target);
					
					//Create web event to send an email.
					WebEvent event = new WebEvent();
					event.setSearchType("order");
					event.setCatalogId(catalogid);
					event.setProperty("filename", xmlFile.getName());
					archive.getMediaEventHandler().eventFired(event);
					continue;
				}
			}
		}
		if (iterCounter > 0) {
			dirList = pageManager.getChildrenPaths(downloadFolder);
			log.info("New updated directory size: " + dirList.size().toString());
			if (dirList.size() == 0) {
				result.setCompleteMessage("Upload processing is complete!");
				result.setComplete(true);
			} else {
				log.info("ERROR: Files are left in the upload folder!");
				//Create web event to send an email.
				WebEvent event = new WebEvent();
				event.setSearchType("order");
				event.setCatalogId(catalogid);
				event.setProperty("message", "ERROR: Files are left in the upload folder!");
				archive.getMediaEventHandler().eventFired(event);
				result.setErrorMessage("ERROR: Files are left in the upload folder!");
			}
		}

		return result;
	}
}

PublishResult result = new PublishResult();
result.setComplete(false);

logs = new ScriptLogger();
logs.startCapture();

try {

	PreprocessDownloads processDownloads = new PreprocessDownloads();
	processDownloads.setLog(logs);
	processDownloads.setContext(context);
	processDownloads.setPageManager(pageManager);

	result = processDownloads.processFiles();
	if (result.isComplete()) {
		//Output value to CSV file!
		context.putPageValue("export", result.getCompleteMessage());
	} else {
		//ERROR: Throw exception
		context.putPageValue("errorout", result.getErrorMessage());
	}
}
finally {
	logs.stopCapture();
}
