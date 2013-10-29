import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.util.CSVReader
import org.openedit.store.Product
import org.openedit.store.Store
import org.openedit.store.orders.OrderSearcher;

import com.openedit.page.Page
import com.openedit.util.FileUtils

import org.entermedia.email.PostMail
import org.entermedia.email.TemplateWebEmail
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.store.Store
import org.openedit.store.orders.Order

import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.Page

import java.util.concurrent.TimeUnit

public void init()
{
	log.info("Starting daily summary of pending orders");
	//Create the MediaArchive object
	Store store = context.getPageValue("store");
	MediaArchive archive = context.getPageValue("mediaarchive");
	
	//Create the searcher objects
	OrderSearcher ordersearcher = store.getOrderSearcher();
	Searcher userprofilesearcher = archive.getSearcher("userprofile");
	
	SearchQuery query = ordersearcher.createSearchQuery();
	query.setAndTogether(false);
	//order statuses: received,accepted,open,authorized,completed,captured,rejected,closed
	query.addMatches("orderstatus","accepted");//accepted - client requests an invoice
	query.addMatches("orderstatus", "authorized");//authorized - payment processed
	query.addSortBy("orderdateDown");
	
	ArrayList<String[]> list = new ArrayList<String[]>();
	
	HitTracker hits = ordersearcher.search(query);
	hits.each{
		Order order = ordersearcher.searchById(it.id);
		String shippingstatus = order.get("shippingstatus");//shipped,partialshipped
		if (shippingstatus == null || shippingstatus.equals("partialshipped"))
		{
			long milli = System.currentTimeMillis() - order.getDate().getTime();
			long hours = TimeUnit.HOURS.convert(milli, TimeUnit.MILLISECONDS);
			
			String alertstatus = "#E42217";//red
			String img = "attention-red.png";
			if (hours < 48){
				alertstatus = "#FFF5EE";//white
				img = "attention-green.png";
			} else if (hours >= 48 && hours < 72){
				alertstatus = "#FFF380";//yellow
				img = "attention-yellow.png";
			}
			
			String date = context.getDate(order.getDate());
			String number = order.getId();
			String customerid = order.getCustomer().getId();
			String customer = "${order.getCustomer().getFirstName()} ${order.getCustomer().getLastName()}";
			String company = " ${order.getCustomer().getCompany()}";
			String items = "${order.getItems().size()}";
			String shipping = shippingstatus == null ? "" : shippingstatus;
			String price = "${order.getTotalPrice()}";
			
			String[] info = [img,date,number,customerid,customer,company,items,shipping,price] as String[];
			list.add(info);
		}
	}
	//create headers
	String [] headers = ["Level","Date","Order Number", "Customer ID", "Customer Name", "Company", "Number of Items", "Shipping Status", "Total Price"] as String[];
	
	//add header and rows to context
	context.putPageValue("rows", list);
	context.putPageValue("headers",headers);
	//check for inventory dates
	checkOther(archive,context);
	
	if("TRUE".equalsIgnoreCase(context.findValue("sendemail"))){
		//get list of storeadmins
		ArrayList emails = new ArrayList();
		HitTracker results = userprofilesearcher.fieldSearch("storeadmin", "true");
		if (results.size() > 0) {
			for(Iterator detail = results.iterator(); detail.hasNext();) {
				Data userInfo = (Data)detail.next();
				emails.add(userInfo.get("email"));
			}
		}
		if (emails.isEmpty())
		{
			log.info("No store administrators found on the system, aborting");
			return;
		}
		log.info("Sending emails to the following store administrators: ${emails}");
		PostMail mail = (PostMail)archive.getModuleManager().getBean( "postMail");
		String templatePage = "/ecommerce/views/modules/storeOrder/notifications/dailysummary/email-notification-template.html";
		Page template = pageManager.getPage(templatePage);
		WebPageRequest newcontext = context.copy(template);
		TemplateWebEmail mailer = mail.getTemplateWebEmail();
		mailer.setFrom("info@wirelessarea.ca");
		mailer.loadSettings(newcontext);
		mailer.setMailTemplatePath(templatePage);
		mailer.setRecipientsFromStrings(emails);
		mailer.setSubject("Daily Orders Summary");
		mailer.send();
		log.info("Finishing sending daily summary to store admins");
	} else {
		log.info("Finishing running daily summary");
	}
}

public void checkOther(MediaArchive archive, WebPageRequest inReq){
	Date date = new Date();
	date.setTime(System.currentTimeMillis() - (48*60*60*1000));//2 days ago
	SearcherManager sm = archive.getSearcherManager();
	String catalogid = archive.getCatalogId();
	Searcher searcher = sm.getSearcher(catalogid, "product");
	SearchQuery query = searcher.createSearchQuery();
	query.addBefore("inventoryupdated", date);
	query.addExact("active","true");
	HitTracker hits = searcher.search(query);
	if (!hits.isEmpty()){
		//show distributor, filter whether is active
		inReq.putPageValue("hits", hits);
	}
}

init();