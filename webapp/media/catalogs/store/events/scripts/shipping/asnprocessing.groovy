package shipping

import com.openedit.users.User
import com.openedit.util.PathProcessor;
import groovy.util.slurpersupport.GPathResult

import java.text.SimpleDateFormat
import java.util.List;

import org.entermedia.email.PostMail
import org.entermedia.email.TemplateWebEmail
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.repository.ContentItem
import org.openedit.store.CartItem
import org.openedit.store.Product
import org.openedit.store.Store
import org.openedit.store.customer.Customer
import org.openedit.store.orders.Order
import org.openedit.store.orders.Shipment
import org.openedit.store.orders.ShipmentEntry
import org.openedit.store.util.MediaUtilities
import org.openedit.util.DateStorageUtil

import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.Page
import com.openedit.page.manage.PageManager


public void init(){
	log.info("Starting ASN processing");
	MediaArchive archive = context.getPageValue("mediaarchive");
	Store store = context.getPageValue("store");
	//check for new asn processing entries
//	Date date = new Date();
//	date.setTime(System.currentTimeMillis() - (48*60*60*1000));//2 days ago
	SearcherManager sm = archive.getSearcherManager();
	String catalogid = archive.getCatalogId();
	Searcher searcher = sm.getSearcher(catalogid, "asn");
//	SearchQuery query = searcher.createSearchQuery();
//	query.addAfter("processdate", date);
//	HitTracker hits = searcher.search(query);
	HitTracker hits = searcher.getAllHits();
	if (!hits.isEmpty()){
		context.putPageValue("hits",hits);
	}
	String send = context.findValue("sendemail");
	if ("true".equalsIgnoreCase(send)){
		
	}
}

init();