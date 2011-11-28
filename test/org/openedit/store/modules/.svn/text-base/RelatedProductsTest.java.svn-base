/*
 * Created on Jun 9, 2005
 */
package org.openedit.store.modules;

import org.openedit.store.Product;
import org.openedit.store.Store;
import org.openedit.store.StoreTestCase;
import org.openedit.store.products.RelatedProduct;

import com.openedit.WebPageRequest;

/**
 * @author cburkey
 * 
 */
public class RelatedProductsTest extends StoreTestCase
{
	/**
	 * @param inArg0
	 */
	public RelatedProductsTest(String inArg0)
	{
		super(inArg0);
	}

	public void testRelateProducts() throws Exception
	{
		RelatedProductsModule module = (RelatedProductsModule) getFixture().getModuleManager().getModule("RelatedProductsModule");

		WebPageRequest context = getFixture().createPageRequest("/store/index.html");
		Store store = module.getStore(context);
		Product source = new Product();
		source.setName("source");
		source.setSourcePath("source.data");
		source.setId("source");
		store.saveProduct(source);

		Product target = new Product();
		target.setName("target");
		target.setSourcePath("target.data");
		target.setId("target");
		store.saveProduct(target);

		context.setRequestParameter("productid", "source");
		context.setRequestParameter("relatedtoproductid", "target");
		context.setRequestParameter("type", "link");

		module.addRelatedProduct(context);
		source = null;
		source = store.getProductBySourcePath("source.data");
		assertTrue(source.getRelatedProducts().size() > 0);
		assertTrue(source.isRelated("target"));
		assertEquals("link", ((RelatedProduct) source.getRelatedProducts().iterator().next()).getType());
		getStore().getProductArchive().deleteProduct(target);
		getStore().getProductArchive().deleteProduct(source);

	}

	public void testCopyAndRelate() throws Exception
	{
		RelatedProductsModule module = (RelatedProductsModule) getFixture().getModuleManager().getModule("RelatedProductsModule");
		CatalogEditModule catalogmodule = (CatalogEditModule) getFixture().getModuleManager().getModule("CatalogEditModule");
		WebPageRequest context = getFixture().createPageRequest("/store/index.html");
		Store store = module.getStore(context);
		Product source = new Product();
		source.setName("source");
		source.setSourcePath("source.data");
		source.setId("source");
		store.saveProduct(source);
		context.setRequestParameter("name", "sourcecopy");
		context.setRequestParameter("productid", "source");
		context.setRequestParameter("relatedtoproductid", "1");
		context.setRequestParameter("type", "link");

		
		module.addRelatedProduct(context);

		source = null;
		source = store.getProduct("source");
		assertTrue(source.getRelatedProducts().size() > 0);
		RelatedProduct p = (RelatedProduct) source.getRelatedProducts().iterator().next();
		Product newproduct = store.getProduct(p.getRelatedToProductId());
		assertNotNull(newproduct);

		assertEquals("link", p.getType());

		getStore().getProductArchive().deleteProduct(source);

	}

	public void testMutliCatalog() throws Exception
	{

		RelatedProductsModule module = (RelatedProductsModule) getFixture().getModuleManager().getModule("RelatedProductsModule");
		CatalogEditModule catalogmodule = (CatalogEditModule) getFixture().getModuleManager().getModule("CatalogEditModule");
		WebPageRequest context = getFixture().createPageRequest("/store/index.html");
		Store store = module.getStore(context);
		Product source = new Product();
		source.setName("source");
		source.setSourcePath("source.data");
		source.setId("source");
		store.saveProduct(source);
		context.setRequestParameter("name", "sourcecopy");

		context.setRequestParameter("productid", "source");
		context.setRequestParameter("relatedtoproductid", new String[] { "1" });
		context.setRequestParameter("relatedtocatalogid", new String[] { "store" });
		context.setRequestParameter("type", "state");

		//catalogmodule.copyProduct(context);
		
		
		//Product target = getStore().getProduct("target");
		//assertNotNull(target);
		
		module.addRelatedProduct(context);

		source = null;
		source = store.getProduct("source");
		assertTrue(source.getRelatedProducts().size() > 0);
		RelatedProduct p = (RelatedProduct) source.getRelatedProducts().iterator().next();
		Product newproduct = store.getProduct(p.getRelatedToProductId());
		assertNotNull(newproduct);

		assertEquals("state", p.getType());

		getStore().getProductArchive().deleteProduct(source);

	}

}
