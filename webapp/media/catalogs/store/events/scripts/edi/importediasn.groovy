package edi;

import java.text.SimpleDateFormat

import org.entermedia.email.PostMail
import org.entermedia.email.TemplateWebEmail
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.store.CartItem
import org.openedit.store.InventoryItem
import org.openedit.store.Product
import org.openedit.store.Store
import org.openedit.store.customer.Customer
import org.openedit.store.orders.Order
import org.openedit.store.orders.Shipment
import org.openedit.store.orders.ShipmentEntry
import org.openedit.store.util.MediaUtilities
import org.openedit.util.DateStorageUtil

import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker
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
	private Order order;
	
	public void setOrder( Order inOrder ) {
		order = inOrder;
	}
	public Order getOrder() {
		return order;
	}

	public void importEdiXml() {

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
			strMsg = "ERROR: Could not load store.\n";
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
											orderID = purchaseOrder;
											setOrder(order);
										} else {
											strMsg = "ERROR: Order(" + purchaseOrder + ") was not found from ASN.";
											log.info(strMsg);
											errorList.add(strMsg);
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
													log.info("Quantity Shipped: " + quantityShipped);
												}
												def String DS = it.Attributes.TblDate.find {it.Qualifier == "004"}.DateValue.text();
												if (!DS.isEmpty()) {
													def newDate = parseDate(DS);
													dateShipped = newDate;
													log.info("Date Shipped: " + dateShipped.toString());
												}

												def String vendorCode = it.Attributes.TblReferenceNbr.find {it.Qualifier == "VN"}.ReferenceNbr.text();
												if (!vendorCode.isEmpty()) {
													foundFlag = true;
													Data product = media.searchForProductByField("manufacturersku", vendorCode);
													if (product != null) {
														foundFlag = true;
														productID = product.getId();
														log.info("ProductID: " + productID);
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
	
													if (!order.containsShipmentByWaybill(waybill)) {
														if (!shipment.containsEntryForSku(productInventory.getSku()) && item !=  null ) {
															ShipmentEntry entry = new ShipmentEntry();
															entry.setCartItem(item);
															entry.setQuantity(Integer.parseInt(quantityShipped));
															shipment.setProperty("courier", carrier);
															shipment.setProperty("waybill", waybill);
															shipment.setProperty("distributor", distributorID);
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
																
																//reset FoundFlag
																foundFlag = false;
															}
														} else {
															strMsg = "Cart Item (" + productID + ") cannot be found(" + orderID + ")";
															log.info(strMsg);
															errorList.add(strMsg);
														} // end if orderitems
													} else {
														// If waybill exists, check quantity in shipment compared to the order
														int totalShipped = 0;
														ArrayList<Shipment> shipments = order.getShipments();
														for (Shipment eShipment in shipments) {
															ArrayList<ShipmentEntry> entries = eShipment.getShipmentEntries();
															for (ShipmentEntry eEntry in entries) {
																if(eEntry.getItem().getSku() == productInventory.getSku()) {
																	totalShipped += eEntry.getQuantity();
																}
															}
														}
														int cartQty = item.getQuantity();
														int qtyShipped = Integer.parseInt(quantityShipped);
														if (qtyShipped <= (cartQty - totalShipped)) {
															ShipmentEntry entry = new ShipmentEntry();
															entry.setCartItem(item);
															entry.setQuantity(qtyShipped);
															shipment.setProperty("courier", carrier);
															shipment.setProperty("waybill", waybill);
															shipment.setProperty("distributor", distributorID);
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
															
															if(shipment.getShipmentEntries().size() >0) {
																if (!order.getShipments().contains(shipment)) {
																	order.addShipment(shipment);
																}
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
																
																//reset FoundFlag
																foundFlag = false;
															}
														}
													} // END CONTAINS WAYBILL
												} // END FOUND FLAG
											} // END SUB-HEADERS
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
					
					strMsg = "Processing of ASN XML file complete. Perform post processing.";
					log.info(strMsg);
					
					if (errorList.size() == 0) {
						sendEmailToCustomer(purchaseOrder, getOrder(), store, media)
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
							log.info(complete);
						}
					}
					if (errorList.size() > 0) {
						String foundErrors = "";
						for (String error : errorList) {
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
				}
			} else {
				String inMsg = "There are no files to process at this time.";
				log.info(inMsg);
			}
		}
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
	
	protected void sendEmailToCustomer(String orderNumber, Order order, Store store, MediaUtilities media ) {
		
		Customer customer = order.getCustomer();
		ArrayList<Shipment> shipments = order.getShipments();

		context.putPageValue("orderid", order.getId());
		context.putPageValue("ordernumber", orderNumber);
		context.putPageValue("order", order);
		context.putPageValue("customer", customer);
		context.putPageValue("shipments", shipments);

		ArrayList emaillist = new ArrayList();
		emaillist.add(customer.getEmail());
		
		String subject = "Order Shipping Notification";
		String templatePage = "/ecommerce/views/modules/storeOrder/workflow/order-shipping-notification.html";
		log.info("Sending email to emaillist: " + subject);
		sendEmail(media.getArchive(), context, emaillist, templatePage, subject);

	}
	protected void sendEmail(MediaArchive archive, WebPageRequest context, List inEmailList, String templatePage, String inSubject){
		PageManager pageManager = archive.getPageManager();
		Page template = pageManager.getPage(templatePage);
		WebPageRequest newcontext = context.copy(template);
		TemplateWebEmail mailer = getMail(archive);
		mailer.loadSettings(newcontext);
		mailer.setMailTemplatePath(templatePage);
		mailer.setFrom("info@wirelessarea.ca");
		mailer.setRecipientsFromStrings(inEmailList);
		mailer.setSubject(inSubject);
		mailer.send();
	}
	
	protected TemplateWebEmail getMail(MediaArchive archive) {
		PostMail mail = (PostMail)archive.getModuleManager().getBean( "postMail");
		return mail.getTemplateWebEmail();
	}

}
log = new ScriptLogger();
log.startCapture();

try {

	ImportEDIASN ImportEDIASN = new ImportEDIASN();
	ImportEDIASN.setLog(log);
	ImportEDIASN.setContext(context);
	ImportEDIASN.setPageManager(pageManager);

	log.info("---- START Import EDI ASN ----");
	ImportEDIASN.importEdiXml();
	log.info("---- END Import EDI ASN ----");
}
finally {
	log.stopCapture();
}
