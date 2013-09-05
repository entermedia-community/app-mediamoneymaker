package testing

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openedit.entermedia.MediaArchive

import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.GroovyScriptRunner
import com.openedit.entermedia.scripts.ScriptLogger

public class TestTemplate  extends EnterMediaObject {
	
	public void doTest() {
		
		Log log = LogFactory.getLog(GroovyScriptRunner.class);
		
		WebPageRequest inReq = context;

		MediaArchive archive = inReq.getPageValue("mediaarchive");
		String catalogid = archive.getCatalogId();
		
	}
}

log = new ScriptLogger();
log.startCapture();

try {

	log.info("START - TestMap");
	TestTemplate test = new TestTemplate()();
	test.setLog(log);
	test.setContext(context);
	test.setModuleManager(moduleManager);
	test.setPageManager(pageManager);
	test.doTest();
	log.info("FINISH - TestMap");
}
finally {
	log.stopCapture();
}
