package invoices

import java.text.SimpleDateFormat

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.store.Store

import com.openedit.OpenEditException
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker

public class LoadInvoice extends EnterMediaObject {
	
	public void init() {
		
		MediaArchive archive = context.getPageValue("mediaarchive");
		String catalogid = getMediaArchive().getCatalogId();
		boolean production = Boolean.parseBoolean(context.findValue('productionmode'));
		
		String strMsg = "";
		Store store = null;
		try {
			store  = context.getPageValue("store");
			if (store != null) {
				log.info("Store loaded");
			} else {
				strMsg = "ERROR: Could not load store";
				throw new Exception(strMsg);
			}
		}
		catch (Exception e) {
			strMsg += "Exception thrown:\n";
			strMsg += "Local Message: " + e.getLocalizedMessage() + "\n";
			strMsg += "Stack Trace: " + e.getStackTrace().toString();;
			log.info(strMsg);
			throw new OpenEditException(strMsg);
		}
		
		String inID = context.getRequestParameter("id");
		
		// Create the Searcher Objects to read values!
		SearcherManager manager = archive.getSearcherManager();

		//Create Searcher Object
		Searcher invoicesearcher = manager.getSearcher(archive.getCatalogId(), "invoice");
		Searcher invoiceitemsearcher = manager.getSearcher(archive.getCatalogId(), "invoiceitem");
		
		Data invoice = invoicesearcher.searchById(inID);
		if (invoice != null) {
			ArrayList list = new ArrayList()
			double subTotal = 0;
			HitTracker invoiceitems = invoiceitemsearcher.fieldSearch("invoiceid", invoice.getId());
			for (Iterator itemIterator = invoiceitems.iterator(); itemIterator.hasNext();) {
				Data item = (Data) itemIterator.next();
				Data invoiceItem = invoiceitemsearcher.searchById(item.getId());
				int quantity = Integer.parseInt(invoiceItem.get("quantity"));
				double price = Double.parseDouble(invoiceItem.get("price"));
				double linetotal = quantity * price;
				invoiceItem.setProperty("linetotal", linetotal.toString());
				invoiceitemsearcher.saveData(invoiceItem, context.getUser());
				subTotal += linetotal;
				list.add(invoiceItem);
			}
			invoice.setProperty("subtotal", subTotal.toString());
			invoicesearcher.saveData(invoice, context.getUser());
			
			double shipping = Double.parseDouble(invoice.get("shipping"));
			double fedtaxes = 0;
			if (invoice.get("fedtaxamount") != null) {
				fedtaxes = Double.parseDouble(invoice.get("fedtaxamount"));
			}
			double provtaxes = 0;
			if (invoice.get("provtaxamount") != null) {
				provtaxes = Double.parseDouble(invoice.get("provtaxamount"));
			}
			double total = subTotal + shipping + fedtaxes + provtaxes;
			
			SimpleDateFormat inFormat = new SimpleDateFormat("mm/dd/yyyy");
			String oldDate = invoice.get("date");
			log.info("Original Date: " + oldDate);
			Date dateOut = null;
			try {
				dateOut = inFormat.parse(oldDate);
			} catch (Exception e) {
				e.printStackTrace();
			}
			SimpleDateFormat outFormat = new SimpleDateFormat("MMMM dd, yyyy");
			String invoiceDate = outFormat.format(dateOut);
			log.info("New Date: " + invoiceDate);
			
			//Put values 
			context.putPageValue("fedtaxes", fedtaxes.toString());
			context.putPageValue("provtaxes", provtaxes.toString());
			context.putPageValue("invoiceitems", list);
			context.putPageValue("invoice", invoice);
			context.putPageValue("total", total.toString());
			context.putPageValue("invoicedate", invoiceDate);
		}
	}
}
logs = new ScriptLogger();
logs.startCapture();

try {
	LoadInvoice invoice = new LoadInvoice();
	
	invoice.setLog(logs);
	invoice.setContext(context);
	invoice.setPageManager(pageManager);

	invoice.init();
//	context.putPageValue("export", invoice);
}
finally {
	logs.stopCapture();
} 
