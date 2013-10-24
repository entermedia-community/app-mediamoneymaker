package products;

import org.apache.commons.lang.StringEscapeUtils;
import org.openedit.store.Product
import org.openedit.store.Store
import org.openedit.store.search.ProductSearcher

import com.openedit.hittracker.HitTracker
import com.openedit.util.PathUtilities;
import com.openedit.util.URLUtilities;


public void init(){

	Store store = context.getPageValue("store");
	ProductSearcher searcher = store.getProductSearcher();
	HitTracker products = searcher.getAllHits();
	List tosave = new ArrayList();
	products.each{
		Product p = store.getProduct(it.id);
		String display = p.getProperty('displaydesignationid');
		log.info("display ${display}");
		if(display != null){
			if("1".equals(display)){
				p.setProperty("groups", "rogers");
			}
			if("2".equals(display)){
				p.setProperty("groups", "fido");
			}

			if("3".equals(display)){
				p.setProperty("groups", "rogers fido");
			}
		}
	}
	searcher.saveAllData(tosave, null);
}
init();
