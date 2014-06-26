package products;


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

import org.openedit.entermedia.Category;


public void init(){
	
	WebPageRequest inReq = context;
	Store store = inReq.getPageValue("store");
	MediaArchive archive = inReq.getPageValue("mediaarchive");
	 
	Searcher productsearcher = store.getProductSearcher();
	Searcher assetsearcher = archive.getAssetSearcher();
	Searcher userprofilesearcher = archive.getSearcher("userprofile");
	Searcher ticketsearcher = archive.getSearcher("ticket");
	Searcher tickethistorysearcher = archive.getSearcher("tickethistory");
	
	Product product = null;
	String templatePage = "";
	boolean newProduct = false;
	
	FileUpload command = new FileUpload();
	command.setPageManager(archive.getPageManager());
	UploadRequest properties = command.parseArguments(inReq);
	
	String productid = inReq.getRequestParameter("productid");
	String ticketid = inReq.getRequestParameter("ticketid");
	
	if (productid != null) {
		product = store.getProduct(productid);
	}
	if (product == null) {
		return;
	}
	String [] fields = inReq.getRequestParameters("field");
	productsearcher.updateData(inReq, fields, product);
	//check for these exceptions
	if (inReq.getRequestParameter("msrp_rogers.value") != null){
		String val = inReq.getRequestParameter("msrp_rogers.value");
		if (val.contains(".")){
			val = val.substring(0,val.indexOf("."));
		}
		int number = toInt(val,-1);
		if (number > 0){
			product.setProperty("msrp_rogers", "${number}.99");
		} else {
			product.setProperty("msrp_rogers", null);
		}
	}
	if (inReq.getRequestParameter("msrp_fido.value") != null){
		String val = inReq.getRequestParameter("msrp_fido.value");
		if (val.contains(".")){
			val = val.substring(0,val.indexOf("."));
		}
		int number = toInt(val,-1);
		if (number > 0){
			product.setProperty("msrp_fido", "${number}.00");
		} else {
			product.setProperty("msrp_fido", null);
		}
	}
	
	productsearcher.saveData(product, inReq.getUser());
	inReq.putPageValue("data", product);
	
//	Data ticket = null;
//	if (ticketid == null) {
//		ticket = ticketsearcher.createNewData();
//		ticket.setId(ticketsearcher.nextId());
//		ticket.setProperty("owner", inReq.getUserProfile().getId());
//		ticket.setProperty("product", product.getId());
//		ticket.setProperty("tickettype", "newproductsubmission");
//		ticket.setProperty("notes", "New product has been submitted.");
//	} else {
//		ticket = ticketsearcher.searchById(ticketid);
//		if (ticket == null) {
//			throw new OpenEditException("Ticket is null");
//		}
//		ticket.setProperty("tickettype", "updateproduct");
//		ticket.setProperty("notes", "Product has been updated.");
//	}
//	ticket.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
//	ticket.setProperty("ticketstatus", "open");
//	ticket.setSourcePath("${inReq.getUserProfile().getId()}");
//	//THESE FIELDS ARE ACTUALLY PRODUCT FIELDS!
//	ticketsearcher.saveData(ticket, inReq.getUser());
//	inReq.putPageValue("ticket", ticket);

//	String ticketStatus = "closed";
//	String ticketNotes = ticket.get("notes");
//	createTicketHistory(tickethistorysearcher, ticket, inReq, ticketStatus, ticketNotes );

//	ArrayList emailList = new ArrayList();
//	HitTracker results = userprofilesearcher.fieldSearch("ticketadmin", "true");
//	if (results.size() > 0) {
//		for(Iterator detail = results.iterator(); detail.hasNext();) {
//			Data userInfo = (Data)detail.next();
//			emailList.add(userInfo.get("email"));
//		}
//		context.putPageValue("data", product);
//		templatePage = "/ecommerce/views/modules/product/workflow/admin-notification-template.html";
//		String subject = "";
//		if (ticketid == null) {
//			if (newProduct) {
//				subject = "New Product Submission";
//			} else {
//				subject = "Product Update Submission";
//			}
//		} else {
//			subject = "Product Update Submission";
//		}
//		sendEmail(context, emailList, templatePage, subject);
//	}
//	emailList = null;
//	results = null;
//
//	Data owner = userprofilesearcher.searchById(ticket.get("owner"))
//	if (owner != null) {
//		ArrayList userEmail = new ArrayList();
//		String email = owner.get("email");
//		if (email != null) {
//			userEmail.add(email);
//			templatePage = "/ecommerce/views/modules/ticket/workflow/user-notification-template.html";
//			sendEmail(inReq, userEmail, templatePage, "Support Ticket Update");
//			
//			if (ticket.get("tickettype") == "updateproduct") {
//				String subject = "";
//				inReq.putPageValue("data", product);
//				if (product.get("approved") == "true") {
//					ticketStatus = "closed";
//					ticketNotes = "Product has been approved";
//					createTicketHistory(tickethistorysearcher, ticket, inReq, ticketStatus, ticketNotes);
//					
//					templatePage = "/ecommerce/views/modules/product/workflow/user-notification-approved.html";
//					subject = "Product has been approved.";
//				} else {
//					templatePage = "/ecommerce/views/modules/product/workflow/user-notification-notapproved.html";
//					subject = "Product needs to be reviewed.";
//				}
//			}
//		}
//		userEmail = null;
//	}
//	
//	emailList = new ArrayList();
//	results = userprofilesearcher.fieldSearch("ticketadmin", "true");
//	if (results.size() > 0) {
//		for(Iterator detail = results.iterator(); detail.hasNext();) {
//			Data userInfo = (Data)detail.next();
//			emailList.add(userInfo.get("email"));
//		}
//		templatePage = "/ecommerce/views/modules/ticket/workflow/admin-notification-template.html";
//		sendEmail(inReq, emailList, templatePage, "Support Ticket Update");
//	}
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

public int toInt(String str, int inDefault)
{
	int out = inDefault;
	if (str)
	{
		try
		{
			out = Integer.parseInt(str);
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

init();