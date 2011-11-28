/*
 * Created on Sep 2, 2004
 */
package com.openedit.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openedit.data.PropertyDetails;
import org.openedit.money.Money;
import org.openedit.store.CartItem;
import org.openedit.store.InventoryItem;
import org.openedit.store.Option;
import org.openedit.store.PriceSupport;
import org.openedit.store.Product;
import org.openedit.store.StoreException;
import org.openedit.store.StoreTestCase;


/**
 * @author cburkey
 *
 */
public class ProductTest extends StoreTestCase
{

	/**
	 * Constructor for ProductTest.
	 * @param arg0
	 */
	public ProductTest(String arg0)
	{
		super(arg0);
	}

	public void testGetPriceByQuantity()
	{

		Product brandNewCar = new Product("brand new car");

		brandNewCar.addTierPrice(1, createPrice(25000));
		brandNewCar.addTierPrice(2, createPrice(23000));
		brandNewCar.addTierPrice(4, createPrice(22000));
		assertEquals( 3, brandNewCar.getPriceSupport().getTiers().size() );
		Money price = brandNewCar.getPriceSupport().getYourPriceByQuantity(2);
		assertEquals(23000, price.doubleValue(), 0.001);
		brandNewCar.addTierPrice( 2, createPrice(24000) );
		assertEquals( 3, brandNewCar.getPriceSupport().getTiers().size() );
		price = brandNewCar.getPriceSupport().getYourPriceByQuantity(2);
		assertEquals(24000, price.doubleValue(), 0.001);
		price = brandNewCar.getPriceSupport().getYourPriceByQuantity(3);
		assertNotNull( price );

		assertEquals( 3, brandNewCar.getPriceSupport().getTiers().size() );
		price = brandNewCar.getPriceSupport().getYourPriceByQuantity(1);
		assertEquals(25000, price.doubleValue(), 0.001);
		price = brandNewCar.getPriceSupport().getYourPriceByQuantity(4);
		assertEquals(22000, price.doubleValue(), 0.001);
	}
	
	public void testCreateCartItem() throws StoreException
	{
		Product brandNewCar = new Product("brand new car");
		brandNewCar.setId("322123ER");
				
		brandNewCar.addTierPrice(1, createPrice(25000));
		brandNewCar.addTierPrice(2, createPrice(23000));
		brandNewCar.addTierPrice(4, createPrice(22000));
		
		
		InventoryItem item = new InventoryItem();
		item.setQuantityInStock(10000);
		item.setColor( "Blue" );
		item.setSize( "Van" );
		item.setSku( "300" );
		item.setSku("junk");
		brandNewCar.addInventoryItem(item);
		
		InventoryItem blueCar = new InventoryItem();
		blueCar.setColor( "Blue" );
		blueCar.setSize( "Sedan" );
		blueCar.setSku( "300" );
		blueCar.setQuantityInStock(100);
		brandNewCar.addInventoryItem( blueCar );

		//try to find the car one
		Collection options = new ArrayList();
		options.add( new Option("size","Sedan"));
		options.add( new Option("color","Blue"));
		InventoryItem found = brandNewCar.getInventoryItemByOptions(options);
		CartItem cartItem = new CartItem();//brandNewCar.createCartItem("Sedan", "Blue", 1, null);
		cartItem.setInventoryItem(found);
		cartItem.setQuantity(1);
		assertEquals( "Blue", found.getColor().getValue() );
		assertEquals( "Sedan", found.getSize().getValue() );
		assertEquals( "300", found.getSku() );
		assertEquals( 1, cartItem.getQuantity() );
		
		
		options = new ArrayList();
		options.add( new Option("size","Sedan"));
		found = brandNewCar.getInventoryItemByOptions(options);
		assertNotNull(found);
		cartItem = new CartItem();
		cartItem.setInventoryItem(found);
		cartItem.setQuantity(1);
		assertEquals( "Blue", cartItem.getColor().getValue() );
		assertEquals( "Sedan", cartItem.getSize().getValue() );
		assertEquals( "300", cartItem.getSku() );
		assertEquals( 1, cartItem.getQuantity() );
	}
	
	public void testIsInStockWithZeroQuantityItems()
	{
		Product brandNewCar = createOutOfStockProduct();
		assertFalse( "Car should not be in stock", brandNewCar.isInStock() );
		assertEquals( "Unit price", 24000, brandNewCar.getYourPrice().doubleValue(), 0.001 );
	}
	
	public void testGetSizesAndGetColorsWithZeroQuantityItems()
	{
		Product brandNewCar = createOutOfStockProduct();
		brandNewCar.getInventoryItem( 1 ).setQuantityInStock( 2 );
		assertEquals( "Number of sizes", 2, brandNewCar.getSizes().size() );
		assertEquals( "Number of colors", 2, brandNewCar.getColors().size() );
		//assertEquals( "Color", "Red", brandNewCar.getColors().get( 0 ) );
		//assertEquals( "Size", "Wagon", brandNewCar.getSizes().get( 0 ) );
	}
	
	protected Product createOutOfStockProduct()
	{
		Product brandNewCar = new Product( "brand new car" );
		brandNewCar.setId( "12345" );
		
		InventoryItem item = new InventoryItem();
		item.setQuantityInStock( 0 );
		item.setColor( "Blue" );
		item.setSize( "Sedan" );
		item.setSku( "300" );
		PriceSupport priceSupport = new PriceSupport();
		priceSupport.addTierPrice( 1, createPrice( 24000 ) );
		item.setPriceSupport( priceSupport );
		brandNewCar.addInventoryItem( item );
		
		item = new InventoryItem();
		item.setQuantityInStock( 0 );
		item.setColor( "Red" );
		item.setSize( "Wagon" );
		item.setSku( "200" );
		priceSupport = new PriceSupport();
		priceSupport.addTierPrice( 1, createPrice( 25000 ) );
		item.setPriceSupport( priceSupport );
		brandNewCar.addInventoryItem( item );
		
		return brandNewCar;
	}
	public void testProductDetails() throws Exception
	{
		PropertyDetails list = getStore().getProductArchive().getPropertyDetails();
		assertTrue( list.getDetails().size() >0 ); //we ship with some
		
		List index = list.findIndexProperties();
		assertTrue(index.size() >0);
	}
	
//	public void testRandomProduct() throws Exception
//	 {
//		Product p = getStore().getRandomProduct("GOODSTUFF");
//		assertNotNull(p);
//	 }

}