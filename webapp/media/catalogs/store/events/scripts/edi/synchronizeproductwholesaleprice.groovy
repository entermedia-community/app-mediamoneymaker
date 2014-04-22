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

import groovy.util.slurpersupport.GPathResult


public void processProducts() {
	
	log.info("---- START Synchronize Product Wholesale Price ----");
	
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
		String wholesale = product.get("rogersprice");//rogersprice IS wholesale
		if (wholesale == null || wholesale.isEmpty() || !isNumber(wholesale)){
			return;//continue
		}
		Money wholesalePrice = new Money(wholesale);
		Money retailPrice = wholesalePrice.multiply("1.10");
		List<InventoryItem> inventoryItems = product.getInventoryItems();
		if (inventoryItems.size() != 1){
			log.info("<span style='color:red;'>Warning: product # $product.id does not have ONE inventory item (${inventoryItems.size()}), skipping</span>");
			return; //continue
		}
		for(InventoryItem inventoryItem:inventoryItems){
			PriceSupport support = inventoryItem.getPriceSupport();
			if (support == null){
				Price price = new Price(retailPrice);
				price.setWholesalePrice(wholesalePrice);
				support = new PriceSupport();
				support.addTierPrice(1, price);
				inventoryItem.setPriceSupport(support);
			}
			if(support.getTiers().size() == 0){
				log.info("NO TIERS ON PRODUCT ${product.id}");
			}
			for (Iterator<?> itr = support.getTiers().iterator(); itr.hasNext();)
			{
				PriceTier tier = (PriceTier)itr.next();
				Price price = tier.getPrice();
				price.setWholesalePrice(wholesalePrice);
				price.setRetailPrice(retailPrice);
				log.info("Products: updated ${product.getId()}(${product}) price: ${price}");
			}
		}
		productstosave.add(product);
		if (productstosave.size() == 100){
			store.saveProducts(productstosave);
			productstosave.clear();
			log.info("Saved 100 products");
		}
		
	}
	store.saveProducts(productstosave);
	log.info("Saved ${productstosave.size()} products");
	store.clearProducts();//forces products to be loaded from disc
	log.info("---- END Synchronize Product Wholesale Price ----");
}

public boolean isNumber(String str){
	try{
		Double.parseDouble(str);
	}catch(Exception e){
		return false;
	}
}

processProducts();