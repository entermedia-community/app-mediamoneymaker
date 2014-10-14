package org.openedit.store.adjustments;

import org.openedit.money.Money;
import org.openedit.store.Cart;
import org.openedit.store.CartItem;
import org.openedit.store.Coupon;

public class DiscountAdjustment extends MultipleProductsAdjustment implements Adjustment{
	
	protected String fieldProductId;
	protected String fieldInventoryItemId;
	protected Money fieldDiscount;
	
	public void setProductId(String fieldProductId)
	{
		this.fieldProductId = fieldProductId;
	}
	
	public String getProductId()
	{
		return fieldProductId;
	}
	
	public void setInventoryItemId(String fieldInventoryItemId)
	{
		this.fieldInventoryItemId = fieldInventoryItemId;
	}
	
	public String getInventoryItemId()
	{
		return fieldInventoryItemId;
	}

	public Money getDiscount() {
		return fieldDiscount;
	}
	
	public void setDiscount(double inDiscount){
		setDiscount(new Money(inDiscount));
	}

	public void setDiscount(Money inDiscount) {
		fieldDiscount = inDiscount;
	}

	@Override
	public Money adjust(Cart inCart, CartItem inItem) {
		return adjust(inItem);
	}
	
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
					if (discount.isNegative())
					{
						return price.add(discount);
					}
					else 
					{
						return price.subtract(discount);
					}
				}
			}
		}
		return null;
	}
	
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(getDiscount()).append(" discount");
		if (getProductId()!=null)
		{
			buf.append(" on "+getProductId()+" products");
		}
		return buf.toString();
	}

}
