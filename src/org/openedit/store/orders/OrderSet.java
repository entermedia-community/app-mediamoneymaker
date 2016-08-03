package org.openedit.store.orders;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.money.Money;
import org.openedit.store.Cart;
import org.openedit.store.CartItem;
import org.openedit.store.InventoryItem;
import org.openedit.store.Product;
import org.openedit.store.ShippingMethod;
import org.openedit.store.Store;
import org.openedit.store.TaxRate;
import org.openedit.store.customer.Customer;

public class OrderSet {
	
	private static final Log log = LogFactory.getLog( OrderSet.class );

	protected List<Order> fieldOrders;
	protected List<Order> fieldRemovedOrders;

	public List<Order> getOrders() {
		if (fieldOrders == null) {
			fieldOrders = new ArrayList<Order>();
		}
		return fieldOrders;
	}

	public List<Order> getRemovedOrders() {
		if (fieldRemovedOrders == null) {
			fieldRemovedOrders = new ArrayList<Order>();
		}
		return fieldRemovedOrders;
	}

	public void setOrders(List<Order> inOrders) {
		fieldOrders = inOrders;
	}
	
	public void setRemovedOrders(List<Order> inOrders) {
		fieldRemovedOrders = inOrders;
	}
	
	public void addOrder(Order inOrder){
		getOrders().add(inOrder);
	}
	
	public void addRemovedOrder(Order inOrder){
		getRemovedOrders().add(inOrder);
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
	
	public Order getOrderByCustomerId(String customerId){
		Iterator<Order> itr = getOrders().iterator();
		while(itr.hasNext()){
			Order order = itr.next();
			if (order.getCustomer().getId().equals(customerId)){
				return order;
			}
		}
		return null;
	}
	
	public Order getRemovedOrder(String inId) {
		Order order = null;
		for (Iterator<Order> iterator = getRemovedOrders().iterator(); iterator.hasNext();) {
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
		for (Object orderObject : getOrders()) {
			Order order = (Order) orderObject;
			qty = getQuantityForProductInOrder(order, product);
		}
		return qty;
	}
	
	public double getQuantityInStock( Product product ) {
		InventoryItem item = product.getInventoryItem(0);
		return item.getQuantityInStock();
	}
	
	public int getQuantityForProductInOrder(Order inOrder, Product inProduct) {
		int qty = 0;
		for (Object cartItemObject : inOrder.getCart().getItems()) {
			CartItem item = (CartItem) cartItemObject;
			if (item.getProduct().equals(inProduct)) {
				qty += item.getQuantity();
			}
		}
		return qty;
	}

	public Set<String> getOutOfStockOrders(){
		Set<String> problemorders = new TreeSet<String>();
		for (Object orderObject : getOrders()) {
			Order order = (Order) orderObject;
			Cart cart = order.getCart();
			for (Object item : cart.getItems()) {
				CartItem cartItem = (CartItem) item;
				if(!cartItem.getProduct().isInStock()){
					problemorders.add(order.getId());	
					continue;
				}
			}
		}
		return problemorders;
	}
	
	public Set<String> getOutOfStockPerOrder(Order inOrder) {
		Set<String> badProducts = new TreeSet<String>();
		Cart cart = inOrder.getCart();
		for (Object item : cart.getItems()) {
			CartItem cartItem = (CartItem) item;
			Product product = cartItem.getProduct();
			if(!product.isInStock()){
				badProducts.add(product.getId());
				continue;
			}
			int totalrequested = getQuantityForProduct(product);
			if(totalrequested > cartItem.getInventoryItem().getQuantityInStock()){
				badProducts.add(product.getId());
			}
		}
		return badProducts;
	}
	
	public Set<String> getAllOutOfStockProductsFromAllOrders() {
		Set<String> badProducts = new TreeSet<String>();
		for (Object orderObject : getOrders()) {
			Order order = (Order) orderObject;
			badProducts.addAll(getOutOfStockPerOrder(order));
		}
		return badProducts;
	}
	
	public boolean doesOrderHaveOutOfStockProducts(Order inOrder) {
		return (getOutOfStockPerOrder(inOrder).size() > 0);
	}
	
	public boolean doesOrderSetHaveOutOfStockProducts() {
		return (getOutOfStockOrders().size() > 0);
	}
	
	public Set<String> getAllBadProducts() {
		Set<String> badProducts = new TreeSet<String>();
		for (Object orderObject : getOrders()) {
			Order order = (Order) orderObject;
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
	
	public void recalculateOrder(Store store){
		Iterator<Order> itr = getOrders().iterator();
		while(itr.hasNext()){
			Order order = itr.next();
			recalculateOrder(order,store);
		}
	}
	
	public void recalculateOrder( Order inOrder, Store store ) {
		Cart cart = inOrder.getCart();
		if (cart.getItems().size() > 0) {
			
			inOrder.setSubTotal(cart.getSubTotal());
	
			Customer customer = inOrder.getCustomer();
			if (customer.getShippingAddress().getState().equals("NF")) {
				customer.getShippingAddress().setState("NL");
			}
			if (customer.getBillingAddress().getState().equals("NF")) {
				customer.getBillingAddress().setState("NL");
			}
	
			String state = customer.getShippingAddress().getState(); 
			List<TaxRate> taxrates = store.getTaxRatesFor(state);
			if (state == null) {
				String billingState = customer.getBillingAddress().getState();
				taxrates = cart.getStore().getTaxRatesFor(billingState);
			}
			
			if (taxrates == null || taxrates.size()==0) {
			//	throw new OpenEditException("Taxrates is null");
			}
			customer.setTaxRates(taxrates);
			cart.setCustomer(customer);
			inOrder.setCustomer(customer);
			inOrder.setTaxes(cart.getTaxes());
			
			ShippingMethod method = (ShippingMethod) store.getAllShippingMethods().get(0);
			cart.setShippingMethod(method);
			cart.setShippingAddress(inOrder.getShippingAddress());
			cart.setBillingAddress(inOrder.getBillingAddress());
			inOrder.setTotalShipping(cart.getTotalShipping());
			
			inOrder.setTotalTax(cart.getTotalTax());
			inOrder.setSubTotal(cart.getSubTotal());
			inOrder.setTotalPrice(cart.getTotalPrice());
//			store.saveOrder(inOrder);
		} else {
			log.info("Recalculate Order ["+inOrder+"]: skipped, no cart items");
		}
	}
	
	public void recalculateAll( Store store ) {
		for (Object object : getOrders()) {
			Order order = (Order) object;
			Cart cart = order.getCart();
			if (cart.getItems().size() > 0) {
				recalculateOrder(order, store);
			}
		}
		removeEmptyOrders(store);
	}
	
	public void removeEmptyOrders(Store store) {
		List<Order> ordersToRemove = new ArrayList<Order>();
		for (Object orderObject : getOrders()) {
			Order order = (Order) orderObject;
			Cart cart = order.getCart();
			List<CartItem> removeItemList = new ArrayList<CartItem>();
			for (Object cartItemObject : cart.getItems()) {
				CartItem item = (CartItem) cartItemObject;
				if (item.getQuantity() == 0) {
					removeItemList.add(item);
				}
			}
			if (removeItemList.size() > 0) {
				for (CartItem item : removeItemList) {
					cart.getItems().remove(item);
				}
			}
			if (cart.getItems().size() == 0) {
				addRemovedOrder(order);
				ordersToRemove.add(order);
			}
		}
		if (ordersToRemove.size() > 0) {
			for (Order orderObject : ordersToRemove) {
				getOrders().remove(orderObject);
			}
		}
	}
	
	public void removeOutOfStockItemsFromOrder( Order inOrder, Store store ) {
		Cart cart = inOrder.getCart();
		List<CartItem> outOfStockItems = new ArrayList<CartItem>();
		for (Object object : cart.getItems()) {
			CartItem item = (CartItem) object;
			if (getQuantityInStock(item.getProduct()) == 0) {
				outOfStockItems.add(item);
			}
		}
		if (outOfStockItems.size() > 0) {
			for (Object item : outOfStockItems) {
				CartItem cartItem = (CartItem) item;
				removeItemFromOrder(cartItem.getProduct().getId(), inOrder, store);
			}
		}
	}
	
	public void removeItemFromAllOrders( String inProduct, Store store ) {
		for (Object object : getOrders()) {
			Order order = (Order) object;
			if (order != null) {
				removeItemFromOrder(inProduct, order, store);
			}
		}
		recalculateAll(store);
	}
	
	public void removeItemFromOrders(String inProduct, List<String> orders, Store store) {
		for (String orderID : orders) {
			Order order = getOrder(orderID);
			removeItemFromOrder(inProduct, order, store);
		}
	}
	
	public void removeItemFromOrder( String inProduct, Order inOrder, Store store ) {
		Cart cart = inOrder.getCart();
		Product product = cart.findProduct(inProduct);
		if (product != null) {
			if (inOrder.containsProduct(product)) {
				for ( Object item : cart.getItems()) {
					CartItem cartItem = (CartItem) item;
					if (cartItem.getProduct().getId().equals(inProduct)) {
						cart.getItems().remove(cartItem);
						//store.saveOrder(inOrder);
						break;
					}
				}
			}
		}
	}
	
}
