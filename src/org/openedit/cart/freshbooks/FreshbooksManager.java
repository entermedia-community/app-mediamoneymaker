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
	
	public static final String FRESHBOOKS_ID = "freshbooksid";
	public static final String FRESHBOOKS_RECURRING_ID = "recurringid";
	
	private static final Log log = LogFactory.getLog(BaseSearcher.class);

	protected String fieldUrl;
	protected String fieldToken;
	protected String fieldGateway;
	protected XmlUtil fieldXmlUtil;
	protected SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

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
	
	public String getGateway() {
		return fieldGateway;
	}

	public void setGateway(String inGateway) {
		fieldGateway = inGateway;
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
		Element root = document.getRootElement();
		return root;

	}

	public void createInvoice(Order inOrder, User inUser) throws Exception {
		Customer customer = inOrder.getCustomer();

		if (!hasFreshbooksId(customer)) {
			createCustmer(new Customer(inUser), inOrder.getBillingAddress(),
					null);
		}
		Element root = DocumentHelper.createElement("request");
		root.addAttribute("method", "invoice.create");
		Element invoice = root.addElement("invoice");
		invoice.addElement("client_id").setText(inUser.get(FRESHBOOKS_ID));
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
		
//		Address shippingAddress = inOrder.getShippingAddress();
//		if (shippingAddress!=null){
//			
//		}

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
			inOrder.setProperty(FRESHBOOKS_ID, clientid);
			log.info("result was " + result.asXML());

		} else {
			log.info("result was " + result.asXML());
		}

	}
	
	public boolean emailInvoice(String inInvoiceId, String inSubject, String inMessage) throws Exception{
		Element root = DocumentHelper.createElement("request");
		root.addAttribute("method", "invoice.sendByEmail");
		root.addElement("invoice_id").setText(inInvoiceId);
		if(inSubject != null){
			root.addElement("subject").setText(inSubject);
		}
		if(inMessage != null){
			root.addElement("message").setText(inMessage);
		}
		Element result = callFreshbooks(root);
		return isStatusOk(result);
	}
	
	/**
	 * 
	 * performs all the logic required to process an order
	 * 1. setting up client if required 
	 * 2. creating a single invoice with auto-payment
	 * 3. if necessary, setting up a recurring payment
	 * 
	 * @param inOrder
	 * @param inStatus
	 * @throws Exception
	 */
	public void processOrder(Order inOrder, FreshbooksStatus inStatus) throws Exception {
		//deal with client
		Customer customer = inOrder.getCustomer();
		if (!hasFreshbooksId(customer)) {
			boolean success = createCustmer(customer,customer.getBillingAddress(),null);
			if (!success){
				throw new Exception("Customer "+customer.getUser()+" cannot be added to Freshbooks");
			}
		}
		//create an invoice set to autop-pay immediately
		//these should include all items, including those that are recurring
		createAutoPayInvoice(inOrder,inStatus);
		if (inStatus.getInvoiceId() != null){
			String invoiceId = inStatus.getInvoiceId();
			inOrder.setProperty(FRESHBOOKS_ID, invoiceId);
		} else {// at this point, completed in error, so don't continue to recurring request
			return;
		}
		//if a recurring order needs to be created, do that as well
		if (inOrder.containsRecurring()){
			createRecurringInvoice(inOrder,inStatus);
			//now update the recurring order id on the order
			if (inStatus.getRecurringId() != null){
				inOrder.setProperty(FRESHBOOKS_RECURRING_ID, inStatus.getRecurringId());
			} else {
				//TODO fix???
				// at this point, not sure what to do. log it???
				log.warn("recurring order request could not be completed");
			}
		}
	}
	
	private void createAutoPayInvoice(Order inOrder, FreshbooksStatus inStatus) throws Exception {
		Element root = DocumentHelper.createElement("request");
		root.addAttribute("method", "recurring.create");
		Element invoice = root.addElement("recurring");
		populateOrderElements(invoice,false,inOrder,inStatus);
		populateCartItemElements(invoice,false,inOrder,inStatus);
		Element result = callFreshbooks(root);
		if (isStatusOk(result)) {
			//since the invoice went through, we don't need 
			//the one-off recurring profile anymore so delete it
			String oneoff = result.elementText("recurring_id");
			if (oneoff!=null){
				cancelRecurringInvoice(oneoff);//not a show stopper if it doesn't go through
			}
			//set the invoice id
			String invoiceId = result.elementText("invoice_id");
			inStatus.setInvoiceId(invoiceId);//set the invoice
			//check the status
			Element element = queryInvoice(invoiceId);
			String status = getInvoiceStatus(element);
			if (isInvoicePaid(element)){
				inStatus.setInvoiceStatus(status);
			} else {
				if (inStatus.isBlocking()){
					int repeats = inStatus.getMaximumQueryRepeat();
					final long delay = inStatus.getDelayBetweenQueries();
					for (int i=0; i < repeats-1; i++){
						//start with a delay
						try{
							Thread.sleep(delay);
						}catch (Exception e){}
						//query invoice status
						Element e = queryInvoice(invoiceId);
						status = getInvoiceStatus(e);
						if (isInvoicePaid(e)){
							inStatus.setInvoiceStatus(status);
							return;
						}
					}
					//finished for loop so now update inStatus with last known invoice status
					inStatus.setInvoiceStatus(status);
					//all good but invoice did not go through during the period of time we initially queried
					//so need to trigger an async processor
					log.warn("payment for "+invoiceId+" has not been paid after "+repeats+" query attempts, current status is "+status);
					
					//TODO handle asynchronous queries!!!
					log.warn("&&&&&&&& need to handle asynchronous queries &&&&&&&");
				} else {
					inStatus.setInvoiceStatus(status);
					//TODO handle asynchronous queries!!!
					log.warn("&&&&&&&& need to handle asynchronous queries &&&&&&&");
				}
			}
		} else {
			String error = result.elementText("error");
			String code = result.elementText("code");
			inStatus.setErrorMessage(error);
			inStatus.setErrorCode(code);
		}
	}
	
	private void populateOrderElements(Element inInvoice, boolean isRecurring, Order inOrder, FreshbooksStatus inStatus) throws Exception {
		Customer customer = inOrder.getCustomer();
		inInvoice.addElement("client_id").setText(customer.get(FRESHBOOKS_ID));
		if (isRecurring){
			inInvoice.addElement("date").setText(format.format(inStatus.getFirstRecurringInvoiceDate()));
			inInvoice.addElement("frequency").setText(inStatus.getFrequency());
			inInvoice.addElement("occurrences").setText(inStatus.getOccurrences());
		} else {
			inInvoice.addElement("occurrences").setText("1");//one time only
		}
		inInvoice.addElement("send_email").setText(inStatus.getSendEmail());
		inInvoice.addElement("send_snail_mail").setText(inStatus.getSendSnailMail());
		if (inOrder.get("notes")!=null) {
			inInvoice.addElement("notes").setText(inOrder.get("notes"));
		}
		
		CreditPaymentMethod creditCard = (CreditPaymentMethod) inOrder.getPaymentMethod();
		
		Element autobill = inInvoice.addElement("autobill");
		autobill.addElement("gateway_name").setText(getGateway());
		Element carddetails = autobill.addElement("card");
		carddetails.addElement("number").setText(creditCard.getCardNumber());
		carddetails.addElement("name").setText(inOrder.getCustomer().getName());
		Element expiration = carddetails.addElement("expiration");
		expiration.addElement("month").setText(creditCard.getExpirationMonthString());
		expiration.addElement("year").setText(creditCard.getExpirationYearString());
	}
	
	private void populateCartItemElements(Element inInvoice, boolean includeRecurring, Order inOrder, FreshbooksStatus inStatus) throws Exception {
		Element lines = inInvoice.addElement("lines");
		for (Iterator<?> iterator = inOrder.getItems().iterator(); iterator.hasNext();) {
			CartItem item = (CartItem) iterator.next();
			boolean include = false;
			if ( (includeRecurring && Boolean.parseBoolean(item.getProduct().get("recurring"))) || !includeRecurring) {
				include = true;
			}
			if (include) {
				Element line = lines.addElement("line");
				line.addElement("name").setText(item.getProduct().getId());
				line.addElement("description").setText(item.getProduct().getName());
				line.addElement("unit_cost").setText(item.getYourPrice().toShortString());
				line.addElement("quantity").setText(String.valueOf(item.getQuantity()));

				int count = 1;
				for (Iterator<?> iterator2 = inOrder.getTaxes().keySet().iterator(); iterator2.hasNext();) {
					TaxRate rate = (TaxRate) iterator2.next();
					String name = rate.getName() == null ? "Tax #"+count : rate.getName();//do only when tax has no name
					line.addElement("tax" + count + "_name").setText(name);
					double percent = rate.getFraction().doubleValue() * 100;
					line.addElement("tax" + count + "_percent").setText(String.valueOf(percent));
					count++;
					//not sure why we cut it off here?
//					if (count > 2) {
//						break;
//					}
				}
			}
		}
	}

	public void createRecurringInvoice(Order inOrder,FreshbooksStatus inStatus) throws Exception {
		Element root = DocumentHelper.createElement("request");
		root.addAttribute("method", "recurring.create");
		Element invoice = root.addElement("recurring");
		populateOrderElements(invoice,true,inOrder,inStatus);
		populateCartItemElements(invoice,true,inOrder,inStatus);
		Element result = callFreshbooks(root);
		if (isStatusOk(result)) {
			//recurring orders create a recurring profile, with a start date (sometime in the future)
			//the start date is set to the current date + the frequency
			//so we don't actually get an invoice_id when we initially create it
			//all we're looking for is a status OK
			//and the recurring_id number, which we can use to cancel the recurring profile at a later date
			String recurringId = result.element("recurring").element("recurring_id").getText();
			inStatus.setRecurringId(recurringId);
		} else {
			String error = result.elementText("error");
			String code = result.elementText("code");
			inStatus.setErrorMessage(error);
			inStatus.setErrorCode(code);
		}
	}
	
	public boolean isRecurringInvoiceActive(String inRecurringId) throws Exception{
		Element root = DocumentHelper.createElement("request");
		root.addAttribute("method", "recurring.get");
		root.addElement("recurring_id").setText(inRecurringId);

		Element result = callFreshbooks(root);
		if (isStatusOk(result)){
			String stopped = result.element("recurring").element("stopped").getText();
			//0 = active, 1 = not active
			return (stopped!=null && stopped.equals("0"));
		}
		return false;
	}
	
	public boolean cancelRecurringInvoice(Order inOrder) throws Exception{
		boolean success = false;
		if (isRecurring(inOrder)){
			String recurringid = inOrder.get(FRESHBOOKS_RECURRING_ID);
			success = cancelRecurringInvoice(recurringid);
			if (success){
				inOrder.setProperty(FRESHBOOKS_RECURRING_ID,"");//remove recurring_id from inOrder
			}
		}
		return success;
	}
	
	public boolean cancelRecurringInvoice(String inRecurringId) throws Exception {
		Element root = DocumentHelper.createElement("request");
		root.addAttribute("method", "recurring.delete");
		root.addElement("recurring_id").setText(inRecurringId);

		Element result = callFreshbooks(root);
		return isStatusOk(result);
	}
	
	public Element queryInvoice(String inInvoiceId) throws Exception {
		Element root = DocumentHelper.createElement("request");
		root.addAttribute("method", "invoice.get");
		root.addElement("invoice_id").setText(inInvoiceId);

		Element result = callFreshbooks(root);
		return result;
	}

	public boolean createCustmer(Customer inCustomer, Address inAddress, List inContacts) throws Exception {
		Element root = DocumentHelper.createElement("request");
		root.addAttribute("method", "client.create");
		Element client = root.addElement("client");
		client.addElement("first_name").setText(inCustomer.getFirstName());
		client.addElement("last_name").setText(inCustomer.getLastName());
		client.addElement("email").setText(inCustomer.getEmail());
		if (inCustomer.get("organization") == null) {
			client.addElement("organization").setText(
					inCustomer.getUser().toString());
		}
		if (inContacts != null) {

			// TODO: implement contacts
			// Element contacts = client.addElement("contacts");
			
			//note: contact fields are optional on freshbooks so not sure if even required

		}
		if (inCustomer.getPhone1() != null) {
			client.addElement("work_phone").setText(inCustomer.getUserName());
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
		if (isStatusOk(result)) {
			String clientid = result.elementText("client_id");
			inCustomer.setProperty(FRESHBOOKS_ID, clientid);
			return true;
		}
		return false;
	}
	
	public Element getInvoice(Order inOrder) throws Exception {
		if (!hasFreshbooksId(inOrder)){
			return null;
		}
		Element root = DocumentHelper.createElement("request");
		root.addAttribute("method", "invoice.get");
		Element invoice = root.addElement("invoice_id");
		invoice.setText(inOrder.get(FRESHBOOKS_ID));
		
		Element result = callFreshbooks(root);
		return result;
	}
	
	public static boolean hasFreshbooksId(Order inOrder) throws Exception {
		return inOrder.get(FRESHBOOKS_ID)!=null;
	}
	
	public static boolean isRecurring(Order inOrder) throws Exception{
		return inOrder.get(FRESHBOOKS_RECURRING_ID)!=null;
	}
	
	public static boolean hasFreshbooksId(Customer inCustomer) throws Exception{
		return inCustomer.get(FRESHBOOKS_ID)!=null;
	}
	
	public boolean isStatusOk(Element inElement) throws Exception{
		return inElement.attributeValue("status").equals("ok");
	}
	
	public static boolean isInvoicePaid(Element inElement) throws Exception {
		String status = getInvoiceStatus(inElement);
		return isInvoicePaid(status);
	}
	
	public static boolean isInvoicePaid(String inStatus) throws Exception{
		return (inStatus!=null && (inStatus.equals("paid") || inStatus.equals("auto-paid")));
	}
	
	public static String getInvoiceStatus(Element inElement) throws Exception{
		String status = inElement.element("invoice").element("status").getText();
		return status;
	}
	
	public static String getOrderStatusFromInvoice(Order inOrder, FreshbooksStatus inStatus) throws Exception{
		// Freshbooks invoice states
		
		// paid: ‘paid’, ‘auto-paid’
		// unpaid: ‘disputed’, ‘sent’, ‘viewed’, ‘retry’, ‘failed’
		// unknown: ‘draft’
		
		//if there's a problem with an online payment, the invoice will be in either a retry or failed state
		String orderStatus = Order.REJECTED;
		if (inStatus.getInvoiceStatus()!=null){
			if (isInvoicePaid(inStatus.getInvoiceStatus())){
				orderStatus = Order.AUTHORIZED;
			} else if (inStatus.getInvoiceStatus().equals("retry")){
				orderStatus = Order.ACCEPTED;//may still go through
			}
		}
		return orderStatus;
	}
	
}
