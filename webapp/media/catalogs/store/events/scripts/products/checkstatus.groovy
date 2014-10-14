import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.store.Store

import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery


public void init() {
	log.info("---- Checking Product Updates ----");
	WebPageRequest inReq = context;
	MediaArchive archive = inReq.getPageValue("mediaarchive");
	Store store = inReq.getPageValue("store");
	Searcher searcher = archive.getSearcher("productupdates");
	SearchQuery query = searcher.createSearchQuery();
	String uuid = inReq.getRequestParameter("uuid");
	if (!uuid){
		//not there so add log and return
		log.info("Unable to find UUID, aborting");
		inReq.putPageValue("results","Error");
		return;	
	}
	query.addMatches("uuid",uuid);//needs to be an exact match
	HitTracker hits = searcher.search(query);
	if (hits.size() == 0){
		inReq.putPageValue("results","Complete");
	} else {
		inReq.putPageValue("results","Not Complete");
	}
	log.info("---- DONE Checking Status ----");
}

init();