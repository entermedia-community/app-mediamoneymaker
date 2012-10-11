import org.openedit.Data
import org.openedit.data.*
import org.openedit.entermedia.util.CSVWriter

import com.openedit.BaseWebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery

public void init(){
	
	//Create Searcher Object
	BaseWebPageRequest inReq = context;
	SearcherManager searcherManager = inReq.getPageValue("searcherManager");
	Searcher searcher = searcherManager.getSearcher(catalogid, "rogers_order");
	Searcher itemsearcher = searcherManager.getSearcher(catalogid, "rogers_order_item");
	Searcher storesearcher = searcherManager.getSearcher(catalogid, "store");
	
	def orderid = inReq.getRequestParameter("orderid");
  	Data order = searcher.searchById(orderid);
	//Get the Order items from the order using the orderid
	HitTracker orderitems = itemsearcher.fieldSearch("rogers_order", orderid);

	int count = 0;
	
	//Create the CSV Writer Objects
	StringWriter output  = new StringWriter();
	CSVWriter writer  = new CSVWriter(output, (char)'\t');

	//List headers = collectStores(orderitems);
	List headerRow = new ArrayList();
	
	for (Iterator itemIterator = orderitems.iterator(); itemIterator.hasNext();)
	{
		Data orderItemID = itemIterator.next();
		if (!headerRow.contains(orderItemID.as400id)) {
			log.info("OrderItem: New item found: " + orderItemID.as400id); 
			headerRow.add(orderItemID.as400id); 
			count++;
			
		}
	}

	String[] nextrow = new String[headerRow.size()+1]; //make an extra spot for the store info
	for ( int i=0; i < headerRow.size(); i++ ){
		nextrow[i+1] = headerRow.get(i);
	}
	writer.writeNext(nextrow);
	
	for (Iterator iterator = storesearcher.getAllHits().iterator(); iterator.hasNext();)
	{
		Data storeInfo =  iterator.next();
		nextrow = new String[headerRow.size()+1]; //make an extra spot for the store info

		int fieldcount = 0;
		nextrow[0] = storeInfo.store;
		fieldcount++;
		
		for (String productId in headerRow) {
			
			SearchQuery query = itemsearcher.createSearchQuery()
			query.addMatches("store", storeInfo.store);
			query.addMatches("rogers_order", orderid);
			query.addMatches("as400id", productId);
			HitTracker results = itemsearcher.search(query);
			if(results.size() > 0){
				Data first = results.get(0);
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