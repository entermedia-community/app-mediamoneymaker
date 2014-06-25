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

	public void doExport() {

		String finalOutput = "";
		log.info("PROCESS: START Orders.exportaffinity");

		Log log = LogFactory.getLog(GroovyScriptRunner.class);
		MediaArchive archive = context.getPageValue("mediaarchive");
		String catalogid = getMediaArchive().getCatalogId();
		boolean production = Boolean.parseBoolean(context.findValue('productionmode'));

		// Create the Searcher Objects to read values!
		def orderid = context.getRequestParameter("id");
		if (orderid == null){
			orderid = context.getRequestParameter("orderid");
		}
		setOrderID(orderid);
		def String distributorName = "Affinity";

		//Create the CSV Writer Objects
		StringWriter output  = new StringWriter();

		// xml generation
		String fileName = "export-" + this.distributorName.replace(" ", "-") + "-" + getOrderID() + ".csv";
		String pageName = "/WEB-INF/data/" + catalogid + "/orders/exports/" + getOrderID() + "/" + fileName;
		Page page = pageManager.getPage(pageName);
		if (page != null) {
			output.append(page.getContent());
			context.putPageValue("export", output.toString());
		} else {
			throw new OpenEditException(fileName + " does not exist.");
		}
	}
}
logs = new ScriptLogger();
logs.startCapture();

try {
	ExportAffinity export = new ExportAffinity();
	export.setLog(logs);
	export.setContext(context);
	export.setOrderID(context.getRequestParameter("orderid"));
	export.setModuleManager(moduleManager);
	export.setPageManager(pageManager);
	export.doExport();
}
finally {
	logs.stopCapture();
}
