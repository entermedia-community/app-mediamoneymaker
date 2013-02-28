package edi;

import java.text.SimpleDateFormat

import org.openedit.Data
import org.openedit.entermedia.publishing.PublishResult
import org.openedit.event.WebEvent
import org.openedit.store.CartItem
import org.openedit.store.InventoryItem
import org.openedit.store.Product
import org.openedit.store.Store
import org.openedit.store.orders.Order
import org.openedit.store.orders.Shipment
import org.openedit.store.orders.ShipmentEntry
import org.openedit.store.util.MediaUtilities;
import org.openedit.util.DateStorageUtil

import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
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

	public PublishResult importEdiXml() {

		PublishResult ediXMLresult = new PublishResult();
		ediXMLresult.setErrorMessage("");
		ediXMLresult.setCompleteMessage("");
		ediXMLresult.setComplete(false);

		MediaUtilities media = new MediaUtilities();
		media.setContext(context);

		boolean foundData = false;

		Store store = null;

		try {
			store  = media.getContext().getPageValue("store");
			if (store != null) {
				log.info("Store loaded");
				foundData = true;
			} else {
				String inMsg = "ERROR: Could not load store";
				throw new Exception(inMsg);
			}
		}
		catch (Exception e) {
			strMsg = "ERROR: Invalid Order: " + purchaseOrder + "\n";
			strMsg += "Exception thrown:\n";
			strMsg += "Local Message: " + e.getLocalizedMessage() + "\n";
			strMsg += "Stack Trace: " + e.getStackTrace().toString();;
			log.info(strMsg);
		}

		if (foundData) {

			boolean production = Boolean.parseBoolean(context.findValue('productionmode'));
			String asnFolder = "/WEB-INF/data/" + media.getCatalogid() + "/incoming/asn/";
			log.info("Searching " + asnFolder);

			PageManager pageManager = media.getArchive().getPageManager();
			List dirList = pageManager.getChildrenPaths(asnFolder);
			log.info("Initial directory size: " + dirList.size().toString());

			if (dirList.size() > 0) {

				def int iterCounter = 0;
				for (Iterator iterator = dirList.iterator(); iterator.hasNext();) {

					List errorList = new ArrayList();
					List completeList = new ArrayList();

					Page page = pageManager.getPage(iterator.next());
					log.info("Processing " + page.getName());

					String realpath = page.getContentItem().getAbsolutePath();

					File xmlFile = new File(realpath);
					if (xmlFile.exists() && xmlFile.isFile()) {

						iterCounter++;

						String orderID = "";
						String purchaseOrder = "";
						String distributorID = "";
						String storeNumber = "";
						String carrier = "";
						String waybill = "";
						String quantityShipped = "";
						String productID = "";
						String foundErrors = "";
						Date dateShipped = null;

						//Create the XMLSlurper Object
						def ASN = new XmlSlurper().parse(page.getReader());

						def ASNGROUPS = ASN.depthFirst().grep{
							it.name() == 'ASNGroup';
						}
						log.info("Found ASNGRoups: " + ASNGROUPS.size().toString());

						ASNGROUPS.each {

							//Get the distributor
							def String GSSND = ASN.Attributes.TblReferenceNbr.find {it.Qualifier == "GSSND"}.ReferenceNbr.text();
							Data distributor = media.searchForDistributor(GSSND, production);
							if (distributor != null) {
								distributorID = distributor.getId();
							} else {
								strMsg = "Distributor value is blank in ASN.";
								log.info(strMsg);
								errorList.add(strMsg);
							}

							def ASNHEADERS = ASN.ASNGroup.depthFirst().grep{
								it.name() == 'ASNHeader';
							}
							log.info("Found ASNHeaders: " + ASNHEADERS.size().toString());

							boolean poFlagFound = false;

							ASNHEADERS.each {

								//PO
								def String PO = it.Attributes.TblReferenceNbr.find {it.Qualifier == "PO"}.ReferenceNbr.text();

								Shipment shipment = new Shipment();

								if (!PO.isEmpty()) {

									//Set the Purchase Order Found Flag to false
									def boolean foundFlag = false;
									poFlagFound = true;

									purchaseOrder = PO;
									log.info("Purchase Order: " + purchaseOrder);
									Order order = null;
									try {
										order = media.searchForOrder(purchaseOrder);
										if (order != null) {
											foundFlag = true;
											if (order != null) {
												orderID = purchaseOrder;
												shipment.setProperty("distributor", distributorID);
											} else {
												strMsg = "ERROR: Order(" + purchaseOrder + ") was not found from ASN.";
												log.info(strMsg);
												errorList.add(strMsg);
											}
										}
									}
									catch (Exception e) {
										strMsg = "Invalid Order: " + purchaseOrder + "\n";
										strMsg += "Exception thrown:\n";
										strMsg += "Local Message: " + e.getLocalizedMessage() + "\n";
										strMsg += "Stack Trace: " + e.getStackTrace().toString();;
										log.info(strMsg);
										errorList.add(strMsg);
									}
									if (foundFlag) {
										//Reset the found flag
										foundFlag = false;
										ASNHEADERS.each {
											if (!foundFlag) {
												//SC
												def String courier = it.Attributes.TblEntityID.find {it.Qualifier == "SC"}.EntityValue.text();
												if (!courier.isEmpty()) {
													foundFlag = true;
													carrier = courier;
													shipment.setProperty("courier", courier);
												}
											}
										}
										if (!foundFlag) {
											strMsg = "Courier cannot be found in ASN XML File(" + page.getName() + ")";
											log.info(strMsg);
											errorList.add(strMsg);
										}

										//Reset the found flag
										foundFlag = false;
										ASNHEADERS.each {
											if (!foundFlag) {
												//_PRO
												def String WB = it.Attributes.TblReferenceNbr.find {it.Qualifier == "_PRO"}.ReferenceNbr.text();
												if (!WB.isEmpty()) {
													foundFlag = true;
													waybill = WB;
												}
											}
										}
										if (!foundFlag) {
											strMsg = "Waybill cannot be found in ASN XML File(" + page.getName() + ")";
											log.info(strMsg);
											errorList.add(strMsg);
										}
										if (foundFlag) {
											if (!order.containsShipmentByWaybill(waybill)) {
												def SUBHEADERS = it.depthFirst().grep {
													it.name() == 'ASNHeader';
												}
												log.info("Found SUBHEADERS: " + SUBHEADERS.size().toString());
	
												//Reset the found flag
												foundFlag = false;
												SUBHEADERS.each {
													
													def String QS = it.Attributes.TblAmount.find {it.Qualifier == "QS"}.Amount.text();
													if (!QS.isEmpty()) {
														quantityShipped = QS;
													}
													def String DS = it.Attributes.TblDate.find {it.Qualifier == "004"}.DateValue.text();
													if (!DS.isEmpty()) {
														def newDate = parseDate(DS);
														dateShipped = newDate;
													}
	
													def String vendorCode = it.Attributes.TblReferenceNbr.find {it.Qualifier == "VN"}.ReferenceNbr.text();
													if (!vendorCode.isEmpty()) {
														foundFlag = true;
														Data product = media.searchForProductBySku("manufacturersku", vendorCode);
														if (product != null) {
															foundFlag = true;
															productID = product.getId();
														} else {
															strMsg = "Product(" + vendorCode + ") was not found from ASN.";
															log.info(strMsg);
															errorList.add(strMsg);
														}
														if (!foundFlag) {
															strMsg = "Product cannot be found in ASN XML File(" + page.getName() + ")";
															log.info(strMsg);
															errorList.add(strMsg);
														}
													}
	
													if ((foundFlag) && (errorList.size() == 0)) {
		
														Product target = media.getProductSearcher().searchById(productID);
														InventoryItem productInventory = target.getInventoryItem(0);
														CartItem item = order.getItem(productInventory.getSku());
		
														if (!shipment.containsEntryForSku(productInventory.getSku()) && item !=  null ) {
															ShipmentEntry entry = new ShipmentEntry();
															entry.setCartItem(item);
															entry.setQuantity(Integer.parseInt(quantityShipped));
															shipment.setProperty("waybill", waybill);
															shipment.setProperty("shipdate", DateStorageUtil.getStorageUtil().formatForStorage(dateShipped));
															shipment.addEntry(entry);
															foundFlag = true;
	
															strMsg = "Order Updated(" + purchaseOrder + ") and saved";
															log.info(strMsg);
															completeList.add(strMsg);
															
															strMsg = "Waybill (" + waybill + ")";
															log.info(strMsg);
															completeList.add(strMsg);
															
															strMsg = "SKU (" + productInventory.getSku() + ")";
															log.info(strMsg);
															completeList.add(strMsg);
														} else {
															strMsg = "Cart Item (" + productID + ") cannot be found(" + orderID + ")";
															log.info(strMsg);
															errorList.add(strMsg);
														} // end if orderitems
													}
												} // END SUB-HEADERS
												if(shipment.getShipmentEntries().size() >0) {
													order.addShipment(shipment);
													order.getOrderStatus();
													if(order.isFullyShipped()){
														order.setProperty("shippingstatus", "shipped");
														strMsg = "Order status(" + purchaseOrder + ") set to shipped.";
														log.info(strMsg);
														completeList.add(strMsg);
													}else{
														order.setProperty("shippingstatus", "partialshipped");
														strMsg = "Order status(" + purchaseOrder + ") set to partially shipped.";
														log.info(strMsg);
														completeList.add(strMsg);
													}
													store.getOrderArchive().saveOrder(store, order);
													strMsg = "Order (" + purchaseOrder + ") saved.";
													log.info(strMsg);
													completeList.add(strMsg);
												}
											} else {
												strMsg = "Waybill entry already exists (" + waybill + ")."
												log.info(strMsg);
												strMsg = "Skipping ASN Entry."
												log.info(strMsg);
											}
										}
									}
								}
							}
							if (!poFlagFound) {
								strMsg = "PO could not be found in ASN";
								log.info(strMsg);
								errorList.add(strMsg);
							}
						}
					} else {
						strMsg = realpath + " does not exist!";
						log.info(strMsg);
						errorList.add(strMsg);
					}
					if (errorList.size() == 0) {
						if (movePageToProcessed(pageManager, page, media.getCatalogid(), true)) {
							strMsg = "COMPLETE: ASN File (" + page.getName() + ") moved to processed";
							log.info(strMsg);
							completeList.add(strMsg);
						} else {
							strMsg = "ERROR: ASN File(" + page.getName() + ") failed to move";
							log.info(strMsg);
							errorList.add(strMsg);
						}
					} else {
						strMsg = "ERROR: ASN File(" + page.getName() + ":" + purchaseOrder + ") failed in processing.";
						log.info(strMsg);
						errorList.add(strMsg);
						if (movePageToProcessed(pageManager, page, media.getCatalogid(), false)) {
							strMsg = "ASN file(" + page.getName() + ") moved to Error Processed";
							log.info(strMsg);
							errorList.add(strMsg);
						} else {
							strMsg = "ASN file(" + page.getName() + ") failed to move to error";
							log.info(strMsg);
							errorList.add(strMsg);
						}
					}
					if (completeList.size() > 0) {
						for (String complete : completeList) {
							ediXMLresult.appendCompleteMessage("<LI>" + complete + "</LI>\n");
						}
					}
					if (errorList.size() > 0) {
						String foundErrors = "";
						for (String error : errorList) {
							ediXMLresult.appendErrorMessage(error);
							foundErrors += error + "\n";
						}
						log.info("ERROR: The folowwing errors occurred:");
						log.info(foundErrors);
						//Create web event to send an email.
					}
					errorList = null;
					completeList = null;
				}
				if (iterCounter == 0) {
					String inMsg = "There are no files to process at this time.";
					log.info(inMsg);
					ediXMLresult.setCompleteMessage(inMsg);
					ediXMLresult.setComplete(true);
				} else {
					ediXMLresult.setComplete(true);
				}
			} else {
				String inMsg = "There are no files to process at this time.";
				log.info(inMsg);
				ediXMLresult.setCompleteMessage(inMsg);
				ediXMLresult.setComplete(true);
			}
		} else {
			ediXMLresult.setErrorMessage("ERROR: An error was found.");
			ediXMLresult.setComplete(false);
		}
		return ediXMLresult;
	}

	private Date parseDate(String date)
	{
		SimpleDateFormat inFormat = new SimpleDateFormat("yyyyMMdd");
		SimpleDateFormat newFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date oldDate = inFormat.parse(date);
		date = newFormat.format(oldDate);
		return (Date)newFormat.parse(date);
	}

	private boolean movePageToProcessed(PageManager pageManager, Page page,
	String catalogid, boolean saved) {
		boolean movePageResult = false;

		String processedFolder = "/WEB-INF/data/${catalogid}/processed/asn/";
		String asnFolder = "/WEB-INF/data/${catalogid}/incoming/asn/";
		if (!saved) {
			processedFolder += "errors/";
		}

		String destinationFile = processedFolder + page.getName();
		Page destination = pageManager.getPage(destinationFile);
		pageManager.movePage(page, destination);
		if (destination.exists()) {
			movePageResult = true;
		}
		return movePageResult;
	}

	private boolean checkQuantities(String inQuantity, String inQuantityShipped) {
		boolean equal = false;

		int quantity = Integer.parseInt(inQuantity);
		int quantityShipped = Integer.parseInt(inQuantityShipped);
		if (quantity == quantityShipped) {
			equal = true;
		}
		return equal;
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

	log.info("---- START Import EDI ASN ----");
	result = ImportEDIASN.importEdiXml();
	if (result.isComplete()) {
		//Output value to CSV file!
		log.info("Return is complete.");
		context.putPageValue("export", result.getCompleteMessage());
		if (result.isError()) {
			String errMsg = result.getErrorMessage();
			if (errMsg.length() > 0) {
				log.info("ErrorOut: " + errMsg);
				context.putPageValue("errorout", errMsg);
			}
		}
	} else {
		if (result.isError()) {
			String errMsg = result.getErrorMessage();
			log.info("ErrorOut: " + errMsg);
			context.putPageValue("errorout", errMsg);
		}
	}
	log.info("---- END Import EDI ASN ----");
}
finally {
	logs.stopCapture();
}
