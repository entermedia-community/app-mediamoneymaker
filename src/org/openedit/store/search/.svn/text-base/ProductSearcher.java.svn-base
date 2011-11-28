package org.openedit.store.search;

import java.util.List;

import org.openedit.data.Searcher;
import org.openedit.store.Cart;
import org.openedit.store.Product;
import org.openedit.store.ProductArchive;
import org.openedit.store.ProductPathFinder;
import org.openedit.store.Store;
import org.openedit.store.StoreException;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;

public interface ProductSearcher extends Searcher, ProductPathFinder
{
	public abstract void fieldSearch(WebPageRequest inPageRequest, Cart cart) throws OpenEditException;

	public HitTracker search(SearchQuery inQuery);

	public abstract void searchCatalogs(WebPageRequest inPageRequest, Cart cart) throws Exception;

	public abstract void searchExactCatalogs(WebPageRequest inPageRequest, Cart cart) throws Exception;

	public abstract HitTracker searchStore(WebPageRequest inPageRequest, Cart cart) throws Exception;

	public abstract void updateIndex(Product inProduct) throws StoreException;

	public abstract void updateIndex(List inProducts, boolean inOptimize) throws StoreException;

	public abstract ProductArchive getProductArchive();

	public abstract Store getStore();

	public abstract void setStore(Store inStore);

	public abstract void deleteFromIndex(Product inProduct) throws StoreException;

	public abstract void deleteFromIndex(String inId) throws StoreException;

	public abstract void deleteFromIndex(HitTracker inOld);

	public abstract HitTracker getAllHits();

	public abstract void flush();

	//public abstract Product getProduct(String inId);
	
	public String createFilter(WebPageRequest inPageRequest, boolean selected);



}