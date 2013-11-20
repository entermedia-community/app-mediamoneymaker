package com.openedit.store.orders;

import java.util.Set;

import org.junit.Test;
import org.openedit.store.Cart;
import org.openedit.store.CartItem;
import org.openedit.store.CustomerArchive;
import org.openedit.store.InventoryItem;
import org.openedit.store.Product;
import org.openedit.store.Store;
import org.openedit.store.StoreTestCase;
import org.openedit.store.customer.Address;
import org.openedit.store.customer.Customer;
import org.openedit.store.modules.CartModule;
import org.openedit.store.modules.OrderModule;
import org.openedit.store.orders.Order;
import org.openedit.store.orders.OrderSet;

import com.openedit.WebPageRequest;
//import org.openedit.store.modules.OrderModule;

public class OrderSetTest  extends StoreTestCase {

	// Store this line for later!
	// OrderModule orderModule = (OrderModule) getFixture().getModuleManager().getModule("StoreOrderModule");

	@Test
	public void testOrderSetBasicOrder() throws Exception {
		Store store = getStore();
		CartModule cartModule = (CartModule) getFixture().getModuleManager().getModule("CartModule");
		WebPageRequest context = getFixture().createPageRequest("/store/index.html");
		Cart cart = cartModule.getCart(context);
		cart.setCustomer(new Customer(context.getUser()));

		Product product1 = setUpProduct("abcd", "abcd1", 10, 5.0);
		store.saveProduct(product1);

		Product product2 = setUpProduct("defg", "defg1", 10, 10.0);
		store.saveProduct(product2);
		
		CustomerArchive archive = getStore().getCustomerArchive();
		Customer customer = archive.createNewCustomer("1006","sdfdsf");
		assertNotNull(customer);
		customer.setFirstName("Bob");
		customer.setLastName("Smith");
		
		customer.setShippingAddress(setupAddress1());
		customer.setBillingAddress(setupAddress2());
		
		archive.saveCustomer(customer);
		
		OrderSet orderSet = new OrderSet();

		CartItem item = new CartItem();
		item.setProduct(product1);
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
		order.setId("test0001");
		order.setCustomer(customer);
		order.setCart(cart);
		
		assertEquals("ON", order.getCustomer().getShippingAddress().getState());
		assertEquals("ON", order.getCustomer().getBillingAddress().getState());
		
		orderSet.addOrder(order);
		assertEquals(1, orderSet.getNumberOfOrders());

		order = null;
		order = orderSet.getOrder("test0001");
		assertEquals(2, order.getCart().getItems().size());

		Product testProduct1 = order.getCartItemByProductID("abcd").getProduct();
		assertNotNull(testProduct1);
		assertEquals(5.0, testProduct1.getYourPrice().doubleValue());
		
	}
	
	@Test
	public void testOrderSetRecalculateOrder() throws Exception {

		Store store = getStore();
		CartModule cartModule = (CartModule) getFixture().getModuleManager().getModule("CartModule");
		WebPageRequest context = getFixture().createPageRequest("/store/index.html");
		Cart cart = cartModule.getCart(context);
		cart.setCustomer(new Customer(context.getUser()));

		Product product1 = setUpProduct("abcd", "abcd1", 10, 5.0);
		store.saveProduct(product1);

		Product product2 = setUpProduct("defg", "defg1", 10, 10.0);
		store.saveProduct(product2);
		
		CustomerArchive archive = getStore().getCustomerArchive();
		Customer customer = archive.createNewCustomer("1006","sdfdsf");
		assertNotNull(customer);
		customer.setFirstName("Bob");
		customer.setLastName("Smith");
		
		customer.setShippingAddress(setupAddress1());
		customer.setBillingAddress(setupAddress2());
		
		archive.saveCustomer(customer);
		
		OrderSet orderSet = new OrderSet();

		CartItem item = new CartItem();
		item.setProduct(product1);
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
		order.setId("test0001");
		order.setCustomer(customer);
		order.setCart(cart);
		
		assertEquals("ON", order.getCustomer().getShippingAddress().getState());
		assertEquals("ON", order.getCustomer().getBillingAddress().getState());
		
		orderSet.addOrder(order);
		assertEquals(1, orderSet.getNumberOfOrders());
		
		order = null;
		order = orderSet.getOrder("test0001");
		assertEquals(2, order.getCart().getItems().size());

		orderSet.recalculateOrder(order, store);
		assertEquals(15.00, orderSet.getSubTotal().doubleValue());
		
		order.getCart().removeItem(order.getCartItemByProductID("defg"));
		assertEquals(1, order.getCart().getItems().size());
		orderSet.recalculateOrder(order, store);
		assertEquals(5.00, orderSet.getSubTotal().doubleValue());
		
		item = null;
		item = order.getCartItemByProductID("abcd");
		item.setQuantity(3);
		store.saveOrder(order);
		orderSet.recalculateOrder(order, store);
		assertEquals(15.0, orderSet.getSubTotal().doubleValue());

	}
	
	@Test
	public void testOrderSetOutOfStockProducts() throws Exception {

		Store store = getStore();
		CartModule cartModule = (CartModule) getFixture().getModuleManager().getModule("CartModule");
		WebPageRequest context = getFixture().createPageRequest("/store/index.html");
		Cart cart = cartModule.getCart(context);
		cart.setCustomer(new Customer(context.getUser()));

		Product product1 = setUpProduct("abcd", "abcd1", 10, 5.0);
		store.saveProduct(product1);

		Product product2 = setUpProduct("defg", "defg1", 0, 10.0);
		store.saveProduct(product2);
		
		OrderSet orderSet = new OrderSet();

		CartItem item = new CartItem();
		item.setProduct(product1);
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
		order.setId("test0001");
		
		orderSet.addOrder(order);
		assertEquals(1, orderSet.getNumberOfOrders());
		
		order = null;
		order = orderSet.getOrder("test0001");
		assertNotNull(order);
		assertEquals(2, order.getCart().getItems().size());
		
		//Check for bad Products (0)
		assertEquals(0, orderSet.getBadProductsPerOrder(order).size());
		
		//Check for out of stock products(0);
		Set<String> badStock = orderSet.getOutOfStockPerOrder(order);
		int outOfStockCount = badStock.size();
		assertEquals(1, outOfStockCount);
		assertTrue(badStock.contains("defg"));
			
	}
	
	@Test
	public void testOrderSetMultipleOrders() throws Exception {

		WebPageRequest context = getFixture().createPageRequest("/store/index.html");
		Store store = getStore();
		CartModule cartModule = (CartModule) getFixture().getModuleManager().getModule("CartModule");
		Cart cart = cartModule.getCart(context);

		CustomerArchive archive = getStore().getCustomerArchive();
		Customer customer = archive.createNewCustomer("1006","sdfdsf");
		assertNotNull(customer);
		customer.setFirstName("Bob");
		customer.setLastName("Smith");
		
		customer.setShippingAddress(setupAddress1());
		customer.setBillingAddress(setupAddress2());
		
		archive.saveCustomer(customer);
		cart.setCustomer(customer);

		Product product1 = setUpProduct("abcd", "abcd1", 10, 5.0);
		store.saveProduct(product1);

		Product product2 = setUpProduct("defg", "defg1", 0, 10.0);
		store.saveProduct(product2);
		
		Product product3 = setUpProduct("hijk", "hijk1", 10, 15.0);
		store.saveProduct(product3);

		Product product4 = setUpProduct("lmno", "lmno1", 5, 20.0);
		store.saveProduct(product4);
		
		OrderSet orderSet = new OrderSet();

		CartItem item1 = new CartItem();
		item1.setProduct(product1);
		item1.setQuantity(1);
		item1.setStatus("accepted");
		cart.addItem(item1);

		CartItem item2 = new CartItem();
		item2.setProduct(product2);
		item2.setQuantity(1);
		item2.setStatus("accepted");
		cart.addItem(item2);

		Order order = store.getOrderGenerator().createNewOrder(store, cart);
		order.setProperty("notes", "This is a note");
		order.setId("test0001");
		order.setCart(cart);
		
		orderSet.addOrder(order);
		
		Customer customer2 = archive.createNewCustomer("1010","fuddah");
		assertNotNull(customer2);
		customer2.setFirstName("Tim");
		customer2.setLastName("Tester");
		customer2.setShippingAddress(setupAddress3());
		customer2.setBillingAddress(setupAddress4());
		
		Cart cart2 = new Cart();
		cart2.setCustomer(customer2);
		
		CartItem item3 = new CartItem();
		item3.setProduct(product3);
		item3.setQuantity(1);
		item3.setStatus("accepted");
		cart2.addItem(item3);

		CartItem item4 = new CartItem();
		item4.setProduct(product4);
		item4.setQuantity(1);
		item4.setStatus("accepted");
		cart2.addItem(item4);

		CartItem item5 = new CartItem();
		item5.setProduct(product1);
		item5.setQuantity(1);
		item5.setStatus("accepted");
		cart2.addItem(item5);

		Order order2 = store.getOrderGenerator().createNewOrder(store, cart2);
		order2.setProperty("notes", "This is a note");
		order2.setId("test0002");
		order2.setCart(cart2);

		orderSet.addOrder(order2);
		assertEquals(2, orderSet.getNumberOfOrders());
		
		assertNotNull(orderSet.getOrder("test0001"));
		assertEquals(2, orderSet.getOrder("test0001").getNumItems());
		
		assertNotNull(orderSet.getOrder("test0002"));
		assertEquals(3, orderSet.getOrder("test0002").getNumItems());
		
		//Check for bad Products (0)
		assertEquals(0, orderSet.getBadProductsPerOrder(order).size());
		assertEquals(0, orderSet.getAllBadProducts().size());
		
		//Check for out of stock products(0);
		assertTrue(orderSet.doesOrderHaveOutOfStockProducts(orderSet.getOrder("test0001")));
		assertFalse(orderSet.doesOrderHaveOutOfStockProducts(orderSet.getOrder("test0002")));
		
		assertEquals(1, orderSet.getOutOfStockPerOrder(orderSet.getOrder("test0001")).size());
		assertTrue(orderSet.getOutOfStockPerOrder(orderSet.getOrder("test0001")).contains("defg"));
		assertFalse(orderSet.getOutOfStockPerOrder(orderSet.getOrder("test0001")).contains("abcd"));
		
		//Check for out of stock products(0);
		assertEquals(0, orderSet.getOutOfStockPerOrder(orderSet.getOrder("test0002")).size());
		assertFalse(orderSet.getOutOfStockPerOrder(orderSet.getOrder("test0002")).contains("defg"));
		
		assertEquals(1, orderSet.getAllOutOfStockProductsFromAllOrders().size());
		assertEquals(1, orderSet.getQuantityForProduct(product1));
			
	}
	
	@Test
	public void testOrderSetTestRemovingOrders() throws Exception {

		WebPageRequest context = getFixture().createPageRequest("/store/index.html");
		Store store = getStore();
		CartModule cartModule = (CartModule) getFixture().getModuleManager().getModule("CartModule");
		Cart cart = cartModule.getCart(context);

		CustomerArchive archive = getStore().getCustomerArchive();
		Customer customer = archive.createNewCustomer("1006","sdfdsf");
		assertNotNull(customer);
		customer.setFirstName("Bob");
		customer.setLastName("Smith");
		
		customer.setShippingAddress(setupAddress1());
		customer.setBillingAddress(setupAddress2());
		archive.saveCustomer(customer);
		cart.setCustomer(customer);

		Product product1 = setUpProduct("abcd", "abcd1", 10, 5.0);
		store.saveProduct(product1);

		Product product2 = setUpProduct("defg", "defg1", 0, 10.0);
		store.saveProduct(product2);
		
		OrderSet orderSet = new OrderSet();

		CartItem item1 = new CartItem();
		item1.setProduct(product1);
		item1.setQuantity(1);
		item1.setStatus("accepted");
		cart.addItem(item1);

		CartItem item2 = new CartItem();
		item2.setProduct(product2);
		item2.setQuantity(1);
		item2.setStatus("accepted");
		cart.addItem(item2);

		Order order = store.getOrderGenerator().createNewOrder(store, cart);
		order.setProperty("notes", "This is a note");
		order.setId("test0001");
		order.setCart(cart);
		
		orderSet.addOrder(order);
		
		//Setup 2nd Order
		Product product3 = setUpProduct("hijk", "hijk1", 10, 15.0);
		store.saveProduct(product3);

		Product product4 = setUpProduct("lmno", "lmno1", 0, 20.0);
		store.saveProduct(product4);
		
		Customer customer2 = archive.createNewCustomer("1010","fuddah");
		assertNotNull(customer2);
		customer2.setFirstName("Tim");
		customer2.setLastName("Tester");
		customer2.setShippingAddress(setupAddress3());
		customer2.setBillingAddress(setupAddress4());
		archive.saveCustomer(customer2);
		
		Cart cart2 = new Cart();
		cart2.setCustomer(customer2);
		
		CartItem item3 = new CartItem();
		item3.setProduct(product3);
		item3.setQuantity(1);
		item3.setStatus("accepted");
		cart2.addItem(item3);

		CartItem item4 = new CartItem();
		item4.setProduct(product4);
		item4.setQuantity(1);
		item4.setStatus("accepted");
		cart2.addItem(item4);

		CartItem item5 = new CartItem();
		item5.setProduct(product1);
		item5.setQuantity(1);
		item5.setStatus("accepted");
		cart2.addItem(item5);

		Order order2 = store.getOrderGenerator().createNewOrder(store, cart2);
		order2.setProperty("notes", "This is a note");
		order2.setId("test0002");
		order2.setCart(cart2);

		orderSet.addOrder(order2);
		assertEquals(2, orderSet.getNumberOfOrders());
		
		assertNotNull(orderSet.getOrder("test0001"));
		assertEquals(2, orderSet.getOrder("test0001").getNumItems());
		
		assertNotNull(orderSet.getOrder("test0002"));
		assertEquals(3, orderSet.getOrder("test0002").getNumItems());
		
		//Check for bad Products (0)
		assertEquals(0, orderSet.getBadProductsPerOrder(order).size());
		assertEquals(0, orderSet.getAllBadProducts().size());
		
		//Check for out of stock products(0);
		assertTrue(orderSet.doesOrderHaveOutOfStockProducts(orderSet.getOrder("test0001")));
		assertTrue(orderSet.doesOrderHaveOutOfStockProducts(orderSet.getOrder("test0002")));
		
		assertEquals(1, orderSet.getOutOfStockPerOrder(orderSet.getOrder("test0001")).size());
		assertTrue(orderSet.getOutOfStockPerOrder(orderSet.getOrder("test0001")).contains("defg"));
		assertFalse(orderSet.getOutOfStockPerOrder(orderSet.getOrder("test0001")).contains("abcd"));
		
		//Check for out of stock products(0);
		assertEquals(1, orderSet.getOutOfStockPerOrder(orderSet.getOrder("test0002")).size());
		assertTrue(orderSet.getOutOfStockPerOrder(orderSet.getOrder("test0002")).contains("lmno"));
		assertFalse(orderSet.getOutOfStockPerOrder(orderSet.getOrder("test0002")).contains("hijk"));
		
		assertEquals(2, orderSet.getAllOutOfStockProductsFromAllOrders().size());

		//Test Remove Empty Orders
		orderSet.getOrder("test0001").getCart().getItemForProduct("abcd").setQuantity(0);
		orderSet.removeItemFromOrder(product2.getId(), order, store);
		orderSet.removeEmptyOrders(store);
		assertEquals(1, orderSet.getNumberOfOrders());
		assertEquals(1, orderSet.getRemovedOrders().size());
		assertNotNull(orderSet.getOrder("test0002"));
		assertNull(orderSet.getOrder("test0001"));
		
		//Recalculate the orderSet
		orderSet.recalculateAll(store);
		assertEquals(40.0, orderSet.getSubTotal().doubleValue());
		assertTrue(orderSet.doesOrderSetHaveOutOfStockProducts());

		orderSet.getOrder("test0002").getCart().getItemForProduct("lmno").setQuantity(0);
		orderSet.removeEmptyOrders(store);
		assertEquals(1, orderSet.getNumberOfOrders());
		assertEquals(1, orderSet.getRemovedOrders().size());
		assertNotNull(orderSet.getOrder("test0002"));
		
		orderSet.recalculateAll(store);
		assertEquals(20.0, orderSet.getSubTotal().doubleValue());
		assertFalse(orderSet.doesOrderSetHaveOutOfStockProducts());
		
	}
	
	@Test
	public void testOrderModule() throws Exception {
		
		Store store = getStore();
		CartModule cartModule = (CartModule) getFixture().getModuleManager().getModule("CartModule");
		OrderModule orderModule = (OrderModule) getFixture().getModuleManager().getModule("StoreOrderModule");
		WebPageRequest context = getFixture().createPageRequest("/store/index.html");
		Cart cart = cartModule.getCart(context);
		cart.setCustomer(new Customer(context.getUser()));

		Product product1 = setUpProduct("abcd", "abcd1", 10, 5.0);
		store.saveProduct(product1);

		Product product2 = setUpProduct("defg", "defg1", 10, 10.0);
		store.saveProduct(product2);
		
		CustomerArchive archive = getStore().getCustomerArchive();
		Customer customer = archive.createNewCustomer("1006","sdfdsf");
		assertNotNull(customer);
		customer.setFirstName("Bob");
		customer.setLastName("Smith");
		
		customer.setShippingAddress(setupAddress1());
		customer.setBillingAddress(setupAddress2());
		
		archive.saveCustomer(customer);
		
		OrderSet orderSet = new OrderSet();

		CartItem item = new CartItem();
		item.setProduct(product1);
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
		order.setId("test0001");
		order.setCustomer(customer);
		order.setCart(cart);
		
		orderSet.addOrder(order);
		assertEquals(1, orderSet.getNumberOfOrders());
		
		context.putSessionValue("orderset", orderSet);
		
		orderSet = null;
		orderModule.loadCurrentOrderSet(context);
		orderSet = (OrderSet) context.getSessionValue("orderset");
		assertEquals(1, orderSet.getNumberOfOrders());
		
		order = null;
		order = orderSet.getOrder("test0001");
		context.setRequestParameter("order", order.getId());
		context.setRequestParameter("product", product2.getId());
		orderModule.removeStockFromOrder(context);
		
		orderModule.loadCurrentOrderSet(context);
		order = null;
		order = orderSet.getOrder("test0001");
		Cart testCart = order.getCart();
		assertEquals(1, testCart.getItems().size());
		
	}
	
	@Test
	public void testOrderModuleRemoveItem() throws Exception {
		
		Store store = getStore();
		CartModule cartModule = (CartModule) getFixture().getModuleManager().getModule("CartModule");
		OrderModule orderModule = (OrderModule) getFixture().getModuleManager().getModule("StoreOrderModule");
		WebPageRequest context = getFixture().createPageRequest("/store/index.html");
		Cart cart = cartModule.getCart(context);
		cart.setCustomer(new Customer(context.getUser()));

		Product product1 = setUpProduct("abcd", "abcd1", 10, 5.0);
		store.saveProduct(product1);

		Product product2 = setUpProduct("defg", "defg1", 10, 10.0);
		store.saveProduct(product2);
		
		CustomerArchive archive = getStore().getCustomerArchive();
		Customer customer = archive.createNewCustomer("1006","sdfdsf");
		assertNotNull(customer);
		customer.setFirstName("Bob");
		customer.setLastName("Smith");
		
		customer.setShippingAddress(setupAddress1());
		customer.setBillingAddress(setupAddress2());
		
		archive.saveCustomer(customer);
		
		OrderSet orderSet = new OrderSet();

		CartItem item = new CartItem();
		item.setProduct(product1);
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
		order.setId("test0001");
		order.setCustomer(customer);
		order.setCart(cart);
		
		orderSet.addOrder(order);
		assertEquals(1, orderSet.getNumberOfOrders());
		
		context.putSessionValue("orderset", orderSet);
		
		orderSet = null;
		orderModule.loadCurrentOrderSet(context);
		orderSet = (OrderSet) context.getSessionValue("orderset");
		assertEquals(1, orderSet.getNumberOfOrders());
		
		order = null;
		order = orderSet.getOrder("test0001");
		context.setRequestParameter("product", product2.getId());
		orderModule.removeItem(context);
		
		orderModule.loadCurrentOrderSet(context);
		order = null;
		order = orderSet.getOrder("test0001");
		Cart testCart = order.getCart();
		assertEquals(1, testCart.getItems().size());
		assertNull(testCart.getItemForProduct(product2.getId()));

		//Add the item back into the order
		CartItem item3 = new CartItem();
		item3.setProduct(product2);
		item3.setQuantity(1);
		item3.setStatus("accepted");
		testCart.addItem(item2);
		order.addItem(item3);
		assertEquals(2, testCart.getItems().size());
		assertNotNull(testCart.getItemForProduct(product2.getId()));

		context = getFixture().createPageRequest("/ecommerce/remove.html");
		getFixture().getEngine().executePathActions(context);
		
	}

	@Test
	public void testOrderModuleWebRequests() throws Exception {
		
		Store store = getStore();
		CartModule cartModule = (CartModule) getFixture().getModuleManager().getModule("CartModule");
		OrderModule orderModule = (OrderModule) getFixture().getModuleManager().getModule("StoreOrderModule");
		WebPageRequest context = getFixture().createPageRequest("/store/index.html");
		Cart cart = cartModule.getCart(context);
		cart.setCustomer(new Customer(context.getUser()));

		Product product1 = setUpProduct("abcd", "abcd1", 10, 5.0);
		store.saveProduct(product1);

		Product product2 = setUpProduct("defg", "defg1", 10, 10.0);
		store.saveProduct(product2);
		
		CustomerArchive archive = getStore().getCustomerArchive();
		Customer customer = archive.createNewCustomer("1006","sdfdsf");
		assertNotNull(customer);
		customer.setFirstName("Bob");
		customer.setLastName("Smith");
		
		customer.setShippingAddress(setupAddress1());
		customer.setBillingAddress(setupAddress2());
		
		archive.saveCustomer(customer);
		
		OrderSet orderSet = new OrderSet();

		CartItem item = new CartItem();
		item.setProduct(product1);
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
		order.setId("test0001");
		order.setCustomer(customer);
		order.setCart(cart);
		
		orderSet.addOrder(order);
		assertEquals(1, orderSet.getNumberOfOrders());
		
		context.putSessionValue("orderset", orderSet);
		
		orderSet = null;
		orderModule.loadCurrentOrderSet(context);
		orderSet = (OrderSet) context.getSessionValue("orderset");
		assertEquals(1, orderSet.getNumberOfOrders());
		
		order = null;
		order = orderSet.getOrder("test0001");
		context = (WebPageRequest) getFixture().createPageRequest("/ecommerce/rogers/orders/orderset/removeitem.html");
		context.putSessionValue("orderset", orderSet);
		context.setRequestParameter("product", product2.getId());
		context.setRequestParameter("order", order.getId());
		getFixture().getEngine().executePathActions(context);
		
		orderModule.loadCurrentOrderSet(context);
		order = null;
		order = orderSet.getOrder("test0001");
		Cart testCart = order.getCart();
		assertEquals(1, testCart.getItems().size());
		assertNull(testCart.getItemForProduct(product2.getId()));
		
	}
	
	private Product setUpProduct(String inID, String inInventoryID, int inQtyInStock, double inPrice) {
		Product product = new Product();
		product.setId(inID);
		product.addInventoryItem(new InventoryItem(inInventoryID));
		product.getInventoryItem(0).setQuantityInStock(inQtyInStock);
		product.addTierPrice(1, createPrice(inPrice));
		return product;
	}
	
	private Address setupAddress1() {
		Address address = new Address();
		address.setAddress1("713 Evergreen Terrace");
		address.setAddress2("Attn:  Bob");
		address.setCity("Bridgetown");
		address.setState("ON");
		address.setZipCode("L5A A1A");
		return address;
	}
	
	private Address setupAddress2() {
		Address address = new Address();
		address.setAddress1("713 Evergreen Terrace");
		address.setAddress2("");
		address.setCity("Your Town");
		address.setState("ON");
		address.setZipCode("L1A A1A");
		return address;
	}
	
	private Address setupAddress3() {
		Address address = new Address();
		address.setAddress1("123 Bubba Lane");
		address.setAddress2("");
		address.setCity("Your Town");
		address.setState("ON");
		address.setZipCode("L1A A1A");
		return address;
	}
	
	private Address setupAddress4() {
		Address address = new Address();
		address.setAddress1("312 Your Street");
		address.setAddress2("Apt 2");
		address.setCity("Your Small Town");
		address.setState("ON");
		address.setZipCode("L2A A1A");
		return address;
	}
	
}
