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

	private enum XMLFileType {
		ASN("ASN"),
		INVOICE("Invoice"),
		INVENTORY("InventoryInquiryAdvice");

		private String type;
		private XMLFileType(String t) {
			type = t;
		}
		@Override public String toString() {
			// TODO Auto-generated method stub
			return type.toString();
		}
	}

	public PublishResult processFiles() {

		PublishResult result = new PublishResult();
		result.setComplete(false);

		String catalogid = getMediaArchive().getCatalogId();
		MediaArchive archive = context.getPageValue("mediaarchive");

		String downloadFolder = "/WEB-INF/data/${catalogid}/uploads/";
		String invoiceFolder = "/WEB-INF/data/${catalogid}/incoming/invoices/";
		String asnFolder = "/WEB-INF/data/${catalogid}/incoming/asn/";
		String inventoryFolder = "/WEB-INF/data/${catalogid}/incoming/inventory/";

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
					String xmlFileContent = xmlFile.getContent();
					if (!isValidXMLFile(xmlFileContent)) {
						throw new Exception("invalid XML file detected: ${xmlFile.getName()}");
					}

					def xmlType = new XmlSlurper().parse(xmlFile.getReader());
					def Enum fileType = getXmlFileType(xmlFileContent);

					switch (fileType) {
						case XMLFileType.INVOICE:
						
							def INVOICEHEADER = xmlType.InvoiceGroup.depthFirst().grep{
								it.name() == 'InvoiceHeader';
							}
							log.info("INVOICE Headers found: " + INVOICEHEADER.size().toString());
							def boolean found = false;
							
							INVOICEHEADER.each {
								if (!found) {
									//Get the INVOICENUMBER details
									def String invoiceFileType = it.Attributes.TblReferenceNbr.find {it.Qualifier == "PO"}.ReferenceNbr.text();
									if (!invoiceFileType.isEmpty()) {
										//THIS IS AN INVOICE - Copy to invoice folder
										log.info("Valid Invoice file detected: ${xmlFile.getName()}");
										String invoiceFile = invoiceFolder + xmlFile.getName()
										Page destination = pageManager.getPage(invoiceFile);
										pageManager.movePage(xmlFile, destination);
										iterCounter++;
										found = true;
									}
								}
							}
							if (found) {
								break;
							} else {
								throw new Exception("Invalid Invoice XML File");
							}
							
						case XMLFileType.ASN:
						
							def ASNHEADER = xmlType.ASNGroup.depthFirst().grep{
								it.name() == 'ASNHeader';
							}
							log.info("ASN Headers found: " + ASNHEADER.size().toString());
							def boolean found = false;
		
							ASNHEADER.each {
								if (!found) {
									def String asnFileType = it.Attributes.TblAddress.find {it.AddressType == "ST"}.AddressName1.text();
									if (!asnFileType.isEmpty()) {
										//THIS IS AN ASN - Copy to ASN folder
										log.info("Valid ASN file detected: ${xmlFile.getName()}");
										String asnFile = asnFolder + xmlFile.getName()
										Page destination = pageManager.getPage(asnFile);
										pageManager.movePage(xmlFile, destination);
										iterCounter++;
										found = true;
									}
								}
							}
							if (found) {
								break;
							} else {
								throw new Exception("Invalid ASN XML File");
							}
							
						case XMLFileType.INVENTORY:
						
							def INVENTORYHEADERS = xmlType.INQGroup.depthFirst().grep{
								it.name() == 'INQHeader';
							}
							log.info("INVENTORY Headers found: " + INVENTORYHEADERS.size().toString());
							def boolean found = false;
		
							INVENTORYHEADERS.each {
								if (!found) {
									def String invFileType = it.ReportTypeCode.text();
									if (!invFileType.isEmpty()) {
										//THIS IS AN ASN - Copy to ASN folder
										log.info("Valid Inventory file detected: ${xmlFile.getName()}");
										String asnFile = inventoryFolder + xmlFile.getName()
										Page destination = pageManager.getPage(asnFile);
										pageManager.movePage(xmlFile, destination);
										iterCounter++;
										found = true;
									}
								}
							}
							if (found) {
								break;
							} else {
								throw new Exception("Invalid Inventory XML File");
							}
						default:
							throw new Exception("Invalid XML File");
					}

				} catch (Exception e) {
					log.info(e.getMessage());
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
				log.info("Upload processing is complete!");
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

	private boolean isValidXMLFile(String checkString) {
		boolean returnValue = false;
		for (XMLFileType t : XMLFileType.values()) {
			if (checkString.contains("<" + t.toString() + ">"))  {
				return true;
			}
		}
		return returnValue;
	}
	private Enum getXmlFileType(String checkString) {
		for (XMLFileType t : XMLFileType.values()) {
			if (checkString.contains("<" + t.toString() + ">"))  {
				return t;
			}
		}
		return null;
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
	MediaArchive archive = context.getPageValue("mediaarchive");
	archive.fireSharedMediaEvent("processinvoices");
	archive.fireSharedMediaEvent("processasns");
	
}
finally {
	logs.stopCapture();
}
