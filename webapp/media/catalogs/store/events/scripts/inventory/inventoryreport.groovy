package inventory

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.util.CSVWriter
import org.openedit.repository.filesystem.StringItem
import org.openedit.store.InventoryItem
import org.openedit.store.Product
import org.openedit.store.util.MediaUtilities

import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.GroovyScriptRunner
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker
import com.openedit.page.Page

public class InventoryReport extends EnterMediaObject {

	private ArrayList<Product> badProductList;
	private ArrayList<Product> goodProductList;
	private List<String> headerRow;
	int totalRows;

	/* GET LISTS */
	public ArrayList<Product> getBadProductList() {
		if(badProductList == null) {
			badProductList = new ArrayList<Product>();
		}
		return badProductList;
	}
	public ArrayList<Product> getGoodProductList() {
		if(goodProductList == null) {
			goodProductList = new ArrayList<Product>();
		}
		return goodProductList;
	}

	/* ADD TO LISTS */
	public void addToBadProductList(Product inProduct) {
		if(badProductList == null) {
			badProductList = new ArrayList<Product>();
		}
		badProductList.add(inProduct);
	}
	public void addToGoodProductList(Product inProduct) {
		if(goodProductList == null) {
			goodProductList = new ArrayList<Product>();
		}
		goodProductList.add(inProduct);
	}
	public int getTotalRows() {
		if (totalRows == null) {
			totalRows = 0;
		}
		return totalRows;
	}
	public void increaseTotalRows() {
		this.totalRows++;
	}

	public void doCheck() {
		
		Log log = LogFactory.getLog(GroovyScriptRunner.class);
		WebPageRequest inReq = context;

		String strMsg = "";
		
		MediaArchive archive = context.getPageValue("mediaarchive");
		String catalogid = getMediaArchive().getCatalogId();

		// Create the Searcher Objects to read values!
		SearcherManager searcherManager = archive.getSearcherManager();

		//Get Inventory Level to check
		String inLevel = inReq.getRequestParameter("level");
		Integer level = Integer.parseInt(inLevel);
		
		Searcher productsearcher = searcherManager.getSearcher(catalogid, "product");
		
		String sessionid = inReq.getRequestParameter("hitssessionid");
		if (sessionid == null) {
			return;
		}
		HitTracker productList = inReq.getSessionValue(sessionid);
		if (productList == null) {
			return;
		}
		ArrayList selectedHits = productList.getSelectedHits();
		log.info("Found # of Products:" + selectedHits.size());
		
		for (Iterator hit = selectedHits.iterator(); hit.hasNext();) {
			Data p = hit.next();
			Product product =  productsearcher.searchById(p.getId());
			InventoryItem item = product.getInventoryItem(0);
			if (item.getQuantityInStock() <= level) {
				addToBadProductList(product);
			} else {
				addToGoodProductList(product);
			}
		}
		
		//Create the Header Row
		headerRow = createHeader();
		
		//Create the CSV Writer Objects
		StringWriter output  = new StringWriter();
		CSVWriter writer  = new CSVWriter(output, (char)',');

		//Write the Header Line
		writeRowToWriter(headerRow, writer);
		
		if (getBadProductList().size() > 0) {
			processProductList(getBadProductList(), searcherManager, archive, writer, output, "export-bad-inventory.csv");
			inReq.putPageValue("badlistcsv", "true");
			inReq.putPageValue("badlist", getBadProductList());
		}			
		if (getGoodProductList().size() > 0) {
			processProductList(getGoodProductList(), searcherManager, archive, writer, output, "export-good-inventory.csv");
			inReq.putPageValue("goodlistcsv", "true");
			inReq.putPageValue("goodlist", getGoodProductList());
		}
		inReq.putPageValue("hitssessionid", sessionid);
		inReq.putPageValue("level", inLevel);
	}
	
	private List createHeader() {
		List headerRow = new ArrayList();
		//Add the Order Number
		headerRow.add("Product ID");
		headerRow.add("Product Name");
		headerRow.add("Distributor");
		headerRow.add("Rogers SKU");
		headerRow.add("Manufacturer SKU");
		headerRow.add("UPC Code");
		headerRow.add("Quantity in Stock");
		log.info(headerRow.toString());
		return headerRow
	}
	
	private void processProductList( ArrayList<Product> inProductList, SearcherManager inManager, MediaArchive inArchive, CSVWriter inWriter, StringWriter inOutput, String inFilename) {
		
		for (Product product : inProductList ) {
			ArrayList<String> row = new ArrayList<String>();
			row = createRowItem(product, inManager, inArchive);
			writeRowToWriter(row, inWriter);
		}
		String inventoryFolder = "/WEB-INF/data/" + inArchive.getCatalogId() + "/inventory/";
		Page page = pageManager.getPage(inventoryFolder + inFilename);

		Boolean processCSV = writeOrderToFile(page, inOutput, inFilename);
		if (processCSV) {
			log.info(inventoryFolder + inFilename + " created sucessfully.");
		} else {
			log.info("ERROR: " + inventoryFolder + inFilename + " could not be created.");
		}

	}
	
	private ArrayList createRowItem(Product inProduct, SearcherManager inManager, MediaArchive inArchive) {
		ArrayList<String> row = new ArrayList<String>();
		row.add(inProduct.getId());
		row.add(inProduct.getName());
		String distributorID = inProduct.get("distributor");
		if (distributorID != null) {
			Searcher distributorSearcher = inManager.getSearcher(inArchive.getCatalogId(), "distributor");
			Data distributor = distributorSearcher.searchById(distributorID);
			if (distributor != null) {
				row.add(distributor.getName());
			} else {
				row.add("[BLANK]");
			}
		}
		else {
			row.add("[BLANK]");
		} 
		row.add(checkProductValue(inProduct, "rogerssku"));
		row.add(checkProductValue(inProduct, "manufacturersku"));
		row.add(checkProductValue(inProduct, "upc"));
		InventoryItem item = inProduct.getInventoryItem(0);
		row.add(item.getQuantityInStock().toString());
		
		log.info(row.toString());
		return row;
	}
	
	private String checkProductValue(Product inProd, String inCheck) {
		if (inProd.get(inCheck) != null) {
			return inProd.get(inCheck);
		}
		else {
			return "[BLANK]";
		}
	}
	
	private void writeRowToWriter( List inRow, CSVWriter writer) {
		
		String[] nextrow = new String[inRow.size()];
		for ( int headerCtr=0; headerCtr < inRow.size(); headerCtr++ ){
			nextrow[headerCtr] = inRow.get(headerCtr);
		}
		try	{
			writer.writeNext(nextrow);
		}
		catch (Exception e) {
			log.info(e.message);
			log.info(e.getStackTrace().toString());
		}
	}
	
	private boolean writeOrderToFile(Page page, StringWriter output, String fileName) {
		
		page.setContentItem(new StringItem(page.getPath(), output.toString(), "UTF-8"));

		//Write out the CSV file.
		pageManager.putPage(page);
		if (!page.exists()) {
			String strMsg = "ERROR:" + fileName + " was not created.";
			log.info(strMsg);
			return false;
		} else {
			return true;
		}
	}
}

log = new ScriptLogger();
log.startCapture();

try {

	log.info("START - InventoryReport");
	InventoryReport inventoryReport = new InventoryReport();
	inventoryReport.setLog(log);
	inventoryReport.setContext(context);
	inventoryReport.setModuleManager(moduleManager);
	inventoryReport.setPageManager(pageManager);
	inventoryReport.doCheck();
	log.info("FINISH - InventoryReport");
}
finally {
	log.stopCapture();
}
