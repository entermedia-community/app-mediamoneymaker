package edi;

import java.text.SimpleDateFormat

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.CatalogConverter;
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
		result.setErrorMessage("");
		String foundErrors = "";

		result.setComplete(false);
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

					def ASNGROUPS = ASN.depthFirst().grep{
						it.name() == 'ASNGroup';
					}
					log.info("Found ASNGRoups: " + ASNGROUPS.size().toString());

					ASNGROUPS.each {
						
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

						def ASNHEADERS = ASN.ASNGroup.depthFirst().grep{
							it.name() == 'ASNHeader';
						}
						log.info("Found ASNHeaders: " + ASNHEADERS.size().toString());

						//Set the Purchase Order Found Flag to false
						def boolean foundFlag = false;

						ASNHEADERS.each {
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
									String inMsg = "Order(" + orderInfo[0] + ") was not found from ASN.";
									result.setErrorMessage(result.getErrorMessage() + "\n" + inMsg);
								}
								HitTracker foundStore = searchForStoreInOrder(manager, archive, order.id, orderInfo[1]);
								if (foundStore != null) {
									setPurchaseOrder(purchaseOrder);
									setStoreID(orderInfo[1]);
									strMsg += output.appendOutMessage("Purchase Order ", purchaseOrder, FOUND);
									strMsg += output.appendOutMessage("Store ", orderInfo[1], FOUND);
								} else {
									log.info("ERROR: Store(" + orderInfo[1] + ") was not found from ASN.");
									log.info("ERROR: PurchaseOrder: " + purchaseOrder);
									//Create web event to send an email.
									strMsg += output.appendOutMessage("Purchase Order ", orderInfo[1], NOT_FOUND);
									String inMsg = "Purchase Order(" + orderInfo[1] + ") was not found from ASN.";
									result.setErrorMessage(result.getErrorMessage() + "\n" + inMsg);
								}
								//Reset the found flag
								foundFlag = false;
								ASNHEADERS.each {
									if (!foundFlag) {
										//SC
										def String courier = it.Attributes.TblEntityID.find {it.Qualifier == "SC"}.EntityValue.text();
										if (!courier.isEmpty()) {
											foundFlag = true;
											strMsg += output.appendOutMessage("Courier", courier, FOUND);
											setCarrier(courier);
										}
									}
								}
								if (!foundFlag) {
									strMsg += output.appendOutMessage("Courier", "", NOT_FOUND);
									String inMsg = "Courier cannot be found in ASN XML File(" + page.getName() + ")";
									result.setErrorMessage(result.getErrorMessage() + "\n" + inMsg);
								}

								//Reset the found flag
								foundFlag = false;
								ASNHEADERS.each {
									if (!foundFlag) {
										//_PRO
										def String waybill = it.Attributes.TblReferenceNbr.find {it.Qualifier == "_PRO"}.ReferenceNbr.text();
										if (!waybill.isEmpty()) {
											foundFlag = true;
											strMsg += output.appendOutMessage("Waybill", waybill, FOUND);
											setWaybill(waybill);
										}
									}
								}
								if (!foundFlag) {
									strMsg += output.appendOutMessage("Waybill", "", NOT_FOUND);
									String inMsg = "Waybill cannot be found in ASN XML File(" + page.getName() + ")";
									result.setErrorMessage(result.getErrorMessage() + "\n" + inMsg);
								}
								if (foundFlag) {

									def SUBHEADERS = it.depthFirst().grep{
										it.name() == 'ASNHeader';
									}
									log.info("Found SUBHEADERS: " + SUBHEADERS.size().toString());

									//Reset the found flag
									foundFlag = false;
									SUBHEADERS.each {
										if (!foundFlag) {
											//QS
											def String quantityShipped = it.Attributes.TblAmount.find {it.Qualifier == "QS"}.Amount.text();
											if (!quantityShipped.isEmpty()) {
												foundFlag = true;
												strMsg += output.appendOutMessage("Quantity Shipped", quantityShipped, FOUND);
												setQuantityShipped(quantityShipped);
											}
										}
									}
									if (!foundFlag) {
										strMsg += output.appendOutMessage("Quantity Shipped", "", NOT_FOUND);
										String inMsg = "Quantity Shipped cannot be found in ASN XML File(" + page.getName() + ")";
										result.setErrorMessage(result.getErrorMessage() + "\n" + inMsg);
									}

									//Reset the found flag
									foundFlag = false;
									SUBHEADERS.each {
										if (!foundFlag) {
											//QS
											def String dateShipped = it.Attributes.TblDate.find {it.Qualifier == "004"}.DateValue.text();
											if (!dateShipped.isEmpty()) {
												foundFlag = true;
												def newDate = parseDate(dateShipped);
												strMsg += output.appendOutMessage("Date Shipped", dateShipped, FOUND);
												setDateShipped(newDate);
											}
										}
									}
									if (!foundFlag) {
										strMsg += output.appendOutMessage("Date Shipped", "", NOT_FOUND);
										String inMsg = "Date Shipped cannot be found in ASN XML File(" + page.getName() + ")";
										result.setErrorMessage(result.getErrorMessage() + "\n" + inMsg);
									}

									//Reset the found flag
									foundFlag = false;
									SUBHEADERS.each {
										if (!foundFlag) {
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
													String inMsg = "Product(" + UPC + ") was not found from ASN.";
													result.setErrorMessage(result.getErrorMessage() + "\n" + inMsg);
												}
											}
										}
									}
									if (!foundFlag) {
										strMsg += output.appendOutMessage("Product", "", NOT_FOUND);
										String inMsg = "Product cannot be found in ASN XML File(" + page.getName() + ")";
										result.setErrorMessage(result.getErrorMessage() + "\n" + inMsg);
									}

									//Finally write the data to the Tables!
									String errMsg = "";
									if (result.getErrorMessage() != null) {
										errMsg = result.getErrorMessage();
									}
									if ((foundFlag) && (errMsg.length() == 0)) {
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
											strMsg += output.appendOutMessage("ITEM UPDATED(" + getPurchaseOrder() + ") AND SAVED");
										} else {
											strMsg += output.appendOutMessage("ERROR ORDER(" + getPurchaseOrder() + ") " + NOT_FOUND);
											String inMsg = "Order cannot be found(" + getOrderID() + ")";
											result.setErrorMessage(result.getErrorMessage() + "\n" + inMsg);
										} // end if orderitems
									} else {
										strMsg += output.appendOutMessage("ERROR ORDER(" + getPurchaseOrder() + ") NOT SAVED");
									}
								} else {
									strMsg += output.appendOutMessage("ERROR PURCHASE ORDER NOT FOUND IN ASN (" + page.getName() + ")");
									String inMsg = "Purchase Order cannot be found in ASN XML File(" + page.getName() + ")";
									result.setErrorMessage(result.getErrorMessage() + "\n" + inMsg);
								}
							}
							if (result.getErrorMessage() != null) {
								foundErrors += result.getErrorMessage();
								result.setErrorMessage("");
							}
						}
					}
					if (foundErrors.isEmpty()) {
						result = movePageToProcessed(pageManager, page, catalogid, true);
						if (result.complete) {
							strMsg += output.appendOutMessage("ASN FILE(" + page.getName() + ") MOVED");
						} else {
							strMsg += output.appendOutMessage("ASN FILE(" + page.getName() + ") FAILED MOVE");
						}
					} else {
						strMsg += output.appendOutMessage("ERROR ORDER(" + getOrderID() + ") NOT SAVED");
						result = movePageToProcessed(pageManager, page, catalogid, false);
						if (result.complete) {
							strMsg += output.appendOutMessage("ASN FILE(" + page.getName() + ") MOVED TO ERROR");
						} else {
							strMsg += output.appendOutMessage("ASN FILE(" + page.getName() + ") FAILED MOVE TO ERROR");
						}
					}
				} else {
					result.setErrorMessage(realpath + " does not exist!");
				}
			}
			strMsg += output.finishTable();
			result.setCompleteMessage(strMsg);
			result.setComplete(true);
			//extractTestXML(page, log, result);
			if (!foundErrors.isEmpty()) {
				result.setErrorMessage(foundErrors + "\n");
				log.info("ERROR: The folowwing errors occurred:");
				log.info(result.getErrorMessage());
				//Create web event to send an email.
				WebEvent event = new WebEvent();
				event.setSearchType("asn_processing");
				event.setCatalogId(catalogid);
				event.setProperty("error", result.getErrorMessage());
				archive.getMediaEventHandler().eventFired(event);
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

	private HitTracker searchForStoreInOrder( SearcherManager manager,
	MediaArchive archive, String orderid, String store_number ) {

		Searcher storesearcher = manager.getSearcher(archive.getCatalogId(), "rogers_order_item");
		SearchQuery itemQuery = storesearcher.createSearchQuery();
		itemQuery.addExact("store", store_number);
		itemQuery.addExact("rogers_order", orderid);
		HitTracker orderitems = storesearcher.search(itemQuery);

		return orderitems;

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

	private PublishResult movePageToProcessed(PageManager pageManager, Page page,
	String catalogid, boolean saved) {

		PublishResult move = new PublishResult();
		move.setComplete(false);

		String processedFolder = "/WEB-INF/data/${catalogid}/processed/asn/";
		String asnFolder = "/WEB-INF/data/${catalogid}/incoming/asn/";
		if (!saved) {
			processedFolder += "errors/";
		}

		String destinationFile = processedFolder + page.getName();
		Page destination = pageManager.getPage(destinationFile);
		pageManager.movePage(page, destination);
		if (destination.exists()) {
			move.setCompleteMessage(page.getName() + " has been moved to " + destinationFile);
			move.setComplete(true);
		} else {
			move.setErrorMessage(page.getName() + " could not be moved.");
		}
		return move;
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
		String errMsg = "";
		if (result.getErrorMessage() != null) {
			errMsg = result.getErrorMessage();
		}
		if (errMsg.length() > 0) {
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
