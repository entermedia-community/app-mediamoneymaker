package org.openedit.store.orders;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.openedit.money.Money;
import org.openedit.store.Cart;
import org.openedit.store.CartItem;
import org.openedit.store.Product;
import org.openedit.store.ShippingMethod;
import org.openedit.store.Store;
import org.openedit.store.customer.Customer;

import com.openedit.OpenEditException;

public class OrderSet {

	protected List<Order> fieldOrders;

	public List<Order> getOrders() {
		if (fieldOrders == null) {
			fieldOrders = new ArrayList<Order>();
		}
		return fieldOrders;
	}

	public void setOrders(List<Order> inOrders) {
		fieldOrders = inOrders;
	}
	
	public void addOrder(Order inOrder){
		getOrders().add(inOrder);
	}
	
	public Order getOrder(String inId) {
		Order order = null;
		for (Iterator<Order> iterator = getOrders().iterator(); iterator.hasNext();) {
			Order newOrder = iterator.next();
			if (newOrder.getId().equals(inId)) {
				order = newOrder;
			}
		}
		return order;
	}
	
	public Money getSubTotal(){
		Money money = new Money();
		for (Iterator<Order> iterator = getOrders().iterator(); iterator.hasNext();) {
			Order order = iterator.next();
			Money total = (Money) order.getSubTotal();
			money = money.add(total);
		}
		return money;
	}
	
	public Money getTotalTaxes(){
		Money money = new Money();
		for (Iterator<Order> iterator = getOrders().iterator(); iterator.hasNext();) {
			Order order = iterator.next();
			Money total = (Money) order.getTax(); 
			money = money.add(total);
		}
		return money;
	}

	public Money getTotalShipping(){
		Money money = new Money();
		for (Iterator<Order> iterator = getOrders().iterator(); iterator.hasNext();) {
			Order order = iterator.next();
			Money total = (Money) order.getTotalShipping(); 
			money = money.add(total);
		}
		return money;
	}

	public Money getTotalPrice(){
		Money money = new Money();
		for (Iterator<Order> iterator = getOrders().iterator(); iterator.hasNext();) {
			Order order = iterator.next();
			Money total = (Money) order.getTotalPrice();
			money = money.add(total);
		}
		return money;
	}
	
	public int getQuantityForProduct(Product product){
		//loop over all orders and total up number request.
		int qty = 0;
		for (Iterator<Order> iterator = getOrders().iterator(); iterator.hasNext();) {
			Order order = iterator.next();
			for (Iterator<CartItem> items = order.getItems().iterator(); items.hasNext();) {
				CartItem item = items.next();
				if (item.getProduct().equals(product)) {
					qty += item.getQuantity();
				}
			}
		}
		return qty;
	}
	
	public int getQuantityForProductInOrder(Order inOrder, Product inProduct) {
		int qty = 0;
		for (Iterator<CartItem> items = inOrder.getItems().iterator(); items.hasNext();) {
			CartItem item = items.next();
			if (item.getProduct().equals(inProduct)) {
				qty += item.getQuantity();
			}
		}
		return qty;
	}

	public Set<String> getOutOfStockOrders(){
		Set<String> problemorders = new TreeSet<String>();
		for (Iterator<Order> iterator = getOrders().iterator(); iterator.hasNext();) {
			Order order = iterator.next();
			for (Iterator<CartItem> iterator2 = order.getItems().iterator(); iterator2.hasNext();) {
				CartItem item = iterator2.next();
				if(!item.getProduct().isInStock()){
					problemorders.add(order.getId());	
					continue;
				}
				int totalrequested = getQuantityForProduct(item.getProduct());
				if(totalrequested > item.getInventoryItem().getQuantityInStock()){
					problemorders.add(order.getId());
				}
			}
			
		}
		return problemorders;
	}
	
	public Set<String> getOutOfStockPerOrder(Order inOrder) {
		Set<String> badProducts = new TreeSet<String>();
		for (Iterator<CartItem> iterator2 = inOrder.getItems().iterator(); iterator2.hasNext();) {
			CartItem item = iterator2.next();
			Product product = item.getProduct();
			if(!product.isInStock()){
				badProducts.add(product.getId());
				continue;
			}
			int totalrequested = getQuantityForProduct(product);
			if(totalrequested > item.getInventoryItem().getQuantityInStock()){
				badProducts.add(product.getId());
			}
		}
		return badProducts;
	}
	
	public boolean doesOrderHaveOutOfStockProducts(Order inOrder) {
		return (getOutOfStockPerOrder(inOrder).size() > 0);
	}
	
	public Set<String> getAllBadProducts() {
		Set<String> badProducts = new TreeSet<String>();
		for (Iterator<Order> iterator = getOrders().iterator(); iterator.hasNext();) {
			Order order = iterator.next();
			for(String badProduct : getBadProductsPerOrder(order)) {
				badProducts.add(badProduct);
			}
		}
		return badProducts;
	}
	
	public Set<String> getBadProductsPerOrder(Order inOrder) {
		return inOrder.getMissingItems();
	}
	
	public boolean doesOrderHaveBadProducts(Order inOrder) {
		return (inOrder.getMissingItems().size() > 0);
	}
	
	public int getNumberOfOrders() {
		return getOrders().size();
	}
	
	public void recalculateOrder( Order inOrder, Store store ) {
		if (inOrder.getNumItems() > 0) {
			Cart cart = inOrder.getCart();
			cart.setStore(store);
			cart.setItems(inOrder.getItems());
	
			Customer customer = inOrder.getCustomer();
			cart.setCustomer(customer);
			
			if (customer.getShippingAddress().getState().equals("NF")) {
				customer.getShippingAddress().setState("NL");
			}
			if (customer.getBillingAddress().getState().equals("NF")) {
				customer.getBillingAddress().setState("NL");
			}
	
			List taxrates = cart.getStore().getTaxRatesFor(
					customer.getShippingAddress().getState());
			if (customer.getShippingAddress().getState() == null) {
				taxrates = cart.getStore().getTaxRatesFor(
						customer.getBillingAddress().getState());
			}
			customer.setTaxRates(taxrates);
			
			inOrder.setTaxes(cart.getTaxes());
			inOrder.setSubTotal(cart.getSubTotal());
			
			ShippingMethod method = (ShippingMethod) store.getAllShippingMethods().get(0);
			cart.setShippingMethod(method);
			cart.setShippingAddress(inOrder.getShippingAddress());
			cart.setBillingAddress(inOrder.getBillingAddress());
			inOrder.setTotalShipping(cart.getTotalShipping());
			
			inOrder.setSubTotal(cart.getSubTotal());
			inOrder.setTotalPrice(cart.getTotalPrice());
			inOrder.setTotalTax(cart.getTotalTax());
		} else {
			System.out.println("Order skipped. No Cart Items");
		}
	}
	
	public void recalculateAll( Store store ) {
		for (Iterator<Order> iterator = getOrders().iterator(); iterator.hasNext();) {
			Order order = (Order) iterator.next();
			recalculateOrder(order, store);
		}
	}
	
}
