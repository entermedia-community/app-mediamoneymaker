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


public class ExportBudHdr extends EnterMediaObject {
	
	String outputFile;
	
	public void setOutputFile( String inItem ) {
		if (outputFile == null) {
			outputFile = "";
		}
		this.outputFile = inItem;
	}
	
	public String getOutputFile() {
		if (outputFile == null) {
			outputFile = "";
		}
		return this.outputFile;
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
		
		HitTracker orderList = ordersearcher.fieldSearch("batchid", batchid)
		if (orderList == null) {
			return;
		}
		
		//Create the CSV Writer Objects
		StringBuffer output  = new StringBuffer();
		//CSVWriter writer  = new CSVWriter(output, ",".getChars()[0], " ".getChars()[0]);
		
		List headerRow = new ArrayList();
		headerRow.add("");
		
		getHeaderRows(orderList, ordersearcher, log, headerRow)
//		writeRowToWriter(headerRow, writer);
		
		Searcher storesearcher = searcherManager.getSearcher(archive.getCatalogId(), "rogersstore");
		int padSpacesLeft = 10;
		int padSpaces = 4;
	
		for (String as400id in headerRow) {
			if (as400id.length() > 0 ) {
				int productCount = 0;
				String nextrow = "";
				for (Iterator orderIterator = orderList.iterator(); orderIterator.hasNext();) {
					Data currentOrder = orderIterator.next();
					Order order = ordersearcher.searchById(currentOrder.getId());
					String as400field = order.get("as400ordertype");
					if (as400field){
						//not null and not empty
					} else {
						//problem: old order so need to default to rogers as400 type
						as400field = "rogersas400id";
						order.setProperty("as400ordertype", as400field);
						ordersearcher.saveData(order, null);//save it
					}
					List cartItems = order.getItems();
					for (Iterator itemIterator = cartItems.iterator(); itemIterator.hasNext();) {
						CartItem item = itemIterator.next();
						Product p = item.getProduct();
						//need to grab the field to search for on the order
						if (p.getProperty(as400field) == as400id) {
							productCount += item.getQuantity();
							break;
						}
					}
				}
				nextrow = as400id + productCount.toString().padLeft(9) + "-";
				nextrow += " ".padLeft(5) + "-";
				nextrow += "WIRELE".padLeft(9) + "-";
				nextrow += " ".padLeft(4) + "-";
				nextrow += " ".padLeft(8);
				nextrow += productCount.toString();
				nextrow += " ".padLeft(6) + "0";
				nextrow += "\r\n";
				//log.info("nextrow: " + nextrow);
				output.append(nextrow);
			}
		}
		
		//writer.close();
		
		String finalout = output.toString();
		//finalout = finalout.replace("\"","").replace("\n","\r\n");
		context.putPageValue("export", finalout);
		context.putPageValue("exporthdr", finalout);
		
		String folder ="/media/catalogs/store/as400zip";
		String filename ="/budhdr.txt";
		Page page = getPageManager().getPage("${folder}${filename}");
		page.setContentItem(new StringItem(page.getPath(), finalout, "UTF-8"));
		getPageManager().putPage(page);
		
		//add path to context
		context.setRequestParameter("path", folder);
		context.setRequestParameter("stripfolders", "/media/catalogs/store/");
		
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
			
			String as400field = order.get("as400ordertype");
			if (as400field){
				//not null and not empty
			} else {
				//problem: old order so need to default to rogers as400 type
				as400field = "rogersas400id";
				order.setProperty("as400ordertype", as400field);
				ordersearcher.saveData(order, null);//save it
			}
			//Write the body of all of the orders
			List orderitems = order.getItems();
			for (Iterator itemIterator = order.getItems().iterator(); itemIterator.hasNext();) {
				CartItem item = itemIterator.next();
				String id = item.getProduct().get(as400field);
				if (id && headerRow.contains(id) == false) {
					log.info("OrderItem: New item found: " + id);
					headerRow.add(id);
				}
			}
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
	ExportBudHdr exportBudHeader = new ExportBudHdr();
	exportBudHeader.setLog(logs);
	exportBudHeader.setContext(context);
	exportBudHeader.setModuleManager(moduleManager);
	exportBudHeader.setPageManager(pageManager);

	exportBudHeader.init();
	log.info("The following file(s) has been created. ");
	log.info(exportBudHeader.getOutputFile().toString());
	log.info("PROCESS: END Orders.ExportRogersCSV");
	context.putPageValue("exportfile", exportBudHeader.getOutputFile());
}
finally {
	logs.stopCapture();
}
