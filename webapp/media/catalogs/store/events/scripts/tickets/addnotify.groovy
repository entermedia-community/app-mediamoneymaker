package tickets

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.store.util.MediaUtilities
import org.openedit.util.DateStorageUtil

import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.GroovyScriptRunner
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery

public class AddNotify extends EnterMediaObject {
	
	public void init() {
		
		//Get Media Info
		Log log = LogFactory.getLog(GroovyScriptRunner.class);
		
		WebPageRequest inReq = context;

		MediaArchive archive = context.getPageValue("mediaarchive");
		SearcherManager manager = archive.getSearcherManager();
		String catalogid = archive.getCatalogId();
		
		MediaUtilities media = new MediaUtilities();
		media.setContext(context);
		
		String doAddNotify = inReq.getRequestParameter("notifyme");
		if (doAddNotify.equals("yes")) {
			
			String productid = inReq.getRequestParameter("product.value");
			
			Searcher notifysearcher = manager.getSearcher(catalogid, "stocknotify");
			SearchQuery query = notifysearcher.createSearchQuery();
			query.addMatches("product", productid);
			query.addMatches("owner", inReq.getUser().getId());
			query.addMatches("notificationsent", "false");
			HitTracker notifyHits = notifysearcher.search(query);
			
			if (notifyHits.size() == 0) {
				Data addNew = notifysearcher.createNewData();
				addNew.setId(notifysearcher.nextId());
				addNew.setName("Notification" + addNew.getId());
				addNew.setProperty("owner", inReq.getUser().getId());
				addNew.setProperty("notificationsent", "false");
				addNew.setProperty("product", productid);
				addNew.setProperty("ticketstatus", "open");
				addNew.setProperty("tickettype", "stockrequest");
				Date now = new Date();
				addNew.setProperty("datesubmitted", DateStorageUtil.getStorageUtil().formatForStorage(now));
				notifysearcher.saveData(addNew, inReq.getUser());
				
				inReq.putPageValue("notification", "setup");
				inReq.putPageValue("productid", productid);
			} else {
				inReq.putPageValue("notification", "duplicate");
				return;
			}		
		} else {
			inReq.putPageValue("notification", "notset");
			return;
		}		
	}
}

logs = new ScriptLogger();
logs.startCapture();

try {
	AddNotify addNotify = new AddNotify();
	addNotify.setLog(logs);
	addNotify.setContext(context);
	addNotify.setModuleManager(moduleManager);
	addNotify.setPageManager(pageManager);

	addNotify.init();
}
finally {
	logs.stopCapture();
}
