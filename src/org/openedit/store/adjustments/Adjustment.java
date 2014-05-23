/*
 * Created on Aug 25, 2004
 */
package org.openedit.store.adjustments;

import org.openedit.money.Money;
import org.openedit.store.Cart;
import org.openedit.store.CartItem;


/**
 * @author Matthew Avery, mavery@einnovation.com
 */
public interface Adjustment
{
	public abstract String getProductId();
	public abstract Money adjust( Cart inCart, CartItem inItem );
	public abstract Money adjust( CartItem inItem );
}