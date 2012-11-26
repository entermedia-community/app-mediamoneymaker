package org.openedit.store.modules;

import org.openedit.data.SearcherManager;
import org.openedit.store.Store;
import org.openedit.store.search.ProductSearcher;

import com.openedit.WebPageRequest;

public class DistributorModule extends BaseStoreModule {
	

	
	
	protected SearcherManager fieldSearcherManager;

	public SearcherManager getSearcherManager() {
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager fieldSearcherManager) {
		this.fieldSearcherManager = fieldSearcherManager;
	}
	
	
	public void loadDistributorProducts(WebPageRequest inReq){
		String distributorid = inReq.getUserProfileValue("distributor");
		Store store = getStore(inReq);
		ProductSearcher searcher = (ProductSearcher) getSearcherManager().getSearcher(store.getCatalogId(),"product");
		
		
	}
	
	
}
