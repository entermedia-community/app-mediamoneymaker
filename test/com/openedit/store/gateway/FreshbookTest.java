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
	 * 
	 * @throws Exception
	 */
	
	
	
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