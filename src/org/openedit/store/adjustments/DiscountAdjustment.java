package org.openedit.store.adjustments;

import org.openedit.money.Money;
import org.openedit.store.Cart;
import org.openedit.store.CartItem;

public class DiscountAdjustment implements Adjustment{
	
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
		if (inItem.getProduct().isCoupon())
		{
			return null;
		}
		Money discount = getDiscount();
		Money price = inItem.getYourPrice();
		Money adjusted = price;
		if (getProductId()!=null && inItem.getProduct()!=null && inItem.getProduct().getId().equals(getProductId()))
		{
			if (discount.isNegative())
			{
				adjusted = price.add(discount);
			}
			else 
			{
				adjusted = price.subtract(discount);
			}
		}
		return adjusted;
	}
	
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(getDiscount()).append(" discount");
//		if (getProductId()!=null)
//		{
//			buf.append(" on "+getProductId()+" products");
//		}
		return buf.toString();
	}

}
