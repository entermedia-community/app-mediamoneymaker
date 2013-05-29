/*
 * Created on Nov 2, 2004
 */
package org.openedit.store.gateway;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.money.Money;
import org.openedit.store.CreditPaymentMethod;
import org.openedit.store.Store;
import org.openedit.store.StoreException;
import org.openedit.store.customer.Address;
import org.openedit.store.customer.Customer;
import org.openedit.store.orders.BaseOrderProcessor;
import org.openedit.store.orders.Order;
import org.openedit.store.orders.OrderState;
import org.openedit.store.orders.Refund;

import com.openecho.Echo;
import com.openedit.WebPageRequest;

/**
 * @author cburkey
 *  
 */
public class EchoOrderProcessor extends BaseOrderProcessor
{
	// Go here to see transactions: https://wwws.echo-inc.com/Review/
	private static final Log log = LogFactory.getLog(EchoOrderProcessor.class);
	protected boolean fieldAddRandomAmountToTotal = false;

	/*
	 * (non-javadoc)
	 * 
	 * @see com.openedit.store.EmailOrderArchive#exportNewOrder(com.openedit.WebPageContext,
	 *      com.openedit.store.Store, com.openedit.store.Order)
	 */
	public void processNewOrder(WebPageRequest inContext, Store inStore, Order inOrder)
		throws StoreException
	{
		// Load properties
		File configDirectory = new File(inStore.getStoreDirectory(), "data");
		File propertiesFile = new File(configDirectory, "echo.properties");
		FileInputStream stream = null;
		
		try
		{
			stream = new FileInputStream(propertiesFile);
		}
		catch (FileNotFoundException e1)
		{
			e1.printStackTrace();
		}
		Properties props = new Properties();
		try
		{
			props.load(stream);
		}
		catch (IOException e2)
		{
			e2.printStackTrace();
		}

		// id
		String id = props.getProperty("id");
		String pin = props.getProperty("pin");
		String test_id = props.getProperty("test-id");
		String test_pin = props.getProperty("test-pin");
		String use_test_id_str = props.getProperty("use-test-id");
		boolean use_test_id = new Boolean(use_test_id_str).booleanValue();
		// add_random
		String add_random_str = props.getProperty("add-random-amount");
		boolean add_random = new Boolean(add_random_str).booleanValue();
		setAddRandomAmountToTotal(add_random);
		// force grand total
		String force_grand_total = props.getProperty("force-grand-total");

		if (inOrder.getPaymentMethod().requiresValidation())
		{
			try
			{
				Echo echo = new Echo();

				// Customer Info
				Customer customer = inOrder.getCustomer();
				Address address = customer.getBillingAddress();
				echo.billing_first_name = customer.getFirstName();
				echo.billing_last_name = customer.getLastName();
				echo.billing_company_name = customer.getCompany();
				echo.billing_address1 = address.getAddress1();
				echo.billing_address2 = address.getAddress2();
				echo.billing_city = address.getCity();
				echo.billing_state = address.getState();
				echo.billing_zip = address.get5DigitZipCode();
				echo.billing_country = address.getCountry();
				echo.billing_phone = customer.getPhone1();
				echo.billing_email = customer.getEmail();
				//echo.billing_ip_address = getRequest().getRemoteAddr();

				// Submit Request
				CreditPaymentMethod creditCard = (CreditPaymentMethod) inOrder.getPaymentMethod();
				echo.cc_number = creditCard.getCardNumber();
				echo.ccexp_month = creditCard.getExpirationMonthString();
				echo.ccexp_year = creditCard.getExpirationYearString();
				if (isAddRandomAmountToTotal())
				{
					Money randomTotalAmount = addRandomAmountToTotal(inOrder.getTotalPrice());
					echo.grand_total = randomTotalAmount.toString();
				}
				else
				{
					echo.grand_total = inOrder.getTotalPrice().toString();
				}

				// Force grand total if force value has been set
				if (force_grand_total != null && !"".equals(force_grand_total.trim()))
				{
					echo.grand_total = force_grand_total;
				}

				if (use_test_id)
				{
					echo.merchant_echo_id = test_id;
					echo.merchant_pin = test_pin;
				}
				else
				{
					echo.merchant_echo_id = id;
					echo.merchant_pin = pin;
				}
				echo.merchant_email = inStore.getFromAddress();
				echo.order_type = "S";
				echo.transaction_type = "AD";
				echo.merchant_trace_nbr = inOrder.getId();
				echo.counter = "5";
				echo.debug = "F";
				echo.submit();

				// Get Responses
				String avs_result = echo.avs_result();
				String echoAuthCode = echo.echoAuthCode();
				String echoDeclineCode = echo.echoDeclineCode();
				String echoOrderNumber = echo.echoOrderNumber();
				String echoReference = echo.echoReference();
				String echoResponse = echo.echoResponse();
				String echoStatus = echo.echoStatus();
				boolean echoSuccess = echo.echoSuccess();
				String echoType1 = echo.echoType1();
				String echoType2 = echo.echoType2();
				String echoType3 = echo.echoType3();

				OrderState orderState = inOrder.getOrderState();

				if (echoSuccess
					&& (avs_result.toUpperCase().equals("X") || avs_result.toUpperCase()
						.equals("Y")))
				{
					echo.transaction_type = "AV";
					echo.submit();
					avs_result = echo.avs_result();
					echoAuthCode = echo.echoAuthCode();
					echoDeclineCode = echo.echoDeclineCode();
					echoOrderNumber = echo.echoOrderNumber();
					echoReference = echo.echoReference();
					echoResponse = echo.echoResponse();
					echoStatus = echo.echoStatus();
					echoSuccess = echo.echoSuccess();
					echoType1 = echo.echoType1();
					echoType2 = echo.echoType2();
					echoType3 = echo.echoType3();

					if (echoSuccess)
					{
						// transaction approved
						orderState.setOk(true);
						orderState.setDescription("Your transaction has been approved.");
					}
					else
					{

						// transaction declined
						log.info("DECLINED: " + echo.echoType2() + "Response Reason Code: "
							+ echoDeclineCode + "/" + echo.echoType1());
						orderState.setDescription(echo.echoType2() + "<br>"
							+ "Please make sure that the credit card "
							+ "number and billing information is correct.<br><br>");
						orderState.setOk(false);
					}
				}
				else
				{
					// Address verification failed
					log.info(echoType2);
					orderState.setDescription(echoType2 + "<br>" + "Code:  " + echoType1);
					orderState.setOk(false);
				}
			}
			catch (Exception e)
			{
				OrderState orderState = new OrderState();
				orderState.setDescription("An error occurred while processing your transaction.");
				orderState.setOk(false);
				inOrder.setOrderState(orderState);
				e.printStackTrace();
				throw new StoreException(e);
			}
		}
		else
		{
			OrderState orderState = inOrder.getOrderState();
			orderState.setOk(true);
			orderState.setDescription("PO Accepted");
		}
	}

	public Money addRandomAmountToTotal(Money inTotal)
	{
		Random generator = new Random(System.currentTimeMillis());
		String randomStr = "";
		for (int i = 0; i < 4; i++)
		{
			int random = generator.nextInt();
			String randomStr1 = (new Integer(random)).toString();
			String lastDigit = randomStr1.substring(randomStr1.length() - 1, randomStr1.length());
			randomStr = randomStr + lastDigit;
			if (i == 1)
			{
				randomStr = randomStr + ".";
			}
		}
		return inTotal.add(new Money(randomStr));
	}

	/**
	 * @return Returns the addRandomAmountToTotal.
	 */
	public boolean isAddRandomAmountToTotal()
	{
		return fieldAddRandomAmountToTotal;
	}

	/**
	 * @param addRandomAmountToTotal The addRandomAmountToTotal to set.
	 */
	public void setAddRandomAmountToTotal(boolean addRandomAmountToTotal)
	{
		fieldAddRandomAmountToTotal = addRandomAmountToTotal;
	}

	@Override
	public void refundOrder(WebPageRequest inContext, Store inStore,
			Refund inRefund) throws StoreException {
		// TODO Auto-generated method stub
		
	}

}