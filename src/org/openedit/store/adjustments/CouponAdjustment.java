package org.openedit.store.adjustments;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.openedit.money.Money;
import org.openedit.store.Cart;
import org.openedit.store.CartItem;

public class CouponAdjustment implements Adjustment {

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
	
	public boolean hasMultipleProducts()
	{
		return (getProductId()!=null && getProductId().contains("|"));
	}
	
	public List<String> getProductIDs()
	{
		List<String> list = new ArrayList<String>();
		String productid = getProductId();
		if (productid!=null && !productid.isEmpty()){
			if (hasMultipleProducts()){
				StringTokenizer tok = new StringTokenizer(productid,"|");
				while(tok.hasMoreTokens())
				{
					list.add(tok.nextToken().trim());
				}
			} else {
				list.add(productid);
			}
		}
		return list;
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
			Money discount = getDiscount();
			Money price = inItem.getYourPrice();
			Money adjusted = price;
			if (getProductId()!=null && inItem.getProduct()!=null)
			{
				if (hasMultipleProducts())
				{
					Iterator<String> itr = getProductIDs().iterator();
					while(itr.hasNext()){
						String product = itr.next();
						if (inItem.getProduct().getId().equals(product))
						{
							if (discount.isNegative())
							{
								adjusted = price.subtract(discount);
							}
							else 
							{
								adjusted = price.add(discount);
							}
							break;
						}
					}
				}
				else
				{
					if (inItem.getProduct().getId().equals(getProductId()))
					{
						if (discount.isNegative())
						{
							adjusted = price.subtract(discount);
						}
						else 
						{
							adjusted = price.add(discount);
						}
					}
				}
			}
			return adjusted;
		}
		return null;
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
