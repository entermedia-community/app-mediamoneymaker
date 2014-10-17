package shipping

import java.text.SimpleDateFormat

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.event.WebEvent
import org.openedit.event.WebEventHandler
import org.openedit.event.WebEventListener
import org.openedit.repository.ContentItem
import org.openedit.store.CartItem
import org.openedit.store.Store
import org.openedit.store.orders.Order
import org.openedit.store.orders.Shipment
import org.openedit.store.orders.ShipmentEntry
import org.openedit.util.DateStorageUtil

import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.page.Page
import com.openedit.users.User
import com.openedit.util.PathProcessor

import groovy.util.slurpersupport.GPathResult;

public void init(){
	log.info("----- Starting Import ASN -----");
	MediaArchive archive = context.getPageValue("mediaarchive");
	XMLPathProcessor processor = new XMLPathProcessor();
	processor.setLogger(log);
	processor.setArchive(archive);
	processor.setRecursive(true);
	processor.setRootPath("/WEB-INF/data/${catalogid}/incoming/asn/");
	processor.setPageManager(archive.getPageManager());
	processor.setContext(context);
	processor.setIncludeExtensions("xml");
	processor.init();
	processor.process();
	log.info("----- End Import ASN -----");
}

class XMLPathProcessor extends PathProcessor
{
	final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
	MediaArchive archive;
	ScriptLogger logger;
	WebPageRequest context;
	Store store;
	SearcherManager manager;
	Searcher distributorsearcher;
	Searcher asnsearcher;
	String processedFolder;
	
	public void setArchive(MediaArchive inArchive)
	{
		archive = inArchive;
	}
	
	public void setLogger(ScriptLogger inLog)
	{
		logger = inLog;
	}
	
	public void setContext(WebPageRequest inReq)
	{
		context = inReq;
	}
	
	public void init(){
		store  = context.getPageValue("store");
		manager = archive.getSearcherManager();
		distributorsearcher = manager.getSearcher(archive.getCatalogId(), "distributor");
		asnsearcher = manager.getSearcher(archive.getCatalogId(), "asn");
		processedFolder = "/WEB-INF/data/${archive.getCatalogId()}/processed/asn/";
	}
	
	public int toInt(String inString, int inDefault){
		try{
			return Integer.parseInt(inString);
		}catch (Exception e){
			log.error("Exception caught parsing integer, ${inString}, ${e.getMessage()}");
		}
		return inDefault;
	}
	
	public boolean isValid(ArrayList<String> inList){
		for(String str:inList){
			if (str==null || str.isEmpty())
				return false;
		}
		return true;
	}
	
	public void updateASN(Page inPage, String inDistributor, String inCarrier, String inWaybill, String inPurchaseOrder, Date inShippingDate, int inQuantity, String inVendorCode, String notes){
		Data data = asnsearcher.createNewData();
		data.setProperty("processdate", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
		data.setProperty("shippingdate", DateStorageUtil.getStorageUtil().formatForStorage(inShippingDate));
		data.setProperty("waybill", inWaybill);
		data.setProperty("carrier", inCarrier);
		data.setProperty("purchaseorder", inPurchaseOrder);
		data.setProperty("vendorcode", inVendorCode);
		data.setProperty("quantity", String.valueOf(inQuantity));
		data.setProperty("asnfile", inPage.getName());
		data.setProperty("distributor", inDistributor);
		if (notes!=null && !notes.isEmpty()){
			data.setProperty("notes",notes);
			data.setProperty("asnstatus", "fail");
		} else {
			data.setProperty("asnstatus", "pass");
		}
		asnsearcher.saveData(data, null);
	}
	
	public void processFile(ContentItem inContent, User inUser)
	{
		String path = inContent.getPath();
		Page page = archive.getPageManager().getPage(path);
		logger.info("Processing ${page.getName()}, path=${page.getParentPath()}");
		GPathResult asnInfo = new XmlSlurper().parse(page.getReader());
		ArrayList<String> asnOrders = new ArrayList<String>();
		String gssnd = asnInfo.Attributes.TblReferenceNbr.find {it.Qualifier == "GSSND"}.ReferenceNbr.text();
		Data distributorData = distributorsearcher.searchByField("idcode", gssnd);
		if (distributorData == null){
			distributorData = distributorsearcher.searchByField("headermailboxprod", gssnd);
		}
		Collection<?> asnGroups = asnInfo.depthFirst().grep {it.name() == "ASNGroup"};
		asnGroups.each {
			Collection<?> asnHeaders = asnInfo.depthFirst().grep {it.name() == "ASNHeader"};
			String carrier = null;//1
			String waybill = null;
			String purchaseOrder = null;//2
			Date shippingDate = null;
			int quantity = 0;//4
			String vendorCode = null;
			asnHeaders.each {
				int level = toInt(it.PackLevel.text(),0);
				switch(level){
					case 1:
						carrier = it.Attributes.TblEntityID.find {it.Qualifier == "SC"}.EntityValue.text();
						waybill = it.Attributes.TblReferenceNbr.find {it.Qualifier == "_PRO"}.ReferenceNbr.text();
						if (!waybill.isEmpty() && carrier.isEmpty()){
							carrier = waybill.startsWith("1Z") || waybill.startsWith("1z") ? "UNITED PARCEL POST" : "NOT PROVIDED";
						}
						purchaseOrder = vendorCode = null;
						quantity = 0;
						shippingDate = null;
						break;
					case 2:
						purchaseOrder = it.Attributes.TblReferenceNbr.find {it.Qualifier == "PO"}.ReferenceNbr.text();
						shippingDate = DATE_FORMAT.parse(it.Attributes.TblDate.find {it.Qualifier == "004"}.DateValue.text());
						break;
					case 4:
						quantity = toInt(it.Attributes.TblAmount.find {it.Qualifier == "QS"}.Amount.text(),0);
						vendorCode = it.Attributes.TblReferenceNbr.find {it.Qualifier == "VN"}.ReferenceNbr.text();
						break;
				}
				//check if variables were updated
				if (!isValid(new ArrayList<String>([carrier,waybill,purchaseOrder,shippingDate==null?"":shippingDate.toString(),quantity == 0?"":"${quantity}",vendorCode]))){
					return;
				}
				//check distributor
				if (distributorData == null){
					updateASN(page,gssnd,carrier,waybill,purchaseOrder,shippingDate,quantity,vendorCode,"Unable to find distributor ${gssnd}.");
					return;
				}
				//check for corporate orders
				purchaseOrder = purchaseOrder.trim();//trim first 
				if(purchaseOrder.toLowerCase().startsWith("rogers") && purchaseOrder.contains("|")){
					//note: cannot use string.split() in goovy - use substring instead
					purchaseOrder = purchaseOrder.substring(0, purchaseOrder.indexOf("|")).trim();
				}
				//make sure to replace upper case chars with mixed case ones
				purchaseOrder = purchaseOrder.replace("ROGERS", "Rogers");
				Order order = (Order) store.getOrderSearcher().searchById(purchaseOrder);
				if (order == null){
					int dash = -1;
					if ((dash = purchaseOrder.indexOf("-"))!=-1){
						String po = purchaseOrder.substring(0, dash).trim();
						order = (Order) store.getOrderSearcher().searchById(purchaseOrder);
					}
				}
				logger.info("searched for $purchaseOrder, found $order");
				if (order == null){
					updateASN(page,distributorData.getId(),carrier,waybill,purchaseOrder,shippingDate,quantity,vendorCode,"Unable to find order ${purchaseOrder}.");
					return;
				}
				CartItem cartItem = order.getCartItemByProductProperty("manufacturersku", vendorCode);
//				logger.info("<span style='color:red'>Order: $order, Item: $vendorCode, Waybill: $waybill, Carrier: $carrier, Date: $shippingDate, Quantity: $quantity</span>");
				if (cartItem == null){
					updateASN(page,distributorData.getId(),carrier,waybill,purchaseOrder,shippingDate,quantity,vendorCode,"Unable to find cart item ${vendorCode} in order ${order.getId()}.");
					return;
				}
				boolean updateOrder = false;
				Shipment shipment = order.getShipmentByWaybill(waybill);
				if(shipment == null){
					shipment = new Shipment();
					shipment.setProperty("courier", carrier);
					shipment.setProperty("waybill", waybill);
					shipment.setProperty("distributor", distributorData.getId());
					shipment.setProperty("shipdate", DateStorageUtil.getStorageUtil().formatForStorage(shippingDate));
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
					if(order.isFullyShipped()){
						order.setProperty("shippingstatus", "shipped");
					}else{
						order.setProperty("shippingstatus", "partialshipped");
					}
					store.saveOrder(order);
					//append to order history
					appendShippingNoticeToOrderHistory(order,waybill);
					if(order.isFullyShipped())
					{
						appendFullyShippedNoticeToOrderHistory(order);
					}
				}
			}
		}
		//move file to processed
		Page destination = archive.getPageManager().getPage(processedFolder+page.getName());
		archive.getPageManager().movePage(page, destination);
		logger.info("Moved ${page.getName()} to ${destination.getParentPath()}");
	}
	
	protected appendShippingNoticeToOrderHistory(Order order, String waybill)
	{
		//record this in order history
		WebEvent event = new WebEvent();
		event.setSearchType("detailedorderhistory");
		event.setCatalogId(archive.getCatalogId());
		event.setProperty("applicationid", context.findValue("applicationid"));
		event.setOperation("orderhistory/appendorderhistory");
		event.setProperty("orderid", order.getId());
		event.setProperty("type","automatic");
		event.setProperty("state","shippingnoticereceived");
		event.setProperty("shipmentid", waybill);
		archive.getMediaEventHandler().eventFired(event);
	}
	
	protected appendFullyShippedNoticeToOrderHistory(Order order)
	{
		//record this in order history
		WebEvent event = new WebEvent();
		event.setSearchType("detailedorderhistory");
		event.setCatalogId(archive.getCatalogId());
		event.setProperty("applicationid", context.findValue("applicationid"));
		event.setOperation("orderhistory/appendorderhistory");
		event.setProperty("orderid", order.getId());
		event.setProperty("type","automatic");
		event.setProperty("state","fullyshipped");
		archive.getMediaEventHandler().eventFired(event);
	}
}

init();
