package com.edi;

import java.util.Iterator;
import java.util.List;

import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;
import org.openedit.store.Store;
import org.openedit.store.StoreTestCase;

public class InvoiceImportTest extends StoreTestCase {

	public void testImportInventory() throws Exception {
		
		Store store = getStore();
		Searcher searcher = getStore().getSearcherManager().getSearcher("media/catalogs/store", "invoice");
		Searcher itemsearcher = getStore().getSearcherManager().getSearcher("media/catalogs/store", "invoiceitem");
		searcher.deleteAll(null);
		itemsearcher.deleteAll(null);
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
