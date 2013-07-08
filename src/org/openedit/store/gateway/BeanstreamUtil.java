package org.openedit.store.gateway;

import java.net.URLDecoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.dom4j.Element;
import org.openedit.money.Money;
import org.openedit.store.CartItem;
import org.openedit.store.CreditPaymentMethod;
import org.openedit.store.Store;
import org.openedit.store.StoreException;
import org.openedit.store.orders.Order;
import org.openedit.store.orders.OrderState;
import org.openedit.store.orders.Refund;

import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.users.User;
import com.openedit.users.UserManager;
import com.openedit.util.XmlUtil;

public class BeanstreamUtil {

	
	protected PageManager fieldPageManager;
	
	protected XmlUtil fieldXmlUtil;
	protected UserManager fieldUserManager;
	
	
	
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
	
	
	public void refund(Store inStore, Order inOrder, Refund inRefund)
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
			
			HttpClient client = new HttpClient();
			String url = "https://www.beanstream.com/scripts/process_transaction.asp";
			PostMethod post = new PostMethod(url);
			post.addParameter("requestType", "BACKEND");
			post.addParameter("merchant_id", merchant);
			post.addParameter("trnAmount",inRefund.getTotalAmount().toShortString());
			
			post.addParameter("trnType", "R");//R=return, VP=void purchase, VR=void return
			post.addParameter("username", userid);
			post.addParameter("password", password);
			
			String transactionId = inOrder.get("transactionid");
			post.addParameter("adjId",transactionId);//References the transaction identification number (trnId) from the original purchase.
			
			//post the refund request
			int result = client.executeMethod(post);
			//process response
			String responsebody = post.getResponseBodyAsString();
			
			HashMap<String,String> pairs = new HashMap<String,String>();
			String[] stuff = responsebody.split("&");
			for (int i = 0; i < stuff.length; i++) {
				String[] pair = stuff[i].split("=");
				if(pair.length == 2){
					pairs.put(pair[0], pair[1]);
				}
			}
			
			//get trnId 
			//approved response			
			//trnApproved=1&trnId=10002118&messageId=1&messageText=Approved&
			//trnOrderNumber=1234R&authCode=TEST&errorType=N&errorFields=&
			//responseType=T&trnAmount=1%2E00&trnDate=8%2F17%2F2009+1%3A44%3A56+PM&
			//avsProcessed=0&avsId=0&avsResult=0&avsAddrMatch=0&avsPostalMatch=0&
			//avsMessage=Address+Verification+not+performed+for+this+transaction%2E&cardType=VI&trnType=R&
			//paymentMethod=CC&ref1=&ref2=&ref3=&ref4=&ref5=
			if("1".equals(pairs.get("trnApproved"))){
					inRefund.setSuccess(true);
					inRefund.setAuthorizationCode(pairs.get("authCode"));
					inRefund.setTransactionId(pairs.get("trnId"));
					inRefund.setDate(new Date());// or parse trnDate
					
			} else{
				//declined response:
				//trnApproved=0&trnId=10002120&messageId=205&messageText=Transaction+only+voidable+on+the+date+processed
				//&trnOrderNumber=1234RETURNTEST&authCode=&errorType=N&errorFields=&responseType=T
				//&trnAmount=30%2E45&trnDate=8%2F17%2F2009+2%3A02%3A34+PM&avsProcessed=0&avsId=0&avsResult=0
				//&avsAddrMatch=0&avsPostalMatch=0&avsMessage=Address+Verification+not+performed+for+this+transaction%2E&cardType=VI&trnType=VP&paymentMethod=CC
				//&ref1=&ref2=&ref3=&ref4=&ref5=
				
				inRefund.setSuccess(false);
				String message = pairs.get("messageText");
				message = message!=null ? URLDecoder.decode(message,"UTF-8").replaceAll("\\<.*?\\>", "") : "Unknown declined response";
//				message = replaceAll("\\<.*?\\>", "");
				inRefund.setMessage(message);
				inRefund.setDate(new Date());//or parse trnDate
				
			}
			
		} catch (Exception e) {
			inRefund.setSuccess(false);
			inRefund.setMessage("An error occurred while processing your transaction.");
			e.printStackTrace();
			throw new StoreException(e);
		}
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
}
