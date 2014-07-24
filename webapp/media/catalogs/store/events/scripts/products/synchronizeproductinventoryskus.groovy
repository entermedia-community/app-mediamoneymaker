package products

import java.text.SimpleDateFormat
import java.util.Iterator;

import org.dom4j.Element
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.util.DateStorageUtil
import org.openedit.money.Money
import org.openedit.store.CartItem
import org.openedit.store.Price;
import org.openedit.store.PriceSupport
import org.openedit.store.PriceTier;
import org.openedit.store.Store
import org.openedit.store.Product
import org.openedit.store.InventoryItem
import org.openedit.store.orders.Order
import org.openedit.store.util.MediaUtilities
import org.openedit.store.orders.OrderArchive
import org.openedit.store.orders.OrderId
import org.openedit.store.orders.Shipment
import org.openedit.store.orders.ShipmentEntry
import org.openedit.store.orders.Refund
import org.openedit.store.orders.RefundItem
import org.openedit.store.orders.RefundState

import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.ListHitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.Page
import com.openedit.page.manage.PageManager
import com.openedit.util.XmlUtil

import groovy.util.slurpersupport.GPathResult


public void init() {
	
	log.info("---- START Synchronizing Product Inventory SKUs ----");
	
	WebPageRequest inReq = context;
	MediaArchive archive = inReq.getPageValue("mediaarchive");
	Store store = inReq.getPageValue("store");
	
	ArrayList<String> list = new ArrayList<String>();
	
	SearcherManager manager = archive.getSearcherManager();
	Searcher productsearcher = store.getProductSearcher();
	HitTracker hits = productsearcher.getAllHits();
	List<Product> productstosave = new ArrayList<Product>();
	hits.each{
		Product product = store.getProduct(it.id);
		if (product.isCoupon()){
			return;
		}
		List<InventoryItem> items = product.getInventoryItems();
		if (items.size() == 1){
			InventoryItem item = items.get(0);
			if (product.getId() == item.getSku()){
				String sku = product.get("manufacturersku");
				if (sku != product.getId()){
					item.setSku(sku);
					productstosave.add(product);
				}
			}
		}
	}
	Iterator<Product> itr = productstosave.iterator();
	List<Product> cache = new ArrayList<Product>();
	while(itr.hasNext()){
		Product product = itr.next();
		cache.add(product);
		if (cache.size() == 100){
			productsearcher.saveAllData(cache, null);
			cache.clear();
		}
	}
	if (cache.isEmpty() == false){
		productsearcher.saveAllData(cache, null);
		cache.clear();
	}
	productstosave.clear();
	log.info("---- END Synchronize Product Inventory SKUs ----");
}

init();