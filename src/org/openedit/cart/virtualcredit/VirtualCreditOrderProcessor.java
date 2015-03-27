package org.openedit.cart.virtualcredit;

import java.util.Date;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermedia.locks.Lock;
import org.openedit.Data;
import org.openedit.data.Searcher;
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
		return inOrder.getSubTotal();// NOT SURE ABOUT THIS: maybe it should be total???
	}
	
	/**
	 * 
	 * @param inRefund
	 * @return
	 */
	protected Money getIncrementValue(Refund inRefund){
		return inRefund.getSubTotal();// again, not sure which one to use
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
		Store store = (Store) inContext.getPageValue("store");
		Customer customer = inOrder.getCustomer();
		if (customer == null || customer.getUser()==null){
			return; //no op
		}
		MediaArchive archive = (MediaArchive) inContext.getPageValue("mediaarchive");
		if (archive == null){
			return;//no op
		}
		String userid = customer.getUser().getId();
		Searcher searcher = archive.getSearcher("userprofile");
		Data userprofile = (Data) searcher.searchById(userid);
		Money balance = getMoney(userprofile.get(PROCESSOR_ID));
		if (balance.isZero() || balance.isNegative()){
			//not enough
			OrderState orderState = inStore.getOrderState(Order.REJECTED);
			orderState.setDescription("Insufficient Funds");
			orderState.setOk(false);
			inOrder.setOrderState(orderState);
		} else {
			Money value = getDecrementValue(inOrder);
			Money temp = balance.subtract(value);
			if (temp.isNegative()){
				OrderState orderState = inStore.getOrderState(Order.REJECTED);
				orderState.setDescription("Insufficient Funds");
				orderState.setOk(false);
				inOrder.setOrderState(orderState);
			} else {
				//get lock on user path
				Lock lock = archive.getLockManager().lockIfPossible(store.getCatalogId(), "userprofile/"+PROCESSOR_ID+"/" + userid, inContext.getUserName());
				if (lock!=null){
					try{
						Money newbalance = balance.subtract(value);
						log.info("decrementing virtual credit for "+userid+": original="+balance+", decrement value="+value+", new balance="+newbalance);
						if (newbalance.isNegative()){//this case is possible so check it
							OrderState orderState = inStore.getOrderState(Order.REJECTED);
							orderState.setDescription("Insufficient Funds");
							orderState.setOk(false);
							inOrder.setOrderState(orderState);
						} else {
							userprofile.setProperty(PROCESSOR_ID,newbalance.toShortString().replace(",",""));
							searcher.saveData(userprofile, inContext.getUser());
							OrderState orderState = inStore.getOrderState(Order.ACCEPTED);
							inOrder.setProperty("transactionid", UUID.randomUUID().toString());
							orderState.setDescription("Your account balance has been modified.");
							orderState.setOk(true);
							inOrder.setOrderState(orderState);
						}
					} catch (Exception e){
						throw new StoreException(e.getMessage());
					} finally{
						archive.releaseLock(lock);
					}
				} else {
					//could not get lock
					OrderState orderState = inStore.getOrderState(Order.REJECTED);
					orderState.setDescription("Unable to decrement credit");
					orderState.setOk(false);
					inOrder.setOrderState(orderState);
				}
			}
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
		Store store = (Store) inContext.getPageValue("store");
		Customer customer = inOrder.getCustomer();
		if (customer == null || customer.getUser()==null){
			return; //no op
		}
		MediaArchive archive = (MediaArchive) inContext.getPageValue("mediaarchive");
		if (archive == null){
			return;//no op
		}
		Money value = getIncrementValue(inRefund);
		if (value.isNegative()){
			inRefund.setSuccess(false);
			inRefund.setMessage("Unable to increment credit");
			inRefund.setDate(new Date());
			return;
		}
		String userid = customer.getUser().getId();
		Searcher searcher = store.getSearcherManager().getSearcher(store.getCatalogId(), "userprofile");
		Data userprofile = (Data) searcher.searchById(userid);
		Money balance = getMoney(userprofile.get(PROCESSOR_ID));
		Lock lock = archive.getLockManager().lockIfPossible(store.getCatalogId(), "userprofile/"+PROCESSOR_ID+"/" + userid, inContext.getUserName());
		if (lock!=null){
			try{
				Money newbalance = balance.add(value);
				log.info("incrementing virtual credit for "+userid+": original="+balance+", increment value="+value+", new balance="+newbalance);
				userprofile.setProperty(PROCESSOR_ID,newbalance.toShortString().replace(",",""));
				searcher.saveData(userprofile, inContext.getUser());
				inRefund.setSuccess(true);
				inRefund.setProperty("refundedby",inContext.getUserName());
				inRefund.setTransactionId(UUID.randomUUID().toString());
				inRefund.setDate(new Date());
			} catch (Exception e){
				throw new StoreException(e.getMessage());
			} finally{
				archive.releaseLock(lock);
			}
		} else {
			//could not get lock
			inRefund.setSuccess(false);
			inRefund.setMessage("Unable to increment credit");
			inRefund.setDate(new Date());
		}
	}
}
