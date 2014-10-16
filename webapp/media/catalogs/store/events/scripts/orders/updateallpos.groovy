import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.event.WebEvent
import org.openedit.store.Store

import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery


public void init() {
	log.info("---- Updating All PO numbers ----");
	
	WebPageRequest inReq = context;
	MediaArchive archive = inReq.getPageValue("mediaarchive");
	Store store = inReq.getPageValue("store");
	WebEvent event = new WebEvent();
	event.setSearchType("storeOrder");
	event.setCatalogId(archive.getCatalogId());
	event.setProperty("applicationid", inReq.findValue("applicationid"));
	event.setOperation("order/updatepo");
	
	Searcher as400searcher = archive.getSearcher("as400");
	HitTracker as400hits = as400searcher.getAllHits();//get all entries from as400 table
	as400hits.each
	{
		String as400_batchid = it.batchid;
		String as400_po = it.as400po;
		if(as400_batchid && as400_po)
		{
			log.info("firing updatepo event with batchid=$as400_batchid, as400po=$as400_po");
			event.setProperty("batchid",as400_batchid);
			event.setProperty("as400po",as400_po);
			archive.getMediaEventHandler().eventFired(event);
		}
	}
}
init();