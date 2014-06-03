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
	
	log.info("---- START Checking Clearance Prices ----");
	
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
		if (Boolean.parseBoolean("${product.clearancecentre}")==false){
			return;
		}
		Money clearanceprice = new Money(toDouble("${product.clearanceprice}",0.00));
		if (clearanceprice.isZero() || clearanceprice.isNegative()) {
			return;
		}
		double pricefactor = getPriceFactor(archive,product);
		clearanceprice = clearanceprice.multiply(pricefactor);
		//check product price support
		PriceSupport productsupport = product.getPriceSupport();
		if(productsupport == null){
			log.error("Product Price Support is null: ${product.getId()} - ${product}");
		} else {
			if (productsupport.getTiers() !=null){
				for (Iterator<?> itr = productsupport.getTiers().iterator(); itr.hasNext();)
				{
					PriceTier tier = (PriceTier)itr.next();
					Price price = tier.getPrice();
					price.setSalePrice(clearanceprice);
					list.add("${product.getId()} - ${product} - sale price ${price.getSalePrice()}");
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
				price.setSalePrice(clearanceprice);
				list.add("${product.getId()} - ${product} - sale price ${price.getSalePrice()}");
			}
		}
		productstosave.add(product);
		if (productstosave.size() == 1000){
			store.saveProducts(productstosave);
			productstosave.clear();
		}
	}
	
	if (!productstosave.isEmpty()){
		store.saveProducts(productstosave);
		productstosave.clear();
	}
	
	if (list.isEmpty()){
		log.info("All Clearance prices are accounted for");
	} else {
		log.info("-----------------------------------------------");
		for(String element:list){
			log.error(element);
		}
		log.info("-----------------------------------------------");
	}
	log.info("---- END Checking Clearance Prices ----");
}

public double getPriceFactor(MediaArchive archive, Product product)
{
	//get price factor for authorized/non-authorized products
	String distributorid = product.get("distributor");
	if (distributorid)
	{
		Searcher distributorsearcher = archive.getSearcher("distributor");
		Data distributordata = distributorsearcher.searchById(distributorid);
		String auth = distributordata.get("rogersauthorizedpricefactor");
		String nonauth = distributordata.get("rogersnonauthorizedpricefactor");
		double authfact = toDouble(auth,1.1);//default 10%
		double nonauthfact = toDouble(nonauth,1.02);// default 2%
		if (authfact < 1.0) authfact +=1.0;
		if (nonauthfact < 1.0) nonauthfact +=1.0;
		log.info("retail price factor for $distributordata (${distributorid}): authorized $authfact, non-authorized $nonauthfact");
		
		String isauth = product.get("rogersauthorized");
		if ("true".equalsIgnoreCase(isauth))
		{
			return authfact;
		}
		return nonauthfact;
	}
	return 1.1;//original default if all else fails
}

public double toDouble(String str, double inDefault)
{
	double out = inDefault;
	if (str)
	{
		try
		{
			out = Double.parseDouble(str);
		}
		catch (Exception e){}
	}
	return out;
}

init();