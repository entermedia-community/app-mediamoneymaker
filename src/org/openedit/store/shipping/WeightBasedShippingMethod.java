/*
 * Created on May 26, 2004
 */
package org.openedit.store.shipping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Element;
import org.openedit.money.Money;
import org.openedit.store.Cart;
import org.openedit.store.CartItem;


/**
 * @author cburkey
 *
 */
public class WeightBasedShippingMethod  extends BaseShippingMethod
{
	public Money getCost(Cart inCart) 
	{
		Money totalPrice = getHandlingCharge(inCart);
		totalPrice = totalPrice.add(getCost());
			
		if ( getPercentageCost() != 0.0 )
		{
			double weight = getWeight(inCart.getItems());
			if ( weight != 0.0)
			{
				double shippingAsPercent = weight * getPercentageCost();
				totalPrice = totalPrice.add(new Money(shippingAsPercent));
			}
		}
		totalPrice = totalPrice.round();
		return totalPrice;
	}
	public boolean applies(Cart inCart)
	{
		List remaining = new ArrayList( inCart.getItems() );
		
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
				remaining.remove(cartI);
			}
		}
		//check the weight
		double weight = getWeight(remaining);
		if ( getLowerThreshold() == null ||	Money.ZERO.equals(getLowerThreshold()) ||
			getLowerThreshold().doubleValue() <= weight)
		{
			if (getUpperThreshold() == null ||
				getUpperThreshold().doubleValue() >= weight)
			{

				return true;
			}
		}
		return false;
	}
	protected double getWeight(List inRemaining)
	{
		double total = 0.0;
		for (Iterator iter = inRemaining.iterator(); iter.hasNext();)
		{
			CartItem item = (CartItem) iter.next();
			if( item.getProduct().getShippingMethodId() == null)
			{
				double w = item.getWeight() * item.getQuantity();
				total = total + w;
			}
		}
		return total;
	}
	@Override
	public void configure(Element inElement) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public Collection getHints(Cart inCart) {
		// TODO Auto-generated method stub
		return null;
	}
}
