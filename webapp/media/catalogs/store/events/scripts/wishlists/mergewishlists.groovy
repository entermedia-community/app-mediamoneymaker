package wishlists
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openedit.Data
import org.openedit.data.*
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.util.CSVWriter
import org.openedit.repository.filesystem.StringItem
import org.openedit.store.CartItem
import org.openedit.store.InventoryItem;
import org.openedit.store.Product
import org.openedit.store.orders.Order
import org.openedit.store.util.MediaUtilities

import com.openedit.OpenEditException
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.GroovyScriptRunner
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker
import com.openedit.page.Page


public class MergeWishLists extends EnterMediaObject {
	
	public void init(){
		
		//Get Media Info
		Log log = LogFactory.getLog(GroovyScriptRunner.class);

		MediaArchive archive = context.getPageValue("mediaarchive");
		SearcherManager manager = archive.getSearcherManager();
		String catalogid = archive.getCatalogId();
		
		MediaUtilities media = new MediaUtilities();
		media.setContext(context);

		// Create the Searcher Objects to read values!
		String action = media.getContext().getRequestParameter("action");
		String sessionid = media.getContext().getRequestParameter("hitssessionid");
		if (sessionid == null) {
			return;
		}
		HitTracker hits = media.getContext().getSessionValue(sessionid);
		if (hits == null) {
			return;
		}
		ArrayList wishLists = hits.getSelectedHits();
		log.info("Found # of WishLists:" + wishLists.size());
		
		if (action.equals("getName")) {
			context.putPageValue("sessionid", sessionid);
			context.putSessionValue("wishlists", wishLists);
			context.putPageValue("action", "doMerge");
			return;
		} else {
			throw new OpenEditException("This page has been called incorrectly!");
		}
	} 
}
boolean result = false;

logs = new ScriptLogger();
logs.startCapture();

try {
	MergeWishLists mergeWishLists = new MergeWishLists();
	mergeWishLists.setLog(logs);
	mergeWishLists.setContext(context);
	mergeWishLists.setModuleManager(moduleManager);
	mergeWishLists.setPageManager(pageManager);

	mergeWishLists.init();
}
finally {
	logs.stopCapture();
}
