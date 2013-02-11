package orders;

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.store.orders.Order
import org.openedit.store.util.MediaUtilities

import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery

public String doReset() {
		
	String strMsg = "";
	WebPageRequest inReq = context;
	
	MediaUtilities media = new MediaUtilities();
	media.setContext(context);
	
	MediaArchive archive = inReq.getPageValue("mediaarchive");
	
	// Create the Searcher Objects to read values!
	SearcherManager searcherManager = archive.getSearcherManager();
	Searcher ordersearcher = media.getOrderSearcher();
	
	SearchQuery orderQuery = ordersearcher.createSearchQuery();
	orderQuery.addExact("csvgenerated", "true");
	orderQuery.addMatches("distributor", "102");
	HitTracker hits = ordersearcher.search(orderQuery);
	if ( hits.size() > 0 ) {
		for (Iterator orders = hits.iterator(); orders.hasNext();) {
			Data order = (Data) orders.next();
			Order thisOrder = ordersearcher.searchById(order.getId());
			thisOrder.setProperty("csvgenerated", "false");
			strMsg += "Order (" + thisOrder.getId() + ") csvgenerated updated to true.<br>";
			log.info("Order (" + thisOrder.getId() + ") csvgenerated updated to true.");
			ordersearcher.saveData(thisOrder, inReq.getUser());
		}
		ordersearcher.reIndexAll();
	} else {
		log.info("No orders found.");
		strMsg = "No orders found.";
	}
	return strMsg;
}
String output = doReset();
context.putPageValue("export", output);
