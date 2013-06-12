package inventory

import org.entermedia.email.PostMail
import org.entermedia.email.TemplateWebEmail
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.store.util.MediaUtilities

import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker
import com.openedit.page.Page

public class InventoryReport extends EnterMediaObject {

	List<String> badProductList;
	List<String> goodProductList;
	List<String> badUPCList;
	List<String> foundUPCList;
	int totalRows;

	public void doCheck() {
		
		WebPageRequest inReq = context;
		MediaArchive archive = context.getPageValue("mediaarchive");
		SearcherManager manager = archive.getSearcherManager();
		String catalogID = archive.getCatalogId();
		SearcherManager searcherManager = archive.getSearcherManager();

		String strMsg = "";
	
		//Create the searcher objects
		Searcher productsearcher = searcherManager.getSearcher(catalogID, "products");
		
		String sessionid = inReq.getRequestParameter("hitssessionid");
		if (sessionid == null) {
			return;
		}
		HitTracker productList = inReq.getSessionValue(sessionid);
		if (productList == null) {
			return;
		}
		log.info("Found # of Products:" + productList.getSelectedHits().size());
		

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
log = new ScriptLogger();
log.startCapture();

try {

	log.info("START - InventoryReport");
	InventoryReport inventoryReport = new InventoryReport();
	inventoryReport.setLog(log);
	inventoryReport.setContext(context);
	inventoryReport.setModuleManager(moduleManager);
	inventoryReport.setPageManager(pageManager);
	inventoryReport.doCheck();
	log.info("FINISH - InventoryReport");
}
finally {
	log.stopCapture();
}
