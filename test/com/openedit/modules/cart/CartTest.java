/*
 * Created on Mar 3, 2004
 *
 */
package com.openedit.modules.cart;

import java.util.Iterator;
import java.util.List;

import org.openedit.money.Money;
import org.openedit.store.Cart;
import org.openedit.store.CartItem;
import org.openedit.store.InventoryItem;
import org.openedit.store.Option;
import org.openedit.store.Price;
import org.openedit.store.PriceSupport;
import org.openedit.store.Product;
import org.openedit.store.ShippingMethod;
import org.openedit.store.Store;
import org.openedit.store.StoreException;
import org.openedit.store.StoreTestCase;
import org.openedit.store.adjustments.SaleAdjustment;
import org.openedit.store.customer.Address;
import org.openedit.store.customer.Customer;
import org.openedit.store.modules.CartModule;
import org.openedit.store.orders.BaseOrderGenerator;
import org.openedit.store.orders.Order;
import org.openedit.store.orders.OrderGenerator;
import org.openedit.store.shipping.WeightBasedShippingMethod;

import com.openedit.WebPageRequest;

/**
 * @author dbrown
 * 
 */
public class CartTest extends StoreTestCase {
	
	public CartTest(String arg0) {
		super(arg0);
	}

	public void testUpdateItemQuantity() throws Exception {
		CartModule cartModule = (CartModule) getStaticFixture().getModuleManager()
				.getModule("CartModule");

		WebPageRequest context = getStaticFixture().createPageRequest(
				"/store/index.html");
		Store store = cartModule.getStore(context);
		assertNotNull(store);

		final String RED_ITEM_SKU = "2001";
		final String BLACK_ITEM_SKU = "2002";

		context.setRequestParameter("productid", "2");
		context.setRequestParameter("color", "red");
		context.setRequestParameter("size", "NB");
		cartModule.updateCart(context);

		Cart cart = (Cart) context.getPageValue("cart");
		assertNotNull(cart);
		assertEquals(1, cart.getNumItems());

		CartItem firstItem = (CartItem) cart.getItemIterator().next();
		assertEquals(RED_ITEM_SKU, firstItem.getSku());
		firstItem.setQuantity(3);

		// This should swap out the item
		WebPageRequest updatecontext = getStaticFixture().createPageRequest(
				"/store/index.html");
		updatecontext.putSessionValue(store.getCatalogId() + "cart", cart);

		updatecontext.setRequestParameter("reloadcart", "true");
		updatecontext.setRequestParameter("productid.1", "2");
		updatecontext.setRequestParameter("quantity.1", "5");
		updatecontext.setRequestParameter("color.1", "black");
		updatecontext.setRequestParameter("size.1", "large"); // change the
																// size
		cartModule.updateCart(updatecontext);

		assertEquals(1, cart.getNumItems());
		Iterator iter = cart.getItemIterator();
		firstItem = (CartItem) iter.next();
		assertEquals(BLACK_ITEM_SKU, firstItem.getSku());
		assertEquals(5, firstItem.getQuantity());

		updatecontext.setRequestParameter("remove.1", "x");
		cartModule.updateCart(updatecontext);

		assertEquals(0, cart.getNumItems());
	}

	public void testUpdateNoSizeNoColor() throws Exception {
		CartModule cartModule = (CartModule) getStaticFixture().getModuleManager()
				.getModule("CartModule");

		WebPageRequest context = getStaticFixture().createPageRequest(
				"/store/index.html");

		Store store = cartModule.getStore(context);
		assertNotNull(store);

		context.setRequestParameter("productid", "3");
		// context.setRequestParameter( "color", "red" );
		// context.setRequestParameter( "size", "NB" );
		cartModule.updateCart(context);

		Cart cart = (Cart) context.getPageValue("cart");
		assertNotNull(cart);
		assertEquals(1, cart.getNumItems());

		final String NOSIZE_ITEM_SKU = "abdefg";

		CartItem firstItem = (CartItem) cart.getItemIterator().next();
		assertEquals(NOSIZE_ITEM_SKU, firstItem.getSku());
		firstItem.setQuantity(3);

		WebPageRequest updatecontext = getStaticFixture().createPageRequest(
				"/store/index.html");
		updatecontext.putSessionValue(store.getCatalogId() + "cart", cart);

		// This should swap out the item
		updatecontext.setRequestParameter("reloadcart", "true");
		updatecontext.setRequestParameter("productid.1", "3");
		updatecontext.setRequestParameter("quantity.1", "5");
		// context.setRequestParameter( "color.1", "black" );
		// context.setRequestParameter( "size.1", "large" ); //change the size
		cartModule.updateCart(updatecontext);

		assertEquals(1, cart.getNumItems());
		firstItem = (CartItem) cart.getItemIterator().next();
		assertEquals(NOSIZE_ITEM_SKU, firstItem.getSku());
		assertEquals(5, firstItem.getQuantity());

		updatecontext.setRequestParameter("remove.1", "x");
		cartModule.updateCart(updatecontext);

		assertEquals(0, cart.getNumItems());
	}

	public void testUpdateSpecialRequest() throws Exception {
		CartModule cartModule = (CartModule) getStaticFixture().getModuleManager()
				.getModule("CartModule");

		WebPageRequest context = getStaticFixture().createPageRequest(
				"/store/index.html");
		Store store = cartModule.getStore(context);
		assertNotNull(store);

		context.setRequestParameter("productid", "3");
		context
				.setRequestParameter("option.specialrequest",
						"My special order");
		cartModule.updateCart(context);

		Cart cart = (Cart) context.getPageValue("cart");
		assertNotNull(cart);
		assertEquals(1, cart.getNumItems());

		final String ITEM_SKU = "abdefg";

		CartItem firstItem = (CartItem) cart.getItems().get(0);
		assertEquals("My special order", firstItem.getOption("specialrequest")
				.getValue());
		assertEquals(ITEM_SKU, firstItem.getSku());
		assertEquals(1, firstItem.getQuantity());

		firstItem.setQuantity(3);

		WebPageRequest updatecontext = getStaticFixture().createPageRequest("/store/index.html");
		updatecontext.putSessionValue(store.getCatalogId() + "cart", cart);
		updatecontext.setRequestParameter("reloadcart", "true");
		// This should swap out the item
		updatecontext.setRequestParameter("productid.1", "3");
		updatecontext.setRequestParameter("quantity.1", "5");
		updatecontext.setRequestParameter("option.1.specialrequest",
				"My special order");
		cartModule.updateCart(updatecontext);

		// TODO: Check price
		assertEquals(1, cart.getNumItems());
		firstItem = (CartItem) cart.getItems().get(0);
		assertEquals(ITEM_SKU, firstItem.getSku());
		assertEquals(5, firstItem.getQuantity());
		assertEquals("My special order", firstItem.getOption("specialrequest")
				.getValue());
	}

	public void testSameProductTwoColors() throws Exception {
		CartModule cartModule = (CartModule) getStaticFixture().getModuleManager()
				.getModule("CartModule");

		WebPageRequest context = getStaticFixture().createPageRequest();
		Store store = cartModule.getStore(context);
		assertNotNull(store);

		context.setRequestParameter("productid", "2");
		context.setRequestParameter("size", "1");
		context.setRequestParameter("color", "red");
		cartModule.updateCart(context);
		Cart cart = (Cart) context.getPageValue("cart");

		context.setRequestParameter("productid", "2");
		context.setRequestParameter("size", "1");
		context.setRequestParameter("color", "blue");
		cartModule.updateCart(context);

		assertNotNull(cart);
		assertEquals(2, cart.getNumItems());

		CartItem item = (CartItem) cart.getItems().get(0);
		assertEquals("2003", item.getSku());
		assertTrue(item.isColor("red"));

		item = (CartItem) cart.getItems().get(1);
		assertEquals("2004", item.getSku());

		assertEquals("blue", item.getColor().getValue());

		assertTrue(item.isColor("blue"));
	}

	// TODO give us an API to list a matrix of size color combos
	public void testListColors() throws Exception {
		Store store = getStore();
		Product two = store.getProduct("2");
		// two.getI();
	}

	public void testShippingAndOptions() throws Exception {
		CartModule cartModule = (CartModule) getStaticFixture().getModuleManager()
				.getModule("CartModule");

		WebPageRequest context = getStaticFixture().createPageRequest(
				"/store/index.html");
		Store store = cartModule.getStore(context);
		assertNotNull(store);
		assertTrue(store.getAllShippingMethods().size() > 0);

		context.setRequestParameter("productid", "1");
		context.setRequestParameter("option.domainname", "http://domain.com");
		cartModule.updateCart(context);

		Cart cart = (Cart) context.getPageValue("cart");
		assertNotNull(cart);
		assertEquals(cart.getNumItems(), 1);

		ShippingMethod standard = store.findShippingMethod("1");
		ShippingMethod nextDay = store.findShippingMethod("3");
		ShippingMethod over200 = store.findShippingMethod("4");
		assertTrue(cart.getAvailableShippingMethods().contains(standard));
		assertTrue(cart.getAvailableShippingMethods().contains(nextDay));
		assertFalse(cart.getAvailableShippingMethods().contains(over200));

		Money costs = standard.getCost(cart);
		assertEquals(new Money("6.00"), costs);

		Money shippingCostForNextDay = nextDay.getCost(cart);
		assertEquals(new Money("2.00"), shippingCostForNextDay);

		costs = over200.getCost(cart);
		assertEquals(new Money("0.00"), costs);

		CartItem firstItem = (CartItem) cart.getItemIterator().next();
		firstItem.setQuantity(12);

		Option option = firstItem.getOption("domainname");
		assertNotNull(option);
		assertEquals(option.getValue(), "http://domain.com");

		cart.setCustomer(createCustomer());
		context.putSessionValue(store.getCatalogId() + "cart", cart);
		// test that inventory item's in stock quantity is decreased by cart
		// item's quantity
		int quantityInStock = firstItem.getInventoryItem().getQuantityInStock();

		Order order = cartModule.processOrder(context);
		assertTrue(order.getOrderState().isOk());
		CartItem item = (CartItem) order.getItems().get(0);

		assertEquals(item.getInventoryItem().getQuantityInStock(),
				quantityInStock - firstItem.getQuantity());
		assertEquals(cart.getShippingMethod(), over200);

		option = item.getOption("domainname");
		assertNotNull(option);
		assertEquals(option.getValue(), "http://domain.com");

	}

	public void testShipByWeight() throws Exception {

		CartModule cartModule = (CartModule) getStaticFixture().getModuleManager()
				.getModule("CartModule");

		WebPageRequest context = getStaticFixture().createPageRequest();
		Store store = cartModule.getStore(context);
		assertNotNull(store);
		assertTrue(store.getAllShippingMethods().size() > 0);

		context.setRequestParameter("productid", "2");
		context.setRequestParameter("color", "red");
		context.setRequestParameter("size", "NB");
		cartModule.updateCart(context);

		Cart cart = (Cart) context.getPageValue("cart");
		assertNotNull(cart);
		assertTrue(cart.getNumItems() > 0);

		WeightBasedShippingMethod weight = (WeightBasedShippingMethod) store
				.findShippingMethod("5");

		// test that percentage shipping cost is added in properly
		Money shippingCostForNextDay = weight.getCost(cart);
		assertEquals(new Money("3.00"), shippingCostForNextDay);

	}
	
	public void testCoupon() throws Exception {
		// THis test depends on search working so the reindex must have happened
		// already
		// And the product 3 should be marked as available
		CartModule cartModule = (CartModule) getStaticFixture().getModuleManager()
				.getModule("CartModule");

		WebPageRequest context = getStaticFixture().createPageRequest();
		Cart cart = cartModule.getCart(context);

		context.setRequestParameter("productid", "2");
		context.setRequestParameter("color", "red");
		context.setRequestParameter("size", "NB");
		cartModule.updateCart(context);
		assertEquals(1, cart.getNumItems());

		// get sub total
		assertEquals(19.95, cart.getSubTotal().doubleValue(), .01);
		context.setRequestParameter("couponcode", "abdefg");
		cartModule.addCoupon(context);
		assertEquals(14.95, cart.getSubTotal().doubleValue(), .01);

	
	
	
	}
	
	
	
	public void testPercentageCoupon() throws Exception {
		// THis test depends on search working so the reindex must have happened
		// already
		// And the product 3 should be marked as available
		CartModule cartModule = (CartModule) getStaticFixture().getModuleManager()
				.getModule("CartModule");

		WebPageRequest context = getStaticFixture().createPageRequest();
		Cart cart = cartModule.getCart(context);

		context.setRequestParameter("productid", "2");
		context.setRequestParameter("color", "red");
		context.setRequestParameter("size", "NB");
		cartModule.updateCart(context);
		assertEquals(1, cart.getNumItems());

		// get sub total
		assertEquals(19.95, cart.getSubTotal().doubleValue(), .01);
		context.setRequestParameter("couponcode", "fifteen");
		cartModule.addCoupon(context);
		assertEquals(16.9575, cart.getSubTotal().doubleValue(), .01);
		cartModule.addCoupon(context);
		assertEquals(16.9575, cart.getSubTotal().doubleValue(), .01);
		
	
	
	}


	public void testHandling() throws Exception {
		CartModule cartModule = (CartModule) getStaticFixture().getModuleManager()
				.getModule("CartModule");

		WebPageRequest context = getStaticFixture().createPageRequest();
		Store store = cartModule.getStore(context);
		assertNotNull(store);

		context.setRequestParameter("productid", "2");

		cartModule.updateCart(context);

		Cart cart = (Cart) context.getPageValue("cart");
		assertNotNull(cart);
		assertTrue(cart.getNumItems() > 0);

		ShippingMethod standard = store.findShippingMethod("1");
		assertNotNull(standard);
		ShippingMethod threeDay = store.findShippingMethod("2");
		assertNotNull(threeDay);

		CartItem firstItem = (CartItem) cart.getItemIterator().next();
		firstItem.setQuantity(2);

		cart.setShippingMethod(standard);
		assertEquals(new Money("36.00"), cart.getTotalShipping());
		assertFalse(cart.isAdditionalShippingCosts());

		cart.setShippingMethod(threeDay);
		Money totalShipping = cart.getTotalShipping();
		assertEquals(new Money("62.00"), totalShipping);
		assertTrue(cart.isAdditionalShippingCosts());
	}

	public void testCustomPrice() throws Exception {
		// Verify that we can change the price of a product when buying it
		// if and only if the customPrice flag is set.

		final double INVOICE_PRICE = 15557.81;

		CartModule cartModule = (CartModule) getStaticFixture().getModuleManager()
				.getModule("CartModule");

		WebPageRequest context = getStaticFixture().createPageRequest();
		Store store = cartModule.getStore(context);
		assertNotNull(store);

		context.setRequestParameter("productid", "1");
		context.setRequestParameter("price", String.valueOf(INVOICE_PRICE));
		cartModule.updateCart(context);

		Cart cart = (Cart) context.getPageValue("cart");
		assertNotNull(cart);
		assertTrue(cart.getNumItems() > 0);
		assertEquals(new Money("19.95"), cart.getSubTotal());
		cart.removeAllItems();

		Product product = store.getProduct("1");
		product.setCustomPrice(true);
		context.setRequestParameter("productid", "1");
		context.setRequestParameter("price", String.valueOf(INVOICE_PRICE));
		cartModule.updateCart(context);
		product.setCustomPrice(false);
		assertEquals(new Money(INVOICE_PRICE), cart.getSubTotal());
	}

	public void XXXtestBackOrder() throws StoreException {
		CartItem cheapToy = createCheapToyCartItem();
		// test that out of stock condition is properly indicated by cart item
		cheapToy
				.setQuantity(cheapToy.getInventoryItem().getQuantityInStock() + 1);
		assertTrue(cheapToy.isBackOrdered());
	}

	public void testCart() throws StoreException {
		Product collegeTextbookProduct = new Product("college textbook");
		final double COLLEGE_TEXTBOOK_PRICE = 94.00;
		collegeTextbookProduct.addTierPrice(1,
				createPrice(COLLEGE_TEXTBOOK_PRICE));
		InventoryItem item = new InventoryItem();
		item.setQuantityInStock(10);
		item.setSku("textbook");
		collegeTextbookProduct.addInventoryItem(item);
		CartItem citem = new CartItem();
		citem.setInventoryItem(item);
		citem.setQuantity(1);
		Cart cart = new Cart();
		cart.addItem(citem);
		assertEquals(new Money(94.00), cart.getTotalPrice());

		CartItem cheapToy = createCheapToyCartItem();
		cart.addItem(cheapToy);
		Money m1 = cart.getTotalPrice();
		Money m2 = new Money(103.95);
		assertEquals(m2, m1);

		cart.removeItem(cheapToy); // removed

		Product brandNewCarProduct = new Product("brand new car");
		final double BRAND_NEW_CAR_PRICE = 25000.0;
		Price newCarPrice = createPrice(BRAND_NEW_CAR_PRICE);
		brandNewCarProduct.addTierPrice(1, newCarPrice);
		item = new InventoryItem();
		item.setQuantityInStock(10);
		item.setSku("car");
		brandNewCarProduct.addInventoryItem(item);

		CartItem citem2 = new CartItem();
		citem2.setInventoryItem(item);
		citem2.setQuantity(1);

		cart.addItem(citem2);
		assertEquals(new Money(94.0 + 25000.0), cart.getTotalPrice());

		// Now add an option to the new car
		Option powerWindows = new Option();
		Price powerWindowsPrice = createPrice(123.0);
		powerWindows.addTierPrice(1, powerWindowsPrice);
		powerWindows.setId("powerwindows");
		citem2.addOption(powerWindows);

		assertEquals(new Money(94.0 + 25000.0 + 123.0), cart.getTotalPrice());

		cart.removeAllItems();
		assertEquals(cart.getTotalPrice(), Money.ZERO);
	}

	public void testStartCheckOut() throws Exception {
		CartModule cartModule = (CartModule) getStaticFixture().getModuleManager()
				.getModule("CartModule");
		WebPageRequest context = getStaticFixture().createPageRequest();
		context.setRequestParameter("productid", "1");
		// Where did this come from?
		// assertNull( context.getRequestParameter( "size" ) );

		cartModule.updateCart(context);

		Cart cart = (Cart) context.getPageValue("cart");
		assertNotNull(cart);
		assertTrue(cart.getNumItems() == 1);
		CartItem item = (CartItem) cart.getItems().get(0);
		assertEquals(item.getProduct().getId(), "1");
		CartItem cheapToy = createCheapToyCartItem();
		cart.addItem(cheapToy);
		assertEquals(cart.getItems().indexOf(cheapToy), 1);
		cart.removeItem(item);
		assertEquals(cart.getItems().indexOf(cheapToy), 0);
	}

	/**
	 * Test live web environment store.xml should have
	 * 
	 * @throws Exception
	 */

	public void testApplyAdjustments() throws Exception {
		Product brandNewCar = new Product("brand new car");

		brandNewCar.addTierPrice(1, createPrice(25000));
		brandNewCar.addTierPrice(2, createPrice(23000));
		brandNewCar.addTierPrice(4, createPrice(22000));

		InventoryItem item = new InventoryItem();
		item.setSku("car");
		brandNewCar.addInventoryItem(item);
		CartItem car = new CartItem();
		car.setQuantity(2);
		car.setInventoryItem(item);

		Cart cart = new Cart();
		cart.addItem(car);
		assertNotNull(cart);

		Money newSubtotal = cart.getSubTotal();
		assertEquals(46000D, newSubtotal.doubleValue(), .001);
		car.setQuantity(3);

		newSubtotal = cart.getSubTotal();
		assertEquals(69000D, newSubtotal.doubleValue(), .001);

		car.setQuantity(5);
		newSubtotal = cart.getSubTotal();
		assertEquals(110000, newSubtotal.doubleValue(), .001);

		SaleAdjustment adjustment = new SaleAdjustment();
		adjustment.setPercentDiscount(20);
		cart.addAdjustment(adjustment);

		assertEquals(88000, cart.getSubTotal().doubleValue(), 0.001);

	}

	/**
	 * Should send an email
	 * 
	 * @throws Exception
	 */
	public void testCheckOut() throws Exception {
		CartModule cartModule = (CartModule) getStaticFixture().getModuleManager()
				.getModule("CartModule");

		WebPageRequest context = getStaticFixture().createPageRequest();
		context.setRequestParameter("productid", "1");
		context.setRequestParameter("size", "large");

		cartModule.updateCart(context);

		context.setRequestParameter("firstName", "John");
		context.setRequestParameter("lastName", "Doe");
		context.setRequestParameter("email", "cburkey@einnovation.com");
		context.setRequestParameter("shipping.address1.value",
				"49 East Slum St.");
		context
				.setRequestParameter("shipping.address2.value",
						"Apartment 2000");
		context.setRequestParameter("shipping.city.value", "Cincinnati");
		context.setRequestParameter("shipping.state.value", "OH");
		context.setRequestParameter("shipping.country.value", "USA");
		context.setRequestParameter("shipping.zipCode.value", "45202");
		cartModule.createCustomer(context);
		cartModule.updateCustomer(context);

		context.setRequestParameter("billing.address1.value", "New Address");

		context.setRequestParameter("cardType", "Visa");
		context.setRequestParameter("cardNumber", "4245123456780909");
		context.setRequestParameter("expirationMonth", "12");
		context.setRequestParameter("expirationYear", "2007");
		cartModule.updateCustomer(context);
		cartModule.saveCreditPaymentMethodData(context);
		Cart cart = (Cart) context.getPageValue("cart");
		assertEquals("New Address", cart.getCustomer().getBillingAddress()
				.getAddress1());

		Money total = cart.getTotalPrice();
		// assertEquals("$21.35", total.toString());
		// cartModule.processOrder( context );
	}

	public void testCreateNewCustomer() throws Exception {
		// First lets create a customer
		CartModule cartModule = (CartModule) getStaticFixture().getModuleManager()
				.getModule("CartModule");

		WebPageRequest context = getStaticFixture().createPageRequest();

		// Now create a customer account
		context = getStaticFixture().createPageRequest();
		context.setRequestParameter("firstName", "John");
		context.setRequestParameter("lastName", "Doe");
		context.setRequestParameter("email", "cburkey@einnovation.com");
		context.setRequestParameter("shipping.address1.value",
				"49 East Slum St.");
		context
				.setRequestParameter("shipping.address2.value",
						"Apartment 2000");
		context.setRequestParameter("shipping.city.value", "Cincinnati");
		context.setRequestParameter("shipping.state.value", "OH");
		context.setRequestParameter("shipping.country.value", "USA");
		context.setRequestParameter("shipping.zipCode.value", "45202");
		cartModule.createCustomer(context);
		cartModule.updateCustomer(context);

		Cart cart = cartModule.getCart(context);
		Customer customer = cart.getCustomer();
		assertNotNull(customer.getShippingAddress());
		assertTrue(customer.getShippingAddress().getCity().equals("Cincinnati"));
		assertNotNull(customer.getUserName());

		String id = customer.getUserName();

		// At this point the customer is on the hard drive and we have an ID

		// Now Try to order some stuff using only the ID we just created
		context = getStaticFixture().createPageRequest();
		// Now start ordering stuff
		context.setRequestParameter("productid", "1");
		context.setRequestParameter("size", "large");
		cartModule.updateCart(context);

		context.setRequestParameter("customerId", id);
		cartModule.loadCustomer(context);

		cart = cartModule.getCart(context);
		customer = cart.getCustomer();
		assertNotNull(customer);
		assertNotNull(customer.getShippingAddress().getCity());
		assertTrue(customer.getShippingAddress().getCity().equals("Cincinnati"));

		context.setRequestParameter("cardType", "Visa");
		context.setRequestParameter("cardNumber", "4245123456780909");
		context.setRequestParameter("expirationMonth", "12");
		context.setRequestParameter("expirationYear", "2007");
		cartModule.saveCreditPaymentMethodData(context);
		Money total = cart.getTotalPrice();
		// assertEquals("$21.35", total.toString());

	}

	public void testAddressManagement() throws Exception {
		// First lets create a customer
		CartModule cartModule = (CartModule) getStaticFixture().getModuleManager()
				.getModule("CartModule");

		WebPageRequest context = getStaticFixture().createPageRequest();

		// Now create a customer account
		context = getStaticFixture().createPageRequest();
		Customer customer = cartModule.loadCustomer(context);
		List list = customer.getAddressList();
	    assertEquals(0, list.size() );

		context.setRequestParameter("prefix", "home");

		context.setRequestParameter("home.address1.value", "49 East Slum St.");
		context.setRequestParameter("home.address2.value", "Apartment 2000");
		context.setRequestParameter("home.city.value", "Toronto");
		context.setRequestParameter("home.state.value", "OH");
		context.setRequestParameter("home.country.value", "USA");
		context.setRequestParameter("home.zipCode.value", "45202");

		cartModule.saveAddress(context);

		customer = cartModule.loadCustomer(context);

		list = customer.getAddressList();
	    assertEquals(1, list.size() );

		Address address = customer.getAddress("home");
		assertNotNull(address);
		assertEquals("Toronto", address.getCity());

		 cartModule.removeAddress(context);
		 address = customer.getAddress("home");
		 assertNull(address);

	}

	public void testNextOrderNumber() throws Exception {
		OrderGenerator orderGenerator = new BaseOrderGenerator();
		String orderNumber = orderGenerator.nextOrderNumber(getStore());
		assertEquals(10, orderNumber.length());
		String nextOrderNumber = orderGenerator.nextOrderNumber(getStore());
		System.out.println(orderNumber + ", " + nextOrderNumber);
		assertFalse(nextOrderNumber.equals(orderNumber));
	}

	public void testAddsRequiredOptions() throws Exception {
		CartModule cartModule = (CartModule) getStaticFixture().getModuleManager()
				.getModule("CartModule");

		WebPageRequest context = getStaticFixture().createPageRequest();
		Store store = cartModule.getStore(context);
		assertNotNull(store);

		context.setRequestParameter("productid", "handling");
		cartModule.updateCart(context);

		Cart cart = (Cart) context.getPageValue("cart");
		assertNotNull(cart);
		assertEquals(1, cart.getNumItems());

		CartItem firstItem = (CartItem) cart.getItems().get(0);
		assertTrue(firstItem.hasOption("handling"));
		assertEquals(new Money("28.00"), cart.getTotalPrice());
	}
	
	
	public void testDuplicateProducts() throws Exception {
		CartModule cartModule = (CartModule) getStaticFixture().getModuleManager()
				.getModule("CartModule");

		WebPageRequest context = getStaticFixture().createPageRequest();		
		context.setRequestParameter("productid", "1");
		context.setRequestParameter("size", "large");
		context.setRequestParameter("quantity", "1");
		cartModule.updateCart(context);
		
		context.setRequestParameter("productid", "1");
		context.setRequestParameter("quantity", "1");
		context.setRequestParameter("size", "small");
		cartModule.updateCart(context);
		
		Cart cart = cartModule.getCart(context);
		assertEquals(cart.getItems().size(),2 );
		
		
		//Now reload cart
		context = getStaticFixture().createPageRequest();
		context.setRequestParameter("productid.1","1");
		context.setRequestParameter("quantity.1","44");
		context.setRequestParameter("size.1","large");
		
		context.setRequestParameter("productid.2","1");
		context.setRequestParameter("quantity.2","2");
		context.setRequestParameter("size.2","small");

		context.setRequestParameter("reloadcart", "true");
		cartModule.updateCart(context);
		assertEquals(cart.getItems().size(),2 );

		cart = cartModule.getCart(context);
		CartItem  item1 = (CartItem) cart.getItems().get(0);
		CartItem  item2 = (CartItem) cart.getItems().get(1);
		assertEquals(item1.getQuantity(), 44);
		assertEquals(item2.getQuantity(), 2);
		
		
	}
	
	
	
	
	public void testItemProperties() throws Exception
	{
		CartModule cartModule = (CartModule) getStaticFixture().getModuleManager().getModule("CartModule");
		WebPageRequest context = getStaticFixture().createPageRequest();
		Store store = cartModule.getStore(context);
		assertNotNull(store);
		
		Product testProduct = new Product();
		testProduct.setId("test");
				
		// start new
		InventoryItem inventoryItem = new InventoryItem();
		PriceSupport priceSupport = new PriceSupport();
		Price price = new Price();
		price.setRetailPrice(new Money(100));
		priceSupport.addTierPrice(1, price);
		inventoryItem.setPriceSupport(priceSupport);
		testProduct.addInventoryItem(inventoryItem);
		// end new
		
		store.saveProduct(testProduct);		
		assertNotNull(store.getProduct("template"));
		
		context.setRequestParameter("productid", "template");
		context.setRequestParameter("property.testprop", "true");
		
		cartModule.clearCart(context);
		cartModule.updateCart(context);
		
		assertEquals(1, cartModule.getCart(context).getItems().size());
		
		CartItem item = (CartItem) cartModule.getCart(context).getItems().get(0);
		assertNotNull(item);
		Product product = item.getInventoryItem().getProduct();
		assertNotNull(product);
		assertEquals("true", item.get("testprop"));
		
	}
	
	/**
	 * @throws Exception
	 */
	public void testRemoveShippingAddress() throws Exception
	{
		CartModule cartModule = (CartModule) getStaticFixture().getModuleManager()
		.getModule("CartModule");
		WebPageRequest context = getStaticFixture().createPageRequest();
		
		Customer customer = cartModule.loadCustomer(context);
		
		context.setRequestParameter("prefix", "shipping");
		context.setRequestParameter("shipping.address1.value", "address 1");
		cartModule.saveAddress(context);
		
		customer = cartModule.loadCustomer(context);
		Address shippingAddress = customer.getShippingAddress();
		assertNotNull( shippingAddress );
		assertEquals( "address 1", shippingAddress.getAddress1() );
		
		//remove the shipping address
		context.setRequestParameter("prefix", "shipping");
		cartModule.removeAddress(context);
		
		customer = cartModule.getCart(context).getCustomer();
		Address newShippingAddress = customer.getShippingAddress();
		
		assertNull(newShippingAddress.getAddress1());
		
	}
}
