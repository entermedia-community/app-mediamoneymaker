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
headerlist = searcherManager.getList(catalogid, "cesium_headers_detail");
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
	nextrow = new String[headerlist.size()+20];
	hit =  iterator.next();
	log.info(hit.id);
	Order order = ordersearcher.searchById(hit.id);
	log.info("" + order.getItems());
	Date orderdate = order.getDate();
	Cart cart = order.getCart();
	for (Iterator iterator2 = order.getItems().iterator(); iterator2.hasNext();){
		CartItem item = iterator2.next();
		Data corporatestore = searcherManager.getData(catalogid, "profile_address_list", item.store);
		//Data product = store.
		//item.getProduct().getId();
		nextrow[0] = order.getId();
		nextrow[2] = item.getProduct().manufacturersku;
		nextrow[4] = item.getQuantity();
		nextrow[5] = item.getYourPrice().toShortString();
		
		if(corporatestore != null){
		
		nextrow[6] = corporatestore.address1;
		nextrow[7] = corporatestore.address2;
		nextrow[8] = corporatestore.city;
		nextrow[9] = corporatestore.province;
		nextrow[10] = corporatestore.country;
		nextrow[11] = corporatestore.postalcode;
		nextrow[12] = corporatestore.phone;
		
		}
		writer.writeNext(nextrow);
		
	}
	
		
	
	
	
}
writer.close();

String finalout = output.toString();
context.putPageValue("export", finalout);


