package products;

import java.util.logging.Logger;

import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.event.WebEvent
import org.openedit.money.Money
import org.openedit.store.InventoryItem
import org.openedit.store.Price
import org.openedit.store.PriceSupport
import org.openedit.store.Product
import org.openedit.store.Store
import org.openedit.Data;

import com.openedit.OpenEditException

public void doProcess() {
	log.info("Verifying product");
	
	WebEvent webevent = context.getPageValue("webevent");
	if (webevent == null){
		return;
	}
	MediaArchive archive = context.getPageValue("mediaarchive");
   
	String dataid = webevent.get('dataid');
	if(dataid == null){
		dataid = context.getRequestParameter("id");
	}
	Store store = context.getPageValue("store");
	Product product = store.getProduct(dataid);
	product.clearItems();
	
	double pricefactor = getPriceFactor(archive,product);
	log.info("price factor for $product: $pricefactor");
	

	Searcher productsearcher = store.getProductSearcher();

	InventoryItem inventoryItem = new InventoryItem(product.get("manufacturersku"));
	Money wholesaleprice = new Money(product.getProperty("rogersprice"));
	Money retailprice = new Money(product.getProperty("rogersprice"));
	retailprice = retailprice.multiply(pricefactor);
	//retail price
	Price price = new Price(retailprice);
	//wholesale price
	price.setWholesalePrice(wholesaleprice);
	
	if(product.clearancecentre == "true"){
		if(!product.clearanceprice){
			throw new OpenEditException("Clearance price wasn't set but product is in clearance section!");
		}
		Money clearanceprice = new Money(product.get("clearanceprice"));
		clearanceprice = clearanceprice.multiply(pricefactor);
		price.setSalePrice(clearanceprice)
	}
	
	
	
	
	PriceSupport pricing = new PriceSupport();
	pricing.addTierPrice(1, price);
	inventoryItem.setPriceSupport(pricing);
	product.addInventoryItem(inventoryItem);

	productsearcher.saveData(product, context.getUser());
	context.putPageValue("product", product);
	log.info("Product verification complete");
	
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

doProcess();
