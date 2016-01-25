package org.openedit.store;

import org.openedit.WebPageRequest;
import org.openedit.money.Money;


public interface MoneyCalculator {	
	public Money getDiscount(WebPageRequest inReq, CartItem inCartItem);

}
