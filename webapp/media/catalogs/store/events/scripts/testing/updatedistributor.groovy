package testing

import org.openedit.Data
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.PublishResult
import org.openedit.entermedia.util.CSVReader
import org.openedit.store.Product
import org.openedit.store.Store
import org.openedit.store.util.MediaUtilities;

import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.page.Page
import com.openedit.util.FileUtils


class UpdateDistributor extends EnterMediaObject {

	public PublishResult doUpdate() {

		PublishResult result = new PublishResult();
		result.setComplete(false);

		MediaUtilities media = new MediaUtilities();
		media.setContext(context);

		WebPageRequest inReq = context;
		Store store = inReq.getPageValue("store");
		MediaArchive archive = inReq.getPageValue("mediaarchive");

		//Define columns from spreadsheet
		def int columnManufacturerSku = 9;
		def int columnDistributorName = 29;

		String strMsg = "";
		String filename = "/" + media.getCatalogid() + "/temp/upload/import.csv";
		Page upload = archive.getPageManager().getPage(filename);
		if (upload.exists()) {
			Reader reader = upload.getReader();
			try
			{
				//Create CSV reader
				CSVReader read = new CSVReader(reader, ',', '\"');
	
				//Read 1 line of header
				String[] headers = read.readNext();
				boolean errorFields = false;
				String errorOut = "";
	
				String[] orderLine;
				while ((orderLine = read.readNext()) != null)
				{
					def String rogersSKU = orderLine[columnManufacturerSku].trim();
					def String distributorName = orderLine[columnDistributorName].trim();
	
					//Create as400list Searcher
					Data product = media.searchForProductbyRogersSKU(rogersSKU);
					if (product != null) {
						Data distributor = media.searchForDistributor(distributorName);
						if (distributor != null) {
							Product target = media.getProductSearcher().searchById(product.getId());
							if (target != null) {
								target.setProperty("distributor", distributor.getId());
								String inMsg = product.getName() + " : " + distributor.getName();
								log.info(inMsg);
								strMsg += wrapTR(wrapTD(inMsg));
							} else {
								String inMsg = "ERROR: Product not found (" + product.getId() + ")";
								log.info(inMsg);
								strMsg += wrapTR(wrapTD(inMsg));
							}
						} else {
							String inMsg = "ERROR: Distributor not found (" + distributorName + ")";
							log.info(inMsg);
							strMsg += wrapTR(wrapTD(inMsg));
						}
					} else {
						String inMsg = "ERROR: Product not found (" + rogersSKU + ")";
						log.info(inMsg);
						strMsg += wrapTR(wrapTD(inMsg));
					}
				}
			}
			finally
			{
				FileUtils.safeClose(reader);
			}
		} else {
			strMsg = wrapTR(wrapTD("File not found!"));
		}
		result.setCompleteMessage(strMsg);
		result.setComplete(true);
		return result;
	}
	private String wrapTD(String inString) {
		return "<td>" + inString + "</td>\n";
	}
	private String wrapTR(String inString) {
		return "<tr>" + inString + "</tr>\n";
	}
}

logs = new ScriptLogger();
logs.startCapture();

try {

	PublishResult result = new PublishResult();
	result.setComplete(false);

	UpdateDistributor update = new UpdateDistributor();
	update.setLog(logs);
	update.setContext(context);
	update.setModuleManager(moduleManager);
	update.setPageManager(pageManager);

	result = update.doUpdate();
	context.putPageValue("export", result.getCompleteMessage());
}
finally {
	logs.stopCapture();
}
