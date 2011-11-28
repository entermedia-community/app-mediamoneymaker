/*
 * Created on Jun 9, 2005
 */
package org.openedit.store.modules;

import org.openedit.store.Product;
import org.openedit.store.ProductArchive;
import org.openedit.store.RelatedFile;
import org.openedit.store.Store;
import org.openedit.store.StoreTestCase;

import com.openedit.WebPageRequest;

/**
 * @author cburkey
 * 
 */
public class RelatedFilesTest extends StoreTestCase {
	/**
	 * @param inArg0
	 */
	public RelatedFilesTest(String inArg0) {
		super(inArg0);
	}

	public void testRelateProducts() throws Exception {
		RelatedProductsModule module = (RelatedProductsModule) getFixture()
				.getModuleManager().getModule("RelatedProductsModule");

		WebPageRequest context = getFixture().createPageRequest(
				"/store/index.html");
		Store store = module.getStore(context);
		ProductArchive archive = store.getProductArchive();
		
		Product source = new Product();
		source.setName("source");
		source.setSourcePath("source.data");
		source.setId("source");
		
		RelatedFile file = new RelatedFile();
		file.setFilename("/test/document.pdf");
		file.setPrimary(true);

		source.addRelatedFile(file);
		 
		assertEquals("Check that first related file is automatically primary", file, source.getPrimaryRelatedFile());
		
		file = new RelatedFile();
		file.setFilename("/test/bob.pdf");
		source.addRelatedFile(file);
			
		store.saveProduct(source);
		store.clearProducts();
		store.clear();
		
		source = store.getProduct("source");
		assertEquals("load related files",source.getRelatedFiles().size(),2);
		assertEquals("/test/document.pdf", source.getPrimaryRelatedFile().getFilename());
//		SearchQuery query =store.getProductSearcher().createSearchQuery();
//		query.addMatches("id", "source", "testing index");
//		HitTracker hits = store.getProductSearcher().search(query);
//		assertEquals(hits.size(), ollingwood1);
//		Document doc = (Document)hits.get(0);
//		String path = doc.get("primaryrelatedfile");
//		assertEquals("/test/document.pdf", path);
		
		
	}

	
	
	
}
