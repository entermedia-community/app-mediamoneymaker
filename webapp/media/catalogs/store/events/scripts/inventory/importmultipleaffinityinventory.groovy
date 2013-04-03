package inventory

/*
 * Created on Aug 24, 2005
 */

import org.entermedia.email.PostMail
import org.entermedia.email.TemplateWebEmail
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.PublishResult;
import org.openedit.entermedia.util.CSVReader
import org.openedit.store.InventoryItem
import org.openedit.store.Product
import org.openedit.store.Store

import com.openedit.OpenEditException
import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker
import com.openedit.page.Page
import com.openedit.page.manage.PageManager
import com.openedit.util.FileUtils


public class ImportMultipleAffinityInventory extends EnterMediaObject {
	
	List<String> badProductList;
	List<String> badUPCList;
	List<String> goodProductList;
	int totalRows;

	public List<String> getBadProductList() {
		if(badProductList == null) {
			badProductList = new ArrayList<String>();
		}
		return badProductList;
	}

	public List<String> getBadUPCList() {
		if(badUPCList == null) {
			badUPCList = new ArrayList<String>();
		}
		return badUPCList;
	}

	public List<String> getGoodProductList() {
		if(goodProductList == null) {
			goodProductList = new ArrayList<String>();
		}
		return goodProductList;
	}
	public void addToBadProductList(String inItem) {
		if(badProductList == null) {
			badProductList = new ArrayList<String>();
		}
		badProductList.add(inItem);
	}
	public void addToBadUPCList(String inItem) {
		if(badUPCList == null) {
			badUPCList = new ArrayList<String>();
		}
		badUPCList.add(inItem);
	}
	public void addToGoodProductList(String inItem) {
		if(goodProductList == null) {
			goodProductList = new ArrayList<String>();
		}
		goodProductList.add(inItem);
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

	public void doImport(){
		
		//Create the MediaArchive object
		Store store = context.getPageValue("store");
		WebPageRequest inReq = context;
		MediaArchive archive = inReq.getPageValue("mediaarchive");
		String catalogID = archive.getCatalogId();
		
		String inDistributor = "Affinity";
	
		//Create the searcher objects.	 
		Searcher productsearcher = store.getProductSearcher();
		Searcher userprofilesearcher = archive.getSearcher("userprofile");
		
		//Get the Uploaded Page
		String filename = "affinityinventory.csv";
		String inventoryFolder = "/WEB-INF/data/${catalogID}/incoming/inventory/";
		
		PageManager pageManager = archive.getPageManager();
		
		List dirList = pageManager.getChildrenPaths(inventoryFolder);
		log.info("Initial directory size: " + dirList.size().toString());

		def int iterCounter = 0;
		for (Iterator iterator = dirList.iterator(); iterator.hasNext();) {
			Page csvFile = pageManager.getPage(iterator.next());
			log.info("Processing " + csvFile.getName());
			if (csvFile != null) {
				if (isCSVFile(csvFile)) {					
					Reader reader = csvFile.getReader();
					try
					{
						//Setup variables
						def ROGERS_SEARCH_FIELD = "rogerssku";
						def MANUFACTURER_SEARCH_FIELD = "manufacturersku";
						def UPC_SEARCH_FIELD = "upc";
						def boolean done = false;
						
						//Create new CSV Reader Object
						CSVReader read = new CSVReader(reader, ',', '\"');
					
						//Read 1 line for headers
						String[] headers = read.readNext();
						
						int affinityID = 0;
						int rogersSKUcol = 1;
						int upc = 2;
						int quantity = 3;
						int description = 4;
						
						//loop over rows
						String[] cols;
						while ((cols = read.readNext()) != null)
						{
							String manufacturerSKU = cols[affinityID].trim();
							String rogersSKU = cols[rogersSKUcol].trim();
							String upcNumber = cols[upc].trim();
							String newQuantity = cols[quantity].trim();
							int qtyInStock = 0;
							if (newQuantity.equals("0") || newQuantity == null) {
								qtyInStock = 0 
							} else {
								qtyInStock = Integer.parseInt(newQuantity);
							}
							Data productHit = null;
							if (rogersSKU.trim() != "" ) {
								//Search for the product by the MANUFACTURER_SEARCH_FIELD
								productHit = productsearcher.searchByField(MANUFACTURER_SEARCH_FIELD, manufacturerSKU);
						        if (productHit == null) {
									//Search for the product by the ROGERS_SEARCH_FIELD
									productHit = productsearcher.searchByField(ROGERS_SEARCH_FIELD, rogersSKU);
									if (productHit == null) {
										//Search for the product by UPC
										productHit = productsearcher.searchByField(UPC_SEARCH_FIELD, upcNumber);
										if (productHit == null) {
											addToBadProductList(rogersSKU);
										} else {
											addToBadUPCList(upcNumber);
										}
										productHit = null;
									}
						        }
							}
							if (productHit) {
									//lookup product with product searcher
								Product product = productsearcher.searchById(productHit.id);
								if (product) {
									InventoryItem productInventory = null
									productInventory = product.getInventoryItem(0);
									if (productInventory == null) {
										//Need to create the Inventory Item
										productInventory = new InventoryItem();
										productInventory.setQuantityInStock(qtyInStock)
										product.addInventoryItem(productInventory);
									} else {
										if (productInventory.getQuantityInStock() != qtyInStock) {
											String msg = "Product(" + product.getName() + ") inventory changed: ";
											msg += productInventory.getQuantityInStock().toString() + ":"
											msg += qtyInStock.toString();
											log.info(msg);
											productInventory.setQuantityInStock(qtyInStock);
											productsearcher.saveData(product, context.getUser());
										} else {
											String msg = "Product(" + product.getName() + ") no inventory changes.";
											log.info(msg);
										}
									}
									addToGoodProductList(rogersSKU);
							
								} else {
									throw new OpenEditException("Could not open product!");
								}
							}
							increaseTotalRows();
						}
						
						boolean move = movePageToProcessed(pageManager, csvFile, catalogID, true);
						if (move) {
							String inMsg = "Inventory File (" + csvFile.getName() + ") moved to processed.";
							log.info(inMsg);
							inMsg = "";
						} else {
							String inMsg = "Inventory File (" + csvFile.getName() + ") failed to move to processed.";
							log.info(inMsg);
						}
					}
					finally
					{
						FileUtils.safeClose(reader);
					}
				} else {
					log.info("Other extension found. Skipping");
				}
			}
		}
		context.putPageValue("totalrows", getTotalRows());
		context.putPageValue("goodproductlist", getGoodProductList());
		context.putPageValue("badproductlist", getBadProductList());
		context.putPageValue("badupclist", getBadUPCList());
		context.putPageValue("distributor", inDistributor);
		
		ArrayList emaillist = new ArrayList();
		HitTracker results = userprofilesearcher.fieldSearch("ticketadmin", "true");
		for(Iterator detail = results.iterator(); detail.hasNext();) {
			Data userInfo = (Data)detail.next();
			emaillist.add(userInfo.get("email"));
		}
		String templatePage = "/ecommerce/views/modules/product/workflow/inventory-notification.html";
		//sendEmail(context, emaillist, templatePage);
		
	}
	protected void sendEmail(WebPageRequest context, List email, String templatePage){
		Page template = pageManager.getPage(templatePage);
		WebPageRequest newcontext = context.copy(template);
		TemplateWebEmail mailer = getMail();
		mailer.setFrom("info@wirelessarea.ca");
		mailer.loadSettings(newcontext);
		mailer.setMailTemplatePath(templatePage);
		mailer.setRecipientsFromCommas(email);
		mailer.setSubject("Support Ticket Update");
		mailer.send();
	}
	
	protected TemplateWebEmail getMail() {
		WebPageRequest inReq = context;
		MediaArchive archive = inReq.getPageValue("mediaarchive");
		PostMail mail = (PostMail)archive.getModuleManager().getBean( "postMail");
		return mail.getTemplateWebEmail();
	}
	
	private boolean isCSVFile(Page inFile) {
		boolean found = false;
		String filename = inFile.getName();
		String extension = filename.substring(filename.lastIndexOf(".") + 1, filename.length());
		if (extension.equals("csv")) {
			found = true;
		}
		return found;
	}
	private boolean movePageToProcessed(PageManager pageManager, Page page,
	String catalogid, boolean saved) {

		boolean move = false;

		String processedFolder = "/WEB-INF/data/${catalogid}/processed/inventory/";
		if (!saved) {
			processedFolder += "errors/";
		}

		String destinationFile = processedFolder + page.getName();
		Page destination = pageManager.getPage(destinationFile);
		pageManager.movePage(page, destination);
		if (destination.exists()) {
			move = true;
		}
		return move;
	}
}

logs = new ScriptLogger();
logs.startCapture();

try {

	log.info("START - ImportAffinityInventory");
	ImportMultipleAffinityInventory importAffinity = new ImportMultipleAffinityInventory();
	importAffinity.setLog(logs);
	importAffinity.setContext(context);
	importAffinity.setModuleManager(moduleManager);
	importAffinity.setPageManager(pageManager);
	importAffinity.doImport();
	log.info("FINISH - ImportAffinityInventory");
}
finally {
	logs.stopCapture();
}
