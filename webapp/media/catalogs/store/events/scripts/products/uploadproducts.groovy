import org.openedit.Data;
import org.entermedia.upload.UploadRequest
import org.openedit.Data
import org.openedit.data.PropertyDetail
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.util.CSVReader
import org.openedit.store.Store

import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.Page
import com.openedit.page.manage.PageManager

public void init() {
	log.info("Uploading products");
	
	WebPageRequest req = context;
	Store store = req.getPageValue("store");
	MediaArchive archive = req.getPageValue("mediaarchive");
	Searcher productsearcher = store.getProductSearcher();
	
	//Create the searcher objects.	 
	SearcherManager manager = archive.getSearcherManager();
	Searcher distributorsearcher = manager.getSearcher(archive.getCatalogId(), "distributor");
	
	String distributorid = req.getRequestParameter("distributorid");
	Data distributor = distributorsearcher.searchById(distributorid);
	if (distributor == null){
		log.error("Error: distributor cannot be found, exiting");
		return;
	}
	
	UploadRequest upload = req.getPageValue("uploadrequest");
	List<Page> files =  req.getPageValue("unzippedfiles");
	PageManager pageManager = req.getPageValue("pageManager");
	//transfer to a map
	Map<String,Page> map = new HashMap<String,Page>();
	Iterator<Page> itr = files.iterator();
	while(itr.hasNext()){
		Page page = itr.next();
		String name = page.get("name");
		String path = page.getPath();
		if (name.toLowerCase().endsWith(".csv")){
			map.put("__csvFile",page);
		} else {
			map.put("${name.trim()}",page);
		}
	}
	if (map.containsKey("__csvFile"))
	{
		readCSVFile(req,distributor.getId(),map);
	}
	cleanup(pageManager,map);
	log.info("Finished uploading products");
}

public void readCSVFile(WebPageRequest inReq, String inDistributor, Map<String,Page> inMap){
	Store store = inReq.getPageValue("store");
	MediaArchive archive = inReq.getPageValue("mediaarchive");
	Searcher productsearcher = store.getProductSearcher();
	List details = productsearcher.getDetailsForView("product/productdistributor_import", inReq.getUser());
	Reader reader = inMap.get("__csvFile").getReader();
	CSVReader csvreader = null;
	//need indexes of SKUs and UPC code
	int rogersskuIndex = 1;
	int manufacturerskuIndex = 2;
	int upcIndex = 4;
	try{
		csvreader = new CSVReader(reader,",","\"");
		List<Data> products = new ArrayList<Data>();
		List<String> ids = new ArrayList<String>();
		List<?> lines = csvreader.readAll();
		Iterator<?> itr = lines.iterator();
		for(int j=1; itr.hasNext(); j++){
			String [] entries = itr.next();
			if (entries.length != details.size()){
				log.error("Format error: row = $j found = ${entries.length} requires = ${details.size()}, skipping");
				continue;
			}
			//todo: include an error list
			Data product = null;
			SearchQuery query = productsearcher.createSearchQuery();
			query.addMatches("rogerssku", entries[rogersskuIndex]);
			query.addMatches("manufacturersku", entries[manufacturerskuIndex]);
			query.addMatches("upc", entries[upcIndex]);
			query.setAndTogether(false);
			HitTracker producthits = productsearcher.search(query);
			for( int k = 0; k < producthits.size(); k++){
				Data data = producthits.get(k);
				if (data.get("distributor") == inDistributor){
					product = productsearcher.searchById(data.getId());
					break;
				}
			}
			if (product == null){
				product = productsearcher.createNewData();
				product.setId(productsearcher.nextId());
				product.setSourcePath("${inDistributor}/${product.getId()}");
				product.setProperty("distributor",inDistributor);
				log.info("created new product, ${product.getId()}")
			} else {
				log.info("loaded existing product, ${product.getId()}")
			}
			String productid = product.getId();
			for (int i=0; i < details.size(); i++) {
				String entry = entries[i].trim();
				PropertyDetail detail = details.get(i);
				if (detail.isList()){
					//categoryid,  phone,  manufacturerid, displaydesignationid,
					if (detail.getListId() == "asset"){
						Page from = inMap.containsKey(entry) ? inMap.get(entry) : inMap.get("${entry}.jpg");
						if (from !=null && from.exists())
						{
							createPrimaryAsset(store,product,from);
						}
					} else {
						Searcher searcher = archive.getSearcher(detail.getListId());
						Data remote = searcher.searchByField("name",entry);
						if (remote!=null){
							product.setProperty(detail.getId(),remote.getId());
						}
					}
				} else if (detail.isBoolean()){
					//approved clearancecentre
					boolean bool = Boolean.parseBoolean(entry);
					product.setProperty(detail.getId(),"$bool");
				} else {
					//name rogerssku manufacturersku , upc msrp, fidomsrp,  rogersprice, clearanceprice, descrip
					if (detail.getId() ==  "msrp" || detail.getId() ==  "fidomsrp" || detail.getId() ==  "rogersprice" || detail.getId() ==  "clearanceprice"){
						double money = toDouble(entry.replace("\$", "").replace(",", "").trim(),-1.0d);
						if (money >= 0.0d){
							product.setProperty(detail.getId(),"$money");
						}//otherwise skip
					} else {
						//if empty then don't overwrite
						if (entry.isEmpty() == false){
							product.setProperty(detail.getId(),entry);
						}
					}
				}
			}
			ids.add(productid);
			products.add(product);
		}
		List cache = new ArrayList();
		for(Data product:products){
			cache.add(product);
			if (cache.size() == 100){
				productsearcher.saveAllData(cache, null);
				cache.clear();
			}
		}
		if (cache.isEmpty() == false){
			productsearcher.saveAllData(cache, null);
			cache.clear();
		}
		//add hits to page
//		SearchQuery query  = productsearcher.createSearchQuery();
//		query.addOrsGroup("id", ids);
//		HitTracker hits = productsearcher.search(query);
//		hits.setHitsPerPage(32);
//		inReq.putPageValue("hits",hits);
		
		//add query to page
		if (ids.isEmpty() == false){
			StringBuilder buf = new StringBuilder();
			buf.append("id:");
			for(String id:ids){
				buf.append("$id").append(":");
			}
			String searchids = buf.toString().substring(0,buf.toString().length()-1);
			inReq.putPageValue("searchquery", searchids);
		}
		
		
	}catch (Exception e){
		log.error(e.getMessage(),e);
		e.printStackTrace();
	}
	finally{
		try{
			csvreader.close();
		}catch(Exception e){}
	}
}

public void cleanup(PageManager inPageManager,Map<String,Page> inMap){
	Iterator<String> itr = inMap.keySet().iterator();
	while(itr.hasNext()){
		Page page = inMap.get(itr.next());
		if (page.exists()){
			inPageManager.removePage(page);
		}
	}
}

public void createPrimaryAsset(Store inStore, Data inProduct, Page fromPage)
{
	try 
	{
		String filename = "original.jpg";
		String sourcepath = "productimages/${inProduct.getSourcePath()}";
		String path = "/WEB-INF/data/${inStore.getCatalogId()}/originals/${sourcepath}/${filename}";
		MediaArchive archive = inStore.getStoreMediaManager().getMediaArchive();
		Page toPage = archive.getPageManager().getPage(path);
		archive.getPageManager().copyPage(fromPage, toPage);
		Asset asset = archive.getAssetBySourcePath(sourcepath);
		if(asset ==  null)
		{
			asset = archive.createAsset(sourcepath);
		}
		asset.setPrimaryFile(filename);
		archive.removeGeneratedImages(asset);
		archive.saveAsset(asset, null);
		inProduct.setProperty("image", asset.getId());
		archive.fireMediaEvent("importing/assetuploaded",null,asset);
	} catch (Exception e) {
		log.error(e.getMessage(),e);
	}
}

public double toDouble(String str, double inDefault)
{
	double out = inDefault;
	if (str)
	{
		try
		{
			out = Double.parseDouble(str);
		}
		catch (Exception e){}
	}
	return out;
}

public int toInt(String str, int inDefault)
{
	int out = inDefault;
	if (str)
	{
		try
		{
			out = Integer.parseInt(str);
		}
		catch (Exception e){}
	}
	return out;
}

init();
