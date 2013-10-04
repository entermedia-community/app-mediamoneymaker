package shipping

import java.text.SimpleDateFormat

import org.entermedia.email.PostMail
import org.entermedia.email.TemplateWebEmail
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.store.CartItem
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

public class ImportASN extends EnterMediaObject {
	private static final String LOG_HEADER = " - RECORDS - ";
	private static final String FOUND = "Found"
	private static final String NOT_FOUND = "NOT FOUND!";
	private static String strMsg = "";
	private static String MANUFACTURER_SEARCH_FIELD = "manufacturersku";

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
	private Page page;

	public void setOrder( Order inOrder ) {
		order = inOrder;
	}
	public Order getOrder() {
		return order;
	}
	
	private void setPage( Page inPage ) {
		page = inPage;
	}
	public Page getPage() {
		return page;
	}

	public void doImport() {
		MediaUtilities media = new MediaUtilities();
		media.setContext(context);

		boolean foundData = false;
		boolean foundFlag = false;

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

		WebPageRequest inReq = context;
		MediaArchive archive = inReq.getPageValue("mediaarchive");
		SearcherManager manager = archive.getSearcherManager();

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

					Page page = pageManager.getPage(iterator.next());
					setPage(page);
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

						String packLevel = "";

						Shipment shipment = new Shipment();
						CartItem cartItem = null;
						Date dateShipped = null;

						Product product = null;

						//Create the XMLSlurper Object
						def ASN = new XmlSlurper().parse(page.getReader());

						//Get the distributor
						def String GSSND = ASN.Attributes.TblReferenceNbr.find {it.Qualifier == "GSSND"}.ReferenceNbr.text();
						Data distributor = media.searchForDistributor(GSSND, production);
						if (distributor != null) {
							distributorID = distributor.getId();
						} else {
							strMsg = "Distributor value is blank in ASN.";
							log.info(strMsg);
						}
						def ASNGROUPS = ASN.depthFirst().grep{
							it.name() == 'ASNGroup';
						}
						log.info("Found ASNGRoups: " + ASNGROUPS.size().toString());
						ASNGROUPS.each {
							def ASNHEADER = ASN.depthFirst().grep{
								it.name() == 'ASNHeader';
							}
							log.info("Found ASNHEADERS: " + ASNHEADER.size().toString());
							ASNHEADER.each {
								packLevel = it.PackLevel.text();
								log.info("PackLevel: " + packLevel);
								if (packLevel.equalsIgnoreCase("1")) {
									def String courier = it.Attributes.TblEntityID.find {it.Qualifier == "SC"}.EntityValue.text();
									if (!courier.isEmpty()) {
										carrier = courier;
									}
									def String WB = it.Attributes.TblReferenceNbr.find {it.Qualifier == "_PRO"}.ReferenceNbr.text();
									if (!WB.isEmpty()) {
										waybill = WB;
										if (carrier.isEmpty()) {
											if (waybill.substring(0, 1).equalsIgnoreCase("1Z")) {
												carrier = "UNITED PARCEL POST";
											} else {
												carrier = "NOT PROVIDED";
											}
										}
									}
								} // END packLevel = 1

								if (packLevel.equalsIgnoreCase("2")) {
									def String PO = it.Attributes.TblReferenceNbr.find {it.Qualifier == "PO"}.ReferenceNbr.text();
									purchaseOrder = PO;
									log.info("Purchase Order: " + purchaseOrder);
									Order order = null;
									try {
										order = media.searchForOrder(purchaseOrder);
										if (order != null) {
											orderID = purchaseOrder;
											setOrder(order);
										} else {
											strMsg = "ERROR: Order(" + purchaseOrder + ") was not found from ASN.";
											log.info(strMsg);
										}
									}
									catch (Exception e) {
										strMsg = "Invalid Order: " + purchaseOrder + "\n";
										strMsg += "Exception thrown:\n";
										strMsg += "Local Message: " + e.getLocalizedMessage() + "\n";
										strMsg += "Stack Trace: " + e.getStackTrace().toString();;
										log.info(strMsg);
									}
									//Get Shipped Date
									String DS = it.Attributes.TblDate.find {it.Qualifier == "004"}.DateValue.text();
									Date newDate = parseDate(DS);
									dateShipped = newDate;
									log.info("Date Shipped: " + dateShipped.toString());
								} // END packLevel = 2

								if (packLevel.equalsIgnoreCase("4")) {
									foundFlag = false;
									def String QS = it.Attributes.TblAmount.find {it.Qualifier == "QS"}.Amount.text();
									if (!QS.isEmpty()) {
										quantityShipped = QS;
										log.info("Quantity Shipped: " + quantityShipped);
										foundFlag = true;
									} else {
										log.info("No Quantity Shipped in ASK");
										foundFlag = false;
									}
									if (foundFlag) {
										foundFlag = false;
										def String vendorCode = it.Attributes.TblReferenceNbr.find {it.Qualifier == "VN"}.ReferenceNbr.text();
										if (!vendorCode.isEmpty()) {
											log.info("VendorCode: " + vendorCode);
											foundFlag = true;
											Searcher productSearcher = store.getProductSearcher();
											Data vendorProduct = productSearcher.searchByField(MANUFACTURER_SEARCH_FIELD, vendorCode);
											if (vendorProduct != null) {
												foundFlag = true;
												productID = vendorProduct.getId();
												log.info("ProductID: " + productID);
											} else {
												strMsg = "Product(" + vendorCode + ") was not found from ASN.";
												log.info(strMsg);
											}
											if (!foundFlag) {
												strMsg = "Product cannot be found in ASN XML File(" + page.getName() + ")";
												log.info(strMsg);
											}
										}
										if (foundFlag) {
											Searcher productsearcher = store.getProductSearcher();
											CartItem item = order.getCartItemByProductID(productID);
											product = item.getProduct();

											if (!order.containsShipmentByWaybill(waybill)) {
												if (!shipment.containsEntryForSku(item.getSku()) && item !=  null ) {
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

													strMsg = "Waybill:Courier (" + waybill + ":" + carrier + ")";
													log.info(strMsg);

													strMsg = "SKU (" + vendorCode + ":" + item.getSku() + ")";
													log.info(strMsg);

													if(shipment.getShipmentEntries().size() >0) {
														order.addShipment(shipment);
														//reset FoundFlag
													}
												} else {
													strMsg = "Cart Item (" + productID + ") cannot be found(" + orderID + ")";
													log.info(strMsg);
												} // end if orderitems
											} else {
												// If waybill exists, check quantity in shipment compared to the order
												int totalShipped = 0;
												ArrayList<Shipment> shipments = order.getShipments();
												for (Shipment eShipment in shipments) {
													ArrayList<ShipmentEntry> entries = eShipment.getShipmentEntries();
													for (ShipmentEntry eEntry in entries) {
														if(eEntry.getItem().getSku() == item.getSku()) {
															totalShipped += eEntry.getQuantity();
														}
													}
												}
												int cartQty = item.getQuantity();
												int qtyShipped = Integer.parseInt(quantityShipped);
												if (qtyShipped <= (cartQty - totalShipped)) {
													foundFlag = true;
													ShipmentEntry entry = new ShipmentEntry();
													entry.setCartItem(item);
													entry.setQuantity(qtyShipped);
													shipment.setProperty("courier", carrier);
													shipment.setProperty("waybill", waybill);
													shipment.setProperty("distributor", distributorID);
													shipment.setProperty("shipdate", DateStorageUtil.getStorageUtil().formatForStorage(dateShipped));
													shipment.addEntry(entry);

													strMsg = "Order Updated(" + purchaseOrder + ") and saved";
													log.info(strMsg);

													strMsg = "Waybill:Courier (" + waybill + ":" + carrier + ")";
													log.info(strMsg);

													strMsg = "SKU (" + vendorCode + ")";
													log.info(strMsg);

													if(shipment.getShipmentEntries().size() >0) {
														if (!order.getShipments().contains(shipment)) {
															order.addShipment(shipment);
														}
														order.getOrderStatus();
														if(order.isFullyShipped()){
															order.setProperty("shippingstatus", "shipped");
															strMsg = "Order status(" + purchaseOrder + ") set to shipped.";
															log.info(strMsg);
														}else{
															order.setProperty("shippingstatus", "partialshipped");
															strMsg = "Order status(" + purchaseOrder + ") set to partially shipped.";
															log.info(strMsg);
														}
													}
												}
											} // END CONTAINS WAYBILL
										} else {
											strMsg = "ERROR: ASN File(" + page.getName() + ") failed!";
											log.info(strMsg);
										} /* END foundFlag */
									} else {
										strMsg = "ERROR: ASN File(" + page.getName() + ") failed!";
										log.info(strMsg);
									} /* END foundFlag */
								} /* END packLevel == 4 */
							} /* END ASNHEADER */

							//Finished processing the GROUP Save the order
							if (foundFlag) {
								order.getOrderStatus();
								if(order.isFullyShipped()){
									order.setProperty("shippingstatus", "shipped");
									strMsg = "Order status(" + purchaseOrder + ") set to shipped.";
									log.info(strMsg);
								}else{
									order.setProperty("shippingstatus", "partialshipped");
									strMsg = "Order status(" + purchaseOrder + ") set to partially shipped.";
									log.info(strMsg);
								}
								store.saveOrder(order);
								strMsg = "Order (" + purchaseOrder + ") saved.";
								log.info(strMsg);
							} /* END FoundFlag */
						} /* end ASNGROUPS */							
					} /* END xmlFile Exists and isFile */
					strMsg = "Processing of ASN XML file complete. Perform post processing.";
					log.info(strMsg);

					if (foundFlag) {
						//sendEmailToCustomer(purchaseOrder, getOrder(), store, media)
						if (movePageToProcessed(pageManager, page, media.getCatalogid(), true)) {
							strMsg = "COMPLETE: ASN File (" + page.getName() + ") moved to processed";
							log.info(strMsg);
						} else {
							strMsg = "ERROR: ASN File(" + page.getName() + ") failed to move";
							log.info(strMsg);
						}
						sendEmailToShippingAdmin(purchaseOrder, getOrder(), store, media, inReq, true);
					} else {
						strMsg = "ERROR: ASN File(" + page.getName() + ":" + purchaseOrder + ") failed in processing.";
						log.info(strMsg);
						if (movePageToProcessed(pageManager, page, media.getCatalogid(), false)) {
							strMsg = "ASN file(" + page.getName() + ") moved to Error Processed";
							log.info(strMsg);
						} else {
							strMsg = "ASN file(" + page.getName() + ") failed to move to error";
							log.info(strMsg);
						}
						sendEmailToShippingAdmin(purchaseOrder, getOrder(), store, media, inReq, false);
					}
				} /* END dirList.iterator() */
			} /* END dirList.size() */
		} /* END FoundData */
	} /* END doImport */

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

		String subject = "Wirelessarea.ca Order Shipping Notification - Order #: " + orderNumber;
		String templatePage = "/ecommerce/views/modules/storeOrder/workflow/order-shipping-notification.html";
		log.info("Sending email to emaillist: " + subject);
		sendEmail(media.getArchive(), context, emaillist, templatePage, subject);

	}
	protected void sendEmailToShippingAdmin(String orderNumber, Order order, 
		Store store, MediaUtilities media, WebPageRequest inReq, boolean success) {

		ArrayList<Shipment> shipments = order.getShipments();

		inReq.putPageValue("orderid", order.getId());
		inReq.putPageValue("ordernumber", orderNumber);
		inReq.putPageValue("order", order);
		inReq.putPageValue("customer", order.getCustomer());
		inReq.putPageValue("shipments", shipments);
		inReq.putPageValue("xmlfile", getPage().getName());
		
		MediaArchive archive = inReq.getPageValue("mediaarchive");
		Searcher userprofilesearcher = archive.getSearcher("userprofile");
		ArrayList emaillist = new ArrayList();
		HitTracker results = userprofilesearcher.fieldSearch("shippingadmin", "true");
		if (results.size() > 0) {
			String templatePage = "";
			String subject = "";
			for(Iterator detail = results.iterator(); detail.hasNext();) {
				Data userInfo = (Data)detail.next();
				emaillist.add(userInfo.get("email"));
			}
			if (success) {
				templatePage = "/ecommerce/views/modules/storeOrder/workflow/order-shipping-notification.html";
				subject = "Wirelessarea.ca Order Shipping Notification - Order #: " + order.getOrderNumber();
			} else {
				templatePage = "/ecommerce/views/modules/storeOrder/workflow/asn-error-notification.html";
				subject = "ERROR Processing ASN XML File";
			}
			log.info("Sending email to emaillist: " + subject);
			sendEmail(media.getArchive(), context, emaillist, templatePage, subject);
		}
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

	ImportASN ImportEDIASN = new ImportASN();
	ImportEDIASN.setLog(log);
	ImportEDIASN.setContext(context);
	ImportEDIASN.setPageManager(pageManager);

	log.info("---- START Import EDI ASN ----");
	ImportEDIASN.doImport();
	log.info("---- END Import EDI ASN ----");
}
finally {
	log.stopCapture();
}
