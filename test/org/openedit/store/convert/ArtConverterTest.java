package org.openedit.store.convert;

import org.openedit.store.Store;
import org.openedit.store.StoreTestCase;

public class ArtConverterTest extends StoreTestCase
{
	ArtConverter converter;
	String line = "15100REINO ANIMAL                                                19900ANIMALES DOMESTICOS Y MASCOTAS      ";
	protected void setUp() throws Exception
	{
		super.setUp();
		converter = new ArtConverter();
	}

	public void testExtractCategoryId()
	{
		assertEquals( "15100", converter.extractCategoryId( line ) );
	}
	
	public void testExtractCategoryName()
	{
		assertEquals( "REINO ANIMAL", converter.extractCategoryName( line ) );
	}
	
	public void testExtractSubCategoryId()
	{
		assertEquals( "19900", converter.extractSubCategoryId( line ) );
	}
	
	public void testExtractSubCategoryName()
	{
		assertEquals( "ANIMALES DOMESTICOS Y MASCOTAS", converter.extractSubCategoryName( line ) );
	}
	
	public void testConvertCatagories() throws Exception
	{
		Store store = getStore();
		ConvertStatus status = new ConvertStatus();
		converter.convertCategories( store, status );
		assertEquals( 19, store.getCategoryArchive().getRootCategory().getChildren().size() );
	}
	
	public void testConvertArt() throws Exception
	{
		Store store = getStore();
		ConvertStatus status = new ConvertStatus();
		converter.convertArt( store, status );
		//assertEquals( 60, store.getProductArchive().listAllProductIds().size() );
	}

}
