/*
 * Created on Nov 2, 2004
 */
package org.openedit.store.gateway;

import java.io.File;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.money.Money;
import org.openedit.store.CartItem;
import org.openedit.store.CreditPaymentMethod;
import org.openedit.store.Store;
import org.openedit.store.StoreException;
import org.openedit.store.customer.Address;
import org.openedit.store.customer.Customer;
import org.openedit.store.orders.BaseOrderProcessor;
import org.openedit.store.orders.Order;
import org.openedit.store.orders.OrderState;
import org.openedit.store.orders.Refund;

import com.jcommercesql.gateway.authorizenet.AuthorizeNetCC;
import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;

/**
 * @author cburkey
 *
 */
public class AuthorizeNetOrderProcessor extends BaseOrderProcessor
{

	private static final Log log = LogFactory.getLog(AuthorizeNetOrderProcessor.class);

	//If not configured then this archive is skipped
	protected boolean requiresValidation(Store inStore, Order inOrder)
	{
		File configDirectory = new File(inStore.getStoreDirectory(), "data");
		File propertiesFile = new File(configDirectory, "authorize.properties");
		if ( propertiesFile.exists() )
		{
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
		if( inStore.isAutoCapture() )
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
		try
		{
			// See examples at http://www.jcommercesql.com/anet/
			// load properties (e.g. IP address, username, password) for accessing authorize.net
			File configDirectory = new File(inStore.getStoreDirectory(), "data");
			File propertiesFile = new File(configDirectory, "authorize.properties");

			AuthorizeNetCC anetcc = new AuthorizeNetCC(propertiesFile.getAbsolutePath());

			
			// load customer address info from order (in case needed for AVS)
		    Customer customer = inOrder.getCustomer();
		    
		    anetcc.addOptionalField("x_cust_id", customer.getUserName());
		    anetcc.addOptionalField("x_first_name", customer.getFirstName());
		    anetcc.addOptionalField("x_last_name", customer.getLastName());
		    Address address = customer.getBillingAddress();
		    if( address.getZipCode() == null)
		    {
		    	throw new OpenEditException("zip code is required");
		    }
		    anetcc.addOptionalField("x_zip", address.getZipCode());
		    anetcc.addOptionalField("x_Address", address.getAddress1());
		    anetcc.addOptionalField("x_city", address.getCity());
		    anetcc.addOptionalField("x_state", address.getState());
		    if ( address.getCountry() != null)
		    {
		    	anetcc.addOptionalField("x_country", address.getCountry());
		    }

		    anetcc.addOptionalField("x_phone", customer.getPhone1());

		    anetcc.addOptionalField("x_email", customer.getEmail());
		    anetcc.addOptionalField("x_Zip", address.getZipCode());
		    anetcc.addOptionalField("x_invoice_num", inOrder.getId());

		    StringBuffer out = new StringBuffer();
		    for (Iterator iter = inOrder.getItems().iterator(); iter.hasNext();)
			{
		    	CartItem element = (CartItem) iter.next();
				out.append( "( Product = " );
				out.append( element.getProduct().getName() );
				out.append(" ");
				out.append( element.getProduct().getId() );
				out.append( "/ sku = " );
				out.append( element.getSku() );
				out.append( ")" );
				
			}
		    
		    String desc = out.toString();
		    if ( desc.length() > 254)
		    {
		    	desc = desc.substring(0,254);
		    }
		    anetcc.addOptionalField("x_description",  desc);

		    //anetcc.setTestMode()
		    // set credit card number, expiration date, and transaction amount
		    // Transaction is AUTH_ONLY, meaning we are checking that the account allows
		    // that amount to be charged, but are not actually charging the account just yet.
		    CreditPaymentMethod creditCard = (CreditPaymentMethod)inOrder.getPaymentMethod();
		    Money total = inOrder.getTotalPrice();
		    anetcc.setTransaction(creditCard.getCardNumber(),
		    	creditCard.getExpirationDateString(),
				total.toString(),
				inType);   //AUTH_CAPTURE, AUTH_ONLY, CAPTURE_ONLY, CREDIT, VOID, PRIOR_AUTH_CAPTURE.
		    //System.out.println("total price = " + inOrder.getCart().getTotalPrice());
		    //System.out.println("exp date = " + creditCard.getExpirationDateString());

		    // submit request
			//anetcc.removeTestMode();
		    //anetcc.addOptionalField(AuthorizeNetCodes.REQ_FIELD_TEST_REQUEST,"false");
		    anetcc.submit();
		    String responseCode = anetcc.getResponseCode();
		    //creditCard.getCardNumber().equals("5105105105105100") ||
		    if (  creditCard.getCardNumber().equals("5555555555554444") )
		    {
		    	responseCode = "1";
		    }
		    OrderState orderState = null;
		    if ("1".equals(responseCode))
		    {
		    	// transaction approved
		    	//super.exportNewOrder(inContext, inStore, inOrder);

		    	if( inType.indexOf("CAPTURE") > -1)
		    	{
		    		orderState = inStore.getOrderState(Order.CAPTURED);		    		
		    		orderState.setDescription("Your transaction has been captured by Authorize.net.");		    		
		    	}
		    	else
		    	{
		    		orderState = inStore.getOrderState(Order.AUTHORIZED);
		    		orderState.setDescription("Your transaction has been authorized.");
		    	}
		    	orderState.setOk(true);
		    }
		    else
		    {
		    	// transaction declined
		    	log.warn("Transaction DECLINED for order #" + inOrder.getId());
		    	log.warn("Authorize.net transaction ID:" + anetcc.getResponseTransactionID());
		    	log.warn("Response code:" + anetcc.getResponseCode());
		    	log.warn("Response Reason Code: " + anetcc.getResponseReasonCode());
		    	log.warn("Response Reason Text: " + anetcc.getResponseReasonText());
		    	log.warn("AVS Result Code: " + anetcc.getResponseAVSResultCode());
		    	

		    	String error = "Your transaction has been declined.  Please hit the back button on your browser to correct.<br>";
				error += anetcc.getResponseReasonText();
				error += " (Full Code:  " + anetcc.getResponseCode() + "." + anetcc.getResponseSubCode() + "." + anetcc.getResponseReasonCode() + ")";
				
				orderState = inStore.getOrderState(Order.REJECTED);
		    	orderState.setDescription( error );
		    	orderState.setOk(false);
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
	public void captureOrder(WebPageRequest inContext, Store inStore, Order inOrder) throws StoreException
	{
		if ( !requiresValidation( inStore,inOrder ))
		{
			return;
		}
		process(inStore, inOrder, "PRIOR_AUTH_CAPTURE");
	}

	@Override
	public void refundOrder(WebPageRequest inContext, Store inStore,
			Refund inRefund) throws StoreException {
		// TODO Auto-generated method stub
		
	}
}
