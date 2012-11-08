package import_tools

/*
 * Created on October 4th, 2012
 * Created by Peter Floyd
 */

//Import List
import java.sql.ResultSet;

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

public class ImportAvocaProducts extends EnterMediaObject {

	private static final String UPDATED = "UPDATED";
	private static final String ERROR = "ERROR";
	private static final String INVALID = "INVALID";
	private static final String VALID = "VALID";
	private static final String NOT_FOUND = "NOT FOUND";

	public PublishResult doImport(){
		//Create Store, MediaArchive Object

		PublishResult result = new PublishResult();
		result.setComplete(false);
		result.setCompleteMessage("");
		result.setErrorMessage("");

		BaseWebPageRequest inReq = context;

		Store store = inReq.getPageValue("store");
		MediaArchive archive = inReq.getPageValue("mediaarchive");

		//Create Searcher Object
		Searcher productsearcher = store.getProductSearcher();
		String catalogid = getMediaArchive().getCatalogId();
		SearcherManager manager = archive.getSearcherManager();

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

		MediaArchive mediaarchive = context.getPageValue("mediaarchive");

		Util output = new Util();
		String strMsg = output.createTable(columnHeadAvocaProductName, columnHeadRogersSKU, "Status");
		String errorOut = "";

		Page upload = archive.getPageManager().getPage("/${catalogid}/temp/upload/avoca.csv");
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
					Data targetProduct = productsearcher.searchByField(SEARCH_FIELD, rogerssku);
					if (targetProduct != null) {

						strMsg += output.appendOutMessage(orderLine[columnAvocaProductName], rogerssku, UPDATED);
						//productsearcher.saveData(real, context.getUser());
						log.info("ProductID Found: " + targetProduct.getId());

						def String validItem = "false";;
						if (targetProduct.upc != null) {
							strMsg += output.appendOutMessage("", "", VALID);
							validItem = "true";
						} else {
							strMsg += output.appendOutMessage("", "", INVALID);
						}

						//Lookup Manufacturer
						def boolean validManufacturer = true;
						Searcher manufacturerSearcher = manager.getSearcher(catalogid , "manufacturer");
						Data manufacturer = manufacturerSearcher.searchByField("name", orderLine[columnManfactName]);
						if (manufacturer == null) {
							validManufacturer = false;
							strMsg += output.appendOutMessage("Manufacturer", orderLine[columnManfactName], INVALID);
						}

						//Everything is good... Update the Product
						Data product = productsearcher.createNewData();
						product.setProperty("product", targetProduct.getId());
						product.setProperty("manufacturersku", orderLine[columnAvocaProductName]);
						if (validManufacturer) {
							product.setProperty("manufacturerid", manufacturer.id);
						}
						product.setProperty("distributor", "106");
						product.setProperty("validitem", validItem);
						productsearcher.saveData(product, context.getUser());

					} else {
						//ID Does not exist!!! Add to badProductIDList
						badProductCount++;
						errorOut += "<li>" + orderLine[columnAvocaProductName] + "</li>";
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
