package quotes

import java.util.List;

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.entermedia.email.TemplateWebEmail;
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.store.Product
import org.openedit.store.util.MediaUtilities
import org.openedit.util.DateStorageUtil

import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.GroovyScriptRunner
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery

import java.text.SimpleDateFormat

import org.entermedia.email.PostMail
import org.entermedia.email.TemplateWebEmail
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.money.Money
import org.openedit.store.CartItem
import org.openedit.store.InventoryItem
import org.openedit.store.Product
import org.openedit.store.Store
import org.openedit.store.orders.Order
import org.openedit.store.orders.Shipment
import org.openedit.store.orders.ShipmentEntry
import org.openedit.store.util.MediaUtilities
import org.openedit.util.DateStorageUtil

import com.openedit.OpenEditException
import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.Page
import com.openedit.page.manage.PageManager

public void init(){
	
	WebPageRequest inReq = context;
	
	MediaArchive archive = context.getPageValue("mediaarchive");
//	SearcherManager manager = archive.getSearcherManager();
//	String catalogid = archive.getCatalogId();
	
//	MediaUtilities media = new MediaUtilities();
//	media.setContext(context);
//	
//	Searcher productSearcher = media.getProductSearcher();
	Searcher userprofilesearcher = archive.getSearcher("userprofile");
	

	//productid, name, email, quantity, details
	String productid = inReq.getRequestParameter("productid");
	String contactname = inReq.getRequestParameter("name");
	String contactemail = inReq.getRequestParameter("email");
	String quantity = inReq.getRequestParameter("quantity");
	String details = inReq.getRequestParameter("details");
	
//	log.info("------------ ${productid} ${contactname} ${contactemail} ${quantity} ${details} --------");
	
//	Data data = productSearcher.searchById(productid);
//	log.info("${data.name}, ${data.sourcepath}");
	
	ArrayList emaillist = new ArrayList();
	HitTracker results = userprofilesearcher.fieldSearch("storeadmin", "true");
	if (results.size() > 0) {
		for(Iterator detail = results.iterator(); detail.hasNext();) {
			Data userInfo = (Data)detail.next();
			emaillist.add(userInfo.get("email"));
		}
	}
	if (emaillist.isEmpty())
	{
		log.info("No store administrators found on the system, aborting");
		emaillist.add("info@shawnbest.com");
//		return;
	}
	
	String templatePage = "/ecommerce/views/myaccount/quotes/email-notification.html";
	String subject = "Request for Quote";
	sendEmail(archive, inReq, emaillist, templatePage, subject);
	log.info("Email has been sent");
}

protected void sendEmail(MediaArchive archive, WebPageRequest context, List inEmailList, String templatePage, String inSubject){
	PageManager pageManager = archive.getPageManager();
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
	PostMail mail = (PostMail)archive.getModuleManager().getBean("postMail");
	return mail.getTemplateWebEmail();
}

init();
