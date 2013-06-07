package inventory

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.entermedia.email.PostMail
import org.entermedia.email.TemplateWebEmail
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.store.InventoryItem
import org.openedit.store.Product
import org.openedit.store.util.MediaUtilities
import org.openedit.util.DateStorageUtil

import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.GroovyScriptRunner
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker
import com.openedit.page.Page
import com.openedit.users.User

public class CheckNotifications extends EnterMediaObject {
	
	List<String> goodlist;
	List<String> badlist;
	
	private void addToGoodList(String inMsg) {
		if (goodlist == null) {
			goodlist = new ArrayList<String>();
		}
		goodlist.add(inMsg);
	}
	public List getGoodList() {
		if (goodlist == null) {
			goodlist = new ArrayList<String>();
		}
		return goodlist;
	}
	
	private void addToBadList(String inMsg) {
		if (badlist == null) {
			badlist = new ArrayList<String>();
		}
		badlist.add(inMsg);
	}
	public List getBadList() {
		if (badlist == null) {
			badlist = new ArrayList<String>();
		}
		return badlist;
	}
	public void doCheck() {
		
		//Get Media Info
		Log log = LogFactory.getLog(GroovyScriptRunner.class);
		
		WebPageRequest inReq = context;

		MediaUtilities media = new MediaUtilities();
		media.setContext(context);
		MediaArchive archive = media.getArchive();
		SearcherManager manager = media.getManager();
		String catalogid = media.getCatalogid();
		Searcher notifysearcher = manager.getSearcher(catalogid, "stocknotify");
		Searcher productsearcher = media.getProductSearcher();
		Searcher userprofilesearcher = media.getUserProfileSearcher();
		Searcher usersearcher = media.getUserSearcher();
		Searcher tickethistorysearcher = archive.getSearcher("tickethistory");
		Searcher ticketsearcher = archive.getSearcher("ticket");
		
		HitTracker hits = notifysearcher.fieldSearch("notificationsent", "false");
		if (hits.size() > 0) {
			for (Iterator notifyIterator = hits.iterator(); notifyIterator.hasNext();) {
				//Read Each Notification
				Data hit = (Data) notifyIterator.next();
	//			//Load the notification
	//			Data notify = notifysearcher.searchById(hit.getId());
				String productID = hit.get("product"); 
				Product prod = productsearcher.searchById(productID);
				InventoryItem item = prod.getInventoryItem(0);
				Integer qtyInStock = item.getQuantityInStock();
				if (qtyInStock > 0) {
					
					//Load the fully qualified hit
					Data notify = notifysearcher.searchById(hit.getId());
					notify.setProperty("notificationsent", "true");
					Date now = new Date();
					notify.setProperty("inventoryupdated", DateStorageUtil.getStorageUtil().formatForStorage(now));
					notifysearcher.saveData(notify, inReq.getUser());
					
					String ticketID = hit.get("ticketid");
					Data ticket = ticketsearcher.searchById(ticketID);
					if (ticket != null) {
						ticket.setProperty("ticketstatus","closed");
						ticketsearcher.saveData(ticket, inReq.getUser());
						
						Data ticketHistory = tickethistorysearcher.createNewData();
						ticketHistory.setId(tickethistorysearcher.nextId());
						ticketHistory.setSourcePath(ticketID);
						
						ticketHistory.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
						ticketHistory.setProperty("ticket", ticketID);
						ticketHistory.setProperty("owner", ticket.get("owner"));
						ticketHistory.setProperty("ticketstatus", ticket.get("ticketstatus"));
						ticketHistory.setProperty("notes", "Stock has been updated");
						tickethistorysearcher.saveData(ticketHistory, inReq.getUser());
					} else {
						log.info("Ticket does not exist: " + ticketID);
					}
										
					//Inventory Item has been updated! Notify customer
					String userID = hit.get("owner");
					User user = usersearcher.searchById(userID);
					String email = user.getEmail();
					inReq.putPageValue("userid", user.getId());
					if (ticket != null) {
						inReq.putPageValue("ticketid", ticket.getId());
					}
					
					String subject = "Product Notification and Support Ticket Update";
					String templatePage = "/ecommerce/views/modules/product/workflow/product-update-user-template.html";
					String outMsg = "Inventory levels have been updated and the following product is now available: " + prod.get("name");
					inReq.putPageValue("outmsg", outMsg);
					sendEmail(archive, context, email, templatePage, subject);
					addToGoodList(user.firstName + " " + user.lastName + " has been notified by email.");
					email = null;
				} else {
					String userID = hit.get("owner");
					User user = usersearcher.searchById(userID);
					String msg = user.firstName + " " + user.lastName + " has not been notified by email. ";
					msg += "Product(" + prod.get("name") + ":" + prod.getId() + ") is still out of stock."
					addToBadList(msg);
				}
			}
		} else {
			addToBadList("There are no notifications to send.");
		}
		inReq.putPageValue("goodlist", getGoodlist());
		inReq.putPageValue("badlist", getBadList());
	}
	
	protected void sendEmail(MediaArchive archive, WebPageRequest context, String inEmailList, String templatePage, String inSubject){
		Page template = pageManager.getPage(templatePage);
		WebPageRequest newcontext = context.copy(template);
		TemplateWebEmail mailer = getMail(archive);
		mailer.loadSettings(newcontext);
		mailer.setMailTemplatePath(templatePage);
		mailer.setFrom("info@wirelessarea.ca");
		mailer.setRecipientsFromCommas(inEmailList);
		mailer.setSubject(inSubject);
		mailer.send();
	}
	
	protected TemplateWebEmail getMail(MediaArchive archive) {
		PostMail mail = (PostMail)archive.getModuleManager().getBean( "postMail");
		return mail.getTemplateWebEmail();
	}
}
log = new ScriptLogger();
log.startCapture();

try {
	log.info("*** START EMAIL NOTIFICATIONS ***");
	CheckNotifications checkNotifications = new CheckNotifications();
	checkNotifications.setLog(log);
	checkNotifications.setContext(context);
	checkNotifications.setModuleManager(moduleManager);
	checkNotifications.setPageManager(pageManager);

	checkNotifications.doCheck();
	log.info("*** END EMAIL NOTIFICATIONS ***");
}
finally {
	log.stopCapture();
}

