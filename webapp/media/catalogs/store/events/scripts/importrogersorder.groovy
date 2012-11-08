
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
import org.openedit.util.DateStorageUtil

import com.openedit.BaseWebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.page.Page
import com.openedit.util.FileUtils

public class ImportRogersOrder extends EnterMediaObject {

	public void orderImport(){
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
		SearcherManager manager = archive.getSearcherManager();

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

		MediaArchive mediaarchive = context.getPageValue("mediaarchive");
		String catalogid = getMediaArchive().getCatalogId();
		Searcher ordersearcher = manager.getSearcher(archive.getCatalogId(), "rogers_order");
		Searcher itemsearcher = manager.getSearcher(archive.getCatalogId(), "rogers_order_item");

		boolean production = Boolean.parseBoolean(context.findValue("productionmode"));
		
		//PropertyDetail detail = itemsearcher.getDetail("quantity");
		//detail.get("column");

		Page upload = archive.getPageManager().getPage("/${catalogid}/temp/upload/order.csv");
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
				def badProductList = new ArrayList();
				Data order = ordersearcher.createNewData();
				order.setId(ordersearcher.nextId());
				order.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
				order.setProperty("rogersorderstatus", "new");
				order.setName(order.getId());

				String[] orderLine;
				while ((orderLine = read.readNext()) != null)
				{
					def storeNum = orderLine[columnStore].trim();


					//Create as400list Searcher
					Searcher storeList = manager.getSearcher(archive.getCatalogId(), "profile_address_list");
					Data targetStore = storeList.searchByField("store", storeNum);
					if(targetStore == null){
						targetStore = storeList.createNewData();
						targetStore.setId(storeList.nextId());
						targetStore.setProperty("store", storeNum);
						//TODO: pull out address details etc from spreadsheet
						targetStore.setProperty("name", orderLine[columnStoreName]);
						targetStore.setProperty("level", orderLine[columnStoreRank].replace(" ", ""));
						targetStore.setSourcePath("rogers");
						storeList.saveData(targetStore, context.getUser());

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
						def String validItem = "false";;
						if (targetProduct.upc != null) {
							log.info(" - UPC Found: " + targetProduct.upc);
							validItem = "true";
						}

						//Everything is good... Craete the cart!

						Data orderitem = itemsearcher.createNewData();
						orderitem.setProperty("rogers_order", order.getId());//foriegn key
						orderitem.setProperty("product", targetProduct.getId());
						orderitem.setProperty("productname", targetProduct.getName());
						orderitem.setProperty("quantity", orderLine[columnQuantity]);
						orderitem.setProperty("store", storeNum);
						orderitem.setProperty("storelevel", targetStore.level.replace(" ", ""));
						orderitem.setProperty("storename", targetStore.name);
						Data distributor = searchForDistributor(manager, archive, orderLine[columnManfactName]);
						orderitem.setProperty("distributor", distributor.id);
						orderitem.setProperty("validitem", validItem);
						orderitem.setProperty("as400id", orderLine[columnAS400id]);

						itemsearcher.saveData(orderitem, context.getUser());

						//				log.info("CartItem Created: " + cartItem.getProduct().name
						//						+ ":" + cartItem.getQuantity() + ":" + targetStore.store);

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
