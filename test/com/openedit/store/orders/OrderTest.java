package com.openedit.store.orders;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.apache.lucene.document.Document;
import org.openedit.WebPageRequest;
import org.openedit.hittracker.HitTracker;
import org.openedit.money.Money;
import org.openedit.store.Cart;
import org.openedit.store.CartItem;
import org.openedit.store.InventoryItem;
import org.openedit.store.Product;
import org.openedit.store.Store;
import org.openedit.store.StoreTestCase;
import org.openedit.store.customer.Customer;
import org.openedit.store.modules.CartModule;
import org.openedit.store.modules.OrderModule;
import org.openedit.store.orders.Order;
import org.openedit.store.orders.Shipment;
import org.openedit.store.orders.ShipmentEntry;

public class OrderTest extends StoreTestCase {

	public OrderTest(String inArg0) {
		super(inArg0);
	}

	public void xtestOrderSearch() throws Exception {
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

		HitTracker tracker = store.getOrderSearcher().fieldSearch("products","1234", "id");
				
		assertTrue(tracker.getTotal() > 0);
		boolean found = false;
		for (Iterator i = tracker.getAllHits(); i.hasNext();) {
			Document orderHit = (Document) i.next();
			Order foundOrder = store.getOrderArchive().loadSubmittedOrder(store, orderHit.get("customer"), orderHit.get("id"));
			assertNotNull(foundOrder);
			if ("This is a note".equals(foundOrder.get("notes"))) {
				found = true;
				break;
			}
		}
		assertEquals(true, found);

		store.getOrderSearcher().reIndexAll();

		Collection hits = store.getOrderSearcher().fieldSearch("customer","admin");
		assertTrue(hits.size() > 0);
	}

	
	public void xtestMultipleAddressSupport() throws Exception {
		Store store = getStore();
		CartModule cartModule = (CartModule) getFixture().getModuleManager().getModule("CartModule");

		WebPageRequest context = getFixture().createPageRequest();
		Cart cart = cartModule.getCart(context);
		Customer customer = new Customer(context.getUser());
		cart.setCustomer(customer);

		Product product = new Product();
		product.setId("1234");
		product.addInventoryItem(new InventoryItem("1234.5"));

		CartItem item = new CartItem();
		item.setProduct(product);
		item.setQuantity(1);
		item.setShippingPrefix("billing");
		cart.addItem(item);

		Product product2 = new Product();
		product2.setId("2222");
		product2.addInventoryItem(new InventoryItem("2222.5"));

		CartItem item2 = new CartItem();
		item2.setProduct(product2);
		item2.setQuantity(1);
		item2.setShippingPrefix("shipping");
		cart.addItem(item2);

		Order order = store.getOrderGenerator().createNewOrder(store, cart);
		order.setProperty("notes", "This is a note");
		store.saveOrder(order);
		Order testLoad = store.getOrderArchive().loadSubmittedOrder(store, customer.getUserName(), order.getId());

		for (Iterator iterator = testLoad.getItems().iterator(); iterator.hasNext();) {
			CartItem cartItem = (CartItem) iterator.next();
			if (cartItem.getProduct().getId().equals("2222")) {
				assertEquals("shipping", cartItem.getShippingPrefix());
			}
			if (cartItem.getProduct().getId().equals("1234")) {
				assertEquals("billing", cartItem.getShippingPrefix());
			}

		}

		HitTracker tracker = store.getOrderSearcher().fieldSearch("products","1234", "id");
		assertTrue(tracker.getTotal() > 0);

		store.getOrderSearcher().reIndexAll();

		Collection hits = store.getOrderSearcher().fieldSearch("customer","admin");
		assertTrue(hits.size() > 0);

	}

	public void testOrderShipTracking() throws Exception {
		Store store = getStore();
		CartModule cartModule = (CartModule) getFixture().getModuleManager().getModule("CartModule");
		OrderModule orderModule = (OrderModule) getFixture().getModuleManager().getModule("StoreOrderModule");
		WebPageRequest context = getFixture().createPageRequest("/store/index.html");
		Cart cart = cartModule.getCart(context);
		cart.setCustomer(new Customer(context.getUser()));

		Product product = new Product();
		product.setId("abcd");
		product.addInventoryItem(new InventoryItem("abcd1"));

		Product product2 = new Product();
		product2.setId("defg");
		product2.addInventoryItem(new InventoryItem("abcd2"));

		store.saveProduct(product);
		store.saveProduct(product2);
		
		CartItem item = new CartItem();
		item.setProduct(product);
		item.setQuantity(10);
		item.setStatus("accepted");
		
		cart.addItem(item);

		CartItem item2 = new CartItem();
		item2.setProduct(product2);
		item2.setQuantity(10);
		item2.setStatus("accepted");
	
		
		cart.addItem(item2);

		Order order = store.getOrderGenerator().createNewOrder(store, cart);
		order.setProperty("notes", "This is a note");
		
		Shipment shipment = new Shipment();
		shipment.setProperty("TESTPROPERTIES", "IAN");
		ShipmentEntry entry1 = new ShipmentEntry();
//		entry1.setCartItem(item);
		entry1.setSku(item.getSku());
		entry1.setQuantity(5);
		
		shipment.addEntry(entry1);

		ShipmentEntry entry2 = new ShipmentEntry();
		entry2.setSku(item2.getSku());
		entry2.setQuantity(10);
		
		shipment.addEntry(entry2);
		
		order.addShipment(shipment);
		
		assertTrue(order.isFullyShipped(item2));
		assertFalse(order.isFullyShipped(item));
		
		assertFalse(order.isFullyShipped());
		
		Shipment shipment2 = new Shipment();
		
		ShipmentEntry entry3 = new ShipmentEntry();
		entry3.setSku(item.getSku());
		entry3.setQuantity(5);
		shipment2.addEntry(entry3);
		
		
		order.addShipment(shipment2);
		
		assertTrue(order.isFullyShipped(item));
		assertTrue(order.isFullyShipped());
		
		store.saveOrder(order);
		String orderid = order.getId();

		order = null;
		
		
	
		order = store.getOrderArchive().loadSubmittedOrder(store, "admin", orderid);
		

		assertTrue(order.isFullyShipped());
		assertEquals(2, order.getShipments().size());
		shipment.setProperty("TESTPROPERTIES", "IAN");
		assertEquals("IAN", ((Shipment) order.getShipments().get(0)).get("TESTPROPERTIES"));
		
	}
	
	public void xtestOrderRefunds() throws Exception {
		Store store = getStore();
		CartModule cartModule = (CartModule) getFixture().getModuleManager().getModule("CartModule");
		OrderModule orderModule = (OrderModule) getFixture().getModuleManager().getModule("StoreOrderModule");
		WebPageRequest context = getFixture().createPageRequest("/store/index.html");
		Cart cart = cartModule.getCart(context);
		cart.setCustomer(new Customer(context.getUser()));

		Product product = new Product();
		product.setId("abcd");
		product.addInventoryItem(new InventoryItem("abcd1"));

		Product product2 = new Product();
		product2.setId("defg");
		product2.addInventoryItem(new InventoryItem("abcd2"));

		store.saveProduct(product);
		store.saveProduct(product2);
		
		CartItem item = new CartItem();
		item.setProduct(product);
		item.setQuantity(10);
		item.setStatus("accepted");
		
		cart.addItem(item);

		CartItem item2 = new CartItem();
		item2.setProduct(product2);
		item2.setQuantity(10);
		item2.setStatus("accepted");
	
		
		cart.addItem(item2);

		Order order = store.getOrderGenerator().createNewOrder(store, cart);
		order.setProperty("notes", "This is a note");
		
		
		store.saveOrder(order);
		
		Money totalamount = item.getTotalPrice();
		Money totaltaxes = order.getTax();
		
	
		String orderid = order.getId();

		order = null;
		
		context.setRequestParameter("orderid", orderid);
		context.setRequestParameter("productid", "abcd");
		context.setRequestParameter("quantity", "2");
		
		orderModule.refundOrder(context);
		
		
		
	
		order = store.getOrderArchive().loadSubmittedOrder(store, "admin", orderid);
		
		
		assertTrue(order.getRefunds().size() >0);
		assertTrue(order.getTotalPrice().doubleValue() < totalamount.doubleValue());
		assertTrue(order.getTax().doubleValue() < totaltaxes.doubleValue());
		//TODO: Do we need to handle taxes 

		
		
		
	}
	
}
