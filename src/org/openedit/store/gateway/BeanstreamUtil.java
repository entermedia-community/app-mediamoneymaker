package org.openedit.store.gateway;

import java.util.HashMap;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.dom4j.Element;
import org.openedit.store.CartItem;
import org.openedit.store.CreditPaymentMethod;
import org.openedit.store.Store;
import org.openedit.store.StoreException;
import org.openedit.store.orders.Order;
import org.openedit.store.orders.OrderState;

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




	public void refund(Store inStore, Order inOrder, CartItem item)
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
			User user = getUserManager().getUser(userid);
			
			String password = getUserManager().getStringEncryption().decrypt(user.getPassword()); 
			
			
			HttpClient client = new HttpClient();
			String url = "https://www.beanstream.com/scripts/process_transaction.asp";
			PostMethod post = new PostMethod(url);

			CreditPaymentMethod creditCard = (CreditPaymentMethod) inOrder
					.getPaymentMethod();
			
					
			post.addParameter("requestType", "BACKEND");
			post.addParameter("merchant_id", merchant);
			post.addParameter("trnAmount", item.getTotalPrice().toShortString());
			
			post.addParameter("trnType", "R");
			post.addParameter("username", userid);
			post.addParameter("password", password);

					
		
			
			
			int result = client.executeMethod(post);
		
			
			OrderState orderState = null;

			String responsebody = post.getResponseBodyAsString();
			
			
			
			HashMap pairs = new HashMap();
			String[] stuff = responsebody.split("&");
			for (int i = 0; i < stuff.length; i++) {
				String[] pair = stuff[i].split("=");
				if(pair.length == 2){
					pairs.put(pair[0], pair[1]);
				}
			}
			
			
			if("1".equals(pairs.get("trnApproved"))){
				// super.exportNewOrder(inContext, inStore, inOrder);
					orderState = inStore.getOrderState(Order.AUTHORIZED);
					orderState.setDescription("Your transaction has been authorized.");
					orderState.setOk(true);
					
					
			} else{
				orderState = inStore.getOrderState(Order.REJECTED);
				orderState.setOk(false);

				orderState.setDescription((String) pairs.get("error"));
				
			}

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
