/*
 * Created on Aug 25, 2004
 */
package org.openedit.store;

import org.openedit.money.Money;


/**
 * @author Matthew Avery, mavery@einnovation.com
 */
public class PriceTier
{
	protected int fieldThresholdQuantity;
	protected Price fieldPrice;
	
	public int getThresholdQuantity()
	{
		return fieldThresholdQuantity;
	}
	public void setThresholdQuantity( int thresholdQuantity )
	{
		fieldThresholdQuantity = thresholdQuantity;
	}
	public Price getPrice()
	{
		return fieldPrice;
	}
	public void setPrice( Price price )
	{
		fieldPrice = price;
	}
	
	public Money getValue()
	{
		return getPrice().getValue();
	}

	public boolean equals(Object inObject)
	{
		if ( inObject instanceof PriceTier )
		{
			PriceTier pt = (PriceTier)inObject;
			if ( getThresholdQuantity() != pt.getThresholdQuantity() )
			{
				return false;
			}
			if ( getPrice() != null )
			{
				if ( !getPrice().equals( pt.getPrice() ) )
				{
					return false;
				}
			}
			else if ( pt.getPrice() != null )
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
		int code = getThresholdQuantity() << 16;
		if ( getPrice() != null )
		{
			code ^= getPrice().hashCode();
		}
		return code;
	}
	
	public PriceTier copy()
	{
		PriceTier priceTier = new PriceTier();
		priceTier.setThresholdQuantity(getThresholdQuantity());
		priceTier.setPrice(getPrice().copy());
		return priceTier;
	}
}
