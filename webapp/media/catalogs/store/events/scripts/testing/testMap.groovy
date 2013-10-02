package testing

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openedit.entermedia.MediaArchive

import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.GroovyScriptRunner
import com.openedit.entermedia.scripts.ScriptLogger

public class TestMap  extends EnterMediaObject {
	
	Map<String, String> testingMap;
	public void doTest() {
		
		Log log = LogFactory.getLog(GroovyScriptRunner.class);
		
		WebPageRequest inReq = context;

		MediaArchive archive = inReq.getPageValue("mediaarchive");
		String catalogid = archive.getCatalogId();
		
		testingMap = new HashMap<String, ArrayList>();
		
		testingMap.put("test123", "123.csv");
		testingMap.put("test234", "423.csv");
		
		context.putPageValue("testmap", testingMap);

	}
}

log = new ScriptLogger();
log.startCapture();

try {

	log.info("START - TestMap");
	TestMap testMap = new TestMap();
	testMap.setLog(log);
	testMap.setContext(context);
	testMap.setModuleManager(moduleManager);
	testMap.setPageManager(pageManager);
	testMap.doTest();
	log.info("FINISH - TestMap");
}
finally {
	log.stopCapture();
}
