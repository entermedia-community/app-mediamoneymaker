package orders;

import java.util.List;

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openedit.Data
import org.openedit.data.*
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.orders.OrderSearcher;
import org.openedit.entermedia.util.CSVWriter
import org.openedit.repository.filesystem.StringItem
import org.openedit.store.CartItem
import org.openedit.store.Product
import org.openedit.store.customer.Address
import org.openedit.store.orders.Order
import org.openedit.store.util.MediaUtilities

import com.openedit.OpenEditException
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.GroovyScriptRunner
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker
import com.openedit.page.Page


public class ExportOrders extends EnterMediaObject {

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

		log.info("PROCESS: START Orders.ExportFullOrderInfo");
		
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
		
		String sessionid = media.getContext().getRequestParameter("hitssessionid");
		if (sessionid == null) {
			return;
		}
		HitTracker orderList = media.getContext().getSessionValue(sessionid);
		if (orderList == null) {
			return;
		}
		log.info("Found # of Orders:" + orderList.getSelectedHits().size());
		
		//Create the CSV Writer Objects
		StringWriter output  = new StringWriter();
		CSVWriter writer  = new CSVWriter(output, (char)',');

		//Write the Header Line
		List headerRow = new ArrayList();
		headerRow = getHeaderFromView("storeOrder", "storeOrder/storeOrderfull_order_details", headerRow);
		headerRow = getHeaderFromView("userprofile", "userprofile/userprofileusername", headerRow);
		headerRow = getHeaderFromView("userprofile", "userprofile/userprofileaddress_list", headerRow);
		headerRow = getHeaderFromView("product", "product/productfull_product_details", headerRow);
		writeRowToWriter(headerRow, writer);
		
		throw new OpenEditException("Internal Break");
		
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
	
	}

	private void createOrderDetails(Order order, CSVWriter writer) {
		
		//Set up result
		boolean result = false;
		
		MediaArchive archive = context.getPageValue("mediaarchive");
		SearcherManager manager = archive.getSearcherManager();
		String catalogid = archive.getCatalogId();
		Searcher ordersearcher = manager.getSearcher(catalogid, "storeOrder");
		
		//Add the Order Number
		
		
		Address shipToAddress = order.getShippingAddress();
		
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
			orderDetailRow.add(order.getId());
			
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
			
			//Get Distributor
			Searcher distribsearcher = manager.getSearcher(catalogid, "distributor");
			Data distributor = distribsearcher.searchById(item.getProduct().get("distributor"));
			orderDetailRow.add(distributor.get("name"));

			//Get Order Info
			orderDetailRow.add(order.getDate().toString());
			
			//Get Display Designation 
			Searcher displaysearcher = manager.getSearcher(catalogid, "displaydesignationid");
			Data displayDesignation = displaysearcher.searchById(item.getProduct().get("displaydesignationid"));
			orderDetailRow.add(displayDesignation.get("name"));
			
			if (order.isFullyShipped(item)) {
				orderDetailRow.add("Shipped")
			} else if (order.getQuantityShipped(item) > 0) {
				orderDetailRow.add("Partially Shipped");
			} else {
				orderDetailRow.add("Not Shipped");
			}			
			orderDetailRow.add(item.getQuantity().toString());
			orderDetailRow.add('$' + item.getProduct().get("rogersprice").toString());
			orderDetailRow.add(item.getYourPrice().toString());
			if (displayDesignation.get("name").equalsIgnoreCase("fido")) {
				orderDetailRow.add('$' + item.getProduct().get("fidomsrp"));
			} else {
				orderDetailRow.add('$' + item.getProduct().get("msrp"));
			}
			orderDetailRow.add(item.getProduct().get("approved"));

			//Write Row to Writer			
			writeRowToWriter(orderDetailRow, writer);
			log.info(orderDetailRow.toString());
			orderDetailRow = null;
		}
	}

	private List getHeaderFromView( String inSearcher, String inViewName, List inListRows) {
		
		MediaArchive archive = context.getPageValue("mediaarchive");
		SearcherManager manager = archive.getSearcherManager();
		Searcher storesearcher = manager.getSearcher(archive.getCatalogId(), inSearcher);
		List details = storesearcher.getDetailsForView(inViewName, context.getUser());
		for (int detailCtr=0; detailCtr < details.size(); detailCtr++) {
			//Get the property detail
			PropertyDetail detail = details.get(detailCtr);
			//Get and set the Header Row Value
			String value = detail.getText().trim().toUpperCase().replace(" ", "_");
			inListRows.add(value);
		}
		return inListRows;
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
				if (detail.isList()) {
					String listID = detail.getListId();
					Data target = manager.getData(archive.getCatalogId(), listID, value);
					if (target != null) {
						value = target.getName();
					} else {
						log.info("Remote data not found: " + listID + ":" + value);
						break;
					}
				}
				inListRows.add(value);
			}
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
	
	private void writeOrderToFile(Page page, StringWriter output, String fileName) {
		
		page.setContentItem(new StringItem(page.getPath(), output.toString(), "UTF-8"));

		//Write out the CSV file.
		pageManager.putPage(page);
		if (!page.exists()) {
			String strMsg = "ERROR:" + fileName + " was not created.";
			log.info(strMsg);
		}
	}
}

boolean result = false;

logs = new ScriptLogger();
logs.startCapture();

try {
	ExportOrders exportOrders = new ExportOrders();
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