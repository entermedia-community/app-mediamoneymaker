package testing

import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger

public class TestStrings extends EnterMediaObject {
	
	public void testDuplicates() {
		WebPageRequest inReq = context;
		
		Set<String> testSet = new TreeSet<String>();
		testSet.add("C");
		testSet.add("B")
		testSet.add("A");
		testSet.add("B");
		
		Iterator i = testSet.iterator();
		while (i.hasNext()) {
			String s = i.next();
			System.out.println (s);
		}
		inReq.putPageValue("exportset", testSet)
	}
}
log = new ScriptLogger();
log.startCapture();

try {
	log.info("START - TestStrings");
	TestStrings test = new TestStrings();
	test.setLog(log);
	test.setContext(context);
	test.setModuleManager(moduleManager);
	test.setPageManager(pageManager);
	test.testDuplicates();
	log.info("STOP - TestStrings");
}
finally {
	log.stopCapture();
}
