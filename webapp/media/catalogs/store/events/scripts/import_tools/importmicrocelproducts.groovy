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

import com.openedit.BaseWebPageRequest
import com.openedit.OpenEditException
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.page.Page
import com.openedit.util.FileUtils

import edi.MediaUtilities
import edi.OutputUtilities

public class ImportMicrocelProducts extends EnterMediaObject {

	private static final String UPDATED = "UPDATED";
	private static final String ERROR = "ERROR";
	private static final String INVALID = "INVALID";
	private static final String VALID = "VALID";
	private static final String NOT_FOUND = "NOT FOUND";
	private static final String ADDED = "ADDED";

	public PublishResult doImport(){
		//Create Store, MediaArchive Object

		PublishResult result = new PublishResult();
		result.setComplete(false);
		result.setCompleteMessage("");
		result.setErrorMessage("");

		MediaUtilities media = new MediaUtilities();
		media.setContext(context);
		media.setSearchers();

		BaseWebPageRequest inReq = context;

		Store store = inReq.getPageValue("store");

		//Create Searcher Object
		//Define columns from spreadsheet
		/*
		 * AccessoryID	AccessoryName	UPC	ManufacturerID	RogersSKU	ManufacturerSKU	ParticipantID
		 */
		def int columnMicrocelAccessoryID = 0;
		def String columnHeadMicrocelAccessoryID = "AccessoryID";
		def int columnMicrocelAccessoryName = 1;
		def String columnHeadMicrocelAccessoryName = "AccessoryName";
		def int columnMicrocelAccessoryNameFR = 2;
		def String columnHeadMicrocelAccessoryNameFR = "AccessoryNameFR";
		def int columnMicrocelUPC = 3;
		def String columnHeadMicrocelUPC = "UPC";
		def int columnMicrocelManufacturerID = 4;
		def String columnHeadMicrocelManufacturerID = "ManufacturerID";
		def int columnMicrocelRogersSKU = 5;
		def String columnHeadMicrocelRogersSKU = "RogersSKU";
		def int columnMicrocelManufacturerSKU = 6;
		def String columnHeadMicrocelManufacturerSKU = "ManufacturerSKU";

		MediaArchive mediaarchive = context.getPageValue("mediaarchive");

		OutputUtilities output = new OutputUtilities();
		String strMsg = output.createTable(columnHeadMicrocelManufacturerID, columnHeadMicrocelRogersSKU, "Status");
		String errorOut = "";

		String pageName = "/" + media.getCatalogid() + "/temp/upload/microcel.csv";
		Page upload = media.getArchive().getPageManager().getPage(pageName);
		Reader reader = upload.getReader();
		try
		{
			boolean done = false;

			//Create CSV reader
			CSVReader read = new CSVReader(reader, ',', '\"');
			read.readNext(); //BLANK FIRST LINE

			//Read 1 line of header
			String[] headers = read.readNext();
			boolean errorFields = false;

			def List columnNumbers = new ArrayList();
			columnNumbers.add(columnMicrocelAccessoryID);
			columnNumbers.add(columnMicrocelAccessoryName);
			columnNumbers.add(columnMicrocelUPC);
			columnNumbers.add(columnMicrocelManufacturerID);
			columnNumbers.add(columnMicrocelRogersSKU);
			columnNumbers.add(columnMicrocelManufacturerSKU);

			def List columnNames = new ArrayList();
			columnNames.add(columnHeadMicrocelAccessoryID);
			columnNames.add(columnHeadMicrocelAccessoryName);
			columnNames.add(columnHeadMicrocelUPC);
			columnNames.add(columnHeadMicrocelManufacturerID);
			columnNames.add(columnHeadMicrocelRogersSKU);
			columnNames.add(columnHeadMicrocelManufacturerSKU);

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
					def SEARCH_FIELD = "upc";
					String searchUPC = orderLine[columnMicrocelUPC];

					//Search the product for the oracle sku(rogerssku)
					Data targetProduct = media.getProductSearcher().searchByField(SEARCH_FIELD, searchUPC);
					if (targetProduct != null) {
						targetProduct = store.getProduct(targetProduct.getId());
					}
					if (targetProduct == null) {
						SEARCH_FIELD = "rogerssku";
						String rogerssku = orderLine[columnMicrocelRogersSKU];
						
						//Search the product for the UPC)
						targetProduct = media.getProductSearcher().searchByField(SEARCH_FIELD, rogerssku);
						if (targetProduct != null) {
							targetProduct = store.getProduct(targetProduct.getId());
						}
						if (targetProduct == null) {
							//Product does not exist - Create blank data
							targetProduct = media.getProductSearcher().createNewData();
							targetProduct.setProperty("name",orderLine[columnMicrocelAccessoryName]);
							targetProduct.setProperty("accessoryid", orderLine[columnMicrocelAccessoryID]);
							targetProduct.setProperty("accessoryname",orderLine[columnMicrocelAccessoryName]);
							targetProduct.setProperty("accessorynamefr",orderLine[columnMicrocelAccessoryNameFR]);
							targetProduct.setProperty("upc",orderLine[columnMicrocelUPC]);
							targetProduct.setProperty("manufacturerid",media.getManufacturerID(orderLine[columnMicrocelManufacturerID]));
							targetProduct.setProperty("rogerssku",orderLine[columnMicrocelRogersSKU]);
							targetProduct.setProperty("manufacturersku",orderLine[columnMicrocelManufacturerSKU]);
							targetProduct.setProperty("validitem", "false");
							media.getProductSearcher().saveData(targetProduct, media.getContext().getUser());

							strMsg += output.appendOutMessage(orderLine[columnMicrocelManufacturerSKU], rogerssku, INVALID);
							targetProduct = media.getProductSearcher().searchByField(SEARCH_FIELD, rogerssku);
							if (targetProduct != null) {
								targetProduct = store.getProduct(targetProduct.getId());
							} else {
								throw new OpenEditException("Invalid Product: " + rogerssku);
							}
							continue;
						}
					}
					log.info("ProductID Found: " + targetProduct.getId());
					targetProduct.setProperty("manufacturersku", orderLine[columnMicrocelManufacturerSKU]);
					targetProduct.setProperty("rogerssku",orderLine[columnMicrocelRogersSKU]);
					targetProduct.setProperty("upc", orderLine[columnMicrocelUPC]);
					targetProduct.setProperty("validitem", "true");
					
					//Lookup Manufacturer
					Data manufacturer = media.getManufacturerSearcher().searchByField("name", orderLine[columnMicrocelManufacturerID]);
					if (manufacturer == null) {
						//Add new Manufacturer
						manufacturer = media.addManufacturer(orderLine[columnMicrocelManufacturerID]);
						if (manufacturer == null) {
							strMsg += output.appendOutMessage("Manufacturer", orderLine[columnMicrocelManufacturerID], INVALID);
						} else {
							strMsg += output.appendOutMessage("Manufacturer", orderLine[columnMicrocelManufacturerID], ADDED);
							targetProduct.setProperty("manufacturerid", manufacturer.id);
						}
					} else {
						targetProduct.setProperty("manufacturerid", manufacturer.id);
					}

					//Everything is good... Update the Product Distributor
					targetProduct.setProperty("distributor", "104");
					media.getProductSearcher().saveData(targetProduct, media.getContext().getUser());
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

	ImportMicrocelProducts importProducts = new ImportMicrocelProducts();
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
