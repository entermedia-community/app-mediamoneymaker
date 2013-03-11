package products;

import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.event.WebEvent
import org.openedit.money.Money
import org.openedit.store.InventoryItem
import org.openedit.store.Price
import org.openedit.store.PriceSupport
import org.openedit.store.Product
import org.openedit.store.Store

import com.openedit.OpenEditException

public void doProcess() {
	log.info("Verifying product");
	WebEvent webevent = context.getPageValue("webevent");
	if(webevent == null){
		return;
	}
	MediaArchive archive = context.getPageValue("mediaarchive");
   
	String dataid = webevent.get('dataid');
	String applicationid = webevent.get('applicationid');
	
	Store store = context.getPageValue("store");
	Product product = store.getProduct(dataid);
	product.clearItems();

	Searcher productsearcher = store.getProductSearcher();

	InventoryItem inventoryItem = new InventoryItem(product.get("manufacturersku"));
	Money money = new Money(product.getProperty("rogersprice"));
	money = money.multiply(1.1);
	Price price = new Price(money);
	
	if(product.clearancecentre == "true"){
		if(!product.clearanceprice){
			throw new OpenEditException("Clearance price wasn't set but product is in clearance section!");
		}
		Money clearanceprice = new Money(product.get("clearanceprice"));
		clearanceprice = clearanceprice.multiply(1.1);
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

doProcess();
