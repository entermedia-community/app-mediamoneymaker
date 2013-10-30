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
		log.info("${p}: display designation id = ${display}");
		if(display == null){
			return;
		}
		if("1".equals(display)){
			p.setProperty("group", "rogers");
		} else if("2".equals(display)){
			p.setProperty("group", "fido");
		} else if("3".equals(display)){
			p.setValues("group", new ArrayList<String>(["fido","rogers"]));
		} else {
			return;
		}
		tosave.add(p);
	}
	searcher.saveAllData(tosave, null);
}
init();
