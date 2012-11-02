package orders;

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openedit.Data
import org.openedit.data.*
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.PublishResult
import org.openedit.entermedia.util.CSVWriter
import org.openedit.repository.filesystem.StringItem

import com.openedit.OpenEditException
import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.GroovyScriptRunner
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.Page


public class ExportAffinityOrder extends EnterMediaObject implements Affinity {

	private static String distributorName = "Affinity";
	private String orderID;

	public void setOrderID( String inOrderID ) {
		orderID = inOrderID;
	}
	public String getOrderID() {
		return this.orderID;
	}
	
	public PublishResult doExport() {

		PublishResult result = new PublishResult();
		result.setComplete(false);

		//Get Media Info
		Log log = LogFactory.getLog(GroovyScriptRunner.class);
		MediaArchive archive = context.getPageValue("mediaarchive");
		String catalogid = getMediaArchive().getCatalogId();
		boolean production = Boolean.parseBoolean(context.findValue('productionmode'));

		// Create the Searcher Objects to read values!
		SearcherManager searcherManager = archive.getSearcherManager();

		//Create OrderSearcher - Look for order
		Searcher orderSearcher = searcherManager.getSearcher(catalogid, "rogers_order");

		//Read the Order Info
		Data order = orderSearcher.searchById(this.orderID);
		if (order != null) {

			log.info("DATA: Order found: " + this.orderID);

			//Create the CSV Writer Objects
			StringWriter output  = new StringWriter();
			CSVWriter writer  = new CSVWriter(output, (char)',');

			//Setup processing PublishResult
			PublishResult processCSV = new PublishResult();
			processCSV.setComplete(false);

			//Write the Header Line
			List orderRows = new ArrayList();
			orderRows = getHeaderRows(writer);

			//Write the body of all of the orders
			processCSV.setComplete(false);
			processCSV = createOrderDetails(catalogid, order, searcherManager,
					distributorName, orderRows, writer);

			if (processCSV.isComplete()) {
				//Order is ready to be written to CSV file.
				processCSV.setComplete(false);

				// xml generation
				String fileName = "export-" + this.distributorName.replace(" ", "-") + ".csv";
				Page page = pageManager.getPage("/WEB-INF/data/${catalogid}/orders/exports/${this.orderID}/${fileName}");

				processCSV = writeOrderToFile(page, output, fileName);
				if (processCSV.isComplete()) {
					result.setCompleteMessage( processCSV.getCompleteMessage());
					result.setComplete(true);
				} else {
					result.setErrorMessage(processCSV.getErrorMessage());
				}
			} else {
				result.setErrorMessage(processCSV.getErrorMessage());
			}
		}
		return result;
	}

	/*
	 * getHeaderRows()
	 * returns: List
	 */
	private List getHeaderRows( CSVWriter writer ) {

		List headerRow = new ArrayList();

		headerRow.add("ROGER_ORDER_NUMBER");
		int detailCtr = 0;
		
		List details = getRogersStoreHeaders();
		if (details == null) {
			throw new OpenEditException("details is null");
		}
		for (detailCtr=0; detailCtr < details.size(); detailCtr++) {
			PropertyDetail detail = details.get(detailCtr);
			String text = detail.getText().trim().toUpperCase().replace(" ", "_");
			headerRow.add(text);
		}

		List itemdetails = getCSVHeaders();
		if (itemdetails == null) {
			throw new OpenEditException("itemdetails is null");
		}
		for (detailCtr=0; detailCtr < itemdetails.size(); detailCtr++) {
			PropertyDetail detail = itemdetails.get(detailCtr);
			String text = detail.getText().trim().toUpperCase().replace(" ", "_");
			headerRow.add(text);
		}
		/*		
		 headerRow.add("AS400_SKU");
		 headerRow.add("ITEM_DESCRIPTION");
		 headerRow.add("SHIP_DATE");
		 headerRow.add("ORDER_STATUS");
		 headerRow.add("CARRIER");
		 headerRow.add("WAYBILL");
		 headerRow.add("ORDERED_QTY");
		 headerRow.add("SHIPPED_QTY");
		 headerRow.add("DELIVERY_DATE");
		 headerRow.add("DELIVERY_STATUS");
		 headerRow.add("DELIVERY_PERSON");
		 */		
		//Create the row
		String[] nextrow = new String[headerRow.size()];
		for ( int headerCtr=0; headerCtr < headerRow.size(); headerCtr++ ){
			nextrow[headerCtr] = headerRow.get(headerCtr);
		}
		writer.writeNext(nextrow);
		log.info(nextrow.toString());

		return headerRow;
	}

	/*
	 * createOrderDetails(String catalogid, Data order, SearcherManager searcherManager,	String distributorName, List orderRows, CSVWriter writer)
	 * returns: PublishResult
	 */
	private PublishResult createOrderDetails(String catalogid, Data order,
	SearcherManager searcherManager, String distributorName,
	List orderRows, CSVWriter writer) {
	
		int detailCtr = 0;

		//Set up result
		PublishResult result = new PublishResult();
		result.setComplete(false);

		//Set up the rows for the output
		//String[] nextRow = new String[orderRows.size()];
		Searcher storesearcher = searcherManager.getSearcher(catalogid, "store");
		List storeDetails = storesearcher.getDetailsForView("store/storestore_headers", context.getUser());
		//Get the store details
		if (storeDetails == null) {
			throw new OpenEditException("storeDetails is null");
		}

		//Create the searcher for the rogers order item
		Searcher itemsearcher = searcherManager.getSearcher(catalogid, "rogers_order_item");

		SearchQuery orderQuery = itemsearcher.createSearchQuery();
		orderQuery.addExact("rogers_order",order.getId());
		orderQuery.addExact("distributor", distributorName);
		HitTracker orderItems = itemsearcher.search(orderQuery);//Load all of the line items for store X

		if (orderItems.size() > 0) {

			//Go through each item and extract the data
			for (Iterator itemIterator = orderItems.iterator(); itemIterator.hasNext();)
			{

				Data orderitem = itemIterator.next();
				Data product = getProduct(catalogid, searcherManager, orderitem.get("product"));
				List nextRow = new ArrayList();

				//Add the order number
				nextRow.add(order.id);
				
				Data store = storesearcher.searchByField("store", orderitem.store);
				if (store != null) {
					for (detailCtr=0; detailCtr < storeDetails.size(); detailCtr++) {
						PropertyDetail detail = storeDetails.get(detailCtr);
						nextRow.add(store.get(detail.id));
					}
				} else {
					throw new OpenEditException("ERROR: Store does not exist! (" + orderitem.store + ")");
				}
				//Get the item details
				List itemDetails = itemsearcher.getDetailsForView("rogers_order_item/rogers_order_itemexport", context.getUser());
				if (itemDetails == null) {
					throw new OpenEditException("itemdetails is null");
				}
				for (detailCtr=0; detailCtr < itemDetails.size(); detailCtr++) {
					PropertyDetail detail = itemDetails.get(detailCtr);
					if(detail.isList()) {
						Data remote = searcherManager.getData(catalogid, detail.getListId(), orderitem.get(detail.id));
						if(detail.get("field") != null){
							nextRow.add(remote.get(detail.get("field")));
						} else{
							nextRow.add(remote.getName());
						}
					}
					else{
						nextRow.add(orderitem.get(detail.id));
					}
				}

				String[] values = (String[])nextRow.toArray();
				writer.writeNext(values);
				log.info(nextRow.toString());

				product = null;
				orderitem = null;

				result.setComplete(true);
			}
		} else {
			result.setCompleteMessage("ERROR: Order(" + order.getId() + ") has no items!");
		}

		return result;

	}

	/*
	 * getProduct( SearcherManager searcherManager, String id )
	 * returns: Data
	 */
	private Data getProduct( String catalogid, SearcherManager searcherManager, String id ) {

		Searcher productSearcher = searcherManager.getSearcher(catalogid, "product");
		Data product = productSearcher.searchById(id);
		if (product != null) {
			return product;
		} else {
			throw new OpenEditException("Product(" + id + ") does not exist!");
		}
	}

	private PublishResult writeOrderToFile(Page page, StringWriter output, String fileName) {

		PublishResult result = new PublishResult();

		//Create the output of the CSV file
		//StringBuffer bufferOut = new StringBuffer();
		//bufferOut.append(writer);
		page.setContentItem(new StringItem(page.getPath(), output.toString(), "UTF-8"));

		//Write out the CSV file.
		pageManager.putPage(page);

		result.setCompleteMessage(fileName + " has been created.");
		result.setComplete(true);

		return result;

	}

	private List getRogersStoreHeaders() {

		MediaArchive archive = context.getPageValue("mediaarchive");
		SearcherManager manager = archive.getSearcherManager();
		Searcher storesearcher = manager.getSearcher(archive.getCatalogId(), "store");
		List details = storesearcher.getDetailsForView("store/storestore_headers", context.getUser());

		return details;
	}
	
	private List getCSVHeaders() {
		
		MediaArchive archive = context.getPageValue("mediaarchive");
		SearcherManager manager = archive.getSearcherManager();
		Searcher itemsearcher = searcherManager.getSearcher(archive.getCatalogId(), "rogers_order_item");
		List details = itemsearcher.getDetailsForView("rogers_order_item/rogers_order_itemexport", context.getUser());
		
		return details;
	}
	
	private List getRogersStoreInfo(String catalogid, String storeNumber,
	SearcherManager searcherManager) {

		List values = new ArrayList();

		Searcher storeSearcher = searcherManager.getSearcher(catalogid, "store");
		List details = storeSearcher.getDetailsForView("store/storestore_headers", context.getUser());

		Data store = storeSearcher.searchByField("store", storeNumber);
		if (store != null) {
			details.each{
				values.add(store.get(it.id));
			}
		}
		return values;
	}
}

PublishResult result = new PublishResult();
result.setComplete(false);

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
	if (result.isComplete()) {
		//Output value to CSV file!
		context.putPageValue("export", result.getCompleteMessage());
		context.putPageValue("id", affinityOrder.getOrderID());
	} else {
		//ERROR: Throw exception
		context.putPageValue("errorout", result.getErrorMessage());
	}
}
finally {
	logs.stopCapture();
}
