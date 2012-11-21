package org.openedit.store.orders;

import java.util.Iterator;

import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.money.Money;
import org.openedit.store.CartItem;
import org.openedit.store.Product;
import org.openedit.store.Store;
import org.openedit.store.StoreException;

import com.openedit.WebPageRequest;

public class RogersOrderProcessor extends BaseOrderProcessor {

	public void processNewOrder(WebPageRequest inContext, Store inStore,
			Order inOrder) throws StoreException {

		String catalogid = inStore.getCatalogId();
		Searcher rogersorders = inStore.getSearcherManager().getSearcher(catalogid, "rogers_order");
		Searcher orderitems = inStore.getSearcherManager().getSearcher(catalogid, "rogers_order_item");
		Data order = rogersorders.createNewData();
		order.setId(rogersorders.nextId());
		order.setProperty("storeorder", inOrder.getId());
				
		for (Iterator iterator = inOrder.getProperties().keySet().iterator(); iterator.hasNext();) {
			String key = (String) iterator.next();
			String val = inOrder.get(key);			
		}
		
		
		
		for (Iterator iterator = inOrder.getItems().iterator(); iterator.hasNext();) {
			CartItem item = (CartItem) iterator.next();
			Product p = item.getProduct();
			int quantity = item.getQuantity();
			Money total = item.getTotalPrice();
			//Money tax =  item.get
			
		}
		
		
		
	}

}
