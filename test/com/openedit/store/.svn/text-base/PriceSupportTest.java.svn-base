/*
 * Created on Sep 23, 2004
 */
package com.openedit.store;

import org.openedit.store.PriceSupport;
import org.openedit.store.StoreTestCase;


/**
 * @author Matthew Avery, mavery@einnovation.com
 */
public class PriceSupportTest extends StoreTestCase
{
	PriceSupport itemPricing = new PriceSupport();
	PriceSupport productPricing = new PriceSupport();
	
	public PriceSupportTest( String name )
	{
		super( name );
	}

	public void setUp()
	{
		itemPricing.addTierPrice(1, createPrice(20) );
		itemPricing.addTierPrice(3, createPrice(19) );
		
		productPricing.addTierPrice( 1, createPrice(15) );
		productPricing.addTierPrice( 3, createPrice(14) );
	}
	public void testGetPrice()
	{
		assertEquals( 20, itemPricing.getYourPriceByQuantity(1).doubleValue(), 0.001 );
		itemPricing.addTierPrice( 1, createPrice(21) );
		assertEquals( 21, itemPricing.getYourPriceByQuantity(1).doubleValue(), 0.001 );
	}

	public void testGetPriceByQuantity()
	{
		assertEquals( 19, itemPricing.getYourPriceByQuantity(3).doubleValue(), 0.001 );
		itemPricing.getTiers().clear();
		assertEquals( 0, itemPricing.getYourPriceByQuantity(3).doubleValue(), 0.001 );
		itemPricing.addTierPrice( 1, createPrice(21) );
		assertEquals( 21, itemPricing.getYourPriceByQuantity(3).doubleValue(), 0.001 );
		itemPricing.addTierPrice( 3, createPrice(20) );
		assertEquals( 20, itemPricing.getYourPriceByQuantity(3).doubleValue(), 0.001 );

	}


}
