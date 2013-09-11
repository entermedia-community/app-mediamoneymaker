/*
 * Created on Oct 12, 2004
 */
package com.openedit.store.gateway;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.openedit.cart.freshbooks.FreshbookInstructions;
import org.openedit.cart.freshbooks.FreshbooksManager;
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
		util.setToken("1486563a14b69f71a3ab62d2f9851ec6");
		util.setUrl("https://shawnbest-billing.freshbooks.com/api/2.1/xml-in");
		
		Customer customer = new Customer(new FileSystemUser());
		customer.getUser().setId("testuser");
		customer.setFirstName("Shawn");
		customer.setLastName("Test");
		customer.setEmail("TEST@shawnbest.com");

		Address address = new Address();
		address.setAddress1("123 Anywhere St.");
		address.setState("Ontario");//Propercase works
		address.setCity("Collingwood");
		address.setCountry("Canada");//Canada CANADA CAN can
		address.setZipCode("L9Y 3T7");
		
		util.createCustmer(customer, address, null);
	}
	
	public void testCreateRecurringInvoice() throws Exception {
//		String clientid = "34008";
		
		Customer customer = new Customer(new FileSystemUser());
		customer.getUser().setId("testuser1222");
		customer.setFirstName("Sean");
		customer.setLastName("Martin");
		customer.setEmail("smartin@shawnbest.com");

		Address address = new Address();
		address.setAddress1("123 Anywhere St.");
		address.setState("Ontario");//Propercase works
		address.setCity("Collingwood");
		address.setCountry("Canada");//Propercase works
		address.setZipCode("L9Y 3T7");
		
		customer.setBillingAddress(address);
		
//		customer.getUser().setProperty("freshbooksid", clientid);
		
		// createRecurring(Order inOrder,FreshbookInstructions inStructions)
		FreshbooksManager util = new FreshbooksManager();
		util.setToken("1486563a14b69f71a3ab62d2f9851ec6");
		util.setUrl("https://shawnbest-billing.freshbooks.com/api/2.1/xml-in");
		util.setGateway("beanstream");
		
		FreshbookInstructions instructions = new FreshbookInstructions();
	    instructions.setSendEmail("1");//send email 
	    instructions.setSendSnailMail("0");// don't send post mail
	    
	    
	    Store store = getStore();
		CartModule cartModule = (CartModule) getFixture().getModuleManager().getModule("CartModule");

		WebPageRequest context = getFixture().createPageRequest();
		Cart cart = cartModule.getCart(context);
		cart.setCustomer(customer);
		cart.addItem(createCheapToyCartItem());
		
		Order order = store.getOrderGenerator().createNewOrder(store, cart);
		order.setProperty("notes", "This is a note");
	    
		CreditPaymentMethod paymentMethod = new CreditPaymentMethod();
		CreditCardType type = new CreditCardType();
		type.setName("Visa");
		paymentMethod.setCreditCardType(type);
		paymentMethod.setCardNumber("4030000010001234");
		paymentMethod.setCardVerificationCode("123");
		paymentMethod.setExpirationMonth(10);
		paymentMethod.setExpirationYear(2015);
		
		order.setPaymentMethod(paymentMethod);
	    
	    util.createRecurring(order, instructions);
		
		
		
	}

	/**
	 * @throws Exception
	 */
	public void testListClients() throws Exception {
		FreshbooksManager util = new FreshbooksManager();
		util.setToken("1486563a14b69f71a3ab62d2f9851ec6");
		util.setUrl("https://shawnbest-billing.freshbooks.com/api/2.1/xml-in");
		Element list = util.getClientList();
		assertNotNull(list);
		System.out.println(list.asXML());
	
	}
	
	public void xxtestListItems() throws Exception {
		FreshbooksManager util = new FreshbooksManager();
		util.setToken("1486563a14b69f71a3ab62d2f9851ec6");
		util.setUrl("https://shawnbest-billing.freshbooks.com/api/2.1/xml-in");
		Element list = util.getItemList();
		assertNotNull(list);
		System.out.println(list.asXML());
	
	}
	
	public void xxtestGatewayList() throws Exception {
		FreshbooksManager util = new FreshbooksManager();
		util.setToken("1486563a14b69f71a3ab62d2f9851ec6");
		util.setUrl("https://shawnbest-billing.freshbooks.com/api/2.1/xml-in");
		Element list = util.getGatewayList();
		assertNotNull(list);
		System.out.println(list.asXML());
	}
	
	
	public void xxtestCreateInvoice() throws Exception {
		FreshbooksManager util = new FreshbooksManager();
		util.setToken("1486563a14b69f71a3ab62d2f9851ec6");
		util.setUrl("https://shawnbest-billing.freshbooks.com/api/2.1/xml-in");
		
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