package org.openedit.store.gateway;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.SearcherManager;
import org.openedit.money.Money;
import org.openedit.page.manage.PageManager;
import org.openedit.store.CartItem;
import org.openedit.store.Coupon;
import org.openedit.store.CreditPaymentMethod;
import org.openedit.store.Store;
import org.openedit.store.StoreException;
import org.openedit.store.customer.Address;
import org.openedit.store.orders.BaseOrderProcessor;
import org.openedit.store.orders.Order;
import org.openedit.store.orders.OrderState;
import org.openedit.store.orders.Refund;
import org.openedit.users.UserManager;
import org.openedit.util.XmlUtil;

import com.stripe.Stripe;
import com.stripe.model.ApplicationFee;
import com.stripe.model.ApplicationFeeCollection;
import com.stripe.model.BalanceTransaction;
import com.stripe.model.Charge;
import com.stripe.model.Fee;


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
		// if the store is set to stripe 
		//		and if the order is not set to something else
		//	then return true
		//this way we can have specific orders setup to go through specific gateways
		if (inStore.get("gateway") !=null && inStore.get("gateway").equals("stripe")){
			if (inOrder.get("gateway") == null || (inOrder.get("gateway").equals("stripe")) ){
				return true;
			}
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
		
		if(inOrder.get("stripetoken") == null){
			//not a stripe order so abort
			//maybe move this check to requiresValidation()?
			log.error("cannot find stripetoken, aborting");
			return;	
		}
		
		//if gateway has not been set, set it so that we can identify where
		// to issue refunds
		if (inOrder.get("gateway") == null){
			inOrder.setProperty("gateway","stripe");
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
		Map<String, Object> chargeParams = new HashMap<String, Object>();
		Money totalprice = inOrder.getTotalPrice();
		//stripe connect: use access_token generated by oauth in place of 
		//secretkey; also define application fee (application_fee parameter)
		Data setting = null;
		boolean forcetestmode = inOrder.getBoolean("forcetestmode");
		if(inStore.isProductionMode() && !forcetestmode){
			 setting = getSearcherManager().getData(inStore.getCatalogId(), "catalogsettings", "stripe_access_token");
		} else{
			 setting = getSearcherManager().getData(inStore.getCatalogId(), "catalogsettings", "stripe_test_access_token");
		}
		if (setting!=null && setting.get("value")!=null){
			String access_token = setting.get("value");
			Money fee = calculateFee(inStore,inOrder);
			if (fee.isNegative()){ //error state
				log.info("Fee is negative for this order "+inOrder.toString()+", rejecting");
				OrderState orderState = inStore.getOrderState(Order.REJECTED);
				orderState.setDescription("Configuration error: fee structure is invalid");
				orderState.setOk(false);
				inOrder.setOrderState(orderState);
				return;
			}
			Money delta = totalprice.subtract(fee);
			if (delta.isNegative()){
				log.error("Configuration error: fee for processing is too big, aborting");
				OrderState orderState = inStore.getOrderState(Order.REJECTED);
				orderState.setDescription("Configuration error: fee structure is invalid");
				orderState.setOk(false);
				inOrder.setOrderState(orderState);
				return;
			}
			String feestring = fee.toShortString().replace(".", "").replace("$", "").replace(",", "");
			Stripe.apiKey = access_token;//does not matter if production or not
			chargeParams.put("application_fee",feestring);//amount in cents
		} else {
			//process as usual
			//check if an administrator has ordered test mode transaction
			if(inStore.isProductionMode() && !inOrder.getCart().getBoolean("forcetestmode")){
				Stripe.apiKey = inStore.get("secretkey");//livesecretkey or secretkey
			} else{
				Stripe.apiKey = inStore.get("testsecretkey");
			}
		}
		String amountstring = totalprice.toShortString().replace(".", "").replace("$", "").replace(",", "");
		chargeParams.put("amount", amountstring);
		String currency = inStore.get("currency");
		if(currency == null){
			currency = "cad";
		}
		chargeParams.put("currency", currency);
		chargeParams.put("card", inOrder.get("stripetoken")); // obtained via js
		
		String descriptor = inStore.get("statement_descriptor");
		if(descriptor != null){
			chargeParams.put("statement_descriptor", descriptor);
		}
		
		
		
		// Stripe.js
		Map<String,String> initialMetadata = new HashMap<String,String>();
		populateMetadata(inOrder,initialMetadata);
		chargeParams.put("description",inOrder.getOrderNumber());
		chargeParams.put("metadata", initialMetadata);
		try
		{
			Charge c = Charge.create(chargeParams);
			OrderState orderState = inStore.getOrderState(Order.AUTHORIZED);
			String balancetransaction = c.getBalanceTransaction();
			
			BalanceTransaction balance = BalanceTransaction.retrieve(balancetransaction);
			int fee = balance.getFee();
			float moneyval = (float)fee / 100;
			inOrder.setProperty("fee", String.valueOf(moneyval));
			List<Fee> details = balance.getFeeDetails();
			for (Iterator<Fee> iterator = details.iterator(); iterator.hasNext();)
			{
				Fee fee2 = iterator.next();
				float feeval = (float)fee2.getAmount() / 100;
				if("stripe_fee".equals(fee2.getType())){
					inOrder.setProperty("stripefee", String.valueOf(feeval));
				}
				if("application_fee".equals(fee2.getType())){
					inOrder.setProperty("profitshare", String.valueOf(feeval));
				}
			}
			inOrder.setProperty("balancetransaction", balancetransaction);
			float net = (float) balance.getNet() / 100;
			inOrder.setProperty("net", String.valueOf(net));
			inOrder.setProperty("stripechargeid", c.getId());
			
			//handle application fees
			
			
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
		if (!requiresValidation(inStore, inOrder)) {
			return;
		}
		log.info("refunding order with Stripe");
		if(inOrder.get("stripetoken") == null || inOrder.get("stripechargeid")==null){
			log.error("cannot find stripetoken, aborting");
			return;
		}
		String chargeId = inOrder.get("stripechargeid");
		//set application key
		boolean forcetestmode = inOrder.getBoolean("forcetestmode");
		Data setting = null;
		if(inStore.isProductionMode()){
			 setting = getSearcherManager().getData(inStore.getCatalogId(), "catalogsettings", "stripe_access_token");
		} else{
			 setting = getSearcherManager().getData(inStore.getCatalogId(), "catalogsettings", "stripe_test_access_token");
		}
		if (setting!=null && setting.get("value")!=null){
			Stripe.apiKey = setting.get("value");
		} else {
			//check if an administrator has ordered test mode transaction
			if(inStore.isProductionMode() && !forcetestmode){
				Stripe.apiKey = inStore.get("secretkey");//livesecretkey or secretkey
			} else{
				Stripe.apiKey = inStore.get("testsecretkey");
			}
		}
		try{
			//load the charge
			Charge c = Charge.retrieve(chargeId);
			if (c.getRefunded()){//this is true if fully refunded
				inRefund.setSuccess(false);
				inRefund.setMessage("Order has already been refunded");

				inRefund.setDate(new Date());
			} else {
				Integer total = c.getAmount();
				Integer totalrefunded = c.getAmountRefunded();
				Integer refundamount = Integer.parseInt(inRefund.getTotalAmount().toShortString().replace(".","").replace(",",""));
				if(refundamount > total){
					refundamount = total;
				}
				Map<String, Object> params = new HashMap<String, Object>();
				params.put("amount", String.valueOf(refundamount));
				com.stripe.model.Refund refund = c.getRefunds().create(params);
				inRefund.setSuccess(true);
				inRefund.setProperty("refundedby" , inContext.getUserName());

				inRefund.setAuthorizationCode(refund.getId());
				inRefund.setTransactionId(refund.getBalanceTransaction());
				inRefund.setDate(new Date());
				//calculate whether application fees should be handled
				//need to fix this logic:
				//handle application fees work only when we have one item on the cart 
				//and when the store is not setup to handle profit shares
				handleApplicationFees(chargeId,inRefund);
				
				//OLD!!!
//				Charge refundedCharge = c.refund();//refunds the full amount
//				if (refundedCharge.getRefunded()){
//					ChargeRefundCollection refunds = refundedCharge.getRefunds();
//					com.stripe.model.Refund refund = refunds.getData().get(0);
//					inRefund.setSuccess(true);
//					inRefund.setAuthorizationCode(refund.getId());
//					inRefund.setTransactionId(refund.getBalanceTransaction());
//					inRefund.setDate(new Date());
//					//client was refunded at this point, but
//					//partners have not been
//					//handle this at the end
//					handleApplicationFees(chargeId,inRefund);
//					
//				} else {
//					inRefund.setSuccess(false);
//					inRefund.setMessage("Order could not be refunded");
//					inRefund.setDate(new Date());
//				}
			}
		}catch (Exception e){
			inRefund.setSuccess(false);
			inRefund.setMessage("An error occurred while processing your transaction.");
			e.printStackTrace();
			throw new StoreException(e);
		}
	}
	
	protected void handleApplicationFees(String inChargeId, Refund inRefund){
		try{
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("charge",inChargeId);
			ApplicationFeeCollection collection = ApplicationFee.all(params);
			List<ApplicationFee> fees  = collection.getData();
			if (fees.size() > 0){
				//refund all fees
				StringBuilder buf = new StringBuilder();
				for (ApplicationFee fee:fees){
					//here we will probably have to refund partial amounts
					if (!fee.getRefunded()){
						ApplicationFee refundedFee = fee.refund();
						if (!refundedFee.getRefunded()){
							buf.append("ID:").append(fee.getId()).append(" ");
						}
					}
				}
				if (buf.toString().isEmpty()){
					inRefund.setMessage("All application fees were refunded");
				} else {
					inRefund.setMessage("Unable to refund all application fees, "+buf.toString().trim());
				}
			} else {
				inRefund.setMessage("No application fees were found on this order");
			}
		}catch (Exception e){
			log.error(e.getMessage(), e);
		}
	}
	
	protected void populateMetadata(Order inOrder, Map<String,String> inMetadata){
		inMetadata.put("firstname", inOrder.getCustomer().getFirstName());
		inMetadata.put("lastname", inOrder.getCustomer().getLastName());
		inMetadata.put("email", inOrder.getCustomer().getEmail());
		if (inOrder.getPaymentMethod() instanceof CreditPaymentMethod){
			String cardholdername = ((CreditPaymentMethod) inOrder.getPaymentMethod()).getCardHolderName();
			if (cardholdername!=null && cardholdername.isEmpty()==false){
				inMetadata.put("cardholdername",cardholdername);
			}
		}
		Address billing = inOrder.getCustomer().getBillingAddress();
		inMetadata.put("billingaddress",billing.toString());
		Address shipping = inOrder.getCustomer().getShippingAddress();
		inMetadata.put("shippingaddress",shipping.toString());
		Iterator<?> itr = inOrder.getItems().iterator();
		for(int i=1; itr.hasNext(); i++){
			CartItem item = (CartItem) itr.next();
			String sku = item.getSku();
			String name = item.getName();
			Money price = item.getYourPrice();
			StringBuilder buf = new StringBuilder();
			buf.append(sku).append(": ");
			if (Coupon.isCoupon(item)){
				buf.append("Coupon - ");
			}
			buf.append(name).append(" ").append(price.toShortString());
			inMetadata.put("cartitem-"+i, buf.toString());
		}
	}
}
