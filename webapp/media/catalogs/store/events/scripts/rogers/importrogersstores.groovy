package rogers

/*
 * Created on October 4th, 2012
 * Created by Peter Floyd
 */

//Import List
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.util.CSVReader
import org.openedit.store.util.MediaUtilities

import com.openedit.BaseWebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.page.Page

public class ImportRogersStores extends EnterMediaObject {
	
	ArrayList<String> goodStoreList;
	ArrayList<String> badStoreList;
	

	public void addToGoodList( String inValue ) {
		if (goodStoreList == null) {
			goodStoreList = new ArrayList<String>();
		}
		goodStoreList.add(inValue);
	}

	public void addToBadList( String inValue ) {
		if (badStoreList == null) {
			badStoreList = new ArrayList<String>();
		}
		badStoreList.add(inValue);
	}
	
	public ArrayList<String> getGoodStoreList() {
		return this.goodStoreList;
	}
	public ArrayList<String> getBadStoreList() {
		return this.badStoreList;
	}

	public void storeImport() 
	{
		//Create Store, MediaArchive Object
		
		BaseWebPageRequest inReq = context;
		
		MediaUtilities media = new MediaUtilities();
		media.setContext(context);

		MediaArchive archive = media.getArchive();
		String catalogid = media.getCatalogid();
		Searcher storesearcher = media.getManager().getSearcher(archive.getCatalogId(), "rogersstore");

		//Define columns from spreadsheet
		def int columnRogersID = 0;
		def int columnStoreName = 1;
		def int columnAddress1 = 2;
		def int columnCity = 3;
		def int columnProvince = 4;
		def int columnPostalCode = 5;
		def int columnPhone1 = 6;
		def int columnBrand = 7
		
		Page upload = archive.getPageManager().getPage("/${catalogid}/temp/upload/rogers_store.csv");
		Reader reader = upload.getReader();
		//Create CSV reader
		CSVReader read = new CSVReader(reader, ',', '\"');

		//Read 1 line of header
		String[] headers = read.readNext();
		
		String[] storeLine;
		while ((storeLine = read.readNext()) != null)
		{
			def storeNum = storeLine[columnRogersID].trim();

			Data hit = storesearcher.searchById(storeNum);
			if (hit == null) {
				Data newStore = storesearcher.createNewData();
				newStore.setId(storeNum);
				newStore.setName(storeLine[columnStoreName]);
				newStore.setProperty("address1", storeLine[columnAddress1]);
				newStore.setProperty("city", storeLine[columnCity]);
				newStore.setProperty("province", storeLine[columnProvince]);
				newStore.setProperty("postalcode", storeLine[columnPostalCode]);
				newStore.setProperty("phone1", storeLine[columnPhone1]);
				newStore.setProperty("brand", storeLine[columnBrand]);
				storesearcher.saveData(newStore, context.getUser());
				log.info("Store Added: " + storeLine[columnStoreName]);
				addToGoodList(storeLine[columnStoreName]);
			} else {
				log.info("Store Exists: " + storeLine[columnStoreName]);
				addToBadList(storeLine[columnStoreName]);
			}
		}
	}
}

logs = new ScriptLogger();
logs.startCapture();
try {
	ImportRogersStores importStores = new ImportRogersStores();
	importStores.setLog(logs);
	importStores.setContext(context);
	importStores.setModuleManager(moduleManager);
	importStores.setPageManager(pageManager);
	importStores.storeImport();
	context.putPageValue("goodlist", importStores.getGoodStoreList());
	context.putPageValue("badlist", importStores.getBadStoreList());
}
finally {
	logs.stopCapture();
}
