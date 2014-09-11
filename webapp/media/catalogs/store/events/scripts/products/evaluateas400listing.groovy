package products

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.util.CSVReader

import com.openedit.WebPageRequest
import com.openedit.page.Page
import com.openedit.util.FileUtils

public void init(){
	log.info("Starting Evaluation of Rogers AS400 IDs");
	WebPageRequest req = context;
	MediaArchive archive = req.getPageValue("mediaarchive");
	Searcher productsearcher = archive.getSearcher("product");
	Page upload = archive.getPageManager().getPage("/${archive.getCatalogId()}/temp/upload/as400list.csv");
	readCSVFile(req,upload,productsearcher);
}

public void readCSVFile(WebPageRequest req, Page csvfile, Searcher productsearcher){
	
	Map<String,List<String>> as400Map = new HashMap<String,ArrayList<String>>();
	
	int columnAS400id = 0;
	String colHeadAS400ID = "INUMBR";
	int columnRogersID = 5;
	String colHeadROGERSID = "IVNDP#";
	int department = 1;
	String colHeadISDEPT = "ISDEPT";
	int type = 10;
	String colHeadISTYPE = "ISTYPE";
	
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
				return;
			}
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
				continue;
			}
			if(rogerssku){
				Data product = productsearcher.searchByField("rogerssku", rogerssku.replace("/", "\\/"));
				if (product) {
					List<String> info = new ArrayList<String>();
					if (as400Map.containsKey(product.getId())){
						info = as400Map.get(product.getId());
					} else {
						as400Map.put(product.getId(), info);
					}
					String details = "${as400id}," + (dept == "74" ? "Rogers" : "Fido");
					info.add(details);
				}
			}
		}
	}
	finally
	{
		FileUtils.safeClose(reader);
		StringBuilder output = new StringBuilder();
		Iterator<String> itr = as400Map.keySet().iterator();
		while(itr.hasNext()){
			String key = itr.next();
			List<String> list = as400Map.get(key);
			String toString = "${list}".replace("[","").replace("]","");
			output.append("$key,$toString").append("\n");
		}
		req.putPageValue("export",output.toString());
	}
}

init();
