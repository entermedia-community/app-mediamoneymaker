/*
 * Created on Jan 21, 2005
 */
package com.openedit.store;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.openedit.store.convert.MainFrameConvertTest;

/**
 * The test suite for this package.
 * 
 * @author cburkey
 */
public class AllTests
{

	public static Test suite()
	{
		TestSuite suite = new TestSuite("Test for com.openedit.store");
		//$JUnit-BEGIN$
		suite.addTestSuite(ProductTest.class);
		suite.addTestSuite(TextExportTest.class);
		//suite.addTestSuite(CustomerTest.class);
		suite.addTestSuite(ItemTest.class);
		suite.addTestSuite(CatalogTest.class);
		suite.addTestSuite(SearchTest.class);
		suite.addTestSuite(ProductEditTest.class);
		suite.addTestSuite(SizeComparatorTest.class);
		suite.addTestSuite(SegmentedProductPathFinderTest.class);
		suite.addTestSuite(PriceSupportTest.class);
		suite.addTestSuite(VotingTest.class);
		suite.addTestSuite(MainFrameConvertTest.class);
		//$JUnit-END$
		return suite;
	}
}
