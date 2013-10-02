package orders;

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openedit.Data
import org.openedit.data.*
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.util.CSVWriter
import org.openedit.repository.filesystem.StringItem
import org.openedit.store.CartItem
import org.openedit.store.Product
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


public class ExportAffinityOrder extends EnterMediaObject {

	private static String distributorName = "Affinity";
	private String orderID;
	private Map<String, String> orderList;

	public void setOrderID( String inOrderID ) {
		orderID = inOrderID;
	}
	public String getOrderID() {
		return orderID;
	}
	public Map getOrderList() {
		if (orderList == null) {
			orderList = new HashMap<String, String>();
		}
		return orderList;
	}
	public void writeOrderToList(String inOrderID, String inFilename) {
		if (orderList == null) {
			orderList = new HashMap<String, String>();
		}
		orderList.put(inOrderID, inFilename);
	}
	
	public void doExport() {

		log.info("PROCESS: START Orders.exportaffinityorder");
		
		WebPageRequest inReq = context;
		
		orderList = new HashMap<String, String>();
				
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
		
		SearchQuery orderQuery = ordersearcher.createSearchQuery();
		orderQuery.addExact("csvgenerated", "false");
		orderQuery.addMatches("distributor", "102");
		HitTracker orderList = ordersearcher.search(orderQuery);
		log.info("Found # of Orders:" + orderList.size());
		for (Iterator orderIterator = orderList.iterator(); orderIterator.hasNext();) {

			Data currentOrder = orderIterator.next();

			Order order = ordersearcher.searchById(currentOrder.getId());
			if (order == null) {
				throw new OpenEditException("Invalid Order");
			}

			log.info("DATA: Order found: " + order.getId());
			String orderStatus = order.get("orderstatus");
			if (orderStatus == "authorized") {

				//Check if order has Affinity Products
				List orderitems = order.getCartItemsByProductProperty("distributor", "102");
				if (orderitems.size() > 0) {
				
					//Create the CSV Writer Objects
					StringWriter output  = new StringWriter();
					CSVWriter writer  = new CSVWriter(output, (char)',');
		
					//Write the body of all of the orders
					createOrderDetails(order, writer);
					
					// xml generation
					String fileName = "export-" + this.distributorName.replace(" ", "-") + "-" + order.getId() + ".csv";
					Page page = pageManager.getPage("/WEB-INF/data/${catalogid}/orders/exports/${order.getId()}/${fileName}");
					page.setContentItem(new StringItem(page.getPath(), output.toString(), "UTF-8"));
					//Write out the CSV file.
					pageManager.putPage(page);
					//Set the order properties
					//order.setProperty("csvgenerated", "true");
					archive.getSearcher("storeOrder").saveData(order, context.getUser());
					writeOrderToList(order.getId(), fileName);
					
				}
			} else {
				log.info("SKIPPING ORDER: Order not authorized.");
			}
		}
		if (getOrderList().isEmpty()) {
			inReq.putPageValue("errorout", "There are no Affinity orders to export at this time.");
		} else {
			inReq.putPageValue("export", getOrderList());
		}
	}

	private void createOrderDetails(Order order, CSVWriter writer) {
		
		//Set up result
		boolean result = false;
		
		MediaArchive archive = context.getPageValue("mediaarchive");
		SearcherManager manager = archive.getSearcherManager();
		String catalogid = archive.getCatalogId();
		
		//Write the Header Line
		List headerRow = new ArrayList();
		//Add the Order Number
		headerRow.add("ORDER_NUMBER");
		getDetailsFromView("userprofile", "userprofile/userprofileusername", order, headerRow, true);
		getDetailsFromView("address", "address/addresslist", order, headerRow, true);
		getDetailsFromView("storeOrder", "storeOrder/storeOrdercsvheaders", order, headerRow, true);
		getDetailsFromView("product", "product/productproduct_info", order, headerRow, true);
		headerRow.add("QUANTITY");
		headerRow.add("PRICE");
		log.info(headerRow.toString());
		writeRowToWriter(headerRow, writer);
		
		for(Iterator i = order.getCartItemsByProductProperty("distributor", "102").iterator(); i.hasNext();) {
			//Get the cart Item
			CartItem item = i.next();
			
			List orderDetailRow = new ArrayList();
			//Add the Order ID
			orderDetailRow.add(order.getId());
			getDetailsFromView("userprofile", "userprofile/userprofileusername", order.getCustomer(), orderDetailRow, false);
			getDetailsFromView("address", "address/addresslist", order.getShippingAddress(), orderDetailRow, false);
			getDetailsFromView("storeOrder", "storeOrder/storeOrdercsvheaders", order, orderDetailRow, false);
			getDetailsFromView("product", "product/productproduct_info", item.getProduct(), orderDetailRow, false);
			
			orderDetailRow.add(item.getQuantity().toString());
			Product p = item.getProduct();
			orderDetailRow.add(p.getProperty("rogersprice"));
			writeRowToWriter(orderDetailRow, writer);
			log.info(orderDetailRow.toString());
			orderDetailRow = null;
		}
	}
	
	private void getDetailsFromView( String inSearcher, String inViewName, Data inOrder, 
		List inListRows, boolean isHeaderRow) {
		
		MediaArchive archive = context.getPageValue("mediaarchive");
		SearcherManager manager = archive.getSearcherManager();
		Searcher storesearcher = manager.getSearcher(archive.getCatalogId(), inSearcher);
		List details = storesearcher.getDetailsForView(inViewName, context.getUser());
		for (int detailCtr=0; detailCtr < details.size(); detailCtr++) {
			String value = "";
			//Get the property detail
			PropertyDetail detail = details.get(detailCtr);
			if (isHeaderRow) {
				//Get and set the Header Row Value
				value = detail.getText().trim().toUpperCase().replace(" ", "_");
				inListRows.add(value);
			} else {
				//Get and set the Detail Row Value
				value = inOrder.get(detail.getId());
				inListRows.add(value);
			}
		}
	}
		
	private void getAddressDetails( Order inOrder, List inListRows, boolean isHeaderRow ) {
		MediaArchive archive = context.getPageValue("mediaarchive");
		SearcherManager manager = archive.getSearcherManager();
		Searcher addressSearcher = manager.getSearcher(archive.getCatalogId(), "address");
		if (isHeaderRow) {
			
		} else {
			
		}
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
	
	private void writeOrderToFile(Page page, StringWriter output) {
		
	}
}

boolean result = false;

logs = new ScriptLogger();
logs.startCapture();

try {
	ExportAffinityOrder affinityOrder = new ExportAffinityOrder();
	affinityOrder.setLog(logs);
	affinityOrder.setContext(context);
	affinityOrder.setOrderID(context.getRequestParameter("orderid"));
	affinityOrder.setModuleManager(moduleManager);
	affinityOrder.setPageManager(pageManager);

	affinityOrder.doExport();
	if (affinityOrder.getOrderList().size() > 0) {
		log.info("The following file(s) has been created. ");
		log.info(affinityOrder.getOrderList().toString());
		log.info("Exporting " + affinityOrder.getOrderList().size().toString() + " Affinity orders.");
	} else {
		log.info("0 Affinity Orders to process.");
	}
		
	log.info("PROCESS: END Orders.exportaffinity");
}
finally {
	logs.stopCapture();
}
