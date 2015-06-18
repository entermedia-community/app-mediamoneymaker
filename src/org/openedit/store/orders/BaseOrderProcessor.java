/*
 * Created on Jul 27, 2006
 */
package org.openedit.store.orders;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openedit.money.Fraction;
import org.openedit.money.Money;
import org.openedit.store.BaseArchive;
import org.openedit.store.CartItem;
import org.openedit.store.Product;
import org.openedit.store.Store;
import org.openedit.store.StoreException;

import com.openedit.WebPageRequest;

public abstract class BaseOrderProcessor extends BaseArchive implements OrderProcessor
{

	public void archiveOrderData(Store inStore) throws StoreException
	{
	}

	public void captureOrder(WebPageRequest inContext, Store inStore, Order inOrder) throws StoreException
	{
	}

	public void changeOrderStatus(OrderState inStatus, Store inStore, Order inOrder) throws StoreException
	{
	}

	public void exportNewOrder(WebPageRequest inContext, Store inStore, Order inOrder) throws StoreException
	{
	}

	public Map getOrderStates(Store inStore)
	{
		return null;
	}

	public List listAllOrderIds(Store inStore) throws StoreException
	{
		return null;
	}
	public SubmittedOrder loadSubmittedOrder(Store inStore, String inUserName,  String inId) throws StoreException
	{
		// TODO Auto-generated method stub
		return null;
	}
	public void saveOrder(Store inStore, Order inOrder) throws StoreException
	{
		
	}
	
	
	protected Money calculateFee(Store inStore, Order inOrder){
		Money totalFee = new Money("0");
		@SuppressWarnings("unchecked")
		Iterator<CartItem> itr = inOrder.getItems().iterator();
		while(itr.hasNext()){
			CartItem item = itr.next();
			Product product = item.getProduct();
			if (product.isCoupon()){
				continue;
			}
			String fee = product.get("partnershipfee");
			String type = product.get("partnershipfeetype");
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
		String fee = inOrder.get("fee");//transaction fee
		if (fee!=null && fee.isEmpty()==false){
			Money transfee = new Money(fee);
			if (transfee.isZero() == false){
				transfee = transfee.multiply(new Fraction(0.5d));//divide by 2
				totalFee = totalFee.subtract(transfee);
			}
		}
		inOrder.setProperty("profitshare", Double.toString(totalFee.doubleValue()));
		return totalFee;
	}

	
	
	
}
