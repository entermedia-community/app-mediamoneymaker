
/*
 * Created on October 4th, 2012
 * Created by Peter Floyd
 */

//Import List
import java.util.List

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.util.CSVReader
import org.openedit.store.Cart
import org.openedit.store.CartItem
import org.openedit.store.Product
import org.openedit.store.Store

import com.openedit.page.Page
import com.openedit.util.FileUtils

public void handleSubmission(){
	//Create Store, MediaArchive Object
	Store store = context.getPageValue("store");
	MediaArchive archive = context.getPageValue("mediaarchive");
	
	//Create Searcher Object
	Searcher productsearcher = store.getProductSearcher();
	SearcherManager manager = archive.getSearcherManager();
	
//	Product product = productsearcher.createNewData();
//	String [] fields = context.getRequestParameters("field");
//	productsearcher.updateData(context, fields, product)
//	productsearcher.saveData(product, context.getUser());
	
	//Create the Cart Object
	Cart cart = context.getPageValue("cart");
	cart.removeAllItems();
	
	def int columnStore = 4;
	def int columnAS400id = 9;
	def int columnQuantity = 21;
	
	//Create as400list Searcher
	Searcher as400list = manager.getSearcher(archive.getCatalogId(), "as400");
	
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
	
		String[] orderLine;
		while ((orderLine = read.readNext()) != null)
		{
			def storeNum = orderLine[columnStore].trim();
			
			//Create as400list Searcher
			Searcher storeList = manager.getSearcher(archive.getCatalogId(), "userprofile_address");
			Data targetStore = storeList.searchByField("store", storeNum);
			if (targetStore != null) {
				
				//Get Product Info
				Data as400info = as400list.searchByField("as400_sku", orderLine[columnAS400id]);
				if (as400info != null) {
	
					//Read the oraclesku from the as400 table
					String rogerssku = as400info.oracle_sku;
					
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
						CartItem cartItem = new CartItem();
						cartItem.setProduct(store.getProduct(targetProduct.getId()));
						cartItem.setQuantity(qty);
						if(storeNum != null){
								cartItem.setProperty("store", targetStore.getId());
						}
						//Set the store info (TO DO!!!!!!!!!!!)
						//Store itemStore = new Store();
						
						//Add the item to the Cart
						cart.addItem(cartItem);
						
						log.info("CartItem Created: " + cartItem.getProduct().name
							+ ":" + cartItem.getQuantity() + ":" + targetStore.store);
							
		
					} else {
					
						//ID Does not exist!!! Add to badProductIDList
						badProductCount++;
						String errMsg = "BAD Product ID: " + orderLine[columnAS400id];
						badProductList.add(errMsg);
						log.info(errMsg);
						
					}
				} else {
				
					badProductCount++;
					//ID Does not exist!!! Add to badProductIDList
					String errMsg = "BAD Store Number: " + orderLine[columnStore];
					badProductList.add(errMsg);
					log.info(errMsg);
				}
	
			} else {
			
				badProductCount++;
				//ID Does not exist!!! Add to badProductIDList
				String errMsg = "Store not found in list: " + storeNum;
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
