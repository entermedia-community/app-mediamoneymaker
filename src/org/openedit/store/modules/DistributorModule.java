package org.openedit.store.modules;

import org.openedit.WebPageRequest;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.store.Store;
import org.openedit.store.search.ProductSearcher;

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
