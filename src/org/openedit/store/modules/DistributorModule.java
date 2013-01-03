package org.openedit.store.modules;

import org.openedit.data.SearcherManager;
import org.openedit.store.Store;
import org.openedit.store.search.ProductSearcher;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;

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
		SearchQuery query = searcher.createSearchQuery();
		query.append("distributor", distributorid);
		if(distributorid != null){
			HitTracker hits =  searcher.cachedSearch(inReq, query);
			String name = inReq.findValue("hitsname");
			inReq.putPageValue(name, hits);
			inReq.putSessionValue(hits.getSessionId(), hits);
		}
	
	}
	
	
}
