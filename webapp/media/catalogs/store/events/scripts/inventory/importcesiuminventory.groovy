package inventory

/*
 * Created on Aug 24, 2005
 */

import org.entermedia.email.PostMail
import org.entermedia.email.TemplateWebEmail
import org.openedit.Data
import org.openedit.data.Searcher
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

public class ImportCesiumInventory extends EnterMediaObject {
	
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

	public void handleSubmission(){
		
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
		String filename = "cesiuminventory.csv";
		Page upload = archive.getPageManager().getPage(catalogID + "/temp/upload/" + filename);
		Reader reader = upload.getReader();
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
			
			context.putPageValue("totalrows", getTotalRows());
			context.putPageValue("goodproductlist", getGoodProductList());
			context.putPageValue("badproductlist", getBadProductList());
			context.putPageValue("badupclist", getBadUPCList())
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
		finally
		{
			FileUtils.safeClose(reader);
		}
		
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
		PostMail mail = (PostMail)mediaarchive.getModuleManager().getBean( "postMail");
		return mail.getTemplateWebEmail();
	}
}

log = new ScriptLogger();
log.startCapture();

try {

	log.info("START - ImportCesiumInventory");
	ImportCesiumInventory importCesium = new ImportCesiumInventory();
	importCesium.setLog(log);
	importCesium.setContext(context);
	importCesium.setModuleManager(moduleManager);
	importCesium.setPageManager(pageManager);
	importCesium.handleSubmission();
	log.info("FINISH - ImportCesiumInventory");
}
finally {
	log.stopCapture();
}
