
/*
 * Created on October 4th, 2012
 * Created by Peter Floyd
 */

//Import List
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.util.CSVReader
import org.openedit.store.Cart
import org.openedit.store.CartItem
import org.openedit.store.Store
import org.openedit.util.DateStorageUtil;

import com.openedit.BaseWebPageRequest
import com.openedit.page.Page
import com.openedit.util.FileUtils

public void handleSubmission(){
	//Create Store, MediaArchive Object

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

	//Create the Cart Object
	Cart cart = inReq.getPageValue("cart");
	cart.removeAllItems();

	//Define columns from spreadsheet
	def int columnStore = 4;
	def int columnStoreRank = 5;
	def int columnStoreName = 6;
	def int columnManfactID = 7;
	def int columnManfactName = 8;
	def int columnAS400id = 9;
	def int columnAvailable = 19;
	def int columnSuggest = 20;
	def int columnQuantity = 21;
	def int columnRogersID = 33;
	def int columnRogersOtherID = 0;

	//Create as400list Searcher
	Searcher as400list = manager.getSearcher(archive.getCatalogId(), "as400");

	Searcher ordersearcher = manager.getSearcher(archive.getCatalogId(), "rogers_order");
	Searcher itemsearcher = manager.getSearcher(archive.getCatalogId(), "rogers_order_item");

	Page upload = archive.getPageManager().getPage("/${mediaarchive.catalogId}/temp/upload/order.csv");
	Reader reader = upload.getReader();
	try
	{
		def SEARCH_FIELD = "rogerssku";
		boolean done = false;

		//Create CSV reader
		CSVReader read = new CSVReader(reader, ',', '\"');

		//Read 1 line of header
		String[] headers = read.readNext();

		int productCount = 0;
		int badProductCount = 0;
		def badProductList = new ArrayList();
		Data order = ordersearcher.createNewData();
		order.setId(ordersearcher.nextId());
		order.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
		order.setProperty("orderstatus", "new");
		
		String[] orderLine;
		while ((orderLine = read.readNext()) != null)
		{
			def storeNum = orderLine[columnStore].trim();
			def manufacturerID = 

			//Create as400list Searcher
			Searcher storeList = manager.getSearcher(archive.getCatalogId(), "userprofile_address");
			Data targetStore = storeList.searchByField("store", storeNum);
			if(targetStore == null){
				targetStore = storeList.createNewData();
				targetStore.setId(storeList.nextId());
				targetStore.setProperty("store", storeNum);
				//pull out address details etc from spreadsheet
				targetStore.setProperty("name", orderLine[columnStoreName]);
				targetStore.setProperty("level", orderLine[columnStoreRank]);
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

				//Everything is good... Craete the cart!
				
				Data orderitem = itemsearcher.createNewData();
				orderitem.setProperty("rogers_order", order.getId());//foriegn key
				orderitem.setProperty("product", targetProduct.getId());
				orderitem.setProperty("productname", targetProduct.getName());
				orderitem.setProperty("quantity", qty);
				orderitem.setProperty("store", targetStore.getId());
				orderitem.setProperty("storelevel", targetStore.level);
				orderitem.setProperty("storename", targetStore.name);
				orderitem.setProperty("distributor", orderLine[columnManfactName]);
				
				itemsearcher.saveData(orderitem, context.getUser());

				log.info("CartItem Created: " + cartItem.getProduct().name
						+ ":" + cartItem.getQuantity() + ":" + targetStore.store);

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
	}
	finally
	{
		FileUtils.safeClose(reader);
	}
}

handleSubmission();
