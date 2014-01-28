package testing

import org.openedit.Data
import org.openedit.store.util.MediaUtilities

import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger


class LookupProduct extends EnterMediaObject {
	
	public void doLookup() {

		MediaUtilities media = new MediaUtilities();
		media.setContext(context);
		
		String productid = media.getContext().getRequestParameter("productid");
		String rogersSKU = media.getContext().getRequestParameter("rogerssku");
		String upcCode = media.getContext().getRequestParameter("upccode");
		if (productid != null || rogersSKU != null || upcCode != null) {
			Data product = null
			if (productid != null) {			
				product = media.getProductSearcher().searchById(productid);
			} else {
				if (rogersSKU != null) {
					product = media.searchForProductByField("rogerssku",rogersSKU);
				} else {
					if (upcCode != null) {
						product = media.searchForProductByField("upc",upcCode);
					}
				}
			}
			if (product != null) {
				ArrayList<String> error = new ArrayList<String>();
				media.getContext().putPageValue("error", error);
				media.getContext().putPageValue("product", product);
			} else {
				ArrayList<String> error = new ArrayList<String>();
				error.add(productid);
				error.add(rogersSKU);
				error.add(upcCode);
				media.getContext().putPageValue("error", error);
				media.getContext().putPageValue("product", null);
				
				media.getContext().getPageValue(upcCode)
			}
		}
	}
}

logs = new ScriptLogger();
logs.startCapture();

try {

	LookupProduct lookup = new LookupProduct();
	lookup.setLog(logs);
	lookup.setContext(context);
	lookup.setModuleManager(moduleManager);
	lookup.setPageManager(pageManager);
	
	lookup.doLookup();
}
finally {
	logs.stopCapture();
}
