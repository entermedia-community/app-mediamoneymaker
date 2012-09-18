import org.openedit.Data
import org.openedit.data.*
import org.openedit.entermedia.util.CSVWriter

import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
	
public void init(){
searcherManager = context.getPageValue("searcherManager");
searcher = searcherManager.getSearcher(catalogid, "corporateorder");
itemsearcher = searcherManager.getSearcher(catalogid, "corporateorderitems");
storesearcher = searcherManager.getSearcher(catalogid, "store");
orderid = context.getRequestParameter("orderid")
order = searcher.searchById(orderid);
HitTracker orderitems = itemsearcher.fieldSearch("corporateorderid", orderid);


StringWriter output  = new StringWriter();
CSVWriter writer  = new CSVWriter(output);

int count = 0;
headers = new String[orderitems.size()+1];
for (Iterator iterator = orderitems.iterator(); iterator.hasNext();)
{
	data = iterator.next();
	headers[count+1] = data.id;
	count++;
}

writer.writeNext(headers);

for (Iterator iterator = storesearcher.getAllHits().iterator(); iterator.hasNext();)
{
	Data hit =  iterator.next();
	tracker = searcher.searchById(hit.get("id"));
	nextrow = new String[orderitems.size()+1];//make an extra spot for c
	int fieldcount = 0;
	nextrow[0] = hit.store;
	fieldcount++;
	for (Iterator iterator2 = orderitems.iterator(); iterator2.hasNext();){
		product = iterator2.next();
		SearchQuery query = itemsearcher.createSearchQuery()
		query.addMatches("store", hit.get("id"));
		query.addMatches("corporateorderid", orderid);
		query.addMatches("product", product.product);
		hits = itemsearcher.search(query);
		if(hits.size() > 0){
			first = hits.get(0);
			nextrow[fieldcount] = first.quantity;
		}
		else{
			nextrow[fieldcount] = "0";	
		}
		fieldcount ++;
		}
	writer.writeNext(nextrow);
}

	
	
	
	

writer.close();

String finalout = output.toString();
context.putPageValue("export", finalout);


}

init();