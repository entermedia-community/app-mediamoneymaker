/*
 * Created on Nov 17, 2004
 */
package com.openedit.modules.cart.editor;

import java.util.Iterator;
import java.util.List;

import org.openedit.data.CompositeData;
import org.openedit.entermedia.modules.DataEditModule;
import org.openedit.store.Category;
import org.openedit.store.InventoryItem;
import org.openedit.store.Product;
import org.openedit.store.StoreTestCase;
import org.openedit.store.edit.StoreEditor;
import org.openedit.store.modules.CatalogEditModule;
import org.openedit.store.modules.StoreSearchModule;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.util.PathUtilities;

/**
 * @author cburkey
 *
 */
public class CatalogEditTest extends StoreTestCase
{
	protected StoreEditor fieldStoreEditor;
	
	/**
	 * @param inArg0
	 */
	public CatalogEditTest(String inArg0)
	{
		super(inArg0);
	}
	protected void setUp() throws Exception
	{
		Category blank = getStoreEditor().addNewCatalog( "GOODSTUFF","Some Good Stuff");
		assertNotNull( blank );
		getStoreEditor().saveCatalog( blank );
		
	}
	
	public void testExtraInfo() throws Exception
	{
		getStore().getProductSearcher().reIndexAll();
		Category catalog = getStoreEditor().getCatalog("GOODSTUFF");
		catalog.setDescription("This is a long description");
		catalog.setProperty("includequickship",true);
		String name = "/images/small.jpg";
		catalog.setProperty("image1small",name);
		catalog.setProperty("image1big","/images/small.jpg");
		catalog.setProperty("otherdownload","/pdfs/other.pdf");
		getStoreEditor().saveCatalog(catalog);
		getStoreEditor().clearCatalogs();
		getStoreEditor().reloadCatalogs();
		catalog = getStoreEditor().getCatalog("GOODSTUFF");
		String image1 = catalog.getProperty("image1small");
		assertNotNull(image1);
		assertEquals(name,image1);
		String includeQuickShip = catalog.getProperty("includequickship");
		assertNotNull(includeQuickShip);
		assertEquals("true", includeQuickShip);
	}

	public void testDeleteCatalog() throws Exception
	{
		Category catalog = getStoreEditor().getCatalog("GOODSTUFF");
		assertNotNull( catalog );
		getStoreEditor().deleteCatalog( catalog);
		getStoreEditor().getStore().clear();

		Category deletedcatalog = getStoreEditor().getCatalog("GOODSTUFF");
		assertNull( deletedcatalog );
		getStoreEditor().saveCatalog(catalog);
	}
	
	public void testListCatalogs() throws Exception
	{
		Category catalog = getStoreEditor().getRootCatalog();
		assertNotNull( catalog );
		assertTrue( catalog.getChildren().size() > 0 );
	}

	public void testAddAndDeleteProduct() throws Exception
	{
		Product product = getStoreEditor().createProduct();
		product.setId("1234");
		product.setName("Test product");
		product.setDescription("Blank");

		//now user hits save
		Category catalog = getStoreEditor().getCatalog( "GOODSTUFF" );
		product.addCatalog( catalog );
		getStoreEditor().saveProduct( product );
		getStoreEditor().clearCatalogs();
		
		//getStoreEditor().getStore().reindexAll();
		product = findProductById(catalog,"1234");
		assertNotNull(product);

		getStoreEditor().deleteProduct( product );
		getStoreEditor().clearCatalogs();

		product = findProductById(catalog,"1234");
		assertNull(product);

	}
	public void testEditProduct() throws Exception
	{
		final String PRODUCT_NAME = "Closed Back Swivel\u2122"; // trademark symbol
		Product product = getStoreEditor().createProduct();
		product.setId("1234");
		product.setSourcePath("/1234");
		product.setName(PRODUCT_NAME);
		
		product.setDescription("Test description");
		
		getStoreEditor().saveProduct( product );
		getStoreEditor().getStore().clearProducts();
		product = getStoreEditor().getProduct("1234");
		assertNotNull( product );
		assertEquals( PRODUCT_NAME, product.getName() );
		
		getStoreEditor().deleteProduct( product );
		//getStoreEditor().getStore().reindexAll();
		
	}

	public void testEditItem() throws Exception
	{
		Product product = getStoreEditor().createProduct();
		product.setId("1234");
		product.setName("Test product");
		product.setDescription("Blank");

		//product.
		InventoryItem item = getStoreEditor().createItem( );
		item.put("depth","42");
		final String DESCRIPTION = "red 23\" high back";
		item.setDescription(DESCRIPTION);
		product.clearItems();
		product.addInventoryItem(item);
		
		getStoreEditor().saveProduct( product );
		product = getStoreEditor().getProduct("1234");
		assertNotNull( product );
		item = product.getInventoryItem(0);
		assertEquals( DESCRIPTION, item.getDescription() );
		assertEquals(  "42", item.getProperty("depth"));		
		getStoreEditor().deleteProduct( product );

	}
	public StoreEditor getStoreEditor() throws Exception
	{
		if ( fieldStoreEditor == null)
		{
			fieldStoreEditor = new StoreEditor();
			fieldStoreEditor.setStore(getStore());
			fieldStoreEditor.setPageManager(getFixture().getPageManager());
		}
		return fieldStoreEditor;
	}
	
	public void testAddAffixesToProducts() throws Exception
	{
		StoreEditor editor = getStoreEditor();
		Category catalog = editor.addNewCatalog( "AFFIXME","Add Some Affixes");
		editor.saveCatalog( catalog );
		editor.setCurrentCatalog( catalog );
		Product product = editor.createProduct();
		product.setId("0000");
		product.setName("Test Product");
		product.addCatalog(catalog);
		product.setDescription("Blank");
		editor.saveProduct(product);
		editor.saveCatalog( catalog );
		editor.addProductAffixes(catalog, "PRE", "SUF");
		product = editor.getProduct("0000");
		assertNotNull(product);
		assertEquals(product.getCategories().size(), 1);  //only is in the old catalog
		Product product2 = editor.getProduct("PRE0000SUF");
	  	assertNotNull(product2);
		assertEquals(product2.getName(), "Test Product"); //still has same name
		assertEquals(product2.getCategories().size(), 1); //only is in the new catalog
		editor.deleteProduct(product);
		editor.deleteProduct(product2);
		//editor.getStore().reindexAll();
		editor.deleteCatalog(catalog);
		editor.getStore().clear();
		editor.reloadCatalogs();
	}

	public void testAddAndEditProduct() throws Exception
	{
		Product product = getStoreEditor().createProduct();
		String newId = "junk"  + System.currentTimeMillis();
		product.setId(newId); // just in case case matters
		product.setName("Test product");
		product.setDescription("Blank");
	
		//now user hits save
		Category catalog = getStoreEditor().getCatalog( "GOODSTUFF" );
		product.addCatalog( catalog );
		getStoreEditor().saveProduct( product );
		//getStore().getProductLoader().saveProduct(product);
		//getStoreEditor().getStore().reindexAll();
		
		Product foundproduct = findProductById(catalog,newId);
		
		//if this is null then the update index is not working
		assertNotNull(foundproduct);
		String name = "New Name" + System.currentTimeMillis();
		foundproduct.setName(name);
		getStoreEditor().saveProduct(foundproduct);
		List products = getStore().getProductsInCatalog( catalog );
		int productsFound = 0;
		for (Iterator iter = products.iterator(); iter.hasNext();)
		{
			Product element = (Product) iter.next();
			if ( element.getName().equals(name))
			{
				productsFound++;
			}
		}
		assertEquals( "Number of products with the right name in the catalog",
			1, productsFound );
	}

	private Product findProductById(Category inCatalog, String inId) throws Exception
	{
		List products = getStore().getProductsInCatalog( inCatalog );
		assertNotNull(products);

		for (Iterator iter = products.iterator(); iter.hasNext();)
		{
			Product element = (Product) iter.next();
			if ( element.getId().equals(inId))
			{
				return element;
			}
		}
		return null;
	}
	
	public void xtestRemoveFromCatalog() throws Exception
	{
		Product product = getStore().getProduct("1");
		Category good = getStore().getCatalog("GOODSTUFF");
		product.addCatalog(good);
		getStore().saveProduct(product);

		product = findProductById(good,"1");
		//assertNotNull( product ); this passes on a local machine
		
		StoreEditor editor = getStoreEditor();
		editor.removeProductFromCatalog(good,new String[]{"1"});
		
		assertNull( findProductById(good,"1") );

	}
	public void xtestCopyProducts() throws Exception {
		// First lets create a customer
		CatalogEditModule mod = (CatalogEditModule) getFixture().getModuleManager()
				.getModule("CatalogEditModule");
		WebPageRequest context = getFixture().createPageRequest();
		context = getFixture().createPageRequest();
		
		Product product = getStore().getProduct("1");
		String sourcePath = product.getSourcePath();
		String newName = "copy.data";
		context.setRequestParameter("sourcepath", sourcePath);
		context.setRequestParameter("name", newName);
		mod.copyProduct(context);
		
		String newSourcePath = PathUtilities.extractDirectoryPath(sourcePath) + "/" + newName;
		Product copy = getStore().getProductBySourcePath(newSourcePath);
		
		assertNotNull(copy);
		assertTrue(newName.equals(copy.getName()));
		
		getStore().getProductArchive().deleteProduct(copy);
	
	}
	
	

	
}
