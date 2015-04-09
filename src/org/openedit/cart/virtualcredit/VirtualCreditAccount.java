package org.openedit.cart.virtualcredit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermedia.locks.Lock;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.entermedia.MediaArchive;
import org.openedit.money.Money;
import org.openedit.store.StoreException;

public class VirtualCreditAccount {
	
	private static final Log log = LogFactory.getLog(VirtualCreditAccount.class);
	
	/**
	 * gets the virtual credit account data of the user
	 * @param inArchive
	 * @param inUserId
	 * @return
	 */
	protected Data getAccountData(MediaArchive inArchive, String inUserId){
		Searcher searcher = inArchive.getSearcher("virtualcreditaccount");
		Data data = (Data) searcher.searchByField("owner",inUserId);
		return data;
	}
	
	/**
	 * returns the current balance on the user's account
	 * @param inArchive
	 * @param inUserId
	 * @return
	 */
	public Money getBalance(MediaArchive inArchive, String inUserId){
		Money money = new Money();
		Data data = getAccountData(inArchive,inUserId);
		if (data != null){
			money = new Money(data.get("currentbalance"));
		}
		return money;
	}
	
	/**
	 * returns the configured overdraft limit on the user's account
	 * @param inArchive
	 * @param inUserId
	 * @return
	 */
	public Money getOverdraftLimit(MediaArchive inArchive, String inUserId){
		Money money = new Money();
		Data data = getAccountData(inArchive,inUserId);
		if (data != null){
			money = new Money(data.get("overdraftlimit"));
		}
		return money;
	}
	
	/**
	 * returns whether the account can be decremented
	 * @param inArchive
	 * @param inUserId
	 * @param inValue
	 * @return
	 */
	public boolean canDecrement(MediaArchive inArchive, String inUserId, Money inValue){
		Money balance = getBalance(inArchive,inUserId);
		Money overdraft = getOverdraftLimit(inArchive,inUserId);
		Money newvalue = balance.subtract(inValue);
		log.info("current balance: "+balance+", overdraft limit: "+overdraft+", value: "+inValue+", new value: "+newvalue);
		if (overdraft.isZero() && newvalue.isNegative()){
			log.info("cannot decrement: insufficient funds (no overdraft)");
			return false;
		}
		else if (!overdraft.isNegative()){
			overdraft = overdraft.multiply(-1.0d);
		}
		int compare = newvalue.compareTo(overdraft);
		if (compare < 0){
			log.info("cannot decrement: insufficient funds");
			return false;
		}
		return true;
	}
	
	/**
	 * increments the balance on the account
	 * @param inArchive
	 * @param inUserId
	 * @param inValue
	 * @return
	 */
	public boolean incrementBalance(MediaArchive inArchive, String inUserId, Money inValue){
		return updateBalance(inArchive,inUserId,inValue,false);
	}
	
	/**
	 * decrements the balance on the account
	 * @param inArchive
	 * @param inUserId
	 * @param inValue
	 * @return
	 */
	public boolean decrementBalance(MediaArchive inArchive, String inUserId, Money inValue){
		return updateBalance(inArchive,inUserId,inValue,true);
	}
	
	/**
	 * updates the balance on the account
	 * @param inArchive
	 * @param inUserId
	 * @param inValue
	 * @param inSubtract
	 * @return
	 */
	protected boolean updateBalance(MediaArchive inArchive, String inUserId, Money inValue, boolean inSubtract){
		Lock lock = inArchive.getLockManager().lockIfPossible(inArchive.getCatalogId(), "userprofile/virtualcreditaccount/" + inUserId, inUserId);
		if (lock!=null){
			try{
				if (inSubtract){
					if (!canDecrement(inArchive,inUserId,inValue)){
						return false;
					}
				}
				Money balance = getBalance(inArchive, inUserId);
				Money newvalue = inSubtract ? balance.subtract(inValue) : balance.add(inValue);
				Searcher searcher = inArchive.getSearcher("virtualcreditaccount");
				Data data = (Data) searcher.searchByField("owner",inUserId);
				if (data == null){
					return false;
				}
				data.setProperty("currentbalance",newvalue.toShortString().replace(",",""));
				searcher.saveData(data, null);
				return true;
			} catch (Exception e){
				throw new StoreException(e.getMessage());
			} finally{
				inArchive.releaseLock(lock);
			}
		} else {
			return false;
		}
	}
}
