package products

/*
 * Created on Aug 24, 2005
 */

import org.entermedia.email.PostMail
import org.entermedia.email.TemplateWebEmail
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.util.CSVReader
import org.openedit.store.Product
import org.openedit.store.util.MediaUtilities

import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker
import com.openedit.page.Page
import com.openedit.util.FileUtils


public class CheckProducts  extends EnterMediaObject {
	
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
		MediaUtilities media = new MediaUtilities();
		media.setContext(context);
		
		MediaArchive archive = media.getArchive();
		String catalogID = media.getCatalogid();

		String strMsg = "";
	
		//Create the searcher objects.	 
		Searcher productsearcher = media.getProductSearcher();
		Searcher userprofilesearcher = archive.getSearcher("userprofile");
		Searcher ordersearcher = media.getOrderSearcher();
		
		WebPageRequest inReq = context;
		
		//Get the Uploaded Page
		String filename = "checkproducts.csv";
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
			headers = read.readNext();
			
			int colManufacturer = 0;
			int colDistributor = 1;
			int colFranchise = 2;
			int colDescription = 3;
			int colRogersSKU = 4;
			int colManufacturerSKU = 5;
			int colCompatibility = 6;
			int colUpc = 7;
			
			//loop over rows
			String[] cols;
			while ((cols = read.readNext()) != null)
			{
				String rogersSKU = cols[colRogersSKU].trim();
				String manufacturerSKU = cols[colManufacturer].trim();
				String distributor = cols[colDistributor].trim();
				String upcNumber = cols[colUpc].trim();
				Data product = null;
				
				//Search for the product by the MANUFACTURER_SEARCH_FIELD
				if (rogersSKU != "") {
					product = productsearcher.searchByField(ROGERS_SEARCH_FIELD, rogersSKU);
					if (product != null) {
						Product p = productsearcher.searchById(product.getId());
						if (product.get("upc").equals(upcNumber)) {
							addToGoodProductList("[FR]|" + product.get(ROGERS_SEARCH_FIELD) + "|" + rogersSKU + 
								"|[FU]|" + product.get(UPC_SEARCH_FIELD) + "|" + upcNumber);
							p.setProperty("validitem", "true");
							productsearcher.saveData(p, inReq.getUser());
						} else {
							addToBadProductList("[FR]|" + product.get(ROGERS_SEARCH_FIELD) + "|" + rogersSKU + 
								"|[NFU]|" + product.get(UPC_SEARCH_FIELD) + "|" + upcNumber);
							strMsg = "Found RogersSKU - Invalid UPC";
							p.setProperty("validerrormessage", strMsg);
							productsearcher.saveData(p, inReq.getUser());

						}
					} else {
						//Search for the product by the ROGERS_SEARCH_FIELD
						if (upcNumber != "") {
							product = productsearcher.searchByField(UPC_SEARCH_FIELD, upcNumber);
							if (product == null) {
								addToBadProductList("[NFR]||" + rogersSKU + "|[NFU]||" + upcNumber);
							} else {
								Product p = productsearcher.searchById(product.getId());
								addToBadProductList("[NFR]||" + rogersSKU + 
									"|[FU]|" + product.get(UPC_SEARCH_FIELD) + "|" + upcNumber);
								strMsg = "NOT Found RogersSKU - Found UPC";
								p.setProperty("validerrormessage", strMsg);
								productsearcher.saveData(p, inReq.getUser());
							}
							product = null;
						}  else {
							addToBadProductList("[NFR]|||[NFU]||BLANK");
						}
			        }
				}  else {
					addToBadProductList("[NFR]||BLANK|[NFU]||");
				}
				increaseTotalRows();
			}
			
			context.putPageValue("totalrows", getTotalRows());
			context.putPageValue("goodproductlist", getGoodProductList());
			context.putPageValue("badproductlist", getBadProductList());
			
			ArrayList emaillist = new ArrayList();
			HitTracker results = userprofilesearcher.fieldSearch("ticketadmin", "true");
			for(Iterator detail = results.iterator(); detail.hasNext();) {
				Data userInfo = (Data)detail.next();
				emaillist.add(userInfo.get("email"));
			}
			String templatePage = "/ecommerce/views/modules/product/workflow/product-notification.html";
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

logs = new ScriptLogger();
logs.startCapture();

try {

	log.info("START - ImportAffinityInventory");
	CheckProducts checkProducts = new CheckProducts();
	checkProducts.setLog(logs);
	checkProducts.setContext(context);
	checkProducts.setModuleManager(moduleManager);
	checkProducts.setPageManager(pageManager);
	checkProducts.handleSubmission();
	log.info("FINISH - ImportAffinityInventory");
}
finally {
	logs.stopCapture();
}
