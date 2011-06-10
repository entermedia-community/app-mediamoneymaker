package org.openedit.cart.paypal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.store.CreditPaymentMethod;
import org.openedit.store.Store;
import org.openedit.store.StoreException;
import org.openedit.store.customer.Address;
import org.openedit.store.customer.Customer;
import org.openedit.store.orders.BaseOrderProcessor;
import org.openedit.store.orders.Order;
import org.openedit.store.orders.OrderState;

import com.openedit.OpenEditRuntimeException;
import com.openedit.WebPageRequest;
import com.openedit.users.User;
import com.openedit.users.UserManager;
import com.paypal.sdk.core.nvp.NVPDecoder;
import com.paypal.sdk.core.nvp.NVPEncoder;
import com.paypal.sdk.exceptions.PayPalException;
import com.paypal.sdk.profiles.APIProfile;
import com.paypal.sdk.profiles.ProfileFactory;
import com.paypal.sdk.services.NVPCallerServices;

public class DirectPaymentsProcessor extends BaseOrderProcessor
{
	private static final Log log = LogFactory.getLog(DirectPaymentsProcessor.class);
	protected UserManager fieldUserManager;
	protected NVPCallerServices caller;
	protected NVPEncoder encoder;
	protected NVPDecoder decoder;
	protected WebPageRequest fieldContext;

	public NVPEncoder getEncoder()
	{
		if (encoder == null)
		{
			encoder = new NVPEncoder();
			
		}

		return encoder;
	}

	public void setEncoder(NVPEncoder inEncoder)
	{
		encoder = inEncoder;
	}

	public NVPDecoder getDecoder()
	{
		if (decoder == null)
		{
			decoder = new NVPDecoder();
			
		}

		return decoder;
	}

	public void setDecoder(NVPDecoder inDecoder)
	{
		decoder = inDecoder;
	}

	public NVPCallerServices getCaller()
	{
		if (caller == null)
		{
			caller = new NVPCallerServices();

		}

		return caller;
	}

	public void setCaller(NVPCallerServices inCaller)
	{
		caller = inCaller;
	}

	public UserManager getUserManager()
	{
		return fieldUserManager;
	}

	public void setUserManager(UserManager inUserManager)
	{
		fieldUserManager = inUserManager;
	}

	public void processNewOrder(WebPageRequest inContext, Store inStore, Order inOrder) throws StoreException
	{
		
		if (!inOrder.getPaymentMethod().requiresValidation()) {
			return;
		}
	/*
		String ip = inContext.getRequest().getRemoteAddr();
		log.info("processing request from: " + ip);
		*/
		User paypalUser = getUserManager().getUser("paypaluser");
		
		
		boolean usepaypal = Boolean.parseBoolean(inContext.getPageProperty("usedirectpayment"));
		if (!usepaypal)
		{
			return;
		}
		
		
		if (paypalUser == null)
		{
			throw new OpenEditRuntimeException("You must create a paypal user with credentials to use the paypal checkout.");
		}

		try
		{

			APIProfile profile = ProfileFactory.createSignatureAPIProfile();
			/*
			 * WARNING: Do not embed plaintext credentials in your application
			 * code. Doing so is insecure and against best practices. Your API
			 * credentials must be handled securely. Please consider encrypting
			 * them for use in any production environment, and ensure that only
			 * authorized individuals may view or modify them.
			 */
			// Set up your API credentials, PayPal end point, API operation and
			// version.
			//Ian's sandbox
			//Fake credit card: 4273454590310042
			//Test User
			//01/10
			//Visa
			//1 Main St, San Jose, CA 95131, United States
			String username = inContext.getPageProperty("paypalusername");
			profile.setAPIUsername(username);
			String password = getUserManager().decryptPassword(paypalUser);
			String signature = inContext.getPageProperty("paypalsignature");
			profile.setAPIPassword(password);
			profile.setSignature(signature);
			String environment = inContext.getPageProperty("environment");
			profile.setEnvironment(environment);
			profile.setSubject( "" );
			getCaller().setAPIProfile(profile);
			log.info(username);
			log.info(password);
			log.info(signature);
			log.info( "Order ID: " + inOrder.getId() );
			getEncoder().add("VERSION", "53.0");
			getEncoder().add("METHOD", "DoDirectPayment");

			// Add request-specific fields to the request string.

			String paymentAction = null;
			if (inStore.isAutoCapture())
			{
				paymentAction = "Sale";
			}
			else
			{
				paymentAction = "Sale";
			}
			getEncoder().add("PAYMENTACTION", paymentAction);
			String currencycode = inContext.getPageProperty("currency");
			if(currencycode != null){
				getEncoder().add("CURRENCYCODE", currencycode);
			}
			
			encodeOrder( inOrder );
			if ( includeShippingData( inContext ) )
			{
				encodeShipping( inOrder.getCustomer() );
			}
			// Execute the API operation and obtain the response.
			String NVPRequest = getEncoder().encode();
			String NVPResponse = callPayPal( NVPRequest );
			getDecoder().decode(NVPResponse);
			String responseCode = getDecoder().get("ACK");

			// Now look at the response

			OrderState orderState = null;
			if ("success".equalsIgnoreCase(responseCode) || "SuccessWithWarning".equalsIgnoreCase(responseCode))
			{

				orderState = inStore.getOrderState(Order.AUTHORIZED);
				orderState.setDescription("Your transaction has been authorized.");

				orderState.setOk(true);
				log.info("Order successfully processed with PayPal");
			}
			else
			{
				
				// transaction declined
				log.info("Transaction DECLINED for order #" + inOrder.getId() );
				log.info( getDecoder().toString() );
				// log.warn("Authorize.net transaction ID:" +
				// anetcc.getResponseTransactionID());
				// log.warn("Response code:" + anetcc.getResponseCode());
				// log.warn("Response Reason Code: " +
				// anetcc.getResponseReasonCode());
				// log.warn("Response Reason Text: " +
				// anetcc.getResponseReasonText());
				// log.warn("AVS Result Code: " +
				// anetcc.getResponseAVSResultCode());
				//			    	

				String error = "Your transaction has been declined.  Please hit the back button on your browser to correct.<br>";
				// error += anetcc.getResponseReasonText();
				// error += " (Full Code:  " + anetcc.getResponseCode() + "." +
				// anetcc.getResponseSubCode() + "." +
				// anetcc.getResponseReasonCode() + ")";

				orderState = inStore.getOrderState(Order.REJECTED);
				orderState.setDescription(error);
				orderState.setOk(false);
				
			}
			inOrder.setOrderState(orderState);
		}
		catch (Exception e)
		{
			OrderState orderState = new OrderState();
			orderState.setDescription("An error occurred while processing your transaction.");
			orderState.setOk(false);
			inOrder.setOrderState(orderState);
			inOrder.getOrderStatus();
			e.printStackTrace();
			throw new StoreException(e);
		}

	}

	protected String callPayPal( String NVPRequest ) throws PayPalException
	{
		return (String) getCaller().call(NVPRequest);
	}

	protected void encodeOrder( Order inOrder )
	{
		CreditPaymentMethod creditCard = (CreditPaymentMethod) inOrder.getPaymentMethod();
		String year = creditCard.getExpirationYearString();
		if(!year.startsWith("20")){
			year = "20" + year;
		}
		String datestring = creditCard.getExpirationMonthString() + year;
		
		getEncoder().add("AMT", inOrder.getTotalPrice().toString());
		getEncoder().add("CREDITCARDTYPE", creditCard.getCreditCardType().getId());
		getEncoder().add("ACCT", creditCard.getCardNumber());
		
		getEncoder().add("EXPDATE", datestring);
		getEncoder().add("CVV2", creditCard.getCardVerificationCode());
		Customer customer = inOrder.getCustomer();
		encodeCustomer( customer );
		
		getEncoder().add("INVNUM", inOrder.getOrderNumber());
	}

	protected void encodeCustomer( Customer customer )
	{
		getEncoder().add("FIRSTNAME", customer.getFirstName());
		getEncoder().add("LASTNAME", customer.getLastName());
		Address address = customer.getBillingAddress();
		getEncoder().add("STREET", address.getAddress1());
		getEncoder().add("CITY", address.getCity());
		getEncoder().add("STATE", address.getState());
		getEncoder().add("ZIP", address.getZipCode());
		getEncoder().add("COUNTRYCODE", address.getCountry());
		 
	}

	protected void encodeShipping( Customer customer )
	{
		Address shipping = customer.getShippingAddress();
		getEncoder().add("SHIPTONAME", customer.getFirstName() + customer.getLastName());
		getEncoder().add("SHIPTOSTREET", shipping.getAddress1());
		getEncoder().add("SHIPTOSTREET2", shipping.getAddress2());
		getEncoder().add("SHIPTOCITY", shipping.getCity());
		getEncoder().add("SHIPTOZIP", shipping.getZipCode());
		getEncoder().add("SHIPTOCOUNTRY", shipping.getCountry());
		getEncoder().add("SHIPTOSTATE", shipping.getCountry());	
	}

	/**
	 * If page property "includeshippingdata" is set to "true", the DirectPaymentProcessor will include
	 * shipping data from the {@link org.openedit.store.customer.Customer} object to the DoDirectPayment PayPall call.
	 * @return
	 */
	protected boolean includeShippingData( WebPageRequest inContext )
	{
		return Boolean.parseBoolean( inContext.getPageProperty("includeshippingdata"));
	}
	// return getDecoder().get("ACK");


}
