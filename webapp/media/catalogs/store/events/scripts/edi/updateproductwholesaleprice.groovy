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
	
	int numberUpdated = 0;
	int ordersUpdated = 0;
	SearcherManager manager = archive.getSearcherManager();
	Searcher productsearcher = archive.getSearcher("product");
	Searcher ordersearcher = archive.getSearcher("storeOrder");
	HitTracker hits = productsearcher.getAllHits();
	hits.each{
		Product product = (Product) productsearcher.searchById(it.id);
		//synchronize the wholesale prices for each order
		ordersUpdated += synchronizeWholesalePrices(manager,ordersearcher,product.getId());
		List<InventoryItem> inventoryItems = product.getInventoryItems();
		boolean updateProduct = false;
		for(InventoryItem inventoryItem:inventoryItems){
			PriceSupport support = inventoryItem.getPriceSupport();
			for (Iterator<?> itr = support.getTiers().iterator(); itr.hasNext();)
			{
				PriceTier tier = (PriceTier)itr.next();
				Price price = tier.getPrice();
				Money retail = price.getRetailPrice();
				Money wholesale = price.getWholesalePrice();
				Money calcwholesale = retail.divide("1.10");
				if (wholesale==null || !calcwholesale.equals(wholesale)){
					price.setWholesalePrice(calcwholesale);
					updateProduct = true;
					log.info("updating ${product.id} wholesale price ${calcwholesale}");
				}
			}
		}
		if (updateProduct){
			numberUpdated ++;
			productsearcher.saveData(product, null);
		}
	}
	log.info("Number of products updated ${numberUpdated}, Number of orders updated ${ordersUpdated} (may include duplicates)");
	log.info("---- END Update Product Wholesale Price ----");
}

public int synchronizeWholesalePrices(SearcherManager searchermanager, Searcher ordersearcher, String productid){
	int numberUpdated = 0;
	HitTracker hits = ordersearcher.getAllHits();
	hits.each{
		Order order = null;
		try{
			order = (Order) ordersearcher.searchById(it.id);
		}catch (Exception e){
//			System.err.println("Exception caught searching for ${it.id}, ${e.getMessage()}");
		}//bug with one of the orders
		if (order == null)
			return;
		
		boolean updateOrder = false;
		List<CartItem> cartItems = order.getItems();
		for(CartItem item:cartItems){
			Product product = item.getProduct();
			if (product.getId().equals(productid)){
				updateOrder = true;
				List<InventoryItem> inventoryItems = product.getInventoryItems();
				for(InventoryItem inventoryItem:inventoryItems){
					PriceSupport support = inventoryItem.getPriceSupport();
					for (Iterator<?> itr = support.getTiers().iterator(); itr.hasNext();)
					{
						PriceTier tier = (PriceTier)itr.next();
						Price price = tier.getPrice();
						Money retail = price.getRetailPrice();
						Money wholesale = price.getWholesalePrice();
						Money calcwholesale = retail.divide("1.10");
						if (wholesale==null || !calcwholesale.equals(wholesale)){
							price.setWholesalePrice(calcwholesale);
						}
					}
				}
				break;
			}
		}
		if (updateOrder){
			log.info("updating ${order.getId()} with ${productid}");
			ordersearcher.saveData(order, null);
			numberUpdated++;
		}
	}
	return numberUpdated;
}

processProducts();