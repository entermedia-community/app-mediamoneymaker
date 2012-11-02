import java.text.SimpleDateFormat

import org.openedit.Data
import org.openedit.data.*
import org.openedit.entermedia.util.CSVWriter

import com.openedit.BaseWebPageRequest
import com.openedit.OpenEditException
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery

public void init(){

	//Create Searcher Object
	BaseWebPageRequest inReq = context;
	SearcherManager searcherManager = inReq.getPageValue("searcherManager");
	Searcher ordersearcher = searcherManager.getSearcher(catalogid, "rogers_order");
	Searcher itemsearcher = searcherManager.getSearcher(catalogid, "rogers_order_item");
	Searcher storesearcher = searcherManager.getSearcher(catalogid, "store");

	def orderid = inReq.getRequestParameter("orderid");
	Data order = ordersearcher.searchById(orderid);
	//Get the Order items from the order using the orderid
	HitTracker orderitems = itemsearcher.fieldSearch("rogers_order", orderid);

	int count = 0;

	//Create the CSV Writer Objects
	StringWriter output  = new StringWriter();
	CSVWriter writer  = new CSVWriter(output, (char)',');

	//List headers = collectStores(orderitems);
	List headerRow = new ArrayList();
	headerRow.add("ORDER_DATE");
	headerRow.add("CUSTOMER_PO_NUMBER");
	headerRow.add("SITE_ID");
	headerRow.add("AS400_SKU");
	headerRow.add("ITEM_DESCRIPTION");
	headerRow.add("VENDOR");
	headerRow.add("SHIP_DATE");
	headerRow.add("ORDER_STATUS");
	headerRow.add("CARRIER");
	headerRow.add("WAYBILL");
	headerRow.add("ORDERED_QTY");
	headerRow.add("SHIPPED_QTY");
	headerRow.add("DELIVERY_DATE");
	headerRow.add("DELIVERY_STATUS");
	headerRow.add("DELIVERY_PERSON");

	//Create the row
	String[] nextrow = new String[headerRow.size()];
	for ( int headerCtr=0; headerCtr < headerRow.size(); headerCtr++ ){
		nextrow[headerCtr] = headerRow.get(headerCtr);
	}
	writer.writeNext(nextrow);
	log.info(nextrow.toString());
	for ( int headerCtr=0; headerCtr < headerRow.size(); headerCtr++ ){
		nextrow[headerCtr] = "";
		headerRow[headerCtr] = "";
	}
	headerRow = null;

	List detailRow = new ArrayList();
	for (Iterator itemIterator = orderitems.iterator(); itemIterator.hasNext();)
	{
		Data orderItemID = itemIterator.next();
		if (!detailRow.contains(orderItemID.as400id)) {
			log.info("OrderItem: New item found: " + orderItemID.as400id);
			detailRow.add(orderItemID.as400id);
			count++;

		}
	}

	for (String productId in detailRow) {

		SearchQuery query = itemsearcher.createSearchQuery()
		query.addMatches("rogers_order", orderid);
		query.addMatches("as400id", productId);
		HitTracker results = itemsearcher.search(query);
		if(results.size() > 0){

			log.info("as400id: " + productId);
			log.info("Result Size: " + results.size());

			int productCount = 0;

			results.each{
		
				Data itemData = it;
				Data product = getProduct(searcherManager, it.product);
				
				nextrow[0] = getDate(order); 
				nextrow[1] = order.as400po; //CUSTOMER_PO_NUMBER
				nextrow[2] = itemData.store; //SITE_ID
				nextrow[3] = productId; //AS400_SKU
				nextrow[4] = product.name;
				nextrow[5] = itemData.distributor; //VENDOR
				nextrow[6] = getDate(order); //SHIP_DATE
				nextrow[7] = getOrderStatus(itemData); //ORDER_STATUS
				nextrow[8] = getCarrier(itemData); //CARRIER
				nextrow[9] = getWaybill(itemData); //WAYBILL
				nextrow[10] = itemData.quantity; //ORDERED_QTY
				nextrow[11] = itemData.shippedquantity; //ORDERED_QTY
				nextrow[12] = getDate(itemData); //DELIVERY_DATE
				nextrow[13] = getDeliveryStatus(itemData); //DELIVERY_STATUS
				nextrow[14] = getDeliveryPerson(itemData); //DELIVERY_PERSON
	
				log.info("nextrow: " + nextrow);
				writer.writeNext(nextrow);
				
			}
		}
	}
	writer.close();

	String finalout = output.toString();
	context.putPageValue("export", finalout);


}

private Data getProduct(SearcherManager searcherManager, String id) {
	Searcher productSearcher = searcherManager.getSearcher(catalogid, "product");
	Data product = productSearcher.searchById(id);
	if (product == null) {
		log.info("Product is null: " + id);
	} else {
		log.info("Product found: " + id);
	}
	return product;
}

private String getDate( Data order ) {

	Date now;
	SimpleDateFormat tableFormat = new SimpleDateFormat("yyyy/MM/dd");
	if (order.date == null) {
		now = new Date();
	} else {
		log.info("INFO: Order date found")
		now = new Date(order.date);
	}
	String outDate = tableFormat.format(now);
	now = null;
	return outDate;

}

private String getOrderStatus( Data order ) {
	//2 possible values - Shipped Cancelled
	return "Shipped";
}

private String getCarrier( Data order ) {
	//2 possible values - Shipped Cancelled
	return "UPS";
}

private String getWaybill( Data order ) {
	//2 possible values - Shipped Cancelled
	return "1Z1RV8840426603961/FT6B1028349";
}

private String getDeliveryDate( Data order ) {
	//2 possible values - Shipped Cancelled
	Date now = new Date();
	SimpleDateFormat tableFormat = new SimpleDateFormat("yyyyMMdd");
	String outDate = tableFormat.format(now);
	now = null;
	return outDate;
}

private String getDeliveryStatus( Data order ) {
	//2 possible values - Shipped Cancelled
	return "Delivered";
}

private String getDeliveryPerson( Data order ) {
	//2 possible values - Shipped Cancelled
	return "John Smith";
}

init();