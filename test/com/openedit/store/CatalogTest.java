/*
 * Created on Jun 9, 2005
 */
package com.openedit.store;

import java.util.List;

import org.entermediadb.webui.tree.WebTree;
import org.openedit.WebPageRequest;
import org.openedit.store.Category;
import org.openedit.store.Option;
import org.openedit.store.StoreTestCase;
import org.openedit.store.modules.CatalogModule;
import org.openedit.users.filesystem.FileSystemGroup;


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

	
	
	
	public void xtestCatalogLimit() throws Exception
	{
		Category cat = getStore().getCatalog("LIMITGOODSTUFF");
		if( cat == null)
		{
			cat = getStore().getCategoryArchive().cacheCategory( new Category( "LIMITGOODSTUFF","Some Good Stuff"));
			getStore().getCategoryArchive().saveCategory( cat );
		}
		CatalogModule mod = (CatalogModule)getFixture().getModuleManager().getModule("CatalogModule");
		
		WebPageRequest req = getFixture().createPageRequest("/store/categories/index.html");
		
		FileSystemGroup g = new FileSystemGroup();
		g.setName("junk");
		g.addPermission("limittocategory:LIMITGOODSTUFF");
		req.getUser().addGroup(g);
		
		req.setRequestParameter("root", "index");
		req.setRequestParameter("tree-name", "test");
		
		WebTree tree = mod.getCatalogTree(req);
		Object top = tree.getModel().getRoot();
		int count = tree.getModel().getChildCount(top);
		assertEquals(1,count);

		req.getUser().removeGroup(g);
		req.setRequestParameter("tree-name", "testfresh");
		tree = mod.getCatalogTree(req);
		top = tree.getModel().getRoot();
		count = tree.getModel().getChildCount(top);
		assertTrue("Should be more than 1 was " + count, count > 1);
		

	}
	
	/*
	public void testCategory() throws Exception
	{
		Category blank = getStore().getCatalogArchive().addCatalog( "GOODSTUFF","Some Good Stuff");
		List children = blank.getCategories();
		
	}
	*/
}
