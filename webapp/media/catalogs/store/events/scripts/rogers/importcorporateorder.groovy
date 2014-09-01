import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.util.CSVReader
import org.openedit.store.Product
import org.openedit.util.DateStorageUtil

import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.Page


public void init(){
	log.info("Importing BUDDTL to generate Corporate Orders");
	
	WebPageRequest req = context;
	MediaArchive archive = req.getPageValue("mediaarchive");
	
	Page upload = archive.getPageManager().getPage("/${catalogid}/temp/upload/rogers_buddtl.csv");
	if (!upload.exists()) {
		req.putPageValue("error","Cannot load uploaded file");
		return;
	}
	
	StringBuilder buf = new StringBuilder();
	String delimiter = guessCellDelimiter(upload);
	
	Reader reader = upload.getReader();
	CSVReader csvreader = new CSVReader(reader, delimiter.charAt(0), "\"".charAt(0));
	List<?> rows = csvreader.readAll();
	Iterator<?> itr = rows.iterator();
	if (itr.hasNext() == false){
		req.putPageValue("error","Cannot parse file");
		return;
	}
	String [] ids = itr.next();
	Map<String,?> map = getProducts(archive,ids);
	Map<String,Integer> inventory = new HashMap<String,Integer>();
	
	Searcher corporateordersearcher = archive.getSearcher("corporateorder");
	Searcher storesearcher = archive.getSearcher("rogersstore");
	
	String uuid = UUID.randomUUID().toString();
	String date = DateStorageUtil.getStorageUtil().formatForStorage(new Date());
	List orders = new ArrayList();
	
	while(itr.hasNext()){
		String [] cells = itr.next();
		String storeid = cells[0].trim();
		Data store = storeid ? storesearcher.searchById(storeid) : null;
		for(int i=1; i < cells.length; i++){
			int index = i + 10;
			int quantity = toInt(cells[i].trim(),0);
			String status = "ok";
			int inStock = 0;
			int delta = 0;
			Data order = corporateordersearcher.createNewData();
			order.setName("${req.getUser().getId()}");
			order.setProperty("date", date);
			order.setProperty("store",storeid);
			order.setProperty("orderno", "${index}");
			order.setProperty("quantity","$quantity");
			Product product = map.get("$i") instanceof Product ? map.get("$i") : null;
			if (product){
				order.setProperty("product",product.get("as400id"));
				if (quantity > 0 && product.getInventoryItem(0) != null){
					inStock = product.getInventoryItem(0).getQuantityInStock();
					if (inventory.containsKey(product.getId()) == false){
						inventory.put(product.getId(), new Integer(inStock));
					} else {
						inStock = inventory.get(product.getId()).intValue();
					}
					delta = inStock;
					if (inStock <= 0){
						status = "outofstock";
					} else if (inStock < quantity) {
						status = "insufficientstock";
					} else {
						if (store){
							delta = inStock - quantity;
							inventory.put(product.getId(), new Integer(delta));
						}
					}
				} else if (product.getInventoryItem(0) == null) {
					status = "inventoryerror";
				}
			} else {
				status = "productunknown";
				order.setProperty("product",map.get("$i"));
			}
			if (store){
				//keep product status as is
			}
			else {
				status = "storeunknown" ;
			}
			order.setProperty("inventory", "${inStock}");
			order.setProperty("delta", "${delta}");
			order.setProperty("status",status);
			order.setProperty("uuid",uuid);
			orders.add(order);
		}
	}
	//save orders
	corporateordersearcher.saveAllData(orders, null);
	req.putPageValue("uuid", uuid);
	log.info("Finishing BUDDTL import");
}

public String guessCellDelimiter(Page inPage){
	Reader reader = null;
	try{
		reader = inPage.getReader();
		CSVReader csvreader = new CSVReader(reader, ",".charAt(0), "\"".charAt(0));
		String [] comma = csvreader.readNext();
		reader.close();
		if (comma && comma.length!=1){
			return ",";
		}
		reader = inPage.getReader();
		csvreader = new CSVReader(reader, "\t".charAt(0), "\"".charAt(0));
		String [] tab = csvreader.readNext();
		reader.close();
		if (tab && tab.length!=1){
			return "\t";
		}
	} finally{
		try{
			reader.close();
		}catch(Exception e){}
	}
	return " ";
}

public Map<String,?> getProducts(MediaArchive archive,String [] ids){
	Searcher productsearcher = archive.getSearcher("product");
	Map<String,Product> map = new HashMap<String,Product>();
	int index = 0;
	ids.each{
		if (it){
			index++;
			Data data = productsearcher.searchByField("as400id", it);
			if (data){
				Product product = productsearcher.searchById(data.id);
				if (product) map.put("$index", product);
				else map.put("$index",it);
			} else {
				map.put("$index",it);
			}
		}
	}
	return map;
}

public int toInt(String inVal, int inDefault){
	int out = inDefault;
	try{
		out = Integer.parseInt(inVal);
	}catch(Exception e){}
	return out;
}


init();