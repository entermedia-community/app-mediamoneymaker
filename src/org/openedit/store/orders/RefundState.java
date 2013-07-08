package org.openedit.store.orders;

import org.openedit.money.Money;

public class RefundState
{
	public static final String REFUND_NIL = "Nil";
	public static final String REFUND_PENDING = "Pending";
	public static final String REFUND_RETRY = "Retry";
	public static final String REFUND_SUCCESS = "Success";
	public static final String REFUND_REJECTED = "Rejected";
	public static final String REFUND_ERROR = "Error";
	
	protected String fieldRefundStatus = REFUND_NIL;
	protected int fieldQuantity = 0;
	protected int fieldPendingQuantity = 0;
	
	protected Money fieldPendingPrice = new Money("0");
	
	public String getRefundStatus()
	{
		if (fieldRefundStatus == null)
		{
			setRefundStatus(REFUND_NIL);
		}
		return fieldRefundStatus;
	}

	public void setRefundStatus(String inRefundStatus)
	{
		fieldRefundStatus = inRefundStatus;
	}

	public int getQuantity()
	{
		return fieldQuantity;
	}

	public void setQuantity(int inQuantity)
	{
		fieldQuantity = inQuantity;
	}

	public int getPendingQuantity()
	{
		return fieldPendingQuantity;
	}

	public void setPendingQuantity(int inPendingQuantity)
	{
		fieldPendingQuantity = inPendingQuantity;
	}

	public Money getPendingPrice()
	{
		return fieldPendingPrice;
	}

	public void setPendingPrice(Money inPendingPrice)
	{
		fieldPendingPrice = inPendingPrice;
	}

}
