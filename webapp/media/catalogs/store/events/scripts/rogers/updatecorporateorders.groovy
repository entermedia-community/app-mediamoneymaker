import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.store.Product

import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery


public void init(){
	log.info("Updating Corporate Orders");
	
	WebPageRequest req = context;
	MediaArchive archive = req.getPageValue("mediaarchive");
	
	String operation = req.getRequestParameter("operation");
	String uuid = req.getRequestParameter("uuid");
	String entry = req.getRequestParameter("entry");
	String quantityupdate = req.getRequestParameter("quantity");
	
	//operations: update, refresh, delete, deletestore
	Searcher searcher = archive.getSearcher("corporateorder");
	if (operation == "update"){	
		if (quantityupdate){
			int val = toInt(quantityupdate,-1);
			if (val > 0){
				if (entry){
					Data data = searcher.searchById(entry);
					data.setProperty("quantity", "$val");
					searcher.saveData(data, null);
				}
			}
		}	
	}
	else if (operation == "delete"){
		if (entry){
			Data data = searcher.searchById(entry);
			if (data) {
				searcher.delete(data, null);
			}
		}
	}
	else if (operation == "deletestore"){
		if (uuid && entry){
			SearchQuery query = searcher.createSearchQuery();
			query.addMatches("uuid", uuid);
			query.addMatches("store",entry);
			HitTracker hits = searcher.search(query);
			hits.each{
				Data data = it;
				if (data) {
					searcher.delete(data, null);
				}
			}
		}
	}
	else if (operation == "quickfix"){
		if (uuid){
			SearchQuery query = searcher.createSearchQuery();
			query.addMatches("uuid", uuid);
			query.addNot("status","ok");
			HitTracker hits = searcher.search(query);
			hits.each{
				Data data = it;
				if (data) {
					searcher.delete(data, null);
				}
			}
		}
	}
	else if (operation == "fixstore"){
		if (uuid && entry){
			SearchQuery query = searcher.createSearchQuery();
			query.addMatches("uuid", uuid);
			query.addMatches("store",entry);
			query.addNot("status","ok");
			HitTracker hits = searcher.search(query);
			hits.each{
				Data data = it;
				if (data) {
					searcher.delete(data, null);
				}
			}
		}
	}
	else if (operation == "fixoutofstock"){
		if (uuid && entry){
			SearchQuery query = searcher.createSearchQuery();
			query.addMatches("uuid", uuid);
			query.addMatches("store",entry);
			query.addMatches("status","outofstock");
			HitTracker hits = searcher.search(query);
			hits.each{
				Data data = it;
				if (data) {
					searcher.delete(data, null);
				}
			}
		}
	}
	//now re-check the status of all orders
	Searcher productsearcher = archive.getSearcher("product");
	Searcher storesearcher = archive.getSearcher("rogersstore");
	
	List updates = new ArrayList();
	Map<String,Integer> inventory = new HashMap<String,Integer>();
	Map<String,Integer> storemap = new HashMap<String,Integer>();//tally of store quantities
	
	SearchQuery query = searcher.createSearchQuery();
	query.addMatches("uuid",uuid);
	query.addSortBy("idUp");
	HitTracker hits = searcher.search(query);
	hits.each{
		Data order = searcher.searchById(it.id);
		String status = "ok";
		int inStock = 0;
		int delta = 0;
		String storeid = order.get("store");
		Data store = storeid ? storesearcher.searchById(storeid) : null;
		if (store){
			String productid = order.get("product");
			int quantity = toInt(order.get("quantity"),0);
			if (storemap.containsKey(storeid) == false){
				storemap.put(storeid,new Integer(quantity));
			} else {
				int tally = storemap.get(storeid).intValue();
				tally += quantity;
				storemap.put(storeid,new Integer(tally));
			}
			if (productid){
				Data productdata = productsearcher.searchByField("as400id", productid);
				if (productdata){
					Product product = productsearcher.searchById(productdata.getId());
					if (product){
						//add tally of inventory here
						if (quantity > 0 && product.getInventoryItem(0) != null){
							inStock = product.getInventoryItem(0).getQuantityInStock();
							if (inventory.containsKey(product.getId()) == false){
								inventory.put(product.getId(), new Integer(inStock));
							} else {
								inStock = inventory.get(product.getId()).intValue();
							}
							delta = inStock;
							if (inStock <= 0){
								status = "outofstock";
							} else if (inStock < quantity) {
								status = "insufficientstock";
							} else {
								if (store){
									delta = inStock - quantity;
									inventory.put(product.getId(), new Integer(delta));
								}
							}
						} else if (product.getInventoryItem(0) == null) {
							status = "inventoryerror";
						}
					} else {
						status = "productunknown";
					}
				} else {
					status = "productunknown";
				}
			} else {
				status = "productunknown";
			}
		} else {
			status = "storeunknown" ;
		}
		order.setProperty("inventory", "${inStock}");
		order.setProperty("delta", "${delta}");
		order.setProperty("status",status);
		
		
		updates.add(order);
	}
	if (updates.isEmpty() == false){
		searcher.saveAllData(updates, null);
	}
	//need to check each order for all zero quantities
	Iterator stores = storemap.keySet().iterator();
	while(stores.hasNext()){
		String storeid = stores.next();
		Integer tally = storemap.get(storeid);
		if (tally.intValue() > 0){
			continue;
		}
		query = searcher.createSearchQuery();
		query.addMatches("uuid",uuid);
		query.addMatches("store",storeid);
		query.addSortBy("idUp");
		hits = searcher.search(query);
		hits.each{
			Data data = searcher.searchById(it.id);
			if (data){
				searcher.delete(data, null);
			}
		}
	}
	
	
}

public int toInt(String inVal, int inDefault){
	int out = inDefault;
	try{
		out = Integer.parseInt(inVal);
	}catch(Exception e){}
	return out;
}

init();