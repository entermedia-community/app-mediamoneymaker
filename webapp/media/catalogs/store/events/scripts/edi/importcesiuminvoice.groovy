package edi;

import java.text.SimpleDateFormat

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.publishing.PublishResult
import org.openedit.money.Money
import org.openedit.store.CartItem
import org.openedit.store.InventoryItem
import org.openedit.store.Product
import org.openedit.store.Store
import org.openedit.store.orders.Order
import org.openedit.store.orders.Shipment
import org.openedit.store.orders.ShipmentEntry
import org.openedit.store.util.MediaUtilities;
import org.openedit.util.DateStorageUtil

import com.openedit.OpenEditException
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.Page
import com.openedit.page.manage.PageManager

public class ImportCesiumInvoice extends EnterMediaObject {

	private static final String LOG_HEADER = " - RECORDS - ";
	private static final String FOUND = "Found"
	private static final String NOT_FOUND = "NOT FOUND!";
	private static final String ADDED = "Added";

	public PublishResult doImport() {

		PublishResult result = new PublishResult();
		result.setComplete(false);

		MediaUtilities media = new MediaUtilities();
		media.setContext(context);

		log.info("-- START Import Cesium Invoice ----");

		def strMsg = "";
		boolean foundData = false;

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

		def String SEARCH_FIELD = "";
		//Read the production value
		boolean production = Boolean.parseBoolean(context.findValue('productionmode'));
		// Get XML File
		String invoiceFolder = "/WEB-INF/data/" + media.getCatalogid() + "/incoming/invoices/cesium/";
		PageManager pageManager = media.getArchive().getPageManager();
		List dirList = pageManager.getChildrenPaths(invoiceFolder);
		log.info("Initial directory size: " + dirList.size().toString());

		if (dirList.size() > 0) {

			Data ediInvoice = null;
			Data ediInvoiceItem = null;

			String distributorID = "";

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
					String productID = "";
					String ediInvoiceItemID = "";
					String invoiceNumber = "";
					String invoiceTotal = "";
					String purchaseOrder = "";
					String waybill = "";

					Order order = null;
					Date newDate = null;

					//Create the XMLSlurper Object
					def INVOICE = new XmlSlurper().parse(page.getReader());

					//Get the distributor
					String distributor = INVOICE.Attributes.TblEntityID.find {it.Qualifier == "GSSND"}.EntityValue.text();
					if (!distributor.isEmpty()) {
						//Find the distributor
						Data DISTRIB = media.searchForDistributor(distributor, production);
						if (DISTRIB != null) {
							distributorID = DISTRIB.getId();
							log.info("Distributor Found: " + distributor);
							log.info("Distributor ID: " + distributorID);
							foundData = true;
						} else {
							strMsg = "ERROR: Distributor cannot be found.";
							log.info(strMsg);
							foundData = false;
							errorList.add(strMsg);
						}
					} else {
						strMsg = "ERROR: Distributor cannot be found in INVOICE.";
						log.info(strMsg);
						foundData = false;
						errorList.add(strMsg);
					}

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

								foundData = false;
								
								Shipment shipment = new Shipment();

								if (!foundData) {
									//Get the INVOICENUMBER details
									invoiceNumber = it.InvoiceNumber.text();
									if (!invoiceNumber.isEmpty()) {
										log.info("Processing Invoice Number: " + invoiceNumber);
										foundData = true;
									} else {
										strMsg = "ERROR: Invoice Number value was not found in INVOICE.";
										log.info(strMsg);
										errorList.add(strMsg);
									}
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
												} else {
													strMsg = "ERROR: Order(" + purchaseOrder + ") was not found from ASN.";
													log.info(strMsg);
													errorList.add(strMsg);
												}
											}
										}
										catch (Exception e) {
											strMsg = "ERROR: Invalid Order: " + purchaseOrder + "\n";
											strMsg += "Exception thrown:\n";
											strMsg += "Local Message: " + e.getLocalizedMessage() + "\n";
											strMsg += "Stack Trace: " + e.getStackTrace().toString();;
											log.info(strMsg);
											errorList.add(strMsg);
										}
									}
								}
								if (foundData) {
									foundData = false;
									//Get the INVOICEAMOUNT details
									invoiceTotal = it.InvoiceAmount.text();
									if (!invoiceTotal.isEmpty()) {
										log.info("Invoice Total: " + invoiceTotal);
										foundData = true;
									} else {
										strMsg = "ERROR: Invoice Total value was not found in INVOICE.";
										log.info(strMsg);
										errorList.add(strMsg);
									}
								}
								if (foundData) {
									foundData = false;
									/* This works - This gets the first level store info */
									def String invoiceDate = it.Attributes.TblDate.find {it.Qualifier == "003"}.DateValue.text();
									if (!invoiceDate.isEmpty()) {
										newDate = parseDate(invoiceDate);
										log.info("Invoice Date: " + invoiceDate);
										foundData = true;
									} else {
										strMsg = "ERROR: Invvoice Date value was not found in INVOICE.";
										log.info(strMsg);
										errorList.add(strMsg);
									}
								}
								if (foundData) {
									foundData = false;
									//_PRO
									def String WB = it.Attributes.TblReferenceNbr.find {it.Qualifier == "WY"}.ReferenceNbr.text();
									if (!WB.isEmpty()) {
										waybill = WB;
										log.info("Waybill: " + waybill);
										foundData = true;
									} else {
										strMsg = "ERROR: Wyabill value was not found in INVOICE.";
										log.info(strMsg);
										errorList.add(strMsg);
									}
								}
								if (foundData) {
									foundData = false;

									Searcher invoiceSearcher = media.getInvoiceSearcher();
									SearchQuery invoiceQuery = invoiceSearcher.createSearchQuery();
									invoiceQuery.addExact("ponumber", purchaseOrder);
									invoiceQuery.addExact("invoicenumber", invoiceNumber);
									HitTracker invoices = media.getInvoiceSearcher().search(invoiceQuery);
									if (invoices.size() == 1) {
										try {
											ediInvoice = media.getInvoiceSearcher().searchById(invoices.get(0).getId());
											if (ediInvoice != null) {
												log.info("Invoice exists: " + ediInvoice.getId());
												ediInvoice.setProperty("invoicetotal", invoiceTotal);
												ediInvoice.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(newDate));
												ediInvoice.setProperty("invoicestatus", "updated");
												invoiceSearcher.saveData(ediInvoice, media.getContext().getUser());
												log.info("Invoice (" + ediInvoice.getId() + ") has been updated.");
												foundData = true;
											}
										}
										catch (Exception e) {
											strMsg = "ERROR: Invalid Invoice/PurchaseOrder match: " + invoiceNumber + ":" + purchaseOrder + "\n";
											strMsg += "Exception thrown:\n";
											strMsg += "Local Message: " + e.getLocalizedMessage() + "\n";
											strMsg += "Stack Trace: " + e.getStackTrace().toString();;
											log.info(strMsg);
											errorList.add(strMsg);
										}
									} else if (invoices.size() == 0) {
										//Create new invoice!
										ediInvoice = invoiceSearcher.createNewData();
										ediInvoice.setId(invoiceSearcher.nextId());
										ediInvoice.setSourcePath(ediInvoice.getId());
										ediInvoice.setProperty("ponumber", purchaseOrder);
										ediInvoice.setProperty("invoicenumber", invoiceNumber);
										ediInvoice.setProperty("invoicetotal", invoiceTotal);
										ediInvoice.setProperty("invoicestatus", "new");
										ediInvoice.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(newDate));

										try {
											invoiceSearcher.saveData(ediInvoice, media.getContext().getUser());
											log.info("New invoice created: " + ediInvoice.getId());
										}
										catch (Exception e) {
											strMsg = "ERROR: Invalid Invoice/PurchaseOrder match: " + invoiceNumber + ":" + purchaseOrder + "\n";
											strMsg += "Exception thrown:\n";
											strMsg += "Local Message: " + e.getLocalizedMessage() + "\n";
											strMsg += "Stack Trace: " + e.getStackTrace().toString();;
											log.info(strMsg);
											errorList.add(strMsg);
										}
										foundData = true;
									}
									if (foundData) {
										def allInvoiceDetails = it.depthFirst().grep{
											it.name() == 'InvoiceDetail';
										}
										log.info("Found InvoiceDetails: " + allInvoiceDetails.size().toString());
										allInvoiceDetails.each {

											Data product = null;
											String linePrice = "";
											String vendorCode = "";
											String quantity = "";

											//Create a new search query for the invoice item
											vendorCode = it.Attributes.TblReferenceNbr.find {it.Qualifier == "VN"}.ReferenceNbr.text();
											if (!vendorCode.isEmpty()) {
												product = media.searchForProductBySku("manufacturersku", vendorCode);
												if (product != null) {
													productID = product.getId();
													log.info("Product Found: " + productID + ":" + product.getName());
													foundData = true;
												} else {
													strMsg = "Product(" + vendorCode + ") cannot be found!";
													log.info(strMsg);
													foundData = false;
													errorList.add(strMsg);
												}
											}
											quantity = it.Quantity;
											if (!quantity.isEmpty()) {
												log.info("Quantity: " + quantity);
											} else {
												//Create web event to send an email.
												strMsg = "Quantity was not found.";
												log.info(strMsg);
												foundData = false;
												errorList.add(strMsg);
											}
											linePrice = it.Attributes.TblAmount.find {it.Qualifier == "LI"}.Amount.text();
											if (!linePrice.isEmpty()) {
												log.info("Line Price: " + linePrice);
											} else {
												//Create web event to send an email.
												strMsg = "LinePrice was not found.";
												log.info(strMsg);
												errorList.add(strMsg);
											}
											if (foundData) {
												try {
													CartItem orderItem = order.getItem(productID);
													if (orderItem == null) {
														throw new Exception("Could not load orderItem(" + productID + ")");
													}
//													//Check Quantities
//													int orderItemQuantity = orderItem.getQuantity();
//													int ediItemQuantity = Integer.parseInt(quantity);
//													if (orderItemQuantity != ediItemQuantity) {
//														throw new Exception("Invalid Quantity (" + orderItemQuantity.toString() + ":" + ediItemQuantity.toString() + ")");
//													}
//													//Check Price
//													Money orderPrice = orderItem.getYourPrice();
//													Money ediPrice = new Money(linePrice);
//													if (orderPrice.getMoneyValue() != ediPrice.getMoneyValue()) {
//														throw new Exception("Invalid Price(" + orderPrice.getMoneyValue().toString() + ":" + ediPrice.getMoneyValue().toString() + ")");
//													}

													SearchQuery invoiceItemQuery = media.getInvoiceItemsSearcher().createSearchQuery();
													invoiceItemQuery.addExact("invoiceid", ediInvoice.getId());
													invoiceItemQuery.addExact("product", productID);
													HitTracker invoiceItems = media.getInvoiceItemsSearcher().search(invoiceItemQuery);
													if (invoiceItems.size() == 1) {
														try {
															ediInvoiceItem = media.getInvoiceItemsSearcher().searchById(invoiceItems.get(0).getId());
															if (ediInvoice != null) {
																ediInvoiceItemID = ediInvoiceItem.getId();
																log.info("Invoice Item exists: " + ediInvoiceItemID);
																foundData = true;
															} else {
																strMsg = "ERROR: Invalid Invoice Item.";
																log.info(strMsg);
																foundData = false;
															}
														}
														catch (Exception e) {
															strMsg = "ERROR: Invalid InvoiceItem Search: " + ediInvoice.getId() + ":" + purchaseOrder + "\n";
															strMsg += "Exception thrown:\n";
															strMsg += "Local Message: " + e.getLocalizedMessage() + "\n";
															strMsg += "Stack Trace: " + e.getStackTrace().toString();;
															log.info(strMsg);
															foundData = false;
														}
													} else if (invoiceItems.size() == 0) {
														log.info("Invoice Item not found!");
														ediInvoiceItem = media.getInvoiceItemsSearcher().createNewData();
														ediInvoiceItem.setId(media.getInvoiceItemsSearcher().nextId());
														ediInvoiceItem.setSourcePath(ediInvoice.getId());

														ediInvoiceItemID = ediInvoiceItem.getId();
														ediInvoiceItem.setProperty("invoiceid", ediInvoice.getId());
														foundData = true;
													}

													//add properties and save
													ediInvoiceItem.setProperty("product", product.getId());
													ediInvoiceItem.setProperty("price", linePrice);
													ediInvoiceItem.setProperty("quantity", quantity);
													media.getInvoiceItemsSearcher().saveData(ediInvoiceItem, media.getContext().getUser());
													strMsg = "Line Item (" + ediInvoiceItem.getId() + ") saved for Invoice(" + ediInvoice.getId() + ")";
													log.info(strMsg);

													if ((foundData) && (errorList.size() == 0)) {

														Product target = media.getProductSearcher().searchById(productID);
														InventoryItem productInventory = target.getInventoryItem(0);
														CartItem item = order.getItem(productInventory.getSku());

														if (!shipment.containsEntryForSku(productInventory.getSku()) && item !=  null ) {
															ShipmentEntry entry = new ShipmentEntry();
															entry.setCartItem(item);
															entry.setQuantity(Integer.parseInt(quantity));
															shipment.setProperty("waybill", waybill);
															shipment.setProperty("shipdate", DateStorageUtil.getStorageUtil().formatForStorage(newDate));
															shipment.addEntry(entry);
															foundData = true;

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
															strMsg = "Cart Item cannot be found(" + orderID + ")";
															log.info(strMsg);
															errorList.add(strMsg);
														} // end if orderitems
													}

													foundData = true;
													orderItem = null;
												}
												catch (Exception e) {
													strMsg = "ERROR: Invalid Order Item: " + purchaseOrder + ":" + productID + "\n";
													strMsg += "Exception thrown:\n";
													strMsg += "Local Message: " + e.getLocalizedMessage() + "\n";
													strMsg += "Stack Trace: " + e.getStackTrace().toString();;
													log.info(strMsg);
													foundData = false;
													errorList.add(strMsg);
													throw new OpenEditException(strMsg);
												}
											} // end FOUND DATA
										} // allInvoiceDetails
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
											foundData = true;
										}
									} // end FOUND DATA
								} // end FOUND DATA
								if (foundData) {
									//Write the Invoice Details
									try {
										log.info("Status: Saving Invoice (" + ediInvoice.getId() +")");
										media.getInvoiceSearcher().saveData(ediInvoice, media.getContext().getUser());
									}
									catch (Exception e) {
										strMsg = "ERROR: Saving Invoice: " + ediInvoice.getId() + ":" + purchaseOrder + "\n";
										strMsg += "Exception thrown:\n";
										strMsg += "Local Message: " + e.getLocalizedMessage() + "\n";
										strMsg += "Stack Trace: " + e.getStackTrace().toString();;
										log.info(strMsg);
										foundData = false;
										throw new OpenEditException(strMsg);
									}
								}
							} // end INVOICE HEADERS
						} // end InvoiceGroups
						if (foundData) {
							boolean move = movePageToProcessed(pageManager, page, media.getCatalogid(), true);
							if (move) {
								strMsg = "Invoice File(" + page.getName() + ") has been moved to processed.";
								log.info(strMsg);
								result.setComplete(true);
							} else {
								strMsg = "INVOICE FILE(" + page.getName() + ") FAILED MOVE TO PROCESSED";
								log.info(strMsg);
							}
						} else {
							strMsg = "ERROR Invoice not saved.";
							log.info(strMsg);
							boolean move = movePageToProcessed(pageManager, page, media.getCatalogid(), false);
							if (move) {
								strMsg = "Invoice File (" + page.getName() + ") moved to error";
								log.info(strMsg);
							} else {
								strMsg = "Invoice File (" + page.getName() + ") failed to move to ERROR";
								log.info(strMsg);
							}
						}
					} // end XML File Exists
				} // end loop through files
			} // end File Loop Iterator
			if (iterCounter == 0) {
				strMsg = "There are no files to process at this time.";
				log.info(strMsg);
				result.setCompleteMessage(strMsg);
				result.setComplete(true);
			}
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

		String processedFolder = "/WEB-INF/data/${catalogid}/processed/invoices/";
		String asnFolder = "/WEB-INF/data/${catalogid}/incoming/invoices/";
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

	ImportCesiumInvoice importCesiumInvoice = new ImportCesiumInvoice();
	importCesiumInvoice.setLog(logs);
	importCesiumInvoice.setContext(context);
	importCesiumInvoice.setPageManager(pageManager);

	result = importCesiumInvoice.doImport();
	if (result.isComplete()) {
		//Output value to CSV file!
		log.info(result.getCompleteMessage())
		context.putPageValue("export", result.getCompleteMessage());
		if (result.isError()) {
			log.info("ERROR " + result.getErrorMessage());
			context.putPageValue("errorout", result.getErrorMessage());
		}
	} else {
		//ERROR: Throw exception
		log.info("ERROR " + result.getErrorMessage());
		context.putPageValue("errorout", result.getErrorMessage());
	}
}
finally {
	logs.stopCapture();
}
