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
	
	log.info("---- START Checking Wholesale Prices ----");
	
	WebPageRequest inReq = context;
	MediaArchive archive = inReq.getPageValue("mediaarchive");
	Store store = inReq.getPageValue("store");
	
	ArrayList<String> list = new ArrayList<String>();
	
	SearcherManager manager = archive.getSearcherManager();
	Searcher productsearcher = archive.getSearcher("product");
	HitTracker hits = productsearcher.getAllHits();
	List productstosave = new ArrayList();
	hits.each{
		Product product = store.getProduct(it.id);
		//check product price support
		PriceSupport productsupport = product.getPriceSupport();
		if(productsupport != null){
			for (Iterator<?> itr = productsupport.getTiers().iterator(); itr.hasNext();)
			{
				PriceTier tier = (PriceTier)itr.next();
				Price price = tier.getPrice();
				Money wholesaleprice = price.getWholesalePrice();
				if (wholesaleprice==null || wholesaleprice.isZero() || wholesaleprice.isNegative()){
					list.add("Product Price Support: ${product.getId()} - ${product} ${wholesaleprice}");
				}
			}
		}
		List<InventoryItem> inventoryItems = product.getInventoryItems();
		for(InventoryItem inventoryItem:inventoryItems){
			PriceSupport support = inventoryItem.getPriceSupport();
			if (support == null){
				log.error("problem with product ${product.id}: inventory item has no price support");
				continue;//problem with one of the inventory item
			}
			if(support.getTiers().size() == 0){
				log.info("NO TIERS ON PRODUCT ${product.id}");
			}
			for (Iterator<?> itr = support.getTiers().iterator(); itr.hasNext();)
			{
				PriceTier tier = (PriceTier)itr.next();
				Price price = tier.getPrice();
				Money wholesaleprice = price.getWholesalePrice();
				if (wholesaleprice==null || wholesaleprice.isZero() || wholesaleprice.isNegative()){
					list.add("Product Inventory Price Support: ${product.getId()} - ${product} ${wholesaleprice}");
				}
			}
		}
	}
	List<OrderId> ids = store.getOrderArchive().listAllOrderIds(store);
	for(OrderId id:ids){
		Order order = store.getOrderSearcher().searchById(id.getOrderId());
		List<CartItem> cartItems = order.getItems();
		for(CartItem item:cartItems){
			Money wholesaleprice = item.getWholesalePrice();
			if (wholesaleprice==null || wholesaleprice.isZero() || wholesaleprice.isNegative()){
				list.add("Order ${order.getId()} CartItem: ${item.getId()} ${wholesaleprice}");
			}
		}
	}
	if (list.isEmpty()){
		log.info("All wholesale prices are accounted for");
	} else {
		log.info("-----------------------------------------------");
		for(String element:list){
			log.error(element);
		}
		log.info("-----------------------------------------------");
	}
	log.info("---- END Checking Wholesale Prices ----");
}

init();