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


public class ReverseCheckProducts  extends EnterMediaObject {
	
	List<String> badProductList;
	List<String> goodProductList;
	List<String> badUPCList;
	List<String> foundUPCList;
	int totalRows;

	/* GET LISTS */
	public List<String> getBadProductList() {
		if(badProductList == null) {
			badProductList = new ArrayList<String>();
		}
		return badProductList;
	}
	public List<String> getGoodProductList() {
		if(goodProductList == null) {
			goodProductList = new ArrayList<String>();
		}
		return goodProductList;
	}
	public List<String> getBadUPCList() {
		if(badUPCList == null) {
			badUPCList = new ArrayList<String>();
		}
		return badUPCList;
	}
	public List<String> getFoundUPCList() {
		if(foundUPCList == null) {
			foundUPCList = new ArrayList<String>();
		}
		return foundUPCList;
	}

	/* ADD TO LISTS */
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
	public void addToFoundUPCList(String inItem) {
		if(foundUPCList == null) {
			foundUPCList = new ArrayList<String>();
		}
		foundUPCList.add(inItem);
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
		String filename = "reverse.csv";
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
			
			List csvSkus = new ArrayList();
			List csvDistributor = new ArrayList();
			List csvDescription = new ArrayList();
			List csvUPCcodes = new ArrayList();
			List upcCodes = new ArrayList();
			
			String[] cols;
			while ((cols = read.readNext()) != null)
			{
				csvSkus.add(cols[colRogersSKU].trim());
				csvDistributor.addAll(cols[colDistributor].trim());
				csvDescription.add(cols[colDescription].trim());
				csvUPCcodes.add(cols[colUpc].trim());
			}

			boolean foundProduct = false
			HitTracker hits = productsearcher.getAllHits();
			for (Iterator iterator = hits.iterator(); iterator.hasNext();) {
				Data p = (Data) iterator.next();
				Product product = productsearcher.searchById(p.getId());
				foundProduct = false;
				if (product != null) {
					for (int indx = 0; indx < csvSkus.size(); indx++) {
						if (product.get(ROGERS_SEARCH_FIELD) == csvSkus.get(indx)) {
							if (product.get(UPC_SEARCH_FIELD) == csvUPCcodes.get(indx)) {
								addToGoodProductList("[FR]|" + product.get(ROGERS_SEARCH_FIELD) + 
									"|" + csvSkus.get(indx) + 
									"|[FU]|" + product.get(UPC_SEARCH_FIELD) +
									"|" + csvUPCcodes.get(indx) +
									"|" + getDistributor(product.get("distributor"), media) +
									"|" + product.get("name"));
								foundProduct = true;
								break;
							} else {
								addToBadProductList("[FR]|" + product.get(ROGERS_SEARCH_FIELD) +
									"|" + csvSkus.get(indx) + 
									"|[NMU]|" + product.get(UPC_SEARCH_FIELD) +
									"|" + csvUPCcodes.get(indx) +
									"|" + getDistributor(product.get("distributor"), media) +
									"|" + product.get("name"));
								foundProduct = true;
								break;
							}
						}
					}
				}
				if (!foundProduct) {
					addToBadProductList("[NFR]|" + p.get(ROGERS_SEARCH_FIELD) +
						"|" + p.get(MANUFACTURER_SEARCH_FIELD) + 
						"|[NFU]|" + p.get(UPC_SEARCH_FIELD) +
						"|" + 
						"|" + getDistributor(p.get("distributor"), media) +
						"|" + p.get("name")); 
					upcCodes.add(p.get(UPC_SEARCH_FIELD));
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
	private String getDistributor(String distributor, MediaUtilities media) {
		Data d = media.getDistributorSearcher().searchById(distributor);
		if (d != null) { 
			return d.get("name");
		} else {
			return "unknown";
		}
	}
}

logs = new ScriptLogger();
logs.startCapture();

try {

	log.info("START - ReverseCheckProducts");
	ReverseCheckProducts checkProducts = new ReverseCheckProducts();
	checkProducts.setLog(logs);
	checkProducts.setContext(context);
	checkProducts.setModuleManager(moduleManager);
	checkProducts.setPageManager(pageManager);
	checkProducts.handleSubmission();
	log.info("FINISH - ReverseCheckProducts");
}
finally {
	logs.stopCapture();
}
