package orders


import org.entermedia.email.PostMail
import org.entermedia.email.TemplateWebEmail
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.store.Store
import org.openedit.store.orders.Order

import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.Page
	
public void init(){
	
	WebPageRequest inReq = context;
	MediaArchive archive = context.getPageValue("mediaarchive");
	Store store = context.getPageValue("store");
	Searcher ordersearcher = archive.getSearcher("order");
	SearchQuery query = ordersearcher.createSearchQuery();
	query.setAndTogether(false);
	query.addNot("orderstatus", "complete");
	HitTracker hits = ordersearcher.cachedSearch(inReq);
	Date now = new Date();
	context.putPageValue("now", now);
	Calendar cal = Calendar.getInstance();
	int day = cal.get(cal.DAY_OF_YEAR);
	cal.add(cal.DAY_OF_YEAR, -2);
	Date fortyeight = cal.getTime();
	context.putPageValue("48", fortyeigth);
	hits.each{
		Order order = ordersearcher.searchById(it.id);	
		ArrayList inprogress = new ArrayList();
		ArrayList late = new ArrayList();
		context.putPageValue("late", late);
		if(order.getDate().before(fortyeight)){
			
		}
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
