package inventory

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

public class ExportInventoryCSV extends EnterMediaObject {

	public void doExport() {
		
		Log log = LogFactory.getLog(GroovyScriptRunner.class);
		WebPageRequest inReq = context;

		ArrayList<String> outputList = new ArrayList<String>();
		
		MediaArchive archive = context.getPageValue("mediaarchive");
		String catalogid = getMediaArchive().getCatalogId();
		
		String inFilename = inReq.getRequestParameter("filename");
		String inventoryFolder = "/WEB-INF/data/" + catalogid + "/inventory/";
		Page page = pageManager.getPage(inventoryFolder + inFilename);
		
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
			inReq.putPageValue("export", finalout);
		} else {
			log.info("Page does not exist! Page: " + page.getName());
		}
	}
	
}

log = new ScriptLogger();
log.startCapture();

try {

	log.info("START - ExportInventoryCSV");
	ExportInventoryCSV exportInventory = new ExportInventoryCSV();
	exportInventory.setLog(log);
	exportInventory.setContext(context);
	exportInventory.setModuleManager(moduleManager);
	exportInventory.setPageManager(pageManager);
	exportInventory.doExport();
	log.info("FINISH - ExportInventoryCSV");
}
finally {
	log.stopCapture();
}
