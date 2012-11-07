
/*
 * Created on Aug 24, 2005
 */

import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.util.CSVReader
import org.openedit.store.Product
import org.openedit.store.Store

import com.openedit.page.Page
import com.openedit.util.FileUtils


public void handleSubmission(){

	//Create the MediaArchive object
	Store store = context.getPageValue("store");
	MediaArchive archive = context.getPageValue("mediaarchive");

	//Create the productsearcher object.
	Searcher productsearcher = store.getProductSearcher();

	//Get the Uploaded Page
	Page upload = archive.getPageManager().getPage("/${mediaarchive.catalogId}/temp/upload/phonecomp.csv");
	Reader reader = upload.getReader();
	try
	{

		//Create new CSV Reader Object
		CSVReader read = new CSVReader(reader, ',', '\"');

		//Read 1 line for headers
		String[] headers = read.readNext();
		HashMap products = new HashMap();
		String[] cols;
		while ((cols = read.readNext()) != null)
		{
			String productid = cols[1];
			String phoneid = cols[2];
			
			Product product = products.get(productid);
			if(product == null){
				 product = productsearcher.searchById(productid);
				if(product != null){
					products.put(product.getId(), product);
				}
			}
			if(product){
				List currentphones = product.getValues("phone");
				if(currentphones == null){
					currentphones  = new ArrayList();
				}
				if(!currentphones.contains(phoneid)){
				
						currentphones = new ArrayList(currentphones);
				
					currentphones.add(phoneid);
					product.setValues("phone", currentphones);
					
				}
				

			}
		}
		productsearcher.saveAllData(products.values(), context.getUser());
	}
	finally
	{
		FileUtils.safeClose(reader);
	}

}
handleSubmission();
