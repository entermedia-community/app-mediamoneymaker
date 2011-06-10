/*
 * Created on Sep 2, 2004
 */
package com.openedit.store;

import org.openedit.store.InventoryItem;
import org.openedit.store.StoreTestCase;


/**
 * @author cburkey
 *
 */
public class ItemTest extends StoreTestCase
{

	/**
	 * Constructor for ItemTest.
	 * @param arg0
	 */
	public ItemTest(String arg0)
	{
		super(arg0);
	}

	public void testexactMatch()
	{
		InventoryItem item1 = new InventoryItem();
		item1.setSize( "m" );
		item1.setColor( "green" );
		
		assertEquals( 2, item1.getOptions().size());
		
		InventoryItem redItem = new InventoryItem();
		redItem.setSize( "M" );
		redItem.setColor( "Red" );
		
		InventoryItem greenItem = new InventoryItem();
		greenItem.setSize( "M" );
		greenItem.setColor( "green" );
		
		InventoryItem sameColorItem = new InventoryItem();
		sameColorItem.setSize( "s" );
		sameColorItem.setColor( "green" );
		
		InventoryItem anotherGreenItem = new InventoryItem();
		anotherGreenItem.setSize("m");
		anotherGreenItem.setColor("GREEN");
		
		
		InventoryItem nullColorItem = new InventoryItem();
		anotherGreenItem.setSize("m");
		//anotherGreenItem.setColor("GREEN");
		
		
		InventoryItem nullItem = new InventoryItem();
		
//		assertFalse( nullItem.isExactMatch( item1 ) );
//		assertFalse( redItem.isExactMatch( item1 ) );
//		assertFalse( item1.isExactMatch( redItem ) );
//		assertFalse( sameColorItem.isExactMatch( greenItem) );
//		assertTrue( greenItem.isExactMatch(anotherGreenItem));
//		assertFalse( nullColorItem.isExactMatch( greenItem) );
//		assertFalse( greenItem.isExactMatch(nullColorItem) );
	}

}
