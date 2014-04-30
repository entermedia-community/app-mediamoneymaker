package edi

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

import org.openedit.util.DateStorageUtil;

import groovy.util.slurpersupport.GPathResult



public void processProducts() {	
	log.info("---- START Updating Product Created On ----");
	
	WebPageRequest inReq = context;
	MediaArchive archive = inReq.getPageValue("mediaarchive");
	Store store = inReq.getPageValue("store");
	String catalogID = archive.getCatalogId();
	
	SearcherManager manager = archive.getSearcherManager();
	Searcher productsearcher = archive.getSearcher("product");
	HitTracker hits = productsearcher.getAllHits();
	log.info("staring processing ${hits.size()} products");
	List productstosave = new ArrayList();
	hits.each{
		Product product = store.getProduct(it.id);
		if (product.get("createdon") == null || product.get("createdon").isEmpty()){
			updateCreatedOnField(archive,product);
			productstosave.add(product);
			if (productstosave.size() == 100){
				store.saveProducts(productstosave);
				productstosave.clear();
				log.info("Saved 100 products");
			}
		}
	}
	store.saveProducts(productstosave);
	log.info("Saved ${productstosave.size()} products");
	store.clearProducts();//forces products to be loaded from disc
	log.info("---- END Updating Product Created On ----");
}

public void updateCreatedOnField(MediaArchive archive,Product product){
	String sourcepath = product.getSourcePath();
	String catalogid = archive.getCatalogId();
	String xconf = "/WEB-INF/data/$catalogid/products/${sourcepath}.xconf";
	Page page = archive.getPageManager().getPage(xconf);
	if (page.exists()){
		String absolutepath = page.getContentItem().getAbsolutePath();
		File file = new File(absolutepath);
		long lastmods = file.lastModified();//created on not logged on linux
		Date date = new Date();
		date.setTime(lastmods);
		String createdon = DateStorageUtil.getStorageUtil().formatForStorage(date);
		product.setProperty("createdon",createdon);
	} else {
		log.info("<span style='color:red;'>Unable to find $xconf</span>");
	}
}

processProducts();