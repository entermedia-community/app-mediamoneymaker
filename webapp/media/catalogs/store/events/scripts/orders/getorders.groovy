package orders

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.store.orders.Order

import com.openedit.OpenEditException
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker

public class GetOrders  extends EnterMediaObject {
	public init() {
		log.info("PROCESS: START Orders.RogersImport");
		
		MediaArchive archive = context.getPageValue("mediaarchive");
		String catalogid = getMediaArchive().getCatalogId();
		SearcherManager manager = archive.getSearcherManager();
		Searcher ordersearcher = manager.getSearcher(archive.getCatalogId(), "storeOrder");
		Searcher as400searcher = manager.getSearcher(archive.getCatalogId(), "as400");
		
		String as400id = context.getRequestParameter("id");
		Data as400data = as400searcher.searchById(as400id);
		String batchID = as400data.get("batchid");
		
		HitTracker orderList = ordersearcher.fieldSearch("batchid", batchID);
		if (orderList == null) {
			return;
		} else {
			log.info("OrderList Size: " + orderList.size());
		}
		ArrayList<String> orders = new ArrayList<String>();
		for (Iterator orderIterator = orderList.iterator(); orderIterator.hasNext();) {
			Data currentOrder = orderIterator.next();
			Order order = ordersearcher.searchById(currentOrder.getId());
			if (order == null) {
				throw new OpenEditException("Invalid Order");
			}
			log.info("DATA: Order found: " + order.getId());
			orders.add(order.getId());
		}
		context.putPageValue("orderlist", orders);
	}
}
logs = new ScriptLogger();
logs.startCapture();

try {

	GetOrders getOrder = new GetOrders();
	getOrder.setLog(logs);
	getOrder.setContext(context);
	getOrder.setModuleManager(moduleManager);
	getOrder.setPageManager(pageManager);

	getOrder.init();
	log.info("PROCESS: END Orders.RogersImport");
}
finally {
	logs.stopCapture();
}
