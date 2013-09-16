package org.openedit.cart.freshbooks;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import org.openedit.Data;
import org.openedit.data.BaseSearcher;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.money.Money;
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
//	public static final String FRESHBOOKS_RECURRING_ID = "recurringid";
	
	private static final Log log = LogFactory.getLog(BaseSearcher.class);

	protected String fieldUrl;
	protected String fieldToken;
	protected String fieldGateway;
	protected XmlUtil fieldXmlUtil;
	protected SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
	
	//required for searching
	protected SearcherManager fieldSearcherManager;
	protected String fieldCatalogId;
	
	public SearcherManager getSearcherManager() {
		return fieldSearcherManager;
	}
	public void setSearcherManager(SearcherManager fieldSearcherManager) {
		this.fieldSearcherManager = fieldSearcherManager;
	}
	public void setCatalogId(String inCatalogId){
		fieldCatalogId = inCatalogId;
	}
	public String getCatalogId(){
		return fieldCatalogId;
	}

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
		//build all invoices first
		//once complete, send the auto-payment one
		//if it fails then update inStatus with fail, don't process recurring profiles
		//otherwise process the recurring profiles
		ArrayList<CartItem> items = getNonRecurringCartItems(inOrder);
		ArrayList<RecurringProfile> profiles = getRecurringProfiles(inOrder);
		inStatus.setRecurringProfiles(profiles);
		createAutoPayInvoice(inOrder,items,inStatus);
		if (!profiles.isEmpty() && inStatus.getInvoiceId() != null){
			createRecurringInvoices(inOrder,inStatus);
		}
	}
	
	public void createAutoPayInvoice(Order inOrder, ArrayList<CartItem> inCartItems, FreshbooksStatus inStatus) throws Exception {
		//check for customer first
		Customer customer = inOrder.getCustomer();
		if (!hasFreshbooksId(customer)) {
			boolean success = createCustmer(customer,customer.getBillingAddress(),null);
			if (!success){
				throw new Exception("Customer "+customer.getUser()+" cannot be added to Freshbooks");
			}
		}
		
		Element root = DocumentHelper.createElement("request");
		root.addAttribute("method", "recurring.create");
		Element invoice = root.addElement("recurring");
		Element lines = invoice.addElement("lines");
		populateOrderElements(invoice,inOrder,null);
		populateItemsForAutopayInvoice(lines,inCartItems,inOrder.getTaxes(),1);
		if (inStatus.getRecurringProfiles()!=null && !inStatus.getRecurringProfiles().isEmpty()){
			for (RecurringProfile profile:inStatus.getRecurringProfiles()){
				populateItemsForAutopayInvoice(lines,profile.getCartItems(),inOrder.getTaxes(),Integer.parseInt(profile.getOccurrence()));
			}
		}
		
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
			inOrder.setProperty(FRESHBOOKS_ID, invoiceId);//persist the invoiceId in the order itself
			
			//check the status
			Element element = getInvoice(invoiceId);
			String status = getInvoiceStatus(element);
			inStatus.setInvoiceStatus(status);
			if (!isInvoicePaid(element)){
				log.warn("payment for "+invoiceId+" has not been received, current status is "+status);
			}
		} else {
			String error = result.elementText("error");
			String code = result.elementText("code");
			inStatus.setErrorMessage(error);
			inStatus.setErrorCode(code);
		}
	}
	
	private void populateOrderElements(Element inInvoice,Order inOrder,RecurringProfile inProfile) throws Exception {
		Customer customer = inOrder.getCustomer();
		inInvoice.addElement("client_id").setText(customer.get(FRESHBOOKS_ID));
		if (inProfile!=null){
			Date startDate = inProfile.getStartDate();
			String frequency = inProfile.getFrequency();
			String occurrences = inProfile.getOccurrence();
			inInvoice.addElement("date").setText(format.format(startDate));
			inInvoice.addElement("frequency").setText(frequency);
			inInvoice.addElement("occurrences").setText(occurrences);
		} else {
			inInvoice.addElement("occurrences").setText("1");//one time only
		}
		
		boolean sendEmail = inOrder.getBoolean("sendemail");
		boolean sendSnailMail = inOrder.getBoolean("sendsnailmail");
		inInvoice.addElement("send_email").setText(sendEmail ? "1" : "0");
		inInvoice.addElement("send_snail_mail").setText(sendSnailMail ? "1" : "0");
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
	
	private void populateItemsForAutopayInvoice(Element inLines, List<CartItem> inCartItems, Map<?,?> inTaxes, int inOccurrences) throws Exception {
		//includes one-time invoices and the first invoice of recurring invoices
		populateCartItemElements(inLines,inCartItems,inTaxes,inOccurrences,false);
	}
	
	private void populateItemsForRecurringInvoices(Element inLines, List<CartItem> inCartItems, Map<?,?> inTaxes, int inOccurrences) throws Exception {
		populateCartItemElements(inLines,inCartItems,inTaxes,inOccurrences,true);
	}
	
	private void populateCartItemElements(Element inLines, List<CartItem> inCartItems, Map<?,?> inTaxes, int inOccurrences, boolean inProcessRecurring) throws Exception {
		for (Iterator<?> iterator = inCartItems.iterator(); iterator.hasNext();) {
			CartItem item = (CartItem) iterator.next();
			Money unitCost = item.getYourPrice();
			//if the item is not recurring, then we are creating a one time invoice so we use provided cost
			//if the item is recurring then
			//	we are either calculating the first invoice or the remaining recurring invoices
			//  if inProcessRecurring is false then this is the first invoice
			//  otherwise this is the remaining invoices
			if (Boolean.parseBoolean(item.getProduct().get("recurring"))){
				ArrayList<Money> costs = getRecurringItemPrices(unitCost,inOccurrences);
				if (!inProcessRecurring){//first invoice of recurring
					unitCost = costs.get(0);
				} else {
					//remaining invoices of recurring
					unitCost = costs.get(1);
				}
			}
			Element line = inLines.addElement("line");
			line.addElement("name").setText(item.getProduct().getId());
			line.addElement("description").setText(item.getProduct().getName());
			line.addElement("unit_cost").setText(unitCost.toShortString());
			line.addElement("quantity").setText(String.valueOf(item.getQuantity()));
			
			int count = 1;
			for (Iterator<?> iterator2 = inTaxes.keySet().iterator(); iterator2.hasNext();) {
				TaxRate rate = (TaxRate) iterator2.next();
				String name = rate.getName() == null ? "Tax #"+count : rate.getName();//do only when tax has no name
				line.addElement("tax" + count + "_name").setText(name);
				double percent = rate.getFraction().doubleValue() * 100;
				line.addElement("tax" + count + "_percent").setText(String.valueOf(percent));
				count++;
				//not sure why we cut it off here?
//				if (count > 2) {
//					break;
//				}
			}	
		}
	}
	
	private ArrayList<Money> getRecurringItemPrices(Money inTotalPrice, int inOccurrences) throws Exception{
		ArrayList<Money> amounts = new ArrayList<Money>();
		if (inOccurrences - 1 > 0){
			int remainingOccurrences = inOccurrences - 1;
			double equalParts = inTotalPrice.doubleValue() / ((double) inOccurrences);
			Money firstPayment = new Money(equalParts);
			Money remainingPayments = inTotalPrice.subtract(firstPayment);
			while ( ( (int) (remainingPayments.doubleValue() * 100) % remainingOccurrences) != 0){//must have remaining payments divide up equally
				firstPayment = firstPayment.add("0.01");//increment first payment by one penny
				remainingPayments = inTotalPrice.subtract(firstPayment);//recalculate remaining payments
			}
			amounts.add(firstPayment);
			amounts.add(new Money(remainingPayments.doubleValue()/(double)remainingOccurrences));//the result here should be properly rounded
		} else {
			amounts.add(inTotalPrice);
			amounts.add(inTotalPrice);
		}
		return amounts;
	}

	public void createRecurringInvoices(Order inOrder,FreshbooksStatus inStatus) throws Exception {
		//check for customer first
		Customer customer = inOrder.getCustomer();
		if (!hasFreshbooksId(customer)) {
			boolean success = createCustmer(customer,customer.getBillingAddress(),null);
			if (!success){
				throw new Exception("Customer "+customer.getUser()+" cannot be added to Freshbooks");
			}
		}
		//check if recurring profiles on freshbooks status is empty
		if (inStatus.getRecurringProfiles().isEmpty()){
			//break up order into separate invoices consisting of cart items itemized by frequency and occurrence
			ArrayList<RecurringProfile> profiles = getRecurringProfiles(inOrder);
			if (profiles.isEmpty()){
				throw new Exception("no recurring order items found in order #"+inOrder.getId()+", "+inOrder.getName());
			}
			inStatus.setRecurringProfiles(profiles);
		}
		//go through each profile and generate recurring profile on freshbooks
		for (RecurringProfile profile:inStatus.getRecurringProfiles()){
			createRecurringInvoice(inOrder,profile);//if succeeds, recurring id will be added to profile object
		}
	}
	
	protected ArrayList<CartItem> getNonRecurringCartItems(Order inOrder){
		ArrayList<CartItem> items = new ArrayList<CartItem>();
		for (Iterator<?> iterator = inOrder.getItems().iterator(); iterator.hasNext();) {
			CartItem item = (CartItem) iterator.next();
			if (!Boolean.parseBoolean(item.getProduct().get("recurring"))){
				items.add(item);
			}
		}
		return items;
	}
	
	protected ArrayList<RecurringProfile> getRecurringProfiles(Order inOrder){
		SearcherManager manager = getSearcherManager();
		Searcher searcher = manager.getSearcher(getCatalogId(), "frequency");
		
		HashMap<String,RecurringProfile> map = new HashMap<String,RecurringProfile>();//key = frequency+occurrence
		for (Iterator<?> iterator = inOrder.getItems().iterator(); iterator.hasNext();) {
			CartItem item = (CartItem) iterator.next();
			if (Boolean.parseBoolean(item.getProduct().get("recurring"))){
				String frequency = item.get("frequency");
				String occurrence = item.get("occurrences");
				if (occurrence == null) occurrence = "0";//infinity if occurrence is not specified
				String key = frequency+"_"+occurrence;
				RecurringProfile profile;
				if (map.containsKey(key)){
					profile = map.get(key);
				} else {
					profile = new RecurringProfile();
					profile.setFrequency(frequency);
					profile.setOccurrence(occurrence);
					Data data = (Data) searcher.searchById(frequency);//get the day,month,year of the particular frequency chosen
					profile.setStartDate(inOrder.getDate(),data.get("day"),data.get("month"),data.get("year"));//calculate the start date based on frequency and order date
					
					map.put(key, profile);
				}
				profile.getCartItems().add(item);
			}
		}
		return new ArrayList<RecurringProfile>(map.values());
	}
	
	public RecurringProfile getRecurringProfile(Order inOrder, String inFrequency, String inOccurrence) throws Exception{
		SearcherManager manager = getSearcherManager();
		Searcher searcher = manager.getSearcher(getCatalogId(), "frequency");
		RecurringProfile profile = new RecurringProfile();
		profile.setOccurrence(inOccurrence);
		profile.setFrequency(inFrequency);
		Data data = (Data) searcher.searchById(inFrequency);
		profile.setStartDate(inOrder.getDate(),data.get("day"),data.get("month"),data.get("year"));
		for (Iterator<?> iterator = inOrder.getItems().iterator(); iterator.hasNext();) {
			CartItem item = (CartItem) iterator.next();
			if (Boolean.parseBoolean(item.getProduct().get("recurring"))){
				String frequency = item.get("frequency");
				String occurrence = item.get("occurrences");
				if (occurrence == null) occurrence = "0";
				if (frequency.equals(inFrequency) && occurrence.equals(inOccurrence)){
					profile.getCartItems().add(item);
				}
			}
		}
		return profile;
	}
	
	public void createRecurringInvoice(Order inOrder, RecurringProfile inProfile) throws Exception{
		//check for customer first
		Customer customer = inOrder.getCustomer();
		if (!hasFreshbooksId(customer)) {
			boolean success = createCustmer(customer,customer.getBillingAddress(),null);
			if (!success){
				throw new Exception("Customer "+customer.getUser()+" cannot be added to Freshbooks");
			}
		}
		
		Element root = DocumentHelper.createElement("request");
		root.addAttribute("method", "recurring.create");
		Element invoice = root.addElement("recurring");
		Element lines = invoice.addElement("lines");
		populateOrderElements(invoice,inOrder,inProfile);
		populateItemsForRecurringInvoices(lines,inProfile.getCartItems(),inOrder.getTaxes(),Integer.parseInt(inProfile.getOccurrence()));
		Element result = callFreshbooks(root);
		if (isStatusOk(result)) {
			String recurringId = result.element("recurring_id").getText();
			inProfile.setRecurringId(recurringId);
		} else {
			String error = result.elementText("error");
			String code = result.elementText("code");
			log.warn("recurring profile for "+inOrder.getId()+" cannot be completed, error code: "+code+", error message: "+error);
		}
	}
	
//	public boolean isRecurringInvoiceActive(String inRecurringId) throws Exception{
//		Element root = DocumentHelper.createElement("request");
//		root.addAttribute("method", "recurring.get");
//		root.addElement("recurring_id").setText(inRecurringId);
//
//		Element result = callFreshbooks(root);
//		if (isStatusOk(result)){
//			String stopped = result.element("recurring").element("stopped").getText();
//			return (stopped!=null && stopped.equals("0"));//0 = active, 1 = not active
//		}
//		return false;
//	}
	
	public boolean cancelRecurringInvoice(String inRecurringId) throws Exception {
		Element root = DocumentHelper.createElement("request");
		root.addAttribute("method", "recurring.delete");
		root.addElement("recurring_id").setText(inRecurringId);

		Element result = callFreshbooks(root);
		return isStatusOk(result);
	}
	
//	public Element queryInvoice(String inInvoiceId) throws Exception {
//		Element root = DocumentHelper.createElement("request");
//		root.addAttribute("method", "invoice.get");
//		root.addElement("invoice_id").setText(inInvoiceId);
//
//		Element result = callFreshbooks(root);
//		return result;
//	}

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
		return getInvoice(inOrder.get(FRESHBOOKS_ID));
	}
	
	public Element getInvoice(String invoiceId) throws Exception{
		Element root = DocumentHelper.createElement("request");
		root.addAttribute("method", "invoice.get");
		Element invoice = root.addElement("invoice_id");
		invoice.setText(invoiceId);
		Element result = callFreshbooks(root);
		return result;
	}
	
	public String getInvoiceStatus(String invoiceId) throws Exception {
		Element root = DocumentHelper.createElement("request");
		root.addAttribute("method", "invoice.get");
		Element invoice = root.addElement("invoice_id");
		invoice.setText(invoiceId);
		Element result = callFreshbooks(root);
		String status = getInvoiceStatus(result);
		return status;
	}
	
	public static boolean hasFreshbooksId(Order inOrder) throws Exception {
		return inOrder.get(FRESHBOOKS_ID)!=null;
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
		String status = inElement!=null ? inElement.element("invoice").element("status").getText() : null;
		return status;
	}
	
	public static String getOrderStatusFromInvoice(Order inOrder, FreshbooksStatus inStatus) throws Exception{
		// Freshbooks invoice states - paid: ‘paid’, ‘auto-paid’; unpaid: ‘disputed’, ‘sent’, ‘viewed’, ‘retry’, ‘failed’; n/a: ‘draft’
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
