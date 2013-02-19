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
		String description = p.getProperty('descrip');
		if(description != null){
			description = StringEscapeUtils.unescapeXml(description);
			p.setProperty("descrip", description);
			//searcher.saveData(p, null);
			tosave.add(p);
		}
	}
	searcher.saveAllData(tosave, null);
	
	
}
init();
