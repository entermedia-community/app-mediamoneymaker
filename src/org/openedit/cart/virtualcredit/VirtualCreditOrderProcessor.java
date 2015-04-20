package org.openedit.cart.virtualcredit;

import java.util.Date;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.entermedia.MediaArchive;
import org.openedit.money.Money;
import org.openedit.store.Store;
import org.openedit.store.StoreException;
import org.openedit.store.customer.Customer;
import org.openedit.store.orders.BaseOrderProcessor;
import org.openedit.store.orders.Order;
import org.openedit.store.orders.OrderState;
import org.openedit.store.orders.Refund;

import com.openedit.WebPageRequest;

/**
 * VirtualCreditOrderProcessor used to increment a virtual credit of a user when purchasing products
 * 
 * @author shawn
 *
 */
public class VirtualCreditOrderProcessor extends BaseOrderProcessor {
	
	private static final Log log = LogFactory.getLog(VirtualCreditOrderProcessor.class);
	
	public static final String PROCESSOR_ID = "virtualcredit";
	
	protected VirtualCreditAccount fieldVirtualCreditAccount;
	
	public VirtualCreditAccount getVirtualCreditAccount() {
		return fieldVirtualCreditAccount;
	}

	public void setVirtualCreditAccount(VirtualCreditAccount inVirtualCreditAccount) {
		fieldVirtualCreditAccount = inVirtualCreditAccount;
	}

	/**
	 * 
	 * @param inStore
	 * @param inOrder
	 * @return
	 */
	protected boolean requiresValidation(Store inStore, Order inOrder)
	{
		if (inStore.get("gateway") !=null && inStore.get("gateway").equals(PROCESSOR_ID)){
			if (inOrder.get("gateway") == null || (inOrder.get("gateway").equals(PROCESSOR_ID)) ){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 
	 * @param inValue
	 * @return
	 */
	protected Money getMoney(String inValue){
		Money money = new Money();
		try{
			money = new Money(inValue);// not sure what happens if inValue is not a number
		}catch (Exception e){}
		return money;
	}
	
	/**
	 * 
	 * @param inOrder
	 * @return
	 */
	protected Money getDecrementValue(Order inOrder){
		return inOrder.getTotalPrice();//when we decrement, we get the total
	}
	
	/**
	 * 
	 * @param inRefund
	 * @return
	 */
	protected Money getIncrementValue(Refund inRefund){
		return inRefund.getTotalAmount();//when we increment, we get the refund total
	}
	
	@Override
	public void processNewOrder(WebPageRequest inContext, Store inStore,
			Order inOrder) throws StoreException {
		//basic idea is the customer has to have enough credit to purchase the product (in virtual dollars)
		//if they do, then the purchase goes through and the user credit is decremented
		//otherwise the user credit is not touched and the result returned is an error
		if (!requiresValidation(inStore, inOrder)){
			return;
		}
		if (inOrder.get("gateway") == null){
			inOrder.setProperty("gateway",PROCESSOR_ID);
		}
		Customer customer = inOrder.getCustomer();
		if (customer == null || customer.getUser()==null){
			return; //no op
		}
		MediaArchive archive = (MediaArchive) inContext.getPageValue("mediaarchive");
		if (archive == null){
			return;//no op
		}
		String userid = customer.getUser().getId();
		Money value = getDecrementValue(inOrder);
		if (getVirtualCreditAccount().decrementBalance(archive,userid,value)){
			OrderState orderState = inStore.getOrderState(Order.AUTHORIZED);
			inOrder.setProperty("transactionid",UUID.randomUUID().toString());
			orderState.setDescription("Your account balance has been modified.");
			orderState.setOk(true);
			inOrder.setOrderState(orderState);
		} else {
			OrderState orderState = inStore.getOrderState(Order.REJECTED);
			orderState.setDescription("Insufficient Funds");
			orderState.setOk(false);
			inOrder.setOrderState(orderState);
		}
	}

	@Override
	public void refundOrder(WebPageRequest inContext, Store inStore,
			Order inOrder, Refund inRefund) throws StoreException {
		//a value is added to the virtual credit of the user
		if (!requiresValidation(inStore, inOrder)){
			return;
		}
		if (inOrder.get("gateway") == null){
			inOrder.setProperty("gateway",PROCESSOR_ID);
		}
		Customer customer = inOrder.getCustomer();
		if (customer == null || customer.getUser()==null){
			return; //no op
		}
		MediaArchive archive = (MediaArchive) inContext.getPageValue("mediaarchive");
		if (archive == null){
			return;//no op
		}
		Money value = getIncrementValue(inRefund);
		if (value.isNegative() || value.isZero()){
			inRefund.setSuccess(false);
			inRefund.setMessage("Unable to update credit balance");
			inRefund.setDate(new Date());
			return;
		}
		String userid = customer.getUser().getId();
		if (getVirtualCreditAccount().incrementBalance(archive,userid,value)){
			inRefund.setSuccess(true);
			inRefund.setProperty("refundedby",inContext.getUserName());
			inRefund.setTransactionId(UUID.randomUUID().toString());
			inRefund.setDate(new Date());
		} else {
			inRefund.setSuccess(false);
			inRefund.setMessage("Unable to update credit balance");
			inRefund.setDate(new Date());
		}
	}
}
