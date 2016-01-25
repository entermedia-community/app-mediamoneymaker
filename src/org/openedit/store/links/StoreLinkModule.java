package org.openedit.store.links;

import java.util.HashMap;
import java.util.Map;

import org.entermediadb.links.LinkModule;
import org.entermediadb.links.XmlLinkLoader;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.page.Page;
import org.openedit.store.Store;
import org.openedit.store.StoreArchive;

public class StoreLinkModule extends LinkModule
{
	Map storeLoader = new HashMap();
	
	public XmlLinkLoader getLinkLoader(WebPageRequest inReq) throws OpenEditException
	{
		//String linkLoader = getLoaderName(inReq);
		Store store = getStore(inReq);
		XmlCatalogLinkLoader loader = (XmlCatalogLinkLoader)storeLoader.get(store.getCatalogId());
		if( loader == null)
		{
			loader = (XmlCatalogLinkLoader)getModuleManager().getBean("CatalogLinkLoader");
			loader.setStore(store);
			storeLoader.put( store.getCatalogId(), loader);
		}
		return loader;
	}
	
	public Store getStore(WebPageRequest inPageRequest) throws OpenEditException
	{
		Page page = inPageRequest.getContentPage();
		String readername = null;
		if( page != null)
		{
			readername = page.get("storereadername");
		}
		if( readername == null)
		{
			readername = "storeArchive";
		}
		StoreArchive reader = (StoreArchive)getModuleManager().getBean(readername);
		Store store = reader.loadStore(inPageRequest);
		return store;		
	}

}
