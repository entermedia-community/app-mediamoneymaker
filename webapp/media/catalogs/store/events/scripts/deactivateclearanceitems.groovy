import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.store.InventoryItem
import org.openedit.store.Product
import org.openedit.store.Store



public void init(){
	log.info("-------- START Deactivating Clearance Items -----");
	
	Store store = context.getPageValue("store");
	MediaArchive archive = context.getPageValue("mediaarchive");
	ArrayList<Product> list = new ArrayList<Product>();
	SearcherManager manager = archive.getSearcherManager();
	Searcher productsearcher = archive.getSearcher("product");
	SearchQuery query = productsearcher.createSearchQuery();
	query.addMatches("clearancecentre", "true");
//	query.addMatches("active", "true");
	HitTracker hits = productsearcher.search(query);
	hits.each{
		Product product = store.getProduct(it.id);
		List inventoryItems = product.getInventoryItems();
		boolean deactivateProduct = false;
		if (inventoryItems.size() > 0){
			inventoryItems.each{
				InventoryItem item = (InventoryItem) it;
				int quantity = item.getQuantityInStock();
				deactivateProduct = (quantity < 1);//if more than one inventory item, will deactive only if all quantities are 0
			}
		}
		if (deactivateProduct){
			log.info("${product.getId()} - ${product.getName()} - deactiving (${product.active})");
			product.setProperty("active", "false");
			list.add(product);
		}
	}
	if (!list.isEmpty()){
		log.info("Saving products (${list.size()})");
		store.saveProducts(list);
	}
	log.info("-------- END Deactivating Clearance Items -----");
}


init();