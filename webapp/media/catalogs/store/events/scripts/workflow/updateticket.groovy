package workflow

import org.entermedia.email.ElasticPostMail
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
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker
import com.openedit.page.Page

public class UpdateTicket extends EnterMediaObject {
	
	public void init(){
		
		WebPageRequest inReq = context;
		MediaArchive archive = context.getPageValue("mediaarchive");
		Store store = context.getPageValue("store");
		Searcher ticketsearcher = archive.getSearcher("ticket");
		Searcher tickethistorysearcher = archive.getSearcher("tickethistory");
		Searcher userprofilesearcher = archive.getSearcher("userprofile");
		
		//HitTracker hits = inReq.getRequestParameters("")
		
		String productid = inReq.getRequestParameter("productid");
		String ticketid = inReq.getRequestParameter("id");
		
		Product product = store.getProduct(productid);
		Data ticket = ticketsearcher.searchById(ticketid);
		
		if (ticket != null) {
	
			Data owner = userprofilesearcher.searchById(ticket.get("owner"))
				
	//		String ticketStatus = inReq.getRequestParameter("ticketstatus.value");
	//		ticket.setProperty("ticketstatus", ticketStatus);
	//		String notes = inReq.getRequestParameter("notes.value");
	//		ticket.setProperty("notes", notes);
	//		String ticketRequestType = inReq.getRequestParameter("tickettype.value");
	//		ticket.setProperty("tickettype", ticketRequestType);
	//	
	//		ticketsearcher.saveData(ticket, inReq.getUser());
	
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
				sendEmail(archive, context, email, templatePage);
				email = null;
			}
			
			ArrayList emaillist = new ArrayList();
			HitTracker results = userprofilesearcher.fieldSearch("ticketadmin", "true");
			for(Iterator detail = results.iterator(); detail.hasNext();) {
				Data userInfo = (Data)detail.next();
				emaillist.add(userInfo.get("email"));
			}
			String templatePage = "/ecommerce/views/modules/ticket/workflow/admin-notification-template.html";
			sendEmail(archive, context, emaillist, templatePage);
	
		} else {
			throw new OpenEditException("Null Ticket: " + ticketid)
		}
	}
	protected void sendEmail(MediaArchive archive, WebPageRequest context, List email, String templatePage){
		Page template = pageManager.getPage(templatePage);
		WebPageRequest newcontext = context.copy(template);
		ElasticPostMail mailer = new ElasticPostMail();
		String from = "info@wirelessarea.ca";
		mailer.setSmtpUsername(from);
		String passwd = "B25a7GWBub12";
		mailer.setSmtpPassword(passwd);
		String server = "mail.wirelessarea.ca";
		mailer.setSmtpServer(server);
		String port = "587";
		mailer.setPort(Integer.parseInt(port));
		mailer.setSmtpSecured(true);
		
		String[] recipients = new String[email.size()];
		int i=0;
		for(String s: email){
		  recipients[i++] = s;
		}
		String subject = "Ticket Generated";
		mailer.postMail(recipients, subject, template.toString(), template.toString(), from);
	}
}
logs = new ScriptLogger();
logs.startCapture();

try {
	UpdateTicket updateTicket = new UpdateTicket();
	updateTicket.setLog(logs);
	updateTicket.setContext(context);
	updateTicket.setModuleManager(moduleManager);
	updateTicket.setPageManager(pageManager);
	updateTicket.init();
}
finally {
	logs.stopCapture();
}
