/*
 * Created on Mar 24, 2004
 */
package com.openedit.modules.cart;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.openedit.BaseTestCase;
import com.openedit.modules.cart.editor.CatalogEditTest;
import com.openedit.store.PriceSupportTest;
import com.openedit.store.orders.OrderExportTest;

/**
 * @author cburkey
 *
 */
public class AllTests
{
	
	
	public static Test suite()
	{
		TestSuite suite = new TestSuite("eCommerce Test");
		//$JUnit-BEGIN$
		String base = System.getProperty("oe.root.path");
		if ( base == null)
		{
			System.setProperty("oe.root.path","webapp");
		}
		//suite.addTest(com.openedit.store.retailproconvert.AllTests.suite());
		//suite.addTestSuite(ExcelConverterTest.class);
		//suite.addTestSuite(AniaraConverterTest.class);
		suite.addTestSuite(CatalogEditTest.class);
		//suite.addTestSuite(AuthorizeNetOrderArchiveTest.class );
		suite.addTest(com.openedit.store.AllTests.suite());
		suite.addTestSuite(CartTest.class);
		//suite.addTestSuite(ImageResizerTest.class);
		suite.addTestSuite(OrderExportTest.class);
		suite.addTestSuite(PriceSupportTest.class);
		BaseTestCase.getStaticFixture().getWebServer().getOpenEditEngine().shutdown();
		//$JUnit-END$
		return suite;
	}
}
