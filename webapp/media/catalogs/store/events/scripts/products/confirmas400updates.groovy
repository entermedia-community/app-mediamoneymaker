package products

import org.entermedia.locks.Lock
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
	if (webevent == null){
		log.info("No webevent found, exiting");
		return;
	}
	String uuid = webevent.get("uuid");
	if (uuid){
		Lock lock = archive.getLockManager().lockIfPossible(archive.getCatalogId(), "/media/catalogs/store/confirmupdateslock/", "admin");
		log.info("Result from getting LOCK: $lock");
		if (lock){
			try{
				List updates = new ArrayList();
				List todelete = new ArrayList();
				SearchQuery query = updatesearcher.createSearchQuery();
				query.addMatches("uuid", uuid);
				HitTracker hits = updatesearcher.search(query);
				hits.each{
					Product product = productsearcher.searchById(it.productid);
					if (product){
						Data data = (Data) it;
						todelete.add(data);
						if (it.rogersas400id) {
							product.setRogersAS400Id(it.rogersas400id);
						}
						else {
							product.removeProperty("rogersas400id");
						}
						if (it.fidoas400id) {
							product.setFidoAS400Id(it.fidoas400id);
						}
						else {
							product.removeProperty("fidoas400id");
						}
						updates.add(product);
						if (updates.size() == 100){
							productsearcher.saveAllData(updates, null);
							updates.clear();
						}
					}
				}
				if (updates.isEmpty() == false){
					productsearcher.saveAllData(updates, null);
					updates.clear();
				}
				//save some stats
				req.putPageValue("updates", "${hits.size()}");
				//nuke update refs
				for(Data data:todelete){
					updatesearcher.delete(data, null);
				}
				todelete.clear();
				log.info("finished deleting entries");
			}
			finally{
				if (lock){
					archive.releaseLock(lock);
				}
			}
		} else {
			log.info("Warning: another thread already has lock, aborting");
		}
	}
	log.info("finished processing event, exiting");
}

init();
