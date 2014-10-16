import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.event.WebEvent
import org.openedit.store.Store
import org.openedit.store.orders.Order

import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery


public void init() {
	log.info("---- Updating PO number ----");
	
	WebPageRequest inReq = context;
	MediaArchive archive = inReq.getPageValue("mediaarchive");
	Store store = inReq.getPageValue("store");
	
	Searcher searcher = store.getOrderSearcher();
	SearchQuery query = searcher.createSearchQuery();
	
	String batchid = inReq.getRequestParameter("batchid");
	String as400po = inReq.getRequestParameter("as400po.value");

	if(!batchid || !as400po )
	{
		WebEvent webevent = inReq.getPageValue("webevent");
		if (webevent)
		{
			batchid = webevent.get("batchid");
			as400po = webevent.get("as400po");
		}
	}
	if (batchid && as400po)
	{
		List<Order> list = new ArrayList<Order>();
		query.addMatches("batchid",batchid);
		HitTracker hits = searcher.search(query);
		hits.each
		{
			Order order = searcher.searchById(it.id);//load the order
			if (order.get("rogersponumber") == as400po){//omit if needed
				return;
			}
			order.setProperty("rogersponumber",as400po);
			log.info("Updating rogerspo to $as400po for $order")
			list.add(order);//add order to list
			if(list.size() == 100)// if size equals some max size, then save and clear list
			{
				searcher.saveAllData(list,null);
				list.clear();
			}
		}
		//if the list still isn't empty then save all
		if (!list.isEmpty()){
			searcher.saveAllData(list,null);
		}
	}
	else
	{
		log.info("batchid or ponumber not provided, aborting");
		return;
	}
	log.info("---- Finished updating PO number ----");
}

init();