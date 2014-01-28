package products

import org.openedit.Data
import org.openedit.store.Product
import org.openedit.store.Store
import org.openedit.store.util.MediaUtilities

import com.openedit.OpenEditException
import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger

public class UpdateApproved extends EnterMediaObject {
	
	public void doLookup() {

		String strMsg = "";
		MediaUtilities media = new MediaUtilities();
		media.setContext(context);
		
		Store store = null;
		try {
			store  = media.getContext().getPageValue("store");
			if (store != null) {
				log.info("Store loaded");
			} else {
				strMsg = "ERROR: Could not load store";
				throw new Exception(strMsg);
			}
		}
		catch (Exception e) {
			strMsg += "Exception thrown:\n";
			strMsg += "Local Message: " + e.getLocalizedMessage() + "\n";
			strMsg += "Stack Trace: " + e.getStackTrace().toString();;
			log.info(strMsg);
			throw new OpenEditException(strMsg);
		}
		
		String approved = media.getContext().getRequestParameter("approved");
		if (approved != null && approved != "") {
			String[] updates = media.getContext().getRequestParameters("updates");
			if (updates != null) {
				for (String productid in updates) {
					Product product = media.getProductSearcher().searchById(productid);
					product.setProperty("approved", approved);
					store.saveProduct(product);
				}
			}
		}
		HashSet<String> productIDs = new HashSet<String>();
		String inProductID = media.getContext().getRequestParameter("productid");
		if (inProductID != null) {
			productIDs = parseParameters(inProductID, productIDs);
			media.getContext().putPageValue("productid", inProductID);
		}
		String inRogersSku = media.getContext().getRequestParameter("rogerssku");
		if (inRogersSku != null) {
			productIDs = parseParameters(inRogersSku, productIDs);
			media.getContext().putPageValue("rogerssku", inRogersSku);
		}
		String inUPCCode = media.getContext().getRequestParameter("upccode");
		if (inUPCCode != null) {
			productIDs = parseParameters(inUPCCode, productIDs);
			media.getContext().putPageValue("upccode", inUPCCode);
		}
		
		if (productIDs.size() > 0) {
			ArrayList<String> products = new ArrayList<String>();
			for(String productID in productIDs) {
				Data d = media.getProductSearcher().searchById(productID);
				if (d != null) {
					products.add(d.getId());
				} else {
					d = media.searchForProductByField("rogerssku",productID); 
					if (d != null) {
						products.add(d.getId());
					} else {
						d = media.searchForProductByField("upc",productID);
						if (d != null) {
							products.add(d.getId());								
						}
					}
				}
			}
			if (products.size() > 0) {
				ArrayList<String> error = new ArrayList<String>();
				media.getContext().putPageValue("error", error);
				media.getContext().putPageValue("products", products);
			} else {
				strMsg = "No current products found with the current search criteria.";
				media.getContext().putPageValue("error", strMsg );
				media.getContext().putPageValue("products", products);
			}
		}
	}
	
	private HashSet<String> parseParameters (String inString, HashSet<String> hashArr) {
		if (inString.contains(":")) {
			String[] values = inString.split(":");
			for(int ctr=0; ctr<values.size(); ctr++) {
				if (!hashArr.contains(values[ctr])) {
					String value = values[ctr];
					hashArr.add(value);
				}
			}
		} else {
			if (!hashArr.contains(inString)) {
				hashArr.add(inString);
			}
		}
		return hashArr;
	}
}

logs = new ScriptLogger();
logs.startCapture();

try {

	UpdateApproved lookup = new UpdateApproved();
	lookup.setLog(logs);
	lookup.setContext(context);
	lookup.setModuleManager(moduleManager);
	lookup.setPageManager(pageManager);
	
	lookup.doLookup();
}
finally {
	logs.stopCapture();
}
