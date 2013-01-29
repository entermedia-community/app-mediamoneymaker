package products

import org.entermedia.email.PostMail
import org.entermedia.email.TemplateWebEmail
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.store.Product
import org.openedit.util.DateStorageUtil

import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.page.Page

public void init(){
	WebPageRequest inReq = context;
	MediaArchive archive = context.getPageValue("mediaarchive");
	Searcher ticketsearcher = archive.getSearcher("ticket");
	Product product = inReq.getPageValue("product");
	
	Data ticket = ticketsearcher.createNewData();
	ticket.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
	ticket.setId(ticketsearcher.nextId());
	ticket.setProperty("owner", inReq.getUserProfile().getId());
	ticket.setProperty("product", product.getId());
	ticket.setProperty("tickettype", "productchangerequest");
	ticket.setProperty("ticketstatus", "open");
	ticket.setProperty("notes", "Product change request.");
	ticket.setSourcePath("${inReq.getUserProfile().getId()}");
	
	//THESE FIELDS ARE ACTUALLY PRODUCT FIELDS!
	String[] fields = inReq.getRequestParameters("field");
	ticketsearcher.updateData(inReq, fields, ticket);
	ticketsearcher.saveData(ticket, inReq.getUser());
	inReq.putPageValue("ticket", ticket);

	Searcher tickethistorysearcher = archive.getSearcher("tickethistory"); 
	Data ticketHistory = tickethistorysearcher.createNewData();
	ticketHistory.setId(tickethistorysearcher.nextId());
	ticketHistory.setSourcePath(ticket.getId());
	
	ticketHistory.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
	ticketHistory.setProperty("ticket", ticket.getId());
	ticketHistory.setProperty("owner", ticket.get("owner"));
	ticketHistory.setProperty("product", ticket.get("product"));
	ticketHistory.setProperty("tickettype", ticket.get("tickettype"));
	ticketHistory.setProperty("ticketstatus", ticket.get("ticketstatus"));
	ticketHistory.setProperty("notes", ticket.get("notes"));
	tickethistorysearcher.saveData(ticketHistory, inReq.getUser());

	Searcher userprofilesearcher = archive.getSearcher("userprofile");
	Data owner = userprofilesearcher.searchById(ticket.get("owner"))
	if (owner != null) {
		ArrayList email = new ArrayList();
		email.add(owner.get("email"));
		String templatePage = "/ecommerce/views/modules/ticket/workflow/user-notification-template.html";
		sendEmail(context, email, templatePage);
		email = null;
	}
	
	ArrayList emaillist = new ArrayList();
	HitTracker results = userprofilesearcher.fieldSearch("ticketadmin", "true");
	for(Iterator detail = results.iterator(); detail.hasNext();) {
		Data userInfo = (Data)detail.next();
		emaillist.add(userInfo.get("email"));
	}
	String templatePage = "/ecommerce/views/modules/ticket/workflow/admin-notification-template.html";
	sendEmail(context, emaillist, templatePage);

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


init();