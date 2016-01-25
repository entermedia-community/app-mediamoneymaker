/*
 * Created on Jul 19, 2006
 */
package org.openedit.store.modules;

import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.store.Cart;
import org.openedit.store.Product;
import org.openedit.store.Store;
import org.openedit.store.StoreArchive;
import org.openedit.util.PathUtilities;

public class BaseStoreModule extends BaseMediaModule
{
	protected static final String CATALOGIDX = "catalogid";
	protected static final String CATEGORYID = "categoryid";

	
	
	
	public Store getStore(String inCatalogid) throws OpenEditException{
		String	readername = "storeArchive";
		StoreArchive reader = (StoreArchive)getModuleManager().getBean(readername);
		Store store = reader.getStore(inCatalogid);
		return store;
	}
	

	public Store getStore(WebPageRequest inPageRequest) throws OpenEditException
	{
		String	readername = "storeArchive";
		StoreArchive reader = (StoreArchive)getModuleManager().getBean(readername);
		Store store = reader.loadStore(inPageRequest);
		inPageRequest.putPageValue("cataloghome", "/" + store.getCatalogId());
		String storeapp = inPageRequest.findValue("cartid");
		inPageRequest.putPageValue("carthome", "/" + storeapp);

		return store;		
	}
//	public ProductSearcher getProductSearcher(String inCatalogId)
//	{
//		return (ProductSearcher)getSearcherManager().getSearcher(inCatalogId, "product");
//	}
//	public OrderSearcher getOrderSearcher(String inCatalogId)
//	{
//		return (OrderSearcher)getSearcherManager().getSearcher(inCatalogId, "order");
//	}
	public Product getProduct(WebPageRequest inReq)
	{
		String productid = inReq.getRequestParameter("productid");
		String sourcePath = inReq.getRequestParameter("sourcepath");
		
		if (productid == null) {
			productid = PathUtilities.extractPageName(inReq.getContentPage()
					.getPath());
		}
		Store store = getStore(inReq);
		
		Product product = null;
		
		if (sourcePath != null)
		{
			product = store.getProductBySourcePath(sourcePath);
		}
		if( productid != null)
		{
			if( productid.startsWith("multiedit:") )
			{
				Data data = (Data)inReq.getSessionValue(productid);
				inReq.putPageValue("product", data);
				inReq.putPageValue("data", data);
				return null;
			}

		}
		if (product == null)
		{
			product = store.getProductBySourcePath(inReq.getContentPage());
		}
		
		if (product == null)
		{
			if (productid != null)
			{
				product = store.getProduct(productid);
			}
		}
		
		
		
		return product;
	}
	public Cart getCart(WebPageRequest inPageRequest) throws OpenEditException
	{
		Store store = getStore(inPageRequest);
		Cart cart = (Cart) inPageRequest.getSessionValue(store.getCatalogId()
				+ "cart");
		if (cart == null)
		{
			cart = new Cart();
			cart.setStore(store);
			inPageRequest.putSessionValue(store.getCatalogId() + "cart", cart);
		}
		inPageRequest.putPageValue("cart", cart);
		return cart;
	}
}
