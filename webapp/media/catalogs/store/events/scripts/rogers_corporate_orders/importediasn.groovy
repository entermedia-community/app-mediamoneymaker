package rogers_corporate_orders;

import java.text.SimpleDateFormat

import org.openedit.Data
import org.openedit.entermedia.publishing.PublishResult
import org.openedit.event.WebEvent
import org.openedit.store.util.MediaUtilities;
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

	public PublishResult importEdiXml() {

		PublishResult result = new PublishResult();
		result.setErrorMessage("");
		String foundErrors = "";
		result.setComplete(false);

		MediaUtilities media = new MediaUtilities();
		media.setContext(context);

		log.info("---- START Import EDI ASN ----");
		
		boolean production = Boolean.parseBoolean(context.findValue('productionmode'));

		String asnFolder = "/WEB-INF/data/" + media.getCatalogid() + "/incoming/asn/";
		PageManager pageManager = media.getArchive().getPageManager();
		List dirList = pageManager.getChildrenPaths(asnFolder);
		log.info("Initial directory size: " + dirList.size().toString());

		if (dirList.size() > 0) {
			def int iterCounter = 0;
			for (Iterator iterator = dirList.iterator(); iterator.hasNext();) {
				
				Page page = pageManager.getPage(iterator.next());
				log.info("Processing " + page.getName());

				String realpath = page.getContentItem().getAbsolutePath();

				File xmlFIle = new File(realpath);
				if (xmlFIle.exists()) {
					
					String orderID = "";
					String purchaseOrder = "";
					String distributorID = "";
					String storeNumber = "";
					String carrier = "";
					String waybill = "";
					String quantityShipped = "";
					String productID = "";
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
							def String PO = it.Attributes.TblReferenceNbr.find {it.Qualifier == "PO"}.ReferenceNbr.text();
							log.info("PO: " + PO);
							if (!PO.isEmpty()) {
								String[] orderInfo = PO.split("-");
								Data order = media.searchForOrder(orderInfo[0]);
								foundFlag = true;
								if (order != null) {
									orderID = orderInfo[0];
								} else {
									log.info("ERROR: Order(" + orderInfo[0] + ") was not found from ASN.");
									//Create web event to send an email.
									String inMsg = "Order(" + orderInfo[0] + ") was not found from ASN.";
									log.info(inMsg);
									result.setErrorMessage(result.getErrorMessage() + "\n" + inMsg);
								}
								HitTracker foundStore = media.searchForStoreInOrder(orderID, orderInfo[1]);
								if (foundStore != null) {
									purchaseOrder = PO;
									storeNumber = orderInfo[1];
								} else {
									log.info("ERROR: Store(" + orderInfo[1] + ") was not found from ASN.");
									log.info("ERROR: PurchaseOrder: " + PO);
									//Create web event to send an email.
									String inMsg = "Purchase Order(" + orderInfo[1] + ") was not found from ASN.";
									log.info(inMsg);
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
											carrier = courier;
										}
									}
								}
								if (!foundFlag) {
									String inMsg = "Courier cannot be found in ASN XML File(" + page.getName() + ")";
									result.setErrorMessage(result.getErrorMessage() + "\n" + inMsg);
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
											def String QS = it.Attributes.TblAmount.find {it.Qualifier == "QS"}.Amount.text();
											if (!QS.isEmpty()) {
												foundFlag = true;
												quantityShipped = QS;
											}
										}
									}
									if (!foundFlag) {
										String inMsg = "Quantity Shipped cannot be found in ASN XML File(" + page.getName() + ")";
										result.setErrorMessage(result.getErrorMessage() + "\n" + inMsg);
									}

									//Reset the found flag
									foundFlag = false;
									SUBHEADERS.each {
										if (!foundFlag) {
											//QS
											def String DS = it.Attributes.TblDate.find {it.Qualifier == "004"}.DateValue.text();
											if (!DS.isEmpty()) {
												foundFlag = true;
												def newDate = parseDate(DS);
												dateShipped = newDate;
											}
										}
									}
									if (!foundFlag) {
										String inMsg = "Date Shipped cannot be found in ASN XML File(" + page.getName() + ")";
										result.setErrorMessage(result.getErrorMessage() + "\n" + inMsg);
									}

									//Reset the found flag
									foundFlag = false;
									SUBHEADERS.each {
										if (!foundFlag) {
											//VN
											def String vendorCode = it.Attributes.TblReferenceNbr.find {it.Qualifier == "VN"}.ReferenceNbr.text();
											if (!vendorCode.isEmpty()) {
												foundFlag = true;
												Data product = media.searchForProductbyRogersSKU(vendorCode);
												if (product != null) {
													foundFlag = true;
													productID = product.getId();
												} else {
													log.info("Product(" + vendorCode + ") was not found from ASN.");
													//Create web event to send an email.
													String inMsg = "Product(" + vendorCode + ") was not found from ASN.";
													result.setErrorMessage(result.getErrorMessage() + "\n" + inMsg);
												}
											}
										}
									}
									if (!foundFlag) {
										String inMsg = "Product cannot be found in ASN XML File(" + page.getName() + ")";
										result.setErrorMessage(result.getErrorMessage() + "\n" + inMsg);
										log.info(inMsg);
									}

									//Finally write the data to the Tables!
									String errMsg = "";
									if (result.getErrorMessage() != null) {
										errMsg = result.getErrorMessage();
									}
									if ((foundFlag) && (errMsg.length() == 0)) {
										
//										Product product = media.getProductSearcher().searchById(productID);
//										InventoryItem productInventory = product.getInventoryItem(0);
//										productInventory.setQuantityInStock(Integer.parseInt(quantityShipped));
//										media.getProductSearcher().saveData(product, context.getUser());
																				
										SearchQuery itemQuery = media.getItemSearcher().createSearchQuery();
										itemQuery.addExact("store", storeNumber);
										itemQuery.addExact("rogers_order", orderID);
										itemQuery.addExact("product", productID);
										HitTracker orderitems = media.getItemSearcher().search(itemQuery);
										if (orderitems.size() > 0 ) {
											Data orderitem = media.getItemSearcher().searchById(orderitems.get(0).getId());
											orderitem.setProperty("quantityshipped", quantityShipped);
											boolean check = checkQuantities(orderitem.get("quantity"), quantityShipped);
											if (check) {
												orderitem.setProperty("orderstatus", "completed");
												orderitem.setProperty("deliverystatus", "shipped");
											} else {
												orderitem.setProperty("orderstatus", "pending");
												orderitem.setProperty("deliverystatus", "partialshipped");
											}
											orderitem.setProperty("carrier", carrier);
											orderitem.setProperty("waybill", waybill);
											orderitem.setProperty("shipdate", DateStorageUtil.getStorageUtil().formatForStorage(dateShipped));
											media.getItemSearcher().saveData(orderitem, context.getUser());
										} else {
											String inMsg = "Order cannot be found(" + orderID + ")";
											result.setErrorMessage(result.getErrorMessage() + "\n" + inMsg);
											log.info(inMsg);
										} // end if orderitems
									} else {
										log.info("ERROR ORDER(" + purchaseOrder + ") NOT SAVED");
									}
								} else {
									String inMsg = "Purchase Order cannot be found in ASN XML File(" + page.getName() + ")";
									log.info(inMsg);
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
						result = movePageToProcessed(pageManager, page, media.getCatalogid(), true);
						if (result.complete) {
							String inMsg = "ASN FILE(" + page.getName() + ") MOVED";
							log.info(inMsg);
						} else {
							String inMsg = "ASN FILE(" + page.getName() + ") FAILED MOVE";
							log.info(inMsg);
						}
					} else {
						String inMsg = "ERROR ORDER(" + orderID + ") NOT SAVED";
						log.info(inMsg);
						result = movePageToProcessed(pageManager, page, media.getCatalogid(), false);
						if (result.complete) {
							inMsg = "ASN FILE(" + page.getName() + ") MOVED TO ERROR";
							log.info(inMsg);
						} else {
							inMsg = "ASN FILE(" + page.getName() + ") FAILED MOVE TO ERROR";
							log.info(inMsg);
						}
					}
				} else {
					result.setErrorMessage(realpath + " does not exist!");
				}
			}
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
				event.setCatalogId(media.getCatalogid());
				event.setProperty("error", result.getErrorMessage());
				media.getArchive().getMediaEventHandler().eventFired(event);
			}
		} else {
			result.setCompleteMessage("There are no files to process at this time.");
			result.setComplete(true);
		}
		return result;
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
