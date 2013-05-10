package workflow

import org.entermedia.email.PostMail
import org.entermedia.email.TemplateWebEmail
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
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
import com.openedit.users.User

public class UpdateTicket extends EnterMediaObject {
	
	public void init(){
		
		WebPageRequest inReq = context;
		MediaArchive archive = context.getPageValue("mediaarchive");
		Store store = context.getPageValue("store");
		SearcherManager manager = archive.getSearcherManager();
		Searcher ticketsearcher = archive.getSearcher("ticket");
		Searcher tickethistorysearcher = archive.getSearcher("tickethistory");
		Searcher userprofilesearcher = archive.getSearcher("userprofile");
		Searcher usersearcher = manager.getSearcher("system", "user");
		
		//HitTracker hits = inReq.getRequestParameters("")
		
		String productid = inReq.getRequestParameter("productid");
		String ticketid = inReq.getRequestParameter("id");
		
		Product product = store.getProduct(productid);
		Data ticket = ticketsearcher.searchById(ticketid);
		
		if (ticket != null) {
	
			Data owner = userprofilesearcher.searchById(ticket.get("owner"))
				
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
	
			User user = usersearcher.searchById(owner.getId());
			inReq.putPageValue("userid", user.getId());
			
			String subject = "New Support Ticket: " + ticket.getId();
			if (owner != null) {
				ArrayList email = new ArrayList();
				email.add(user.getEmail());	
				String templatePage = "/ecommerce/views/modules/ticket/workflow/user-notification-template.html";
				sendEmail(archive, context, email, templatePage, subject);
				log.info("Email sent to user (" + user.getFirstName() + " " + user.getLastName() + ")");
				email = null;
			}
			ArrayList emaillist = new ArrayList();
			HitTracker results = userprofilesearcher.fieldSearch("ticketadmin", "true");
			if (results.size() > 0) {
				for(Iterator detail = results.iterator(); detail.hasNext();) {
					Data userInfo = (Data)detail.next();
					emaillist.add(userInfo.get("email"));
				}
				String templatePage = "/ecommerce/views/modules/ticket/workflow/admin-notification-template.html";
				sendEmail(archive, context, emaillist, templatePage, subject);
				log.info("Email sent to Ticket Admins");
			}
		} else {
			throw new OpenEditException("Null Ticket: " + ticketid)
		}
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
