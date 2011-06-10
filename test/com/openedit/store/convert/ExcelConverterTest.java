/*
 * Created on Sep 23, 2004
 */
package com.openedit.store.convert;

import java.io.File;

import org.openedit.store.Category;
import org.openedit.store.Product;
import org.openedit.store.StoreTestCase;
import org.openedit.store.convert.ConvertStatus;
import org.openedit.store.excelconvert.GenericExcelConvert;

import com.openedit.util.OutputFiller;

/**
 * @author Matthew Avery, mavery@einnovation.com
 */
public class ExcelConverterTest extends StoreTestCase
{
	
	public ExcelConverterTest( String name )
	{
		super( name );
	}
	
	public void testWriteAndRead() throws Exception
	{
		
		GenericExcelConvert converter = new GenericExcelConvert();
		converter.convert(getStore(), new ConvertStatus());
		
		Product product = getStore().getProduct("cup1");
		
		assertNotNull( "Should have a product with ID cup1", product );

		assertEquals( 3, product.getInventoryItems().size() );

		Category catalog = (Category)product.getCategories().iterator().next();
		assertNotNull( "Should have a catalog", catalog );
		assertEquals("mugs",catalog.getId());
		
	}
	public void setUp() throws Exception 
	{
		File testfile = new File( getRoot().getParentFile(), "etc/testupload/inventory.xls");
		
		File uploadDir = new File( getRoot(), "/store/upload" );
		uploadDir.mkdir();

		OutputFiller filler = new OutputFiller();
		File newFile = new File( uploadDir, "inventory.xls" );
		filler.fill( testfile, newFile );
	}

}
