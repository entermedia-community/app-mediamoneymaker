package orders;

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openedit.data.*
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.PublishResult
import org.openedit.entermedia.util.CSVWriter

import com.openedit.OpenEditException
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.GroovyScriptRunner
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.page.Page


public class ExportAffinity extends EnterMediaObject {

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

		String finalOutput = "";
		log.info("PROCESS: START Orders.exportaffinity");

		Log log = LogFactory.getLog(GroovyScriptRunner.class);
		MediaArchive archive = context.getPageValue("mediaarchive");
		String catalogid = getMediaArchive().getCatalogId();
		boolean production = Boolean.parseBoolean(context.findValue('productionmode'));

		// Create the Searcher Objects to read values!
		def orderid = context.getRequestParameter("orderid");
		def String distributorName = "Affinity";

		//Create the CSV Writer Objects
		StringWriter output  = new StringWriter();

		// xml generation
		String fileName = "export-" + this.distributorName.replace(" ", "-") + ".csv";
		Page page = pageManager.getPage("/WEB-INF/data/${catalogid}/orders/exports/${this.orderID}/${fileName}");
		output.append(page.getContent());
		
		result.setCompleteMessage(output.toString());
		result.setComplete(true);
		
		return result;
	}
}
PublishResult result = new PublishResult();

logs = new ScriptLogger();
logs.startCapture();

try {
	ExportAffinity export = new ExportAffinity();
	export.setLog(logs);
	export.setContext(context);
	export.setOrderID(context.getRequestParameter("orderid"));
	export.setModuleManager(moduleManager);
	export.setPageManager(pageManager);
	result = export.doExport();
	
	if (result.isComplete()) {
		//Output value to CSV file!
		export.getContext().putPageValue("exportcsv", result.getCompleteMessage());
	} else {
		//ERROR: Throw exception
		throw new OpenEditException("Error: " + result.getErrorMessage());
	}
}
finally {
	logs.stopCapture();
}
