package org.openedit.store;

import org.openedit.money.Money;

import com.openedit.WebPageRequest;


public interface MoneyCalculator {	
	public Money getDiscount(WebPageRequest inReq, CartItem inCartItem);

}
