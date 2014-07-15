import org.openedit.Data;
import org.openedit.entermedia.CompositeAsset;

import com.openedit.WebPageRequest;


import org.entermedia.upload.UploadRequest
import org.openedit.Data
import org.openedit.data.PropertyDetail
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.util.CSVReader
import org.openedit.money.Money
import org.openedit.store.Store

import com.openedit.WebPageRequest
import com.openedit.page.Page
import com.openedit.page.manage.PageManager

public void init() {
	log.info("Uploading products");
	
	WebPageRequest req = context;
	Store store = req.getPageValue("store");
	MediaArchive archive = req.getPageValue("mediaarchive");
	String catalogID = archive.getCatalogId();
	
	//Create the searcher objects.	 
	SearcherManager manager = archive.getSearcherManager();
	Searcher inventorysearcher = manager.getSearcher(archive.getCatalogId(), "inventoryimport");
	Searcher distributorsearcher = manager.getSearcher(archive.getCatalogId(), "distributor");
	Searcher userprofilesearcher = archive.getSearcher("userprofile");
	
	String distributorid = req.getRequestParameter("distributorid");
	Data distributor = distributorsearcher.searchById(distributorid);
	
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
			map.put("csvFile",page);
		} else {
			map.put("${name.trim()}",page);
		}
	}
	Page csvfile = map.get("csvFile");
	Searcher productsearcher = archive.getSearcher("product");
	List details = productsearcher.getDetailsForView("product/productdistributor_import", req.getUser());
	readCSVFile(archive,csvfile,details,map);
	//cleanup
	log.info("Finished uploading products");
}

public void readCSVFile(MediaArchive inArchive, Page inPage, List inDetails, Map<String,Page> inMap){
	Searcher productsearcher = inArchive.getSearcher("product");
	List<Data> products = new ArrayList<Data>();
	Reader reader = inPage.getReader();
	CSVReader csvreader = null;
	try{
		csvreader = new CSVReader(reader,"\t","\"");
		List<?> lines = csvreader.readAll();
		Iterator<?> itr = lines.iterator();
		while( itr.hasNext()){
			String [] entries = itr.next();
			if (entries.length != inDetails.size()){
				System.out.println("### $entries");
				continue;
			}
//			Data product = productsearcher.createNewData();
			for (int i=0; i < inDetails.size(); i++) {
				String entry = entries[i].trim();
				PropertyDetail detail = inDetails.get(i);
				if (detail.isList()){
					//categoryid,  phone,  manufacturerid, displaydesignationid,
					if (detail.getListId() == "asset"){
						if (inMap.containsKey(entry)==false && inMap.containsKey("${entry}.jpg") == false){
							System.out.println("### ERROR: cannot find $entry ${inMap.keySet()}");
							continue;
						}
						//move page to originals folder, ingest asset, etc
						
					} else {
						Searcher searcher = inArchive.getSearcher(detail.getListId());
						Data remote = searcher.searchByField("name",entry);
						if (remote==null){
							System.out.println("### ERROR: cannot find $entry (${detail.getListId()})");
							continue;
						}
//						product.setProperty(detail.getId(),remote.getId());
					}
				} else if (detail.isBoolean()){
					boolean bool = Boolean.parseBoolean(entry);
					//approved clearancecentre
//					product.setProperty(detail.getId(),bool+"");
				} else {
					//name rogerssku manufacturersku , upc msrp, fidomsrp,  rogersprice, clearanceprice, descrip
//					product.setProperty(detail.getId(),entry);
				}
			}
//			products.add(product);
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
	}catch (Exception e){
		e.printStackTrace();
	}
	finally{
		try{
			csvreader.close();
		}catch(Exception e){}
	}
}

public void convertPageToAsset(){

	//load AssetEditModule
//	public void createAssetsFromFile(WebPageRequest inReq)
//	{
//		String sourcepath = inReq.findValue("sourcepath");
//		String catalogid = inReq.findValue("catalogid");
//		String unzip = inReq.findValue("unzip");
//		
//		Data asset = getAssetImporter().createAssetFromExistingFile(getMediaArchive(catalogid), inReq.getUser(), Boolean.valueOf(unzip), sourcepath);
//		if(asset == null)
//		{
//			return;
//		}
//		if( asset instanceof CompositeAsset)
//		{
//			asset.setId("multiedit:new");
//			inReq.putSessionValue(asset.getId(), asset);
//		}
//		inReq.setRequestParameter("id", asset.getId());
//		inReq.putPageValue("asset", asset);
//	}
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
