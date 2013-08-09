package pages

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive

import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.GroovyScriptRunner
import com.openedit.entermedia.scripts.ScriptLogger


public class GetPageInfo extends EnterMediaObject {
	
	private Map<String, String> searchValues;

	// SETTERS	
	public void setSearchValues( String inKey, String inValue) {
		
		if (searchValues == null) {
			searchValues = new HashMap<String, String>();
		}
		searchValues.put(inKey, inValue);
	}
	// GETTERS
	public HashMap getSearchValues() {
		if (searchValues == null) {
			searchValues = new HashMap<String, String>();
		}
		return searchValues;
	}
	public String getSearchValue( inKey ) {
		String result;
		if (searchValues == null) {
			result = "";
			return result;
		} else {
			result = (String) searchValues.get(inKey);
			if (result != null && result.length() > 0) {
				return result;
			} else {
				return "";
			}
		}
	}
	
	public void getInfo() {
		
		ArrayList values = new ArrayList();
		//Get Media Info
		Log log = LogFactory.getLog(GroovyScriptRunner.class);
		
		WebPageRequest inReq = context;

		MediaArchive archive = inReq.getPageValue("mediaarchive");
		SearcherManager manager = archive.getSearcherManager();
		String catalogid = archive.getCatalogId();
		
		Map<String, String> reqs = inReq.getParameterMap();
		Iterator entries = reqs.entrySet().iterator();
		while (entries.hasNext()) {
			Map.Entry entry = (Map.Entry) entries.next();
			String key = (String) entry.getKey();
			if (key.equals("field")) {
				if (entry.getValue() instanceof String[]) {
					for(String e in entry.getValue()) {
						log.info("field element ${e}");
						if (!e.contains(":")){
							setSearchValues(e, "");
							continue;
						}
						String[] s = e.split(":");
						setSearchValues(s[2], "");
					}
				} else {
					setSearchValues(entry.getValue(), "");
				}
			} else if (key.equals("query")) {
				String eValue = entry.getValue()
				inReq.putPageValue("query", eValue);
				log.info("Query:" + eValue);
				return;
			} else if (key.toLowerCase().contains(".value")) {
				String[] field = key.split("\\."); 
				setSearchValues(field[0], entry.getValue());
			}
		}
		if (getSearchValues().size() == 1) {
			Map.Entry<String, String> entry = getSearchValues().entrySet().iterator().next();
			if (entry != null) {
				String searchField = entry.getKey();
				if (searchField.equals("manufacturerid")) {
					searchField = "manufacturer";
				}
				log.info("searchfield: " + searchField);
				switch (searchField) {
					case "manufacturer": 
						inReq.putPageValue("searchfield", "Manufacturer");
						break;
					case "categoryid": 
						inReq.putPageValue("searchfield", "Product Category");
						break;
					case "name":
						inReq.putPageValue("searchfield", "Name");
						break;
					case "phone":
						inReq.putPageValue("searchfield", "Phone");
						break;
					default:
						inReq.putPageValue("searchfield", searchField);
						break;
				}
				String searchValue = entry.getValue();
				Searcher categorysearcher = manager.getSearcher(catalogid, searchField)
				Data cat = categorysearcher.searchById(searchValue);
				if (cat != null) {
					log.info(cat.getName());
					inReq.putPageValue("searchfor", cat.getName());
					inReq.putPageValue("category", searchField);
					inReq.putPageValue("categoryvalue", searchValue);
					inReq.putPageValue("action", "single");
				}
			}
		} else {
			Iterator searchEntries = getSearchValues().iterator();
			String searchField = "";
			String searchFor = "";
			while (searchEntries.hasNext()) {
				Map.Entry entry = (Map.Entry) searchEntries.next();
				String entryValue = (String) entry.getValue();
				if (entryValue != null && entryValue.length() > 0) {
					String key = (String) entry.getKey();
					log.info("key: " + key);
					log.info("value: " + entryValue);
					switch (key) {
						case "manufacturerid": 
							searchField += "Manufacturer";
							break;
						case "categoryid": 
							searchField += "Category";
							break;
						case "name":
							searchField += "Name";
							break;
						case "phone":
							searchField += "Phone";
							break;
						default:
							searchField += key;
							break;
					}
					if (key != "name") {
						Searcher datasearcher = manager.getSearcher(catalogid, key)
						Data dataResults = datasearcher.searchById(entryValue);
						if (dataResults != null) {
							searchFor += dataResults.getName();
						}
					} else {
						searchFor += "\"" + entryValue + "\"";
					}
					if (searchEntries.hasNext()) {
						searchField += ", ";
						searchFor += ", ";
					}
				}
			}
			inReq.putPageValue("searchfield", searchField);
			inReq.putPageValue("searchfor", searchFor);
			inReq.putPageValue("action", "multiple");
		}
	}
}

log = new ScriptLogger();
log.startCapture();

try {

	log.info("START - GetPageInfo");
	GetPageInfo getPageInfo = new GetPageInfo();
	getPageInfo.setLog(log);
	getPageInfo.setContext(context);
	getPageInfo.setModuleManager(moduleManager);
	getPageInfo.setPageManager(pageManager);
	getPageInfo.getInfo();
	log.info("FINISH - GetPageInfo");
}
finally {
	log.stopCapture();
}

	
