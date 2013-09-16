/*
 * Created on Oct 12, 2004
 */
package com.openedit.store.gateway;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.openedit.Data;
import org.openedit.cart.freshbooks.FreshbooksStatus;
import org.openedit.cart.freshbooks.FreshbooksManager;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.entermedia.MediaArchive;
import org.openedit.money.Money;
import org.openedit.store.Cart;
import org.openedit.store.CartItem;
import org.openedit.store.CreditCardType;
import org.openedit.store.CreditPaymentMethod;
import org.openedit.store.InventoryItem;
import org.openedit.store.Product;
import org.openedit.store.Store;
import org.openedit.store.StoreTestCase;
import org.openedit.store.customer.Address;
import org.openedit.store.customer.Customer;
import org.openedit.store.gateway.EchoOrderProcessor;
import org.openedit.store.modules.CartModule;
import org.openedit.store.orders.Order;

import com.openedit.WebPageRequest;
import com.openedit.users.filesystem.FileSystemUser;

/**
 * @author Ian Miller
 */
public class FreshbookTest extends StoreTestCase {
	

	/**
	 * @param name
	 */
	public FreshbookTest(String name) {
		super(name);
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	public void xxtestCreateCustomer() throws Exception {
		FreshbooksManager util = new FreshbooksManager();
		util.setToken("627d27a09019099d6c8b0e989c1ba8f0");
		util.setUrl("https://learningevolved.freshbooks.com/api/2.1/xml-in");
		
		Customer customer = new Customer(new FileSystemUser());
		customer.getUser().setId("testuser");
		customer.setFirstName("Shawn");
		customer.setLastName("Test");
		customer.setEmail("TEST@shawnbest.com");

		Address address = new Address();
		address.setAddress1("123 Anywhere St.");
		address.setState("Ontario");//Propercase!
		address.setCity("Collingwood");
		address.setCountry("Canada");//Propercase!
		address.setZipCode("L9Y 3T7");
		
		util.createCustmer(customer, address, null);
	}
	
	public void testProcessInvoice() throws Exception {
		
		Customer customer = new Customer(new FileSystemUser());
		customer.getUser().setId("testuser1222");
		customer.setFirstName("Sean");
		customer.setLastName("Martin");
		customer.setEmail("smartin@shawnbest.com");

		Address address = new Address();
		address.setAddress1("123 Anywhere St.");
		address.setState("Ontario");//Propercase!
		address.setCity("Collingwood");
		address.setCountry("Canada");//Propercase!
		address.setZipCode("L9Y 3T7");
		
		customer.setBillingAddress(address);//add address to customer as a billing address
		
		//setup order
		Store store = getStore();
		CartModule cartModule = (CartModule) getFixture().getModuleManager().getModule("CartModule");

		WebPageRequest context = getFixture().createPageRequest();
		Cart cart = cartModule.getCart(context);
		cart.setCustomer(customer);
		
		cart.addItem(createCheapToyCartItem());//add non-recurring
		
		
		CartItem item1 = createRecurringCartItem("Item 1",12.00);
		CartItem item2 = createRecurringCartItem("Item 2",4.00);
		CartItem item3 = createRecurringCartItem("Item 3",6.00);

		//TODO in the UI:
		//frequency and occurrences have to be added dynamically 
		//ie., result of form submit
		//if (item.getProduct().get("recurring") && item.getProduct().get("allowspartialpayments"))
		//	display frequency and occurrences options
		//on submit, before order is processed, 
		// go through and find those fields and add them to the 
		// cart items
		
		item1.setProperty("frequency", "2 weeks");
		item1.setProperty("occurrences", "12");
		
		item2.setProperty("frequency", "monthly");
		item2.setProperty("occurrences", "4");
		
		item3.setProperty("frequency", "monthly");
		item3.setProperty("occurrences", "6");
		cart.addItem(item1);
		cart.addItem(item2);
		cart.addItem(item3);
		
		
		/*
		 * String frequency = item.get("frequency");
				String occurrence = item.get("occurrences");
		 */
		
		Order order = store.getOrderGenerator().createNewOrder(store, cart);
		order.setProperty("notes", "This is a note");
	    
		CreditPaymentMethod paymentMethod = new CreditPaymentMethod();
		CreditCardType type = new CreditCardType();
		type.setName("Visa");
		paymentMethod.setCreditCardType(type);
		paymentMethod.setCardNumber("4030000010001234");//beanstream test credit card#
		//declined: 4003050500040005
		//approved: 4030000010001234
		paymentMethod.setCardVerificationCode("123");
		paymentMethod.setExpirationMonth(10);
		paymentMethod.setExpirationYear(2014);
		
		order.setPaymentMethod(paymentMethod);
		
		FreshbooksManager util = (FreshbooksManager) getFixture().getModuleManager().getBean(getStore().getCatalogId(), "freshbooksManager");
		util.setToken("627d27a09019099d6c8b0e989c1ba8f0");
		util.setUrl("https://learningevolved.freshbooks.com/api/2.1/xml-in");
		util.setGateway("beanstream");
		
		FreshbooksStatus status = new FreshbooksStatus();
	    util.processOrder(order, status);
	    
	    System.out.println(status.toString());
		
	}

	/**
	 * @throws Exception
	 */
	public void xxtestListClients() throws Exception {
		FreshbooksManager util = new FreshbooksManager();
		util.setToken("627d27a09019099d6c8b0e989c1ba8f0");
		util.setUrl("https://learningevolved.freshbooks.com/api/2.1/xml-in");
		Element list = util.getClientList();
		assertNotNull(list);
		System.out.println(list.asXML());
	
	}
	
	public void xxtestListItems() throws Exception {
		FreshbooksManager util = new FreshbooksManager();
		util.setToken("627d27a09019099d6c8b0e989c1ba8f0");
		util.setUrl("https://learningevolved.freshbooks.com/api/2.1/xml-in");
		Element list = util.getItemList();
		assertNotNull(list);
		System.out.println(list.asXML());
	
	}
	
	public void xxtestGatewayList() throws Exception {
		FreshbooksManager util = new FreshbooksManager();
		util.setToken("627d27a09019099d6c8b0e989c1ba8f0");
		util.setUrl("https://learningevolved.freshbooks.com/api/2.1/xml-in");
		Element list = util.getGatewayList();
		assertNotNull(list);
		System.out.println(list.asXML());
	}
	
	
	public void xxtestCreateInvoice() throws Exception {
		FreshbooksManager util = new FreshbooksManager();
		util.setToken("627d27a09019099d6c8b0e989c1ba8f0");
		util.setUrl("https://learningevolved.freshbooks.com/api/2.1/xml-in");
		
		Element root = DocumentHelper.createElement("request");
		root.addAttribute("method", "invoice.create");
		Element invoice = root.addElement("invoice");
		invoice.addElement("client_id").setText("3");
		//invoice.addElement("number")."test");
		Element lines = invoice.addElement("lines");
		Element line1 = lines.addElement("line");
		line1.addElement("name").setText("TEST");
		line1.addElement("description").setText("AGAIN");

		line1.addElement("unit_cost").setText("100");
		line1.addElement("quantity").setText("100");
		line1.addElement("type").setText("item");
		//line1.addElement("item_id").setText("100");
		Element results = util.callFreshbooks(root);
		System.out.println(results.asXML());
		
	
	}

}