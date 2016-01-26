/*
 * Created on Sep 23, 2004
 */
package com.openedit.store.convert;

import java.io.File;

import org.openedit.store.Category;
import org.openedit.store.Product;
import org.openedit.store.StoreTestCase;
import org.openedit.store.convert.ConvertStatus;
import org.openedit.store.convert.CsvConverter;
import org.openedit.util.OutputFiller;

/**
 * @author Matthew Avery, mavery@einnovation.com
 */
public class CsvConvertTest extends StoreTestCase
{
	
	public CsvConvertTest( String name )
	{
		super( name );
	}
	
	public void testWriteAndRead() throws Exception
	{
		
		CsvConverter converter = new CsvConverter();
		converter.convert(getStore(), new ConvertStatus());
		
		Product product = getStore().getProduct("PAAAAAKCBNGEAHAI");
		
		assertNotNull( "Should have a product with ID PAAAAAKCBNGEAHAI", product );

		assertEquals( 5, product.getInventoryItems().size() );

		Category catalog = (Category)product.getCategories().iterator().next();
		assertNotNull( "Should have a catalog", catalog );
		
	}
	public void XsetUp() throws Exception 
	{
		File testfile = new File( getRoot().getParentFile(), "etc/testupload/inventory.csv");
		
		File uploadDir = new File( getRoot(), "/store/upload" );
		uploadDir.mkdir();

		OutputFiller filler = new OutputFiller();
		File newFile = new File( uploadDir, "inventory.csv" );
		filler.fill( testfile, newFile );
	}

}
