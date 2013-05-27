package products

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.store.util.MediaUtilities

import com.openedit.OpenEditException
import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery

public class GetProductResults extends EnterMediaObject {

	public void getResults() {
		
		WebPageRequest inReq = context;
		String strMsg = "";
		
		//Create the MediaArchive object
		MediaUtilities media = new MediaUtilities();
		media.setContext(context);
		
		MediaArchive archive = media.getArchive();
		String catalogID = media.getCatalogid();

		//Create the searcher objects.
		Searcher categoryIDSearcher = archive.getSearcherManager().getSearcher(catalogID, "categoryid");
		Searcher productSearcher = media.getProductSearcher();
		
		SearchQuery categoryQuery = categoryIDSearcher.createSearchQuery();
		categoryQuery.addMatches("id", "*");
		categoryQuery.addMatches("active", "true");
		HitTracker categoryResults = categoryIDSearcher.cachedSearch(inReq, categoryQuery);
		if (categoryResults != null && categoryResults.size() > 0) {
			for(Iterator categoryIterator = categoryResults.iterator(); categoryIterator.hasNext();) {
				Data category = (Data)categoryIterator.next();
				
				SearchQuery productQuery = productSearcher.createSearchQuery();
				productQuery.addMatches("category", category.getName());
				HitTracker productResults = productSearcher.cachedSearch(inReq, productQuery);
				if (productResults != null && productResults.size() > 0) {
					strMsg = category.getName() + "_results";
					inReq.putPageValue(strMsg, productResults);
					log.info(strMsg + ":" + productResults.size().toString());
				}
			}
		} else {
			throw new OpenEditException("No categories to export");
		}
	}
}

log = new ScriptLogger();
log.startCapture();

try {

	log.info("START - getProductResults");
	GetProductResults getProductResults = new GetProductResults();
	getProductResults.setLog(log);
	getProductResults.setContext(context);
	getProductResults.setModuleManager(moduleManager);
	getProductResults.setPageManager(pageManager);
	getProductResults.getResults();
	log.info("FINISH - getProductResults");
}
finally {
	log.stopCapture();
}
