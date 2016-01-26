package com.openedit.store;

import org.openedit.store.products.SegmentedProductPathFinder;

import junit.framework.TestCase;

public class SegmentedProductPathFinderTest extends TestCase
{
	protected SegmentedProductPathFinder fPathFinder;
	
	protected void setUp()
	{
		fPathFinder = new SegmentedProductPathFinder();
		fPathFinder.setSegmentLength( 3 );
		//fPathFinder.setMaxSegments( 2 );
	}
	
	public void testIdToPath()
	{
		fPathFinder = new SegmentedProductPathFinder();
		fPathFinder.setSegmentLength( 3 );
		assertEquals( "abc/abcde", fPathFinder.idToPath( "abcde" ) );
	}
	

	public void testReverseIdToPath()
	{
		//fPathFinder.setMaxSegments( 0 );
		fPathFinder = new SegmentedProductPathFinder();
		fPathFinder.setSegmentLength( 3 );
		fPathFinder.setReverse(true);
		
		assertEquals( "hij/abcdefghij", fPathFinder.idToPath( "abcdefghij" ) );
	}

	
	public void testGroupIdToPath()
	{
		//fPathFinder.setMaxSegments( 0 );
		fPathFinder = new SegmentedProductPathFinder();
		fPathFinder.setSegmentLength( 3 );
		fPathFinder.setGroupInTopCategory(true);
		
		assertEquals( "photo/abc/abcdefghij_photo", fPathFinder.idToPath( "abcdefghij_photo" ));
	}

	public void testGroupReverseToPath()
	{
		//fPathFinder.setMaxSegments( 0 );
		fPathFinder = new SegmentedProductPathFinder();
		fPathFinder.setSegmentLength( 3 );
		fPathFinder.setReverse(true);
		fPathFinder.setGroupInTopCategory(true);
		
		assertEquals( "photo/hij/abcdefghij_photo", fPathFinder.idToPath( "abcdefghij_photo" ) );
	}

	
}
