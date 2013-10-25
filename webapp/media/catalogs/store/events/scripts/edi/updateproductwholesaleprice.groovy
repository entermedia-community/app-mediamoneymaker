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
	String catalogID = archive.getCatalogId();
	
	SearcherManager manager = archive.getSearcherManager();
	Searcher productsearcher = archive.getSearcher("product");
	Searcher ordersearcher = archive.getSearcher("storeOrder");
	HitTracker hits = productsearcher.getAllHits();
	log.info("staring processing ${hits.size()} products");
	hits.each{
		Product product = (Product) productsearcher.searchById(it.id);
		List<InventoryItem> inventoryItems = product.getInventoryItems();
		log.info("Item size: ${inventoryItems.size()}");
		for(InventoryItem inventoryItem:inventoryItems){
			PriceSupport support = inventoryItem.getPriceSupport();
			if (support == null){
				log.error("problem with product ${product.id}: inventory item has no price support");
				continue;//problem with one of the inventory item
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
		productsearcher.saveData(product, null);
	}
	updateWholesalePricesForAllOrders(manager,ordersearcher);
	log.info("---- END Update Product Wholesale Price ----");
}

public void updateWholesalePricesForAllOrders(SearcherManager searchermanager, Searcher ordersearcher){
	HitTracker hits = ordersearcher.getAllHits();
	hits.each{
		Order order = null;
		try{
			order = (Order) ordersearcher.searchById(it.id);
		}catch (Exception e){
			System.err.println("Exception caught searching for ${it.id}, ${e.getMessage()}");
			log.error("Exception caught searching for ${it.id}, ${e.getMessage()}");
		}//bug with one of the orders
		if (order == null){
			return;//continue to next
		}
		List<CartItem> cartItems = order.getItems();
		for(CartItem item:cartItems){
			if(item.getWholesalePrice() == null || item.getWholesalePrice().isZero()){
				Money retail = item.getYourPrice();
				Money calcwholesale = retail.divide("1.10");
				item.setWholesalePrice(calcwholesale);
				log.info("Orders: updated ${order.getId()} wholesale price ${item.getWholesalePrice()}");
			}
		}
		ordersearcher.saveData(order, null);
	}
}

processProducts();