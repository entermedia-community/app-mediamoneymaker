/*
 * Created on Sep 23, 2004
 */
package org.openedit.store.adjustments;

import org.openedit.money.Fraction;
import org.openedit.money.Money;
import org.openedit.store.Cart;
import org.openedit.store.CartItem;

/**
 * @author Matthew Avery, mavery@einnovation.com
 */
public class SaleAdjustment implements Adjustment {
	protected Fraction fieldPercentage;

	public SaleAdjustment() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Money adjust(Cart inCart, CartItem inItem) {
		return inItem.getYourPrice().multiply(
				Fraction.ONE.subtract(getPercentage()));
	}

	public void setPercentDiscount(double inAdjustmentPercentage) {
		fieldPercentage = new Fraction(inAdjustmentPercentage / 100);
	}

	public Fraction getPercentage() {
		return fieldPercentage;
	}

	public String toString() {
		Double percent = getPercentage().doubleValue() * 100;
		String val = Integer.valueOf(percent.intValue()).toString();
		return val;

	}
}
