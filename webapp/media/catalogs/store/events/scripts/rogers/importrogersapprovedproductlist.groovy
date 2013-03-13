package rogers

/*
 * Created on October 4th, 2012
 * Created by Peter Floyd
 */

//Import List
import org.apache.lucene.search.TotalHitCountCollector;
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.util.CSVReader
import org.openedit.store.Product
import org.openedit.store.util.MediaUtilities

import com.openedit.BaseWebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.page.Page

public class ImportRogersApprovedProductList extends EnterMediaObject {
	
	ArrayList<String> goodProductList;
	ArrayList<String> badProductList;
	int totalrows;

	public void addToGoodProductList( String inValue ) {
		if (goodProductList == null) {
			goodProductList = new ArrayList<String>();
		}
		goodProductList.add(inValue);
	}

	public void addToBadProductList( String inValue ) {
		if (badProductList == null) {
			badProductList = new ArrayList<String>();
		}
		badProductList.add(inValue);
	}
	public void addToTotal() {
		if (totalrows == null) {
			totalrows = 0;
		}
		totalrows++;
	}
	
	public ArrayList<String> getGoodProductList() {
		return this.goodProductList;
	}
	public ArrayList<String> getBadProductList() {
		return this.badProductList;
	}
	public int getTotal() {
		if (totalrows == null) {
			totalrows = 0;
		}
		return totalrows;
	}

	public void productImport() 
	{
		//Create Store, MediaArchive Object
		
		BaseWebPageRequest inReq = context;
		
		MediaUtilities media = new MediaUtilities();
		media.setContext(context);

		MediaArchive archive = media.getArchive();
		String catalogid = media.getCatalogid();
		Searcher productsearcher = media.getProductSearcher();

		//Define columns from spreadsheet
		def int columnProductID = 0;
		
		Page upload = archive.getPageManager().getPage("/${catalogid}/temp/upload/rogers_products.csv");
		Reader reader = upload.getReader();
		//Create CSV reader
		CSVReader read = new CSVReader(reader, ',', '\"');

		//Read 1 line of header
		String[] headers = read.readNext();
		
		String[] storeLine;
		while ((storeLine = read.readNext()) != null)
		{
			String productID = storeLine[columnProductID].trim();

			Data hit = productsearcher.searchById(productID);
			if (hit != null) {
				Product product = productsearcher.searchById(productID);
				if (product != null) {
					product.setProperty("approved", "true");
					productsearcher.saveData(product, context.getUser());
					addToGoodProductList(productID);
				} else {
					String strMsg = "Could not load product: " + productID;
					addToBadProductList(strMsg);
				}
			} else {
				String strMsg = "Could not load product: " + productID;
				addToBadProductList(strMsg);
			}
			addToTotal();
		}
	}
}

logs = new ScriptLogger();
logs.startCapture();
try {
	ImportRogersApprovedProductList importProducts = new ImportRogersApprovedProductList();
	importProducts.setLog(logs);
	importProducts.setContext(context);
	importProducts.setModuleManager(moduleManager);
	importProducts.setPageManager(pageManager);
	importProducts.productImport();
	context.putPageValue("goodlist", importProducts.getGoodProductList());
	context.putPageValue("badlist", importProducts.getBadProductList());
	context.putPageValue("totalrows", importProducts.getTotal().toString());
}
finally {
	logs.stopCapture();
}
