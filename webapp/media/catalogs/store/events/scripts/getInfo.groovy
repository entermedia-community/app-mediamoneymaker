
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
	
		//Read 4 lines of headers
		def numHeaderLines = 4;
		for ( i in 1..numHeaderLines )
		{
			String[] headers = read.readNext();
		}
		int productCount = 0;
	
		//Read in All of the product IDs
		String[] productIDs = read.readNext();
	
		def productIDList = new ArrayList();
		def badProductIDList = new ArrayList<String>();

		//Loop through each line
		for ( id in 1..productIDs.length-1 )
		{
			if (productIDs[id] != "(empty)" && productIDs[id] != "") {
				
				Data as400info = as400list.searchByField("as400_sku", productIDs[id]);
				if (as400info != null) {

					//Read the oraclesku from the as400 table
					
					String rogerssku = as400info.oracle_sku;
					
					//Search the product for the oracle sku(rogerssku)
					Data target = productsearcher.searchByField(SEARCH_FIELD, rogerssku);
					if (target != null) {
						
						//Add the target.id to the productIDList
						productIDList.add(target.getId());
					
						//productsearcher.saveData(real, context.getUser());
						log.info("ProductID Found: " + target.getId());
						productCount++;
						
					} else {
					
						//ID Does not exist!!! Add to badProductIDList
						badProductIDList.add(productIDs[id]);
						log.info("BAD Product ID: " + productIDs[id]);
						
					}
				} else {
				
					//ID Does not exist!!! Add to badProductIDList
					badProductIDList.add(productIDs[id]);
					log.info("BAD Product ID: " + productIDs[id]);
					
				}
			} else {
			
				//ID Does not exist!!! Add to badProductIDList
				log.info("Empty ID found in list");
				
			}
		}
		String[] storeQuantities;
		while ((storeQuantities = read.readNext()) != null)
		{
			if (storeQuantities[0] != "ISTORE" && storeQuantities[0] != "Grand Total") {
				
				def storeNum = storeQuantities[0].trim();
				//Create as400list Searcher
				Searcher storeList = manager.getSearcher(archive.getCatalogId(), "userprofile_address");
				Data targetStore = storeList.searchByField("store", storeNum);
				if (targetStore != null) {
					
					for (index in 1..productCount)
					{
						if (storeQuantities[index] != "(empty)" && storeQuantities[index] != "") {
							//Create cartItem
							CartItem cartItem = new CartItem();
							cartItem.setProduct(store.getProduct(productIDList.get(index-1)));
							int qty = Integer.parseInt(storeQuantities[index]);
							if ( qty != null && qty > 0 ) {
								cartItem.setQuantity(qty);
							} else {
								log.info("ERROR: Invalid quantity value");
								log.info(" - Quantity: " + storeQuantities[index]);
								log.info(" - Store: " + storeNum);
								log.info(" - ProductID: " + productIDList.get(index-1));
								cartItem = null;
								break;
							}
							//cartItem.setProperty("store", storeQuantities[0].trim());
							//Add the item to the Cart
							cart.addItem(cartItem);
						
							log.info("CartItem Created: " + cartItem.getProduct().name 
								+ ":" + storeQuantities[index] + ":" + targetStore.store);
						
						} else {
						
							log.info("Blank quantity found. Skipping");
							
						}
					}
				} else {
				
					//ID Does not exist!!! Add to badProductIDList
					log.info("Store not found in list: " + storeNum);
					
				}
			}
		}
		
	}
	finally
	{
		FileUtils.safeClose(reader);
	}
	
}

handleSubmission();
