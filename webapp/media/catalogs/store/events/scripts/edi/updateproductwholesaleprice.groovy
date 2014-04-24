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
	
	log.info("---- START Update Product Wholesale Price ----");
	
	WebPageRequest inReq = context;
	MediaArchive archive = inReq.getPageValue("mediaarchive");
	Store store = inReq.getPageValue("store");
	String catalogID = archive.getCatalogId();
	
	SearcherManager manager = archive.getSearcherManager();
	Searcher productsearcher = archive.getSearcher("product");
//	SearchQuery query = productsearcher.createSearchQuery();
//	query.addMatches("id","10495");
	HitTracker hits = productsearcher.getAllHits(); //search(query) ;//getAllHits();
	log.info("staring processing ${hits.size()} products");
	List productstosave = new ArrayList();
	hits.each{
		Product product = store.getProduct(it.id);
		List<InventoryItem> inventoryItems = product.getInventoryItems();
		log.info("Item size: ${inventoryItems.size()}");
		PriceSupport productsupport = product.getPriceSupport();
		if(productsupport != null){
			for (Iterator<?> itr = productsupport.getTiers().iterator(); itr.hasNext();)
			{
				PriceTier tier = (PriceTier)itr.next();
				Price price = tier.getPrice();
				Money retail = price.getRetailPrice();
				Money calcwholesale = retail.divide("1.10");
				price.setWholesalePrice(calcwholesale);
				log.info("Products: updated ${product.getId()}(${product}) price: ${price}");
			}
		}
		for(InventoryItem inventoryItem:inventoryItems){
			PriceSupport support = inventoryItem.getPriceSupport();
			if (support == null){
				String rogersprice = product.getProperty("rogersprice");
				if (rogersprice == null || rogersprice.isEmpty()){
					log.info("Warning: Unable to update product $product price - rogersprice is not set, skipping");
					continue;
				}
				Money money = new Money(rogersprice);
				money = money.multiply(1.1);
				Price price = new Price(money);
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
				Money retail = price.getRetailPrice();
				Money calcwholesale = retail.divide("1.10");
				price.setWholesalePrice(calcwholesale);
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
	updateWholesalePrices(store);
	log.info("---- END Update Product Wholesale Price ----");
}

public void updateWholesalePrices(Store store){
	List<OrderId> ids = store.getOrderArchive().listAllOrderIds(store);
	for(OrderId id:ids){
		Order order = store.getOrderSearcher().searchById(id.getOrderId());
		if (order == null) continue;
		if (order.getItems() == null) continue;
		List<CartItem> cartItems = order.getItems();
		for(CartItem item:cartItems){
			Money retail = item.getYourPrice();
			Money wholesale = retail.divide("1.10");
			item.setWholesalePrice(wholesale);
			log.info("Orders: updated ${order.getId()} wholesale price ${item.getWholesalePrice()}");
		}
		store.saveOrder(order);
	}
}

processProducts();