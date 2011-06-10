/*
 * Created on Mar 4, 2004
 *
 */
package org.openedit.store;

/**
 * @author dbrown
 *
 */
public abstract class PaymentMethod
{

	public PaymentMethod()
	{
	}

	public abstract boolean requiresValidation();
	
	public abstract String getType();
}
