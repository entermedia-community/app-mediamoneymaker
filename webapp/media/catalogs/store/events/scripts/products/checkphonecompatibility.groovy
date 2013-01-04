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

	public List<String> doCheck(String choice) {
		
		log.info("PROCESS: CheckPhoneCompatibility.doIt()");
		
		List<String> export = new ArrayList<String>();
		
		WebPageRequest inReq = context;

		MediaArchive archive = inReq.getPageValue("mediaarchive");
		SearcherManager manager = archive.getSearcherManager();

		String catalogid = archive.getCatalogId();
		PageManager pageManager = archive.getPageManager();

		//Create Searcher Object
		Searcher phonesearcher = manager.getSearcher(archive.getCatalogId(), "phone");
		Searcher manufacturersearcher = manager.getSearcher(archive.getCatalogId(), "manufacturer");
		Searcher productsearcher = manager.getSearcher(archive.getCatalogId(), "product");

		export.add("<strong>" + choice + "</strong>");
		int counter = 0;
				
		HitTracker theList;
		if (choice.equals("phone")) {
			theList = phonesearcher.getAllHits();
			for (Iterator iterator = theList.iterator(); iterator.hasNext();) {
				Data item = (Data) iterator.next();
				if ((item.get("active") == null) || (item.get("active") == "true")) {
					HitTracker productList = productsearcher.fieldSearch("phone", item.getId());
					if (choice.equals("manufacturer")) {
						log.info(item.getName() + "List Count: " + productList.size().toString());
					}
					if (productList.size() == 0) {
						item.setProperty("active", "false");
						String strMsg = item.getId() + ":" + item.getName(); 
						export.add(strMsg);
						log.info(strMsg + " has no accessories associated with it");
						counter++;
					} else {
						String strMsg = item.getId() + ":" + item.getName();
						log.info(strMsg + " has " + productList.size().toString() + " accessories associated with it");
						//phone.setProperty("active", "true");
					}
				} else {
					log.info(item.getId() + ":" + item.getName() + " is not active.");
				}
			}
		} else if (choice.equals("manufacturer")) {
			theList = manufacturersearcher.getAllHits();
			for (Iterator iterator = theList.iterator(); iterator.hasNext();) {
				Data item = (Data) iterator.next();
				if ((item.get("active") == null) || (item.get("active") == "true")) {
					HitTracker productList = productsearcher.fieldSearch("manufacturer", item.getId());
					if (choice.equals("manufacturer")) {
						log.info(item.getName() + "List Count: " + productList.size().toString());
					}
					if (productList.size() == 0) {
						item.setProperty("active", "false");
						String strMsg = item.getId() + ":" + item.getName(); 
						export.add(strMsg);
						log.info(strMsg + " has no accessories associated with it");
						counter++;
					} else {
						String strMsg = item.getId() + ":" + item.getName();
						log.info(strMsg + " has " + productList.size().toString() + " accessories associated with it");
						//phone.setProperty("active", "true");
					}
				} else {
					log.info(item.getId() + ":" + item.getName() + " is not active.");
				}
			}
		}
		if (counter == 0) {
			export.add("No items in the list");
		}
		return export;
	}
}

logs = new ScriptLogger();
logs.startCapture();

try {
	CheckPhoneCompatibility checkData = new CheckPhoneCompatibility();
	checkData.setLog(logs);
	checkData.setContext(context);
	checkData.setModuleManager(moduleManager);
	checkData.setPageManager(pageManager);
	List export = checkData.doCheck("phone");
	context.putPageValue("exportphones", export);
	export = null;
	export = checkData.doCheck("manufacturer");
	context.putPageValue("exportmanufacturer", export);
}
finally {
	logs.stopCapture();
}

