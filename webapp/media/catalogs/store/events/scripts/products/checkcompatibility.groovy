package products

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive

import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker
import com.openedit.page.manage.PageManager

public class CheckCompatibility extends EnterMediaObject {

	public List<String> doPhoneCheck() {
		
		log.info("PROCESS: CheckCompatibility.doPhoneCheck()");
		
		List<String> export = new ArrayList<String>();
		
		WebPageRequest inReq = context;

		MediaArchive archive = inReq.getPageValue("mediaarchive");
		SearcherManager manager = archive.getSearcherManager();

		String catalogid = archive.getCatalogId();
		PageManager pageManager = archive.getPageManager();

		//Create Searcher Object
		Searcher phonesearcher = manager.getSearcher(archive.getCatalogId(), "phone");
		Searcher productsearcher = manager.getSearcher(archive.getCatalogId(), "product");

		int counter = 0;
				
		HitTracker theList;
		theList = phonesearcher.getAllHits();
		for (Iterator iterator = theList.iterator(); iterator.hasNext();) {
			Data item = (Data) iterator.next();
			if (item.get("active") != null) {
				if (item.get("active") == "true") {
					HitTracker productList = productsearcher.fieldSearch("phone", item.getId());
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
			} else {
				HitTracker productList = productsearcher.fieldSearch("phone", item.getId());
				if (productList.size() == 0) {
					item.setProperty("active", "false");
					String strMsg = item.getId() + ":" + item.getName();
					export.add(strMsg);
					log.info(strMsg + " has no accessories associated with it");
					counter++;
				} else {
					item.setProperty("active", "true");
					String strMsg = item.getId() + ":" + item.getName();
					log.info(strMsg + " has " + productList.size().toString() + " accessories associated with it");
					//phone.setProperty("active", "true");
				}
			}
		}
		if (counter > 0) {
			phonesearcher.reIndexAll();
		}
		return export;
	}

	public List<String> doManufacturerCheck() {
		
		log.info("PROCESS: CheckCompatibility.doManufacturerCheck()");
		
		List<String> export = new ArrayList<String>();
		
		WebPageRequest inReq = context;

		MediaArchive archive = inReq.getPageValue("mediaarchive");
		SearcherManager manager = archive.getSearcherManager();

		String catalogid = archive.getCatalogId();
		PageManager pageManager = archive.getPageManager();

		//Create Searcher Object
		Searcher manufacturersearcher = manager.getSearcher(archive.getCatalogId(), "manufacturer");
		Searcher productsearcher = manager.getSearcher(archive.getCatalogId(), "product");

		int counter = 0;
				
		HitTracker theList;
		theList = manufacturersearcher.getAllHits();
		for (Iterator iterator = theList.iterator(); iterator.hasNext();) {
			Data item = (Data) iterator.next();
			if (item.get("active") != null) {
				if (item.get("active") == "true") {
					HitTracker productList = productsearcher.fieldSearch("manufacturerid", item.getId());
					if (productList.size() == 0) {
						item.setProperty("active", "false");
						String strMsg = item.getId() + ":" + item.getName();
						export.add(strMsg);
						log.info(strMsg + " has no products associated with it");
						counter++;
					} else {
						String strMsg = item.getId() + ":" + item.getName();
						log.info(strMsg + " has " + productList.size().toString() + " accessories associated with it");
						//phone.setProperty("active", "true");
					}
				} else {
					log.info(item.getId() + ":" + item.getName() + " is not active.");
				}
			} else {
				HitTracker productList = productsearcher.fieldSearch("manufacturerid", item.getId());
				if (productList.size() == 0) {
					item.setProperty("active", "false");
					String strMsg = item.getId() + ":" + item.getName();
					export.add(strMsg);
					log.info(strMsg + " has no products associated with it");
					counter++;
				} else {
					item.setProperty("active", "true");
					String strMsg = item.getId() + ":" + item.getName();
					log.info(strMsg + " has " + productList.size().toString() + " accessories associated with it");
					//phone.setProperty("active", "true");
				}
			}
		}
		if (counter > 0) {
			manufacturersearcher.reIndexAll();
		}
		return export;
	}
	public List<String> doCategoryCheck() {
		
		log.info("PROCESS: CheckCompatibility.doCategoryCheck()");
		
		List<String> export = new ArrayList<String>();
		
		WebPageRequest inReq = context;

		MediaArchive archive = inReq.getPageValue("mediaarchive");
		SearcherManager manager = archive.getSearcherManager();

		String catalogid = archive.getCatalogId();
		PageManager pageManager = archive.getPageManager();

		//Create Searcher Object
		Searcher categorysearcher = manager.getSearcher(archive.getCatalogId(), "categoryid");
		Searcher productsearcher = manager.getSearcher(archive.getCatalogId(), "product");

		int counter = 0;
				
		HitTracker theList;
		theList = categorysearcher.getAllHits();
		for (Iterator iterator = theList.iterator(); iterator.hasNext();) {
			Data item = (Data) iterator.next();
			if (item.get("active") != null) {
				if (item.get("active") == "true") {
					HitTracker productList = productsearcher.fieldSearch("categoryid", item.getId());
					if (productList.size() == 0) {
						item.setProperty("active", "false");
						String strMsg = item.getId() + ":" + item.getName();
						export.add(strMsg);
						log.info(strMsg + " has no products associated with it");
						counter++;
					} else {
						item.setProperty("active", "true");
						String strMsg = item.getId() + ":" + item.getName();
						log.info(strMsg + " has " + productList.size().toString() + " products associated with it");
						//phone.setProperty("active", "true");
					}
				} else {
					log.info(item.getId() + ":" + item.getName() + " is not active.");
				}
			} else {
				HitTracker productList = productsearcher.fieldSearch("categoryid", item.getId());
				if (productList.size() == 0) {
					item.setProperty("active", "false");
					String strMsg = item.getId() + ":" + item.getName();
					export.add(strMsg);
					log.info(strMsg + " has no products associated with it");
					counter++;
				} else {
					String strMsg = item.getId() + ":" + item.getName();
					log.info(strMsg + " has " + productList.size().toString() + " products associated with it");
					//phone.setProperty("active", "true");
				}
			}
		}
		if (counter > 0) {
			categorysearcher.reIndexAll();
		}
		return export;
	}
}

logs = new ScriptLogger();
logs.startCapture();

try {
	CheckCompatibility checkData = new CheckCompatibility();
	checkData.setLog(logs);
	checkData.setContext(context);
	checkData.setModuleManager(moduleManager);
	checkData.setPageManager(pageManager);
	
	//Master Export List
	List export;
	
	export = checkData.doPhoneCheck();
	context.putPageValue("exportphones", export);
	export = null;
	
	export = checkData.doManufacturerCheck();
	context.putPageValue("exportmanufacturer", export);
	export = null;

	export = checkData.doCategoryCheck();
	context.putPageValue("exportcategory", export);
	export = null;

}
finally {
	logs.stopCapture();
}

