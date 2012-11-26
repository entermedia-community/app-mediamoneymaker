package edi;

import java.text.SimpleDateFormat

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.PublishResult
import org.openedit.store.PurchaseOrderMethod;
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

		OutputUtilities output = new OutputUtilities();

		MediaUtilities media = new MediaUtilities();
		media.setContext(context);
		media.setSearchers();
		
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
			Data product = null;

			String distributorID = "";
			String orderID = "";
			String productID = "";
			String ediInvoiceID = "";
			String ediInvoiceItemID = "";
			String storeID = "";

			def int iterCounter = 0;
			for (Iterator iterator = dirList.iterator(); iterator.hasNext();) {

				Page page = pageManager.getPage(iterator.next());
				log.info("Processing " + page.getName());

				String realpath = page.getContentItem().getAbsolutePath();

				File xmlFile = new File(realpath);
				if (xmlFile.exists() && xmlFile.isFile()) {

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
							if (distributorID.equals(CESIUM_ID)) {
								result = movePageToCesium(pageManager, page, media.getCatalogid());
								if (result.isComplete()) {
									String inMsg = "Invoice File(" + page.getName() + ") has been moved to Cesium processing.";
									strMsg += output.appendList(inMsg);
									log.info(inMsg);
								} else {
									String inMsg = "INVOICE FILE(" + page.getName() + ") FAILED MOVE TO PROCESSED";
									result.appendErrorMessage(output.appendList(inMsg));
								}
								continue;
							}
						} else {
							String inMsg = "ERROR: Distributor cannot be found.";
							log.info(inMsg);
							result.setErrorMessage(result.getErrorMessage() + "\n" + output.appendList(inMsg));
							//throw new OpenEditException(inMsg);
						}
					} else {
						String inMsg = "ERROR: Distributor cannot be found in INVOICE.";
						log.info(inMsg);
						result.setErrorMessage(result.getErrorMessage() + "\n" + output.appendList(inMsg));
						//throw new OpenEditException(inMsg);
					}

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

							if (!result.isError()) {
								//Get the INVOICENUMBER details
								def String invoiceNumber = it.InvoiceNumber.text();
								if (!invoiceNumber.isEmpty()) {
									log.info("Processing Invoice Number: " + invoiceNumber);
								} else {
									String inMsg = "ERROR: Invoice Number value is blank in Invoice.";
									log.info(inMsg);
									result.setErrorMessage(result.getErrorMessage() + "\n" + output.appendList(inMsg));
									//throw new OpenEditException(inMsg);
								}

								SearchQuery invoiceQuery = media.getInvoiceSearcher().createSearchQuery();
								invoiceQuery.addExact("distributor", distributorID);
								invoiceQuery.addExact("invoicenumber", invoiceNumber);
								HitTracker invoices = media.getInvoiceSearcher().search(invoiceQuery);
								if (invoices.size() == 1) {
									ediInvoice = media.getInvoiceSearcher().searchById(invoices.get(0).getId());
									if (ediInvoice != null) {
										ediInvoiceID = ediInvoice.getId();
										log.info("Invoice exists: " + ediInvoiceID);
									} else {
										String inMsg = "ERROR: Invalid Invoice.";
										log.info(inMsg);
										result.setErrorMessage(result.getErrorMessage() + "\n" + output.appendList(inMsg));
										//throw new OpenEditException(inMsg);
									}
								} else if (invoices.size() == 0) {
									log.info("No invoices found!");
									Searcher invoiceSearcher = media.getInvoiceSearcher();
									ediInvoice = invoiceSearcher.createNewData();
									ediInvoice.setId(invoiceSearcher.nextId());

									ediInvoiceID = ediInvoice.getId();

									ediInvoice.setSourcePath(ediInvoiceID);
									ediInvoice.setProperty("distributor", distributorID);
									ediInvoice.setProperty("invoicenumber", invoiceNumber);
									log.info("Creating new invoice: " + ediInvoiceID);
									invoiceSearcher.saveData(ediInvoice, media.getContext().getUser());

									invoiceQuery = invoiceSearcher.createSearchQuery();
									invoiceQuery.addExact("distributor", distributorID);
									invoiceQuery.addExact("invoicenumber", invoiceNumber);
									invoices = invoiceSearcher.search(invoiceQuery);

									if (invoices.size() == 1) {
										ediInvoice = media.getInvoiceSearcher().searchById(invoices.get(0).getId());
										if (ediInvoice != null) {
											log.info("Invoice exists: " + ediInvoiceID);
										} else {
											String inMsg = "ERROR: Invalid Invoice.";
											log.info(inMsg);
											result.setErrorMessage(result.getErrorMessage() + "\n" + output.appendList(inMsg));
											//throw new OpenEditException(inMsg);
										}
									} else {
										String inMsg = "ERROR: Invalid Invoice.";
										log.info(inMsg);
										result.setErrorMessage(result.getErrorMessage() + "\n" + output.appendList(inMsg));
										//throw new OpenEditException(inMsg);
									}
								} else {
									String inMsg = "ERROR: Multiple invoices found for single Distributor and Invoice Number.";
									log.info(inMsg);
									result.setErrorMessage(result.getErrorMessage() + "\n" + output.appendList(inMsg));
									//throw new OpenEditException(inMsg);
								}
							}
							if (!result.isError()) {
								ediInvoiceID = ediInvoice.getId();
							}

							if (!result.isError()) {
								//Get the PO details
								def String PO = it.Attributes.TblReferenceNbr.find {it.Qualifier == "PO"}.ReferenceNbr.text();
								if (!PO.isEmpty()) {
									ediInvoice.setProperty("ponumber", PO);
									log.info("Purchase Order: " + PO);
									//Get Rogers Order ID
									Data order = media.searchForOrder(PO);
									if (order != null) {
										orderID = orderInfo[0];
										ediInvoice.setProperty("orderid", orderID);
										log.info("Order ID: " + orderID);
										HitTracker foundStore = media.searchForStoreInOrder(orderInfo[0], orderInfo[1]);
										if (foundStore != null) {
											storeID = media.getStoreID(orderInfo[1]);
											ediInvoice.setProperty("store", storeID);
											log.info("Store: " + media.getStoreName(storeID) + ":" + orderInfo[1]);
										} else {
											//Create web event to send an email.
											String inMsg = "ERROR: Store(" + orderInfo[1] + ") was not found in INVOICE. PurchaseOrder: " + PO;
											log.info(inMsg);
											result.setErrorMessage(result.getErrorMessage() + "\n" + output.appendList(inMsg));
										}
									} else {
										String inMsg = "ERROR: Order(" + orderInfo[0] + ") was not found in INVOICE.";
										log.info(inMsg);
										result.setErrorMessage(result.getErrorMessage() + "\n" + output.appendList(inMsg));
										//throw new OpenEditException(inMsg);
									}
								} else {
									String inMsg = "ERROR: PO value is blank in INVOICE.";
									log.info(inMsg);
									result.setErrorMessage(result.getErrorMessage() + "\n" + output.appendList(inMsg));
									//throw new OpenEditException(inMsg);
								}
							}
							if (!result.isError()) {
								//Get the INVOICEAMOUNT details
								def String invoiceTotal = it.InvoiceAmount.text();
								if (!invoiceTotal.isEmpty()) {
									ediInvoice.setProperty("invoicetotal", invoiceTotal);
									log.info("Invoice Total: " + invoiceTotal);
								} else {
									String inMsg = "ERROR: Invoice Amount value is blank in Invoice.";
									log.info(inMsg);
									result.setErrorMessage(result.getErrorMessage() + "\n" + output.appendList(inMsg));
									//throw new OpenEditException(inMsg);
								}
							}
							if (!result.isError()) {
								/* This works - This gets the first level store info */
								def String invoiceDate = it.Attributes.TblDate.find {it.Qualifier == "003"}.DateValue.text();
								if (!invoiceDate.isEmpty()) {
									Date newDate = parseDate(invoiceDate);
									ediInvoice.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(newDate));
									log.info("Invoice Date: " + invoiceDate);
								} else {
									String inMsg = "ERROR: Invoice Date value is blank in Invoice.";
									log.info(inMsg);
									result.setErrorMessage(result.getErrorMessage() + "\n" + output.appendList(inMsg));
									//throw new OpenEditException(inMsg);
								}
							}

							if (!result.isError()) {
								log.info("Status: No errors - setting status to new");
								ediInvoiceID = ediInvoice.getId();
								ediInvoice.setProperty("invoicestatus", "new");
							}

							if (!result.isError()) {
								def allInvoiceDetails = it.depthFirst().grep{
									it.name() == 'InvoiceDetail';
								}
								log.info("Found InvoiceDEtails: " + allInvoiceDetails.size().toString());

								allInvoiceDetails.each {

									if (!result.isError()) {
										//Go through each invoice
										def int ctr = it.LineItemNumber.toInteger();
										log.info("Line Item Number: " + ctr.toString());

										//Create a new search query for the invoice item
										SearchQuery invoiceItemQuery = media.getInvoiceItemsSearcher().createSearchQuery();
										invoiceItemQuery.addExact("invoiceid", ediInvoiceID);
										invoiceItemQuery.addExact("store", storeID);

										log.info(ediInvoiceID+":"+storeID);
										def String vendorCode = it.Attributes.TblReferenceNbr.find {it.Qualifier == "VN"}.ReferenceNbr.text();
										if (!vendorCode.isEmpty()) {
											product = media.searchForProductbyRogersSKU(vendorCode);
											if (product != null) {
												productID = product.getId();
												log.info("Product Found: " + productID + ":" + product.getName());
												invoiceItemQuery.addExact("product", productID);
											} else {
												String inMsg = "Product(" + vendorCode + ") cannot be found!";
												log.info(inMsg);
												result.setErrorMessage(result.getErrorMessage() + "\n" + output.appendList(inMsg));
												//throw new OpenEditException(inMsg);
											}
										}
										HitTracker invoiceItems = media.getInvoiceItemsSearcher().search(invoiceItemQuery);
										if (invoiceItems.size() == 1) {
											ediInvoiceItem = media.getInvoiceItemsSearcher().searchById(invoiceItems.get(0).getId());
											if (ediInvoice != null) {
												ediInvoiceItemID = ediInvoiceItem.getId();
												log.info("Invoice Item exists: " + ediInvoiceItemID);
											} else {
												String inMsg = "ERROR: Invalid Invoice Item.";
												log.info(inMsg);
												result.setErrorMessage(result.getErrorMessage() + "\n" + output.appendList(inMsg));
												//throw new OpenEditException(inMsg);
											}
										} else if (invoiceItems.size() == 0) {
											log.info("Invoice Item not found!");
											ediInvoiceItem = media.getInvoiceItemsSearcher().createNewData();
											ediInvoiceItem.setId(media.getInvoiceItemsSearcher().nextId());
											ediInvoiceItem.setSourcePath(ediInvoice.getId());

											ediInvoiceItemID = ediInvoiceItem.getId();

											ediInvoiceItem.setProperty("store", storeID);
											ediInvoiceItem.setProperty("invoiceid", ediInvoiceID);
											ediInvoiceItem.setProperty("product", product.getId());

											log.info("Creating new invoice item: " + ediInvoiceItemID);
											media.getInvoiceItemsSearcher().saveData(ediInvoiceItem, media.getContext().getUser());

											ediInvoiceItemID = ediInvoiceItem.getId();
										}
									}
									if (!result.isError()) {
										String quantity = it.Quantity;
										if (!quantity.isEmpty()) {
											log.info("Quantity: " + quantity);
											ediInvoiceItem.setProperty("quantity", quantity);
										} else {
											//Create web event to send an email.
											String inMsg = "Quantity was not found.";
											log.info(inMsg);
											result.setErrorMessage(result.getErrorMessage() + "\n" + output.appendList(inMsg));
										}
									}
									if (!result.isError()) {

										def String linePrice = it.Attributes.TblAmount.find {it.Qualifier == "LI"}.Amount.text();
										if (!linePrice.isEmpty()) {
											log.info("Line Price: " + linePrice);
											ediInvoiceItem.setProperty("price", linePrice);
										} else {
											//Create web event to send an email.
											String inMsg = "LinePrice was not found.";
											log.info(inMsg);
											result.setErrorMessage(result.getErrorMessage() + "\n" + output.appendList(inMsg));
										}
									}
									if (!result.isError()) {
										//add properties and save
										ediInvoiceItem.setProperty("invoiceid", ediInvoiceID);
										media.getInvoiceItemsSearcher().saveData(ediInvoiceItem, media.getContext().getUser());
										String inMsg = "Line Item (" + ediInvoiceItem.getId() + ") saved for Invoice(" + ediInvoice.getId() + ")";
										strMsg += output.appendList(inMsg);
										log.info(inMsg);
									}
								}
							}
							if (!result.isError()) {
								//Write the Invoice Details
								log.info("Status: Saving Invoice (" + ediInvoiceID +")");
								media.getInvoiceSearcher().saveData(ediInvoice, media.getContext().getUser());
								strMsg += output.appendList("Invoice (" + ediInvoiceID + ") has been created!");
							}
						}
						if (!result.isError()) {
							result = movePageToProcessed(pageManager, page, media.getCatalogid(), true);
							if (result.isComplete()) {
								String inMsg = "Invoice File(" + page.getName() + ") has been moved to processed.";
								strMsg += output.appendList(inMsg);
								log.info(inMsg);
							} else {
								String inMsg = "INVOICE FILE(" + page.getName() + ") FAILED MOVE TO PROCESSED";
								result.setErrorMessage(result.getErrorMessage() + "\n" + output.appendList(inMsg));
							}
						} else {
							String inMsg = "ERROR INVOICE(" + orderID + ") NOT SAVED";
							result.setErrorMessage(result.getErrorMessage() + output.appendList(inMsg));
							result = movePageToProcessed(pageManager, page, media.getCatalogid(), false);
							if (result.isComplete()) {
								inMsg = "INVOICE FILE(" + page.getName() + ") MOVED TO ERROR";
								result.setErrorMessage(result.getErrorMessage() + "\n" + output.appendList(inMsg));
							} else {
								inMsg = "INVOICE FILE(" + page.getName() + ") FAILED MOVE TO ERROR";
								result.setErrorMessage(result.getErrorMessage() + "\n" + output.appendList(inMsg));
							}
						}
					}
				}
			}
			result.setCompleteMessage(strMsg);
			result.setComplete(true);
		} else {
			String inMsg = "There are no files to process at this time.";
			log.info(inMsg);
			result.setCompleteMessage(inMsg);
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
	private PublishResult movePageToCesium(PageManager pageManager, Page page, String catalogid) {

		PublishResult move = new PublishResult();
		move.setComplete(false);

		String processedFolder = "/WEB-INF/data/${catalogid}/incoming/invoices/cesium/";
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
}
finally {
	logs.stopCapture();
}
