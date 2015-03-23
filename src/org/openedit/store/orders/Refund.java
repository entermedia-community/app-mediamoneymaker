package org.openedit.store.orders;

import java.util.ArrayList;
import java.util.Date;

import org.openedit.data.BaseData;
import org.openedit.money.Money;

public class Refund extends BaseData{
	
	protected Money fieldTotalAmount;//this is the total for the refund
	protected Money fieldTaxAmount;//this is calculated based on the taxes associated with the order
	protected Money fieldSubTotal;//this is calculated based on the total - tax amount
	
	protected boolean fieldSuccess;
	protected String fieldMessage;
	protected String fieldTransactionId;
	protected String fieldAuthorizationCode;
	protected Date fieldDate;
	
	protected ArrayList<RefundItem> fieldItems;
	
	public boolean isSuccess() {
		return fieldSuccess;
	}
	public void setSuccess(boolean inSuccess) {
		fieldSuccess = inSuccess;
	}
	public String getMessage()
	{
		return fieldMessage;
	}
	public void setMessage(String inMessage)
	{
		fieldMessage = inMessage;
	}
	public Money getTotalAmount() {
		return fieldTotalAmount;
	}
	public void setTotalAmount(Money inTotalAmount) {
		fieldTotalAmount = inTotalAmount;
	}
	public Money getTaxAmount()
	{
		return fieldTaxAmount;
	}
	public void setTaxAmount(Money inTaxAmount)
	{
		fieldTaxAmount = inTaxAmount;
	}
	public Money getSubTotal()
	{
		return fieldSubTotal;
	}
	public void setSubTotal(Money inSubTotal)
	{
		fieldSubTotal = inSubTotal;
	}
	public String getTransactionId()
	{
		return fieldTransactionId;
	}
	public void setTransactionId(String inTransactionId)
	{
		fieldTransactionId = inTransactionId;
	}
	public String getAuthorizationCode()
	{
		return fieldAuthorizationCode;
	}
	public void setAuthorizationCode(String inAuthorizationCode)
	{
		fieldAuthorizationCode = inAuthorizationCode;
	}
	public Date getDate()
	{
		return fieldDate;
	}
	public void setDate(Date inDate)
	{
		fieldDate = inDate;
	}
	
	public ArrayList<RefundItem> getItems()
	{
		if (fieldItems == null)
		{
			fieldItems = new ArrayList<RefundItem>();
		}
		return fieldItems;
	}
	public void setItems(ArrayList<RefundItem> inItems)
	{
		fieldItems = inItems;
	}
}
