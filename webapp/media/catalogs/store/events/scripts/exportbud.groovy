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
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.GroovyScriptRunner
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker
import com.openedit.page.Page


public class ExportBud extends EnterMediaObject {
	
	String outputFile;
	ArrayList<String> goodOrderList;
	
	public void setOutputFile( String inItem ) {
		if (outputFile == null) {
			outputFile = "";
		}
		this.outputFile = inItem;
	}
	
	public ArrayList<String> getOutputFile() {
		if (outputFile == null) {
			outputFile = "";
		}
		return this.outputFile;
	}
	
	public void addToGoodOrderList( String inItem ) {
		if (goodOrderList == null) {
			goodOrderList = new ArrayList<String>();
		}
		this.goodOrderList.add(inItem);
	}
	
	public ArrayList<String> getGoodOrderList() {
		if (goodOrderList == null) {
			goodOrderList = new ArrayList<String>();
		}
		return this.goodOrderList;
	}
	
	public boolean existsInGoodOrderList( String inItem ) {
		if (getGoodOrderList().contains(inItem)) {
			return true;
		} else {
			return false;
		}
	}
	
	public void init(){
		
		//Get Media Info
		Log log = LogFactory.getLog(GroovyScriptRunner.class);

		MediaArchive archive = context.getPageValue("mediaarchive");
		String catalogid = getMediaArchive().getCatalogId();
		
		MediaUtilities media = new MediaUtilities();
		media.setContext(context);

		// Create the Searcher Objects to read values!
		SearcherManager searcherManager = archive.getSearcherManager();
		Searcher ordersearcher = media.getOrderSearcher();
		
		String batchid = context.getRequestParameter("batchid");
		
//		String sessionid = media.getContext().getRequestParameter("hitssessionid");
//		if (sessionid == null) {
//			return;
//		}
		HitTracker orderList = ordersearcher.fieldSearch("batchid", batchid)
		if (orderList == null) {
			return;
		}
		//log.info("Found # of Orders:" + orderList.getSelectedHits().size());
		
		//Create the CSV Writer Objects
		StringWriter output  = new StringWriter();
		CSVWriter writer  = new CSVWriter(output, (char)',');
		
		List headerRow = new ArrayList();
		headerRow.add("");
		
		getHeaderRows(orderList, ordersearcher, log, headerRow)
		writeRowToWriter(headerRow, writer);
		
		Searcher storesearcher = searcherManager.getSearcher(archive.getCatalogId(), "rogersstore");
		
		for (Iterator iterator = storesearcher.getAllHits().iterator(); iterator.hasNext();)
		{
			List detailRow = new ArrayList();
			Data store = iterator.next();
			String storeNum = store.getId();
			if (storeNum.length() > 0) {
				detailRow.add(storeNum);
				if (existsInGoodOrderList("rogers-"+storeNum)) {
					log.info(" - Processing Store: " + storeNum);
					for (String as400id in headerRow) {
						if (as400id.length() > 0 ) {
							int quantity = 0;
							for (Iterator orderIterator = orderList.iterator(); orderIterator.hasNext();) {
								Data currentOrder = orderIterator.next();
								Order order = ordersearcher.searchById(currentOrder.getId());
								if (order.getCustomer().getId().equals("rogers-"+storeNum)) {
									List cartItems = order.getItems();
									for (Iterator itemIterator = cartItems.iterator(); itemIterator.hasNext();) {
										CartItem item = itemIterator.next();
										Product p = item.getProduct();
										if (p.getProperty("as400id") == as400id) {
											quantity += item.getQuantity();
											break;
										}
									}
								}
							}
							detailRow.add(quantity.toString());
						}
					}
				} else {
					for (String as400id in headerRow) {
						if (as400id.length() > 0 ) {
							detailRow.add("0");
						}
					}
				}
			}
			writeRowToWriter(detailRow, writer);
		}
		
		writer.close();
		
		String finalout = output.toString();
		context.putPageValue("export", finalout);
		
		Searcher as400searcher = searcherManager.getSearcher(archive.getCatalogId(), "as400");
		Data exportStatus = as400searcher.searchByField("batchid", batchid);
		String currentStatus = exportStatus.get("exportstatus");
		if (currentStatus.equals("open")) {
			exportStatus.setProperty("exportstatus", "partial");
		} else {
			exportStatus.setProperty("exportstatus", "fully");
		}
		as400searcher.saveData(exportStatus, context.getUser());
	}

	private getHeaderRows(HitTracker orderList, Searcher ordersearcher, Log log, List headerRow) {
		
		for (Iterator orderIterator = orderList.iterator(); orderIterator.hasNext();) {

			Data currentOrder = orderIterator.next();

			Order order = ordersearcher.searchById(currentOrder.getId());
			if (order == null) {
				throw new OpenEditException("Invalid Order");
			}

			log.info("DATA: Order found: " + order.getId());

			//Write the body of all of the orders
			List orderitems = order.getItems();
			for (Iterator itemIterator = order.getItems().iterator(); itemIterator.hasNext();) {
				CartItem item = itemIterator.next();
				String id = item.getProduct().get("as400id");
				if (!headerRow.contains(id)) {
					log.info("OrderItem: New item found: " + id);
					headerRow.add(id);
					if (!existsInGoodOrderList(order.getCustomer().getId())){
						addToGoodOrderList(order.getCustomer().getId());
					}
				}
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
	ExportBud exportBud = new ExportBud();
	exportBud.setLog(logs);
	exportBud.setContext(context);
	exportBud.setModuleManager(moduleManager);
	exportBud.setPageManager(pageManager);

	exportBud.init();
}
finally {
	logs.stopCapture();
}
