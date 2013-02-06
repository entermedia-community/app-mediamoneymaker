package orders;

import java.io.StringWriter;

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openedit.Data
import org.openedit.data.*
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.PublishResult
import org.openedit.entermedia.util.CSVWriter
import org.openedit.repository.filesystem.StringItem
import org.openedit.store.CartItem
import org.openedit.store.orders.Order
import org.openedit.store.util.MediaUtilities

import com.openedit.OpenEditException
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.GroovyScriptRunner
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.Page
import com.sun.star.beans.GetDirectPropertyTolerantResult;


public class ExportAffinityOrder extends EnterMediaObject {

	private static String distributorName = "Affinity";
	private String orderID;

	public void setOrderID( String inOrderID ) {
		orderID = inOrderID;
	}
	public String getOrderID() {
		return this.orderID;
	}
	
	public boolean doExport() {

		log.info("PROCESS: START Orders.exportaffinityorder");
		
		boolean result = false;
				
		MediaUtilities media = new MediaUtilities();
		media.setContext(context);

		//Get Media Info
		Log log = LogFactory.getLog(GroovyScriptRunner.class);
		MediaArchive archive = context.getPageValue("mediaarchive");
		String catalogid = getMediaArchive().getCatalogId();
		boolean production = Boolean.parseBoolean(context.findValue('productionmode'));

		// Create the Searcher Objects to read values!
		SearcherManager searcherManager = archive.getSearcherManager();

		//Read the Order Info
		Order order = media.searchForOrder(this.orderID);
		if (order != null) {

			log.info("DATA: Order found: " + this.orderID);

			//Create the CSV Writer Objects
			StringWriter output  = new StringWriter();
			CSVWriter writer  = new CSVWriter(output, (char)',');

			//Write the body of all of the orders
			result = createOrderDetails(order, writer);
			
			if (result) {
				// xml generation
				String fileName = "export-" + this.distributorName.replace(" ", "-") + "-" + this.orderID + ".csv";
				Page page = pageManager.getPage("/WEB-INF/data/${catalogid}/orders/exports/${this.orderID}/${fileName}");
				result = writeOrderToFile(page, output, fileName);
			}
		}
		return result;
	}

	private boolean createOrderDetails(Order order, CSVWriter writer) {
		
		//Set up result
		boolean result = false;
		
		MediaArchive archive = context.getPageValue("mediaarchive");
		SearcherManager manager = archive.getSearcherManager();
		String catalogid = archive.getCatalogId();
		
		//Write the Header Line
		List headerRow = new ArrayList();
		//Add the Order Number
		headerRow.add("ORDER_NUMBER");
		result = getDetailsFromView("userprofile", "userprofile/userprofileusername", order, headerRow, true);
		if (result) {
			result = getDetailsFromView("userprofile", "userprofile/userprofileaddress_list", order, headerRow, true);
		}
		if (result) {
			result = getDetailsFromView("storeOrder", "storeOrder/storeOrdercsvheaders", order, headerRow, true);
		}
		if (result) {
			result = getDetailsFromView("product", "product/productproduct_info", order, headerRow, true);
		}
		if (result) {
			headerRow.add("QUANTITY");
			log.info(headerRow.toString());
			result = writeRowToWriter(headerRow, writer);
		}
		
		if (result) {
		// Loop through each order item.
			for(Iterator i = order.getItems().iterator(); i.hasNext();) {
				//Get the cart Item
				CartItem item = i.next();
				
				List orderDetailRow = new ArrayList();
				//Add the Order ID
				orderDetailRow.add(order.getId());
				if (result) {
					//Get Customer Info
					result = getDetailsFromView("userprofile", "userprofile/userprofileusername", order.getCustomer(), orderDetailRow, false);
				}
				if (result) {
					//Get Customer Info
					result = getDetailsFromView("address", "userprofile/userprofileaddress_list", order.getShippingAddress(), orderDetailRow, false);
				}
				if (result) {
					//Get Order Info
					result = getDetailsFromView("storeOrder", "storeOrder/storeOrdercsvheaders", order, orderDetailRow, false);
				}
				if (result) {
					//Get Product Info
					result = getDetailsFromView("product", "product/productproduct_info", item.getProduct(), orderDetailRow, false);
				}
				if (result) {
					//Get Order Quantity
					orderDetailRow.add(item.getQuantity().toString());
					result = writeRowToWriter(orderDetailRow, writer);
					log.info(orderDetailRow.toString());
					orderDetailRow = null;
				} else {
					break;
				}
			}
		}
		return result;
	}

	private boolean getDetailsFromView( String inSearcher, String inViewName, Data inOrder, 
		List inListRows, boolean isHeaderRow) {
		
		boolean result = false;
				
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
					log.info(listID + ":" + value);
					Data target = manager.getData(archive.getCatalogId(), listID, value);
					if (target != null) {
						value = target.getName();
					} else {
						log.info("Remote data not found: " + listID + ":" + value);
					}
				}
				inListRows.add(value);
			}
		}
		result = true;
		return result;
	}

	private boolean writeRowToWriter( List inRow, CSVWriter writer) {
		
		boolean result = false;
		String[] nextrow = new String[inRow.size()];
		for ( int headerCtr=0; headerCtr < inRow.size(); headerCtr++ ){
			nextrow[headerCtr] = inRow.get(headerCtr);
		}
		try	{
			writer.writeNext(nextrow);
			result = true;
		}
		catch (Exception e) {
			result = false;
			log.info(e.message);
			log.info(e.getStackTrace().toString());
		}
		return result;
	}
	
	private boolean writeOrderToFile(Page page, StringWriter output, String fileName) {
		
		boolean result = false;
		//Create the output of the CSV file
		//StringBuffer bufferOut = new StringBuffer();
		//bufferOut.append(writer);
		page.setContentItem(new StringItem(page.getPath(), output.toString(), "UTF-8"));

		//Write out the CSV file.
		pageManager.putPage(page);
		if (page.exists()) {
			result = true;
			String strMsg = fileName + " has been created.";
			log.info(strMsg);
		} else {
			String strMsg = "ERROR:" + fileName + " was not created.";
			log.info(strMsg);
		}
		return result;
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

	result = affinityOrder.doExport();
	if (result) {
		//Output value to CSV file!
		log.info("PROCESS: END Orders.exportaffinity");
		context.putPageValue("export", "The file has been exported. Click the download link.");
		context.putPageValue("id", affinityOrder.getOrderID());
	} else {
		//ERROR: Throw exception
		context.putPageValue("errorout", "There was a problem exporting the file. Please check with your administrator.");
		log.info("PROCESS: ERROR Orders.exportaffinity");
	}
}
finally {
	logs.stopCapture();
}
