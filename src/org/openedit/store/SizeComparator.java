/*
 * Created on May 26, 2004
 */
package org.openedit.store;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author cburkey
 *
 */
public class SizeComparator implements Comparator {
	protected List fieldSortedSizes;
	
	/* (non-javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	public int compare(Object in01, Object in02) {
		String first = (String)in01;
		String second = (String)in02;
		int firstIndex = getSortedSizes().indexOf(first);
		int secondIndex = getSortedSizes().indexOf(second);
		if ( firstIndex == secondIndex)
		{
			return 0;			
		}
		if ( firstIndex > secondIndex)
		{
			return 1;
		}
		return -1;
	}

	public List getSortedSizes() {
		if (fieldSortedSizes == null) {
			fieldSortedSizes = new ArrayList();			
			fieldSortedSizes.add("P");
			fieldSortedSizes.add("NB");
			fieldSortedSizes.add("0M");
			fieldSortedSizes.add("3M");
			fieldSortedSizes.add("6M");
			fieldSortedSizes.add("9M");
			fieldSortedSizes.add("12M");
			fieldSortedSizes.add("18M");
			fieldSortedSizes.add("24M");
			fieldSortedSizes.add("2T");
			fieldSortedSizes.add("3T");
			fieldSortedSizes.add("4T");
			fieldSortedSizes.add("4");
			fieldSortedSizes.add("5");
			fieldSortedSizes.add("6");
			fieldSortedSizes.add("6X");
			fieldSortedSizes.add("7");
			fieldSortedSizes.add("8");
			fieldSortedSizes.add("10");
			fieldSortedSizes.add("12");
			fieldSortedSizes.add("14");
			fieldSortedSizes.add("16");
			fieldSortedSizes.add("N/A");

		}
		return fieldSortedSizes;
	}
	public void setSortedSizes(List inSortedSizes) {
		fieldSortedSizes = inSortedSizes;
	}
}
