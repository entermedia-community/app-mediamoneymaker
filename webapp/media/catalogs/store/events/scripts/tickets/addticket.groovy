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
				ticketData.setProperty("notes", "Request of stock for: " + product.getName() + " (" + product.getId() + ")");
				ticketSearcher.saveData(ticketData, inReq.getUser());
				ticketSearcher.reIndexAll();
				
				inReq.putPageValue("product.value", product.getId());
				inReq.putPageValue("data", ticketData);
				String email = inReq.getUser().getEmail();
				outMsg = "<p>A request for this product has been created.<br>Product: " + product.getName() + " (" + product.getId() + ")</p>";
				if (email != null && email.length() > 0) {
					inReq.putPageValue("notifyme", "yes");
				} else {
					inReq.putPageValue("notifyme", "no");
					outMsg += "<p>An email notification cannot be setup because you do not have a valid email address set in your profile.</p>";
					outMsg += "<p>To have email notifications setup in the future, ";
					outMsg += "please go to the <a href=\"/ecommerce/views/myaccount/index.html\">My Account section</a> and update your profile.";
				}
				inReq.putPageValue("message", outMsg);
			
			} else {
				outMsg = "<p>You have already put in a request for this product.<br>Product: " + product.getName() + " (" + product.getId() + ")";
				inReq.putPageValue("message", outMsg);
				inReq.putPageValue("notifyme", "no");
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
