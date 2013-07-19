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

import com.openedit.OpenEditException
import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker
import com.openedit.page.Page
import com.openedit.util.FileUtils


public class ImportInventory  extends EnterMediaObject {
	
	List<String> badProductList;
	List<String> badUPCList;
	List<String> badQtyList;
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
		
		//Create the searcher objects.	 
		SearcherManager manager = archive.getSearcherManager();
		Searcher inventorysearcher = manager.getSearcher(archive.getCatalogId(), "inventoryimport");
		Searcher distributorsearcher = manager.getSearcher(archive.getCatalogId(), "distributor");
		Searcher productsearcher = store.getProductSearcher();
		Searcher userprofilesearcher = archive.getSearcher("userprofile");
		
		//Get CSV Type
		String csvType = inReq.getRequestParameter("csvtype");
		Data distributor = distributorsearcher.searchById(csvType);
		String inDistributor = "unknown";
		if (distributor != null) {
			inDistributor = distributor.get("name");
		}		
		
		//Get the Uploaded Page
		String filename = "inventory.csv";
		Page upload = archive.getPageManager().getPage(catalogID + "/temp/upload/" + filename);
		
		Page archive = archive.getPageManager().getPage(catalogID + "/temp/upload/" + filename);
		
		
		
		Reader reader = upload.getReader();
		try
		{
			Data csvFields = inventorysearcher.searchByField("distributor", csvType);
			if (csvFields == null) {
				inReq.putPageValue("errorout", "Invalid Distributor");
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

			def ROGERS_SEARCH_FIELD = "rogerssku";
			def MANUFACTURER_SEARCH_FIELD = "manufacturersku";
			def UPC_SEARCH_FIELD = "upc";

			//loop over rows
			def boolean done = false;
			String[] cols;
			while ((cols = read.readNext()) != null)
			{
				String manufacturerSKU = null;
				String rogersSKU = null;
				if (csvType.equals("104")) {
					manufacturerSKU = parseMicrocelData(cols);
					rogersSKU = manufacturerSKU;
				} else {
					manufacturerSKU = cols[manufacturerSKUcol].trim();
					rogersSKU = cols[rogersSKUcol].trim();
				}
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
			
			context.putPageValue("totalrows", getTotalRows());
			context.putPageValue("goodproductlist", getGoodProductList());
			context.putPageValue("badproductlist", getBadProductList());
			context.putPageValue("badupclist", getBadUPCList());
			context.putPageValue("badqtylist", getBadQTYList());
			context.putPageValue("distributor", inDistributor);
			
			String subject = "Inventory Report - " + inDistributor;
			Date newDate = new Date();
			subject += " - " + parseDate(newDate);
			if ((getBadProductList().size() > 0) || (getBadUPCList().size() > 0) || (getBadQTYList().size()>0)) {
				subject += " - ERRORS FOUND!";
			} 
			ArrayList emaillist = new ArrayList();
			HitTracker results = userprofilesearcher.fieldSearch("storeadmin", "true");
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
		finally
		{
			FileUtils.safeClose(reader);
		}
		
	}
	private String trimNumber( String inNumber, String inSKU ) {
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
	
	private String parseMicrocelData( String[] inData ) {
		String[] split = inData[0].split("-");
		String extract = split[2].trim();
		return extract;
	}
	protected void sendEmail(MediaArchive archive, WebPageRequest context, List inEmailList, String templatePage, String inSubject){
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
}

log = new ScriptLogger();
log.startCapture();

try {

	log.info("START - ImportInventory");
	ImportInventory importInventory = new ImportInventory();
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
