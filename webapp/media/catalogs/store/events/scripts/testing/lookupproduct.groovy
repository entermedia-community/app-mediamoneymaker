package testing

import java.sql.ResultSet;

import org.openedit.Data
import org.openedit.entermedia.publishing.PublishResult
import org.openedit.store.InventoryItem
import org.openedit.store.Product
import org.openedit.store.Store

import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger

import edi.MediaUtilities

class LookupProduct extends EnterMediaObject {
	
	public PublishResult doLookup() {

		PublishResult result = new PublishResult();
		result.setComplete(false);

		MediaUtilities media = new MediaUtilities();
		media.setContext(context);

		def productid = media.getContext().getRequestParameter("productid");
		def rogersSKU = media.getContext().getRequestParameter("rogerssku");
		if (productid != null || rogersSKU != null) {
			Data product = null
			if (productid != null) {			
				product = media.getProductSearcher().searchById(productid);
			} else {
				if (rogersSKU != null) {
					product = media.searchForProductbyRogersSKU(rogersSKU);
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
				result.setErrorMessage(wrapTR("<td colspan=6>Product not found (" + productid + ")</td>"));
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
