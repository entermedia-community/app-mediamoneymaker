
import java.text.SimpleDateFormat

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.util.CSVReader
import org.openedit.event.WebEvent
import org.openedit.store.CartItem
import org.openedit.store.Product
import org.openedit.store.Store
import org.openedit.store.orders.Order
import org.openedit.store.orders.Shipment
import org.openedit.store.orders.ShipmentEntry
import org.openedit.util.DateStorageUtil

import com.openedit.WebPageRequest
import com.openedit.page.Page
import com.openedit.util.FileUtils


public void init(){
	WebPageRequest req = context;
	MediaArchive archive = req.getPageValue("mediaarchive");
	Store store = req.getPageValue("store");
	Searcher productsearcher = store.getProductSearcher();
	Searcher ordersearcher = store.getOrderSearcher();
	Searcher distributorsearcher = archive.getSearcher("distributor");
	List<List<String>> errors = new ArrayList<ArrayList<String>>();
	Map<String,List<String>> updates = new HashMap<String,ArrayList<String>>();//map of orderid:waybills
	Reader reader = null;
	try{
		Page csvfile = archive.getPageManager().getPage("/${archive.getCatalogId()}/temp/upload/shipments.csv");
		if (!csvfile.exists()){
			errors.add(["","","Uploaded CSV file cannot be found."]);
			return;
		}
		reader = csvfile.getReader();
		CSVReader csvreader = new CSVReader(reader, ',', '\"');
		//check headers
		String[] headers = csvreader.readNext();
		String [] matched = ["Invoice Number","Sku","Quantity","Waybill","Carrier","MM/DD/YYYY"] as String[];
		String toString = "${matched}".replace("[","").replace("]","");
		if (headers.length != 6){
			errors.add(["","","Headers do not match, expecting \"$toString\""]);
			return;
		}
		for (int i=0; i < matched.length; i++){
			if (!matched[i].equalsIgnoreCase(headers[i])){
				errors.add(["","","Headers do not match, expecting \"$toString\", check header labelled \"${headers[i]}\""]);
				return;
			}
		}
		String[] line;
		for ( int i = 1; (line = csvreader.readNext()) != null; i++){
			String orderid = formatOrder(line[0].trim());
			String sku = line[1].trim();
			int quantity = toInt(line[2].trim(),-1);
			String waybill = line[3].trim();
			String carrier = line[4].trim();
			String date = parseDate(line[5].trim());
			toString = "${line}".replace("[","").replace("]","");
			if (!orderid || !sku || !waybill || !carrier || !date || quantity <= 0 ){
				errors.add(["$i","$toString","Format Error"]);
				continue;
			}
			Order order = (Order) store.getOrderSearcher().searchById(orderid);
			if (!order){
				errors.add(["$i","$toString","Unable to find order $orderid"]);
				continue;
			}
			CartItem cartItem = order.getCartItemByProductProperty("manufacturersku", sku);
			if (!cartItem){
				//for some reason, product ids are shown instead of manufacturerskus, so check id field too
				cartItem = order.getCartItemByProductProperty("id", sku);
			}
			if (!cartItem){
				errors.add(["$i","$toString","Unable to find product $sku"]);
				continue;
			}
			int available = order.getQuantityAvailableForShipment(cartItem);
			if (quantity > available){
				errors.add(["$i","$toString","Quantity exceeds $available, the quantity available to ship, for $sku"]);
				continue;
			}
			Product product = cartItem.getProduct();
			if (!product){
				errors.add(["$i","$toString","Unable to find product $sku"]);
				continue;
			}
			if (!product.get("distributor")){
				errors.add(["$i","$toString","Unable to find distributor for product $sku"]);
				continue;
			}
			Data distributor = distributorsearcher.searchById(product.get("distributor"));
			if (!distributor){
				errors.add(["$i","$toString","Unable to find distributor for product $sku"]);
				continue;
			}
			boolean updateOrder = true;
			Shipment shipment = order.getShipmentByWaybill(waybill);
			if(shipment == null){
				shipment = new Shipment();
				shipment.setProperty("courier", carrier);
				shipment.setProperty("waybill", waybill);
				shipment.setProperty("distributor", distributor.getId());
				shipment.setProperty("shipdate", date);
				order.addShipment(shipment);
				updateOrder = true;
			}
			if (!shipment.containsEntryForSku(cartItem.getSku())) {
				ShipmentEntry entry = new ShipmentEntry();
				entry.setQuantity(quantity);
				entry.setSku(cartItem.getSku());
				shipment.addEntry(entry);
				updateOrder = true;
			}
			if (updateOrder){
				if (!updates.containsKey(orderid)){
					updates.put(orderid, new ArrayList<String>());
				}
				List<String> waybills = updates.get(orderid);
				if (!waybills.contains(waybill)){
					waybills.add(waybill);
				}
				if(order.isFullyShipped()){
					order.setProperty("shippingstatus", "shipped");
				}else{
					order.setProperty("shippingstatus", "partialshipped");
				}
				ordersearcher.saveData(order, null);
				appendShippingNoticeToOrderHistory(req,order,waybill);
				if (order.isFullyShipped()){
					appendFullyShippedNoticeToOrderHistory(req,order);
				}
			}
		}
	}
	finally{
		if (reader!=null){
			FileUtils.safeClose(reader);
		}
		req.putPageValue("errors", errors);
		req.putPageValue("orders", updates);
	}
}

public String parseDate(String str){
	SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
	try{
		Date date = format.parse(str);
		return DateStorageUtil.getStorageUtil().formatForStorage(date);
	} catch (Exception e){}//not handled
	return null;
}

public String formatOrder(String str){
	if (str){
		return str.toUpperCase().replace("ROGERS", "Rogers");
	}
	return null;
}

public int toInt(String str, int defaultInt){
	try{
		return Integer.parseInt(str);
	}catch (Exception e){}
	return defaultInt;
}

public appendShippingNoticeToOrderHistory(WebPageRequest req, Order order, String waybill)
{
	MediaArchive archive = req.getPageValue("mediaarchive");
	WebEvent event = new WebEvent();
	event.setSearchType("detailedorderhistory");
	event.setCatalogId(archive.getCatalogId());
	event.setProperty("applicationid", req.findValue("applicationid"));
	event.setOperation("orderhistory/appendorderhistory");
	event.setProperty("orderid", order.getId());
	event.setProperty("type","automatic");
	event.setProperty("state","shippingnoticereceived");
	event.setProperty("shipmentid", waybill);
	archive.getMediaEventHandler().eventFired(event);
}

public appendFullyShippedNoticeToOrderHistory(WebPageRequest req, Order order)
{
	MediaArchive archive = req.getPageValue("mediaarchive");
	WebEvent event = new WebEvent();
	event.setSearchType("detailedorderhistory");
	event.setCatalogId(archive.getCatalogId());
	event.setProperty("applicationid", req.findValue("applicationid"));
	event.setOperation("orderhistory/appendorderhistory");
	event.setProperty("orderid", order.getId());
	event.setProperty("type","automatic");
	event.setProperty("state","fullyshipped");
	archive.getMediaEventHandler().eventFired(event);
}

init();