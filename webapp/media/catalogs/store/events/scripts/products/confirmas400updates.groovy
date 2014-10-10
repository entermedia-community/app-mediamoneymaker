package products

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.event.WebEvent
import org.openedit.store.Product
import org.openedit.store.Store

import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery

public void init(){
	log.info("Confirm Rogers AS400 IDs");
	
	WebPageRequest req = context;
	MediaArchive archive = req.getPageValue("mediaarchive");
	Store store = req.getPageValue("store");
	Searcher productsearcher = store.getProductSearcher();
	Searcher updatesearcher = archive.getSearcher("productupdates");
	
	
	WebEvent webevent = req.getPageValue("webevent");
	if (webevent == null)
	{
		log.info("No webevent found, exiting");
		return;
	}
	String uuid = webevent.get("uuid");
	log.info("#### SUCESS $uuid");
	//COMMENT THIS BACK IN ONCE THINGS ARE WORKING
//	if (uuid){
//		List updates = new ArrayList();
//		SearchQuery query = updatesearcher.createSearchQuery();
//		query.addMatches("uuid", uuid);
//		HitTracker hits = updatesearcher.search(query);
//		hits.each{
//			Product product = productsearcher.searchById(it.productid);
//			if (product){
//				if (it.rogersas400id) {
//					product.setRogersAS400Id(it.rogersas400id);
//				}
//				else {
//					product.removeProperty("rogersas400id");
//				}
//				if (it.fidoas400id) {
//					product.setFidoAS400Id(it.fidoas400id);
//				}
//				else {
//					product.removeProperty("fidoas400id");
//				}
//				updates.add(product);
//				if (updates.size() == 100){
//					productsearcher.saveAllData(updates, null);
//					updates.clear();
//				}
//			}
//		}
//		if (updates.isEmpty() == false){
//			productsearcher.saveAllData(updates, null);
//			updates.clear();
//		}
//		//save some stats
//		req.putPageValue("updates", "${hits.size()}");
//		//nuke update refs
//		hits.each{
//			Data data = it;
//			updatesearcher.delete(data, null);
//		}
//	}
}

init();
