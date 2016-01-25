/*
 * Created on Jul 19, 2006
 */
package org.openedit.store.modules;

import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.SearchQueryArchive;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.ListHitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.store.Cart;
import org.openedit.store.Store;

public class StoreSearchModule extends BaseStoreModule
{
	private static final Log log = LogFactory.getLog(StoreSearchModule.class);
	protected SearchQueryArchive fieldSearchQueryArchive;
	
	public void loadClearance(WebPageRequest inReq){
		Store store = getStore(inReq);
		
	}
	
	public SearchQueryArchive getSearchQueryArchive() {
		return fieldSearchQueryArchive;
	}

	public void setSearchQueryArchive(SearchQueryArchive inSearchQueryArchive) {
		fieldSearchQueryArchive = inSearchQueryArchive;
	}

	/*
	 * public void loadSearchFields(WebPageRequest inPageRequest) throws
	 * Exception {
	 * 
	 * String prefix = findValue("prefix", inPageRequest); List keys =
	 * inPageRequest.getPage().getPageSettings().getAllPropertyKeysWithoutPrefix(prefix);
	 * Store store = getStore(inPageRequest); List details = new ArrayList();
	 * for (Iterator iter = keys.iterator(); iter.hasNext();) { String key =
	 * (String) iter.next(); Detail detail =
	 * store.getProductArchive().getPropertyDetails().getDetail(key);
	 * details.add(detail); } inPageRequest.putPageValue(prefix, details); }
	 */
	public void fieldSearch(WebPageRequest inPageRequest) throws Exception
	{
		Store store = getStore(inPageRequest);
		store.getProductSearcher().fieldSearch(inPageRequest, getCart(inPageRequest));
		inPageRequest.putPageValue("searcher", store.getProductSearcher());
	}

	public void returnAllProducts(WebPageRequest inReq) throws Exception
	{
		Store store = getStore(inReq);
		String hitsname = inReq.findValue("hitsname");
		if (hitsname == null)
		{
			hitsname = "hits";
		}
		if( inReq.getRequestParameter("page") == null)
		{
			//HitTracker all = store.getProductSearcher().getAllHits();
			//inReq.putSessionValue(hitsname + store.getCatalogId(),  all);
			//inReq.putPageValue(hitsname, all);
			SearchQuery query = store.getProductSearcher().createSearchQuery();
			query.addMatches("id","*");
			store.getProductSearcher().cachedSearch(inReq, query);
		}
		else
		{
			loadPageOfSearch(inReq);
		}
		inReq.putPageValue("searcher", store.getProductSearcher());
	}

	public void searchCatalogs(WebPageRequest inPageRequest) throws Exception
	{
		Store store = getStore(inPageRequest);
		store.getProductSearcher().searchCatalogs(inPageRequest, getCart(inPageRequest));
	}

	
	/**
	 * This should all be moved to the new CatalogModule and into objects
	 * 
	 * @param inPageRequest
	 */
	

	public Cart getCart(WebPageRequest inPageRequest) throws OpenEditException
	{
		Store store = getStore(inPageRequest);
		Cart cart = (Cart) inPageRequest.getSessionValue(store.getCatalogId() + "cart");
		if (cart == null)
		{
			cart = new Cart();
			cart.setStore(store);
			inPageRequest.putSessionValue(store.getCatalogId() + "cart", cart);
		}
		inPageRequest.putPageValue("cart", cart);
		return cart;
	}

	/**
	 * Only searches if the query does not match the page they want
	 * 
	 * @param inPageRequest
	 * @throws Exception
	 */
	public void loadPageOfSearch(WebPageRequest inReq) throws Exception
	{
		Store store = getStore(inReq);
		store.getProductSearcher().loadPageOfSearch(inReq);
	}

	public void changeSort(WebPageRequest inReq) throws Exception
	{
		String sort = inReq.getRequestParameter("sortby");
		HitTracker tracker = loadHits(inReq);
		if (tracker != null)
		{
			SearchQuery group = tracker.getSearchQuery();
			group.setSortBy(sort);
			tracker.setIndexId(tracker.getIndexId() + sort); // Causes the
																// hits to be
																// reloaded
			// inReq.removeSessionValue("hits");
			Store store = getStore(inReq);
			store.getProductSearcher().cachedSearch(inReq, group);
		}
	}

	public HitTracker loadHits(WebPageRequest inReq) throws OpenEditException
	{
		String hitsname = inReq.findValue("hitsname");
		if (hitsname == null)
		{
			hitsname = "hits";
		}
		Store store = getStore(inReq);
		// LuceneHitTracker tracker = (LuceneHitTracker)
		// inReq.getSessionValue(hitsname + store.getCatalogId() );
		//		
		// if( tracker != null)
		// {
		// inReq.putPageValue(hitsname, tracker);
		// }
		return store.getProductSearcher().loadHits(inReq, hitsname);
	}

	

	

	

	

	

	

	public void clearIndex(WebPageRequest inReq) throws OpenEditException{
		getStore(inReq).getProductSearcher().clearIndex();
	}

	

	public void saveSearchQuery(WebPageRequest inReq) {
		HitTracker hits = loadHits(inReq);
		
		String catalogid = inReq.findValue("catalogid");
		if(catalogid == null){
			catalogid = inReq.findValue("applicationid");
		}
		SearchQuery query = hits.getSearchQuery();
		String id=inReq.getRequestParameter("id");
		String name=inReq.getRequestParameter("name");
		String description = inReq.getRequestParameter("description");
		if(id == null){
			id = String.valueOf(new Date().getTime());
		}
		query.setId(id);
		query.setName(name);
		query.setDescription(description);
		if(id != null){
			getSearchQueryArchive().saveQuery(getStore(inReq).getCatalogId(), query, id, inReq.getUser());
		}
		
	}
	
	public void deleteSearchQuery(WebPageRequest inReq) throws Exception {
		String id=inReq.getRequestParameter("id");
		if(id != null){
			String catid=inReq.getRequestParameter("catalogid");
			getSearchQueryArchive().deleteQuery(catid,  id, inReq.getUser());
		}
		
	}
	
	public void runSavedQuery(WebPageRequest inReq) throws Exception{
		
		String id = findId(inReq);
		if(id != null)
		{
			SearchQuery query = getSearchQueryArchive().loadQuery(getStore(inReq).getCatalogId(),"product", id, inReq.getUser());
			HitTracker tracker = getStore(inReq).getProductSearcher().search(query);
			
			String hitsname = inReq.findValue("hitsname");
			if (hitsname == null)
			{
				hitsname = "hits";
			}
			inReq.putSessionValue(hitsname + getStore(inReq).getCatalogId(), tracker);
			inReq.putPageValue(hitsname, tracker);
			
			inReq.putPageValue("searcher", getStore(inReq).getProductSearcher());
			//log.info("ran saved query: " + query);
		}	
		
	}
	
	private String findId(WebPageRequest inReq){
		String id = inReq.getRequestParameter("id");
		if(id == null){
			id = inReq.findValue("id");
		}
		if(id == null){
			String path = inReq.getPath();
			id = path.substring(path.lastIndexOf("/"), path.lastIndexOf("."));			
		}
		return id;
	}
	
	
	public void loadSavedQueryList(WebPageRequest inReq) throws Exception{
		HitTracker hits = new ListHitTracker();
		String catalogid = inReq.findValue("catalogid");
		if(catalogid == null){
			catalogid = inReq.findValue("applicationid");
		}
		List queries =getSearchQueryArchive().loadSavedQueryList(catalogid,"product", inReq.getUser());
		hits.addAll(queries);
		String hitsname = inReq.findValue("hitsname");
		if (hitsname == null)
		{
			hitsname = "hits";
		}
		inReq.putPageValue(hitsname, hits);		
	}
		
}