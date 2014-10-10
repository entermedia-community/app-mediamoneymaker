import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.store.Store

import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery


public void init() {
	
	log.info("---- START Searching for Products ----");
	
	WebPageRequest inReq = context;
	MediaArchive archive = inReq.getPageValue("mediaarchive");
	Store store = inReq.getPageValue("store");
	
	Searcher searcher = store.getProductSearcher();
	SearchQuery query = searcher.createSearchQuery();
	
	String id = inReq.getRequestParameter("id.value");
	String sku = inReq.getRequestParameter("rogerssku.value");
	String upc = inReq.getRequestParameter("upc.value");
	
	int terms = 0;
	if (id) {
		query.addContains("id",id);
		terms += 1;
	}
	if (sku){
		query.addContains("rogerssku",sku);
		terms += 1;
	}
	if (upc){
		query.addContains("upc",upc);
		terms += 1;
	}
	if (terms == 0){
		log.info("no terms, aborting");
		return;
	}
	if (terms > 1){
		query.setAndTogether(false);
	}
	String distributorid = inReq.findValue("distributorid");
	if (distributorid){
		SearchQuery childquery = searcher.createSearchQuery();
		childquery.addNot("active", "false");
		childquery.addMatches("distributor",distributorid);
		query.addChildQuery(childquery);
	}
	int hitsperpage = toInt(inReq.findValue("hitsperpage"),9);
	HitTracker hits = searcher.search(query);
	hits.setHitsPerPage(hitsperpage);
	String hitsname = inReq.findValue("hitsname");
	if (!hitsname){
		hitsname = "hits";
	}
	inReq.putPageValue(hitsname, hits);
	log.info("---- END Searching for Products ----");
}

public int toInt(String inValue, int defaultValue){
	if (inValue){
		try{
			return Integer.parseInt(inValue);
		}catch(Exception e){}
	}
	return defaultValue;
}

init();