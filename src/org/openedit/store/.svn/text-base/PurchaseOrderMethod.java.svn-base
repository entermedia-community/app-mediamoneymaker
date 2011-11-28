/*
 * Created on Nov 7, 2004
 */
package org.openedit.store;

/**
 * @author cburkey
 *
 */
public class PurchaseOrderMethod extends CreditPaymentMethod
{

	protected String fieldPoNumber;
	

	public String getPoNumber()
	{
		return fieldPoNumber;
	}
	public void setPoNumber(String inOrderNumber)
	{
		fieldPoNumber = inOrderNumber;
	}
	/* (non-javadoc)
	 * @see com.openedit.store.PaymentMethod#requiresValidation()
	 */
	public boolean requiresValidation()
	{
		//	If a credit card number has been entered, then validate it.
		//	Otherwise, allow transaction to be approved solely based on PO #.
		//	This may need to be revisited at some point.
		if ( getCardNumber() != null && getCardNumber().trim().length() > 0 )
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	public String getType()
	{
		if ( requiresValidation() )
		{
			return "creditcardwithpo";
		}
		return "po";
	}
}
