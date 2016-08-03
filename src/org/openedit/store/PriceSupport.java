/*
 * Created on Sep 23, 2004
 */
package org.openedit.store;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openedit.money.Money;


/**
 * @author Matthew Avery, mavery@einnovation.com
 */
public class PriceSupport
{
	protected List fieldTiers;
	
	public Money getYourPriceByQuantity(double inQuantity)
	{
		Money price = Money.ZERO;
		// This code assumes that the tier prices must be entered in ascending price order.
		for (Iterator iter = getTiers().iterator(); iter.hasNext();)
		{
			PriceTier tier = (PriceTier)iter.next();
			if( inQuantity >= tier.getThresholdQuantity())
			{
				price = tier.getValue();
			}
			
		}
		return price;

	}
	
	public Money getWholesalePriceByQuantity(double inQuantity)
	{
		Money wholesale = Money.ZERO;
		// This code assumes that the tier prices must be entered in ascending price order.
		for (Iterator iter = getTiers().iterator(); iter.hasNext();)
		{
			PriceTier tier = (PriceTier)iter.next();
			if( inQuantity >= tier.getThresholdQuantity())
			{
				Price price = tier.getPrice();
				wholesale = price.getWholesalePrice();
			}
			
		}
		return wholesale;

	}
	
	
	public List getTiers()
	{
		if (fieldTiers == null)
		{
			fieldTiers = new ArrayList();
		}
		return fieldTiers;
	}
	
	public void addTierPrice( double inQuantity, Price inPrice )
	{
		
		int finalPlace = -1;
		for (int i = 0; i < getTiers().size(); i++)
		{
			PriceTier tier = (PriceTier) getTiers().get(i);
			if( tier.getThresholdQuantity() == inQuantity )
			{
				finalPlace = i;
				getTiers().remove(tier);
				break;
			}
			if( tier.getThresholdQuantity() > inQuantity )
			{
				finalPlace = i-1;
				break;
			}
			
		}
		//Cound not find a place to put tier so append it to end of list
		if ( finalPlace == -1)
		{
			finalPlace = getTiers().size();
		}
		PriceTier tier = new PriceTier();
		tier.setPrice( inPrice );
		tier.setThresholdQuantity( inQuantity );
		getTiers().add( finalPlace, tier ); 
	}

	/**
	 * @return
	 */
	public Money getRetailPrice()
	{
		// This code assumes that the tier prices must be entered in ascending price order.
		for (Iterator iter = getTiers().iterator(); iter.hasNext();)
		{
			PriceTier tier = (PriceTier)iter.next();
			if( 1 >= tier.getThresholdQuantity())
			{
				Money price = tier.getPrice().getRetailPrice();
				return price;
			}
		}
		return null;
	}
	public Money getSalePrice()
	{
		// This code assumes that the tier prices must be entered in ascending price order.
		for (Iterator iter = getTiers().iterator(); iter.hasNext();)
		{
			PriceTier tier = (PriceTier)iter.next();
			if( 1 >= tier.getThresholdQuantity())
			{
				Money price = tier.getPrice().getSalePrice();

				return price;
			}
		}
		return null;
	}
	
	public Money getWholesalePrice()
	{
		// This code assumes that the tier prices must be entered in ascending price order.
		for (Iterator iter = getTiers().iterator(); iter.hasNext();)
		{
			PriceTier tier = (PriceTier)iter.next();
			if( 1 >= tier.getThresholdQuantity())
			{
				Money price = tier.getPrice().getWholesalePrice();

				return price;
			}
		}
		return null;
	}
	
	public boolean isOnSale()
	{
		if ( getTiers().isEmpty() )
		{
			return false;
		}
		//TODO: Loop over all the items to see if on sale
		PriceTier tier = (PriceTier)getTiers().get(0);
		return tier.getPrice().isOnSale();
	}
	
	public void removeSalePrice()
	{
		// This code assumes that the tier prices must be entered in ascending price order.
		for (Iterator iter = getTiers().iterator(); iter.hasNext();)
		{
			PriceTier tier = (PriceTier)iter.next();
			if( 1 >= tier.getThresholdQuantity())
			{
				tier.getPrice().setSalePrice(null);
				return;
			}
		}
	}


	/**
	 * 
	 */
	public void clear()
	{
		getTiers().clear();
	}

	public PriceSupport copy()
	{
		PriceSupport priceSupport = new PriceSupport();
		List tiers = new ArrayList();
		Iterator it = getTiers().iterator();
		while( it.hasNext() )
		{
			tiers.add(((PriceTier)it.next()).copy());
		}
		priceSupport.fieldTiers = tiers;
		return priceSupport;
	}
}