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

public class ImportCesiumProducts extends EnterMediaObject {

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

		def String distributorID = "105";

		BaseWebPageRequest inReq = context;

		Store store = inReq.getPageValue("store");

		//Define columns from spreadsheet
		def int columnItemNumber = 0;
		def String columnHeadCesiumItemNum = "No";
		def int columnCesiumSKU = 1;
		def String columnHeadCesiumSKU = "SKU";
		def int columnDescription = 2;
		def String columnHeadDescription = "Description";

		String pageName = "/" + media.getCatalogid() + "/temp/upload/cesium.csv";
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
			columnNumbers.add(columnItemNumber);
			columnNumbers.add(columnCesiumSKU);
			columnNumbers.add(columnDescription);

			def List columnNames = new ArrayList();
			columnNames.add(columnHeadCesiumItemNum);
			columnNames.add(columnHeadCesiumSKU);
			columnNames.add(columnHeadDescription);

			for ( int index=0; index < columnNumbers.size(); index++ ) {
				String compare1 = headers[columnNumbers.get(index)].toString().toUpperCase();
				String compare2 = columnNames.get(index).toString().toUpperCase();
				if ( compare1 != compare2 ) {
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
					String rogerssku = orderLine[columnCesiumSKU];

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
						targetProduct.setProperty("rogerssku",orderLine[columnCesiumSKU]);
						targetProduct.setProperty("manufacturersku",orderLine[columnCesiumSKU]);
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

						def String outType = "";
						if (targetProduct.upc != null) {
							targetProduct.setProperty("validitem", "true");
						} else {
							targetProduct.setProperty("validitem", "false");
						}
						
						//Everything is good... Update the Product
						targetProduct.setProperty("manufacturersku", orderLine[columnCesiumSKU]);
						targetProduct.setProperty("distributor", distributorID);
						media.getProductSearcher().saveData(targetProduct, context.getUser());
					}
				}
			} else {
				result.setErrorMessage("There seems to be a problem with the input file.");
				return result;
			}
		}
		finally
		{
			FileUtils.safeClose(reader);
		}

		result.setCompleteMessage("The script ran successfully.");
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

	ImportCesiumProducts importProducts = new ImportCesiumProducts();
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
			context.putPageValue("errorout", result.getErrorMessage());
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
