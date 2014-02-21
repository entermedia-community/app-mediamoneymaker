/*
 * Created on Mar 4, 2004
 *
 */
package org.openedit.store;

/**
 * @author dbrown
 *
 */
public class PrepaidPaymentMethod extends PaymentMethod
{

	protected String fieldPrepaidCode;
	
	
	public String getPrepaidCode()
	{
		return fieldPrepaidCode;
	}

	public void setPrepaidCode(String inPrepaidCode)
	{
		fieldPrepaidCode = inPrepaidCode;
	}

	public PrepaidPaymentMethod()
	{
	}

	public boolean requiresValidation(){
		return true;
	}
	
	public  String getType(){
		return "prepaidcode";
	}
	
	
}
