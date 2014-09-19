/*
 * Created on Sep 23, 2004
 */
package org.openedit.store.adjustments;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.openedit.money.Fraction;
import org.openedit.money.Money;
import org.openedit.store.Cart;
import org.openedit.store.CartItem;

/**
 * @author Matthew Avery, mavery@einnovation.com
 */
public class SaleAdjustment implements Adjustment {
	protected Fraction fieldPercentage;
	protected String fieldProductId;
	protected String fieldInventoryItemId;

	public SaleAdjustment() {
		super();
	}
	
	@Override
	public Money adjust(Cart inCart, CartItem inItem) {
		return adjust(inItem);
	}
	
	@Override
	public Money adjust(CartItem inItem) {
		if (getProductId()!=null)
		{
			if (inItem.getProduct() == null)
			{
				return null;
			}
			if (hasMultipleProducts())
			{
				Iterator<String> itr = getProductIDs().iterator();
				while(itr.hasNext()){
					String product = itr.next();
					if ( inItem.getProduct().getId().equals(product) )
					{
						return inItem.getYourPrice().multiply(Fraction.ONE.subtract(getPercentage()));
					}
				}
				return null;
			}
			if ( inItem.getProduct().getId().equals(getProductId()) )
			{
				return inItem.getYourPrice().multiply(Fraction.ONE.subtract(getPercentage()));
			}
			return null;
		}
		return inItem.getYourPrice().multiply(Fraction.ONE.subtract(getPercentage()));
	}

	public void setPercentDiscount(double inAdjustmentPercentage) {
		fieldPercentage = new Fraction(inAdjustmentPercentage / 100);
	}
	
	public void setPercentage(double inPercentage){
		fieldPercentage = new Fraction(inPercentage);
	}

	public Fraction getPercentage() {
		return fieldPercentage;
	}
	
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

	public String toString() {
		Double percent = getPercentage().doubleValue() * 100;
		StringBuilder buf = new StringBuilder();
		buf.append(percent.intValue()).append("% discount");
//		if (getProductId()!=null)
//		{
//			buf.append(" on "+getProductId()+" products");
//		}
		return buf.toString();

	}
}
