package org.openedit.store.gateway;

import java.util.Date;
import java.util.HashMap;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
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

	private static final Log log = LogFactory
			.getLog(BeanstreamOrderProcessor.class);
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

	protected boolean requiresValidation(Store inStore, Order inOrder) {

		Page page = getPageManager().getPage(
				inStore.getStoreHome() + "/configuration/beanstream.xml");
		
		
		if (page.exists()) {
			return inOrder.getPaymentMethod().requiresValidation();
		}
		return false;
	}

	public void processNewOrder(WebPageRequest inContext, Store inStore,
			Order inOrder) throws StoreException {
		if (!requiresValidation(inStore, inOrder)) {
			return;
		}
		// "AUTH_ONLY"); //AUTH_CAPTURE, AUTH_ONLY, CAPTURE_ONLY, CREDIT, VOID,
		// PRIOR_AUTH_CAPTURE.
		if (inStore.isAutoCapture()) {
			process(inStore, inOrder, "AUTH_CAPTURE");
		} else {
			process(inStore, inOrder, "AUTH_ONLY");
		}
	}

	protected void process(Store inStore, Order inOrder, String inType)
			throws StoreException {
		try {
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
			if (password == null)
			{
				User user = getUserManager().getUser(userid);
				password = getUserManager().getStringEncryption().decrypt(user.getPassword()); 
			}
			
//			System.out.println(" &&&&&& using "+userid+" "+password);

			Customer customer = inOrder.getCustomer();

			HttpClient client = new HttpClient();
			String url = "https://www.beanstream.com/scripts/process_transaction.asp";
			PostMethod post = new PostMethod(url);

			CreditPaymentMethod creditCard = (CreditPaymentMethod) inOrder
					.getPaymentMethod();
			Money total = inOrder.getTotalPrice();

			
			
			
			
			post.addParameter("requestType", "BACKEND");
			post.addParameter("merchant_id", merchant);
			post.addParameter("username", userid);
			post.addParameter("password", password);
//			System.out.println(" &&& updated parameters ");
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
			if(customer.getEmail() != null){
			post.addParameter("ordEmailAddress", customer.getEmail());
			}
			if(customer.getPhone1() != null){
				post.addParameter("ordPhoneNumber", customer.getPhone1());
			}
			post.addParameter("ordAddress1", customer.getBillingAddress(true).getAddress1());
			if(customer.getBillingAddress(true).getAddress2() != null){
			post.addParameter("ordAddress2", customer.getBillingAddress(true).getAddress2());
			}
			post.addParameter("ordCity", customer.getBillingAddress(true).getCity());
			
			//map this to the correct codes..
			post.addParameter("ordProvince", customer.getBillingAddress(true).getState());
			
			post.addParameter("ordPostalCode", customer.getBillingAddress(true).getZipCode());
			String country = customer.getBillingAddress(true).getCountry();
			if(country != null){
			post.addParameter("ordCountry", country);
			}
			
			
			int result = client.executeMethod(post);
		
			
			OrderState orderState = null;

			String responsebody = post.getResponseBodyAsString();
			
			
			
			log.info(responsebody);
			log.info(post.getParams());
			//[15:11:13.154] trnApproved=1&trnId=10000000&messageId=1&messageText=Approved&trnOrderNumber=WEB0000025&authCode=TEST&errorType=N&errorFields=&responseType=T&trnAmount=6%2E01&trnDate=6%2F25%2F2012+12%3A10%3A33+PM&avsProcessed=0&avsId=U&avsResult=0&avsAddrMatch=0&avsPostalMatch=0&avsMessage=Address+information+is+unavailable%2E&cvdId=1&cardType=VI&trnType=P&paymentMethod=CC&ref1=&ref2=&ref3=&ref4=&ref5=
			HashMap pairs = new HashMap();
			String[] stuff = responsebody.split("&");
			for (int i = 0; i < stuff.length; i++) {
				String[] pair = stuff[i].split("=");
				if(pair.length == 2){
					pairs.put(pair[0], pair[1]);
				}
			}
			
			
			if("1".equals(pairs.get("trnApproved")) && pairs.containsKey("trnId")){
				// super.exportNewOrder(inContext, inStore, inOrder);
				 orderState = inStore.getOrderState(Order.AUTHORIZED);
				 inOrder.setProperty("transactionid", pairs.get("trnId").toString());
				orderState.setDescription("Your transaction has been authorized.");
				orderState.setOk(true);
					
			} else{
				orderState = inStore.getOrderState(Order.REJECTED);
				orderState.setOk(false);
				orderState.setDescription((String) pairs.get("error"));
				
			}
//			if ("0".equals(ssl_result)) {
//				// transaction approved
//				//
//
//				
//			} else {
//				// transaction declined
//				log.warn("Transaction DECLINED for order #" + inOrder.getId());
//				log.warn("Response code:" + ssl_result);
//				log.warn("Response Message: " + ssl_message);
//
//				String error = "Your transaction has been declined.  Please hit the back button on your browser to correct.<br>";
//				error += " (Full Code:  " + ssl_result + "." + ssl_message
//						+ ")";
//				//error =error += "<br> " + root.asXML();
//				orderState = inStore.getOrderState(Order.REJECTED);
//				orderState.setDescription(error);
//				orderState.setOk(false);
//			}
			inOrder.setOrderState(orderState);
		} catch (Exception e) {
			OrderState orderState = new OrderState();
			orderState
					.setDescription("An error occurred while processing your transaction.");
			orderState.setOk(false);
			inOrder.setOrderState(orderState);
			e.printStackTrace();
			throw new StoreException(e);
		}
	}

	public void captureOrder(WebPageRequest inContext, Store inStore,
			Order inOrder) throws StoreException {
		if (!requiresValidation(inStore, inOrder)) {
			return;
		}
		process(inStore, inOrder, "");
	}

	@Override
	public void refundOrder(WebPageRequest inContext, Store inStore, Order inOrder,
			Refund inRefund) throws StoreException {
		getBeanstreamUtil().refund(inStore, inOrder, inRefund);
	}
}
