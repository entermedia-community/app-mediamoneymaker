package org.openedit.store.convert;

import org.openedit.store.Product;
import org.openedit.store.StoreTestCase;
import org.openedit.store.edit.ProductSourcePathCreator;

public class SourcePathTest extends StoreTestCase
{

	
	public void testLookup() throws Exception
	{
		Product product = new Product();
		product.setProperty("originalpath", "\\\\server\\share\\mystuff\\here\\1234.gif");
		product.setProperty("externalid", "photo");
		
		ProductSourcePathCreator create = new ProductSourcePathCreator();
		String sourcepath = create.createSourcePath(product);
		assertEquals("photo/share/mystuff/here/1234.gif",sourcepath);
		

		product.setProperty("originalpath", "D:\\mystuff\\here\\1234.gif");
		sourcepath = create.createSourcePath(product);
		assertEquals("photo/mystuff/here/1234.gif",sourcepath);

		//Linux with windows
		product.setProperty("originalpath", "/mystuff/here/1234.pdf");
		product.setProperty("pagenumber", "2");
		sourcepath = create.createSourcePath(product);
		assertEquals("photo/mystuff/here/1234_page2.pdf",sourcepath);

		product.setProperty("originalpath", "D:\\vault\\here\\1234");
		product.setProperty("pagenumber", null);
		product.setProperty("fileformat", "pdf");
		sourcepath = create.createSourcePath(product);
		assertEquals("photo/vault/here/1234.pdf",sourcepath);

		
	}
	
}
