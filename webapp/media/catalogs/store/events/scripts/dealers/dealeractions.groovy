package dealers

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.util.CSVReader
import org.openedit.store.customer.Address
import org.openedit.store.util.MediaUtilities

import com.openedit.OpenEditException
import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.GroovyScriptRunner
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker
import com.openedit.page.Page
import com.openedit.users.DuplicateUserException;
import com.openedit.users.User
import com.openedit.users.UserManager
import com.openedit.users.filesystem.FileSystemGroup
import com.openedit.util.FileUtils

public class DealerActions extends EnterMediaObject {
	
	private int totalCounter;
	private int goodCounter;
	private int badCounter;
	
	private void increaseCounter(String inCounterType) {
		if (inCounterType.equals("total")) {
			if (totalCounter == null) {
				totalCounter = 0;
			}
			totalCounter++;
		} else 	if (inCounterType.equals("good")) {
			if (goodCounter == null) {
				goodCounter = 0;
			}
			goodCounter++;
		} else 	if (inCounterType.equals("bad")) {
			if (badCounter == null) {
				badCounter = 0;
			}
			badCounter++;
		}
	}
	public int getTotalCounter() {
		if (totalCounter == null) {
			totalCounter = 0;
		}
		return totalCounter;
	}
	public int getGoodCounter() {
		if (goodCounter == null) {
			goodCounter = 0;
		}
		return goodCounter;
	}
	public int getBadCounter() {
		if (badCounter == null) {
			badCounter = 0;
		}
		return badCounter;
	}

	public void getAction() {
		
		Log log = LogFactory.getLog(GroovyScriptRunner.class);
		WebPageRequest inReq = context;

		String action = inReq.getRequestParameter("action");
		if (action.equals("add")) {
			createDealer();
		} else if (action.equals("import")) {
			importDealers();
		}
	}
	
	protected void createDealer() {
		
		Log log = LogFactory.getLog(GroovyScriptRunner.class);
		WebPageRequest inReq = context;
		MediaArchive archive = context.getPageValue("mediaarchive");

		SearcherManager manager = archive.getSearcherManager();
		String catalogid = archive.getCatalogId();
		
		MediaUtilities media = new MediaUtilities();
		media.setContext(context);
		
		String dealerID = inReq.getRequestParameter("id");
		String dealerName = inReq.getRequestParameter("name");
		
		Searcher dealersearcher = manager.getSearcher(catalogid, "dealer");
		HitTracker hits = dealersearcher.fieldSearch("id", dealerID);
		if (hits == null || hits.size() == 0) {
			Data newDealer = dealersearcher.createNewData();
			newDealer.setId(dealerID);
			newDealer.setProperty("name", dealerName);
			dealersearcher.saveData(newDealer, inReq.getUser());
			
			inReq.putPageValue("result", "created");
			inReq.putPageValue("results", "Dealer has been created.");
			inReq.putPageValue("dealername", dealerName);
			
		} else {
			inReq.putPageValue("result", "error");
			inReq.putPageValue("results", "Cannot add dealer. ID already exists!");
		}		
	}
	
	protected void importDealers() {
		
		Log log = LogFactory.getLog(GroovyScriptRunner.class);
		WebPageRequest inReq = context;
		MediaArchive archive = context.getPageValue("mediaarchive");

		SearcherManager manager = archive.getSearcherManager();
		String catalogid = archive.getCatalogId();
		
		MediaUtilities media = new MediaUtilities();
		media.setContext(context);
		
		Searcher dealersearcher = manager.getSearcher(catalogid, "dealer");
		Searcher usersearcher = media.getUserSearcher();
		Searcher userprofilesearcher = media.getUserProfileSearcher();
		UserManager usermanager = archive.getModuleManager().getBean("userManager");
		Searcher addresssearcher = manager.getSearcher(catalogid, "address");
		
		Searcher settingsgroupsearcher = manager.getSearcher(catalogid, "settingsgroup");
		Data settingsgroup = settingsgroupsearcher.searchByField("name", "Store Rep");
		if (settingsgroup == null) {
			throw new OpenEditException("Store Rep settings group not available.");
		}
		String settingGroupID = settingsgroup.getId();
		
		Page upload = archive.getPageManager().getPage("/${catalogid}/temp/upload/dealers.csv");
		if (!upload.exists()) {
			
		}
		Reader reader = upload.getReader();
		try
		{
			Map<String, Integer> importColumns = new HashMap<String, Integer>();
			importColumns.put("dealer", new Integer(0));
			importColumns.put("id", new Integer(1));
			importColumns.put("userName", new Integer(2));
			importColumns.put("screenname", new Integer(3));
			importColumns.put("firstName", new Integer(4));
			importColumns.put("lastName", new Integer(5));
			importColumns.put("email", new Integer(6));
			importColumns.put("enabled", new Integer(7));
			importColumns.put("password", new Integer(8));
			importColumns.put("phone", new Integer(9));
			importColumns.put("country", new Integer(10));
			importColumns.put("address1", new Integer(11));
			importColumns.put("address2", new Integer(12));
			importColumns.put("city", new Integer(13));
			importColumns.put("state_province", new Integer(14));
			importColumns.put("postal_code", new Integer(15));
		
			//Create CSV reader
			CSVReader read = new CSVReader(reader, ',', '\"');
			
			//Read 1 line of header
			String[] headers = read.readNext();
			
			String[] orderLine;
			while ((orderLine = read.readNext()) != null)
			{
				int colID = (Integer)importColumns.get("dealer");
				HitTracker hits = dealersearcher.fieldSearch("id", orderLine[colID]);
				if (hits != null && hits.size() > 0) {
					
					String userName = orderLine[(Integer)importColumns.get("userName")];
					String password = orderLine[(Integer)importColumns.get("password")];
					try {
						User user = usermanager.createUser(userName, password);
						if (user != null) { 
							Set es = importColumns.entrySet();
							Iterator it = es.iterator();
							while (it.hasNext()) {
								Map.Entry item = (Map.Entry) it.next();
								String colName = (String)item.getKey();
								if ((!colName.equals("userName")) && (!colName.equals("password")) && (!colName.equals("dealer"))) {
									int colNumber = (Integer)item.getValue();
									String colData = orderLine[colNumber];
									user.setProperty(colName, colData);
								} else if (!colName.equals("id")) {
									int colNumber = (Integer)item.getValue();
									String colData = orderLine[colNumber];
									user.setId(orderLine[colNumber]);
								}
							}
							FileSystemGroup group = new FileSystemGroup();
							group.setName("users");
							user.addGroup(group);
							usersearcher.saveData(user, inReq.getUser());
							
							Data address = addresssearcher.searchById(userName);
							if (address == null) { 
								address = addresssearcher.createNewData();
								address.setId(userName);
								address.setProperty("name", userName);
								address.setProperty("address1", orderLine[(Integer)importColumns.get("address1")]);
								address.setProperty("address2", orderLine[(Integer)importColumns.get("address2")]);
								address.setProperty("city", orderLine[(Integer)importColumns.get("city")]);
								address.setProperty("state", orderLine[(Integer)importColumns.get("state_province")]);
								address.setProperty("zip", orderLine[(Integer)importColumns.get("postal_code")]);
								address.setProperty("userprofile", userName);
								address.setProperty("storenumber", userName);
								address.setProperty("dealer", orderLine[colID]);
								addresssearcher.saveData(address, user);
							} else {
								log.info("Address Exists: " + userName);
							}

							Data profile = userprofilesearcher.searchById(userName);
							//Create the user profile
							if (profile == null) {
								profile = userprofilesearcher.createNewData();
								profile.setId(userName);
								profile.setProperty("userid", user.getId());
								profile.setProperty("password", user.getPassword());
								profile.setProperty("firstName", user.getFirstName());
								profile.setProperty("lastName", user.getLastName());
								profile.setProperty("email", user.getEmail());
								profile.setProperty("enabled", "true");
								profile.setProperty("settingsgroup", settingGroupID);
								profile.setProperty("address", userName);
								userprofilesearcher.saveData(profile, user);
							} else {
								log.info("Profile Exists: " + userName);
							}
							increaseCounter("good");
							
						} else {
							log.info("User Exists: " + orderLine[colID]);
						}
					} catch (DuplicateUserException ex) {
						log.info("SKIPPING: User Already Exists: " + orderLine[colID]);
						log.info(ex.getMessage());
						increaseCounter("bad");
					}
				} else {
					log.info("Invalid Dealer: " + orderLine[colID]);
				}
				increaseCounter("total");
			}
		}
		finally
		{
			FileUtils.safeClose(reader);
		}
		inReq.putPageValue("total", getTotalCounter().toString());
		inReq.putPageValue("good", getGoodCounter().toString());
		inReq.putPageValue("bad", getBadCounter().toString());
	}

}
logs = new ScriptLogger();
logs.startCapture();

try {
	DealerActions dealerAction = new DealerActions();
	dealerAction.setLog(logs);
	dealerAction.setContext(context);
	dealerAction.setModuleManager(moduleManager);
	dealerAction.setPageManager(pageManager);
	dealerAction.getAction();
}
finally {
	logs.stopCapture();
}
