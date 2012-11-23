package com.openedit.store.orders;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.apache.lucene.document.Document;
import org.openedit.store.Cart;
import org.openedit.store.CartItem;
import org.openedit.store.InventoryItem;
import org.openedit.store.Product;
import org.openedit.store.Store;
import org.openedit.store.StoreTestCase;
import org.openedit.store.customer.Customer;
import org.openedit.store.modules.ProductControlModule;
import org.openedit.store.modules.CartModule;
import org.openedit.store.modules.OrderModule;
import org.openedit.store.orders.Order;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;

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

		HitTracker tracker = store.getOrderSearcher().search("products:1234", "id");
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

		Collection hits = store.getOrderSearcher().search("customer","admin");
		assertTrue(hits.size() > 0);
	}

	public void testOrderItemUpdate() throws Exception {
		Store store = getStore();
		CartModule cartModule = (CartModule) getFixture().getModuleManager().getModule("CartModule");
		OrderModule orderModule = (OrderModule) getFixture().getModuleManager().getModule("OrderModule");
		ProductControlModule assetControl = (ProductControlModule) getFixture().getModuleManager().getModule("AssetControlModule");
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
		item.setQuantity(1);
		item.setStatus("accepted");
		cart.addItem(item);

		CartItem item2 = new CartItem();
		item2.setProduct(product2);
		item2.setQuantity(1);
		item2.setStatus("accepted");
		cart.addItem(item2);

		Order order = store.getOrderGenerator().createNewOrder(store, cart);
		
		order.setProperty("notes", "This is a note");
		
		store.saveOrder(order);

		context.setRequestParameter("sku", new String[] { item.getSku(), item2.getSku() });
		context.setRequestParameter("abcd1.status", "rejected");
		context.setRequestParameter("abcd2.status", "approved");

		String orderid = order.getId();
		context.setRequestParameter("ordernumber", orderid);
		orderModule.changeItemStatus(context);
		CartItem test = store.getOrderArchive().loadSubmittedOrder(store, "admin", orderid).getItem("abcd1");
		assertEquals("rejected", test.getStatus());

		// test download permission
		String path = store.getCatalogId() + "/products/" + store.getProductPathFinder().idToPath("abcd") + ".html";
		WebPageRequest accessrequest = getFixture().createPageRequest(path);
		getFixture().getEngine().executePathActions(accessrequest);
		getFixture().getEngine().executePageActions(accessrequest);

		assertEquals(Boolean.FALSE, accessrequest.getPageValue("candownload"));

		path = store.getCatalogId() + "/products/" + store.getProductPathFinder().idToPath("defg") + ".html";
		accessrequest = getFixture().createPageRequest(path);
		getFixture().getEngine().executePathActions(accessrequest);
		getFixture().getEngine().executePageActions(accessrequest);

		assertEquals(Boolean.TRUE, accessrequest.getPageValue("candownload"));

		// test date limited

		Calendar rightNow = Calendar.getInstance();

		rightNow.add(Calendar.YEAR, (-1));
		Date searchDate = rightNow.getTime();
		order.setDate(searchDate);
		store.saveOrder(order);
		path = store.getCatalogId() + "/products/" + store.getProductPathFinder().idToPath("defg") + ".html";
		accessrequest = getFixture().createPageRequest(path);
		getFixture().getEngine().executePathActions(accessrequest);
		getFixture().getEngine().executePageActions(accessrequest);
		assertEquals(Boolean.FALSE, accessrequest.getPageValue("candownload"));
		//test download count limited.
		order.setDate(new Date());
		store.saveOrder(order);
		assertEquals(Boolean.TRUE, accessrequest.getPageValue("candownload"));
		

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

		HitTracker tracker = store.getOrderSearcher().search("products:1234", "id");
		assertTrue(tracker.getTotal() > 0);

		store.getOrderSearcher().reIndexAll();

		Collection hits = store.getOrderSearcher().search("customer","admin");
		assertTrue(hits.size() > 0);

	}

	
	
	
	
	public void testOrderShipTracking() throws Exception {
		Store store = getStore();
		CartModule cartModule = (CartModule) getFixture().getModuleManager().getModule("CartModule");
		OrderModule orderModule = (OrderModule) getFixture().getModuleManager().getModule("OrderModule");
		ProductControlModule assetControl = (ProductControlModule) getFixture().getModuleManager().getModule("AssetControlModule");
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
		item.setProperty("shipped", "true");
//		item.setProperty("waybill", "1234");
//		item.setProperty("quantityshippied", "12");
		cart.addItem(item);

		CartItem item2 = new CartItem();
		item2.setProduct(product2);
		item2.setQuantity(10);
		item2.setStatus("accepted");
//		item.setProperty("shipped", "true");
//		item.setProperty("waybill", "1234");
//		
		cart.addItem(item2);

		Order order = store.getOrderGenerator().createNewOrder(store, cart);
		
		order.setProperty("notes", "This is a note");
		
		
		Shipment shipment = new Shipment();
		shipment.setProperty("waybill", "12345");
		shipment.setProperty("person", "12345");
		
		ShipmentEntry entry = new ShipmenEntry();
		entry.setCartItem(item);
		entry.setQuantity(5);
		shipment.addEntry(entry);
		
		
		ShipmentEntry entry = new ShipmenEntry();
		entry.setCartItem(item2);
		entry.setQuantity(10);
		shipment.addEntry(entry);
		
		
		order.addShipment(shipment);
		

		assertTrue(order.isFullyShipped(item2));
		assertFalse(order.isFullyShipped(item1));
		
		assertFalse(order.isFullyShipped());
		
		
		Shippment shipment2 = new Shippment();
		
		
		ShipmentEntry entry2 = new ShipmenEntry();
		entry.setCartItem(item);
		entry.setQuantity(5);
		shipment2.addEntry(entry);
		
		
		order.addShipment(entry2);
		
		assertTrue(order.isFullyShipped(item1));
		assertTrue(order.isFullyShipped());
		
		
		
		store.saveOrder(order);
		String orderid = order.getId();

		order = null;
		
		
	
		order = store.getOrderArchive().loadSubmittedOrder(store, "admin", orderid);
		
		assertTrue(order.isFullyShipped());
		

	}
	
	
	
	
}
