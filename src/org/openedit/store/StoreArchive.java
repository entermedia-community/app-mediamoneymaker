package org.openedit.store;


import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.page.Page;

public interface StoreArchive
{

	public abstract boolean hasChanged(Store inStore) throws OpenEditException;

	public abstract Store loadStore(WebPageRequest inReq) throws StoreException;

	public abstract Store getStore(Page inPage) throws StoreException;

	public abstract Store getStore(String inCatalogId) throws StoreException;

	public abstract Store getStoreForCatalog(String inCatalogId) throws Exception;

}