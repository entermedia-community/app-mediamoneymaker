package inventory

/*
 * Created on Aug 24, 2005
 */

import java.text.SimpleDateFormat

import org.entermedia.email.PostMail
import org.entermedia.email.TemplateWebEmail
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.util.CSVReader
import org.openedit.store.InventoryItem
import org.openedit.store.Product
import org.openedit.store.Store
import org.openedit.util.DateStorageUtil

import com.openedit.OpenEditException
import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker
import com.openedit.modules.update.Downloader
import com.openedit.page.Page
import com.openedit.page.manage.PageManager
import com.openedit.util.FileUtils

public class ImportInventoryFromUrl  extends EnterMediaObject {

	public static final String MICROCEL_ID = "";
	public static final String CESIUM_ID = "";
	
	List<String> badProductList;
	List<String> badUPCList;
	List<String> badQtyList;
	List<String> badRowList; 
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
	public List<String> getBadQTYList() {
		if(badQtyList== null) {
			badQtyList = new ArrayList<String>();
		}
		return badQtyList;
	}
	public List<String> getBadRowList() {
		if (badRowList == null) {
			badRowList = new ArrayList<String>();
		}
		return badRowList;
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
	public void addToBadQTYList(String inItem) {
		if(badQtyList == null) {
			badQtyList = new ArrayList<String>();
		}
		badQtyList.add(inItem);
	}
	public void addToBadRowList(String inItem) {
		if (badRowList == null) {
			badRowList = new ArrayList<String>();
		}	
		badRowList.add(inItem);
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

	public void handleSubmission(){
		
		//Create the MediaArchive object
		Store store = context.getPageValue("store");
		WebPageRequest inReq = context;
		MediaArchive archive = inReq.getPageValue("mediaarchive");
		String catalogID = archive.getCatalogId();
		String inURL = "";
		
		//Create the searcher objects.	 
		SearcherManager manager = archive.getSearcherManager();
		Searcher inventorysearcher = manager.getSearcher(archive.getCatalogId(), "inventoryimport");
		Searcher distributorsearcher = manager.getSearcher(archive.getCatalogId(), "distributor");
		Searcher productsearcher = store.getProductSearcher();
		Searcher userprofilesearcher = archive.getSearcher("userprofile");
		
		//Get CSV Type
		String inDistributor = inReq.findValue("distributor");
		Data distributor = distributorsearcher.searchById(inDistributor);
		if (distributor == null) {
			context.putPageValue("errorout", "Distributor does not exist.");
			log.info("Distributor does not exist.");
			return;
		}
		log.info("Distributor: " + distributor.getName());
		
		Data inventoryImport = inventorysearcher.searchByField("distributor", distributor.getId());
		if (inventoryImport != null) {
			inURL = inventoryImport.get("importurl");
			if (inURL == null || inURL.equals("")) {
				context.putPageValue("errorout", "URL does not exist!");
				log.info("URL does not exist!");
				return;
			}
			log.info("Inventory URL: " + inURL);
		}
				
		//Get the Uploaded Page
		String filename = "inventory.csv";
		Page upload = archive.getPageManager().getPage(catalogID + "/temp/upload/" + filename);
		String path = upload.getPath();
		log.info("Upload Path: " + path);
		File file = new File(upload.getContentItem().getAbsolutePath());
		if (file.exists()) {
			file.delete();
			if (!file.exists()) {
				String msg = " - Existing file has been removed.";
				log.info(msg);
			}
		}

		Downloader dl = new Downloader();
		dl.download(inURL, file);
		
		if (upload.exists()) {
			log.info("File exists: " + upload.getPath())
		} else {
			String msg = "File does not exist: " + upload.getPath();
			log.info(msg);
			return;
		}
		
		//Get the Uploaded Page
		Reader reader = upload.getReader();
		try
		{
			Data csvFields = inventorysearcher.searchByField("distributor", inDistributor);
			if (csvFields == null) {
				String msg = "Invalid Distributor in InventoryImport Table";
				log.info(msg);
				return;
			}
			
			//Create new CSV Reader Object
			CSVReader read = new CSVReader(reader, ',', '\"');
		
			//Read 1 line for headers
			String[] headers = read.readNext();
			
			int manufacturerSKUcol = Integer.parseInt(csvFields.get("sku"));
			int rogersSKUcol = Integer.parseInt(csvFields.get("sku"));
			int upcCol = Integer.parseInt(csvFields.get("upc"));
			int quantityCol = Integer.parseInt(csvFields.get("quantity"));
			int descriptionCol = Integer.parseInt(csvFields.get("description"));
			int transDate = Integer.parseInt(csvFields.get("transdate"));
			int transTime = Integer.parseInt(csvFields.get("transtime"));
			int transDateTime = Integer.parseInt(csvFields.get("transdatetime"));
			double lastImportDateTime = csvFields.get("lastimportdate").toDouble();

			def ROGERS_SEARCH_FIELD = "rogerssku";
			def MANUFACTURER_SEARCH_FIELD = "manufacturersku";
			def UPC_SEARCH_FIELD = "upc";

			//loop over rows
			def boolean startImport = false;
			String[] cols;
			String inTransDateTime = "";
			double lastDateTime = 0;
			while ((cols = read.readNext()) != null)
			{
				try {
					if (transDateTime > -1 && !startImport) {
						log.info("TransDateTime found in CSV file. Checking Dates");
						inTransDateTime = cols[transDateTime];
						lastDateTime = inTransDateTime.toDouble();
						if (lastDateTime <= lastImportDateTime) {
							log.info("SKIPPING: LastImportDate is greater than TransDate found in CSV file.");
							log.info("LastImportDate: " + lastImportDateTime.toString());
							log.info("TransDateTime: " + lastDateTime.toString());
							break;
						} else {
							if (!startImport) {
								startImport = true;
								log.info("lastDateTime is greater than lastImportDate. Starting Import");
							}
						}
					} else {
						if (!startImport) {
							startImport = true;
							log.info("No TransDateTime found in CSV file. Starting Import");
						}
					}
					if (startImport) {
						String manufacturerSKU;
						String rogersSKU;
						if (inDistributor.equals("104")) {
							manufacturerSKU = parseMicrocelData(cols);
							rogersSKU = manufacturerSKU;
						} else if (inDistributor.equals(CESIUM_ID)) {
						
						} else {
							manufacturerSKU = cols[manufacturerSKUcol];
							rogersSKU = cols[rogersSKUcol];
						}
						if (manufacturerSKU != null && manufacturerSKU.length() > 0) {
						
							manufacturerSKU = manufacturerSKU.trim();
							rogersSKU = rogersSKU.trim();
							
							String upcNumber = cols[upcCol].trim();
							String newQuantity = cols[quantityCol].trim();
							newQuantity = trimNumber(newQuantity, rogersSKU);
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
								if (product != null) {
									InventoryItem productInventory = null
									productInventory = product.getInventoryItem(0);
									if (productInventory == null) {
										//Need to create the Inventory Item
										productInventory = new InventoryItem();
										productInventory.setQuantityInStock(qtyInStock)
										product.addInventoryItem(productInventory);
									} else {
										if (productInventory.getQuantityInStock() != qtyInStock) {
											String msg = "Product(" + manufacturerSKU + ":" + product.getName() + ") inventory changed: ";
											msg += productInventory.getQuantityInStock().toString() + ":"
											msg += qtyInStock.toString();
											log.info(msg);
											productInventory.setQuantityInStock(qtyInStock);
											product.setProperty("inventoryupdated", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
											productsearcher.saveData(product, context.getUser());
										} else {
											String msg = "Product(" + manufacturerSKU + ":" + product.getName() + ") no inventory changes.";
											log.info(msg);
										}
									}
									addToGoodProductList(rogersSKU);
							
								} else {
									log.info("Could not open product (" + productHit.id + ")!");
									addToBadProductList(manufacturerSKU)
								}
							}
							increaseTotalRows();
						} else {
							String msg = "Blank Row Found in CSV file:Row:" + getTotalRows().toString();
							addToBadRowList(msg);
							log.info(msg);
							increaseTotalRows();
						}
					}
				}
				catch (ArrayIndexOutOfBoundsException aex) {
					log.info("ArrayIndexOutOfBounds Exception caught:" + aex.getMessage());
					String msg = "Blank Row Found in CSV file:Row:" + getTotalRows().toString();
					addToBadRowList(msg);
					log.info(msg);
					increaseTotalRows();
				}
				catch (NullPointerException ne) {
					log.info("Null Pointer Exception caught:" + ne.getMessage());
					log.info("Cause:" + ne.getCause().toString());
					log.info("Trace:" + ne.getStackTrace().toString());
				}
				catch (OpenEditException oeex) {
					log.info("OpenEdit Exception caught:" + oeex.getMessage());
					log.info("Cause:" + oeex.getCause().toString());
					log.info("Path:" + oeex.getPathWithError());
				}
				catch (Exception e) {
					log.info("Exception caught:" + e.getMessage());
					log.info("Cause:" + e.getCause().toString());
					log.info("Trace:" + e.getStackTrace().toString());
				}
			}
			read.close();
			
			String inText = "Import complete: ";
			inText += "totalrows:" + getTotalRows().toString();
			log.info(inText);
			
			if (getTotalRows() > 0 && startImport) {
				//More than one row was processed
				Date now = new Date();
				String newFilename = "inventory-";
				if (transDateTime > -1) {
					newFilename += inTransDateTime;
				} else {
					newFilename += parseDateTime(now);
				}
				newFilename += ".csv";
				Boolean result = movePageToProcessed(pageManager, upload, newFilename, catalogID, startImport);
				if (result) {
					log.info("Inventory File has been saved: " + newFilename);
				}
				if (inTransDateTime != null && inTransDateTime.length() > 0) {
					csvFields.setProperty("lastimportdate", inTransDateTime);
				} else {
					csvFields.setProperty("lastimportdate", parseDateTime(now));
				}
				inventorysearcher.saveData(csvFields, inReq.getUser());
			
				context.putPageValue("export", inURL);
				context.putPageValue("url", inURL);
				context.putPageValue("totalrows", getTotalRows());
				context.putPageValue("badrows", getBadRowList())
				context.putPageValue("goodproductlist", getGoodProductList());
				context.putPageValue("badproductlist", getBadProductList());
				context.putPageValue("badupclist", getBadUPCList());
				context.putPageValue("badqtylist", getBadQTYList());
				context.putPageValue("distributor", distributor.getName());
				context.putPageValue("errorout", "[NONE]");
				
				String subject = "Inventory Report - " + distributor.getName();
				Date newDate = new Date();
				subject += " - " + parseDate(newDate);
				if ((getBadProductList().size() > 0) || (getBadUPCList().size() > 0) || (getBadQTYList().size()>0)) {
					subject += " - ERRORS FOUND!";
				} 
				ArrayList emaillist = new ArrayList();
				HitTracker results = userprofilesearcher.fieldSearch("productadmin", "true");
				if (results.size() > 0) {
					for(Iterator detail = results.iterator(); detail.hasNext();) {
						Data userInfo = (Data)detail.next();
						emaillist.add(userInfo.get("email"));
					}
					String templatePage = "/ecommerce/views/modules/product/workflow/inventory-notification.html";
					sendEmail(archive, context, emaillist, templatePage, subject);
					log.info("Email sent to Store Admins");
				}
			}
		}
		finally
		{
			FileUtils.safeClose(reader);
		}
		
	}
	private String trimNumber( String inNumber, String inSKU ) 
	{
		if (inNumber.indexOf(".") != -1) {
			String msg = inSKU + ": Invalid Quentity Found: " + inNumber; 
			log.info(msg);
			addToBadQTYList(msg)
			float n = inNumber.toFloat();
			int c = (int) n;
			inNumber = c.toString();
		}
		return inNumber;
	}
	
	private String parseMicrocelData( String[] inData ) 
	{
		String[] split = inData[0].split("-");
		String extract = split[2].trim();
		return extract;
	}

	private String parseCesiumData( String[] inData ) 
	{
		String[] split = inData[0].split("-");
		String extract = split[2].trim();
		return extract;
	}
	
	protected void sendEmail(MediaArchive archive, WebPageRequest context, List inEmailList, String templatePage, String inSubject)
	{
		Page template = pageManager.getPage(templatePage);
		WebPageRequest newcontext = context.copy(template);
		TemplateWebEmail mailer = getMail(archive);
		mailer.loadSettings(newcontext);
		mailer.setMailTemplatePath(templatePage);
		mailer.setFrom("info@wirelessarea.ca");
		mailer.setRecipientsFromStrings(inEmailList);
		mailer.setSubject(inSubject);
		mailer.send();
	}
	
	protected TemplateWebEmail getMail(MediaArchive archive) {
		PostMail mail = (PostMail)archive.getModuleManager().getBean( "postMail");
		return mail.getTemplateWebEmail();
	}
	private String parseDate(Date inDate)
	{
		SimpleDateFormat newFormat = new SimpleDateFormat("yyyy-MM-dd");
		String out = newFormat.format(inDate);
		return out;
	}
	private String parseDateTime(Date inDate)
	{
		SimpleDateFormat newFormat = new SimpleDateFormat("yyyyMMddmmss");
		String out = newFormat.format(inDate);
		return out;
	}
	
	private boolean movePageToProcessed(PageManager pageManager, Page page, String newFilename, String catalogid, boolean saved) 
	{
		boolean movePageResult = false;

		String processedFolder = "/WEB-INF/data/${catalogid}/processed/inventory/imports/";
		if (!saved) {
			processedFolder += "errors/";
		}

		String destinationFile = processedFolder + newFilename;
		Page destination = pageManager.getPage(destinationFile);
		pageManager.movePage(page, destination);
		if (destination.exists()) {
			movePageResult = true;
		}
		return movePageResult;
	}
}

log = new ScriptLogger();
log.startCapture();

try {

	log.info("START - ImportInventory");
	ImportInventoryFromUrl importInventory = new ImportInventoryFromUrl();
	importInventory.setLog(log);
	importInventory.setContext(context);
	importInventory.setModuleManager(moduleManager);
	importInventory.setPageManager(pageManager);
	importInventory.handleSubmission();
	log.info("FINISH - ImportInventory");
}
finally {
	log.stopCapture();
}
