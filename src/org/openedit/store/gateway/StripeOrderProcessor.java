package org.openedit.store.gateway;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.openedit.data.SearcherManager;
import org.openedit.store.Store;
import org.openedit.store.StoreException;
import org.openedit.store.orders.BaseOrderProcessor;
import org.openedit.store.orders.Order;
import org.openedit.store.orders.OrderState;
import org.openedit.store.orders.Refund;

import com.openedit.WebPageRequest;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.users.UserManager;
import com.openedit.util.XmlUtil;
import com.stripe.Stripe;
import com.stripe.model.Charge;

public class StripeOrderProcessor extends BaseOrderProcessor
{

	private static final Log log = LogFactory.getLog(StripeOrderProcessor.class);
	protected PageManager fieldPageManager;
	protected XmlUtil fieldXmlUtil;
	protected BeanstreamUtil fieldBeanstreamUtil;

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager fieldSearcherManager)
	{
		this.fieldSearcherManager = fieldSearcherManager;
	}

	protected UserManager fieldUserManager;
	protected SearcherManager fieldSearcherManager;

	public XmlUtil getXmlUtil()
	{
		if (fieldXmlUtil == null)
		{
			fieldXmlUtil = new XmlUtil();
		}
		return fieldXmlUtil;
	}

	public void setXmlUtil(XmlUtil inXmlUtil)
	{
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

	public UserManager getUserManager()
	{
		return fieldUserManager;
	}

	public void setUserManager(UserManager inUserManager)
	{
		fieldUserManager = inUserManager;
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	protected boolean requiresValidation(Store inStore, Order inOrder)
	{

		Page page = getPageManager().getPage("/WEB-INF/data/" + inStore.getCatalogId() + "/configuration/stripe.xml");

		if (page.exists())
		{
			return inOrder.getPaymentMethod().requiresValidation();
		}
		return false;
	}

	public void processNewOrder(WebPageRequest inContext, Store inStore, Order inOrder) throws StoreException
	{
		if (!requiresValidation(inStore, inOrder))
		{
			return;
		}
		String stripetoken = inContext.getRequestParameter("stripeToken");
		inOrder.setProperty("stripetoken", stripetoken);
		// "AUTH_ONLY"); //AUTH_CAPTURE, AUTH_ONLY, CAPTURE_ONLY, CREDIT, VOID,
		// PRIOR_AUTH_CAPTURE.
		if (inStore.isAutoCapture())
		{
			process(inStore, inOrder, "AUTH_CAPTURE");
		}
		else
		{
			process(inStore, inOrder, "AUTH_ONLY");
		}
	}

	protected void process(Store inStore, Order inOrder, String inType) throws StoreException
	{
		log.info("processing order with Stripe");

		// See examples at http://www.jcommercesql.com/anet/
		// load properties (e.g. IP address, username, password) for
		// accessing authorize.net
		// load customer address info from order (in case needed for AVS)
		Page page = getPageManager().getPage("/WEB-INF/data/" + inStore.getCatalogId() + "/configuration/stripe.xml");
		Element conf = getXmlUtil().getXml(page.getReader(), "UTF-8");
		String testkey = conf.element("testkey").getText();
		String prodkey = conf.element("prodkey").getText();
		if(inStore.isProductionMode()){
			Stripe.apiKey = prodkey;
		} else{
			Stripe.apiKey = testkey;
		}
	
		Map<String, Object> chargeParams = new HashMap<String, Object>();
		String amountstring = inOrder.getTotalPrice().toShortString();
		amountstring = amountstring.replace(".", "").replace("$", "").replace(",", "");
		chargeParams.put("amount", amountstring);
		chargeParams.put("currency", "cad");
		chargeParams.put("card", inOrder.get("stripetoken")); // obtained with
																// Stripe.js
		chargeParams.put("description", "Charge for test@example.com");
		// chargeParams.put("metadata", initialMetadata);

		try
		{
			Object c = Charge.create(chargeParams);
			OrderState orderState = inStore.getOrderState(Order.AUTHORIZED);
			// inOrder.setProperty("transactionid",
			// pairs.get("trnId").toString());
			orderState.setDescription("Your transaction has been authorized.");
			orderState.setOk(true);
			inOrder.setOrderState(orderState);
			

		}

		catch (Exception e)
		{
			OrderState orderState = inStore.getOrderState(Order.REJECTED);
			e.printStackTrace();
			orderState.setDescription("An error occurred while processing your transaction.");
			orderState.setOk(false);
			inOrder.setOrderState(orderState);
		}

	}

	public void captureOrder(WebPageRequest inContext, Store inStore, Order inOrder) throws StoreException
	{
		if (!requiresValidation(inStore, inOrder))
		{
			return;
		}
		process(inStore, inOrder, "");
	}

	@Override
	public void refundOrder(WebPageRequest inContext, Store inStore, Order inOrder, Refund inRefund) throws StoreException
	{
	//	getBeanstreamUtil().refund(inStore, inOrder, inRefund);
	}
}
