/*
 * Created on Oct 11, 2004
 */
package org.openedit.store;

import org.openedit.money.CurrencyFormat;
import org.openedit.money.Money;

/**
 * @author Matthew Avery, mavery@einnovation.com
 */
public class Price
{
	protected Money fieldWholesalePrice;
	protected Money fieldRetailPrice;
	protected Money fieldSalePrice;
	protected String fieldRegion;
	public static final String REGION_EU = "eu";
	public static final String REGION_AMERICA = "a";

	public Price()
	{
	}

	public Price(Money inRetailPrice)
	{
		setRetailPrice(inRetailPrice);
	}

	public Money getRetailPrice()
	{
		return fieldRetailPrice;
	}
	public void setRetailPrice( Money retailPrice )
	{
		fieldRetailPrice = retailPrice;
		adjustRegion(fieldRetailPrice);
	}
	public Money getWholesalePrice()
	{
		return fieldWholesalePrice;
	}
	public void setWholesalePrice( Money wholesalePrice )
	{
		fieldWholesalePrice = adjustRegion(wholesalePrice);
	}
	public Money getSalePrice()
	{
		return fieldSalePrice;
	}
	public void setSalePrice( Money salePrice )
	{
		fieldSalePrice = salePrice;
		adjustRegion(fieldSalePrice);
	}
	
	public boolean isOnSale()
	{
		return getSalePrice() != null && !getSalePrice().equals(getRetailPrice());
		
	}
	public Money getValue()
	{
		Money value = null;
		if ( isOnSale() )
		{
			value = getSalePrice();
		}
		else if ( getRetailPrice() != null )
		{
			value =  getRetailPrice();
		}
		else
		{
			value = Money.ZERO;
		}
		return adjustRegion(value);
		
	}

	public boolean equals(Object inObject)
	{
		if ( inObject instanceof Price )
		{
			Price price = (Price)inObject;
			if ( getRetailPrice() != null )
			{
				if ( !getRetailPrice().equals( price.getRetailPrice() ) )
				{
					return false;
				}
			}
			else if ( price.getRetailPrice() != null )
			{
				return false;
			}
			if ( getSalePrice() != null )
			{
				if ( !getSalePrice().equals( price.getSalePrice() ) )
				{
					return false;
				}
			}
			else if ( price.getSalePrice() != null )
			{
				return false;
			}
			return true;
		}
		else
		{
			return false;
		}
	}

	public int hashCode()
	{
		int code = 0;
		if ( getRetailPrice() != null )
		{
			code ^= getRetailPrice().hashCode();
		}
		if ( getSalePrice() != null )
		{
			code ^= (getSalePrice().hashCode() << 1);
		}
		return code;
	}
	public String getRegion()
	{
		return fieldRegion;
	}
	/**
	 * This is not normally used. A user can customize a single price to use an alternative format
	 * @param inRegion
	 */
	public void setRegion(String inRegion)
	{
		fieldRegion = inRegion;
	}
	public Money adjustRegion(Money inTotalPrice)
	{
		if ( fieldRegion != null && inTotalPrice != null )
		{
			inTotalPrice.setFormat(CurrencyFormat.findFormat( fieldRegion) );
		}
		return inTotalPrice;
	}

	public Price copy() {
		Price price = new Price();
		price.setRetailPrice(getRetailPrice().copy());
		price.setSalePrice(getSalePrice().copy());
		if (getWholesalePrice()!=null)
		{
			price.setWholesalePrice(getWholesalePrice().copy());//avoid null pointer exceptions
		}
		price.setRegion(getRegion());
		return price;
	}
	
	public String toString(){
		StringBuilder buf = new StringBuilder();
		buf.append("Retail=").append(getRetailPrice()).append(", Sale=").append(getSalePrice());
		if (getWholesalePrice()!=null)
		{
			buf.append(", Wholesale=").append(getWholesalePrice());
		}
		buf.append(" (Region=").append(getRegion()).append(")");
		return buf.toString();
	}

}