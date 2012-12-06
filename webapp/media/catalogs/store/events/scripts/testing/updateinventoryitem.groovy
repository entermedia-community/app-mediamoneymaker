package testing

import org.openedit.Data
import org.openedit.entermedia.publishing.PublishResult
import org.openedit.store.InventoryItem
import org.openedit.store.Product
import org.openedit.store.Store

import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger

import edi.MediaUtilities

class UpdateInventoryItem extends EnterMediaObject {
	
	public PublishResult updateInventoryItem( String rogersSKU, String quantityShipped, Store store ) {

		PublishResult result = new PublishResult();
		result.setComplete(false);

		MediaUtilities media = new MediaUtilities();
		media.setContext(context);

		Data searchproduct = media.searchForProductbyRogersSKU(rogersSKU);
		Product product = store.getProduct(searchproduct.getId());
		InventoryItem productInventory = null
		productInventory = product.getInventoryItem(0);
		if (productInventory == null) {
			//Need to create the Inventory Item
			productInventory = new InventoryItem();
			productInventory.setSku(product.getId());
			product.addInventoryItem(productInventory);
		}
		productInventory.setQuantityInStock(Integer.parseInt(quantityShipped));
		
		media.getProductSearcher().saveData(product, media.getContext().getUser());
		result.setComplete(true);
		return result;
	}

}

logs = new ScriptLogger();
logs.startCapture();

try {

	Store store = context.getPageValue("store");
	
	PublishResult result = new PublishResult();
	result.setComplete(false);
	
	UpdateInventoryItem update = new UpdateInventoryItem();
	update.setLog(logs);
	update.setContext(context);
	update.setModuleManager(moduleManager);
	update.setPageManager(pageManager);
	
	result = update.updateInventoryItem("HM1700", "99", store);
	if (result.isComplete()) {
		context.putPageValue("export", "Update Product Success!");
	} else {
		context.putPageValue("export", "Update Product Failed!");
	}
}
finally {
	logs.stopCapture();
}
