import java.text.SimpleDateFormat

import org.openedit.Data
import org.openedit.data.*
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.util.CSVWriter
import org.openedit.store.Cart
import org.openedit.store.CartItem
import org.openedit.store.Store
import org.openedit.store.orders.Order

import com.openedit.hittracker.HitTracker
	

HitTracker hits = (HitTracker) context.getPageValue("hits");
if(hits == null){
 String sessionid = context.getRequestParameter("hitssessionid");
 hits = context.getSessionValue(sessionid);
}
log.info("hits: " +hits);
MediaArchive archive = context.getPageValue("mediaarchive");
SearcherManager searcherManager = archive.getSearcherManager();
String catalogid = context.findValue("catalogid");
Searcher ordersearcher = searcherManager.getSearcher(catalogid, "storeOrder");

Store store = context.getPageValue("store");
StringWriter output  = new StringWriter();
CSVWriter writer  = new CSVWriter(output);
int count = 0;
headerlist = searcherManager.getList(catalogid, "cesium_headers");
headers = new String[headerlist.size()];
for (Iterator iterator = headerlist.iterator(); iterator.hasNext();)
{
	Data nextheader = (Data) iterator.next();
	headers[count] = nextheader.header;		
	count++;
}

writer.writeNext(headers);
SimpleDateFormat cesiumformat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");
for (Iterator iterator = hits.getSelectedHits().iterator(); iterator.hasNext();)
{
	count=0;
	nextrow = new String[headerlist.size()];
	hit =  iterator.next();
	log.info(hit.id);
	Order order = ordersearcher.searchById(hit.id);
	
	Date orderdate = order.getDate();
	Cart cart = order.getCart();
		nextrow[0] = order.getId();
		if(orderdate != null){
			nextrow[1] = cesiumformat.format(orderdate);
		}
//		nextrow[3] = corporatestore.name;
//		nextrow[4] = corporatestore.address1;
//		nextrow[5] = corporatestore.address2;
//		nextrow[6] = corporatestore.city;
//		nextrow[7] = corporatestore.province;
//		nextrow[8] = corporatestore.country;
//		nextrow[9] = corporatestore.postalcode;
//		nextrow[10] = corporatestore.phone;
		nextrow[11] = "Wireless Area" 
		nextrow[12] = "1 Hurontario Street";
		nextrow[14] = "Mississauga";
		nextrow[15] = "ON";
		nextrow[16] = "CANADA";
		nextrow[17] = "L5G0A3";
		nextrow[18] = "905-271-2732";
		nextrow[19] = "orders@wirelessarea.ca";
		nextrow[23] = "Collect";
		
			
		
	
	writer.writeNext(nextrow);
}
writer.close();

String finalout = output.toString();
context.putPageValue("export", finalout);


