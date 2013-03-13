package testing

import org.openedit.Data
import org.openedit.entermedia.publishing.PublishResult
import org.openedit.store.util.MediaUtilities

import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger


class LookupProduct extends EnterMediaObject {
	
	public PublishResult doLookup() {

		PublishResult result = new PublishResult();
		result.setComplete(false);

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
				String strMsg = "";
				strMsg += wrapTD(product.getId());
				strMsg += wrapTD(product.getName());
				strMsg += wrapTD(product.get("distributor"));
				strMsg += wrapTD(product.get("rogerssku"));
				strMsg += wrapTD(product.get("manufacturersku"));
				strMsg += wrapTD(product.get("upc"));
				strMsg += wrapTD(product.get("rogersprice"));
				result.setCompleteMessage(wrapTR(strMsg));
				result.setComplete(true);
			} else {
				String strMsg = "<td colspan=6>Product not found ";
				if (productid != null) {
					strMsg += "(ProductID:" + productid + ")";
				} else {
					if (rogersSKU != null) {
						strMsg += "(RogersSKU:" + rogersSKU + ")";
					} else {
						if (upcCode != null) {
							strMsg += "(UPC:" + upcCode + ")";
						}
					}
				}
				strMsg += "</td>\n";
				result.setErrorMessage(wrapTR(strMsg));
			}
		} else {
			result.setErrorMessage(wrapTR("<td colspan=6>No product selected.</td>"));
		}
		return result;
	}
	private String wrapTD(String inString) {
		return "<td>" + inString + "</td>\n";
	}
	private String wrapTR(String inString) {
		return "<tr>" + inString + "</tr>\n";
	}

}

logs = new ScriptLogger();
logs.startCapture();

try {

	PublishResult result = new PublishResult();
	result.setComplete(false);
	
	LookupProduct lookup = new LookupProduct();
	lookup.setLog(logs);
	lookup.setContext(context);
	lookup.setModuleManager(moduleManager);
	lookup.setPageManager(pageManager);
	
	result = lookup.doLookup();
	if (result.isComplete()) {
		context.putPageValue("export", result.getCompleteMessage());
	} else {
		context.putPageValue("export", result.getErrorMessage());
	}
}
finally {
	logs.stopCapture();
}
