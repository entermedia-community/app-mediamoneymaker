/*
 * Created on May 26, 2004
 */
package org.openedit.store.shipping;

import java.util.Collection;
import java.util.Iterator;

import org.dom4j.Element;
import org.openedit.money.Money;
import org.openedit.store.Cart;
import org.openedit.store.CartItem;


/**
 * @author cburkey
 *
 */
public class PriceBasedShippingMethod  extends BaseShippingMethod
{
	public Money getCost(Cart inCart) 
	{
		Money totalPrice = getHandlingCharge(inCart);
		totalPrice = totalPrice.add(getCost());

		return totalPrice;
	}
	public boolean applies(Cart inCart)
	{
		double subTotal = inCart.getSubTotal().doubleValue();
		
		//If any products have a fixed shipping method
		//Then we must include that in the list
		//and remove it from the pricing totals remaining
		
		for (Iterator iter = inCart.getItemIterator(); iter.hasNext();)
		{
			CartItem cartI = (CartItem) iter.next();
			String method = cartI.getProduct().getShippingMethodId();
			if ( method != null )
			{
				//this has its own method and should be removed from the list
				subTotal = subTotal -  cartI.getYourPrice().doubleValue();
			}
		}

		if ( getLowerThreshold() == null ||
			Money.ZERO.equals(getLowerThreshold()) ||
			getLowerThreshold().doubleValue() <= subTotal)
		{
			if (getUpperThreshold() == null ||
				getUpperThreshold().doubleValue() >= subTotal)
			{

				return true;
			}
		}
		return false;
	}
	
	public void configure(Element inElement) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public Collection getHints(Cart inCart) {
		// TODO Auto-generated method stub
		return null;
	}

}
