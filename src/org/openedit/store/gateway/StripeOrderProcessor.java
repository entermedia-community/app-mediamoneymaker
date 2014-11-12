package org.openedit.store.gateway;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.data.SearcherManager;
import org.openedit.money.Fraction;
import org.openedit.money.Money;
import org.openedit.store.CartItem;
import org.openedit.store.Coupon;
import org.openedit.store.Store;
import org.openedit.store.StoreException;
import org.openedit.store.customer.Address;
import org.openedit.store.orders.BaseOrderProcessor;
import org.openedit.store.orders.Order;
import org.openedit.store.orders.OrderState;
import org.openedit.store.orders.Refund;

import com.openedit.WebPageRequest;
import com.openedit.page.manage.PageManager;
import com.openedit.users.UserManager;
import com.openedit.util.XmlUtil;
import com.stripe.Stripe;
import com.stripe.model.ApplicationFee;
import com.stripe.model.ApplicationFeeCollection;
import com.stripe.model.BalanceTransaction;
import com.stripe.model.Charge;
import com.stripe.model.ChargeRefundCollection;


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
			log.error("cannot find stripetoken, aborting");
//			OrderState orderState = inStore.getOrderState(Order.REJECTED);
//			orderState.setDescription("Configuration error: stripe token not found");
//			orderState.setOk(false);
//			inOrder.setOrderState(orderState);
			return;	
		}
		
		Map<String, Object> chargeParams = new HashMap<String, Object>();
		Money totalprice = inOrder.getTotalPrice();
		//stripe connect: use access_token generated by oauth in place of 
		//secretkey; also define application fee (application_fee parameter)
		Data setting = getSearcherManager().getData(inStore.getCatalogId(), "catalogsettings", "stripe_access_token");
		if (setting!=null && setting.get("value")!=null){
			String access_token = setting.get("value");
			Money fee = calculateFee(inStore,inOrder);
			if (fee.isNegative() || fee.isZero()){ //error state
				log.info("Fee is negative for this order "+inOrder.toString()+", rejecting");
				OrderState orderState = inStore.getOrderState(Order.REJECTED);
				orderState.setDescription("Configuration error: fee structure is invalid");
				orderState.setOk(false);
				inOrder.setOrderState(orderState);
				return;
			}
			totalprice = totalprice.subtract(fee);
			if (totalprice.isNegative()){
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
		chargeParams.put("currency", "cad");
		chargeParams.put("card", inOrder.get("stripetoken")); // obtained via js
		
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
	
	protected Money calculateFee(Store inStore, Order inOrder){
		Money totalFee = new Money("0");
		@SuppressWarnings("unchecked")
		Iterator<CartItem> itr = inOrder.getItems().iterator();
		while(itr.hasNext()){
			CartItem item = itr.next();
			if (item.getProduct().isCoupon()){
				continue;
			}
			String fee = item.getProduct().get("partnershipfee");
			String type = item.getProduct().get("partnershipfeetype");
			if (fee!=null && type!=null){
				if (type.equals("flatrate")){
					Money money = new Money(fee);
					if (money.isNegative() || money.isZero()){
						continue;
					}
					totalFee = totalFee.add(money);
				} else if (type.equals("percentage")){
					Money itemprice = item.getTotalPrice();
					double rate = Double.parseDouble(fee);
					if (rate < 0.0d || rate > 1.0d){
						continue;
					}
					Money money = itemprice.multiply(new Fraction(rate));
					totalFee = totalFee.add(money);
				}
			}
		}
		if (totalFee.isZero() && inStore.get("fee_structure")!=null){
			String fee_structure = inStore.get("fee_structure");
			double rate = Double.parseDouble(fee_structure);
			totalFee = new Money(rate);
			if (rate < 1.0d){
				totalFee = inOrder.getSubTotal().multiply(new Fraction(rate));
			}
		}
		return totalFee;
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
		try{
			Charge c = Charge.retrieve(chargeId);
			if (c.getRefunded()){
				inRefund.setSuccess(false);
				inRefund.setMessage("Order has already been refunded");
				inRefund.setDate(new Date());
			} else {
				Charge refundedCharge = c.refund();//refunds the full amount
				if (refundedCharge.getRefunded()){
					ChargeRefundCollection refunds = refundedCharge.getRefunds();
					com.stripe.model.Refund refund = refunds.getData().get(0);
					inRefund.setSuccess(true);
					inRefund.setAuthorizationCode(refund.getId());
					inRefund.setTransactionId(refund.getBalanceTransaction());
					inRefund.setDate(new Date());
					//client was refunded at this point, but
					//partners have not been
					//handle this at the end
					handleApplicationFees(chargeId,inRefund);
					
				} else {
					inRefund.setSuccess(false);
					inRefund.setMessage("Order could not be refunded");
					inRefund.setDate(new Date());
				}
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
			}
		}catch (Exception e){
			log.error(e.getMessage(), e);
		}
	}
	
	protected void populateMetadata(Order inOrder, Map<String,String> inMetadata){
		inMetadata.put("firstname", inOrder.getCustomer().getFirstName());
		inMetadata.put("lastname", inOrder.getCustomer().getLastName());
		inMetadata.put("email", inOrder.getCustomer().getEmail());
		inMetadata.put("phone", inOrder.getCustomer().getPhone1()!=null ? inOrder.getCustomer().getPhone1() : "");
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
