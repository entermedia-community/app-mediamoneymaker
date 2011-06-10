package org.openedit.store.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.openedit.store.Product;
import org.openedit.store.Store;
import org.openedit.store.products.RelatedProduct;
import org.openedit.store.products.RelatedProductsTracker;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.CompositeHitTracker;

public class RelatedProductsModule extends BaseStoreModule {
	
	
	
	
	public void addRelatedProduct( WebPageRequest inRequest ) throws OpenEditException
	{
		Store store = getStore(inRequest);
		
		String productid = inRequest.getRequestParameter("productid");
		String[] relatedproductid = inRequest.getRequestParameters( "relatedtoproductid" );
		if( relatedproductid == null)
		{
			//someone created a new product
			relatedproductid = inRequest.getRequestParameters( "newproductid" );
		}
		
		if(relatedproductid == null){
			return;
		}
		String type = inRequest.getRequestParameter( "type" );

		boolean redirect = Boolean.parseBoolean(inRequest.findValue("redirect"));
		String parentrelationship = inRequest.findValue("parentrelationship");
		
		Product source = getStore(inRequest).getProduct(productid);
		if(  source == null)
		{
			throw new OpenEditException("Source is null");
		}
		String[] catalogs = inRequest.getRequestParameters("relatedtocatalogid");
		String catalogid = store.getCatalogId();
		for (int i = 0; i < relatedproductid.length; i++)
		{
			if( catalogs != null)
			{
				catalogid = catalogs[i];
			}
			Store savestore = getStore(catalogid);
//			if( relatedproductid[i].equals(productid))
//			{
//				continue;
//			}
			Product target = savestore.getProduct(relatedproductid[i]);
			target.setProperty("datatype", type);

			createRelationship(savestore, type, parentrelationship, source, target);
		}
		if(redirect){
			String path = source.getSourcePath(); //TODO: Should go back to related product list
			inRequest.redirect(store.getStoreHome() + "/products/" +path + ".html");		
		}
	}

	private void createRelationship(Store store, String type, String parentrelationship, Product source, Product target)
	{
		if(  target == null)
		{
			throw new OpenEditException("target is null");
		}
		RelatedProduct related = new RelatedProduct();
		related.setProductId(source.getId());
		related.setRelatedToProductId(target.getId());
		related.setRelatedToCatalogId(target.getCatalogId());
		related.setType(type);				
		source.addRelatedProduct(related );
		store.saveProduct( source );
		
		if(parentrelationship != null)
		{
			RelatedProduct back = new RelatedProduct();
			back.setProductId(target.getId());
			back.setRelatedToProductId(source.getId());
			back.setRelatedToCatalogId(source.getCatalogId());
			
			back.setType(parentrelationship);
			target.addRelatedProduct( back );
			store.saveProduct( target );
		}
	}
	
	public void loadRelatedProducts( WebPageRequest inRequest ) throws OpenEditException
	{

		Product product = getProduct(inRequest);
		RelatedProductsTracker tracker = new RelatedProductsTracker();

			tracker.addAll(product.getRelatedProducts());

		inRequest.putPageValue("relatedhits", tracker);
	}
	
	/*
	 * This will load a tracker of all the products that happen to be related to this one, even transitively. 
	 */
	public RelatedProductsTracker loadAllRelatedProducts( WebPageRequest inRequest ) throws OpenEditException
	{
		RelatedProductsTracker list = (RelatedProductsTracker)inRequest.getPageValue("relatedhits");
		if( list != null )
		{
			return list;
		}
		Store store = getStore(inRequest);
		Product product = getProduct(inRequest);
		if( product == 	null)
		{
			return null;
		}
		List all = new ArrayList();
		all.addAll(product.getRelatedProducts()); //initial list
		HashSet targets = new HashSet();
		targets.add(product.getCatalogId()+":::"+product.getId());
		for (Iterator iterator = all.iterator(); iterator.hasNext();)
		{
			RelatedProduct rp = (RelatedProduct) iterator.next();
			targets.add(rp.getRelatedToCatalogId()+":::"+rp.getRelatedToProductId());
		}
		
		for (int i = 0; i < all.size(); i++)  //iterate through all. new ones added to the end of the list during iteration will also be checked.
		{
			RelatedProduct relatedProduct = (RelatedProduct) all.get(i);
			String catalogid = relatedProduct.getRelatedToCatalogId();
			if(catalogid == null)
			{
				catalogid = store.getCatalogId();
			}
			Store targetstore = getStore(catalogid);
			Product target = targetstore.getProduct( relatedProduct.getRelatedToProductId());
			if(target == null)
			{
				product.removeRelatedProduct(catalogid, relatedProduct.getRelatedToProductId());
				store.saveProduct(product);
				continue;
			}
			//Go one level deeper
			Collection newOnes = target.getRelatedProducts(); //for each item in the list, add all their relatives
			for (Iterator iterator = newOnes.iterator(); iterator.hasNext();) {
				RelatedProduct newRelated = (RelatedProduct) iterator.next();
				if (!all.contains(newRelated) && !targets.contains(newRelated.getRelatedToCatalogId()+":::"+newRelated.getRelatedToProductId()))
				{
					targets.add(newRelated.getRelatedToCatalogId()+":::"+newRelated.getRelatedToProductId());
					all.add(newRelated); //put this new one at the end of the list. we will check check its relations eventually
				}
			}
		}

		RelatedProductsTracker tracker = new RelatedProductsTracker();
		tracker.addAll(all);
		inRequest.putPageValue("relatedhits", tracker);

		return tracker;
	}
	
	public CompositeHitTracker getAllTracker(WebPageRequest inReq) throws Exception
	{
		Store store = getStore(inReq);
		Product product = getProduct(inReq);
		
		CompositeHitTracker composite = new CompositeHitTracker();
		composite.ensureHasSubTracker(inReq, store.getCatalogId());
		composite.addToSubTracker(store.getCatalogId(), product);
		
		RelatedProductsTracker relatedtracker = loadAllRelatedProducts(inReq);
		
		for (Iterator iterator = relatedtracker.iterator(); iterator.hasNext();)
		{
			RelatedProduct item = (RelatedProduct) iterator.next();
			String catalogid = item.getRelatedToCatalogId();
			Store targetstore = getStore(catalogid);
			Product target = targetstore.getProduct( item.getRelatedToProductId());
			composite.ensureHasSubTracker(inReq, catalogid);
			composite.addToSubTracker(catalogid, target);
		}
		
		inReq.putPageValue("allrelatedhits", composite);
		
		composite.setHitsName("allrelatedhits");
		composite.setCatalogId(store.getCatalogId());
		inReq.putPageValue(composite.getHitsName(), composite);
		inReq.putSessionValue(composite.getSessionId(), composite);
		return composite;
	}
	
	public void removeRelatedProduct(WebPageRequest inRequest) throws OpenEditException
	{
		String sourceid = inRequest.getRequestParameter("productid");
	
		String targetid = inRequest.getRequestParameter("targetid");
		String catalogid = inRequest.getRequestParameter("targetcatalogid");
		Store sourcestore = getStore(inRequest);
		if(catalogid == null)
		{
			catalogid = sourcestore.getCatalogId();
		}
		Store targetStore = getStore(catalogid);
		Product target = targetStore.getProduct(targetid);
		target.removeRelatedProduct(sourcestore.getCatalogId(), sourceid);
		targetStore.saveProduct(target);
		
		Product source = sourcestore.getProduct(sourceid);
		source.removeRelatedProduct(targetStore.getCatalogId(),targetid);
		sourcestore.saveProduct(source);
		
		inRequest.removePageValue("relatedhits");
		loadAllRelatedProducts(inRequest);
	}
//	
//	private Product loadProduct(WebPageRequest inRequest) {
//		String sourcepath = inRequest.findValue("sourcepath");		
//		return getStore(inRequest).getProductBySourcePath(sourcepath);
//		
//	}
//
//	public void removeRelatedProduct( WebPageRequest inRequest ) throws OpenEditException
//	{
//		StoreEditor editor = getStoreEditor( inRequest );
//		Product product = editor.getCurrentProduct();
//		String[] productIds = inRequest.getRequestParameters( "relatedid" );
//		for ( int i = 0; i < productIds.length; i++ )
//		{
//			product.removeRelatedProduct( productIds[i] );
//		}
//		editor.saveProduct( product );
//	}
//	
//	public void updateRelatedProductIds( WebPageRequest inRequest ) throws OpenEditException
//	{
//		StoreEditor editor = getStoreEditor( inRequest );
//		Product product = editor.getCurrentProduct();
//		
//		String[] productIds = inRequest.getRequestParameters( "productid" );
//		for ( int i = 0; i < productIds.length; i++ )
//		{
//			String add = inRequest.getRequestParameter( productIds[i] + ".value" );
//			if(add != null)
//			{
//			     product.addRelatedProductId( productIds[i] );
//			}
//			else 
//			{
//				product.removeRelatedProductId(productIds[i]);	
//			}
//		}
//		editor.saveProduct( product );
//	}
//	
//	public void relateProductsInCategory( WebPageRequest inRequest ) throws OpenEditException
//	{
//		StoreEditor editor = getStoreEditor( inRequest );
//		Product product = editor.getCurrentProduct();
//		String catalogid = inRequest.getRequestParameter("categoryid");
//		
//		Store store = getStore(inRequest);
//		if(catalogid == null){
//			return;
//		}
//		Category cat = store.getCategory(catalogid);
//		if(cat != null)
//		{
//			List productList = store.getProductsInCatalog(cat);
//			for (Iterator iter = productList.iterator(); iter.hasNext();) 
//			{
//				Product current = (Product) iter.next();
//				current.addRelatedProductId( product.getId() );
//				editor.saveProduct( current );
//
//			}
//		}
//	
//	}
//	
//	public void unrelateProductsInCategory( WebPageRequest inRequest ) throws OpenEditException
//	{
//		StoreEditor editor = getStoreEditor( inRequest );
//		Product product = editor.getCurrentProduct();
//		String catalogid = inRequest.getRequestParameter("categoryid");
//		Store store = getStore(inRequest);
//		if(catalogid == null)
//		{
//			return;
//		}
//		Category cat = store.getCatalog(catalogid);
//		if(cat != null)
//		{
//			List productList = store.getProductsInCatalog(cat);
//			for (Iterator iter = productList.iterator(); iter.hasNext();)
//			{
//				Product current = (Product) iter.next();
//			    current.removeRelatedProductId( product.getId() );
//				editor.saveProduct( current );		
//			}	
//		}
//	}
	
	
	
}
