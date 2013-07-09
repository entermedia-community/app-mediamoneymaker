
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
	
	//Create the MediaArchive object
	Store store = context.getPageValue("store");
	MediaArchive archive = context.getPageValue("mediaarchive");
	
	//Create the ordersearcher object
	OrderSearcher ordersearcher = store.getOrderSearcher();
	
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
			if (hours < 48) alertstatus = "#FFF5EE";//white
			else if (hours >= 48 && hours < 72) alertstatus = "#FFF380";//yellow
			
			String date = order.getDate().toString();
			String number = order.getId();
			String customerid = order.getCustomer().getId();
			String customer = "${order.getCustomer().getFirstName()} ${order.getCustomer().getLastName()}";
			String company = " ${order.getCustomer().getCompany()}";
			String items = "${order.getItems().size()}";
			String shipping = shippingstatus == null ? "" : shippingstatus;
			String price = "${order.getTotalPrice()}";
			
			String[] info = [alertstatus,date,number,customerid,customer,company,items,shipping,price] as String[];
			list.add(info);
		}
	}
	
	String [] headers = ["Date","Order Number", "Customer ID", "Customer Name", "Company", "Number of Items", "Shipping Status", "Total Price"] as String[];
	
	//load template email page
	String templatePage = "/ecommerce/views/modules/storeOrder/notifications/dailysummary/email-notification-template.html";
	Page template = pageManager.getPage(templatePage);
	WebPageRequest newcontext = context.copy(template);
	
	newcontext.putPageValue("rows", list);
	newcontext.putPageValue("headers",headers);
	
	PostMail mail = (PostMail)archive.getModuleManager().getBean( "postMail");
	TemplateWebEmail mailer = mail.getTemplateWebEmail();
	mailer.setFrom("noreply@entermediasoftware.com");//change this
	mailer.loadSettings(newcontext);
	mailer.setMailTemplatePath(templatePage);
	mailer.setTo("info@shawnbest.com");//make this configurable
	mailer.setSubject("Daily Orders Summary");
	mailer.send();
}

init();