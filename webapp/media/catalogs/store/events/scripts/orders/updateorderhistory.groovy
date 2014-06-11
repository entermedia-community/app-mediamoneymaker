package orders;

import java.text.SimpleDateFormat
import java.util.Date;
import java.util.Iterator;

import org.dom4j.Element
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.util.DateStorageUtil
import org.openedit.money.Money
import org.openedit.store.CartItem
import org.openedit.store.CreditPaymentMethod
import org.openedit.store.PaymentMethod
import org.openedit.store.Price;
import org.openedit.store.PriceSupport
import org.openedit.store.PriceTier;
import org.openedit.store.Store
import org.openedit.store.Product
import org.openedit.store.InventoryItem
import org.openedit.store.orders.Order
import org.openedit.store.orders.OrderState
import org.openedit.store.util.MediaUtilities
import org.openedit.store.orders.OrderArchive
import org.openedit.store.orders.OrderId
import org.openedit.store.orders.Shipment
import org.openedit.store.orders.ShipmentEntry
import org.openedit.store.orders.Refund
import org.openedit.store.orders.RefundItem
import org.openedit.store.orders.RefundState

import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.ListHitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.Page
import com.openedit.page.manage.PageManager
import com.openedit.util.XmlUtil

import groovy.util.slurpersupport.GPathResult


public void init(){
	log.info("---- START Update Order History ----");
	
	WebPageRequest inReq = context;
	MediaArchive archive = inReq.getPageValue("mediaarchive");
	Searcher invoicesearcher = archive.getSearcher("invoice");
	
	Store store = inReq.getPageValue("store");
	List<OrderId> ids = store.getOrderArchive().listAllOrderIds(store);
	for(OrderId id:ids){
		Order order = store.getOrderSearcher().searchById(id.getOrderId());
		if (order == null || order.getItems() == null){
			continue;
		}
		addOpened(archive,order);
		addOrderPlaced(archive,order);
		
		String edi = order.get("edistatus");
		if ("sendtoedi".equals(edi)) {
			addGeneratedEDI(archive,order);
			addSendToEDI(archive,order);
		} else if ("generated".equals(edi)){
			addGeneratedEDI(archive,order);
		}
		if (!order.getRefunds().isEmpty()){
			addItemsRefunded(archive,order);
		}
		SearchQuery query = invoicesearcher.createSearchQuery();
		query.addMatches("orderid", order.getId());
		query.addSortBy("date");
		HitTracker hits = invoicesearcher.search(query);
		hits.each{
			Data invoice = (Data) it;
			addInvoiceReceived(archive,order,invoice);
		}
		List shipments = order.getShipments();
		if (!shipments.isEmpty()){
			addShippingNoticeReceived(archive,order);
		}
	}
	
	log.info("---- END Update Order History ----");
}

public void addOpened(MediaArchive archive, Order order)
{
	String stateid = "opened";
	Date date = order.getDate();
	addOrderState(archive,date,order.getId(),stateid,"automatic","order created","","","","");
}

public void addOrderPlaced(MediaArchive archive, Order order)
{
	String stateid = "orderplaced";
	Date date = order.getDate();
	OrderState orderstate = order.getOrderStatus();
	String state = orderstate.getId();
	if ("authorized".equals(state) || "accepted".equals(state)){
		addOrderState(archive,date,order.getId(),stateid,"automatic",orderstate.toString(),"","","","");
	} else if ("rejected".equals(state)) {
		PaymentMethod method = order.getPaymentMethod();
		if (method !=null && method instanceof CreditPaymentMethod){
			CreditPaymentMethod cc = (CreditPaymentMethod) method;
			addOrderState(archive,date,order.getId(),stateid,"automatic","Credit Card Authorized, but Rejected","","","","");
		}
	}
}

public void addGeneratedEDI(MediaArchive archive, Order order)
{
	String stateid = "generatededi";
	Date date = order.getDate();
	addOrderState(archive,date,order.getId(),stateid,"automatic","","","","","");
}

public void addSendToEDI(MediaArchive archive, Order order)
{
	String stateid = "senttoedi";
	Date date = order.getDate();
	addOrderState(archive,date,order.getId(),stateid,"automatic","","","","","");
}

public void addShippingNoticeReceived(MediaArchive archive, Order order)
{
	String stateid = "shippingnoticereceived";
	Date fullyshippeddate = order.getDate();
	Iterator itr = order.getShipments().iterator();
	while(itr.hasNext()){
		Shipment shipment = (Shipment) itr.next();
		Date date = DateStorageUtil.getStorageUtil().parseFromStorage("${shipment.shipdate}");
		if (date.before(fullyshippeddate)) {
			fullyshippeddate = date;
		}
		String waybill = "${shipment.waybill}";
		int numbershipped = shipment.getShipmentEntries()==null ? 0 : shipment.getShipmentEntries().size();
		addOrderState(archive,date,order.getId(),stateid,"automatic","waybill=$waybill, number shipped=$numbershipped","","","","$waybill");
	}
	if (order.isFullyShipped()){
		addFullyShipped(archive,order,fullyshippeddate);
	}
}

public void addFullyShipped(MediaArchive archive, Order order, Date date){
	String stateid = "fullyshipped";
	addOrderState(archive,date,order.getId(),stateid,"automatic","","","","","");
}

public void addInvoiceReceived(MediaArchive archive, Order order, Data invoice)
{
	String stateid = "invoicereceived";
	Date date = DateStorageUtil.getStorageUtil().parseFromStorage("${invoice.date}");
	Searcher searcher = archive.getSearcher("invoicestatus");
	Data status = searcher.searchById("${invoice.invoicestatus}");
	String notes = invoice.get("notes") == null ? "" : "${invoice.notes}";
	Money money = new Money("${invoice.invoicetotal}");
	String note = "id=${invoice.id}, status=${status.name}, total=${money}, notes=${notes}";
	addOrderState(archive,date,order.getId(),stateid,"automatic",note,"",invoice.getId(),"","");
}

public void addItemsRefunded(MediaArchive archive, Order order)
{
	String stateid = "itemsrefunded";
	Iterator<Refund> itr = order.getRefunds().iterator();
	while(itr.hasNext()){
		Refund refund =  itr.next();
		if (!refund.isSuccess()) {
			continue;
		}
		Date date = refund.getDate();
		Money total = refund.getTotalAmount();
		if (date!=null && total !=null){
			addOrderState(archive,date,order.getId(),stateid,"automatic","Total $total","","",refund.getTransactionId(),"");
		}
	}
}

public void addOrderState(MediaArchive archive, Date date, String orderid, String state, String type, String note, String user, String invoice, String refund, String shipment)
{
	Searcher historysearcher = archive.getSearcher("detailedorderhistory");
	Data data = historysearcher.createNewData();
	data.setProperty("orderid",orderid);
	data.setProperty("state",state);
	data.setProperty("entrytype",type);
	data.setProperty("note",note);
	
	data.setProperty("userid",user);
	data.setProperty("invoiceid",invoice);
	data.setProperty("refundid",refund);
	data.setProperty("shipmentid",shipment);
	
	
	data.setProperty("date",DateStorageUtil.getStorageUtil().formatForStorage(date));
	historysearcher.saveData(data, null);
}

init();