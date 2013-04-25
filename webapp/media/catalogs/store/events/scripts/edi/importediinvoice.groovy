package edi;

import java.text.SimpleDateFormat

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.publishing.PublishResult
import org.openedit.money.Money
import org.openedit.store.CartItem
import org.openedit.store.orders.Order
import org.openedit.store.util.MediaUtilities
import org.openedit.util.DateStorageUtil

import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.Page
import com.openedit.page.manage.PageManager

public class ImportEDIInvoice extends EnterMediaObject {

	private static final String LOG_HEADER = " - RECORDS - ";
	private static final String FOUND = "Found"
	private static final String NOT_FOUND = "NOT FOUND!";
	private static final String ADDED = "Added";
	private static final String CESIUM_ID = "105";
	private static String strMsg = "";

	public PublishResult importEdiXml() {

		PublishResult result = new PublishResult();
		result.setComplete(false);

		MediaUtilities media = new MediaUtilities();
		media.setContext(context);

		log.info("---- START Import EDI Invoice ----");

		def String SEARCH_FIELD = "";
		//Read the production value
		boolean production = Boolean.parseBoolean(context.findValue('productionmode'));

		// Get XML File
		//String fileName = "export-" + this.distributorName.replace(" ", "-") + ".csv";
		String invoiceFolder = "/WEB-INF/data/" + media.getCatalogid() + "/incoming/invoices/";
		PageManager pageManager = media.getArchive().getPageManager();
		List dirList = pageManager.getChildrenPaths(invoiceFolder);
		log.info("Initial directory size: " + dirList.size().toString());

		if (dirList.size() > 0) {

			Data ediInvoice = null;
			Data ediInvoiceItem = null;

			String distributorID = "";

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
					String shippingAmount = "";
					
					Order order = null;
					Date newDate = null;
					boolean foundData = false;

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
							if (distributorID.equals(CESIUM_ID)) {
								boolean move = movePageToCesium(pageManager, page, media.getCatalogid());
								if (move) {
									String inMsg = "Invoice File(" + page.getName() + ") has been moved to Cesium processing.";
									log.info(inMsg);
								} else {
									String inMsg = "INVOICE FILE(" + page.getName() + ") FAILED MOVE TO PROCESSED";
									log.info(inMsg);
								}
								continue;
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

							foundData = false;
							INVOICEHEADERS.each {

								if (!foundData) {
									//Get the INVOICENUMBER details
									invoiceNumber = it.InvoiceNumber.text();
									if (!invoiceNumber.isEmpty()) {
										log.info("Processing Invoice Number: " + invoiceNumber);
										foundData = true;
									} else {
										String inMsg = "ERROR: Invoice Number value is blank in Invoice.";
										log.info(inMsg);
									}
								}
								if (!foundData) {
									String inMsg = "ERROR: Invoice Number value was not found in INVOICE.";
									log.info(inMsg);
								}
								foundData = false;
								if (!foundData) {
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
												}
											}
										}
										catch (Exception e) {
											strMsg = "ERROR: Invalid Order: " + purchaseOrder + "\n";
											strMsg += "Exception thrown:\n";
											strMsg += "Local Message: " + e.getLocalizedMessage() + "\n";
											strMsg += "Stack Trace: " + e.getStackTrace().toString();;
											log.info(strMsg);
										}
									}
								}
								foundData = false;
								if (!foundData) {
									//Get the INVOICEAMOUNT details
									invoiceTotal = it.InvoiceAmount.text();
									if (!invoiceTotal.isEmpty()) {
										log.info("Invoice Total: " + invoiceTotal);
										foundData = true;
									}
									if (!foundData) {
										String inMsg = "ERROR: Invoice Total value was not found in INVOICE.";
										log.info(inMsg);
									}
								}
								foundData = false;
								if (!foundData) {
									/* This works - This gets the first level store info */
									def String invoiceDate = it.Attributes.TblDate.find {it.Qualifier == "003"}.DateValue.text();
									if (!invoiceDate.isEmpty()) {
										newDate = parseDate(invoiceDate);
										log.info("Invoice Date: " + invoiceDate);
										foundData = true;
									}
									if (!foundData) {
										String inMsg = "ERROR: Invvoice Date value was not found in INVOICE.";
										log.info(inMsg);
									}
								}
								foundData = false;
								if (!foundData) {
									/* This works - This gets the first level store info */
									taxAmount = it.Attributes.TblTax.TaxAmount.text();
									if (!taxAmount.isEmpty()) {
										log.info("Tax Amount: " + taxAmount);
										foundData = true;
									}
									if (!foundData) {
										String inMsg = "ERROR: Tax Amount value was not found in INVOICE.";
										log.info(inMsg);
									}
								}

								foundData = false;
								if (!foundData) {
									/* This works - This gets the first level store info */
									shippingAmount = it.Attributes.TblAllowCharge.AllowChargeAmount.text();
									if (!shippingAmount.isEmpty()) {
										log.info("Shipping Amount: " + shippingAmount);
										foundData = true;
									}
									if (!foundData) {
										String inMsg = "ERROR: Shipping Amount value was not found in INVOICE.";
										log.info(inMsg);
									}
								}
								
								if (foundData) {

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
												ediInvoice.setProperty("taxamount", taxAmount);
												ediInvoice.setProperty("shipping", shippingAmount);
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
										}
									} else if (invoices.size() == 0) {
										//Create new invoice!
										ediInvoice = invoiceSearcher.createNewData();
										ediInvoice.setId(invoiceSearcher.nextId());
										ediInvoice.setSourcePath(ediInvoice.getId());
										ediInvoice.setProperty("ponumber", purchaseOrder);
										ediInvoice.setProperty("invoicenumber", invoiceNumber);
										ediInvoice.setProperty("invoicetotal", invoiceTotal);
										ediInvoice.setProperty("taxamount", taxAmount);
										ediInvoice.setProperty("shipping", shippingAmount);
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
												product = media.searchForProductByField("manufacturersku", vendorCode);
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
											quantity = it.Quantity;
											if (!quantity.isEmpty()) {
												log.info("Quantity: " + quantity);
											} else {
												//Create web event to send an email.
												String inMsg = "Quantity was not found.";
												log.info(inMsg);
												foundData = false;
											}
											linePrice = it.Attributes.TblAmount.find {it.Qualifier == "LI"}.Amount.text();
											if (!linePrice.isEmpty()) {
												log.info("Line Price: " + linePrice);
											} else {
												//Create web event to send an email.
												String inMsg = "LinePrice was not found.";
												log.info(inMsg);
											}
											if (foundData) {
												try {
													CartItem orderItem = order.getItem(productID);
													if (orderItem == null) {
														throw new Exception("Could not load orderItem(" + productID + ")");
													}
													//Check Quantities
													int orderItemQuantity = orderItem.getQuantity();
													int ediItemQuantity = Integer.parseInt(quantity);
													if (orderItemQuantity != ediItemQuantity) {
														throw new Exception("Invalid Quantity (" + orderItemQuantity.toString() + ":" + ediItemQuantity.toString() + ")");
													}
													
													//Check Price
													//Money orderPrice = orderItem.getYourPrice();
													Money productPrice = new Money(product.get("rogersprice"));
													Money ediPrice = new Money(linePrice);
													if (productPrice.getMoneyValue() != ediPrice.getMoneyValue()) {
														throw new Exception("Invalid Price(" + orderPrice.getMoneyValue().toString() + ":" + ediPrice.getMoneyValue().toString() + ")");
													}

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
																String inMsg = "ERROR: Invalid Invoice Item.";
																log.info(inMsg);
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
													String inMsg = "Line Item (" + ediInvoiceItem.getId() + ") saved for Invoice(" + ediInvoice.getId() + ")";
													log.info(inMsg);
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
												}
											} // end FOUND DATA
										} // allInvoiceDetails
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
									}
								}
							} // end INVOICE HEADERS
						} // end InvoiceGroups
						if (foundData) {
							boolean move = movePageToProcessed(pageManager, page, media.getCatalogid(), true);
							if (move) {
								String inMsg = "Invoice File(" + page.getName() + ") has been moved to processed.";
								log.info(inMsg);
								result.setComplete(true);
							} else {
								String inMsg = "INVOICE FILE(" + page.getName() + ") FAILED MOVE TO PROCESSED";
								log.info(inMsg);
							}
						} else {
							String inMsg = "ERROR Invoice not saved.";
							log.info(inMsg);
							boolean move = movePageToProcessed(pageManager, page, media.getCatalogid(), false);
							if (move) {
								inMsg = "Invoice File (" + page.getName() + ") moved to error";
								log.info(inMsg);
							} else {
								inMsg = "Invoice File (" + page.getName() + ") failed to move to ERROR";
								log.info(inMsg);
							}
						}
						log.info("---- END Import EDI Invoice ----");
					} // end XML File Exists
				} // end loop through files
			} // end File Loop Iterator
			if (iterCounter == 0) {
				String inMsg = "There are no files to process at this time.";
				log.info(inMsg);
				result.setCompleteMessage(inMsg);
				result.setComplete(true);
			}
		}
		log.info("---- FINISH Import EDI Invoice ----");
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
}
PublishResult result = new PublishResult();
result.setComplete(false);

logs = new ScriptLogger();
logs.startCapture();

try {

	ImportEDIInvoice ImportEDIInvoice = new ImportEDIInvoice();
	ImportEDIInvoice.setLog(logs);
	ImportEDIInvoice.setContext(context);
	ImportEDIInvoice.setPageManager(pageManager);

	result = ImportEDIInvoice.importEdiXml();
	if (result.isComplete()) {
		//Output value to CSV file!
		context.putPageValue("export", result.getCompleteMessage());
		if (result.isError()) {
			context.putPageValue("errorout", result.getErrorMessage());
		}
	} else {
		//ERROR: Throw exception
		context.putPageValue("errorout", result.getErrorMessage());
	}
	log.info
}
finally {
	logs.stopCapture();
}
