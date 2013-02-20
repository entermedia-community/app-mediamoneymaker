package products;


import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.store.Product
import org.openedit.store.util.MediaUtilities

import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker

public class ResetValidFlag  extends EnterMediaObject {
	
	public String doReset() {
			
		String strMsg = "";
		
		WebPageRequest inReq = context;
		
		MediaUtilities media = new MediaUtilities();
		media.setContext(context);
		
		MediaArchive archive = inReq.getPageValue("mediaarchive");
		
		// Create the Searcher Objects to read values!
		SearcherManager searcherManager = archive.getSearcherManager();
		Searcher productsearcher = media.getProductSearcher();
		
		HitTracker hits = productsearcher.getAllHits();
		if ( hits.size() > 0 ) {
			for (Iterator products = hits.iterator(); products.hasNext();) {
				Data product = (Data) products.next();
				Product thisProduct = productsearcher.searchById(product.getId());
				thisProduct.setProperty("validitem", "false");
				strMsg += "Product (" + thisProduct.getId() + ") valid item set to false.<BR>\n";
				productsearcher.saveData(thisProduct, inReq.getUser());
			}
			productsearcher.reIndexAll();
		}
		return strMsg;
	}
}
logs = new ScriptLogger();
logs.startCapture();

try {
	log.info("START - ResetValidFlag");
	ResetValidFlag reset = new ResetValidFlag();
	reset.setLog(logs);
	reset.setContext(context);
	reset.setModuleManager(moduleManager);
	reset.setPageManager(pageManager);
	String output = reset.doReset();
	context.putPageValue("export", output);
	log.info("FINISH - ResetValidFlag");
}
finally {
	logs.stopCapture();
}
