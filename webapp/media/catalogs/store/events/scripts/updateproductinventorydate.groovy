
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

public init(){
	log.info("Starting update of inventory date fields");
	Date date = new Date();
	long ms = System.currentTimeMillis() - (14 * 24 * 60 * 60 * 1000);//2 weeks ago
	date.setTime(ms);
	
	System.out.println("Inventory Date: ${date}");
	
	String df = DateStorageUtil.getStorageUtil().formatForStorage(date);
	
	System.out.println("Formatted data ${df}");
	
	Store store = context.getPageValue("store");
	System.out.println("Store ${store}");
	
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
			//store.saveProduct(product);
			productsearcher.saveData(product, null);
//			
//			Product product = productsearcher.searchById(data.getId());
//			product.setProperty("inventoryupdated", df);
//			productsearcher.saveData(product, null);
			log.info("&&&& Updated the inventory date to ${df} for of ${product} PRODUCT ID ${data.getId()}");
		}
	}
	log.info("Finished updating inventory dates");
}

init();