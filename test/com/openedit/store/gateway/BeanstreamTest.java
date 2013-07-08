/*
 * Created on Oct 12, 2004
 */
package com.openedit.store.gateway;

import org.openedit.money.Money;
import org.openedit.store.Cart;
import org.openedit.store.CartItem;
import org.openedit.store.InventoryItem;
import org.openedit.store.Product;
import org.openedit.store.Store;
import org.openedit.store.StoreTestCase;
import org.openedit.store.customer.Customer;
import org.openedit.store.gateway.BeanstreamUtil;
import org.openedit.store.modules.CartModule;
import org.openedit.store.orders.Order;

import com.openedit.WebPageRequest;

/**
 * @author Ian Miller
 */
public class BeanstreamTest extends StoreTestCase {
	

	/**
	 * @param name
	 */
	public BeanstreamTest(String name) {
		super(name);
	}

	/**
	 * @throws Exception
	 */

	public void testBeanstreamRefund() throws Exception {
		BeanstreamUtil util = (BeanstreamUtil) getFixture().getModuleManager().getBean("beanstreamUtil");
		Money money = new Money("10");
		
		Store store = getStore();
		CartModule cartModule = (CartModule) getFixture().getModuleManager().getModule("CartModule");

		WebPageRequest context = getFixture().createPageRequest();
		Cart cart = cartModule.getCart(context);
		cart.setCustomer(new Customer(context.getUser()));
		Product product = new Product();
		product.setId("1234");
		product.addInventoryItem(new InventoryItem("1234.5"));
		CartItem item = new CartItem();
		item.setProduct(product);
		item.setQuantity(1);
		cart.addItem(item);
		Order order = store.getOrderGenerator().createNewOrder(store, cart);
		order.setProperty("notes", "This is a note");
		store.saveOrder(order);
		
		
		
		
		
	}
	
	
}