/*
 * Created on Nov 2, 2004
 */
package org.openedit.cart.freshbooks;

import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.openedit.Data;
import org.openedit.store.CreditPaymentMethod;
import org.openedit.store.Store;
import org.openedit.store.StoreException;
import org.openedit.store.customer.Customer;
import org.openedit.store.orders.BaseOrderProcessor;
import org.openedit.store.orders.Order;
import org.openedit.store.orders.OrderState;
import org.openedit.store.orders.Refund;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.users.User;
import com.openedit.users.UserManager;
import com.openedit.util.XmlUtil;

/**
 * @author cburkey
 *
 */
public class FreshbooksOrderProcessor extends BaseOrderProcessor
{

	private static final Log log = LogFactory.getLog(FreshbooksOrderProcessor.class);

	protected FreshbooksManager util;
	protected PageManager fieldPageManager;
	protected UserManager fieldUserManager;
	protected XmlUtil fieldXmlUtil;
	
	public XmlUtil getXmlUtil() {
		if (fieldXmlUtil == null) {
			fieldXmlUtil = new XmlUtil();
		}
		return fieldXmlUtil;
	}

	public void setXmlUtil(XmlUtil inXmlUtil) {
		fieldXmlUtil = inXmlUtil;
	}

	

	public PageManager getPageManager() {
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager) {
		fieldPageManager = inPageManager;
	}

	public FreshbooksManager getUtil(Store inStore) {
		if (util == null) {
			util = new FreshbooksManager();
			
		}

		return util;
	}

	public void setUtil(FreshbooksManager inUtil) {
		util = inUtil;
	}

	protected boolean requiresValidation(Store inStore, Order inOrder)
	{
		Page page = getPageManager().getPage(
				inStore.getStoreHome() + "/configuration/freshbooks.xml");
		
		
		if (page.exists()) {
			return inOrder.getPaymentMethod().requiresValidation();
		}
		return false;
	}

	public void processNewOrder(WebPageRequest inContext, Store inStore, Order inOrder)
		throws StoreException
	{
		if ( !requiresValidation( inStore,inOrder ))
		{
			return;
		}
		//	"AUTH_ONLY");   //AUTH_CAPTURE, AUTH_ONLY, CAPTURE_ONLY, CREDIT, VOID, PRIOR_AUTH_CAPTURE.
		
			process(inStore, inOrder, "AUTH_CAPTURE");
		
	}

	protected void process(Store inStore, Order inOrder, String inType) throws StoreException
	{
		try
		{
			Page page = getPageManager().getPage(inStore.getStoreHome() + "/configuration/freshbooks.xml");
			Element conf = getXmlUtil().getXml(page.getReader(), "UTF-8");
			
			String uri = conf.element("uri").getText();
			String token = conf.element("token").getText();
			String gateway = conf.element("gateway").getText();
			
			log.info("uri: "+uri+", token: "+token+", gateway: "+gateway);
			
			FreshbooksManager util = new FreshbooksManager();
			util.setToken(token);
			util.setUrl(uri);
			
//			
//			
//			
//			CreditPaymentMethod creditCard = (CreditPaymentMethod)inOrder.getPaymentMethod();
//			if (  creditCard.getCardNumber().equals("5555555555554444") )
//		    {
//				OrderState orderState = null;
//				orderState = inStore.getOrderState(Order.AUTHORIZED);
//		    	orderState.setDescription("TEST ORDER");
//		    	orderState.setOk(true);
//		    	inOrder.setOrderState(orderState);
//		    	return;
//		    }
		
			
			
			// load customer address info from order (in case needed for AVS)
//		    Customer customer = inOrder.getCustomer();
		    
		    
		    FreshbookInstructions instructions = new FreshbookInstructions();
		    instructions.setGateway(gateway);
		    //not sure why we do this...
//		    HitTracker hits = inStore.getSearcherManager().getList(inStore.getCatalogId(), "frequency");
//		    for (Iterator iterator = hits.iterator(); iterator.hasNext();) {
//				Data freq = (Data) iterator.next();
//				instructions.setFrequency(freq.get("id"));
			    instructions.setSendEmail("1");		
			    instructions.setSendSnailMail("0");			    
			    util.createRecurring(inOrder, instructions);
//			    
//			}
//		    
		    
		    OrderState orderState = null;
		    if (inOrder.get("freshbooksid") != null){
				orderState = inStore.getOrderState(Order.AUTHORIZED);
		    	orderState.setDescription("Authorized by Freshbooks");
		    	orderState.setOk(true);
		    	inOrder.setOrderState(orderState);
		    } else {
		    	String error = instructions.getErrorMessage();
		    	
		    	orderState = inStore.getOrderState(Order.REJECTED);
		    	orderState.setDescription( error );
		    	orderState.setOk(false);
		    	
		    }
		    
		    
//		    if ("1".equals(responseCode))
//		    {
//		    	// transaction approved
//		    	//super.exportNewOrder(inContext, inStore, inOrder);
//
//		    	if( inType.indexOf("CAPTURE") > -1)
//		    	{
//		    		orderState = inStore.getOrderState(Order.CAPTURED);		    		
//		    		orderState.setDescription("Your transaction has been captured by Authorize.net.");		    		
//		    	}
//		    	else
//		    	{
//		    		orderState = inStore.getOrderState(Order.AUTHORIZED);
//		    		orderState.setDescription("Your transaction has been authorized.");
//		    	}
//		    	orderState.setOk(true);
//		    }
//		    else
//		    {
//		    	// transaction declined
//		    	log.warn("Transaction DECLINED for order #" + inOrder.getId());
//		    	log.warn("Authorize.net transaction ID:" + anetcc.getResponseTransactionID());
//		    	log.warn("Response code:" + anetcc.getResponseCode());
//		    	log.warn("Response Reason Code: " + anetcc.getResponseReasonCode());
//		    	log.warn("Response Reason Text: " + anetcc.getResponseReasonText());
//		    	log.warn("AVS Result Code: " + anetcc.getResponseAVSResultCode());
//		    	
//
//		    	String error = "Your transaction has been declined.  Please hit the back button on your browser to correct.<br>";
//				error += anetcc.getResponseReasonText();
//				error += " (Full Code:  " + anetcc.getResponseCode() + "." + anetcc.getResponseSubCode() + "." + anetcc.getResponseReasonCode() + ")";
//				
//				orderState = inStore.getOrderState(Order.REJECTED);
//		    	orderState.setDescription( error );
//		    	orderState.setOk(false);
//		    }
		    inOrder.setOrderState(orderState);
		}
		catch ( Exception e )
		{
			OrderState orderState = new OrderState();
			orderState.setDescription(
				"An error occurred while processing your transaction.");
			orderState.setOk(false);
			inOrder.setOrderState(orderState);
			e.printStackTrace();
			throw new StoreException(e);
		}
	}
	public UserManager getUserManager() {
		return fieldUserManager;
	}

	public void setUserManager(UserManager inUserManager) {
		fieldUserManager = inUserManager;
	}

	public void captureOrder(WebPageRequest inContext, Store inStore, Order inOrder) throws StoreException
	{
		if ( !requiresValidation( inStore,inOrder ))
		{
			return;
		}
		process(inStore, inOrder, "PRIOR_AUTH_CAPTURE");
	}

	@Override
	public void refundOrder(WebPageRequest inContext, Store inStore, Order inOrder,
			Refund inRefund) throws StoreException {
		// TODO Auto-generated method stub
		
	}
}
