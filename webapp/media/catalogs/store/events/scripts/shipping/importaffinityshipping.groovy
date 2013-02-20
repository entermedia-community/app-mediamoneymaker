package shipping

import java.text.SimpleDateFormat

import org.entermedia.email.PostMail
import org.entermedia.email.TemplateWebEmail
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.util.CSVReader
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
import com.openedit.page.Page
import com.openedit.util.FileUtils


public class ImportAffinityShipping  extends EnterMediaObject {
	
	List<String> badOrders;
	List<String> goodOrders;
	
	int totalRows;
	static int colOrderLineNumber = 0;
	static int colShipDate = 1;
	static int colOrderNumber = 6;
	static int colPONumber = 8;
	static int colWaybill = 9;
	static int colCourier = 10;
	static int colManufacturerSKU = 11;
	static int colQuantity = 12;
	static String inDistributor = "Affinity";
	static String distributorID = "102";
	static String ERR_MSG = "was not found in " + inDistributor + " Shipping Notice.";

	public List<String> getBadOrders() {
		if(badOrders == null) {
			badOrders = new ArrayList<String>();
		}
		return badOrders;
	}
	public List<String> getGoodOrders() {
		if(goodOrders == null) {
			goodOrders = new ArrayList<String>();
		}
		return goodOrders;
	}
	public void addToBadOrders(String inItem) {
		if(badOrders == null) {
			badOrders = new ArrayList<String>();
		}
		badOrders.add(inItem);
	}
	public void addToGoodOrders(String inItem) {
		if(goodOrders == null) {
			goodOrders = new ArrayList<String>();
		}
		goodOrders.add(inItem);
	}

	public int getTotalRows() {
		if (totalRows == null) {
			totalRows = 0;
		}
		return totalRows;
	}
	public void increaseTotalRows() {
		this.totalRows++;
	}

	public void handleSubmission(){
		
		//Create the MediaArchive object
		Store store = context.getPageValue("store");
		WebPageRequest inReq = context;
		
		MediaUtilities media = new MediaUtilities();
		media.setContext(context);
		
		MediaArchive archive = media.getArchive();
		String catalogID = media.getCatalogid();

		String strMsg = "";
	
		//Create the searcher objects.	 
		Searcher productsearcher = media.getProductSearcher();
		Searcher userprofilesearcher = archive.getSearcher("userprofile");
		Searcher ordersearcher = media.getOrderSearcher();
		
		//Get the Uploaded Page
		String filename = "affinityshipping.csv";
		Page upload = archive.getPageManager().getPage(catalogID + "/temp/upload/" + filename);
		Reader reader = upload.getReader();
		try
		{
			//Create new CSV Reader Object
			CSVReader read = new CSVReader(reader, ',', '\"');
		
			//Read 1 line for headers
			String[] headers = read.readNext();
			
			//loop over rows
			String[] cols;
			while ((cols = read.readNext()) != null)
			{
				if (cols[colOrderLineNumber].trim().equals("99999")) {
					break;
				}
				
				Shipment shipment = new Shipment();
				String orderID = "";
				String orderNumber = cols[colOrderNumber].trim();
				
				Order order = null;
				order = media.searchForOrder(orderNumber);
				if (order != null) {
					boolean result = processOrder(orderNumber, cols, order, 
						shipment, media)
					if (result) {
						addToGoodOrders(orderNumber);
					} else {
						addToBadOrders(orderNumber);
					}
				} else {
					strMsg = "ERROR: Order(" + orderNumber + ") " + ERR_MSG;
					log.info(strMsg);
					addToBadOrders(orderNumber);
				}
				increaseTotalRows();
			}
			
			context.putPageValue("totalrows", getTotalRows());
			context.putPageValue("goodorderlist", getGoodOrders());
			context.putPageValue("badorderlist", getBadOrders());
			context.putPageValue("distributor", inDistributor);
			
			ArrayList emaillist = new ArrayList();
			HitTracker results = userprofilesearcher.fieldSearch("ticketadmin", "true");
			for(Iterator detail = results.iterator(); detail.hasNext();) {
				Data userInfo = (Data)detail.next();
				emaillist.add(userInfo.get("email"));
			}
			String templatePage = "/ecommerce/views/modules/storeOrder/workflow/shipping-notification.html";
			//sendEmail(context, emaillist, templatePage);
			
		}
		finally
		{
			FileUtils.safeClose(reader);
		}
		
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
	
	private boolean processOrder(String orderNumber, String[] orderLine, Order order, 
		Shipment shipment, MediaUtilities media ) {
		
		boolean result = false;
		
		log.info("Processing Order: " + orderNumber);
		Data distributor = media.getDistributorSearcher().searchById(this.distributorID);
		log.info("Distributor: " + distributor.getName());

		String shipDate = orderLine[this.colShipDate].trim();
		String courier = orderLine[this.colCourier].trim();
		String waybill = orderLine[this.colWaybill].trim();
		String manufacturerSku = orderLine[this.colManufacturerSKU].trim();
		String quantity = orderLine[this.colQuantity].trim();
		Date dateShipped = parseDate(shipDate);
		
		shipment.setProperty("distributor", distributorID);
		shipment.setProperty("courier", courier);
		
		Data product = media.searchForProductBySku("manufacturersku", manufacturerSku);
		if (product != null) {
			
			String productID = product.getId();

			Product target = media.getProductSearcher().searchById(productID);
			CartItem item = order.getItem(productID);

			if (!shipment.containsEntryForSku(productID) && item !=  null ) {
				ShipmentEntry entry = new ShipmentEntry();
				entry.setCartItem(item);
				entry.setQuantity(Integer.parseInt(quantity));
				shipment.setProperty("waybill", waybill);
				shipment.setProperty("shipdate", DateStorageUtil.getStorageUtil().formatForStorage(dateShipped));
				shipment.addEntry(entry);

				String strMsg = "Order Updated(" + orderNumber + ") and saved";
				log.info(strMsg);
				
				strMsg = "Waybill (" + waybill + ")";
				log.info(strMsg);
				
				strMsg = "SKU (" + manufacturerSku + ")";
				log.info(strMsg);
				
				if(shipment.getShipmentEntries().size() >0) {
					order.addShipment(shipment);
					order.getOrderStatus();
					if(order.isFullyShipped()){
						order.setProperty("shippingstatus", "shipped");
						strMsg = "Order status(" + orderNumber + ") set to shipped.";
						log.info(strMsg);
					}else{
						order.setProperty("shippingstatus", "partialshipped");
						strMsg = "Order status(" + orderNumber + ") set to partially shipped.";
						log.info(strMsg);
					}
					media.getOrderSearcher().saveData(order, media.getContext().getUser());
					strMsg = "Order (" + orderNumber + ") saved.";
					log.info(strMsg);
				}

			} else {
				String strMsg = "Cart Item cannot be found(" + manufacturerSku + ")";
				log.info(strMsg);
				throw new OpenEditException(strMsg);
			} // end if orderitems
		} else {
			String strMsg = "Product(" + manufacturerSku + ") " + ERR_MSG;
			log.info(strMsg);
			throw new OpenEditException(strMsg);
		}
		result = true;
		return result;
	}
	private Date parseDate(String date)
	{
		SimpleDateFormat inFormat = new SimpleDateFormat("yyyyMMddmmss");
		SimpleDateFormat newFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date oldDate = inFormat.parse(date);
		date = newFormat.format(oldDate);
		return (Date)newFormat.parse(date);
	}
}

logs = new ScriptLogger();
logs.startCapture();

try {

	log.info("START - ImportAffinityShipping");
	ImportAffinityShipping importAffinity = new ImportAffinityShipping();
	importAffinity.setLog(logs);
	importAffinity.setContext(context);
	importAffinity.setModuleManager(moduleManager);
	importAffinity.setPageManager(pageManager);
	importAffinity.handleSubmission();
	log.info("FINISH - ImportAffinityShipping");
}
finally {
	logs.stopCapture();
}
