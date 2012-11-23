package com.edi;

import org.openedit.entermedia.publishing.PublishResult;
import org.openedit.store.InventoryItem;
import org.openedit.store.Product;

import com.openedit.entermedia.scripts.EnterMediaObject;

public class TestUpdateInventoryItem  extends EnterMediaObject{

	public PublishResult testUpdateInventoryItem( String productID, String quantityShipped ) {

		PublishResult result = new PublishResult();
		result.setComplete(false);
		
		TestMediaUtilities media = new TestMediaUtilities();
		media.setContext(context);
		media.setSearchers();
		
		Product product = (Product) media.getProductSearcher().searchById(productID);
		InventoryItem productInventory = product.getInventoryItem(0);
		productInventory.setQuantityInStock(Integer.parseInt(quantityShipped));
		media.getProductSearcher().saveData(product, context.getUser());

		if (String.valueOf(productInventory.getQuantityInStock()).equals(quantityShipped)) {
			result.setComplete(true);
		}		
		return result;
	}
}
