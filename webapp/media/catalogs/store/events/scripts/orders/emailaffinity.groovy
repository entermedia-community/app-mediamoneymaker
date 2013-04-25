package orders;

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.entermedia.email.PostMail
import org.entermedia.email.TemplateWebEmail
import org.openedit.data.*
import org.openedit.entermedia.MediaArchive

import com.openedit.OpenEditException
import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.GroovyScriptRunner
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.page.Page
import com.openedit.page.manage.PageManager;


public class EmailAffinity extends EnterMediaObject {

	private static String distributorName = "Affinity";
	private String orderID;

	public void setOrderID( String inOrderID ) {
		orderID = inOrderID;
	}
	public String getOrderID() {
		return this.orderID;
	}

	public void doExport() {

		String finalOutput = "";
		log.info("PROCESS: START Orders.exportaffinity");

		Log log = LogFactory.getLog(GroovyScriptRunner.class);
		MediaArchive archive = context.getPageValue("mediaarchive");
		String catalogid = getMediaArchive().getCatalogId();
		boolean production = Boolean.parseBoolean(context.findValue('productionmode'));

		// Create the Searcher Objects to read values!
		String inOrderID = context.getRequestParameter("id");
		setOrderID(inOrderID);
		def String distributorName = "Affinity";
		
		// xml generation
		String fileName = "export-" + this.distributorName.replace(" ", "-") + "-" + getOrderID() + ".csv";
		String pageName = "/WEB-INF/data/" + catalogid + "/orders/exports/" + getOrderID() + "/" + fileName;
		Page page = pageManager.getPage(pageName);
		if (page.exists()) {
			List email = new ArrayList();
			email.add("pf@area.ca");
			List<String> attachments = new ArrayList<String>();
			attachments.add(page.getPath());
			
			context.putPageValue("orderid", getOrderID());
			String templatePage = "/ecommerce/rogers/orders/email-template.html";
			Page tempPage = pageManager.getPage(templatePage);
			String tempPath = tempPage.getPath();
			if (tempPage.exists()) {
				sendEmail(archive, context, email, attachments, tempPath, getOrderID());
				context.putPageValue("id", getOrderID());
			} else {
				context.putPageValue("errorout", tempPath + " does not exist.");
				return;
			}
		} else {
			context.putPageValue("errorout", "CSV (" + page.getPath() + ") cannot be found.");
		}
		return;
	}
	protected void sendEmail(MediaArchive archive, WebPageRequest context, List email, List attachments, String templatePage, String orderID){
		Page template = pageManager.getPage(templatePage);
		WebPageRequest newcontext = context.copy(template);
		TemplateWebEmail mailer = getMail(archive);
		mailer.setFrom("info@wirelessarea.ca");
		mailer.setFileAttachments(attachments);
		mailer.loadSettings(newcontext);
		mailer.setMailTemplatePath(templatePage);
		mailer.setRecipientsFromCommas(email);
		mailer.setSubject("Process Web Order (" + orderID + ")");
		mailer.send();
	}
	protected TemplateWebEmail getMail(MediaArchive mediaarchive) {
		PostMail mail = (PostMail)mediaarchive.getModuleManager().getBean( "postMail");
		return mail.getTemplateWebEmail();
	}

}
logs = new ScriptLogger();
logs.startCapture();

try {
	EmailAffinity emailAffinity = new EmailAffinity();
	emailAffinity.setLog(logs);
	emailAffinity.setContext(context);
	emailAffinity.setOrderID(context.getRequestParameter("orderid"));
	emailAffinity.setModuleManager(moduleManager);
	emailAffinity.setPageManager(pageManager);
	emailAffinity.doExport();
}
finally {
	logs.stopCapture();
}
