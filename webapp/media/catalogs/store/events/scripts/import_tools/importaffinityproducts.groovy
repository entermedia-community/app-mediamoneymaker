package import_tools

/*
 * Created on October 4th, 2012
 * Created by Peter Floyd
 */

//Import List
import java.sql.ResultSet;

import javax.swing.ToolTipManager.outsideTimerAction;

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.PublishResult
import org.openedit.entermedia.util.CSVReader
import org.openedit.store.Store

import com.openedit.BaseWebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.page.Page
import com.openedit.util.FileUtils

import edi.Util

public class ImportAffinityProducts extends EnterMediaObject {

	private static final String UPDATED = "UPDATED";
	private static final String VALID = "VALID";
	private static final String ERROR = "<strong>ERROR</strong>";
	private static final String INVALID = "<strong>INVALID</strong>";
	private static final String INVALID_SKU = "<strong>INVALID_SKU</strong>"
	private static final String NOT_FOUND = "<strong>NOT FOUND</strong>";

	public PublishResult doImport(){
		//Create Store, MediaArchive Object

		PublishResult result = new PublishResult();
		result.setComplete(false);
		result.setCompleteMessage("");
		result.setErrorMessage("");
		
		def String distributorID = "102";

		BaseWebPageRequest inReq = context;

		Store store = inReq.getPageValue("store");
		MediaArchive archive = inReq.getPageValue("mediaarchive");

		//Create Searcher Object
		Searcher productsearcher = store.getProductSearcher();
		String catalogid = getMediaArchive().getCatalogId();
		SearcherManager manager = archive.getSearcherManager();

		//Define columns from spreadsheet
		def int columnAffinitySKU = 0;
		def String columnHeadAffinitySKU = "Affinity Active Skus";
		def int columnAffinityDescription = 1;
		def String columnHeadAffinityDescription = "Description";

		MediaArchive mediaarchive = context.getPageValue("mediaarchive");

		Util output = new Util();
		String strMsg = output.createTable(columnHeadAffinitySKU, "Rogers SKU", "Status");
		String errorOut = "";

		Page upload = archive.getPageManager().getPage("/${catalogid}/temp/upload/affinity.csv");
		Reader reader = upload.getReader();
		try
		{
			def SEARCH_FIELD = "rogerssku";
			boolean done = false;

			//Create CSV reader
			CSVReader read = new CSVReader(reader, ',', '\"');
			read.readNext(); //BLANK FIRST LINE

			//Read 1 line of header
			String[] headers = read.readNext();
			boolean errorFields = false;

			def List columnNumbers = new ArrayList();
			columnNumbers.add(columnAffinitySKU);
			columnNumbers.add(columnAffinityDescription);

			def List columnNames = new ArrayList();
			columnNames.add(columnHeadAffinitySKU);
			columnNames.add(columnHeadAffinityDescription);

			for ( int index=0; index < columnNumbers.size(); index++ ) {
				String compare1 = headers[columnNumbers.get(index)].toString().toUpperCase();
				String compare2 = columnNames.get(index).toString().toUpperCase();
				if ( compare1 != compare2 ) {
					errorOut += "<li>" + addQuotes(headers[columnNumbers.get(index)].toString()) + " at column " + columnNumbers.get(index).toString() + " is invalid.</li>";
					errorFields = true;
				}
			}
			if (!errorFields == true) {

				int productCount = 0;
				int badProductCount = 0;
				def badProductList = new ArrayList();

				String[] orderLine;
				while ((orderLine = read.readNext()) != null)
				{
					String rogerssku = orderLine[columnAffinitySKU];
					Data targetProduct = productsearcher.searchByField(SEARCH_FIELD, rogerssku);
					if (targetProduct != null) {

						//productsearcher.saveData(real, context.getUser());
						log.info("ProductID Found: " + targetProduct.getId() + ":" + rogerssku);

						def String outType = "";
						def String validItem = ""
						if (targetProduct.upc != null) {
							outType = UPDATED;
							validItem = "true";
						} else {
							outType = INVALID_SKU;
							validItem = "false";
						}
						strMsg += output.appendOutMessage(orderLine[columnAffinitySKU], rogerssku, outType);
						
						//Everything is good... Update the Product
						Data product = productsearcher.createNewData();
						product.setProperty("product", targetProduct.getId());
						product.setProperty("manufacturersku", orderLine[columnAffinitySKU]);
						product.setProperty("distributor", distributorID);
						product.setProperty("validitem", validItem);
						productsearcher.saveData(product, context.getUser());

					} else {
						//ID Does not exist!!! Add to badProductIDList
						badProductCount++;
						errorOut += "<li>" + orderLine[columnAffinitySKU] + "</li>";
					}
				}
			} else {
				result.setErrorMessage("<p>The following fields in the input file are invalid:<ul>" + errorOut + "</ul></p>");
				return result;
			}
		}
		finally
		{
			FileUtils.safeClose(reader);
		}

		if (errorOut != null) {
			result.setErrorMessage(errorOut)
		} else {
			result.setErrorMessage("");
		}
		if (strMsg != null) {
			result.setCompleteMessage(strMsg)
		} else {
			result.setCompleteMessage("");
		}
		result.setCompleteMessage(strMsg);
		result.setComplete(true);
		return result;
	}

	public String addQuotes( String s ) {
		return "\"" + s + "\"";
	}

	private Data searchForDistributor( SearcherManager manager,
	MediaArchive archive, String searchForName) {

		String SEARCH_FIELD = "name";
		Searcher distributorsearcher = manager.getSearcher(archive.getCatalogId(), "distributor");
		Data targetDistributor = distributorsearcher.searchByField(SEARCH_FIELD, searchForName);

		return targetDistributor;
	}
}

logs = new ScriptLogger();
logs.startCapture();

try {

	ImportAffinityProducts importProducts = new ImportAffinityProducts();
	importProducts.setLog(logs);
	importProducts.setContext(context);
	importProducts.setModuleManager(moduleManager);
	importProducts.setPageManager(pageManager);

	//Read the production value
	boolean production = Boolean.parseBoolean(context.findValue('productionmode'));

	PublishResult result = importProducts.doImport();
	String output = "";
	if (result.isComplete()) {
		context.putPageValue("export", result.getCompleteMessage());
		if (result.getErrorMessage() != null) {
			String errorHeader = "<p><strong>The following are invalid codes</strong></p>";
			errMsg = errorHeader + result.getErrorMessage();
			context.putPageValue("errorout", errMsg);
		} else {
			context.putPageValue("errorout", "");
		}
	} else {
		context.putPageValue("export", result.getErrorMessage());
	}
}
finally {
	logs.stopCapture();
}
