package tickets

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.store.Product
import org.openedit.store.util.MediaUtilities
import org.openedit.util.DateStorageUtil

import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.GroovyScriptRunner
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery

public class AddTicket extends EnterMediaObject {

	public void doAdd() {
		
		Log log = LogFactory.getLog(GroovyScriptRunner.class);
		
		WebPageRequest inReq = context;

		MediaArchive archive = context.getPageValue("mediaarchive");
		SearcherManager manager = archive.getSearcherManager();
		String catalogid = archive.getCatalogId();
		
		MediaUtilities media = new MediaUtilities();
		media.setContext(context);
		
		Searcher productSearcher = media.getProductSearcher();
		Searcher ticketSearcher = media.getTicketSearcher();

		String productValue = inReq.getRequestParameter("product.value");
		String ticketType = inReq.getRequestParameter("tickettype.value");
		String outMsg = "";
		
		Product product = productSearcher.searchById(productValue);
		if (product != null) {
			
			SearchQuery query = ticketSearcher.createSearchQuery();
			query.addMatches("product", product.getId());
			query.addMatches("tickettype", ticketType);
			query.addMatches("owner", inReq.getUser().getId());
			HitTracker ticketHits = ticketSearcher.search(query);
			
			if (ticketHits == null || ticketHits.size() == 0) {
				//Create new ticket
				Data ticketData = ticketSearcher.createNewData();
				ticketData.id = ticketSearcher.nextId();
				ticketData.sourcePath = product.getId();
				ticketData.setProperty("product", productValue);
				ticketData.setProperty("tickettype", ticketType);
				ticketData.setProperty("ticketstatus", "open");
				Date now = new Date();
				ticketData.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(now));
				ticketData.setProperty("owner", inReq.getUser().getId());
				ticketData.setProperty("notes", "Request of stock for: " + product.getName() + " (" + product.get("rogerssku") + ")");
				ticketSearcher.saveData(ticketData, inReq.getUser());
				ticketSearcher.reIndexAll();
				
				inReq.putPageValue("product.value", product.getId());
				inReq.putPageValue("data", ticketData);
				if (product.get("rogerssku") != "") {
					inReq.putPageValue("rogerssku", product.get("rogerssku"));
				}
				String email = inReq.getUser().getEmail();
				if (email != null && email.length() > 0) {
					inReq.putPageValue("notifyme", "yes");
				} else {
					inReq.putPageValue("notifyme", "no");
					inReq.putPageValue("reason", "noemail");
				}
			
			} else {
				inReq.putPageValue("notifyme", "no");
				inReq.putPageValue("reason", "duplicate");
			}			
		}
	}
	
}
logs = new ScriptLogger();
logs.startCapture();

try {
	AddTicket addTicket = new AddTicket();
	addTicket.setLog(logs);
	addTicket.setContext(context);
	addTicket.setModuleManager(moduleManager);
	addTicket.setPageManager(pageManager);

	addTicket.doAdd();
}
finally {
	logs.stopCapture();
}
