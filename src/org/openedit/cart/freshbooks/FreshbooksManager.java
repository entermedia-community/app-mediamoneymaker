package org.openedit.cart.freshbooks;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.openedit.data.BaseSearcher;
import org.openedit.store.CartItem;
import org.openedit.store.CreditPaymentMethod;
import org.openedit.store.PurchaseOrderMethod;
import org.openedit.store.TaxRate;
import org.openedit.store.customer.Address;
import org.openedit.store.customer.Customer;
import org.openedit.store.orders.Order;

import com.openedit.users.User;
import com.openedit.util.XmlUtil;

public class FreshbooksManager {
	private static final Log log = LogFactory.getLog(BaseSearcher.class);

	protected String fieldUrl;
	protected String fieldToken;
	protected XmlUtil fieldXmlUtil;
	protected SimpleDateFormat format = new SimpleDateFormat(
			"yyyy-MM-dd hh:mm:ss");

	public XmlUtil getXmlUtil() {
		if (fieldXmlUtil == null) {
			fieldXmlUtil = new XmlUtil();
		}
		return fieldXmlUtil;
	}

	public void setXmlUtil(XmlUtil inXmlUtil) {
		fieldXmlUtil = inXmlUtil;
	}

	public String getToken() {
		return fieldToken;
	}

	public void setToken(String inToken) {
		fieldToken = inToken;
	}

	public String getUrl() {
		return fieldUrl;
	}

	public void setUrl(String inUrl) {
		fieldUrl = inUrl;
	}

	public Element getClientList() throws Exception {

		Element root = DocumentHelper.createElement("request");
		root.addAttribute("method", "client.list");
		root.addElement("folder").setText("active");

		Element result = callFreshbooks(root);
		return result;

	}

	public Element getItemList() throws Exception {

		Element root = DocumentHelper.createElement("request");
		root.addAttribute("method", "item.list");
		// root.addElement("folder").setText("active");
		root.addElement("per_page").setText("100");
		Element result = callFreshbooks(root);
		return result;

	}
	
	public Element getGatewayList() throws Exception {
		Element root = DocumentHelper.createElement("request");
		root.addAttribute("method", "gateway.list");
		root.addElement("autobill_capable").setText("1");
		return callFreshbooks(root);
	}

	public Element callFreshbooks(Element element) throws HttpException,
			IOException, DocumentException {

		HttpClient client = new HttpClient();

		client.getParams().setAuthenticationPreemptive(true);
		URL url = new URL(getUrl());
		client.getState().setCredentials(
				new AuthScope(url.getHost(), 443, AuthScope.ANY_REALM),
				new UsernamePasswordCredentials(getToken(), ""));

		PostMethod method = new PostMethod(getUrl());

		method.setContentChunked(false);
		method.setDoAuthentication(true);
		method.setFollowRedirects(false);

		method.setRequestEntity(new StringRequestEntity(element.asXML(),
				"text/xml", "utf-8"));
		method.getParams().setContentCharset("utf-8");

		log.info("sending: " + element.asXML());

		int result = client.executeMethod(method);
		String responsebody = method.getResponseBodyAsString();
		log.info("response:  " + responsebody);
		Document document = DocumentHelper.parseText(responsebody);
		Element root1 = document.getRootElement();
		return root1;

	}

	public Element callFreshbooks(String xml) throws HttpException,
			IOException, DocumentException {

		HttpClient client = new HttpClient();

		client.getParams().setAuthenticationPreemptive(true);
		URL url = new URL(getUrl());
		client.getState().setCredentials(
				new AuthScope(url.getHost(), 443, AuthScope.ANY_REALM),
				new UsernamePasswordCredentials(getToken(), ""));

		PostMethod method = new PostMethod(getUrl());

		method.setContentChunked(false);
		method.setDoAuthentication(true);
		method.setFollowRedirects(false);

		method.setRequestEntity(new StringRequestEntity(xml, "text/xml",
				"utf-8"));
		method.getParams().setContentCharset("utf-8");

		log.info("sending: " + xml);

		int result = client.executeMethod(method);

		String responsebody = method.getResponseBodyAsString();
		log.info("result: " + responsebody);

		Document document = DocumentHelper.parseText(responsebody);
		Element root1 = document.getRootElement();

		return root1;

	}

	public void createInvoice(Order inOrder, User inUser) throws Exception {
		Customer customer = inOrder.getCustomer();

		if (inUser.get("freshbooksid") == null) {
			createCustmer(new Customer(inUser), inOrder.getBillingAddress(),
					null);
		}
		Element root = DocumentHelper.createElement("request");
		root.addAttribute("method", "invoice.create");
		Element invoice = root.addElement("invoice");
		invoice.addElement("client_id").setText(inUser.get("freshbooksid"));
		invoice.addElement("number").setText(inOrder.getId());
		invoice.addElement("status").setText("draft");
		invoice.addElement("date").setText(format.format(inOrder.getDate()));

		if (inOrder.getPaymentMethod() instanceof PurchaseOrderMethod) {
			PurchaseOrderMethod method = (PurchaseOrderMethod) inOrder
					.getPaymentMethod();
			if (method.getPoNumber() != null) {
				invoice.addElement("po_number").setText(method.getPoNumber());
			}
		}
		Element lines = invoice.addElement("lines");
		for (Iterator iterator = inOrder.getItems().iterator(); iterator
				.hasNext();) {
			CartItem item = (CartItem) iterator.next();
			Element line = lines.addElement("line");
			line.addElement("name").setText(item.getProduct().getId());
			line.addElement("description").setText(item.getProduct().getName());
			line.addElement("unit_cost").setText(
					item.getYourPrice().toShortString());
			line.addElement("quantity").setText(
					String.valueOf(item.getQuantity()));

			int count = 1;
			for (Iterator iterator2 = inOrder.getTaxes().keySet().iterator(); iterator2
					.hasNext();) {
				TaxRate rate = (TaxRate) iterator2.next();
				line.addElement("tax" + count + "_name")
						.setText(rate.getName());
				double percent = rate.getFraction().doubleValue() * 100;
				line.addElement("tax" + count + "_percent").setText(
						String.valueOf(percent));
				count++;
				if (count > 2) {
					break;
				}
			}

			// line.addEle
		}

		Element shipping = lines.addElement("line");
		shipping.addElement("name").setText("shipping");
		shipping.addElement("description").setText("Shipping");
		shipping.addElement("quantity").setText("1");
		if (inOrder.getTotalShipping() != null) {
			shipping.addElement("unit_cost").setText(
					inOrder.getTotalShipping().toShortString());
		}
		int count = 1;
		for (Iterator<?> iterator2 = inOrder.getTaxes().keySet().iterator(); iterator2
				.hasNext();) {
			TaxRate rate = (TaxRate) iterator2.next();
			shipping.addElement("tax" + count + "_name")
					.setText(rate.getName());
			double percent = rate.getFraction().doubleValue() * 100;
			shipping.addElement("tax" + count + "_percent").setText(
					String.valueOf(percent));
			count++;
			if (count > 2) {
				break;
			}
		}

		Element result = callFreshbooks(root);
		if (result.attributeValue("status").equals("ok")) {
			String clientid = result.elementText("invoice_id");
			inOrder.setProperty("freshbooksid", clientid);
			log.info("result was " + result.asXML());

		} else {
			log.info("result was " + result.asXML());
		}

	}
	
	public void createOneTimeInvoice(Order inOrder, FreshbookInstructions instructions) throws Exception {
		
	}

	public void createRecurring(Order inOrder,FreshbookInstructions inStructions) throws Exception {
		Customer customer = inOrder.getCustomer();

		if (inOrder.getCustomer().get("freshbooksid") == null) {
			createCustmer(inOrder.getCustomer(), inOrder.getBillingAddress(),
					null);
		}
		
		Element root = DocumentHelper.createElement("request");
		root.addAttribute("method", "recurring.create");
		Element invoice = root.addElement("recurring");
		invoice.addElement("client_id").setText(customer.get("freshbooksid"));
		invoice.addElement("po_number").setText(inOrder.getId());
//		invoice.addElement("status").setText("draft");
//		invoice.addElement("date").setText(format.format(inOrder.getDate()));
		invoice.addElement("occurrences").setText("1");
//		invoice.addElement("frequency").setText("weekly");//inStructions.getFrequency());	
		invoice.addElement("send_email").setText(inStructions.getSendEmail());
		invoice.addElement("send_snail_mail").setText(inStructions.getSendSnailMail());
		if (inOrder.get("notes")!=null) invoice.addElement("notes").setText(inOrder.get("notes"));
		
		CreditPaymentMethod creditCard = (CreditPaymentMethod) inOrder
				.getPaymentMethod();
		
		
		
		Element autobill = invoice.addElement("autobill");
		autobill.addElement("gateway_name").setText(inStructions.getGateway());
		Element carddetails = autobill.addElement("card");
		carddetails.addElement("number").setText(creditCard.getCardNumber());
		carddetails.addElement("name").setText(inOrder.getCustomer().getName());
		Element expiration = carddetails.addElement("expiration");
		expiration.addElement("month").setText(creditCard.getExpirationMonthString());
		expiration.addElement("year").setText(creditCard.getExpirationYearString());
		
		
		
		
		
		Element lines = invoice.addElement("lines");
		for (Iterator iterator = inOrder.getItems().iterator(); iterator
				.hasNext();) {
			CartItem item = (CartItem) iterator.next();
//			if (Boolean.parseBoolean(item.getProduct().get("recurring"))) {

				Element line = lines.addElement("line");
				line.addElement("name").setText(item.getProduct().getId());
				line.addElement("description").setText(
						item.getProduct().getName());
				line.addElement("unit_cost").setText(
						item.getYourPrice().toShortString());
				line.addElement("quantity").setText(
						String.valueOf(item.getQuantity()));

				int count = 1;
				for (Iterator iterator2 = inOrder.getTaxes().keySet()
						.iterator(); iterator2.hasNext();) {
					TaxRate rate = (TaxRate) iterator2.next();
					line.addElement("tax" + count + "_name").setText(
							rate.getName());
					double percent = rate.getFraction().doubleValue() * 100;
					line.addElement("tax" + count + "_percent").setText(
							String.valueOf(percent));
					count++;
					if (count > 2) {
						break;
					}
				}
//			}

			// line.addEle
		}

		

		Element result = callFreshbooks(root);
		if (result.attributeValue("status").equals("ok")) {
			String clientid = result.elementText("invoice_id");
			inOrder.setProperty("freshbooksid", clientid);
			log.info("result was " + result.asXML());
		} else {
			log.info("result was " + result.asXML());
			
			/*
			 * <error>Invalid value for field 'send_snail_mail'. Value must be 0 or 1.</error>
[19:22:42.105]   <code>40063</code>
[19:22:42.105]   <field>send_snail_mail</field>
			 */
			String error = result.elementText("error");
			String code = result.elementText("code");
			inStructions.setErrorMessage(error);
			inStructions.setErrorCode(code);
		}

	}

	public boolean createCustmer(Customer inUser, Address inAddress,
			List inContacts) throws Exception {
		Element root = DocumentHelper.createElement("request");
		root.addAttribute("method", "client.create");
		Element client = root.addElement("client");
		client.addElement("first_name").setText(inUser.getFirstName());
		client.addElement("last_name").setText(inUser.getLastName());
		client.addElement("email").setText(inUser.getEmail());
		// client.addElement("username").setText(inUser.getUserName());
		if (inUser.get("organization") == null) {
			client.addElement("organization").setText(
					inUser.getUser().toString());
		}
		if (inContacts != null) {

			// TODO: implement contacts
			// Element contacts = client.addElement("contacts");

		}
		if (inUser.getPhone1() != null) {
			client.addElement("work_phone").setText(inUser.getUserName());
		}

		if (inAddress != null) {
			client.addElement("p_street1").setText(inAddress.getAddress1());
			if (inAddress.getAddress2() != null) {
				client.addElement("p_street2").setText(inAddress.getAddress2());
			}
			client.addElement("p_city").setText(inAddress.getCity());
			client.addElement("p_state").setText(inAddress.getState());
			client.addElement("p_country").setText(inAddress.getCountry());
			client.addElement("p_code").setText(inAddress.getZipCode());

		}
		Element result = callFreshbooks(root);
		if (result.attributeValue("status").equals("ok")) {
			String clientid = result.elementText("client_id");
			inUser.setProperty("freshbooksid", clientid);
			log.info("result was " + result.asXML());
			return true;

		} else {
			log.info("result was " + result.asXML());
			return false;
		}

	}

	
	public boolean emailInvoice(String invoiceid, String subject, String message) throws Exception{
		Element root = DocumentHelper.createElement("request");
		root.addAttribute("method", "invoice.sendByEmail");
		root.addElement("invoice_id").setText(invoiceid);
		if(subject != null){
		root.addElement("subject").setText("subject");
		}
		
		if(message != null){
			root.addElement("message").setText("message");
		}
		Element result = callFreshbooks(root);
		
		if (result.attributeValue("status").equals("ok")) {
			return true;

		} else {
			log.info("result was " + result.asXML());
			return false;

		}
		
		// client.addElement("username").setText(inUser.getUserName());

	}
	
}
