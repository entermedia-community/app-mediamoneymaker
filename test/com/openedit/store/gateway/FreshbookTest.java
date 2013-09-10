/*
 * Created on Oct 12, 2004
 */
package com.openedit.store.gateway;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.openedit.cart.freshbooks.FreshbooksManager;
import org.openedit.store.StoreTestCase;

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
	
	public void testListItems() throws Exception {
		FreshbooksManager util = new FreshbooksManager();
		util.setToken("1486563a14b69f71a3ab62d2f9851ec6");
		util.setUrl("https://shawnbest-billing.freshbooks.com/api/2.1/xml-in");
		Element list = util.getItemList();
		assertNotNull(list);
		System.out.println(list.asXML());
	
	}
	
	public void testGatewayList() throws Exception {
		FreshbooksManager util = new FreshbooksManager();
		util.setToken("1486563a14b69f71a3ab62d2f9851ec6");
		util.setUrl("https://shawnbest-billing.freshbooks.com/api/2.1/xml-in");
		Element list = util.getGatewayList();
		assertNotNull(list);
		System.out.println(list.asXML());
	}
	
	
	public void testCreateInvoice() throws Exception {
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