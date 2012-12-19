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
import com.openedit.OpenEditException
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.page.Page
import com.openedit.util.FileUtils

import edi.OutputUtilities

public class ImportAvocaProducts extends EnterMediaObject {

	private static final String UPDATED = "UPDATED";
	private static final String ERROR = "ERROR";
	private static final String INVALID = "INVALID";
	private static final String VALID = "VALID";
	private static final String NOT_FOUND = "NOT FOUND";
	private static final String ADDED = "ADDED";
	private static final String INVALID_SKU = "<strong>INVALID_SKU</strong>"

	public PublishResult doImport(){
		//Create Store, MediaArchive Object

		PublishResult result = new PublishResult();
		result.setComplete(false);
		result.setCompleteMessage("");
		result.setErrorMessage("");

		MediaUtilities media = new MediaUtilities();
		media.setContext(context);

		BaseWebPageRequest inReq = context;

		Store store = inReq.getPageValue("store");

		//Define columns from spreadsheet
		def int columnAvocaProductName = 0;
		def String columnHeadAvocaProductName = "Avoca Product Name";
		def int columnRogersSKU = 1;
		def String columnHeadRogersSKU = "Rogers Product Code";
		def int columnDescription = 2;
		def String columnHeadDescription = "Description";
		def int columnCategory = 3;
		def String columnHeadCategory = "Category"
		def int columnManfactName = 4;
		def String columnHeadManfactName = "Brand";

		OutputUtilities output = new OutputUtilities();
		String strMsg = output.createTable(columnHeadAvocaProductName, columnHeadRogersSKU, "Status");
		String errorOut = "";

		String pageName = "/" + media.getCatalogid() + "/temp/upload/avoca.csv";
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
			boolean errorFields = false;

			def List columnNumbers = new ArrayList();
			columnNumbers.add(columnAvocaProductName);
			columnNumbers.add(columnRogersSKU);
			columnNumbers.add(columnDescription);
			columnNumbers.add(columnCategory);
			columnNumbers.add(columnManfactName);

			def List columnNames = new ArrayList();
			columnNames.add(columnHeadAvocaProductName);
			columnNames.add(columnHeadRogersSKU);
			columnNames.add(columnHeadDescription);
			columnNames.add(columnHeadCategory);
			columnNames.add(columnHeadManfactName);

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
					//Create as400list Searcher
					//Read the oraclesku from the as400 table
					String rogerssku = orderLine[columnRogersSKU];

					//Search the product for the oracle sku(rogerssku)
					Data targetProduct = media.getProductSearcher().searchByField(SEARCH_FIELD, rogerssku);
					if (targetProduct != null) {
						targetProduct = store.getProduct(targetProduct.getId());
					}
					if(targetProduct == null){

						//Product does not exist - Create blank data
						targetProduct = media.getProductSearcher().createNewData();
						targetProduct.setProperty("name",orderLine[columnDescription]);
						targetProduct.setProperty("accessoryname",orderLine[columnDescription]);
						targetProduct.setProperty("manufacturerid",media.getManufacturerID(orderLine[columnManfactName]));
						targetProduct.setProperty("rogerssku",orderLine[columnRogersSKU]);
						targetProduct.setProperty("manufacturersku",orderLine[columnAvocaProductName]);
						targetProduct.setProperty("validitem", "false");
						media.getProductSearcher().saveData(targetProduct, media.getContext().getUser());

						strMsg += output.appendOutMessage(orderLine[columnAvocaProductName], rogerssku, INVALID);

						targetProduct = media.getProductSearcher().searchByField(SEARCH_FIELD, rogerssku);
						if (targetProduct != null) {
							targetProduct = store.getProduct(targetProduct.getId());
						} else {
							throw new OpenEditException("Invalid Product: " + rogerssku);
						}
					} else {

						//productsearcher.saveData(real, context.getUser());
						log.info("ProductID Found: " + targetProduct.getId() + ":" + rogerssku);

						def String outType = "";
						if (targetProduct.upc != null) {
							targetProduct.setProperty("validitem", "true");
						} else {
							targetProduct.setProperty("validitem", "false");
							strMsg += output.appendOutMessage(orderLine[columnAvocaProductName], rogerssku, INVALID_SKU);
						}

						//Lookup Manufacturer
						def boolean validManufacturer = true;
						Data manufacturer = media.getManufacturerSearcher().searchByField("name", orderLine[columnManfactName]);
						if (manufacturer == null) {
							//Add new Manufacturer
							manufacturer = media.addManufacturer(orderLine[columnManfactName]);
							if (manufacturer == null) {
								strMsg += output.appendOutMessage("Manufacturer", orderLine[columnManfactName], INVALID);
							} else {
								targetProduct.setProperty("manufacturerid", manufacturer.id);
							}
						} else {
							targetProduct.setProperty("manufacturerid", manufacturer.id);
						}

						//Everything is good... Update the Product
						targetProduct.setProperty("distributor", "106");
						media.getProductSearcher().saveData(targetProduct, media.getContext().getUser());

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

	ImportAvocaProducts importProducts = new ImportAvocaProducts();
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
