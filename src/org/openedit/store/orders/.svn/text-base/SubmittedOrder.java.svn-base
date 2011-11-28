/*
 * Created on Jan 17, 2005
 */
package org.openedit.store.orders;

import java.text.DateFormat;
import java.util.Locale;

import org.openedit.money.Money;

/**
 * @author dbrown
 *
 */
public class SubmittedOrder extends Order 
{
	protected Money fieldShippingCost;
	protected String fieldDateOrdered; //formated version of the date
	
	public SubmittedOrder()
	{
	}

	public Money getShippingCost()
	{
		return fieldShippingCost;
	}
	public void setShippingCost(Money inShippingCost)
	{
		fieldShippingCost = inShippingCost;
	}
	
	
	public String getDateOrdered()
	{
		return fieldDateOrdered;
	}
	public void setDateOrdered(String inDateOrdered)
	{
		fieldDateOrdered = inDateOrdered;
	}
	
	public String getShortDateOrdered()
	{
		if (fieldDate!=null)
		{
			return DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()).format(fieldDate);
		}
		return "";
	}
	/*
	public List getPurchasedItems()
	{
		if (fieldPurchasedItems == null)
		{
			fieldPurchasedItems = new ArrayList();
		}
		return fieldPurchasedItems;
	}


	public String getOrderStatus()
	{
		if (fieldOrderStatus == null)
		{
			return "accepted";
		}
		return fieldOrderStatus;
	}

	public void setOrderStatus(String inOrderStatus)
	{
		fieldOrderStatus = inOrderStatus;
	}
	
	public List getStatusOptions()
	{
		List list = new ArrayList();
		list.add("accepted");
		list.add("completed");
		list.add("invalid");
		list.add("retracted");
		list.add("waiting for payment");
		return list;
	}
*/


}
