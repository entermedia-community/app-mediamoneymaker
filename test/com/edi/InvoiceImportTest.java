package com.edi;

import java.util.Iterator;
import java.util.List;

import org.openedit.data.Searcher;
import org.openedit.store.Store;
import org.openedit.store.StoreTestCase;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;

public class InvoiceImportTest extends StoreTestCase {

	public void testImportInventory() throws Exception {
		
		Store store = getStore();
		Searcher searcher = getStore().getSearcherManager().getSearcher(store.getCatalogId(), "invoice");
		searcher.deleteAll(null);
		PageManager pm = getFixture().getPageManager();
		List <String> files = pm.getChildrenPaths("/media/catalogs/store/samples/invoices/");
		
		for (Iterator iterator = files.iterator(); iterator.hasNext();)
		{
			Page test = pm.getPage((String)iterator.next());
			Page destination = pm.getPage("/WEB-INF/data/media/catalogs/store/incoming/invoices/" + test.getName());
			pm.copyPage(test, destination);
			
		}
		WebPageRequest req = getFixture().createPageRequest("/media/catalogs/store/events/invoice/processinvoices.html");
		getFixture().getEngine().executePathActions(req);
		
		HitTracker invoices = searcher.getAllHits();
		assertTrue(invoices.size() > 0);
		
		
	}

}
