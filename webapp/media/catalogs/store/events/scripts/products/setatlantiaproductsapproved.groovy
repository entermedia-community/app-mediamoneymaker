package products;

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.event.WebEvent
import org.openedit.money.Money
import org.openedit.store.InventoryItem
import org.openedit.store.Price
import org.openedit.store.PriceSupport
import org.openedit.store.Product
import org.openedit.store.Store
import org.openedit.store.search.ProductSearcher

import com.openedit.OpenEditException
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery

public void doProcess() {
	log.info("Setting products as active");

	MediaArchive archive = context.getPageValue("mediaarchive");
	Store store = context.getPageValue("store");
	
	Searcher distributorsearcher = archive.getSearcherManager().getSearcher(archive.getCatalogId(), "distributor");
	log.info("Distributor Searcher ${distributorsearcher}");
	Data distributor = (Data) distributorsearcher.searchByField("name", "Atlantia");
	log.info("Distributor Data ${distributor}");
	String distributorid = distributor.getId();
	log.info("Distributor id ${distributorid}");
	
	List productsToSave = new ArrayList();
	ProductSearcher searcher = store.getProductSearcher();
	SearchQuery query = searcher.createSearchQuery();
	query.append("distributor",distributorid);
	HitTracker hits = searcher.search(query);
	hits.each{
		Data d = it;
		Product product = store.getProduct(d.getId());
		product.setProperty("approved", "true");
		productsToSave.add(product);
		if (productsToSave.size() == 100){
			store.saveProducts(productsToSave);
			productsToSave.clear();
			log.info("Saved 100 products");
		}
	}
	searcher.saveAllData(productsToSave, null);
	log.info("Saved ${productsToSave.size()} products");
//	searcher.clearIndex();//forces them to be loaded from disc
	log.info("Finished setting products to active");
}

doProcess();
