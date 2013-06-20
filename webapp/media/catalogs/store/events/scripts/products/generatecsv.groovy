package products
import org.openedit.Data
import org.openedit.data.*
import org.openedit.entermedia.util.CSVWriter
import org.openedit.store.InventoryItem;

import com.openedit.hittracker.HitTracker
	

HitTracker hits = (HitTracker) context.getPageValue("hits");	
if(hits == null){
 String sessionid = context.getRequestParameter("hitssessionid");
 hits = context.getSessionValue(sessionid);
}
log.info("hits: " +hits);
searcherManager = context.getPageValue("searcherManager");
searchtype = context.findValue("searchtype");
catalogid = context.findValue("catalogid");
String view = context.findValue("view");
searcher = searcherManager.getSearcher(catalogid, searchtype);
boolean friendly = Boolean.parseBoolean(context.getRequestParameter("friendly"));
String[] detaillist = context.getRequestParameters("detail");
Collection details = null;

if(detaillist != null){
	log.info("Detail List was used - customizing export");
	details = new ArrayList();
	for(int i = 0;i<detaillist.length;i++){
		String detailid = detaillist[i];
		detail = searcher.getDetail(detailid);
		if(detail != null){
			details.add(detail);
		}
	}
} 
else{

details = searcher.getDetailsForView(view, context.getUser());
}


StringWriter output  = new StringWriter();
CSVWriter writer  = new CSVWriter(output);
int count = 0;
headers = new String[details.size()+1];
for (Iterator iterator = details.iterator(); iterator.hasNext();)
{
	PropertyDetail detail = (PropertyDetail) iterator.next();
	headers[count] = detail.getText();		
	count++;
}
headers[details.size()] = "Qty In Stock";
writer.writeNext(headers);

log.info("about to start: " + hits);
Iterator i = null;
if(hits.getSelectedHits().size() == 0){
	i = hits.iterator();
} else{

i = hits.getSelectedHits().iterator();

}
for (Iterator iterator = i; iterator.hasNext();)
{
	hit =  iterator.next();
	tracker = searcher.searchById(hit.get("id"));


	nextrow = new String[details.size()+1];//make an extra spot for c
	int fieldcount = 0;
	for (Iterator detailiter = details.iterator(); detailiter.hasNext();)
	{
		PropertyDetail detail = (PropertyDetail) detailiter.next();
		String value = tracker.get(detail.getId());
		//do special logic here
		if(detail.isList() && friendly){
			detail.get
			Data remote  = searcherManager.getData( detail.getListCatalogId(),detail.getListId(), value);
		
				if(remote != null){
				value= remote.getName();
			}
			
		}
	

		nextrow[fieldcount] = value;
	
		fieldcount++;
	}
	InventoryItem item = tracker.getInventoryItem(0);
	Integer qtyInStock = item.getQuantityInStock();

	nextrow[details.size()] = qtyInStock.toString();
	
	writer.writeNext(nextrow);
}
writer.close();

String finalout = output.toString();
context.putPageValue("export", finalout);

