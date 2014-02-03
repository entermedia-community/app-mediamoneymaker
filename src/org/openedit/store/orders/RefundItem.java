package org.openedit.store.orders;

import org.openedit.money.Money;

public class RefundItem
{
	protected String fieldId;
	protected int fieldQuantity;
	protected Money fieldUnitPrice;
	protected Money fieldTotalPrice;
	protected boolean fieldShipping;
	
	public String getId()
	{
		return fieldId;
	}
	public void setId(String inId)
	{
		fieldId = inId;
	}
	public int getQuantity()
	{
		return fieldQuantity;
	}
	public void setQuantity(int inQuantity)
	{
		fieldQuantity = inQuantity;
	}
	public Money getUnitPrice()
	{
		return fieldUnitPrice;
	}
	public void setUnitPrice(Money inUnitPrice)
	{
		fieldUnitPrice = inUnitPrice;
	}
	public Money getTotalPrice()
	{
		return fieldTotalPrice;
	}
	public void setTotalPrice(Money inTotalPrice)
	{
		fieldTotalPrice = inTotalPrice;
	}
	public boolean isShipping()
	{
		return fieldShipping;
	}
	public void setShipping(boolean inShipping)
	{
		fieldShipping = inShipping;
	}
}
