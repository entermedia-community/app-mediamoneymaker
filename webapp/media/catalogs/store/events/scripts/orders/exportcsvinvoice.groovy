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


public class ExportCSVInvoice extends EnterMediaObject {

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
			orderRows, writer);

			if (processCSV.isComplete()) {
				//Order is ready to be written to CSV file.
				processCSV.setComplete(false);

				// xml generation
				String fileName = "export-invoice.csv";
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
		headerRow.add("TRANSACTION_DATE");
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

		headerRow.add("ROGERS_SKU");
		headerRow.add("PRODUCT_NAME");
		headerRow.add("PRICE");
		headerRow.add("QUANTITY");
		headerRow.add("QUANTITY_SHIPPED");
		headerRow.add("EXTENSION_COST");
		headerRow.add("REV_SHARE");
		headerRow.add("DATE_RANGE");

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
	SearcherManager searcherManager, List orderRows, CSVWriter writer) {

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
		HitTracker orderItems = itemsearcher.search(orderQuery);//Load all of the line items for store X

		if (orderItems.size() > 0) {

			//Go through each item and extract the data
			for (Iterator itemIterator = orderItems.iterator(); itemIterator.hasNext();)
			{

				Data orderitem = itemIterator.next();
				Data product = getProduct(catalogid, searcherManager, orderitem.get("product"));
				if (product == null) {
					throw new OpenEditException("Product is null!");
				}
				List nextRow = new ArrayList();

				//Add the order number
				nextRow.add(order.name);
				nextRow.add(order.date);

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
				nextRow.add(product.get("rogerssku"));
				nextRow.add(product.get("name"));
				nextRow.add(product.get("rogersprice"));
				nextRow.add(orderitem.get("quantity"));
				nextRow.add(orderitem.get("shippedquantity"));
				nextRow.add(calcExtCost(orderitem.get("quantity"), product.get("rogersprice")));
				nextRow.add("0.00");
				nextRow.add(order.date);

				String[] values = (String[])nextRow.toArray();
				writer.writeNext(values);
				log.info(nextRow.toString());
				result.setComplete(true);
			}
		} else {
			result.setCompleteMessage("ERROR: Order(" + order.getId() + ") has no items!");
		}

		return result;

	}
	
	private String calcExtCost( String quantity, String price ) {
		def double qty = quantity as Double;
		def double pri = price as Double;
		double extCost = qty * pri;
		return extCost as String;
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
		List details = itemsearcher.getDetailsForView("rogers_order_item/rogers_order_itemcsvexport", context.getUser());

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
	ExportCSVInvoice invoice = new ExportCSVInvoice();
	invoice.setLog(logs);
	invoice.setContext(context);
	invoice.setOrderID(context.getRequestParameter("orderid"));
	invoice.setModuleManager(moduleManager);
	invoice.setPageManager(pageManager);

	result = invoice.doExport();
	if (result.isComplete()) {
		//Output value to CSV file!
		context.putPageValue("export", result.getCompleteMessage());
		context.putPageValue("id", invoice.getOrderID());
	} else {
		//ERROR: Throw exception
		context.putPageValue("errorout", result.getErrorMessage());
	}
}
finally {
	logs.stopCapture();
}
