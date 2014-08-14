package shipping

import java.util.List;

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
import org.openedit.store.customer.Customer
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
import com.openedit.page.manage.PageManager;
import com.openedit.util.FileUtils


public class ImportAffinityShipping  extends EnterMediaObject {
	
	List<String> badOrders;
	List<String> goodOrders;
	int totalRows;

	static int colOrderNumber = 0;
	static int colOrderDate = 9;
	static int colShipStatus = 10;
	static int colShipDate = 11;
	static int colCourier = 12;
	static int colWaybill = 13;
	static int colManufacturerSKU = 14;
	static int colRogersSKU = 15;
	static int colQuantity = 16;
	
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
		WebPageRequest inReq = context;
		
		MediaUtilities media = new MediaUtilities();
		media.setContext(context);
		MediaArchive archive = media.getArchive();
		String catalogID = media.getCatalogid();
		String strMsg = "";
	
		Store store = null;
		try {
			store  = media.getContext().getPageValue("store");
			if (store != null) {
				log.info("Store loaded");
			} else {
				String inMsg = "ERROR: Could not load store";
				throw new Exception(inMsg);
			}
		}
		catch (Exception e) {
			strMsg = "ERROR: Store cannot be loaded.";
			strMsg += "Exception thrown:\n";
			strMsg += "Local Message: " + e.getLocalizedMessage() + "\n";
			strMsg += "Stack Trace: " + e.getStackTrace().toString();;
			log.info(strMsg);
		}
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
				String orderID = "";
				String orderNumber = cols[colOrderNumber].trim();
				
				Order order = null;
				order = media.searchForOrder(orderNumber);
				if (order != null) {
					boolean result = processOrder(orderNumber, cols, order, store, media)
					if (result) {
						sendEmailToCustomer(orderNumber, order, store, media );
					} else {
						addToBadOrders(orderNumber + " - Line # " + (totalRows+1).toString());
					}
				} else {
					strMsg = "ERROR: Order(" + orderNumber + ") " + ERR_MSG;
					log.info(strMsg);
					addToBadOrders(orderNumber + " - Line # " + (totalRows+1).toString());
				}
				increaseTotalRows();
			}
			
			context.putPageValue("totalrows", getTotalRows());
			context.putPageValue("goodorderlist", getGoodOrders());
			context.putPageValue("badorderlist", getBadOrders());
			context.putPageValue("distributor", inDistributor);
			
			ArrayList emaillist = new ArrayList();
			HitTracker results = userprofilesearcher.fieldSearch("storeadmin", "true");
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
	
	protected void sendEmailToCustomer(String orderNumber, Order order, Store store, MediaUtilities media ) {
		
		Customer customer = order.getCustomer();
		ArrayList<Shipment> shipments = order.getShipments();

		context.putPageValue("orderid", order.getId());		
		context.putPageValue("ordernumber", orderNumber);
		context.putPageValue("order", order);
		context.putPageValue("customer", customer);
		context.putPageValue("shipments", shipments);

		ArrayList emaillist = new ArrayList();
		emaillist.add(customer.getEmail());
		String subject = "Wirelessarea.ca Order Shipping Notification - Order #: " + orderNumber;
		String templatePage = "/ecommerce/views/modules/storeOrder/workflow/order-shipping-notification.html";
		sendEmail(media.getArchive(), context, emaillist, templatePage, subject);

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
		PostMail mail = (PostMail)archive.getModuleManager().getBean( "postMail");
		return mail.getTemplateWebEmail();
	}
	
	private boolean processOrder(String orderNumber, String[] orderLine, Order order, Store store, 
		MediaUtilities media ) {
		
		boolean result = false;
		String strMsg = "";
		
		log.info("Processing Order: " + orderNumber);
		Data distributor = media.getDistributorSearcher().searchById(this.distributorID);
		log.info("Distributor: " + distributor.getName());

		String shipDate = orderLine[this.colShipDate].trim();
		String courier = orderLine[this.colCourier].trim();
		String waybill = orderLine[this.colWaybill].trim();
		String manufacturerSku = orderLine[this.colManufacturerSKU].trim();
		String rogersSKU = orderLine[this.colRogersSKU].trim();
		String quantity = orderLine[this.colQuantity].trim();
		Date dateShipped = parseDate(shipDate);
		
		Data inProduct = media.searchForProductByField("rogerssku", rogersSKU);
		if (inProduct != null) {
			
			Shipment shipment = null;
			String productID = inProduct.getId();
			Product product = media.getProductSearcher().searchById(productID);
			InventoryItem productItem = product.getInventoryItem(0);
			String productSku = productItem.getSku();
			CartItem item = order.getItem(productSku);
			
			if (item != null) {
	
				if (order.containsShipmentByWaybill(waybill)) {
					
					int totalShipped = 0;
					ArrayList<Shipment> shipments = order.getShipments();
					for (Shipment eShipment in shipments) {
						if (eShipment.containsEntryForSku(productSku)) {
							ArrayList<ShipmentEntry> entries = null;
							entries = eShipment.getShipmentEntries();
							for (ShipmentEntry eEntry in entries) {
								if (eEntry.getSku() == productSku) {
									totalShipped += eEntry.getQuantity();
								}
							}
						}
					}
					int cartQty = item.getQuantity();
					int qtyShipped = Integer.parseInt(quantity);
					if (qtyShipped <= (cartQty - totalShipped)) {
						//Load the existing Shipment
						Shipment existShipment = order.getShipmentByWaybill(waybill);
						ShipmentEntry entry = new ShipmentEntry();
						entry.setSku(item.getSku());
						entry.setQuantity(qtyShipped);
						existShipment.setProperty("distributor", distributor.getId());
						existShipment.setProperty("courier", courier);
						existShipment.setProperty("waybill", waybill);
						existShipment.setProperty("shipdate", DateStorageUtil.getStorageUtil().formatForStorage(dateShipped));
						existShipment.addEntry(entry);
			
						strMsg = "Order Updated(" + orderNumber + ") and saved";
						log.info(strMsg);
						
						strMsg = "Waybill (" + waybill + ")";
						log.info(strMsg);
						
						strMsg = "SKU (" + item.getSku() + ")";
						log.info(strMsg);
						
						if(existShipment.getShipmentEntries().size() >0) {
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
							store.saveOrder(store);
							strMsg = "Order (" + orderNumber + ") saved.";
							log.info(strMsg);
						}
					} else {
						strMsg = "Entry already exists for " + item.getSku() + " for this order (" + orderNumber + ")";
						log.info(strMsg);
					}
				} else {
					shipment = new Shipment(); 
					ShipmentEntry entry = new ShipmentEntry();
					entry.setSku(item.getSku());
					entry.setQuantity(Integer.parseInt(quantity));
					shipment.setProperty("distributor", distributor.getId());
					shipment.setProperty("waybill", waybill);
					shipment.setProperty("courier", courier);
					shipment.setProperty("shipdate", DateStorageUtil.getStorageUtil().formatForStorage(dateShipped));
					shipment.addEntry(entry);
		
					strMsg = "Order Updated(" + orderNumber + ") and saved";
					log.info(strMsg);
					
					strMsg = "Waybill (" + waybill + ")";
					log.info(strMsg);
					
					strMsg = "SKU (" + productItem.getSku() + ")";
					log.info(strMsg);
					
					if(shipment.getShipmentEntries().size() >0) {
						if (!order.getShipments().contains(shipment)) {
							order.addShipment(shipment);
						}
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
						store.saveOrder(order);
						strMsg = "Order (" + orderNumber + ") saved.";
						log.info(strMsg);
					}
				} // END containsShipmentByWaybill
			} else {
				strMsg = "Cart Item not found (" productID + ":" + rogersSKU + ") ";
				log.info(strMsg);
				throw new OpenEditException(strMsg);
			}
		} else {
			strMsg = "Product(" + manufacturerSku + ") " + ERR_MSG;
			log.info(strMsg);
			throw new OpenEditException(strMsg);
		}
		result = true;
		return result;
	}
	private Date parseDate(String inDate)
	{
		Integer intDate = Integer.parseInt(inDate);
		return getDate(intDate);
	}
	public static Date getDate(int days) { //modified to use Calendar
		Calendar cal = Calendar.getInstance();
		cal.set(1900, Calendar.JANUARY, 1);
		cal.add(Calendar.DATE, days);
		return cal.getTime();
	}
}

log = new ScriptLogger();
log.startCapture();

try {

	log.info("START - ImportAffinityShipping");
	ImportAffinityShipping importAffinity = new ImportAffinityShipping();
	importAffinity.setLog(log);
	importAffinity.setContext(context);
	importAffinity.setModuleManager(moduleManager);
	importAffinity.setPageManager(pageManager);
	importAffinity.handleSubmission();
	log.info("FINISH - ImportAffinityShipping");
}
finally {
	log.stopCapture();
}
