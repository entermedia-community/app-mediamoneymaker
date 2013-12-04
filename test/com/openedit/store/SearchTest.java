/*
 * Created on May 2, 2004
 */
package com.openedit.store;

import java.util.Iterator;
import java.util.List;

import junit.textui.TestRunner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.data.lucene.LuceneHitTracker;
import org.openedit.store.Category;
import org.openedit.store.Product;
import org.openedit.store.Store;
import org.openedit.store.StoreTestCase;
import org.openedit.store.edit.StoreEditor;
import org.openedit.store.modules.CartModule;
import org.openedit.store.modules.StoreSearchModule;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;

/**
 * @author cburkey
 *
 */
public class SearchTest extends StoreTestCase
{
	private static final Log log = LogFactory.getLog(SearchTest.class);
	
	protected StoreEditor fieldStoreEditor;
	
	public StoreEditor getStoreEditor() throws Exception
	{
		if ( fieldStoreEditor == null)
		{
			fieldStoreEditor = new StoreEditor();
			fieldStoreEditor.setStore(getStore());
			fieldStoreEditor.setPageManager(getStaticFixture().getPageManager());
		}
		return fieldStoreEditor;
	}
	
	 public SearchTest(String inName)
	 {
	 	super( inName );
	 }
		public static void main(String[] args) throws Exception
		{
//			 runner = new TestRunner();
			 //TestRunner.run(ProductEditTest.class);
			//SearchTest test = new SearchTest("SearchTest");
			//test.setUp();
			//test.testMakeIndex();
			TestRunner runner = new TestRunner();
			runner.run(SearchTest.class);
		}

	 
	 public void testMakeIndex() throws Exception
	 {
	 	//store.reindexall
		CartModule module = (CartModule)getStaticFixture().getModuleManager().getModule("CartModule");
//		Store store = getStore();
//		Product three = store.getProduct("3");
//		three.setAvailable(true);
//		store.saveProduct(three);
		WebPageRequest context = getStaticFixture().createPageRequest("/store/index.html");
		module.reIndexStore(context);
	 }
	 
	 /* TODO: Write a test for store.getProperties("sometype")
	 public void testFieldSearchLoad() throws Exception
	 {
		 	//load the store search module
		
			StoreSearchModule module = (StoreSearchModule)getStaticFixture().getModuleManager().getModule("StoreSearchModule");
			WebPageRequest context = getStaticFixture().createPageRequest("/store/index.html");
			module.loadSearchFields(context);
			List fields = (List) context.getPageValue("searchFieldList");
		 	assertNotNull(fields);
		
	 }
	 */
	 
	 public void testSearch() throws Exception
	 {
		StoreSearchModule module = (StoreSearchModule)getStaticFixture().getModuleManager().getModule("StoreSearchModule");
		WebPageRequest context = getStaticFixture().createPageRequest();
		context.setRequestParameter("query","catalogs:NOSUCHCATALOG");
		//assertNotNull(module.getSearcherManager());
		module.searchStore(context);
		LuceneHitTracker hits = (LuceneHitTracker)context.getPageValue("hits");
	 	assertNotNull(hits);
	 	//This product should not be available
	 	assertFalse(hits.getAllHits().hasNext());
	 }
	 
 
	public void xtestCatalogSearch() throws Exception
	{
	
		StoreEditor editor = getStoreEditor();
		
		Category catalog = editor.addNewCatalog( "SEARCHME","Ready for Searching");
		editor.saveCatalog( catalog );
		editor.setCurrentCatalog( catalog );
		Product product = null;
		product = editor.getProduct("3");
		product.addCatalog(catalog);
		product.setAvailable(true);
		product.setDescription("Blank");
		editor.saveProduct(product);
		product = editor.getProduct("1");
		product.addCatalog(catalog);
		product.setAvailable(true);
		product.setDescription("Blank");
		editor.saveProduct(product);
		product = editor.getProduct("4");
		product.addCatalog(catalog);
		product.setAvailable(true);
		product.setDescription("Blank");
		editor.saveProduct(product);
		//editor.getStore().reindexAll();
		WebPageRequest context = getStaticFixture().createPageRequest("/store/categories/SEARCHME.html");
		getStaticFixture().getModuleManager().executePathActions(context.getPage(),context);
		LuceneHitTracker hits = (LuceneHitTracker)context.getPageValue("hits");
	 	assertNotNull(hits);

	 	assertEquals(3,hits.getTotal());
	 	Iterator hitIterator = hits.getAllHits();
	 	Data doc = (Data) hitIterator.next();
	 	
	 	String id = doc.get("id");
	 	/*
	 	assertEquals( "1",id);
	 	doc = (Document) hitIterator.next();
	 	id = doc.getField("id").stringValue();
	 	assertEquals( "3",id);
	 	doc = (Document) hitIterator.next();
	 	id = doc.getField("id").stringValue();
	 	assertEquals( "4",id);
*/
	 	
		context = getStaticFixture().createPageRequest("/store/categories/byprice/SEARCHME.html");
		
		getStaticFixture().getModuleManager().executePathActions(context.getPage(),context);
		hits = (LuceneHitTracker)context.getPageValue("hits");
	 	assertNotNull(hits);
	 	assertEquals(3,hits.getTotal());
	 	hitIterator = hits.getAllHits();
	 	doc = (Data) hitIterator.next();
	 	id = doc.get("id");
//	 	log.info(id);
//		assertEquals( "1",id);
//	 	doc = (Document) hitIterator.next();
//	 	id = doc.getField("id").stringValue();
//	 	log.info(id);
//	 	assertEquals( "4",id);
//	 	doc = (Document) hitIterator.next();
//	 	id = doc.getField("id").stringValue();
//	 	log.info(id);
//	 	assertEquals( "3",id);

		//Document hit = hits.getHits().doc(0);
		//Document hit2 = hits.getHits().doc(1);
		 
		//assertEquals("PAAAIAOKJOLAOMAH",hit.get("id"));
		//assertEquals("PAAAIAMBOGHOOMAH",hit2.get("id"));
	}
	public void testWeirdIds() throws Exception
	{
		StoreEditor editor = getStoreEditor();
		Category catalog = editor.addNewCatalog( "SEA_R,Ch-ME.txt","Weird ID");
		editor.saveCatalog( catalog );
		editor.setCurrentCatalog( catalog );
		Product product = null;
		product = editor.getProduct("4");
		product.addCatalog(catalog);
		product.setAvailable(true);
		editor.saveProduct(product);

		
		//We have lost the ability to search within html descriptions
//		getStore().getProductArchive().saveProductDescription(product,"Blank chris With chris_amy.jpg Category");
//		HitTracker hits = editor.getStore().search("chris");
//		assertEquals(1, hits.getTotal());
		
		//hits = editor.getStore().search("really");
		//assertEquals(1, hits.length());

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
	
	 public void testAdvancedSearch() throws Exception
	 {
		StoreSearchModule module = (StoreSearchModule)getStaticFixture().getModuleManager().getModule("StoreSearchModule");
		WebPageRequest context = getStaticFixture().createPageRequest("/store/index.html");
		context.setRequestParameter("categoryid","testing");
		context.setRequestParameter("andwords","andthis");
		context.setRequestParameter("quotewords","this exactly");
		context.setRequestParameter("orwords","orthis1 orthis2");
		context.setRequestParameter("notwords","nothis");
		//context.setRequestParameter("daterange","12");
		context.setRequestParameter("sortby","random");
	
		//module.advancedSearch(context);
		//LuceneHitTracker hits = (LuceneHitTracker)context.getPageValue("hits");
	 	//assertNotNull(hits);
	 	//String query = hits.getQuery();
	 	//String q = "NOT nothis AND category:testing AND andthis AND \"this exactly\" AND (orthis1 OR orthis2) AND date:";//[07/19/2005 TO 07/19/2006] ";
	 	//String q = " NOT nothis +category:(testing) +andthis +\"this exactly\" +(orthis1 OR orthis2)";//NOT nothis +category:testing +andthis +\"this exactly\" +(orthis1 OR orthis2) +date:[";
	 	//assertFalse(hits.getAllHits().hasNext());
	 	//assertTrue(query.startsWith(q));
	 }
 
	 public void xtestFieldSearch() throws Exception
	 {
		StoreSearchModule module = (StoreSearchModule)getStaticFixture().getModuleManager().getModule("StoreSearchModule");
		WebPageRequest context = getStaticFixture().createPageRequest("/store/index.html");
	/*	
		<input name="field" value="Product Name" type="hidden">
<input name="op" value="contains" type="hidden">
<input name="value" value="" size="15" type="text"><br>
	<input value="go" name="go" type="submit"><br>
	<input name="sorton" value="" type="hidden">
	<input name="ascending" value="1" type="hidden">
<br>
<input name="field" value="Department" type="hidden">
Department<br>
matches<br>
<input name="op" value="matches" type="hidden">
<select name="value">
<option></option>
*/		
		context.setRequestParameter("field",new String[] {"name","department","imagenumber", "dateshot"});
		context.setRequestParameter("operation",new String[] {"matches","matches","startswith", "after"});
		context.setRequestParameter("name.value","test");
		context.setRequestParameter("department.value","01 02");
		context.setRequestParameter("imagenumber.value","123");
		//context.setRequestParameter("dateshot.value","1/10/2004");
		module.fieldSearch(context);
		LuceneHitTracker hits = (LuceneHitTracker)context.getPageValue("hits");
	 	assertNotNull(hits);
	 	String query = hits.getQuery();
	 	String q = "name:(test) +department:(01 02) +imagenumber:(123*)";
	 	//assertFalse(hits.getAllHits().hasNext());
	 	assertEquals(q, query);
	 }
	 
	 public void testUnderscore() throws Exception
	 {
		 Store store = getStore();
		 Product mug = new Product();
		 mug.setName("apple_books.eps");
		 mug.setId("mugtest");
		 store.saveProduct(mug);
		 HitTracker hits = store.search("apple_books.eps");
		 log.info("search was: " + hits.getQuery());
		 assertTrue( hits.getTotal() > 0);
		 
		 hits = store.search("apple_books.ep*");
		 log.info("search was: " + hits.getQuery());
		 assertTrue( hits.getTotal() > 0);

		 hits = store.search("apple_boo*");
		 assertTrue( hits.getTotal() > 0);
	 
		 hits = store.search("apple_junk*");
		 assertTrue( hits.getTotal() == 0);
	 }
	 
	 public void xtestStemmerSearch() throws Exception
	 {
		StoreSearchModule module = (StoreSearchModule)getStaticFixture().getModuleManager().getModule("StoreSearchModule");
		WebPageRequest context = getStaticFixture().createPageRequest("/store/index.html");
	/*	
		<input name="field" value="Product Name" type="hidden">
<input name="op" value="contains" type="hidden">
<input name="value" value="" size="15" type="text"><br>
	<input value="go" name="go" type="submit"><br>
	<input name="sorton" value="" type="hidden">
	<input name="ascending" value="1" type="hidden">
<br>
<input name="field" value="Department" type="hidden">
Department<br>
matches<br>
<input name="op" value="matches" type="hidden">
<select name="value">
<option></option>
*/		
		context.setRequestParameter("field",new String[] {"description"});
		context.setRequestParameter("operation",new String[] {"matches"});
		context.setRequestParameter("description.value","mug");
	
		module.fieldSearch(context);
		LuceneHitTracker hits = (LuceneHitTracker)context.getPageValue("hits");
		context.setRequestParameter("description.value","mugs");
		module.fieldSearch(context);
		LuceneHitTracker hits2 = (LuceneHitTracker)context.getPageValue("hits");
		log.info("query was: " + hits2.getQuery());
		assertEquals(hits.getTotal(), hits2.getTotal());
		assertTrue( hits.getTotal() > 0 );
	 }

	 public void xtestCentury21() throws Exception
	 {
		StoreSearchModule module = (StoreSearchModule)getStaticFixture().getModuleManager().getModule("StoreSearchModule");
		WebPageRequest context = getStaticFixture().createPageRequest("/store/index.html");
		context.setRequestParameter("field",new String[] {"description"});
		context.setRequestParameter("fieldid",new String[] {"description"});
		context.setRequestParameter("operation",new String[] {"startswith"});
		context.setRequestParameter("description.value","centur");
	
		module.fieldSearch(context);
		LuceneHitTracker hits = (LuceneHitTracker)context.getPageValue("hits");
		assertTrue( hits.getTotal() > 0 );

		context.setRequestParameter("description.value","century");
		module.fieldSearch(context);
		hits = (LuceneHitTracker)context.getPageValue("hits");
		assertTrue( hits.getTotal() > 0 );

		context.setRequestParameter("description.value","harleyd");
		module.fieldSearch(context);
		hits = (LuceneHitTracker)context.getPageValue("hits");
		assertTrue( hits.getTotal() > 0 );

		context.setRequestParameter("description.value","harley");
		module.fieldSearch(context);
		hits = (LuceneHitTracker)context.getPageValue("hits");
		assertTrue( hits.getTotal() > 0 );
	 }

	 
	 public void xtestSearchSecurity() throws Exception 
	 {
		 System.out.println("Starting security test");
		 StoreSearchModule module = (StoreSearchModule) getStaticFixture().getModuleManager().getModule("StoreSearchModule");
		 Store store = getStore();
		 store.setUseSearchSecurity(true);
		 WebPageRequest inRequest = getStaticFixture().createPageRequest("/store/index.html");
		 Product p = store.getProduct("restricted");
		 p.setProperty("name", "testname");
		 //update the index for the test.
		 store.saveProduct(p);
		 //restricted has view permissions on it.

		 //admin user can get this
		 inRequest.setRequestParameter("field",new String[] {"name"});
		 inRequest.setRequestParameter("fieldid",new String[] {"name"});
		 inRequest.setRequestParameter("operation",new String[] {"matches"});
		 inRequest.setRequestParameter("name.value","testname");
		 module.fieldSearch(inRequest);
		 LuceneHitTracker hits = (LuceneHitTracker)inRequest.getPageValue("hits");
		 assertTrue(hits.size() > 0);

		 /* THIS IS NOT PASSING. PLEASE SOMEBODY TAKE A LOOK */
		 //test user can't find it.
		 //inRequest.setUser(getStaticFixture().getUserManager().getUser("testuser"));		
		 //module.fieldSearch(inRequest);
		 //hits = (LuceneHitTracker)inRequest.getPageValue("hits");
		 //assertEquals(0, hits.getTotal());
	 }

	 public void xtestNumericalSearch() throws Exception 
	 {
		 StoreSearchModule module = (StoreSearchModule) getStaticFixture().getModuleManager().getModule("StoreSearchModule");
		 Store store = getStore();
		 store.setUseSearchSecurity(false);
		 WebPageRequest inRequest = getStaticFixture().createPageRequest("/testcatalog/index.html");

		 Product testProduct = new Product();
		 testProduct.setId("accessible");
		 testProduct.setName("access");
		 testProduct.setProperty("number", "-49.9");
		 store.saveProduct(testProduct);

		 inRequest.setRequestParameter("field",new String[] {"number"});
		 inRequest.setRequestParameter("fieldid",new String[] {"number"});
		 inRequest.setRequestParameter("operation",new String[] {"lessthan"});
		 inRequest.setRequestParameter("number.value","100");

		 module.fieldSearch(inRequest);
		 LuceneHitTracker hits = (LuceneHitTracker)inRequest.getPageValue("hits");

		 assertTrue(hits.size() > 0);
		 inRequest.setRequestParameter("operation",new String[] {"greaterthan"});
		 inRequest.setRequestParameter("number.value","-100");		    
		 module.fieldSearch(inRequest);

		 hits = (LuceneHitTracker)inRequest.getPageValue("hits");

		 assertTrue(hits.size() > 0);

		 inRequest.setRequestParameter("operation",new String[] {"between"});
		 inRequest.setRequestParameter("number.lowval","-100.12");		    
		 inRequest.setRequestParameter("number.highval","100.25");		    

		 module.fieldSearch(inRequest);

		 hits = (LuceneHitTracker)inRequest.getPageValue("hits");
		 assertTrue(hits.size() > 0);
	 }

}
