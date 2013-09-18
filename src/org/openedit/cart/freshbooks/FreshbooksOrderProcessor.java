/*
 * Created on Nov 2, 2004
 */
package org.openedit.cart.freshbooks;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.store.CartItem;
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

	protected FreshbooksManager fieldFreshbooksManager;
	protected PageManager fieldPageManager;
	protected UserManager fieldUserManager;
	protected XmlUtil fieldXmlUtil;
	protected SearcherManager fieldSearcherManager;

	public SearcherManager getSearcherManager() {
		return fieldSearcherManager;
	}
	public void setSearcherManager(SearcherManager fieldSearcherManager) {
		this.fieldSearcherManager = fieldSearcherManager;
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
	public PageManager getPageManager() {
		return fieldPageManager;
	}
	public void setPageManager(PageManager inPageManager) {
		fieldPageManager = inPageManager;
	}
	public FreshbooksManager getFreshbooksManager(String inCatalogId) {
		if (fieldFreshbooksManager == null) {
			fieldFreshbooksManager = (FreshbooksManager) getSearcherManager().getModuleManager().getBean(inCatalogId,"freshbooksManager");
			
		}
		return fieldFreshbooksManager;
	}
	public void setFreshbooksManager(FreshbooksManager fieldFreshbooksManager) {
		this.fieldFreshbooksManager = fieldFreshbooksManager;
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
			//check for test card before anything else
//			CreditPaymentMethod creditCard = (CreditPaymentMethod)inOrder.getPaymentMethod();
//			if (creditCard.getCardNumber().equals("5555555555554444"))
//		    {
//				OrderState orderState = null;
//				
//				String cvc = creditCard.getCardVerificationCode();
//				if (cvc.equals("123")){
//					log.info("&&&& test condition: authorized case &&&&");
//					orderState = inStore.getOrderState(Order.AUTHORIZED);
//			    	orderState.setDescription("Authorized by Freshbooks");
//			    	orderState.setOk(true);
//			    	
//			    	inOrder.getCustomer().setProperty(FreshbooksManager.FRESHBOOKS_ID, "100");
//			    	inOrder.setProperty(FreshbooksManager.FRESHBOOKS_ID, "101");//invoice
//			    	inOrder.setProperty(FreshbooksManager.FRESHBOOKS_RECURRING_ID, "102");//recurring
//			    	
//				} else if (cvc.equals("321")){
//					log.info("&&&& test condition: accepted case - requires invoice query &&&&");
//					orderState = inStore.getOrderState(Order.ACCEPTED);
//			    	orderState.setDescription("Accepted by Freshbooks");
//			    	orderState.setOk(true);
//			    	
//			    	inOrder.getCustomer().setProperty(FreshbooksManager.FRESHBOOKS_ID, "100");
//			    	inOrder.setProperty(FreshbooksManager.FRESHBOOKS_ID, "101");//invoice
//			    	inOrder.setProperty(FreshbooksManager.FRESHBOOKS_RECURRING_ID, "102");//recurring
//			    	
//			    	inOrder.setProperty("requiresinvoicequery", "true");//requires invoice query
//					
//				} else if (cvc.equals("111")){
//					log.info("&&&& test condition: authorized case - requires recurring profile query &&&&");
//					orderState = inStore.getOrderState(Order.AUTHORIZED);
//			    	orderState.setDescription("Accepted by Freshbooks");
//			    	orderState.setOk(true);
//			    	
//			    	inOrder.getCustomer().setProperty(FreshbooksManager.FRESHBOOKS_ID, "100");
//			    	inOrder.setProperty(FreshbooksManager.FRESHBOOKS_ID, "101");//invoice
//			    	
//			    	inOrder.setProperty("requirescurringprofile", "true");//requires invoice query
//					
//				} else if (cvc.equals("222")){
//					log.info("&&&& test condition: authorized case - requires invoice AND recurring profile query &&&&");
//					orderState = inStore.getOrderState(Order.AUTHORIZED);
//			    	orderState.setDescription("Accepted by Freshbooks");
//			    	orderState.setOk(true);
//			    	
//			    	inOrder.getCustomer().setProperty(FreshbooksManager.FRESHBOOKS_ID, "100");
//			    	inOrder.setProperty(FreshbooksManager.FRESHBOOKS_ID, "101");//invoice
//			    	
//			    	inOrder.setProperty("requirescurringprofile", "true");//requires invoice query
//					
//				} else {
//					log.info("&&&& test condition: rejected case &&&&");
//					orderState = inStore.getOrderState(Order.REJECTED);
//			    	orderState.setDescription("Card Declined");
//			    	orderState.setOk(false);
//				}
//		    	inOrder.setOrderState(orderState);
//		    	return;
//		    }
			
			Page page = getPageManager().getPage(inStore.getStoreHome() + "/configuration/freshbooks.xml");
			Element conf = getXmlUtil().getXml(page.getReader(), "UTF-8");
			
			String uri = conf.element("uri").getText();
			String token = conf.element("token").getText(); 
			String gateway = conf.element("gateway").getText();
			log.info("freshbooks configuration, uri: "+uri+", token: "+token+", gateway: "+gateway);
			
			FreshbooksManager manager = getFreshbooksManager(inStore.getCatalogId());
			manager.setToken(token);
			manager.setUrl(uri);
			manager.setGateway(gateway);
		    
		    FreshbooksStatus inStatus = new FreshbooksStatus();
		    manager.processOrder(inOrder, inStatus);
		    
		    //need to map the freshbooks invoice state to an Order state
		    String mappedState = FreshbooksManager.getOrderStatusFromInvoice(inOrder,inStatus);
		    OrderState orderState = inStore.getOrderState(mappedState);
		    if (mappedState.equals(Order.REJECTED)){
		    	log.warn("Transaction DECLINED for order #" + inOrder.getId());
		    	log.warn("Freshbooks Response Code:" + inStatus.getErrorCode());
		    	log.warn("Freshbooks Reason Text: " + inStatus.getErrorMessage());
		    	orderState.setDescription(inStatus.getErrorMessage());
		    	orderState.setOk(false);
		    } else {//may be authorized or accepted
		    	String message = mappedState.equals(Order.ACCEPTED) ? "Pending Authorization from Freshbooks" : "Authorized by Freshbooks";
		    	orderState.setDescription(message);
		    	orderState.setOk(true);
		    	//update invoice profile table
			    updateInvoiceProfiles(inStore, inOrder,inStatus);
		    }
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
	
	public void updateInvoiceProfiles(Store inStore, Order inOrder, FreshbooksStatus inStatus) throws Exception{
		//update invoice profile table with invoice_id and list of recurring profile info
		SearcherManager sm = getSearcherManager();
		Searcher searcher = sm.getSearcher(inStore.getCatalogId(), "invoiceprofile");
		Data data = searcher.createNewData();
		data.setProperty("orderid", inOrder.getId());
		data.setProperty("querytype", "invoice");
		data.setProperty("invoicefrequency", "weekly");//default is weekly so put that
		data.setProperty("invoiceoccurrence", "1");
		data.setProperty("remoteid", inStatus.getInvoiceId());
		data.setProperty("remotestatus", inStatus.getInvoiceStatus());
		if ( FreshbooksManager.isInvoicePaid(inStatus.getInvoiceStatus()) ){
			data.setProperty("requiresupdate", "false");
			data.setProperty("querystate","ok");
		} else {
			data.setProperty("requiresupdate", "true");
			data.setProperty("querystate","retry");
		}
		data.setProperty("querycount", "0");
		searcher.saveData(data, null);
		if (!inStatus.getRecurringProfiles().isEmpty()){
			for (RecurringProfile profile: inStatus.getRecurringProfiles()){
				Data pdata = searcher.createNewData();
				pdata.setProperty("orderid", inOrder.getId());
				pdata.setProperty("querytype", "recurringprofile");
				pdata.setProperty("invoicefrequency", profile.getFrequency());
				pdata.setProperty("invoiceoccurrence", profile.getOccurrence());
				if (profile.getRecurringId() !=null){
					pdata.setProperty("remoteid", profile.getRecurringId());
					pdata.setProperty("requiresupdate","false");
					pdata.setProperty("querystate","ok");
				} else {
					pdata.setProperty("requiresupdate","true");
					pdata.setProperty("querystate","retry");
				}
				pdata.setProperty("querycount", "0");
				searcher.saveData(pdata, null);
			}
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
