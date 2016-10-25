/*
 * Created on Jun 9, 2005
 */
package com.openedit.store;

import java.util.List;

import org.openedit.store.Category;
import org.openedit.store.Option;
import org.openedit.store.StoreTestCase;


/**
 * @author cburkey
 *
 */
public class CatalogTest extends StoreTestCase
{
	/**
	 * @param inArg0
	 */
	public CatalogTest(String inArg0)
	{
		super(inArg0);
	}

	public void testCatalogOptions() throws Exception
	{
		Category blank = getStore().getCategoryArchive().cacheCategory( new Category("GOODSTUFF","Some Good Stuff"));
		getStore().getCategoryArchive().saveCategory( blank );

		Category cat = getStore().getCatalog("GOODSTUFF");
		cat.clearOptions();
		
		Option oneOption = new Option();
		oneOption.setId("one");
		oneOption.setName("Another");
		cat.addOption(oneOption);
		
		getStore().getCategoryArchive().saveAll();
		
		cat = getStore().getCatalog("GOODSTUFF");
		Option one = cat.getOption("one");
		assertNotNull(one);
	}

	
	
	public void testRelatedCatalogs() throws Exception
	{
		Category blank = getStore().getCategoryArchive().cacheCategory( new Category( "GOODSTUFF","Some Good Stuff"));
		getStore().getCategoryArchive().saveCategory( blank );
		Category related = getStore().getCategoryArchive().cacheCategory( new Category( "RELATED","Some Related Stuff"));
		Category cat = getStore().getCatalog("GOODSTUFF");
		cat.clearRelatedCategoryIds();
		cat.addRelatedCategoryId(related.getId());
		getStore().getCategoryArchive().saveAll();
		
		cat = getStore().getCatalog("GOODSTUFF");
		List relatedIds = cat.getRelatedCategoryIds();
		assertTrue(relatedIds.size() >0);
	}

	
	
	
	
	/*
	public void testCategory() throws Exception
	{
		Category blank = getStore().getCatalogArchive().addCatalog( "GOODSTUFF","Some Good Stuff");
		List children = blank.getCategories();
		
	}
	*/
}
