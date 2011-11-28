/*
 * Created on Jun 15, 2004
 */
package com.openedit.store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openedit.store.SizeComparator;
import org.openedit.store.StoreTestCase;

/**
 * @author cburkey
 *
 */
public class SizeComparatorTest extends StoreTestCase {
/*
 * 
 * Preemie
Newborn
3M
6M
9M
12M
18M
24M
2T
3T
4T
4
5
6
6X
7
8
10
12
14
16


 * 
 */
   public SizeComparatorTest( String inName )
   {
      super( inName );
   }
   
	public void testSort()
	{
		List allSizes = new ArrayList();
		allSizes.add("P");
		allSizes.add("0M");
		allSizes.add("3M");
		allSizes.add("6M");
		allSizes.add("9M");
		allSizes.add("12M");
		allSizes.add("18M");
		allSizes.add("24M");
		allSizes.add("2T");
		allSizes.add("3T");
		allSizes.add("4T");
		allSizes.add("4");
		allSizes.add("5");
		allSizes.add("6");
		allSizes.add("6X");
		allSizes.add("7");
		allSizes.add("8");
		allSizes.add("10");
		allSizes.add("12");
		allSizes.add("14");
		allSizes.add("16");
		

		List unsorted = new ArrayList();
		unsorted.add("9M");
		unsorted.add("12M");
		unsorted.add("18M");
		unsorted.add("0M");
		unsorted.add("3M");
		unsorted.add("24M");
		unsorted.add("P");
		unsorted.add("6M");
		unsorted.add("6X");
		unsorted.add("7");
		unsorted.add("8");
		unsorted.add("4T");
		unsorted.add("4");
		unsorted.add("5");
		unsorted.add("16");
		unsorted.add("6");
		unsorted.add("10");
		unsorted.add("2T");
		unsorted.add("3T");
		unsorted.add("12");
		unsorted.add("14");

		String sorted = allSizes.toString();
		Collections.sort(unsorted, new SizeComparator());
		
		assertEquals(unsorted.toString(), allSizes.toString());
	}
	
}
