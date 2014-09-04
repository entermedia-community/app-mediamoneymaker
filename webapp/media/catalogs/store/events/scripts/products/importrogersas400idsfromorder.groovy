package products

/*
 * Created on October 4th, 2012
 * Created by Peter Floyd
 */

//Import List
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.util.CSVReader
import org.openedit.store.Product
import org.openedit.store.orders.Order

import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.page.Page
import com.openedit.util.FileUtils

public class ImportRogersAS400IdsFromOrder extends EnterMediaObject {

	List<String> goodProductList;
	List<String> badProductList;
	int totalRows;
	Map<String, Order> orderMap;

	public List<String> getGoodProductList() {
		if(goodProductList == null) {
			goodProductList = new ArrayList<String>();
		}
		return goodProductList;
	}
	public void addToGoodProductList(String inItem) {
		if(goodProductList == null) {
			goodProductList = new ArrayList<String>();
		}
		goodProductList.add(inItem);
	}

	public List<String> getBadProductList() {
		if(badProductList == null) {
			badProductList = new ArrayList<String>();
		}
		return badProductList;
	}
	public void addToBadProductList(String inItem) {
		if(badProductList == null) {
			badProductList = new ArrayList<String>();
		}
		boolean found = false;
		for (String currValue : badProductList) {
			if (currValue.equalsIgnoreCase(inItem)) {
				found = true;
				break;
			}
		}
		if (!found) {
			badProductList.add(inItem);
		}
	}

	public int getTotalRows() {
		if (totalRows == null) {
			totalRows = 0;
		}
		return totalRows;
	}
	public void increaseTotalRows() {
		if (totalRows == null) {
			totalRows = 0;
		}
		this.totalRows++;
	}

	public void orderImport(){
		//Create Store, MediaArchive Object

		log.info("PROCESS: START Products.ImportRogersAS400IdsFromOrder");
		
		MediaArchive archive = context.getPageValue("mediaarchive");
		String catalogid = getMediaArchive().getCatalogId();
		boolean production = Boolean.parseBoolean(context.findValue('productionmode'));
		
		// Create the Searcher Objects to read values!
		SearcherManager manager = archive.getSearcherManager();

		//Create Searcher Object
		Searcher productsearcher = manager.getSearcher(archive.getCatalogId(), "product");
		///Searcher itemsearcher = manager.getSearcher(archive.getCatalogId(), "rogers_order_item");

		def int columnAS400id = 0;//9;
		def String colHeadAS400ID = "INUMBR";
		def int columnRogersID = 5;//33;
		def String colHeadROGERSID = "IVNDP#";
		def int department = 1;
		def String colHeadISDEPT = "ISDEPT";

		//PropertyDetail detail = itemsearcher.getDetail("quantity");
		//detail.get("column");

		Page upload = archive.getPageManager().getPage("/${catalogid}/temp/upload/rogers_order.csv");
		Reader reader = upload.getReader();
		try
		{
			boolean done = false;
			String errorOut = "";
			boolean errorFields = false;

			//Create CSV reader
			CSVReader read = new CSVReader(reader, ',', '\"');
			
			//Read 1 line of header
			String[] headers = read.readNext();

			def List columnNumbers = new ArrayList();
			columnNumbers.add(columnAS400id);
			columnNumbers.add(columnRogersID);

			def List columnNames = new ArrayList();
			columnNames.add(colHeadAS400ID);
			columnNames.add(colHeadROGERSID);
			columnNames.add(colHeadISDEPT);

			for ( int index=0; index < columnNumbers.size(); index++ ) {
				if ( headers[columnNumbers.get(index)].toString().toUpperCase() != columnNames.get(index).toString().toUpperCase()) {
					errorOut += "<li>" + headers[columnNumbers.get(index)].toString() + " at column " + columnNumbers.get(index).toString() + " is invalid.</li>";
					errorFields = true;
				}
			}
			if (errorFields == true) {
				errorOut = "<p>The following fields in the input file are invalid:<ul>" + errorOut + "</ul></p>";
				context.putPageValue("errorout", errorOut);
			} else {
				Map<String,Product> productmap = new HashMap<String,Product>();
				String[] orderLine;
				while ((orderLine = read.readNext()) != null)
				{
					//Get Product Info
					//Read the oraclesku from the as400 table
					String rogerssku = orderLine[columnRogersID].trim();
					String as400id = orderLine[columnAS400id].trim();
					String isdept = orderLine[department].trim();
					boolean isRogers = (isdept == "74");//rogers: 74, fido: 174
					if(rogerssku){
						Data targetProduct = productsearcher.searchByField("rogerssku", rogerssku.replace("/", "\\/"));
						if (targetProduct) {
							//product may have already been updated
							//load from hashmap
							if (productmap.containsKey(targetProduct.getId()) == false){
								Product p = productsearcher.searchById(targetProduct.getId());
								p.setProperty("rogersas400id", null);
								p.setProperty("fidoas400id", null);
								p.setProperty("fidosas400id", null);
								p.setProperty("as400id", null);
								productmap.put(targetProduct.getId(), p);
							}
							Product product = productmap.get(targetProduct.getId());
							//support both fido and rogers as400 ids
							if (isRogers) {
								product.setProperty("rogersas400id", as400id);
							}
							else {
								product.setProperty("fidoas400id", as400id);
							}
						} else {
							//ID Does not exist!!! Add to badProductIDList
							addToBadProductList(rogerssku);
//							log.info("Product could not be found: " + rogerssku);
						}
					}
					increaseTotalRows();
				}
				
				//go through hasmap, adding to goodproductlist and updating backend
				List edited = new ArrayList();
				Iterator itr = productmap.keySet().iterator();
				while(itr.hasNext()){
					Product product = productmap.get(itr.next());
					addToGoodProductList(product.getId() + " - updated");
					edited.add(product);
					if (edited.size() == 2000){
						productsearcher.saveAllData(edited, null);
						edited.clear();
					}
				}
				productsearcher.saveAllData(edited, null);
			}
		}
		finally
		{
			FileUtils.safeClose(reader);
		}
	}
}

logs = new ScriptLogger();
logs.startCapture();

try {

	ImportRogersAS400IdsFromOrder importAS400Ids = new ImportRogersAS400IdsFromOrder();
	importAS400Ids.setLog(logs);
	importAS400Ids.setContext(context);
	importAS400Ids.setModuleManager(moduleManager);
	importAS400Ids.setPageManager(pageManager);

	//Read the production value
	boolean production = Boolean.parseBoolean(context.findValue('productionmode'));

	importAS400Ids.orderImport();
	log.info("The following file(s) has been created. ");
	context.putPageValue("totalrows", importAS400Ids.getTotalRows());
	if (importAS400Ids.getGoodProductList().size()>0) {
		log.info("Updated Product List ");
		log.info(importAS400Ids.getGoodProductList().toString());
		context.putPageValue("goodproductlist", importAS400Ids.getGoodProductList());
	}
	if (importAS400Ids.getBadProductList().size()>0) {
		log.info("Bad Product List ");
		log.info(importAS400Ids.getBadProductList().toString());
		context.putPageValue("badproductlist", importAS400Ids.getBadProductList());
	}
	log.info("PROCESS: END Products.ImportRogersAS400IdsFromOrder");
}
finally {
	logs.stopCapture();
}
