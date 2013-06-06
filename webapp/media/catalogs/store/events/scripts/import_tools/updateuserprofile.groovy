package import_tools

/*
 * Created on October 4th, 2012
 * Created by Peter Floyd
 */

//Import List
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.util.CSVReader
import org.openedit.store.Product
import org.openedit.store.util.MediaUtilities

import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.GroovyScriptRunner
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.page.Page
import com.openedit.users.User
import com.openedit.util.FileUtils

public class UpdateUserProfile extends EnterMediaObject {

	private ArrayList<String> badProductList;
	private ArrayList<String> goodProductList;
	
	private void addToBadProductList( String inValue ) {
		if (badProductList == null) {
			badProductList = new ArrayList<String>();
		}
		badProductList.add(inValue);
	}

	private void addToGoodProductList( String inValue ) {
		if (goodProductList == null) {
			goodProductList = new ArrayList<String>();
		}
		goodProductList.add(inValue);
	}
	
	private ArrayList<String> getBadProductList() {
		if (badProductList == null) {
			badProductList = new ArrayList<String>();
		}
		return badProductList;
	}

	private ArrayList<String> getGoodProductList() {
		if (goodProductList == null) {
			goodProductList = new ArrayList<String>();
		}
		return goodProductList;
	}

	public void doImport(){
		//Create Store, MediaArchive Object

		Log log = LogFactory.getLog(GroovyScriptRunner.class);
		WebPageRequest inReq = context;
		MediaArchive archive = context.getPageValue("mediaarchive");

		SearcherManager manager = archive.getSearcherManager();
		String catalogid = archive.getCatalogId();
		
		MediaUtilities media = new MediaUtilities();
		media.setContext(context);
		Searcher productSearcher = media.getProductSearcher();
		Searcher userProfileSearcher = media.getUserProfileSearcher();
		
		//Create Searcher Object
		//Define columns from spreadsheet
		/*
		 * AccessoryID	AccessoryName	UPC	ManufacturerID	RogersSKU	ManufacturerSKU	ParticipantID
		 */
		def int columnProductID = 0;
		def String columnHeadProductID = "Product ID";
		def int columnUserProfileID = 1;
		def String columnHeadUserProfileID = "Profile ID";

		String pageName = "/" + media.getCatalogid() + "/temp/upload/update.csv";
		Page upload = media.getArchive().getPageManager().getPage(pageName);
		Reader reader = upload.getReader();
		try
		{
			boolean done = false;

			//Create CSV reader
			CSVReader read = new CSVReader(reader, ',', '\"');

			//Read 1 line of header
			String[] headers = read.readNext();
			
			String[] orderLine;
			while ((orderLine = read.readNext()) != null)
			{
				//Create as400list Searcher
				//Read the oraclesku from the as400 table
				String inProductID = orderLine[columnProductID];
				Product product = productSearcher.searchById(inProductID);
				if (product != null) {
					String inUserProfileID = orderLine[columnUserProfileID];
					Data userProfile = userProfileSearcher.searchById(inUserProfileID);
					if (userProfile != null) {
						Data newInfo = productSearcher.createNewData();
						newInfo.setProperty("profileid", inUserProfileID);
						productSearcher.saveData(newInfo, inReq.getUser());
						addToGoodProductList(inProductID + " updated.");
					} else {
						addToBadProductList("Invalid UserProfile(" + inUserProfileID +  ") for product (" + inProductID + ")");
					}
				} else {
					addToBadProductList("Invalid Product (" + inProductID + ")");
				}
			}
			inReq.putPageValue("goodlist", getGoodProductList());
			inReq.putPageValue("badlist", getBadProductList());
		}
		finally
		{
			FileUtils.safeClose(reader);
		}
	}

	public String addQuotes( String s ) {
		return "\"" + s + "\"";
	}
}

logs = new ScriptLogger();
logs.startCapture();

try {

	UpdateUserProfile updateUsers = new UpdateUserProfile();
	updateUsers.setLog(logs);
	updateUsers.setContext(context);
	updateUsers.setModuleManager(moduleManager);
	updateUsers.setPageManager(pageManager);

	updateUsers.doImport();
}
finally {
	logs.stopCapture();
}
