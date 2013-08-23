package org.openedit.store.orders;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openedit.money.Money;
import org.openedit.store.CartItem;
import org.openedit.store.Product;

public class OrderSet {

	
	protected List fieldOrders;

	public List getOrders() {
	if (fieldOrders == null) {
		fieldOrders = new ArrayList();
		
	}

	return fieldOrders;
	}

	public void setOrders(List inOrders) {
		fieldOrders = inOrders;
	}
	
	
	public void addOrder(Order inOrder){
		getOrders().add(inOrder);
		
	}
	
	
	public Money getTotalPrice(){
		Money money = new Money();
		for (Iterator iterator = getOrders().iterator(); iterator.hasNext();) {
			Order order = (Order) iterator.next();
			money.add(order.getTotalPrice());
		}
		return money;
	}
	
	public Money getTotalTaxes(){
		Money money = new Money();
		for (Iterator iterator = getOrders().iterator(); iterator.hasNext();) {
			Order order = (Order) iterator.next();
			money.add(order.getTax());
		}
		return money;
	}
	
	
	public int getQuantityForProduct(Product product){
return 0;	}
	
	
	public List getOutOfStockOrders(){
		List problemorders = new ArrayList();
		for (Iterator iterator = getOrders().iterator(); iterator.hasNext();) {
			Order order = (Order) iterator.next();
			for (Iterator iterator2 = order.getItems().iterator(); iterator2
					.hasNext();) {
				CartItem item = (CartItem)iterator2.next();
				if(!item.getProduct().isInStock()){
					
					problemorders.add(order);	
					continue;
				}
				int totalrequested = getQuantityForProduct(item.getProduct());
				if(totalrequested > item.getInventoryItem().getQuantityInStock()){
					problemorders.add(order);
				}
			}
			
		}
		return problemorders;
	}
	
	
	
}
