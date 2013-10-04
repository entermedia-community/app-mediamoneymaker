package invoices

import java.text.SimpleDateFormat

import org.entermedia.email.PostMail
import org.entermedia.email.TemplateWebEmail
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.money.Money
import org.openedit.store.CartItem
import org.openedit.store.InventoryItem
import org.openedit.store.Product
import org.openedit.store.Store
import org.openedit.store.orders.Order
import org.openedit.store.orders.Shipment
import org.openedit.store.orders.ShipmentEntry
import org.openedit.store.util.MediaUtilities
import org.openedit.util.DateStorageUtil

import com.openedit.OpenEditException
import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.Page
import com.openedit.page.manage.PageManager

public class EdiCesiumImport extends EnterMediaObject {

	private static final String LOG_HEADER = " - RECORDS - ";
	private static final String FOUND = "Found"
	private static final String NOT_FOUND = "NOT FOUND!";
	private static final String ADDED = "Added";
	private static final String CESIUM_ID = "105";
	private static String strMsg = "";
	private static String MANUFACTURER_SEARCH_FIELD = "manufacturersku";

	public void doImport() {

		MediaUtilities media = new MediaUtilities();
		media.setContext(context);

		WebPageRequest inReq = context;
		MediaArchive archive = inReq.getPageValue("mediaarchive");
		String catalogID = archive.getCatalogId();

		log.info("---- START Import EDI Invoice ----");

		def String SEARCH_FIELD = "";
		//Read the production value
		boolean production = Boolean.parseBoolean(context.findValue('productionmode'));

		Store store = null;
		try {
			store  = media.getContext().getPageValue("store");
			if (store != null) {
				log.info("Store loaded");
			} else {
				strMsg = "ERROR: Could not load store";
				throw new Exception(strMsg);
			}
		}
		catch (Exception e) {
			strMsg += "Exception thrown:\n";
			strMsg += "Local Message: " + e.getLocalizedMessage() + "\n";
			strMsg += "Stack Trace: " + e.getStackTrace().toString();;
			log.info(strMsg);
			throw new OpenEditException(strMsg);
		}
		
		// Load searchers
		SearcherManager manager = archive.getSearcherManager();
		Searcher userprofilesearcher = archive.getSearcher("userprofile");
		Searcher invoiceSearcher = manager.getSearcher(catalogID, "invoice");
		Searcher invoiceItemSearcher = media.getInvoiceItemsSearcher();
		Searcher productSearcher = store.getProductSearcher();
		
		// Get XML File
		//String fileName = "export-" + this.distributorName.replace(" ", "-") + ".csv";
		String invoiceFolder = "/WEB-INF/data/" + media.getCatalogid() + "/incoming/invoices/cesium/";
		PageManager pageManager = media.getArchive().getPageManager();
		List dirList = pageManager.getChildrenPaths(invoiceFolder);
		log.info("Initial directory size: " + dirList.size().toString());

		if (dirList.size() > 0) {

			Data ediInvoice = null;
			Data ediInvoiceItem = null;
			String distributorID = "";
			List<String> processedInvoices = new ArrayList<String>();

			def int iterCounter = 0;
			for (Iterator iterator = dirList.iterator(); iterator.hasNext();) {

				Page page = pageManager.getPage(iterator.next());
				log.info("Processing " + page.getName());

				String realpath = page.getContentItem().getAbsolutePath();

				File xmlFile = new File(realpath);
				if (xmlFile.exists() && xmlFile.isFile()) {
					iterCounter++;

					String orderID = "";
					String productID = "";
					String ediInvoiceItemID = "";
					String invoiceNumber = "";
					String invoiceTotal = "";
					String purchaseOrder = "";
					String taxAmount = "";
					String taxType = "";
					String taxFedID = "";
					String shippingAmount = "";
					String waybill = "";
					String courier = "";

					Money invoiceAmount;
					Boolean checkShipping = true;
					Date newDate = null;

					Order order = null;
					CartItem cartItem = null;
					boolean foundData = false;

					//Create the XMLSlurper Object
					def INVOICE = new XmlSlurper().parse(page.getReader());
					String distributor = INVOICE.Attributes.TblEntityID.find {it.Qualifier == "GSSND"}.EntityValue.text();
					if (!distributor.isEmpty()) {
						//Find the distributor
						Data DISTRIB = media.searchForDistributor(distributor, production);
						if (DISTRIB != null) {
							distributorID = DISTRIB.getId();
							log.info("Distributor Found: " + distributor);
							log.info("Distributor ID: " + distributorID);
							foundData = true;
							if (distributorID.equals(CESIUM_ID)) {
								foundData = true;
							} else {
								String inMsg = "ERROR: Cannot process NON Cesium Invoice from this folder.";
								log.info(inMsg);
								return;
							}
						} else {
							String inMsg = "ERROR: Distributor cannot be found.";
							log.info(inMsg);
							foundData = false;
						}
					} else {
						String inMsg = "ERROR: Distributor cannot be found in INVOICE.";
						log.info(inMsg);
						foundData = false;
					} /* END !distributor.isEmpty() */
					if (foundData) {
						def INVOICEGROUPS = INVOICE.depthFirst().grep{
							it.name() == 'InvoiceGroup';
						}
						log.info("Found InvoiceGroups: " + INVOICEGROUPS.size().toString());
						INVOICEGROUPS.each {

							def INVOICEHEADERS = it.depthFirst().grep{
								it.name() == 'InvoiceHeader';
							}
							log.info("Found InvoiceHeaders: " + INVOICEHEADERS.size().toString());

							INVOICEHEADERS.each {
								List<Data> invoiceItems = new ArrayList<Data>();
								Shipment shipment = new Shipment();

								foundData = true;

								if (foundData) {
									foundData = false;
									//Get the INVOICENUMBER details
									invoiceNumber = it.InvoiceNumber.text();
									if (!invoiceNumber.isEmpty()) {
										log.info("Processing Invoice Number: " + invoiceNumber);
										foundData = true;
									} else {
										String inMsg = "ERROR: Invoice Number value is blank in Invoice.";
										log.info(inMsg);
										foundData = false;
									}
								}
								if (!foundData) {
									String inMsg = "ERROR: Invoice Number value was not found in INVOICE.";
									log.info(inMsg);
								}
								if (foundData) {
									foundData = false;
									//Get the PO details
									def String PO = it.Attributes.TblReferenceNbr.find {it.Qualifier == "PO"}.ReferenceNbr.text();
									if (!PO.isEmpty()) {
										purchaseOrder = PO;
										log.info("Purchase Order: " + purchaseOrder);
										try {
											// Load the order
											order = media.searchForOrder(purchaseOrder);
											if (order != null) {
												foundData = true;
												if (order != null) {
													orderID = purchaseOrder;
													strMsg = "OrderID: " + orderID;
													log.info(strMsg);

													//Check to see if the invoice has been created!
													SearchQuery invoiceQuery = invoiceSearcher.createSearchQuery();
													invoiceQuery.addExact("ponumber", purchaseOrder);
													invoiceQuery.addExact("invoicenumber", invoiceNumber);
													HitTracker invoices = media.getInvoiceSearcher().search(invoiceQuery);
													if (invoices.size() == 1) {
														try {
															ediInvoice = media.getInvoiceSearcher().searchById(invoices.get(0).getId());
															if (ediInvoice != null) {
																log.info("Invoice exists: " + ediInvoice.getId());
																ediInvoice.setProperty("distributor", distributorID);
																ediInvoice.setProperty("invoicestatus", "updated");
																foundData = true;
															}
														}
														catch (Exception e) {
															strMsg = "ERROR: Invalid Invoice/PurchaseOrder match." + "\n";
															strMsg += "Invoice Number: " + invoiceNumber + "\n";
															strMsg += "Purchase Order: " + purchaseOrder + "\n";
															log.info(strMsg);
														}
													} else if (invoices.size() == 0) {
														//Create new invoice!
														try {
															ediInvoice = invoiceSearcher.createNewData();
															ediInvoice.setId(invoiceSearcher.nextId());
															ediInvoice.setSourcePath(ediInvoice.getId());
															ediInvoice.setProperty("distributor", distributorID);
															ediInvoice.setProperty("ponumber", purchaseOrder);
															ediInvoice.setProperty("invoicenumber", invoiceNumber);
															ediInvoice.setProperty("invoicestatus", "new");
															foundData = true;
														}
														catch (Exception e) {
															strMsg = "ERROR: Invalid creation of new invoice." + "\n";
															strMsg += "Invoice Number: " + invoiceNumber + "\n";
															strMsg += "Purchase Order: " + purchaseOrder + "\n";
															log.info(strMsg);
														}
													}
												} else {
													strMsg = "ERROR: Order(" + purchaseOrder + ") was not found from ASN.";
													log.info(strMsg);
												}
											}
										}
										catch (Exception e) {
											strMsg = "ERROR: Invalid Order: " + purchaseOrder + "\n";
											strMsg += "Searching for " + purchaseOrder + " retured NULL!" + "\n";
											log.info(strMsg);
										}
									}
								}
								if (foundData) {
									foundData = false;
									//Get the INVOICEAMOUNT details
									invoiceTotal = it.InvoiceAmount.text();
									if (!invoiceTotal.isEmpty()) {
										log.info("Invoice Total: " + invoiceTotal);
										invoiceAmount = new Money(invoiceTotal);
										ediInvoice.setProperty("invoicetotal", invoiceTotal);
										foundData = true;
									}
									if (!foundData) {
										String inMsg = "ERROR: Invoice Total value was not found in INVOICE.";
										log.info(inMsg);
									}
								}
								if (foundData) {
									foundData = false;
									/* This works - This gets the first level store info */
									def String invoiceDate = it.Attributes.TblDate.find {it.Qualifier == "003"}.DateValue.text();
									if (!invoiceDate.isEmpty()) {
										newDate = new SimpleDateFormat("yyyymmdd").parse(invoiceDate);
										ediInvoice.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(newDate));
										log.info("Invoice Date: " + newDate.toString());
										foundData = true;
									}
									if (!foundData) {
										String inMsg = "ERROR: Invvoice Date value was not found in INVOICE.";
										log.info(inMsg);
									}
								}
								if (foundData) {
									foundData = false;
									def String WB = it.Attributes.TblReferenceNbr.find {it.Qualifier == "WY"}.ReferenceNbr.text();
									if (!WB.isEmpty()) {
										waybill = WB;
										log.info("Waybill: " + waybill);
										if (waybill.substring(0, 1).equalsIgnoreCase("1Z")) {
											courier = "UNITED PARCEL POST";
										} else {
											courier = "NOT PROVIDED";
										}
										foundData = true;
									} else {
										strMsg = "ERROR: Wyabill value was not found in INVOICE.";
										log.info(strMsg);
									}
								}
								if (foundData) {
									foundData = false;
									/* This works - This gets the first level store info */
									taxAmount = it.Attributes.TblTax.TaxAmount.text();
									if (!taxAmount.isEmpty()) {
										log.info("Tax Amount: " + taxAmount);
										foundData = true;
										taxType = it.Attributes.TblTax.TaxType.text();
										log.info("Tax Type: " + taxType);
										taxFedID = it.Attributes.TblTax.TaxID.text();
										log.info("Tax FedID: " + taxFedID);
										if (taxType == "GSOHSP") {
											ediInvoice.setProperty("fedtaxamount", taxAmount);
											ediInvoice.setProperty("fedtaxid", taxFedID);
										} else {
											ediInvoice.setProperty("provtaxamount", taxAmount);
										}
									}
									if (!foundData) {
										String inMsg = "ERROR: Tax Amount value was not found in INVOICE.";
										log.info(inMsg);
									}
								}

								if (foundData) {
									foundData = false;
									/* This works - This gets the first level store info */
									shippingAmount = it.Attributes.TblAllowCharge.AllowChargeAmount.text();
									if (!shippingAmount.isEmpty()) {
										ediInvoice.setProperty("shipping", shippingAmount);
										log.info("Shipping Amount: " + shippingAmount);
										foundData = true;
									} else {
										shippingAmount = "0";
										ediInvoice.setProperty("shipping", shippingAmount);
										foundData = true;
									}
									if (!foundData) {
										String inMsg = "ERROR: Shipping Amount value was not found in INVOICE.";
										log.info(inMsg);
									}
								}
								/* TODO: Go through and add invoice itmes to ArrayList.
								 * TODO: Add invoice items to invoiceitems table.
								 * TODO: SAVE INVOICE SOMEWHERE! */
								if (foundData) {
									/* Get the products from the XML file */
									def allInvoiceDetails = it.depthFirst().grep{
										it.name() == 'InvoiceDetail';
									}
									log.info("Found InvoiceDetails: " + allInvoiceDetails.size().toString());
									allInvoiceDetails.each {

										Product product = null;
										String linePrice = "";
										String vendorCode = "";
										String quantity = "";

										//Create a new search query for the invoice item
										vendorCode = it.Attributes.TblReferenceNbr.find {it.Qualifier == "VN"}.ReferenceNbr.text();
										if (!vendorCode.isEmpty()) {
											log.info("vendorCode: " + vendorCode);
											Data findProduct = productSearcher.searchByField(MANUFACTURER_SEARCH_FIELD, vendorCode);
											String findProductID = findProduct.getId();
											cartItem = order.getCartItemByProductID(findProductID);
											if (cartItem == null) {
												cartItem = order.getCartItemByProductSku(vendorCode);
											}
											if (cartItem != null) {
												product = cartItem.getProduct();
												if (product != null) {
													productID = product.getId();
													log.info("Product Found: " + productID + ":" + product.getName());
													foundData = true;
												} else {
													String inMsg = "Product(" + vendorCode + ") cannot be found!";
													log.info(inMsg);
													foundData = false;
												}
											}
										}
										if (foundData) {
											foundData = false;
											quantity = it.Quantity;
											if (!quantity.isEmpty()) {
												log.info("Quantity: " + quantity);
												foundData = true;
											} else {
												//Create web event to send an email.
												String inMsg = "Quantity was not found.";
												log.info(inMsg);
												foundData = false;
											}
										}
										if (foundData) {
											foundData = false;
											linePrice = it.Attributes.TblAmount.find {it.Qualifier == "LI"}.Amount.text();
											if (!linePrice.isEmpty()) {
												log.info("Line Price: " + linePrice);
												foundData = true;
											} else {
												//Create web event to send an email.
												String inMsg = "LinePrice was not found.";
												log.info(inMsg);
											}
										}
										if (foundData) {
											foundData = false;
											SearchQuery invoiceItemQuery = invoiceItemSearcher.createSearchQuery();
											invoiceItemQuery.addExact("invoiceid", ediInvoice.getId());
											invoiceItemQuery.addExact("product", productID);
											HitTracker items = invoiceItemSearcher.search(invoiceItemQuery);
											if (items.size() == 1) {
												try {
													ediInvoiceItem = invoiceItemSearcher.searchById(items.get(0).getId());
													if (ediInvoice != null) {
														ediInvoiceItemID = ediInvoiceItem.getId();
														log.info("Invoice Item exists: " + ediInvoiceItemID);
														foundData = true;
													} else {
														String inMsg = "ERROR: Invalid Invoice Item.";
														log.info(inMsg);
														foundData = false;
													}
												} catch (Exception e) {
													strMsg = "ERROR: Invalid InvoiceItem Search.";
													strMsg += "InvoiceID: " + ediInvoice.getId() + "\n";
													strMsg += "Purchase Order: " + purchaseOrder + "\n";
													log.info(strMsg);
													foundData = false;
												}
											} else if (items.size() == 0) {
												try {
													log.info("Invoice Item not found! Add new Invoice Line Item");
													ediInvoiceItem = media.getInvoiceItemsSearcher().createNewData();
													ediInvoiceItem.setId(media.getInvoiceItemsSearcher().nextId());
													ediInvoiceItem.setSourcePath(ediInvoice.getId());

													ediInvoiceItemID = ediInvoiceItem.getId();
													ediInvoiceItem.setProperty("invoiceid", ediInvoice.getId());
													foundData = true;
												} catch (Exception e) {
													strMsg = "ERROR: Problem creating Invoice Item.";
													strMsg += "Invoice ID: " + ediInvoice.getId() + "\n";
													strMsg += "Purchase Order: " + purchaseOrder + "\n";
													log.info(strMsg);
													foundData = false;
												}
											}
											if (foundData) {
												try {
													//add properties and save
													ediInvoiceItem.setProperty("product", productID);
													ediInvoiceItem.setProperty("price", linePrice);
													ediInvoiceItem.setProperty("quantity", quantity);
													invoiceItems.add(ediInvoiceItem);
													String inMsg = "Line Item (" + ediInvoiceItem.getId() + ") saved for Invoice(" + ediInvoice.getId() + ")";
													log.info(inMsg);
													foundData = true;
													ediInvoiceItem = null;
												} catch (Exception e) {
													strMsg = "ERROR: Problem saving Invoice Item.";
													strMsg += "Invoice ID: " + ediInvoice.getId() + "\n";
													strMsg += "Purchase Order: " + purchaseOrder + "\n";
													log.info(strMsg);
													foundData = false;
												}
											} /* end FoundData */
											if (foundData) {
												foundData = false;
												if (!order.containsShipmentByWaybill(waybill)) {
													if (!shipment.containsEntryForSku(cartItem.getProduct().getInventoryItem(0).getSku()) && cartItem !=  null ) {
														ShipmentEntry entry = new ShipmentEntry();
														entry.setCartItem(cartItem);
														entry.setQuantity(Integer.parseInt(quantity));
														shipment.setProperty("shipdate", DateStorageUtil.getStorageUtil().formatForStorage(newDate));
														shipment.setProperty("waybill", waybill);
														shipment.setProperty("courier", courier);
														shipment.addEntry(entry);
														foundData = true;

														strMsg = "Order Updated(" + purchaseOrder + ") and saved";
														log.info(strMsg);

														strMsg = "Waybill:Courier (" + waybill + ":" + courier + ")";
														log.info(strMsg);

														strMsg = "SKU (" + vendorCode + ")";
														log.info(strMsg);

													} else {
														// If waybill exists, check quantity in shipment compared to the order
														foundData = true;
														int totalShipped = 0;
														ArrayList<Shipment> shipments = order.getShipments();
														for (Shipment eShipment in shipments) {
															ArrayList<ShipmentEntry> entries = eShipment.getShipmentEntries();
															for (ShipmentEntry eEntry in entries) {
																totalShipped += eEntry.getQuantity();
															}
														}
														int cartQty = cartItem.getQuantity();
														int qtyShipped = Integer.parseInt(quantity);
														if (qtyShipped <= (cartQty - totalShipped)) {
															ShipmentEntry entry = new ShipmentEntry();
															entry.setCartItem(cartItem);
															entry.setQuantity(Integer.parseInt(quantity));
															shipment.setProperty("waybill", waybill);
															shipment.setProperty("courier", courier);
															shipment.setProperty("shipdate", DateStorageUtil.getStorageUtil().formatForStorage(newDate));
															shipment.addEntry(entry);

															strMsg = "Order Updated(" + purchaseOrder + ") and saved";
															log.info(strMsg);

															strMsg = "Waybill:Courier (" + waybill + ":" + courier + ")";
															log.info(strMsg);

															strMsg = "SKU (" + vendorCode + ")";
															log.info(strMsg);
														}
													}
												} else {
													// If waybill exists, check quantity in shipment compared to the order
													foundData = true;
													int totalShipped = 0;
													ArrayList<Shipment> shipments = order.getShipments();
													for (Shipment eShipment in shipments) {
														ArrayList<ShipmentEntry> entries = eShipment.getShipmentEntries();
														for (ShipmentEntry eEntry in entries) {
															totalShipped += eEntry.getQuantity();
														}
													}
													int cartQty = cartItem.getQuantity();
													int qtyShipped = Integer.parseInt(quantity);
													if (qtyShipped <= (cartQty - totalShipped)) {
														ShipmentEntry entry = new ShipmentEntry();
														entry.setCartItem(cartItem);
														entry.setQuantity(Integer.parseInt(quantity));
														shipment.setProperty("waybill", waybill);
														shipment.setProperty("courier", courier);
														shipment.setProperty("shipdate", DateStorageUtil.getStorageUtil().formatForStorage(newDate));
														shipment.addEntry(entry);

														strMsg = "Order Updated(" + purchaseOrder + ") and saved";
														log.info(strMsg);

														strMsg = "Waybill:Courier (" + waybill + ":" + courier + ")";
														log.info(strMsg);

														strMsg = "SKU (" + vendorCode + ")";
														log.info(strMsg);
													}
												}
											} /* END FoundData */
										} /* END foundData */
									} /* END allInvoiceDetails */
									if (foundData) {
										if (shipment != null) {
											if (!order.getShipments().contains(shipment)) {
												order.addShipment(shipment);
											}
										}
										if(shipment.getShipmentEntries().size() >0) {
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
											foundData = true;
										}
									}
									if (foundData) {
										strMsg = "";
										for(Iterator<Data> item = invoiceItems.iterator(); item.hasNext();) {
											Data invoiceItem = (Data)item.next();
											invoiceItemSearcher.saveData(invoiceItem, inReq.getUser());
											strMsg += "InvoiceItem created: " + invoiceItem.getId() + " for Invoice #" + ediInvoice.getId() + ":" + ediInvoice.get("ponumber") + "\n";
										}
										log.info(strMsg);
									}
								} // end FOUND DATA
							} /* END INVOICEHEADERS */
							if (foundData) {
								invoiceSearcher.saveData(ediInvoice, inReq.getUser());
								processedInvoices.add(ediInvoice.getId());
								strMsg = "Invoice saved (" + ediInvoice.getId() + ")" + "\n";
								strMsg += "Purchase Order: " + purchaseOrder + "\n";
								log.info(strMsg);
							}
						} /* END INVOICEGROUPS */
					} /* END foundData */
					if (foundData) {
						ArrayList emaillist = new ArrayList();
						HitTracker results = userprofilesearcher.fieldSearch("storeadmin", "true");
						if (results.size() > 0) {
							for(Iterator detail = results.iterator(); detail.hasNext();) {
								Data userInfo = (Data)detail.next();
								emaillist.add(userInfo.get("email"));
							}
							for (Iterator inv = processedInvoices.iterator(); inv.hasNext();) {
								String invoiceID = (String)inv.next();
								Data invoice = invoiceSearcher.searchById(invoiceID);
								String purchaseOrderNumber = invoice.get("ponumber");
								String iNumber = invoice.get("invoicenumber");
								Order foundOrder = media.searchForOrder(purchaseOrderNumber);
								if (foundOrder != null) {
									inReq.putPageValue("id", invoiceID);
									inReq.putPageValue("order", foundOrder);
									String templatePage = "/ecommerce/views/modules/invoice/workflow/invoice-notification.html";
									String subject = "INVOICE has been generated: " + iNumber;
									sendEmail(archive, context, emaillist, templatePage, subject);
									log.info("Email sent to Store Admins");
								} else {
									strMsg = "ORDER (" + purchaseOrderNumber + ") for Invoice (" + invoiceNumber + ") could not be loaded. ";
									log.info(strMsg);
									log.info("Email NOT sent to admins.");
								}
							}
						}
						boolean move = movePageToProcessed(pageManager, page, media.getCatalogid(), true);
						if (move) {
							String inMsg = "Invoice File(" + page.getName() + ") has been moved to processed.";
							log.info(inMsg);
						} else {
							String inMsg = "INVOICE FILE(" + page.getName() + ") FAILED MOVE TO PROCESSED";
							log.info(inMsg);
						}
					} else {
						String inMsg = "ERROR Invoice not saved.";
						log.info(inMsg);
						ArrayList emaillist = new ArrayList();
						HitTracker results = userprofilesearcher.fieldSearch("storeadmin", "true");
						if (results.size() > 0) {
							for(Iterator detail = results.iterator(); detail.hasNext();) {
								Data userInfo = (Data)detail.next();
								emaillist.add(userInfo.get("email"));
							}
							inReq.putPageValue("xmlfile", page.getName());
							String templatePage = "/ecommerce/views/modules/invoice/workflow/invoice-error-notification.html";
							String subject = "ERROR processing Invoice XML File";
							sendEmail(archive, inReq, emaillist, templatePage, subject);
							log.info("ERROR Email sent to Store Admins");
						}
						boolean move = movePageToProcessed(pageManager, page, media.getCatalogid(), false);
						if (move) {
							inMsg = "Invoice File (" + page.getName() + ") moved to error";
							log.info(inMsg);
						} else {
							inMsg = "Invoice File (" + page.getName() + ") failed to move to ERROR";
							log.info(inMsg);
						}
					}
				} /* END (xmlFile.exists() && xmlFile.isFile()) */
			} /* END For dirList.iterator() */
			if (iterCounter == 0) {
				String inMsg = "There are no files to process at this time.";
				log.info(inMsg);
			}
		} /* END dirList.size() > 0 */
	}

	private boolean movePageToProcessed(PageManager pageManager, Page page,
			String catalogid, boolean saved) {

		boolean move = false;

		String processedFolder = "/WEB-INF/data/${catalogid}/processed/invoices/";
		String asnFolder = "/WEB-INF/data/${catalogid}/incoming/invoices/";
		if (!saved) {
			processedFolder += "errors/";
		}

		String destinationFile = processedFolder + page.getName();
		Page destination = pageManager.getPage(destinationFile);
		pageManager.movePage(page, destination);
		if (destination.exists()) {
			move = true;
		}
		return move;
	}
	private boolean movePageToCesium(PageManager pageManager, Page page, String catalogid) {

		boolean move = false;

		String processedFolder = "/WEB-INF/data/${catalogid}/incoming/invoices/cesium/";
		String destinationFile = processedFolder + page.getName();
		Page destination = pageManager.getPage(destinationFile);
		pageManager.movePage(page, destination);
		if (destination.exists()) {
			move = true;
			return move;
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

logs = new ScriptLogger();
logs.startCapture();
try {
	EdiCesiumImport ediCesiumImport = new EdiCesiumImport();
	ediCesiumImport.setLog(logs);
	ediCesiumImport.setContext(context);
	ediCesiumImport.setPageManager(pageManager);
	ediCesiumImport.doImport();
}
finally {
	logs.stopCapture();
}

