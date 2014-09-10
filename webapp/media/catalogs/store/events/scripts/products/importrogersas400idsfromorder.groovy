package products

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.util.CSVReader
import org.openedit.store.Product
import org.openedit.store.Store
import org.openedit.util.DateStorageUtil

import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.page.Page
import com.openedit.util.FileUtils

public void init(){
	log.info("Starting Import Rogers AS400 IDs");
	
	WebPageRequest req = context;
	MediaArchive archive = req.getPageValue("mediaarchive");
	Store store = req.getPageValue("store");
	Searcher productsearcher = store.getProductSearcher();
	Searcher updatesearcher = archive.getSearcher("productupdates");
	Page upload = archive.getPageManager().getPage("/${archive.getCatalogId()}/temp/upload/rogers_order.csv");
	readCSVFile(req,upload,productsearcher,updatesearcher);
}

public void readCSVFile(WebPageRequest req, Page csvfile, Searcher productsearcher, Searcher updatesearcher){
	List ignored = new ArrayList();
	List notfound = new ArrayList();
	StringBuilder error = new StringBuilder();
	Map<String,Data> map = new HashMap<String,Data>();
	
	int columnAS400id = 0;
	String colHeadAS400ID = "INUMBR";
	int columnRogersID = 5;
	String colHeadROGERSID = "IVNDP#";
	int department = 1;
	String colHeadISDEPT = "ISDEPT";
	int type = 10;
	String colHeadISTYPE = "ISTYPE";
	
	String uuid = UUID.randomUUID().toString();
	String date = DateStorageUtil.getStorageUtil().formatForStorage(new Date());
	
	Reader reader = csvfile.getReader();
	try
	{
		CSVReader csvreader = new CSVReader(reader, ',', '\"');
		String[] headers = csvreader.readNext();
		
		List<Integer> columnNumbers = new ArrayList<Integer>();
		columnNumbers.add(columnAS400id);
		columnNumbers.add(columnRogersID);
		columnNumbers.add(department);
		columnNumbers.add(type);

		List<String> columnNames = new ArrayList<String>();
		columnNames.add(colHeadAS400ID);
		columnNames.add(colHeadROGERSID);
		columnNames.add(colHeadISDEPT);
		columnNames.add(colHeadISTYPE);
		
		for ( int i=0; i < columnNumbers.size(); i++ ) {
			int number = columnNumbers.get(i);
			String header = headers[number];
			String column = columnNames.get(i);
			if ( header != column) {
				error.append("<li><strong>${header}</strong>: column #${number} is incorrect, expecting <strong>${columnNames.get(i)}</strong></li>");
			}
		}
		
		if (error.toString().size()!=0){
			return;
		}
		
		String[] line;
		for ( int i = 1; (line = csvreader.readNext()) != null; i++){
			//product info
			String rogerssku = line[columnRogersID].trim();
			String as400id = line[columnAS400id].trim();
			String dept = line[department].trim();
			//process only 01, 02, 03
			String istype = line[type].trim();
			if ("01".equals(istype) == false && "02".equals(istype) == false && "03".equals(istype) == false){
				//add to ignored list
				ignored.add(["${i}","${as400id}","${rogerssku}","${dept}","$istype"]);
				continue;
			}
			if(rogerssku){
				Data product = productsearcher.searchByField("rogerssku", rogerssku.replace("/", "\\/"));
				if (product) {
					Data update = null;
					if (map.containsKey(product.getId()) == false){
						update = updatesearcher.createNewData();
						update.setName("${req.getUser().getId()}");
						update.setProperty("date", date);
						update.setProperty("productid",product.getId());
						update.setProperty("uuid", uuid);
						map.put(product.getId(), update);
					}
					else {
						update = map.get(product.getId());
					}
					//rogers: 74, fido: 174
					if (dept == "74") update.setProperty("rogersas400id",as400id);
					else update.setProperty("fidoas400id",as400id);
				}
				else {
					//id does not exist
					notfound.add(["${i}","${as400id}","${rogerssku}","${dept}","$istype"]);
				}
			}
			else {
				//rogerssku not defined
				notfound.add(["${i}","${as400id}","${rogerssku}","${dept}","$istype"]);
			}
		}
	}
	finally
	{
		FileUtils.safeClose(reader);
		req.putPageValue("uuid", uuid);
		if (error.toString().size()!=0){
			req.putPageValue("errorout", "<ul>${error.toString()}</ul>");
		}
		if (ignored.isEmpty() == false){
			ignored.add(0, ["Row#","${colHeadAS400ID}","${colHeadROGERSID}","${colHeadISDEPT}","$colHeadISTYPE"]);
			req.putPageValue("ignored", ignored);
		}
		if (notfound.isEmpty() == false){
			notfound.add(0, ["Row#","${colHeadAS400ID}","${colHeadROGERSID}","${colHeadISDEPT}","$colHeadISTYPE"]);
			req.putPageValue("notfound", notfound);
		}
		if (map.isEmpty() == false){
			List list = new ArrayList();
			Iterator<String> itr = map.keySet().iterator();
			while(itr.hasNext()){
				Data data = map.get(itr.next());
				if (data){
					list.add(data);
					if (list.size() == 1000){
						updatesearcher.saveAllData(list, null);
						list.clear();
					}
				}
				
			}
			if (list.isEmpty() == false){
				updatesearcher.saveAllData(list, null);
				list.clear();
			}
		}
	}
}

init();
