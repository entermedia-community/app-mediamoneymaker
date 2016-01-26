package com.openedit.store;

import org.openedit.WebPageRequest;
import org.openedit.hittracker.HitTracker;
import org.openedit.store.Category;
import org.openedit.store.Product;
import org.openedit.store.Store;
import org.openedit.store.StoreTestCase;
import org.openedit.store.modules.StoreSearchModule;
import org.openedit.store.search.ProductSecurityArchive;

public class ProductSecurityTest extends StoreTestCase{

	
	 public void testSearchSecurity() throws Exception {
		    System.out.println("Starting security test");
			StoreSearchModule module = (StoreSearchModule) getFixture().getModuleManager().getModule("StoreSearchModule");
			Store store = getStore();
			ProductSecurityArchive archive = store.getProductSecurityArchive();
			
			store.setUseSearchSecurity(true);
			WebPageRequest inRequest = getFixture().createPageRequest("/store/index.html");
		    Product p = store.getProduct("restricted");
		    p.setProperty("description", "ok");
		    //update the index for the test.
		    store.saveProduct(p);
			//restricted has view permissions on it.
		    archive.grantViewAccess(inRequest.getUser(),p );
		    Category cat = getStore().getCategoryArchive().getRootCategory();
		    
		    archive.grantViewAccess(inRequest.getUser(),p );
		    archive.grantViewAccess(inRequest.getUser(),cat );
		    //admin user can get this
			inRequest.setRequestParameter("field",new String[] {"status"});
		   
		    inRequest.setRequestParameter("operation",new String[] {"matches"});
		    inRequest.setRequestParameter("description.value","ok");
		    module.fieldSearch(inRequest);
		    HitTracker hits = (HitTracker)inRequest.getPageValue("hits");
			assertTrue(hits.size() > 0);
			//test user can't find it.
		    inRequest.setUser(getFixture().getUserManager().getUser("testuser"));		
		    module.fieldSearch(inRequest);
		    hits = (HitTracker)inRequest.getPageValue("hits");
			assertTrue(hits.getTotal() == 0);
		    
			
		}
	
}
