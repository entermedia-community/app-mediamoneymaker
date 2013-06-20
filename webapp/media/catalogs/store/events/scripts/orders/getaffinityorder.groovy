package orders

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.util.CSVReader
import org.openedit.entermedia.util.CSVWriter

import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.GroovyScriptRunner
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.page.Page

public class GetAffinityOrder extends EnterMediaObject {
	
	private static String distributorName = "Affinity";
	
	public void getOrder() {
		
		WebPageRequest inReq = context;
		Log log = LogFactory.getLog(GroovyScriptRunner.class);
		MediaArchive archive = context.getPageValue("mediaarchive");
		String catalogid = getMediaArchive().getCatalogId();
		
		String orderID = inReq.getRequestParameter("orderid");
		String fileName = "export-" + this.distributorName.replace(" ", "-") + "-" + orderID + ".csv";
		String orderFolder = "/WEB-INF/data/" + catalogid + "/orders/exports/" + orderID + "/";
		Page page = pageManager.getPage(orderFolder + fileName);
		if (page.exists()) {
			
			Reader inputFile = page.getReader();
			CSVReader reader = new CSVReader(inputFile, (char)',');
			
			//Create the CSV Writer Objects
			StringWriter output  = new StringWriter();
			CSVWriter writer  = new CSVWriter(output, (char)',');
			
			String[] csvLine;
			while ((csvLine = reader.readNext()) != null)
			{
				try	{
					log.info("Export: " + csvLine.toString())
					writer.writeNext(csvLine);
				}
				catch (Exception e) {
					log.info(e.message);
					log.info(e.getStackTrace().toString());
				}
			}
			String finalout = output.toString();
			inReq.putPageValue("exportcsv", finalout);
		} else {
			log.info(orderID + " order file (" + fileName + ") does not exist.");
		}
	}
}
log = new ScriptLogger();
log.startCapture();

try {

	log.info("PROCESS: START: GetAffinityOrder");
	GetAffinityOrder getAffinityOrder = new GetAffinityOrder();
	getAffinityOrder.setLog(log);
	getAffinityOrder.setContext(context);
	getAffinityOrder.setModuleManager(moduleManager);
	getAffinityOrder.setPageManager(pageManager);

	getAffinityOrder.getOrder();
	log.info("PROCESS: END: GetAffinityOrder");
}
finally {
	log.stopCapture();
}
