
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.util.CSVReader
import org.openedit.store.Product
import org.openedit.store.Store
import org.openedit.store.orders.OrderSearcher;

import com.openedit.page.Page

import com.openedit.util.FileUtils

import org.entermedia.email.PostMail
import org.entermedia.email.TemplateWebEmail
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.store.Store
import org.openedit.store.orders.Order

import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.Page

import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit

import org.openedit.util.DateStorageUtil

import java.util.Calendar;
import java.util.GregorianCalendar;

public init(){
	log.info("Starting update of inventory date fields");
//	Date date = new Date();
//	long ms = System.currentTimeMillis() - (14 * 24 * 60 * 60 * 1000);//2 weeks ago
//	date.setTime(ms);
	//make date really old
	Calendar cal = new GregorianCalendar(2012, 0, 1);
	String df = DateStorageUtil.getStorageUtil().formatForStorage(cal.getTime());
	Store store = context.getPageValue("store");
	//Create the MediaArchive object
	MediaArchive archive = context.getPageValue("mediaarchive");
	SearcherManager searcherManager = archive.getSearcherManager();
	String catalogid = archive.getCatalogId();
	Searcher productsearcher = searcherManager.getSearcher(catalogid, "product");
	HitTracker hits = productsearcher.getAllHits();
	hits.each{
		Data data = it;
		if (data.get("inventoryupdated") == null){
			Product product = store.getProduct(data.getId());
			product.setProperty("inventoryupdated", df);
			store.saveProduct(product);
			log.info("Updated inventory date to ${df} of ${product} (${data.getId()})");
		}
	}
	log.info("Finished updating inventory dates");
}

init();