package products

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.util.CSVReader
import org.openedit.store.Product
import org.openedit.store.Store
import org.openedit.util.DateStorageUtil

import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.page.Page
import com.openedit.util.FileUtils

public void init(){
	log.info("Clearing Rogers AS400 IDs");
	
	WebPageRequest req = context;
	MediaArchive archive = req.getPageValue("mediaarchive");
	Store store = req.getPageValue("store");
	Searcher searcher = store.getProductSearcher();
	List updates = new ArrayList();
	HitTracker hits = searcher.getAllHits();
	hits.each{
		Product product = searcher.searchById(it.id);
		if (product){
			boolean add = false;
			if (product.get("rogersas400id")) {
				product.removeProperty("rogersas400id");
				add = true;
			}
			if (product.get("fidoas400id")) {
				product.removeProperty("fidoas400id");
				add = true;
			}
			if (product.get("as400id")) {
				product.removeProperty("as400id");
				add = true;
			}
			if (add == true){
				updates.add(product);
				if (updates.size() == 100){
					searcher.saveAllData(updates, null);
					updates.clear();
				}
			}
		}
	}
	searcher.saveAllData(updates, null);
	updates.clear();
}
init();
