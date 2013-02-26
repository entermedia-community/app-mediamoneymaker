package rogers

/*
 * Created on October 4th, 2012
 * Created by Peter Floyd
 */

//Import List
import java.util.List;

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.PublishResult
import org.openedit.entermedia.util.CSVReader
import org.openedit.store.CartItem
import org.openedit.store.Product
import org.openedit.store.Store
import org.openedit.store.util.MediaUtilities;
import org.openedit.util.DateStorageUtil

import com.openedit.BaseWebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.page.Page
import com.openedit.util.FileUtils

public class ImportRogersOrder extends EnterMediaObject {

	List<String> goodOrderList;
	int totalRows;
	

	public List<String> getGoodOrderList() {
		if(goodOrderList == null) {
			goodOrderList = new ArrayList<String>();
		}
		return goodOrderList;
	}
	public void addToGoodOrderList(String inItem) {
		if(goodOrderList == null) {
			goodOrderList = new ArrayList<String>();
		}
		goodOrderList.add(inItem);
	}
	public int getTotalRows() {
		if (totalRows == null) {
			totalRows = 0;
		}
		return totalRows;
	}
	public void increaseTotalRows() {
		if (totalRows == null) {
			totalRows = 0;
		}
		this.totalRows++;
	}

	public void orderImport(){
		//Create Store, MediaArchive Object
		
		MediaUtilities media = new MediaUtilities();
		media.setContext(context);
		
		MediaArchive archive = media.getArchive();

		//Create Searcher Object
		Searcher productsearcher = media.getProductSearcher();
		SearcherManager manager = media.getSearcherManager();

		//	Product product = productsearcher.createNewData();
		//	String [] fields = context.getRequestParameters("field");
		//	productsearcher.updateData(context, fields, product)
		//	productsearcher.saveData(product, context.getUser());


		//Define columns from spreadsheet
		def int columnStore = 4;
		def String colHeadSTORE = "ISTORE";
		def int columnStoreRank = 5;
		def String colHeadRANK = "RANK"
		def int columnStoreName = 6;
		def String colHeadSTORENAME = "STORE"
		def int columnManfactID = 7;
		def String colHeadMANUFACTID = "IMFGR"
		def int columnManfactName = 8;
		def String colHeadMANUFACTNAME = "MFC"
		def int columnAS400id = 9;
		def String colHeadAS400ID = "INUMBR";
		def int columnAvailable = 19;
		def String colHeadAVAILABLE = "AVAILABLE";
		def int columnSuggest = 20;
		def String colHeadSUGGEST = "Sugg";
		def int columnQuantity = 21;
		def String colHeadQUANTITY = "To Order";
		def int columnRogersID = 33;
		def String colHeadROGERSID = "IVNDP#";
		def int columnRogersOtherID = 34;
		def String colHeadROGERSOTHERID = "IRLSDT";

		String catalogid = media.getCatalogid();
		Searcher ordersearcher = media.getOrderSearcher();

		//PropertyDetail detail = itemsearcher.getDetail("quantity");
		//detail.get("column");

		Page upload = archive.getPageManager().getPage("/${catalogid}/temp/upload/rogers_order.csv");
		Reader reader = upload.getReader();
		try
		{
			def SEARCH_FIELD = "rogerssku";
			boolean done = false;

			//Create CSV reader
			CSVReader read = new CSVReader(reader, ',', '\"');

			//Read 1 line of header
			String[] headers = read.readNext();
			boolean errorFields = false;
			String errorOut = "";

			def List columnNumbers = new ArrayList();
			columnNumbers.add(columnStore);
			columnNumbers.add(columnStoreRank);
			columnNumbers.add(columnStoreName);
			columnNumbers.add(columnManfactID);
			columnNumbers.add(columnManfactName);
			columnNumbers.add(columnAS400id);
			columnNumbers.add(columnAvailable);
			columnNumbers.add(columnSuggest);
			columnNumbers.add(columnQuantity);
			columnNumbers.add(columnRogersID);
			columnNumbers.add(columnRogersOtherID);

			def List columnNames = new ArrayList();
			columnNames.add(colHeadSTORE);
			columnNames.add(colHeadRANK);
			columnNames.add(colHeadSTORENAME);
			columnNames.add(colHeadMANUFACTID);
			columnNames.add(colHeadMANUFACTNAME);
			columnNames.add(colHeadAS400ID);
			columnNames.add(colHeadAVAILABLE);
			columnNames.add(colHeadSUGGEST);
			columnNames.add(colHeadQUANTITY);
			columnNames.add(colHeadROGERSID);
			columnNames.add(colHeadROGERSOTHERID);

			for ( int index=0; index < columnNumbers.size(); index++ ) {
				if ( headers[columnNumbers.get(index)].toString().toUpperCase() != columnNames.get(index).toString().toUpperCase()) {
					errorOut += "<li>" + addQuotes(headers[columnNumbers.get(index)].toString()) + " at column " + columnNumbers.get(index).toString() + " is invalid.</li>";
					errorFields = true;
				}
			}
			if (errorFields == true) {

				errorOut = "<p>The following fields in the input file are invalid:<ul>" + errorOut + "</ul></p>";
				context.putPageValue("errorout", errorOut);

			} else {
			
			

				int productCount = 0;
				int badProductCount = 0;
				List badProductList = new ArrayList();
				List badStoreList = new ArrayList();

				String[] orderLine;
				while ((orderLine = read.readNext()) != null)
				{
					def storeNum = orderLine[columnStore].trim();

					//Create as400list Searcher
					Searcher storeList = manager.getSearcher(archive.getCatalogId(), "rogersstore");
					Data targetStore = storeList.searchById(storeNum);
					if(targetStore == null){
//						targetStore = storeList.createNewData();
//						targetStore.setId(storeList.nextId());
//						targetStore.setProperty("store", storeNum);
//						//TODO: pull out address details etc from spreadsheet
//						targetStore.setProperty("name", orderLine[columnStoreName]);
//						targetStore.setProperty("level", orderLine[columnStoreRank].replace(" ", ""));
//						targetStore.setSourcePath("rogers");
//						storeList.saveData(targetStore, context.getUser());
						badStoreList.add(storeNum);
					}

					//Get Product Info
					//Read the oraclesku from the as400 table
					String rogerssku = orderLine[columnRogersID];

					//Search the product for the oracle sku(rogerssku)
					Data targetProduct = productsearcher.searchByField(SEARCH_FIELD, rogerssku);
					if (targetProduct != null) {

						//productsearcher.saveData(real, context.getUser());
						log.info("ProductID Found: " + targetProduct.getId());

						int qty = Integer.parseInt(orderLine[columnQuantity]);
						if ( qty < 1 ) {
							log.info("ERROR: Invalid quantity value");
							log.info(" - Quantity: " + orderLine[columnQuantity]);
							log.info(" - Store: " + storeNum);
							log.info(" - ProductID: " + targetProduct.getId());
							break;
						}
						def boolean validCheck = Boolean.parseBoolean(targetProduct.get("validitem"));
						if (validCheck) {
							log.info(" - Item is valid!");
							//Everything is good... Create the cart item!
							CartItem orderitem = new CartItem();
							Product product = store.getProduct(targetProduct.getId());
							orderitem.setProperty("orderid", order.getId());//foriegn key
							orderitem.setProduct(product);
							orderitem.setQuantity(Integer.parseInt(orderLine[columnQuantity]));
							
							orderitem.setProperty("productname", targetProduct.getName());
							
							orderitem.setProperty("store", storeNum);
							orderitem.setProperty("storelevel", targetStore.level.replace(" ", ""));
							orderitem.setProperty("storename", targetStore.name);
							Data distributor = searchForDistributor(manager, archive, orderLine[columnManfactName]);
							orderitem.setProperty("distributor", distributor.id);
							orderitem.setProperty("validitem", "true");
							orderitem.setProperty("as400id", orderLine[columnAS400id]);
	
							itemsearcher.saveData(orderitem, context.getUser());
						} else {
							badProductCount++;
							log.info(" - Item is INVALID!");
							String errMsg = "INVALID Product ID: " + targetProduct.getId();
							badProductList.add(errMsg);
						}
					} else {

						//ID Does not exist!!! Add to badProductIDList
						badProductCount++;
						String errMsg = "BAD Product ID: " + orderLine[columnAS400id];
						badProductList.add(errMsg);
						log.info(errMsg);

					}
				}
				if (badProductList.size() > 0) {
					context.putPageValue("badlist", badProductList);
				}
				ordersearcher.saveData(order, context.getUser());
				context.putPageValue("order", order);
			}
		}
		finally
		{
			FileUtils.safeClose(reader);
		}
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
	
	ImportRogersOrder importOrder = new ImportRogersOrder();
	importOrder.setLog(logs);
	importOrder.setContext(context);
	importOrder.setModuleManager(moduleManager);
	importOrder.setPageManager(pageManager);
	
	//Read the production value
	boolean production = Boolean.parseBoolean(context.findValue('productionmode'));

	importOrder.orderImport();
}
finally {
	logs.stopCapture();
}
