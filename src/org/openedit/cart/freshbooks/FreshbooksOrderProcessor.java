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

	protected FreshbooksManager util;
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
			//check for test card before anything else
			CreditPaymentMethod creditCard = (CreditPaymentMethod)inOrder.getPaymentMethod();
			if (creditCard.getCardNumber().equals("5555555555554444"))
		    {
				OrderState orderState = null;
				orderState = inStore.getOrderState(Order.AUTHORIZED);
		    	orderState.setDescription("TEST ORDER");
		    	orderState.setOk(true);
		    	inOrder.setOrderState(orderState);
		    	return;
		    }
			
			Page page = getPageManager().getPage(inStore.getStoreHome() + "/configuration/freshbooks.xml");
			Element conf = getXmlUtil().getXml(page.getReader(), "UTF-8");
			
			String uri = conf.element("uri").getText();
			String token = conf.element("token").getText();
			String gateway = conf.element("gateway").getText();
			
			log.info("uri: "+uri+", token: "+token+", gateway: "+gateway);
			
			FreshbooksManager manager = new FreshbooksManager();
			manager.setToken(token);
			manager.setUrl(uri);
			manager.setGateway(gateway);
		    
		    FreshbooksStatus inStatus = new FreshbooksStatus();
		    populateFreshbooksVariables(inStatus,inOrder,inStore);
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
		    	String message = mappedState.equals(Order.ACCEPTED) ? "Pending Authorization by Freshbooks" : "Authorized by Freshbooks";
		    	orderState.setDescription(message);
		    	orderState.setOk(true);
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
	
	protected void populateFreshbooksVariables(FreshbooksStatus inStatus, Order inOrder, Store inStore) throws Exception {
		inStatus.setSendEmail("1");//send email 
		inStatus.setSendSnailMail("0");// don't send post mail
		inStatus.setBlocking(true);// blocking call when chaining a non-recurring with a recurring invoice
		inStatus.setDelayBetweenQueries(100);// 100ms between repeated queries (i.e., querying the status of an invoice)
		inStatus.setMaximumQueryRepeat(25);// perform only 25 queries at most
		inStatus.setOccurrences("0");//infinity
		
		String frequency = inOrder.get("frequency");
		if (frequency == null || !inOrder.containsRecurring()){
			frequency = "weekly";//if no recurring, default is weekly anyway
			//test mode!!
			//force the order to have recurring items
//			Iterator <?> itr = inOrder.getCart().getItems().iterator();
//			if(itr.hasNext()){
//				CartItem item = (CartItem) itr.next();
//				item.getProduct().setProperty("recurring", "true");
//			}
		}
		inStatus.setFrequency(frequency);
		//calculate the future date
		Date date = inOrder.getDate();
		SearcherManager manager = getSearcherManager();
		Searcher searcher = manager.getSearcher(inStore.getCatalogId(), "frequency");
		Data data = (Data) searcher.searchById(frequency);
		String day = data.get("day");
		String month = data.get("month");
		String year = data.get("year");
		long days = 0;
		if (!day.equals("0")){//days
			days = Long.parseLong(day);
		} else if (!year.equals("0")){ //years
			days = Integer.parseInt(year) * 365;
		} else { //months
			int months = Integer.parseInt(month);
			Calendar cal = new GregorianCalendar();
			cal.setTime(date);
			cal.add(Calendar.MONTH,months);
			days = (cal.getTimeInMillis() - date.getTime())/(24*60*60*1000);
		}
		long futureTime = date.getTime() + ((long)days) *24*60*60*1000;
		Date futureDate = new Date();
		futureDate.setTime(futureTime);
	    inStatus.setFirstRecurringInvoiceDate(futureDate);
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
