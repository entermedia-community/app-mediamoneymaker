package workflow

import org.entermedia.email.PostMail
import org.entermedia.email.TemplateWebEmail
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.store.Product
import org.openedit.store.Store
import org.openedit.util.DateStorageUtil

import com.openedit.OpenEditException
import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.page.Page
	
public void init(){
	
	WebPageRequest inReq = context;
	MediaArchive archive = context.getPageValue("mediaarchive");
	Store store = context.getPageValue("store");
	Searcher ticketsearcher = archive.getSearcher("ticket");
	Searcher tickethistorysearcher = archive.getSearcher("tickethistory");
	Searcher userprofilesearcher = archive.getSearcher("userprofile");
	
	String productid = inReq.getRequestParameter("productid");
	String ticketid = inReq.getRequestParameter("ticketid");
	
	Product product = store.getProduct(productid);
	Data ticket = ticketsearcher.searchById(ticketid);
	
	if (ticket != null) {

		Data owner = userprofilesearcher.searchById(ticket.get("owner"))
			
		String ticketStatus = inReq.getRequestParameter("ticketstatus");
		ticket.setProperty("ticketstatus", ticketStatus);
		String notes = inReq.getRequestParameter("notes");
		ticket.setProperty("notes", notes);
	
		ticketsearcher.saveData(ticket, inReq.getUser());

		Data ticketHistory = tickethistorysearcher.createNewData();
		ticketHistory.setId(tickethistorysearcher.nextId());
		ticketHistory.setSourcePath(ticketid);
		
		ticketHistory.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
		ticketHistory.setProperty("ticket", ticketid);
		ticketHistory.setProperty("owner", ticket.get("owner"));
		ticketHistory.setProperty("product", ticket.get("product"));
		ticketHistory.setProperty("tickettype", ticket.get("tickettype"));
		ticketHistory.setProperty("ticketstatus", ticket.get("ticketstatus"));
		ticketHistory.setProperty("notes", ticket.get("notes"));
		tickethistorysearcher.saveData(ticketHistory, inReq.getUser());
		
		inReq.putPageValue("ticketid", ticketid);
		inReq.putPageValue("productid", productid);
		inReq.putPageValue("ticket", ticket);
		inReq.putPageValue("product", product)
		inReq.putPageValue("tickethistoryid", ticketHistory.getId());

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

	} else {
		throw new OpenEditException("Null Ticket: " + ticketid)
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
init();
