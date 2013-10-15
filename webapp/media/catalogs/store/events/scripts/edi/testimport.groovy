package edi;

import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.store.Store

import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.page.Page
import com.openedit.page.manage.PageManager


	public void testImportInventory() {
		
		Store store = context.getPageValue("store");
		Searcher searcher = store.getSearcherManager().getSearcher("media/catalogs/store", "invoice");
		Searcher itemsearcher = store.getSearcherManager().getSearcher("media/catalogs/store", "invoiceitem");
		searcher.deleteAll(null);
		itemsearcher.deleteAll(null);
		PageManager pm = store.getPageManager();
		List <String> files = pm.getChildrenPaths("/media/catalogs/store/samples/invoices/");
		
		for (Iterator iterator = files.iterator(); iterator.hasNext();)
		{
			Page test = pm.getPage((String)iterator.next());
			Page destination = pm.getPage("/WEB-INF/data/media/catalogs/store/incoming/invoices/" + test.getName());
			pm.copyPage(test, destination);
			
		}
		MediaArchive archive = context.getPageValue("mediaarchive");
		archive.fireMediaEvent("invoice/processinvoices", null);
				
		
		
	}




testImportInventory();