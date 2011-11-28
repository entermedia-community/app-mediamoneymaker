/*
 * Created on Feb 15, 2005
 */
package com.openedit.store;

import java.io.File;

import junit.framework.TestCase;

import org.openedit.store.TextExport;

/**
 * @author Matthew Avery, mavery@einnovation.com
 */
public class TextExportTest extends TestCase
{
	File fieldOrdersFile = new File("/home/avery/workspace/openedit-cart/webapp/store/orders/orders.xml");

	public void testExports() throws Exception
	{
		TextExport exporter = new TextExport();
		File orders = File.createTempFile("orders", ".txt");
		exporter.exportOrders( getOrdersFile(), orders );
		File customers =File.createTempFile("customers", ".txt");
		exporter.exportCustomers( getOrdersFile(), customers );
		File items = File.createTempFile( "items", ".txt");
		exporter.exportItems( getOrdersFile(), items  );
	}

	

	public File getOrdersFile()
	{
		return fieldOrdersFile;
	}
}
