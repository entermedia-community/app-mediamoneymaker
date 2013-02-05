package products;

import java.io.File;
import java.io.FileReader;
import java.io.StringWriter;

import org.openedit.data.Searcher
import org.openedit.money.Money
import org.openedit.store.InventoryItem
import org.openedit.store.Price
import org.openedit.store.PriceSupport
import org.openedit.store.Product
import org.openedit.store.Store
import org.openedit.store.StoreException;

import com.openedit.hittracker.HitTracker
import com.openedit.util.FileUtils;
import com.openedit.util.OutputFiller;

public void doProcess() {

	
	Store store = context.getPageValue("store");

	Searcher productsearcher = store.getProductSearcher();
	HitTracker products = productsearcher.getAllHits();
	products.each{
		Product product = store.getProduct(it.id);
		String htmlPath = product.getSourcePath() + ".html";
		// Low level reading in of text
		
		// add a bunch of stuff to the full text field
		File descriptionFile = new File(store.getRootDirectory(), "/"
				+store.getCatalogId() + "/products/" + htmlPath);
		if (descriptionFile.exists() || descriptionFile.length() > 0) {
			FileReader descread = null;
			try {
				descread = new FileReader(descriptionFile);
				StringWriter out = new StringWriter();
				new OutputFiller().fill(descread, out);
				String description = out.toString();
				product.setProperty("description", description);
				productsearcher.saveData(product, null);
			} catch (Exception ex) {
				throw new StoreException(ex);
			} finally {
				FileUtils.safeClose(descread);
			}
		}
	}
	
	

	
	
	
}

doProcess();
