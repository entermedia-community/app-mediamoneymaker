
/*
 * Created on Aug 24, 2005
 */

import org.entermedia.email.PostMail
import org.entermedia.email.TemplateWebEmail
import org.entermedia.upload.FileUpload
import org.entermedia.upload.FileUploadItem
import org.entermedia.upload.UploadRequest
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.money.Money
import org.openedit.store.InventoryItem
import org.openedit.store.Price
import org.openedit.store.PriceSupport
import org.openedit.store.Product
import org.openedit.store.Store
import org.openedit.util.DateStorageUtil

import com.openedit.OpenEditException
import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.page.Page
import com.openedit.page.manage.PageManager
import com.openedit.util.Replacer


public void handleSubmission(){
	
	WebPageRequest inReq = context;
	Store store = inReq.getPageValue("store");
	MediaArchive archive = inReq.getPageValue("mediaarchive");
	PageManager pagemanager = archive.getPageManager();
	 
	Searcher productsearcher = store.getProductSearcher();
	Searcher assetsearcher = archive.getAssetSearcher();
	Searcher userprofilesearcher = archive.getSearcher("userprofile");
	Searcher ticketsearcher = archive.getSearcher("ticket");
	Searcher tickethistorysearcher = archive.getSearcher("tickethistory");
	
	Product product = null;
	String templatePage = "";
	boolean newProduct = false;
	
	//first establish the multipart file upload
	FileUpload command = new FileUpload();
	command.setPageManager(pagemanager);
	UploadRequest properties = command.parseArguments(inReq);
	if(properties == null)
	{
		properties = (UploadRequest) inReq.getPageValue("properties");
	}
	if (properties != null && properties.getFirstItem() == null) 
	{
		properties = (UploadRequest) inReq.getPageValue("properties");
	}
	//now get request parameters
	String productid = inReq.getRequestParameter("productid");
	if (productid == null || productid.trim().isEmpty())
	{
		productid = inReq.getRequestParameter("id");
	}
	String ticketid = inReq.getRequestParameter("ticketid");
	if (productid != null) {
		product = store.getProduct(productid);
	}
	if (product == null) {
		//if no product can be loaded then this is a new product
		Data d = productsearcher.createNewData();
		d.setId(productsearcher.nextId());
		productid = d.getId();
		d.setSourcePath(inReq.getUserProfile().get("distributor") + "/" + productid);
		productsearcher.saveData(d, null);
		
		product = store.getProduct(productid);
		newProduct = true;
	}
	if(properties != null && properties.getFirstItem() != null){
		//handle the upload only if we actually have items to upload
		//need to have the product sourcepath to complete uploads
		handleUpload(inReq,properties,product);
	}
	//update all fields: if there are no uploads but just selecting another assetid, fields will populate
	String [] fields = inReq.getRequestParameters("field");
	productsearcher.updateData(inReq, fields, product);
	product.setProperty("submittedby", inReq.getUserName());
	if (product.get("distributor") == null) {	
		product.setProperty("distributor", inReq.getUserProfile().get("distributor"));
	}
	if (product.get("profileid") == null) {
		product.setProperty("profileid", inReq.getUserProfile().getId());
	}
	if (product.get("rogersprice") == null) {
		throw new OpenEditException("Cannot create product without pricing information.");
	}
	
	//Clear the items!
	product.clearItems();
	
	double pricefactor = getPriceFactor(archive,product);
	log.info("price factor for $product: $pricefactor");
	
	
	//Create the new item
	InventoryItem inventoryItem = new InventoryItem(product.get("manufacturersku"));
	Money wholesaleprice = new Money(product.get("rogersprice"));
	Money retailprice = new Money(product.get("rogersprice"));
	retailprice = retailprice.multiply(pricefactor);
	//retail price
	Price price = new Price(retailprice);
	//wholesale price
	price.setWholesalePrice(wholesaleprice);
	
	PriceSupport pricing = new PriceSupport();
	
	if(product.clearancecentre == "true"){
		if(!product.clearanceprice){
			throw new OpenEditException("Clearance price wasn't set but product is in clearance section!");
		}
		Money clearanceprice = new Money(product.get("clearanceprice"));
		clearanceprice = clearanceprice.multiply(pricefactor);
		price.setSalePrice(clearanceprice);			
	}
	pricing.addTierPrice(1, price);
	inventoryItem.setPriceSupport(pricing);
	product.addInventoryItem(inventoryItem);
	
		
	productsearcher.saveData(product, context.getUser());
	context.putPageValue("data", product);
	
	Data ticket = null;
	if (ticketid == null) {
		ticket = ticketsearcher.createNewData();
		ticket.setId(ticketsearcher.nextId());
		ticket.setProperty("owner", inReq.getUserProfile().getId());
		ticket.setProperty("product", product.getId());
		ticket.setProperty("tickettype", "newproductsubmission");
		ticket.setProperty("notes", "New product has been submitted.");
	} else {
		ticket = ticketsearcher.searchById(ticketid);
		if (ticket == null) {
			throw new OpenEditException("Ticket is null");
		}
		ticket.setProperty("tickettype", "updateproduct");
		ticket.setProperty("notes", "Product has been updated.");
	}
	ticket.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
	ticket.setProperty("ticketstatus", "open");
	ticket.setSourcePath("${inReq.getUserProfile().getId()}");
	//THESE FIELDS ARE ACTUALLY PRODUCT FIELDS!
	ticketsearcher.saveData(ticket, inReq.getUser());
	inReq.putPageValue("ticket", ticket);

	String ticketStatus = "closed";
	String ticketNotes = ticket.get("notes");
	createTicketHistory(tickethistorysearcher, ticket, inReq, ticketStatus, ticketNotes );

	ArrayList emailList = new ArrayList();
	HitTracker results = userprofilesearcher.fieldSearch("ticketadmin", "true");
	if (results.size() > 0) {
		for(Iterator detail = results.iterator(); detail.hasNext();) {
			Data userInfo = (Data)detail.next();
			emailList.add(userInfo.get("email"));
		}
		context.putPageValue("data", product);
		templatePage = "/ecommerce/views/modules/product/workflow/admin-notification-template.html";
		String subject = "";
		if (ticketid == null) {
			if (newProduct) {
				subject = "New Product Submission";
			} else {
				subject = "Product Update Submission";
			}
		} else {
			subject = "Product Update Submission";
		}
		sendEmail(context, emailList, templatePage, subject);
	}
	emailList = null;
	results = null;

	Data owner = userprofilesearcher.searchById(ticket.get("owner"))
	if (owner != null) {
		ArrayList userEmail = new ArrayList();
		String email = owner.get("email");
		if (email != null) {
			userEmail.add(email);
			templatePage = "/ecommerce/views/modules/ticket/workflow/user-notification-template.html";
			sendEmail(inReq, userEmail, templatePage, "Support Ticket Update");
			
			if (ticket.get("tickettype") == "updateproduct") {
				String subject = "";
				inReq.putPageValue("data", product);
				if (product.get("approved") == "true") {
					ticketStatus = "closed";
					ticketNotes = "Product has been approved";
					createTicketHistory(tickethistorysearcher, ticket, inReq, ticketStatus, ticketNotes);
					
					templatePage = "/ecommerce/views/modules/product/workflow/user-notification-approved.html";
					subject = "Product has been approved.";
				} else {
					templatePage = "/ecommerce/views/modules/product/workflow/user-notification-notapproved.html";
					subject = "Product needs to be reviewed.";
				}
			}
		}
		userEmail = null;
	}
	
	emailList = new ArrayList();
	results = userprofilesearcher.fieldSearch("ticketadmin", "true");
	if (results.size() > 0) {
		for(Iterator detail = results.iterator(); detail.hasNext();) {
			Data userInfo = (Data)detail.next();
			emailList.add(userInfo.get("email"));
		}
		templatePage = "/ecommerce/views/modules/ticket/workflow/admin-notification-template.html";
		sendEmail(inReq, emailList, templatePage, "Support Ticket Update");
	}
}

public double getPriceFactor(MediaArchive archive, Product product)
{
	//get price factor for authorized/non-authorized products
	String distributorid = product.get("distributor");
	if (distributorid)
	{
		Searcher distributorsearcher = archive.getSearcher("distributor");
		Data distributordata = distributorsearcher.searchById(distributorid);
		String auth = distributordata.get("rogersauthorizedpricefactor");
		String nonauth = distributordata.get("rogersnonauthorizedpricefactor");
		double authfact = toDouble(auth,1.1);//default 10%
		double nonauthfact = toDouble(nonauth,1.02);// default 2%
		if (authfact < 1.0) authfact +=1.0;
		if (nonauthfact < 1.0) nonauthfact +=1.0;
		log.info("retail price factor for $distributordata (${distributorid}): authorized $authfact, non-authorized $nonauthfact");
		
		String isauth = product.get("rogersauthorized");
		if ("true".equalsIgnoreCase(isauth))
		{
			return authfact;
		}
		return nonauthfact;
	}
	return 1.1;//original default if all else fails
}

public double toDouble(String str, double inDefault)
{
	double out = inDefault;
	if (str)
	{
		try
		{
			out = Double.parseDouble(str);
		}
		catch (Exception e){}
	}
	return out;
}

protected createTicketHistory(Searcher tickethistorysearcher, Data ticket, WebPageRequest inReq,
		String ticketStatus, String ticketNotes ) {
	Data ticketHistory = tickethistorysearcher.createNewData();
	ticketHistory.setId(tickethistorysearcher.nextId());
	ticketHistory.setSourcePath(ticket.getId());
	ticketHistory.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
	ticketHistory.setProperty("ticket", ticket.getId());
	ticketHistory.setProperty("owner", ticket.get("owner"));
	ticketHistory.setProperty("product", ticket.get("product"));
	ticketHistory.setProperty("tickettype", ticket.get("tickettype"));
	ticketHistory.setProperty("ticketstatus", ticketStatus);
	ticketHistory.setProperty("notes", ticketNotes);
	tickethistorysearcher.saveData(ticketHistory, inReq.getUser());
}
protected void sendEmail(WebPageRequest context, List email, String templatePage, String subject){
	Page template = pageManager.getPage(templatePage);
	WebPageRequest newcontext = context.copy(template);
	TemplateWebEmail mailer = getMail();
	mailer.setFrom("info@wirelessarea.ca");
	mailer.loadSettings(newcontext);
	mailer.setMailTemplatePath(templatePage);
	mailer.setRecipientsFromStrings(email);
	mailer.setSubject(subject);
	mailer.send();
}

protected TemplateWebEmail getMail() {
	PostMail mail = (PostMail)mediaarchive.getModuleManager().getBean( "postMail");
	return mail.getTemplateWebEmail();
}

public void handleUpload(WebPageRequest inReq, UploadRequest properties, Product product) {
	//moved out from AssetEditModule.handleUploads
	//need to have an existing product to figure out the sourcepath
	
//	FileUpload command = new FileUpload();
	MediaArchive archive = inReq.getPageValue("mediaarchive");
//	PageManager pagemanager = archive.getPageManager();
//	command.setPageManager(pagemanager);
//	UploadRequest properties = command.parseArguments(inReq);
//	if(properties == null){
//		properties = (UploadRequest) inReq.getPageValue("properties");
//	}
//	if (properties == null) {
//		return;
//	}
//	
//	if (properties.getFirstItem() == null) {
//		properties = (UploadRequest) inReq.getPageValue("properties");
//		if(properties == null || properties.getFirstItem() == null){
//			return;
//		}
//		
//	}
	for (Iterator iterator = properties.getUploadItems().iterator(); iterator
			.hasNext();) {
		FileUploadItem item = (FileUploadItem) iterator.next();
		
		String name = item.getFieldName();
		if(item.getName().length() == 0){
			continue;
		}
		String[] splits = name.split("\\.");
		String detailid = splits[1];
		String sourcepath = inReq.getRequestParameter(detailid + ".sourcepath");
		if(sourcepath == null){
			 sourcepath = archive.getCatalogSettingValue("projectassetupload");  //${division.uploadpath}/${user.userName}/${formateddate}
		}
		String[] fields = inReq.getRequestParameters("field");
		Map vals = new HashMap();
		vals.putAll(inReq.getPageMap());
		String prefix ="";

		if( fields != null)
		{
			for (int i = 0; i < fields.length; i++)
			{
				String val = inReq.getRequestParameter(prefix + fields[i]+ ".value");
				if( val != null)
				{
					vals.put(fields[i],val);
				}
			}
		}
		String id = inReq.getRequestParameter("id");
		if(id != null){
			vals.put("id",id);
		}
		vals.put("filename", item.getName());
		vals.put("sourcepath",product.getSourcePath());
		
		Replacer replacer = new Replacer();
		replacer.setSearcherManager(archive.getSearcherManager());
		replacer.setCatalogId(archive.getCatalogId());
		replacer.setAlwaysReplace(true);
		sourcepath = replacer.replace(sourcepath, vals);
		sourcepath = sourcepath.replace("//", "/"); //in case of missing data
		String path = "/WEB-INF/data/${archive.getCatalogId()}/originals/${sourcepath}/${item.getName()}";
		path = path.replace("//","/");
		Asset current = archive.getAssetBySourcePath(sourcepath);
		if(current ==  null)
		{	
			current = archive.createAsset(sourcepath);
		}
		current.setProperty("owner", inReq.getUser().getId());
		properties.saveFileAs(item, path, inReq.getUser());
		current.setPrimaryFile(item.getName());
		archive.removeGeneratedImages(current);
		archive.saveAsset(current, null);
		inReq.setRequestParameter(detailid + ".value", current.getId());
		archive.fireMediaEvent("importing/assetuploaded",inReq.getUser(),current);
	}

}

handleSubmission();



		