package edi;

import java.text.SimpleDateFormat

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.PublishResult
import org.openedit.event.WebEvent
import org.openedit.util.DateStorageUtil

import com.openedit.OpenEditException
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.Page
import com.openedit.page.manage.PageManager

public class ImportEDIASN extends EnterMediaObject {

	private static final String LOG_HEADER = " - RECORDS - ";
	private static final String FOUND = "Found"
	private static final String NOT_FOUND = "NOT FOUND!";
	private static String strMsg = "";

	private String orderID;
	private String purchaseOrder;
	private String distributorID;
	private String storeID;
	private String carrier;
	private String waybill;
	private String quantityShipped;
	private String productID;
	private Date dateShipped;

	private void setOrderID(String inOrderID) {
		orderID = inOrderID;
	}

	private String getOrderID() {
		return orderID;
	}

	private void setPurchaseOrder(String inPurchaseOrder) {
		purchaseOrder = inPurchaseOrder;
	}

	private String getPurchaseOrder() {
		return purchaseOrder;
	}

	private void setDistributorID(String inDistributorID) {
		distributorID = inDistributorID;
	}

	private String getDistributorID() {
		return distributorID;
	}

	private void setStoreID(String inStore) {
		storeID = inStore;
	}

	private String getStoreID() {
		return storeID;
	}

	private void setCarrier(String inCarrier) {
		carrier = inCarrier;
	}

	private String getCarrier() {
		return carrier;
	}

	private void setWaybill(String inWaybill) {
		waybill = inWaybill;
	}

	private String getWaybill() {
		return waybill;
	}
	
	private void setQuantityShipped(String inQuantityShipped) {
		quantityShipped = inQuantityShipped;
	}
	
	private String getQuantityShipped() {
		return quantityShipped;
	}

	private void setProductID(String inProductID) {
		productID = inProductID;
	}
	
	private String getProductID() {
		return productID;
	}
	
	private void setDateShipped(Date inDate) {
		dateShipped = inDate;
	}
	
	private Date getDateShipped() {
		return dateShipped;
	}

	public PublishResult importEdiXml() {

		PublishResult result = new PublishResult();

		result.setComplete(false);
		result.setErrorMessage("");
		Util output = new Util();

		String catalogid = getMediaArchive().getCatalogId();
		MediaArchive archive = context.getPageValue("mediaarchive");

		SearcherManager manager = archive.getSearcherManager();
		PageManager pageManager = archive.getPageManager();
		boolean production = Boolean.parseBoolean(context.findValue('productionmode'));

		String asnFolder = "/WEB-INF/data/${catalogid}/incoming/asn/";

		List dirList = pageManager.getChildrenPaths(asnFolder);
		log.info("Initial directory size: " + dirList.size().toString());

		strMsg = output.createTable("Section", "Values", "Status");

		if (dirList.size() > 0) {
			def int iterCounter = 0;
			for (Iterator iterator = dirList.iterator(); iterator.hasNext();) {
				Page page = pageManager.getPage(iterator.next());
				log.info("Processing " + page.getName());

				String realpath = page.getContentItem().getAbsolutePath();

				File xmlFIle = new File(realpath);
				if (xmlFIle.exists()) {
					strMsg += output.appendOutMessage("<b>" + page.getName() + "</b>");

					//Create the XMLSlurper Object
					def ASN = new XmlSlurper().parse(page.getReader());

					//Get the distributor
					def String GSSND = ASN.Attributes.TblReferenceNbr.find {it.Qualifier == "GSSND"}.ReferenceNbr.text();
					strMsg += output.appendOutMessage("GSSND", GSSND, FOUND);
					Data distributor = searchForDistributor(manager, archive, GSSND, production);
					if (distributor != null) {
						strMsg += output.appendOutMessage("Distributor", distributor.name, FOUND);
						setDistributorID(distributor.getId());
					} else {
						throw new OpenEditException("ERROR: Distributor value is blank in ASN.");
					}

					def ASNHeaders = ASN.ASNGroup.depthFirst().grep{
						it.name() == 'ASNHeader';
					}
					log.info("Found ASNHeaders: " + ASNHeaders.size().toString());

					//Set the Purchase Order Found Flag to false
					def boolean foundFlag = false;

					ASNHeaders.each {
						//PO
						def String purchaseOrder = it.Attributes.TblReferenceNbr.find {it.Qualifier == "PO"}.ReferenceNbr.text();
						if (!purchaseOrder.isEmpty()) {
							String[] orderInfo = purchaseOrder.split("-");
							Data order = searchForOrder(manager, archive, orderInfo[0]);
							foundFlag = true;
							if (order != null) {
								setOrderID(orderInfo[0]);
								strMsg += output.appendOutMessage("Rogers Order ", orderInfo[0], FOUND);
							} else {
								log.info("ERROR: Order(" + orderInfo[0] + ") was not found from ASN.");
								//Create web event to send an email.
								strMsg += output.appendOutMessage("Rogers Order ", orderInfo[0], NOT_FOUND);
								String inMsg = "<p>Order(" + orderInfo[0] + ") was not found from ASN.</p>";
								result.setErrorMessage(result.getErrorMessage() + "\n" + inMsg);
							}
							if (order.as400po.equalsIgnoreCase(orderInfo[1])) {
								setPurchaseOrder(order.as400po);
								strMsg += output.appendOutMessage("Purchase Order ", orderInfo[1], FOUND);
							} else {
								log.info("Purchase Order(" + orderInfo[0] + ") was not found from ASN.");
								//Create web event to send an email.
								strMsg += output.appendOutMessage("Purchase Order ", orderInfo[1], NOT_FOUND);
								String inMsg = "<p>Purchase Order(" + orderInfo[1] + ") was not found from ASN.</p>";
								result.setErrorMessage(result.getErrorMessage() + "\n" + inMsg);
							}
						}
					}
					if (!foundFlag) {
						strMsg += output.appendOutMessage("Purchase Order ", "", NOT_FOUND);
						String inMsg = "<p>Purchase Order cannot be found in ASN XML File(" + page.getName() + ")</p>";
						result.setErrorMessage(result.getErrorMessage() + "\n" + inMsg);
					}

					//Reset the found flag
					foundFlag = false;
					ASNHeaders.each {
						//ST
						def String storeInfo = it.Attributes.TblAddress.find {it.AddressType == "ST"}.AddressName1.text();
						if (!storeInfo.isEmpty()) {
							def String[] storeValue = storeInfo.split("-");
							Data store = searchForStore(manager, archive, storeValue[0]);
							if (store != null) {
								foundFlag = true;
								strMsg += output.appendOutMessage("Store", store.name, FOUND);
								setStoreID(store.store);
							} else {
								log.info("Store(" + storeValue + ") was not found from ASN.");
								//Create web event to send an email.
								strMsg += output.appendOutMessage("Purchase Order ", storeValue[0], NOT_FOUND);
								String inMsg = "<p>Purchase Order(" + storeValue[0] + ") was not found from ASN.</p>";
								result.setErrorMessage(result.getErrorMessage() + "\n" + inMsg);
							}
						}
					}
					if (!foundFlag) {
						strMsg += output.appendOutMessage("Store", "", NOT_FOUND);
						String inMsg = "<p>Store cannot be found in ASN XML File(" + page.getName() + ")</p>";
						result.setErrorMessage(result.getErrorMessage() + "\n" + inMsg);
					}

					//Reset the found flag
					foundFlag = false;
					ASNHeaders.each {
						//SC
						def String courier = it.Attributes.TblEntityID.find {it.Qualifier == "SC"}.EntityValue.text();
						if (!courier.isEmpty()) {
							foundFlag = true;
							strMsg += output.appendOutMessage("Courier", courier, FOUND);
							setCarrier(courier);
						}
					}
					if (!foundFlag) {
						strMsg += output.appendOutMessage("Courier", "", NOT_FOUND);
						String inMsg = "<p>Courier cannot be found in ASN XML File(" + page.getName() + ")</p>";
						result.setErrorMessage(result.getErrorMessage() + "\n" + inMsg);
					}

					//Reset the found flag
					foundFlag = false;
					ASNHeaders.each {
						//_PRO
						def String waybill = it.Attributes.TblReferenceNbr.find {it.Qualifier == "_PRO"}.ReferenceNbr.text();
						if (!waybill.isEmpty()) {
							foundFlag = true;
							strMsg += output.appendOutMessage("Waybill", waybill, FOUND);
							setWaybill(waybill);
						}
					}
					if (!foundFlag) {
						strMsg += output.appendOutMessage("Waybill", "", NOT_FOUND);
						String inMsg = "<p>Waybill cannot be found in ASN XML File(" + page.getName() + ")</p>";
						result.setErrorMessage(result.getErrorMessage() + "\n" + inMsg);
					}

					//Reset the found flag
					foundFlag = false;
					ASNHeaders.each {
						//QS
						def String quantityShipped = it.Attributes.TblAmount.find {it.Qualifier == "QS"}.Amount.text();
						if (!quantityShipped.isEmpty()) {
							foundFlag = true;
							strMsg += output.appendOutMessage("Quantity Shipped", quantityShipped, FOUND);
							setQuantityShipped(quantityShipped);
						}
					}
					if (!foundFlag) {
						strMsg += output.appendOutMessage("Quantity Shipped", "", NOT_FOUND);
						String inMsg = "<p>Quantity Shipped cannot be found in ASN XML File(" + page.getName() + ")</p>";
						result.setErrorMessage(result.getErrorMessage() + "\n" + inMsg);
					}

					//Reset the found flag
					foundFlag = false;
					ASNHeaders.each {
						//QS
						def String dateShipped = it.Attributes.TblDate.find {it.Qualifier == "004"}.DateValue.text();
						if (!dateShipped.isEmpty()) {
							foundFlag = true;
							def newDate = parseDate(dateShipped);
							strMsg += output.appendOutMessage("Date Shipped", dateShipped, FOUND);
							setDateShipped(newDate);
						}
					}
					if (!foundFlag) {
						strMsg += output.appendOutMessage("Quantity Shipped", "", NOT_FOUND);
						String inMsg = "<p>Quantity Shipped cannot be found in ASN XML File(" + page.getName() + ")</p>";
						result.setErrorMessage(result.getErrorMessage() + "\n" + inMsg);
					}

					//Reset the found flag
					foundFlag = false;
					ASNHeaders.each {
						//_PRO
						def String UPC = it.Attributes.TblReferenceNbr.find {it.Qualifier == "UP"}.ReferenceNbr.text();
						if (!UPC.isEmpty()) {
							foundFlag = true;
							Data product = searchForProduct(manager, archive, UPC);
							if (product != null) {
								foundFlag = true;
								strMsg += output.appendOutMessage("Product", product.name, FOUND);
								setProductID(product.getId());
							} else {
								log.info("Product(" + UPC + ") was not found from ASN.");
								//Create web event to send an email.
								strMsg += output.appendOutMessage("Product ", UPC, NOT_FOUND);
								String inMsg = "<p>Product(" + UPC + ") was not found from ASN.</p>";
								result.setErrorMessage(result.getErrorMessage() + "\n" + inMsg);
							}
						}
					}
					if (!foundFlag) {
						strMsg += output.appendOutMessage("Quantity Shipped", "", NOT_FOUND);
						String inMsg = "<p>Quantity Shipped cannot be found in ASN XML File(" + page.getName() + ")</p>";
						result.setErrorMessage(result.getErrorMessage() + "\n" + inMsg);
					}

					//Finally write the data to the Tables!
					if ((foundFlag) && (result.getErrorMessage().length() == 0)) {
						Searcher itemsearcher = manager.getSearcher(archive.getCatalogId(), "rogers_order_item");
						SearchQuery itemQuery = itemsearcher.createSearchQuery();
						itemQuery.addExact("store", getStoreID());
						itemQuery.addExact("rogers_order", getOrderID());
						itemQuery.addExact("product", getProductID());
						HitTracker orderitems = itemsearcher.search(itemQuery);
						if (orderitems.size() > 0 ) {
							Data orderitem = itemsearcher.createNewData();
							orderitem.setProperty("rogers_order", getOrderID());//foriegn key
							orderitem.setProperty("store", getStoreID());
							orderitem.setProperty("distributor", getDistributorID());
							orderitem.setProperty("product", getProductID());
							orderitem.setProperty("carrier", getCarrier());
							orderitem.setProperty("waybill", getWaybill());
							orderitem.setProperty("orderstatus", "shipped");
							orderitem.setProperty("shipdate", DateStorageUtil.getStorageUtil().formatForStorage(getDateShipped()));
							itemsearcher.saveData(orderitem, context.getUser());
							strMsg += output.appendOutMessage("ITEM SAVED", getOrderID(), "SAVED");
						} else {
							strMsg += output.appendOutMessage("ERROR ORDER", getOrderID(), NOT_FOUND);
							String inMsg = "<p>Order cannot be found(" + getOrderID() + ")</p>";
							result.setErrorMessage(result.getErrorMessage() + "\n" + inMsg);
						} // end if orderitems
					} else {
						strMsg += output.appendOutMessage("ERROR ORDER", getOrderID(), "NOT SAVED");
					}
				} else {
					result.setErrorMessage(realpath + " does not exist!");
				}
			}
			strMsg += output.finishTable();
			result.setCompleteMessage(strMsg);
			result.setComplete(true);
			//extractTestXML(page, log, result);
			if (result.getErrorMessage().length() == 0) {
				result.setErrorMessage(result.getErrorMessage() + "\n");
				log.info("ERROR: The folowwing errors occurred:");
				log.info(result.getErrorMessage());
				//Create web event to send an email.
				WebEvent event = new WebEvent();
				event.setSearchType("asn_processing");
				event.setCatalogId(catalogid);
				event.setProperty("error", result.getErrorMessage());
				archive.getMediaEventHandler().eventFired(event);
				result.setErrorMessage(result.getErrorMessage() + "\n" + inMsg);
			}
		} else {
			result.setCompleteMessage("There are no files to process at this time.");
			result.setComplete(true);
		}
		return result;
	}

	private HitTracker searchForItem( SearcherManager manager,
	MediaArchive archive, String orderID, String store, String distributor) {

		Searcher itemsearcher = manager.getSearcher(archive.getCatalogId(), "rogers_order_item");
		SearchQuery itemQuery = itemsearcher.createSearchQuery();
		itemQuery.addExact("store", storeID);
		itemQuery.addExact("rogers_order", orderid);
		itemQuery.addExact("distributor", distributor.name);
		HitTracker orderitems = itemsearcher.search(itemQuery);

		return orderitems;

	}
	private Data searchForOrder( SearcherManager manager,
	MediaArchive archive, String searchForName ) {

		Searcher ordersearcher = manager.getSearcher(archive.getCatalogId(), "rogers_order");
		Data rogersOrder = ordersearcher.searchById(searchForName);
		return rogersOrder;

	}

	private Data searchForDistributor( SearcherManager manager,
	MediaArchive archive, String searchForName, Boolean production ) {

		String SEARCH_FIELD = "";
		if (production == true){
			SEARCH_FIELD = "headermailboxprod";
		} else {
			SEARCH_FIELD = "headermailboxtest";
		}
		Searcher distributorsearcher = manager.getSearcher(archive.getCatalogId(), "distributor");
		Data targetDistributor = distributorsearcher.searchByField(SEARCH_FIELD, searchForName);

		return targetDistributor;
	}

	private Data searchForStore( SearcherManager manager,
	MediaArchive archive, String searchForName ) {

		String SEARCH_FIELD = "store";
		Searcher storesearcher = manager.getSearcher(archive.getCatalogId(), "store");
		Data rogersStore = storesearcher.searchByField(SEARCH_FIELD, searchForName);

		return rogersStore;

	}
	
	private Data searchForProduct( SearcherManager manager,
	MediaArchive archive, String searchForName ) {

		String SEARCH_FIELD = "upc";
		Searcher productsearcher = manager.getSearcher(archive.getCatalogId(), "product");
		Data product = productsearcher.searchByField(SEARCH_FIELD, searchForName);

		return product;

	}

	private Date parseDate(String date)
	{
		SimpleDateFormat inFormat = new SimpleDateFormat("yyyyMMdd");
		SimpleDateFormat newFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date oldDate = inFormat.parse(date);
		date = newFormat.format(oldDate);
		return (Date)newFormat.parse(date);
	}
}
PublishResult result = new PublishResult();
result.setComplete(false);

logs = new ScriptLogger();
logs.startCapture();

try {

	ImportEDIASN ImportEDIASN = new ImportEDIASN();
	ImportEDIASN.setLog(logs);
	ImportEDIASN.setContext(context);
	ImportEDIASN.setPageManager(pageManager);

	result = ImportEDIASN.importEdiXml();
	if (result.isComplete()) {
		//Output value to CSV file!
		context.putPageValue("export", result.getCompleteMessage());
		if (result.getErrorMessage().length() > 0) {
			context.putPageValue("errorout", result.getErrorMessage());
		}
	} else {
		//ERROR: Throw exception
		context.putPageValue("errorout", result.getErrorMessage());
	}
}
finally {
	logs.stopCapture();
}
