import org.apache.commons.lang.time.FastDateFormat.PaddedNumberField;

import org.apache.commons.lang.time.FastDateFormat.PaddedNumberField;

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

	int padSpacesLeft = 10;
	int padSpaces = 4;
	
	for (String productId in headerRow) {
		
		SearchQuery query = itemsearcher.createSearchQuery()
		query.addMatches("rogers_order", orderid);
		query.addMatches("as400id", productId);
		HitTracker results = itemsearcher.search(query);
		if(results.size() > 0){
			
			log.info("as400id: " + productId);
			log.info("Result Size: " + results.size());
			
			String nextrow = "";
			int productCount = 0;
			
			for ( int index=0; index < results.size(); index++) {
				
				Data product = results.get(index);
				productCount += product.quantity.toInteger();
				
			}
			nextrow = productId + productCount.toString().padLeft(padSpacesLeft) + "-";
			nextrow += " ".padLeft(padSpaces) + "-";
			nextrow += "WIRELES".padLeft(9) + "-";
			nextrow += " ".padLeft(padSpaces) + "-";
			nextrow += " ".padLeft(7);
			nextrow += productCount.toString();
			nextrow += " ".padLeft(7) + "0";
			log.info("nextrow: " + nextrow);
			writer.writeNext(nextrow);
			
		}
		
	}
	writer.close();
	
	String finalout = output.toString();
	context.putPageValue("export", finalout);


}

init();
