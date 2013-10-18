package edi

import java.text.SimpleDateFormat

import org.dom4j.Element
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.util.DateStorageUtil

import org.openedit.money.Money
import org.openedit.store.CartItem
import org.openedit.store.Store
import org.openedit.store.Product
import org.openedit.store.InventoryItem
import org.openedit.store.orders.Order
import org.openedit.store.util.MediaUtilities
import org.openedit.store.orders.Shipment
import org.openedit.store.orders.ShipmentEntry
import org.openedit.store.orders.Refund
import org.openedit.store.orders.RefundItem
import org.openedit.store.orders.RefundState

import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.Page
import com.openedit.page.manage.PageManager
import com.openedit.util.XmlUtil

import groovy.util.slurpersupport.GPathResult


public void processInvoices() {
	
	WebPageRequest inReq = context;
	MediaArchive archive = inReq.getPageValue("mediaarchive");
	String catalogID = archive.getCatalogId();

	SearcherManager manager = archive.getSearcherManager();
	Searcher invoicesearcher = archive.getSearcher("invoice");
	Searcher itemsearcher = archive.getSearcher("invoiceitem");
	
	SearchQuery query = invoicesearcher.createSearchQuery().append("invoicestatus", "new");
	HitTracker hits = invoicesearcher.search(query);
	hits.each{
		Data data = it;
		String orderId = data.get("orderid");
		Order order = (Order) archive.getData("storeOrder", orderId);
		if (order == null){
			Data errdata = invoicesearcher.searchById(data.getId());
			errdata.setProperty("invoicestatus", "error");
			errdata.setProperty("notes", "Order ${orderId} cannot be found on the system");
			invoicesearcher.saveData(errdata, null);
			return;//equivalent to continue;
		}
		processInvoice(data,order,invoicesearcher,itemsearcher);
		//update invoicestatus: new approved complete updated submitted waiting paid printed error
	}
}

public void processInvoice(Data data, Order order, Searcher invoiceSearcher, Searcher invoiceItemSearcher){
	StringBuilder buf = new StringBuilder();
	//get deltas from other invoices already processed
	ArrayList<Money> tally = getDeltasFromApprovedInvoices(order,invoiceSearcher);
	//get initial deltas: tax - 0, shipping - 1, total - 2
	Money orderTax = tally.get(0);
	Money orderShipping = tally.get(1);
	Money orderTotal = tally.get(2);
	//calculate deltas from current invoice data
	Money deltaTax = calculateDelta(orderTax,data,new ArrayList<String>(["fedtaxamount","provtaxamount"]));
	Money deltaShipping = calculateDelta(orderShipping,data,new ArrayList<String>(["shipping"]));
	Money deltaTotal = calculateDelta(orderTotal,data,new ArrayList<String>(["invoicetotal"]));
	//if any of the deltas are negative then we recorder it
	if (deltaTotal.isNegative() || deltaTax.isNegative() || deltaShipping.isNegative()){
		buf.append("At least one of the tallies is less than zero (Tax=${deltaTax}, Shipping=${deltaShipping}, Total=${deltaTotal})");
	}
	//go through each invoice item
	boolean isItemsOk = true;
	List<CartItem> cartItems = order.getItems();
	SearchQuery itemsquery = invoiceItemSearcher.createSearchQuery().append("invoiceid", data.getId());
	HitTracker invoiceitems = invoiceItemSearcher.search(itemsquery);
	invoiceitems.each{
		Data invoiceItem = it;
		StringBuilder msg = new StringBuilder();
		int quantity = Integer.parseInt(invoiceItem.get("quantity") == null || invoiceItem.get("quantity").isEmpty() ? "1" : invoiceItem.get("quantity"));
		Money invoiceUnitPrice = new Money(invoiceItem.get("unitprice") == null || invoiceItem.get("unitprice").isEmpty() ? "0" : invoiceItem.get("unitprice"));
		Money invoicePrice = new Money(invoiceItem.get("price") == null || invoiceItem.get("price").isEmpty() ? "0" : invoiceItem.get("price"));
		Money subtotal = invoiceUnitPrice.multiply(new Integer(quantity));
		if ( subtotal.compareTo(invoicePrice) != 0){
			msg.append("Arithmetic incorrect on invoice (${invoicePrice} vs. ${subtotal})");
			isItemsOk = false;
		}
		String productid = invoiceItem.get("productid");
		if (productid!=null){
			CartItem target = null;
			for(CartItem cartItem:cartItems){
				if (productid.equals(cartItem.getProduct().getId())){
					target = cartItem;
					break;
				}
			}
			if (target!=null){
				Money listedPrice = target.getYourPrice();
				//@todo: need to save wholesale price on an order
				//using temporary fix that calculates the known mark-up but needs to change going forward
				listedPrice = listedPrice.divide("1.10");
				if (invoiceUnitPrice.compareTo(listedPrice)!=0){
					if (!msg.toString().isEmpty()) msg.append("; ");
					msg.append("Unit price conflict (${invoiceUnitPrice} vs. ${listedPrice})");
					isItemsOk = false;
				}
				int listedQuantity = target.getQuantity();
				if (quantity > listedQuantity){
					if (!msg.toString().isEmpty()) msg.append("; ");
					msg.append("Quantity exceeds order quantity (${quantity} vs. ${listedQuantity})");
					isItemsOk = false;
				}
				//@todo: add refund logic here
//				RefundState refundState = target.getRefundState();
			} else {
				if (!msg.toString().isEmpty()) msg.append("; ");
				msg.append("Cannot find item in order (${productid})");
				isItemsOk = false;
			}
		} else {
			if (!msg.toString().isEmpty()) msg.append("; ");
			msg.append("No product Id on invoice item");
			isItemsOk = false;
		}
		Data item = invoiceItemSearcher.searchById(invoiceItem.getId());
		if (msg.toString().isEmpty()){
			item.setProperty("invoiceitemstatus", "verified");
		} else {
			item.setProperty("invoiceitemstatus", "conflictdetection");
			item.setProperty("notes",msg.toString());
		}
		invoiceItemSearcher.saveData(item, null);
	}
	if (!isItemsOk){
		if (!buf.toString().isEmpty()) buf.append("; ");
		buf.append("Problem with at least one of the items on the invoice");
	}
	//update invoice table
	Data invoice = invoiceSearcher.searchById(data.getId());
	if (buf.toString().isEmpty()){
		invoice.setProperty("invoicestatus", "verified");//data has been verified
	} else {
		invoice.setProperty("invoicestatus", "conflictdetection");//a conflict was detected
		invoice.setProperty("notes", buf.toString());
	}
	invoiceSearcher.saveData(invoice, null);
}

public ArrayList<Money> getDeltasFromApprovedInvoices(Order order, Searcher searcher) throws Exception {
	ArrayList<Money> list = new ArrayList<Money>();
	Money orderTax = order.getTax() != null ? order.getTax() : new Money("0");
	Money orderShipping = order.getTotalShipping() != null ? order.getTotalShipping() : new Money("0");
	Money orderTotal = order.getTotalPrice() != null ? order.getTotalPrice() : new Money("0");
	SearchQuery query = searcher.createSearchQuery().append("orderid", order.getId()).append("invoicestatus", "approved");//approved || complete
	HitTracker hits = searcher.search(query);
	hits.each{
		Data data = it;
		orderTotal = calculateDelta(orderTotal,data,new ArrayList<String>(["invoicetotal"]));
		orderTax = calculateDelta(orderTax,data,new ArrayList<String>(["fedtaxamount","provtaxamount"]));
		orderShipping = calculateDelta(orderShipping,data,new ArrayList<String>(["shipping"]));
	}
	list.add(orderTax);
	list.add(orderShipping);
	list.add(orderTotal);
	return list;
}

public Money calculateDelta(Money initial, Data data, ArrayList<String> parameters){
	for (String parameter:parameters){
		String value = data.get(parameter);
		if (value==null || value.isEmpty()){
			continue;
		}
		initial = initial.subtract(new Money(value));
	}
	return initial;
}

processInvoices();