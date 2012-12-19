package import_tools

/*
 * Created on October 4th, 2012
 * Created by Peter Floyd
 */

//Import List
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.PublishResult
import org.openedit.entermedia.util.CSVReader
import org.openedit.store.Store
import org.openedit.store.util.MediaUtilities;

import com.openedit.BaseWebPageRequest
import com.openedit.OpenEditException;
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.page.Page
import com.openedit.util.FileUtils

import edi.OutputUtilities

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

		MediaUtilities media = new MediaUtilities();
		media.setContext(context);

		def String distributorID = "102";
		boolean errorFields = false;
		
		BaseWebPageRequest inReq = context;

		Store store = inReq.getPageValue("store");

		//Define columns from spreadsheet
		def int columnAffinitySKU = 0;
		def String columnHeadAffinitySKU = "Affinity Active Skus";
		def int columnAffinityDescription = 1;
		def String columnHeadAffinityDescription = "Description";

		OutputUtilities output = new OutputUtilities();
		String strMsg = output.createTable(columnHeadAffinitySKU, "Rogers SKU", "Status");
		String errorOut = "";

		String pageName = "/" + media.getCatalogid() + "/temp/upload/affinity.csv";
		Page upload = media.getArchive().getPageManager().getPage(pageName);
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
					Data targetProduct = media.getProductSearcher().searchByField(SEARCH_FIELD, rogerssku);
					if (targetProduct != null) {
						targetProduct = store.getProduct(targetProduct.getId());
					}
					if (targetProduct == null) {

						//Product does not exist - Create blank data
						targetProduct = media.getProductSearcher().createNewData();
						targetProduct.setProperty("name",orderLine[columnAffinityDescription]);
						targetProduct.setProperty("accessoryname",orderLine[columnAffinityDescription]);
						targetProduct.setProperty("rogerssku",orderLine[columnAffinitySKU]);
						targetProduct.setProperty("manufacturersku",orderLine[columnAffinitySKU]);
						targetProduct.setProperty("validitem", "false");
						media.getProductSearcher().saveData(targetProduct, media.getContext().getUser());

						targetProduct = media.getProductSearcher().searchByField(SEARCH_FIELD, rogerssku);
						if (targetProduct != null) {
							targetProduct = store.getProduct(targetProduct.getId());
						} else {
							throw new OpenEditException("Invalid Product: " + rogerssku);
						}
					} else {
						//productsearcher.saveData(real, context.getUser());
						log.info("ProductID Found: " + targetProduct.getId() + ":" + rogerssku);

						if (targetProduct.get("upc") == null) {
							strMsg += output.appendOutMessage(orderLine[columnAffinitySKU], rogerssku, INVALID_SKU);
							targetProduct.setProperty("validitem", "false");
							log.info(" - Invalid SKU");
						} else {
							targetProduct.setProperty("validitem", "true");
						}

						//Everything is good... Update the Product
						targetProduct.setProperty("manufacturersku", orderLine[columnAffinitySKU]);
						targetProduct.setProperty("distributor", distributorID);
						media.getProductSearcher().saveData(targetProduct, context.getUser());
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
		
		if (errorFields) {
			result.setErrorMessage(errorOut);
			result.setComplete(false);
		} else {
			result.setCompleteMessage(strMsg);
			result.setComplete(true);
		}
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
	} else {
		String errorHeader = "<p><strong>The following are invalid codes</strong></p>";
		errMsg = errorHeader + result.getErrorMessage();
		context.putPageValue("errorout", errMsg);
	}
}
finally {
	logs.stopCapture();
}
