package org.openedit.store.adjustments;

import org.openedit.money.Money;
import org.openedit.store.CartItem;
import org.openedit.store.Coupon;

public class SecurityGroupAdjustment extends FixedPriceAdjustment {
	
	@Override
	public Money adjust(CartItem inItem) {
		if (!Coupon.isCoupon(inItem))
		{
			if (getProductId()!=null && inItem.getProduct()!=null)
			{
				if ( inItem.getProduct().getId().equals(getProductId()) )
				{
					Money discount = getDiscount();
					Money price = inItem.getYourPrice();
					return price.subtract(discount);
				}
			}
		}
		return null;
	}
}
