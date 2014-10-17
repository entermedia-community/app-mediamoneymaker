package orders;

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.entermedia.email.TemplateWebEmail
import org.openedit.Data
import org.openedit.data.*
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.util.CSVWriter
import org.openedit.repository.filesystem.StringItem
import org.openedit.store.CartItem
import org.openedit.store.customer.Address
import org.openedit.store.orders.Order
import org.openedit.store.util.MediaUtilities

import com.openedit.OpenEditException
import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.GroovyScriptRunner
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.Page
import com.openedit.page.manage.PageManager


public class ExportRogersCsv extends EnterMediaObject {

	private String orderID;
	private List fullOrderList;


	public void setOrderID( String inOrderID ) {
		orderID = inOrderID;
	}
	public List getOrderList() {
		if (fullOrderList == null) {
			this.fullOrderList = new ArrayList();
		}
		return this.fullOrderList;
	}

	public void doExport() {

		log.info("PROCESS: START Orders.ExportRogersCSV");

		fullOrderList = new ArrayList();

		MediaUtilities media = new MediaUtilities();
		media.setContext(context);

		//Get Media Info
		Log log = LogFactory.getLog(GroovyScriptRunner.class);
		MediaArchive archive = context.getPageValue("mediaarchive");
		String catalogid = getMediaArchive().getCatalogId();
		boolean production = Boolean.parseBoolean(context.findValue('productionmode'));

		// Create the Searcher Objects to read values!
		SearcherManager searcherManager = archive.getSearcherManager();
		Searcher ordersearcher = media.getOrderSearcher();
		HitTracker orderList = null;
		
		String sessionid = media.getContext().getRequestParameter("hitssessionid");
		if(sessionid != null) {
			orderList = media.getContext().getSessionValue(sessionid);
		}
		String mode = context.findValue("mode");
		if(orderList == null){
			String days = context.findValue("days");
			SearchQuery q = ordersearcher.createSearchQuery();
			if(days != null){
				int tosubtract = Integer.parseInt(days);
							Calendar calendar = new GregorianCalendar();
				calendar.add(Calendar.DAY_OF_MONTH, tosubtract);
				q.addAfter("orderdate", calendar.getTime());
			}
		
			
			orderList = ordersearcher.search(q);
			orderList.setAllSelected(true);
		
		}
		
		log.info("Found # of Orders:" + orderList.getSelectedHits().size());

		//Create the CSV Writer Objects
		StringWriter output  = new StringWriter();
		CSVWriter writer  = new CSVWriter(output, (char)',');

		//Write the Header Line
		createHeader(writer);

		for (Iterator orderIterator = orderList.getSelectedHits().iterator(); orderIterator.hasNext();) {

			Data currentOrder = orderIterator.next();

			Order order = ordersearcher.searchById(currentOrder.getId());
			if (order == null) {
				throw new OpenEditException("Invalid Order");
			}

			log.info("DATA: Order found: " + order.getId());

			//Check if order has Affinity Products
			List orderitems = order.getItems()
			if (orderitems.size() > 0) {

				//Write the body of all of the orders
				createOrderDetails(order, writer);
			}
		}
		String finalout = output.toString();
		context.putPageValue("exportcsv", finalout);
		String filename = context.findValue("filename");
		
		Page target = getPageManager().getPage("/WEB-INF/tempfiles/${filename}");
		getPageManager().removePage(target);
		
		writeOrderToFile(target, output, "report.csv");
		File file = new File(target.getContentItem().getAbsolutePath());
		ArrayList attachments = new ArrayList();
		ArrayList recipients = new ArrayList();
		recipients.add("ian@ijsolutions.ca");
		attachments.add(file);
		if(context.findValue("sendemail") == "true"){
		sendEmail(getContext(), recipients, "/ecommerce/reportemail.html", context.findValue("subject"), attachments)
		}
		
		
		
	}

	private void createOrderDetails(Order order, CSVWriter writer) {

		String mode = context.findValue("reportmode");
		if(mode == "corporate"){
			if(order.getId().startsWith("WEB")) {
				return;
			}
		}
		//Set up result
		boolean result = false;

		MediaArchive archive = context.getPageValue("mediaarchive");
		SearcherManager manager = archive.getSearcherManager();
		String catalogid = archive.getCatalogId();
		Address shipToAddress = order.getShippingAddress();

		boolean saveAS400Po = false;
		String as400po = order.get("rogersponumber");


		String batchId = order.get("batchid");
		if (as400po == null || as400po.trim().isEmpty()){
			as400po = null;
			Searcher as400searcher = archive.getSearcher("as400");
			if (batchId != null && batchId.trim().isEmpty() == false){
				SearchQuery query = as400searcher.createSearchQuery();
				query.addMatches("batchid",batchId);
				HitTracker hits = as400searcher.search(query);
				for(int i = 0; i < hits.size(); i ++){
					Data d = hits.get(i);
					if (d.get("as400po")!=null && d.get("as400po").trim().isEmpty() == false){
						as400po = d.get("as400po");
						saveAS400Po = true;
						break;
					}
				}
			}
		}
		
		


		for(Iterator i = order.getItems().iterator(); i.hasNext();) {
			//Get the cart Item
			CartItem item = i.next();
			List orderDetailRow = new ArrayList();
			
			
			
			
			//Add the Order ID
			orderDetailRow.add(item.getProduct().get("rogerssku"));
			orderDetailRow.add(item.getProduct().get("name"));

			Searcher catasearcher = manager.getSearcher(catalogid, "categoryid");
			Data category = catasearcher.searchById(item.getProduct().get("categoryid"));
			if (category != null) {
				orderDetailRow.add(category.get("name"));
			} else {
				orderDetailRow.add("");
			}
			if (order.getId().startsWith("WEB")) {
				orderDetailRow.add("WEB");
			} else {
				orderDetailRow.add("CORPORATE");
			}
			orderDetailRow.add(order.orderstatus);

			orderDetailRow.add(order.getId());
			orderDetailRow.add(shipToAddress.getId());
			
			//Get Company
			if (order.getCustomer().company != null) {
				orderDetailRow.add(order.getCustomer().company);
			} else {
				orderDetailRow.add(order.getCustomer().firstName + " " + order.getCustomer().lastName);
			}
			
			
			//Get Address
			String address = shipToAddress.getAddress1();
			if (shipToAddress.getAddress2() != null) {
				address += ", " + shipToAddress.getAddress2();
			}
			orderDetailRow.add(address);

			address = shipToAddress.getCity() + ", " +
					shipToAddress.getState() + ", " +
					shipToAddress.getZipCode();
			orderDetailRow.add(address);

			//Bill to
			if (order.getOrderStatus().getId().equals("accepted"))
			{
				Address billing = order.getBillingAddress();
				if (billing == null) {
					throw new OpenEditException("Invalid Billing Address (populateHeader) (" + order.getId() + ")");
				}
				if (order.getCustomer().company != null) {
					orderDetailRow.add(order.getCustomer().company);
				} else {
					orderDetailRow.add(order.getCustomer().firstName + " " + order.getCustomer().lastName);
				}
				address = billing.getAddress1();
				if (billing.getAddress2() != null) {
					address += ", " + billing.getAddress2();
				}
				orderDetailRow.add(address);
				address = billing.getCity() + ", " +
						billing.getState() + ", " +
						billing.getZipCode();
				orderDetailRow.add(address);
			}
			else
			{
				orderDetailRow.add("Area Communications");
				orderDetailRow.add("Area Marketing, 1 Hurontario Street, Suite 220");
				orderDetailRow.add("Mississauga, ON, L5G 0A3, CA");
			}


			//Get Distributor
			Searcher distribsearcher = manager.getSearcher(catalogid, "distributor");
			if (item.getProduct().get("distributor") != null){
				Data distributor = distribsearcher.searchById(item.getProduct().get("distributor"));
				if (distributor!=null){
					orderDetailRow.add(distributor.get("name"));
				} else {
					orderDetailRow.add("[unknown]");
				}
			} else {
				orderDetailRow.add("[unknown]");
			}



			//Get Order Info
			orderDetailRow.add(order.getDate().toString());

			//Get Display Designation
			Searcher displaysearcher = manager.getSearcher(catalogid, "displaydesignationid");
			Data displayDesignation = displaysearcher.searchById(item.getProduct().get("displaydesignationid"));
			if (displayDesignation != null) {
				orderDetailRow.add(displayDesignation.get("name"));
			}
			else {
				orderDetailRow.add("None");
			}


			if (order.isFullyShipped(item)) {
				orderDetailRow.add("Shipped")
			} else if (order.getQuantityShipped(item) > 0) {
				orderDetailRow.add("Partially Shipped");
			} else {
				orderDetailRow.add("Not Shipped");
			}

			Date date = order.getLastDateShipped(item);
			if(date != null){
				orderDetailRow.add(date.toString());
			}else{
			orderDetailRow.add("");
			}
			orderDetailRow.add(item.getQuantity().toString());
			//price fields
			String rogersprice = item.getProduct().get("rogersprice");
			if (rogersprice == null){
				rogersprice = "0.00";
			}
			String wholesale = item.getProduct().get("wholesaleprice");
			if (wholesale == null){
				wholesale = "0.00";
			}
			String price = "0.00";
			//			orderDetailRow.add('$'+ item.getProduct().get("rogersprice").toString());
			orderDetailRow.add("\$${rogersprice}");
			orderDetailRow.add(item.getYourPrice().toString());
			//			orderDetailRow.add('$' + item.getProduct().get("wholesaleprice")).toString();
			orderDetailRow.add("\$${wholesale}");

			if (displayDesignation != null) {
				if (displayDesignation.get("name").equalsIgnoreCase("fido")) {
					//					orderDetailRow.add('$' + item.getProduct().get("fidomsrp"));
					price = item.getProduct().get("fidomsrp");
				} else {
					//					orderDetailRow.add('$' + item.getProduct().get("msrp"));
					price = item.getProduct().get("msrp");
				}
			} else {
				//				orderDetailRow.add('$' + item.getProduct().get("msrp"));
				price = item.getProduct().get("msrp");
			}
			orderDetailRow.add("\$${price}");
			boolean isApproved = Boolean.parseBoolean(item.getProduct().get("approved"));
			orderDetailRow.add(isApproved ? "Yes" : "No");
			//add as400 po
			orderDetailRow.add(as400po == null ? "" : as400po);

			String targettype = order.as400ordertype;
			String as400id = null;
			
			if(targettype == "fido"){
				as400id = item.getProduct().fidoas400id;
			} else{
				as400id = item.getProduct().rogersas400id;
			}
			orderDetailRow.add(as400id);
			
			//Write Row to Writer
			writeRowToWriter(orderDetailRow, writer);
			log.info(orderDetailRow.toString());
			orderDetailRow = null;
		}

		//update as400 po if required
		if (saveAS400Po){
			//			order.setProperty("rogersponumber",as400po);
			//			store.saveOrder(order);
		}
	}

	private createHeader(CSVWriter writer) {
		List headerRow = new ArrayList();
		//Add the Order Number
		headerRow.add("SKU");
		headerRow.add("Item Description");
		headerRow.add("Category");
		headerRow.add("Order Type");
		headerRow.add("Order Status");

		headerRow.add("Order Number");
		headerRow.add("Rogers Store Number");
		
		headerRow.add("Customer Name");
		headerRow.add("Address1");
		headerRow.add("Address2");

		headerRow.add("Bill To");
		headerRow.add("Address1");
		headerRow.add("Address2");


		headerRow.add("Distributor");
		headerRow.add("Order Date");
		headerRow.add("Brand");
		headerRow.add("Order Shipping Status");
		headerRow.add("Order Shipping Date");
		
		headerRow.add("Ordered Quantity");
		headerRow.add("Rogers Cost");
		headerRow.add("Dealer Cost");
		headerRow.add("Area Cost");

		headerRow.add("MSRP");
		headerRow.add("ROGERS APPROVED?");
		headerRow.add("AS400 PO");
		headerRow.add("AS400 SKU");
		
		log.info(headerRow.toString());
		writeRowToWriter(headerRow, writer)
	}

	private void writeRowToWriter( List inRow, CSVWriter writer) {

		String[] nextrow = new String[inRow.size()];
		for ( int headerCtr=0; headerCtr < inRow.size(); headerCtr++ ){
			nextrow[headerCtr] = inRow.get(headerCtr);
		}
		try	{
			writer.writeNext(nextrow);
		}
		catch (Exception e) {
			log.info(e.message);
			log.info(e.getStackTrace().toString());
		}
	}

	private void writeOrderToFile(Page page, StringWriter output, String fileName) {

		page.setContentItem(new StringItem(page.getPath(), output.toString(), "UTF-8"));

		//Write out the CSV file.
		pageManager.putPage(page);

	}
	protected void sendEmail( WebPageRequest context, List inEmailList, String templatePage, String inSubject, List attachments){
		PageManager pageManager = getPageManager();
		Page template = pageManager.getPage(templatePage);
		WebPageRequest newcontext = context.copy(template);
		TemplateWebEmail mailer = getModuleManager().getBean("postMail").getTemplateWebEmail();
		mailer.loadSettings(newcontext);
		mailer.setMailTemplatePath(templatePage);
		mailer.setFrom("info@wirelessarea.ca");
		mailer.setRecipientsFromStrings(inEmailList);
		mailer.setSubject(inSubject);
		mailer.setFileAttachments(attachments)
		mailer.send();
	}

	
	
}

boolean result = false;

logs = new ScriptLogger();
logs.startCapture();

try {
	ExportRogersCsv exportOrders = new ExportRogersCsv();
	exportOrders.setLog(logs);
	exportOrders.setContext(context);
	exportOrders.setOrderID(context.getRequestParameter("orderid"));
	exportOrders.setModuleManager(moduleManager);
	exportOrders.setPageManager(pageManager);
	

	exportOrders.doExport();
	log.info("The following file(s) has been created. ");
	log.info(exportOrders.getOrderList().toString());
	log.info("PROCESS: END Orders.ExportRogersCSV");
	context.putPageValue("export", exportOrders.getOrderList().toString());
}
finally {
	logs.stopCapture();
}
