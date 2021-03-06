package org.openedit.store.gateway;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.entermedia.MediaArchive;
import org.openedit.money.Money;
import org.openedit.store.CreditPaymentMethod;
import org.openedit.store.Store;
import org.openedit.store.StoreException;
import org.openedit.store.customer.Customer;
import org.openedit.store.orders.BaseOrderProcessor;
import org.openedit.store.orders.Order;
import org.openedit.store.orders.OrderState;
import org.openedit.store.orders.Refund;

import com.openedit.WebPageRequest;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.users.User;
import com.openedit.users.UserManager;
import com.openedit.util.XmlUtil;

public class BeanstreamOrderProcessor extends BaseOrderProcessor {

	private static final Log log = LogFactory.getLog(BeanstreamOrderProcessor.class);
	protected PageManager fieldPageManager;
	protected XmlUtil fieldXmlUtil;
	protected BeanstreamUtil fieldBeanstreamUtil;
	protected UserManager fieldUserManager;
	
	public XmlUtil getXmlUtil() {
		if (fieldXmlUtil == null) {
			fieldXmlUtil = new XmlUtil();
		}
		return fieldXmlUtil;
	}

	public void setXmlUtil(XmlUtil inXmlUtil) {
		fieldXmlUtil = inXmlUtil;
	}

	public BeanstreamUtil getBeanstreamUtil()
	{
		return fieldBeanstreamUtil;
	}

	public void setBeanstreamUtil(BeanstreamUtil inBeanstreamUtil)
	{
		fieldBeanstreamUtil = inBeanstreamUtil;
	}
	
	public UserManager getUserManager() {
		return fieldUserManager;
	}
	
	public void setUserManager(UserManager inUserManager) {
		fieldUserManager = inUserManager;
	}

	public PageManager getPageManager() {
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager) {
		fieldPageManager = inPageManager;
	}

	protected boolean requiresValidation(Store inStore, Order inOrder) 
	{

		/// if the store is set to beanstream 
		//		and if the order is not set to something else
		//	then return true
		//this way we can have specific orders setup to go through specific gateways
		
		if (inOrder.getPaymentMethod().requiresValidation()){
			Page page = getPageManager().getPage(inStore.getStoreHome() + "/configuration/beanstream.xml");
			if (page.exists()){
				//config may exist but store may not be configured for beanstream
				String ordergateway = inOrder.get("gateway");
				String storegateway = inStore.get("gateway");
				if ("beanstream".equals(ordergateway)){
					return true;
				}
				if (storegateway == null || "beanstream".equals(storegateway)){
					return true;
				}
			}
		}
		return false;
	}

	public void processNewOrder(WebPageRequest inContext, Store inStore,
			Order inOrder) throws StoreException {
		if (!requiresValidation(inStore, inOrder) ) {
			return;
		}
		// "AUTH_ONLY"); //AUTH_CAPTURE, AUTH_ONLY, CAPTURE_ONLY, CREDIT, VOID,
		// PRIOR_AUTH_CAPTURE.
		
		MediaArchive archive = (MediaArchive) inContext.getPageValue("mediaarchive");
		if (inStore.isAutoCapture()) {
			process(archive, inStore, inOrder, "AUTH_CAPTURE");
		} else {
			process(archive, inStore, inOrder,"AUTH_ONLY");
		}
	}

	protected void process(MediaArchive inArchive, Store inStore, Order inOrder, String inType)
			throws StoreException {
		try {
			
			//if gateway has not been set, set it so that we can identify where
			// to issue refunds
			if (inOrder.get("gateway") == null){
				inOrder.setProperty("gateway","beanstream");
			}
			//set cardtype if it hasn't already been set
			if (inOrder.get("cardtype") == null){
				if (inOrder.getPaymentMethod() instanceof CreditPaymentMethod){
					CreditPaymentMethod method = (CreditPaymentMethod) inOrder.getPaymentMethod();
					if (method.getCreditCardType() != null){
						inOrder.setProperty("cardtype", method.getCreditCardType().getId());
					}
				}
			}
			
			// See examples at http://www.jcommercesql.com/anet/
			// load properties (e.g. IP address, username, password) for
			// accessing authorize.net
			// load customer address info from order (in case needed for AVS)
			Page page = getPageManager().getPage(
					inStore.getStoreHome() + "/configuration/beanstream.xml");
			Element conf = getXmlUtil().getXml(page.getReader(), "UTF-8");
			
			
			String merchant = conf.element("merchantid").getText();
			String userid = conf.element("user").getText();
			String password = conf.element("password") != null && !conf.element("password").getText().isEmpty() ? conf.element("password").getText() : null;
			boolean avsEnabled = conf.element("avs") != null ? Boolean.parseBoolean(conf.element("avs").getText()) : false;
			if (password == null)
			{
				User user = getUserManager().getUser(userid);
				password = getUserManager().getStringEncryption().decrypt(user.getPassword()); 
			}
			
			Customer customer = inOrder.getCustomer();
			
			HttpClient client = new HttpClient();
			String url = "https://www.beanstream.com/scripts/process_transaction.asp";
			PostMethod post = new PostMethod(url);

			CreditPaymentMethod creditCard = (CreditPaymentMethod) inOrder.getPaymentMethod();
			Money total = inOrder.getTotalPrice();

			
			
			
			post.addParameter("requestType", "BACKEND");
			post.addParameter("merchant_id", merchant);
			post.addParameter("username", userid);
			post.addParameter("password", password);
			post.addParameter("trnOrderNumber", inOrder.getId());
			post.addParameter("trnAmount", inOrder.getTotalPrice().toShortString());
			post.addParameter("trnCardOwner", customer.getFirstName() + " " + customer.getLastName());
			
			
			post.addParameter("trnCardNumber", creditCard.getCardNumber());
			//these are probably wrong.
			
			String expirationmonth = creditCard.getExpirationMonthString();
			String expirationyear = creditCard.getExpirationYearString();
			
			post.addParameter("trnExpMonth", expirationmonth);
			post.addParameter("trnExpYear", expirationyear);
			post.addParameter("trnCardCvd", creditCard.getCardVerificationCode());
			
			post.addParameter("ordName", customer.getFirstName() + " " + customer.getLastName());
			if(customer.getEmail() != null)
			{
				post.addParameter("ordEmailAddress", customer.getEmail());
			}
			if(customer.getPhone1() != null)
			{
				post.addParameter("ordPhoneNumber", customer.getPhone1());
			}
			post.addParameter("ordAddress1", customer.getBillingAddress(true).getAddress1());
			if(customer.getBillingAddress(true).getAddress2() != null)
			{
				post.addParameter("ordAddress2", customer.getBillingAddress(true).getAddress2());
			}
			post.addParameter("ordCity", customer.getBillingAddress(true).getCity());
			
			//map this to the correct codes..
			//post.addParameter("ordProvince", customer.getBillingAddress(true).getState());
			String province = getBeanstreamState(inArchive,customer.getBillingAddress(true).getState());
			log.info("input province: "+customer.getBillingAddress(true).getState()+", beanstream province: "+province);
			post.addParameter("ordProvince", province);
			post.addParameter("ordPostalCode", customer.getBillingAddress(true).getZipCode());
			String country = customer.getBillingAddress(true).getCountry();
			if(country != null){
				//country fix
				if ("Canada".equalsIgnoreCase(country)){
					country = "CA";
				}
				post.addParameter("ordCountry", country);
			}
			
			log.info("Posting the following parameters to beanstream");
			NameValuePair [] params = post.getParameters();
			for(NameValuePair param:params){
				if (param.getName().equals("password")){
					log.info(param.getName()+": ********");
				} else if (param.getName().equals("trnCardCvd")){
					log.info(param.getName()+": ***");
				} else if (param.getName().equals("trnCardNumber")){
					log.info(param.getName()+": ********");
				} else {
					log.info(param.getName()+": "+param.getValue());
				}
			}
			
			
			int result = client.executeMethod(post);
		
			
			OrderState orderState = null;

			String responsebody = post.getResponseBodyAsString();
			
			
			
			log.info(responsebody);
			log.info(post.getParams());
			//[15:11:13.154] trnApproved=1&trnId=10000000&messageId=1&messageText=Approved&trnOrderNumber=WEB0000025&authCode=TEST&errorType=N&errorFields=&responseType=T&trnAmount=6%2E01&trnDate=6%2F25%2F2012+12%3A10%3A33+PM&avsProcessed=0&avsId=U&avsResult=0&avsAddrMatch=0&avsPostalMatch=0&avsMessage=Address+information+is+unavailable%2E&cvdId=1&cardType=VI&trnType=P&paymentMethod=CC&ref1=&ref2=&ref3=&ref4=&ref5=
			
			Map<String,String> pairs = new HashMap<String,String>();
			String[] stuff = responsebody.split("&");
			for (int i = 0; i < stuff.length; i++) {
				String[] pair = stuff[i].split("=");
				if(pair.length == 2){
					pairs.put(pair[0], pair[1]);
				}
			}
			
			boolean avsPassed = true;
			if (avsEnabled)
			{
				avsPassed = getBeanstreamUtil().verifyAVS(pairs);
			}
			if("1".equals(pairs.get("trnApproved")) && pairs.containsKey("trnId"))
			{
				orderState = inStore.getOrderState(Order.AUTHORIZED);
				inOrder.setProperty("transactionid", pairs.get("trnId").toString());
				orderState.setDescription("Your transaction has been authorized.");
				orderState.setOk(true);
			}
			else
			{
				orderState = inStore.getOrderState(Order.REJECTED);
				orderState.setOk(false);
				if (!avsPassed)
				{
					orderState.setDescription(pairs.get("avsMessage"));
				}
				else
				{
					orderState.setDescription((String) pairs.get("error"));
				}
			}
			inOrder.setOrderState(orderState);
		} catch (Exception e) {
			OrderState orderState = new OrderState();
			orderState = inStore.getOrderState(Order.REJECTED);

			orderState.setDescription("An error occurred while processing your transaction.");
			orderState.setOk(false);

			inOrder.setOrderState(orderState);
			e.printStackTrace();
			throw new StoreException(e);
		}
	}
	
	protected String getBeanstreamState(MediaArchive inArchive, String inState){
		String state = inState;
		if (inArchive !=null){
			Searcher searcher = inArchive.getSearcher("states");
			Data entry = (Data) searcher.searchByField("name",inState);
			if (entry!=null){
				return entry.getId();
			}
		}
		return state;
	}

	public void captureOrder(WebPageRequest inContext, Store inStore,
			Order inOrder) throws StoreException {
		if (!requiresValidation(inStore, inOrder)) {
			return;
		}
		MediaArchive archive = (MediaArchive) inContext.getPageValue("mediaarchive");
		process(archive,inStore, inOrder, "");
	}

	@Override
	public void refundOrder(WebPageRequest inContext, Store inStore, Order inOrder,
			Refund inRefund) throws StoreException {
		if (!requiresValidation(inStore, inOrder)) {
			return;
		}
		getBeanstreamUtil().refund(inStore, inOrder, inRefund);
	}
}
