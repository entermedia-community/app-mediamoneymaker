import org.openedit.Data
import org.openedit.data.*
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.util.CSVWriter
import org.openedit.store.InventoryItem
import org.openedit.store.Product

import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
	


public void init(){
	WebPageRequest req = context;
	String sessionid = req.getRequestParameter("hitssessionid");
	HitTracker hits = req.getSessionValue(sessionid);
	if (hits == null) {
		return;
	}
	ArrayList selectedHits = hits.getSelectedHits();
	log.info("Found # of Products:" + selectedHits.size());
	
	MediaArchive archive = req.getPageValue("mediaarchive");
	String searchtype = req.findValue("searchtype");
	Searcher searcher = archive.getSearcher(searchtype);
	boolean friendly = Boolean.parseBoolean(req.getRequestParameter("friendly"));
	String[] detaillist = req.getRequestParameters("detail");
	Collection details = null;
	
	System.out.println("#### $detaillist")
	
	if(detaillist != null){
		log.info("Detail List was used - customizing export");
		details = new ArrayList();
		for(int i = 0;i<detaillist.length;i++){
			String detailid = detaillist[i];
			PropertyDetail detail = searcher.getDetail(detailid);
			if(detail != null){
				details.add(detail);
			}
		}
	}
	else
	{
		details = searcher.getDetailsForView("csvexport", req.getUser());
	}
	if(details == null)
	{
		details = searcher.getPropertyDetails();
	}
	
	System.out.println("#### $detaillist")
	
	
	
	
//	StringWriter output  = new StringWriter();
//	CSVWriter writer  = new CSVWriter(output);
//	int count = 0;
//	headers = new String[details.size()+5];
//	for (Iterator iterator = details.iterator(); iterator.hasNext();)
//	{
//		PropertyDetail detail = (PropertyDetail) iterator.next();
//		headers[count] = detail.getText();		
//		count++;
//	}
//	headers[count] = "QTY_IN_STOCK";
//	writer.writeNext(headers);
//	log.info("about to start: " + hits);
//	Iterator itr = selectedHits.iterator();
//	while (itr.hasNext())
//	{
//		hit =  itr.next();
//		Product tracker = searcher.searchById(hit.get("id"));
//	
//	
//		nextrow = new String[details.size()+5];//make an extra spot for c
//		int fieldcount = 0;
//		for (Iterator detailiter = details.iterator(); detailiter.hasNext();)
//		{
//			PropertyDetail detail = (PropertyDetail) detailiter.next();
//			String value = tracker.get(detail.getId());
//			//do special logic here
//			if(detail.isList() && friendly){
//				detail.get
//				Data remote  = searcherManager.getData( detail.getListCatalogId(),detail.getListId(), value);
//			
//					if(remote != null){
//					value= remote.getName();
//				}
//				
//			}
//		
//	
//			nextrow[fieldcount] = value;
//		
//			fieldcount++;
//		}
//		InventoryItem inven = tracker.getInventoryItem(0);
//		if (inven != null) {
//			if (inven.quantityInStock != null) {
//				nextrow[fieldcount] = inven.quantityInStock.toString();
//			} else {
//				nextrow[fieldcount] = "null";
//			} 
//		} else {
//			nextrow[fieldcount] = "null";
//		}
//		
//		writer.writeNext(nextrow);
//	}
//	writer.close();
	
//	String finalout = output.toString();
	
	StringBuilder buf = new StringBuilder();
	for (Iterator itr = details.iterator(); itr.hasNext();)
	{
		PropertyDetail detail = (PropertyDetail) itr.next();
		buf.append("\"${detail.getText()}\"");
		if (itr.hasNext()) buf.append(",");
		else buf.append("\n");
	}
	
	System.out.println("$buf")
	
	req.putPageValue("export", buf.toString());
}

init();


