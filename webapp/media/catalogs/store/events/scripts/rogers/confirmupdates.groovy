import org.openedit.entermedia.MediaArchive
import org.openedit.event.WebEvent
import org.openedit.event.WebEventHandler
import org.openedit.store.Store

import com.openedit.WebPageRequest


public void init(){
	log.info("Confirm Corporate Orders");
	
	WebPageRequest req = context;
	MediaArchive archive = req.getPageValue("mediaarchive");
	Store store = req.getPageValue("store");
	
	String uuid = req.getRequestParameter("uuid");
	if (uuid){
		req.putPageValue("uuid",uuid);
		WebEvent evt = new WebEvent();
		evt.setSearchType("productupdates");
		evt.setCatalogId(archive.getCatalogId());
		evt.setProperty("applicationid", req.findValue("applicationid"));
		evt.setOperation("product/confirmas400updates");
		evt.setProperty("uuid",uuid);
		WebEventHandler a = archive.getMediaEventHandler();
		archive.getMediaEventHandler().eventFired(evt);
	}
}

init();