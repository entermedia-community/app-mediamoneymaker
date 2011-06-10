/*
 * Created on Feb 3, 2006
 */
package org.openedit.store.shipping;

import org.dom4j.Element;
import org.openedit.money.Money;
import org.openedit.store.Cart;

public class FixedCostShippingMethod extends BaseShippingMethod
{
	public Money getCost(Cart inCart)
	{
		return getCost();
	}

	public boolean applies(Cart inCart)
	{
		return false;
	}


	public void configure(Element inElement) {
		// TODO Auto-generated method stub
		
	}

}
