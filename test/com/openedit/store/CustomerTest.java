/*
 * Created on Oct 5, 2004
 */
package com.openedit.store;

import org.openedit.store.CustomerArchive;
import org.openedit.store.StoreTestCase;
import org.openedit.store.customer.Customer;

/**
 * @author cburkey
 *
 */
public class CustomerTest extends StoreTestCase
{

	/**
	 * @param inArg0
	 */
	public CustomerTest(String inArg0)
	{
		super(inArg0);
	}
//	public void testFindCustomer() throws Exception
//	{
//		XmlCustomerArchive archive = (XmlCustomerArchive)getStore().getCustomerArchive();
//		
//		List users = archive.findUser("user-name:admin");
//		assertEquals(1,users.size() );
//		
////		users = archive.findUser("Phone1:5135423401");
////		assertEquals(2,users.size() );
////
////		users = archive.findUser("lastName:Burkey");
////		assertEquals(1,users.size() );
////		
////		List hits = archive.findCustomer(User.LAST_NAME_PROPERTY,"BURKEY", 
////			Customer.PHONE1,"513-542-3401");
////		assertNotNull(hits);
////		assertEquals(1,hits.size() );
//	}

	public void xtestEditCustomer() throws Exception
	{
		/**
		 * This test assumes Retail pro conversion worked ok
		 */
		CustomerArchive archive = getStore().getCustomerArchive();
		Customer customer = archive.createNewCustomer("1006","sdfdsf");
		assertNotNull(customer);
		
		//assertEquals("Julia", customer.getFirstName());
		//assertEquals("955 Lakepointe Court", customer.getShippingAddress().getAddress1());

		customer.setFirstName("Bob");
		customer.getBillingAddress().setAddress1("713 Evergreen Terrace");
		customer.getShippingAddress().setAddress1("P.O. Box 500");
		customer.getShippingAddress().setAddress2("Attn:  Bob");
		customer.getShippingAddress().setCity("Bridgetown");
		customer.getShippingAddress().setState("OH");
		customer.getShippingAddress().setZipCode("45212");
		archive.saveCustomer(customer);

		customer = archive.getCustomer("1006");
		assertEquals("Bob", customer.getFirstName());
		assertEquals("713 Evergreen Terrace", customer.getBillingAddress().getAddress1());
		assertEquals("P.O. Box 500", customer.getShippingAddress().getAddress1());
		assertEquals("Attn:  Bob", customer.getShippingAddress().getAddress2());
		assertEquals("Bridgetown", customer.getShippingAddress().getCity());
		assertEquals("OH", customer.getShippingAddress().getState());
		assertEquals("45212", customer.getShippingAddress().getZipCode());
	}

	public void XtestExport() throws Exception
	{
		CustomerArchive archive = getStore().getCustomerArchive();
		Customer c = archive.getCustomer("1008");
		assertNotNull(c); 
		archive.saveAndExportCustomer(c);
		assertEquals("Barbara", c.getFirstName());
		c.setFirstName("Lucy");
		archive.saveAndExportCustomer(c);
		c = archive.getCustomer("1008");
		assertEquals("Lucy", c.getFirstName());
		c.setFirstName("Barbara");
		archive.saveAndExportCustomer(c);
		c = archive.getCustomer("1008");
		assertEquals("Barbara", c.getFirstName());	
		
	}
	//public void testOrderCustomer
}
