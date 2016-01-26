package com.edi;

import org.entermediadb.asset.publishing.PublishResult;
import org.entermediadb.scripts.EnterMediaObject;
import org.openedit.store.InventoryItem;
import org.openedit.store.Product;

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
