import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.store.InventoryItem
import org.openedit.store.Product
import org.openedit.store.Store

import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery



public void init(){
	WebPageRequest req = context;
	MediaArchive archive = req.getPageValue("mediaarchive");
	Store store = req.getPageValue("store");
	Searcher searcher = store.getProductSearcher();
	String [] fields = req.getRequestParameters("field");
	String [] operations = req.getRequestParameters("operation");
	if (!fields || !operations){
		log.info("Require fields and operations search parameters, aborting");
		return;
	}
	if (fields.length != operations.length){
		log.info("Require fields and operations of equal length, aborting");
		return;	
	}
	SearchQuery query = searcher.createSearchQuery();
	for (int i=0; i < fields.length; i++){
		String field = fields[i];
		String operation = operations[i];
		String value = req.getRequestParameter("${field}.value");
		if (value){
			if ("matches".equals(operation)){
				query.addMatches(field, value);
			} else if ("contains".equals(operation)){
				query.addContains(field, value);
			} else if ("startswith".equals(operation)){
				query.addStartsWith(field, value);
			} else if ("not".equals(operation)){
				query.addNot(field, value);
			} else {
				log.info("Unknown operation, $operation, ignoring");
			}
		}
	}
	if (query.isEmpty()){
		log.info("Query is empty, aborting");
		return;
	}
	int quantity = toInt(req.getRequestParameter("inventory"),-1);
	log.info("Updating quantity levels for ${query.toFriendly()} to $quantity");
	List list = new ArrayList();
	HitTracker hits = searcher.search(query);
	hits.each{
		Product product = searcher.searchById(it.id);
		if (product && product.getInventoryItemCount() > 0){
			log.info("Updating $product");
			InventoryItem item = product.getInventoryItem(0);
			item.setQuantityInStock(quantity);
			list.add(product);
			if (list.size() == 100){
				searcher.saveAllData(list, null);
				list.clear();
			}
		}
	}
	if (!list.isEmpty()){
		searcher.saveAllData(list, null);
	}
	log.info("Finished updating product inventory levels");
}

public int toInt(String str, int defaultInt){
	if (str){
		try{
			return Integer.parseInt(str);
		}catch (Exception e){}
	}
	return defaultInt;
}

init();