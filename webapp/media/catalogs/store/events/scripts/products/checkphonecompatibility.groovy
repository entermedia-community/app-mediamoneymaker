package products

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.store.Product

import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker
import com.openedit.page.manage.PageManager

public class CheckPhoneCompatibility extends EnterMediaObject {

	public List<String> doIt() {
		
		log.info("PROCESS: ExportEdiOrder.init()");
		
		List<String> export = new ArrayList<String>();
		
		WebPageRequest inReq = context;

		MediaArchive archive = inReq.getPageValue("mediaarchive");
		SearcherManager manager = archive.getSearcherManager();

		String catalogid = archive.getCatalogId();
		PageManager pageManager = archive.getPageManager();

		//Create Searcher Object
		Searcher phoneSearcher = manager.getSearcher(archive.getCatalogId(), "phone");
		Searcher productsearcher = manager.getSearcher(archive.getCatalogId(), "product");
		
		HitTracker phoneList = phoneSearcher.getAllHits();
		for (Iterator phoneIterator = phoneList.iterator(); phoneIterator.hasNext();) {
			Data phone = (Data) phoneIterator.next();
			if (phone.get("active") == "true") {
				HitTracker productList = productsearcher.fieldSearch("phone", phone.getId());
				if (productList.size() == 0) {
					phone.setProperty("active", "false");
					String strMsg = phone.getId() + ":" + phone.getName(); 
					export.add(strMsg);
					log.info(strMsg + " has no accessories associated with it");
				} else {
					String strMsg = phone.getId() + ":" + phone.getName();
					log.info(strMsg + " has " + productList.size().toString() + " accessories associated with it");
					//phone.setProperty("active", "true");
				}
			} else {
				log.info(phone.getId() + ":" + phone.getName() + " is not active.");
			}
		}
		return export;
	}
}

logs = new ScriptLogger();
logs.startCapture();

try {
	CheckPhoneCompatibility checkPhones = new CheckPhoneCompatibility();
	checkPhones.setLog(logs);
	checkPhones.setContext(context);
	checkPhones.setModuleManager(moduleManager);
	checkPhones.setPageManager(pageManager);
	List export = checkPhones.doIt();
	context.putPageValue("exportphones", export);
}
finally {
	logs.stopCapture();
}

