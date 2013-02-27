
/*
 * Created on Aug 24, 2005
 */

import javax.swing.text.DefaultEditorKit.PreviousWordAction;

import org.openedit.Data
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
	Page upload = archive.getPageManager().getPage("/${mediaarchive.catalogId}/temp/upload/rogersdata.csv");
	Reader reader = upload.getReader();
	try
	{
		//Setup variables
		def SEARCH_FIELD = "rogerssku";
		def UPDATE_FIELD = "as400id";
		def boolean done = false;
		def int badRows = 0;
		def int goodRows = 0;
		def int totalRows = 0;
		
		//Create new CSV Reader Object
		CSVReader read = new CSVReader(reader, ',', '\"');
	
		//Read 1 line for headers
		String[] headers = read.readNext();
		
		int as400idcol = 0;
		int rogersSKUcol = 5;
		
		def List<String> badProductList = new ArrayList<String>();
		def List<Product> productsToSave = new ArrayList<Product>();
		
		//loop over rows
		String[] cols;
		while ((cols = read.readNext()) != null)
		{
			String as400id = cols[as400idcol];
			String rogersSKU = cols[rogersSKUcol];
			
			if (rogersSKU.trim() != "" ) {
				//Search for the product by the RogersSKU
				
				Data product = productsearcher.searchByField(SEARCH_FIELD, rogersSKU);
		        if(product){
					//lookup product with product searcher
					Product real= productsearcher.searchById(product.id);
					if (real)
					{
						if (!real.getProperty("as400id").equals(as400id)) {
							//Set the new value of 
							real.setProperty("as400id", as400id);
							//Add to the ProductsToSave List
							productsToSave.add(real);
						}
						goodRows++;
					} else {
						badProductList.add(rogersSKU);
						badRows++;
					}
			
		        } else {
					badProductList.add(rogersSKU);
					badRows++;
		        }
			} else {
				badProductList.add((String) as400id);
				badRows++;
			}
			
			totalRows++;
		}
		productsearcher.saveAllData(productsToSave, context.getUser());
		context.putPageValue("badlist", badProductList);
		context.putPageValue("totalrows", totalRows);
		context.putPageValue("goodrows", goodRows);
		context.putPageValue("badrows", badRows);
	}
	finally
	{
		FileUtils.safeClose(reader);
	}
	
}
handleSubmission();
