package products

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.store.Store

import com.openedit.OpenEditException
import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery

public class GetProductInfo extends EnterMediaObject {

	public void getResults() {
		
		String strMsg = "";
		WebPageRequest inReq = context;
		Store store = null;
		try {
			store  = inReq.getPageValue("store");
			if (store != null) {
				log.info("Store loaded");
			} else {
				strMsg = "ERROR: Could not load store";
				throw new Exception(strMsg);
			}
		}
		catch (Exception e) {
			strMsg += "Exception thrown:\n";
			strMsg += "Local Message: " + e.getLocalizedMessage() + "\n";
			strMsg += "Stack Trace: " + e.getStackTrace().toString();;
			log.info(strMsg);
			throw new OpenEditException(strMsg);
		}
		
		// Create the Searcher Objects to read values!
		MediaArchive archive = inReq.getPageValue("mediaarchive");
		String catalogid = getMediaArchive().getCatalogId();
		SearcherManager manager = archive.getSearcherManager();

		//Create the searcher objects.
		Searcher categoryIDSearcher = manager.getSearcher(catalogid, "categoryid");
		Searcher productsearcher = manager.getSearcher(archive.getCatalogId(), "product");
		
		HitTracker list = productsearcher.fieldSearch("category", "index", "random",  inReq);
		inReq.putPageValue("list", list);
		inReq.getRequestParameter(catalogid);
		inReq.getPageValue(catalogid);
	}
}

log = new ScriptLogger();
log.startCapture();

try {

	log.info("START - getProductResults");
	GetProductResults getProductInfo = new GetProductInfo();
	getProductInfo.setLog(log);
	getProductInfo.setContext(context);
	getProductInfo.setModuleManager(moduleManager);
	getProductInfo.setPageManager(pageManager);
	getProductInfo.getResults();
	log.info("FINISH - getProductResults");
}
finally {
	log.stopCapture();
}
