package edi

import java.text.SimpleDateFormat

import org.entermedia.email.PostMail
import org.entermedia.email.TemplateWebEmail
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.money.Money
import org.openedit.store.CartItem
import org.openedit.store.Product
import org.openedit.store.Store
import org.openedit.store.orders.Order
import org.openedit.store.util.MediaUtilities
import org.openedit.util.DateStorageUtil

import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.Page
import com.openedit.page.manage.PageManager

public class EdiInvoiceImport extends EnterMediaObject {

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

		SearcherManager manager = archive.getSearcherManager();
		Searcher userprofilesearcher = archive.getSearcher("userprofile");

		log.info("---- START Import EDI Invoice ----");

		Store store = null;

		try {
			store  = inReq.getPageValue("store");
			if (store != null) {
				log.info("Store loaded");
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

			def int iterCounter = 0;
			for (Iterator iterator = dirList.iterator(); iterator.hasNext();) {

				Page page = pageManager.getPage(iterator.next());
				log.info("Processing " + page.getName());

				String realpath = page.getContentItem().getAbsolutePath();

				File xmlFile = new File(realpath);
				if (xmlFile.exists() && xmlFile.isFile()) {
					iterCounter++;

					String distributorID = "";
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

								Searcher invoiceSearcher = media.getInvoiceSearcher();
								Data ediInvoice = null;

								String orderID = "";
								String ediInvoiceItemID = "";
								String invoiceNumber = "";
								String purchaseOrder = "";

								Money invoiceAmount;
								Boolean checkShipping = true;

								Order order = null;

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
									String invoiceTotal = it.InvoiceAmount.text();
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
									String subTotal = it.Attributes.TblAmount.find {it.Qualifier == "6"}.Amount.text();
									if (!subTotal.isEmpty()) {
										ediInvoice.setProperty("subtotal", subTotal);
										log.info("Sub Total: " + subTotal);
										foundData = true;
									}
									if (!foundData) {
										String inMsg = "ERROR: Sub Total value was not found in INVOICE.";
										log.info(inMsg);
									}
								}
								if (foundData) {
									foundData = false;
									/* This works - This gets the first level store info */
									String invoiceDate = it.Attributes.TblDate.find {it.Qualifier == "003"}.DateValue.text();
									if (!invoiceDate.isEmpty()) {
										Date newDate = new SimpleDateFormat("yyyyMMdd").parse(invoiceDate);
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
									/* This works - This gets the first level store info */
									String inMsg = "";
									String taxAmount = it.Attributes.TblTax.TaxAmount.text();
									if (!taxAmount.isEmpty()) {
										log.info("Tax Amount: " + taxAmount);
										foundData = true;
										String taxType = it.Attributes.TblTax.TaxType.text();
										String taxFedID = it.Attributes.TblTax.TaxID.text();
										if (taxType == "GS") {
											ediInvoice.setProperty("fedtaxamount", taxAmount);
											inMsg = "Fed Tax Amount: " + taxAmount;
											log.info(inMsg);
											ediInvoice.setProperty("fedtaxid", taxFedID);
											inMsg = "Fed Tax ID" + taxFedID;
											log.info(inMsg);
										} else {
											ediInvoice.setProperty("provtaxamount", taxAmount);
											inMsg = "Provincial Tax Amount" + taxAmount;
											log.info(inMsg);
											ediInvoice.setProperty("fedtaxid", "NOT PROVIDED");
										}
									}
									if (!foundData) {
										inMsg = "ERROR: Tax Amount value was not found in INVOICE.";
										log.info(inMsg);
									}
								}

								if (foundData) {
									foundData = false;
									/* This works - This gets the first level store info */
									String shippingAmount = it.Attributes.TblAllowCharge.AllowChargeAmount.text();
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

									Searcher invoiceItemSearcher = media.getInvoiceItemsSearcher();
									List<Data> invoiceItems = new ArrayList<Data>();

									/* Get the products from the XML file */
									def allInvoiceDetails = it.depthFirst().grep{
										it.name() == 'InvoiceDetail';
									}
									log.info("Found InvoiceDetails: " + allInvoiceDetails.size().toString());
									allInvoiceDetails.each {

										Product product = null;
										String linePrice = "";
										String quantity = "";
										CartItem cartItem = null;

										//Create a new search query for the invoice item
										String vendorCode = it.Attributes.TblReferenceNbr.find {it.Qualifier == "VN"}.ReferenceNbr.text();
										if (!vendorCode.isEmpty()) {
											cartItem = order.getCartItemByProductSku(vendorCode);
											if (cartItem != null) {
												product = cartItem.getProduct();
											} else {
												Searcher productSearcher = store.getProductSearcher();
												Data vendorProduct = productSearcher.searchByField(MANUFACTURER_SEARCH_FIELD, vendorCode);
												String productID = vendorProduct.getId();
												cartItem = order.getCartItemByProductID(productID);
												if (cartItem != null) {
													product = cartItem.getProduct();
												}	
											}
											if (product != null) {
												log.info("Product Found: " + product.getId() + ":" + product.getName());
												foundData = true;
											} else {
												String inMsg = "Product(" + vendorCode + ") cannot be found!";
												log.info(inMsg);
												foundData = false;
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
											Data ediInvoiceItem = null;
											SearchQuery invoiceItemQuery = invoiceItemSearcher.createSearchQuery();
											invoiceItemQuery.addExact("invoiceid", ediInvoice.getId());
											invoiceItemQuery.addExact("product", product.getId());
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
													ediInvoiceItem.setProperty("product", product.getId());
													ediInvoiceItem.setProperty("price", linePrice);
													ediInvoiceItem.setProperty("quantity", quantity);
													invoiceItems.add(ediInvoiceItem);
													String inMsg = "Line Item (" + ediInvoiceItem.getId() + ") saved for Invoice(" + ediInvoice.getId() + ")";
													log.info(inMsg);
													foundData = true;
												} catch (Exception e) {
													strMsg = "ERROR: Problem saving Invoice Item.";
													strMsg += "Invoice ID: " + ediInvoice.getId() + "\n";
													strMsg += "Purchase Order: " + purchaseOrder + "\n";
													log.info(strMsg);
													foundData = false;
												}
											} /* end FoundData */
										} /* END foundData */
									} /* END allInvoiceDetails */
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
  								if (foundData) {
									invoiceSearcher.saveData(ediInvoice, inReq.getUser());
									strMsg = "Invoice saved (" + ediInvoice.getId() + ")" + "\n";
									strMsg += "Purchase Order: " + purchaseOrder + "\n";
									log.info(strMsg);
									
									ArrayList emaillist = new ArrayList();
									String invoiceID = ediInvoice.getId();
									HitTracker results = userprofilesearcher.fieldSearch("storeadmin", "true");
									if (results.size() > 0) {
										for(Iterator detail = results.iterator(); detail.hasNext();) {
											Data userInfo = (Data)detail.next();
											emaillist.add(userInfo.get("email"));
										}
										Data invoice = invoiceSearcher.searchById(invoiceID);
										String purchaseOrderNumber = invoice.get("ponumber");
										String iNumber = invoice.get("invoicenumber");
										inReq.putPageValue("id", invoiceID);
										inReq.putPageValue("orderid", order.getId());
										inReq.putPageValue("distributorid", distributorID);
										String templatePage = "/ecommerce/views/modules/invoice/workflow/invoice-notification.html";
										String subject = "INVOICE has been generated: " + iNumber;
										sendEmail(archive, context, emaillist, templatePage, subject);
										log.info("Email sent to Store Admins");
									}
								} /* END foundData - Save Invoice and Send EMAIL */
							} /* END INVOICEHEADERS */
						} /* END INVOICEGROUPS */
					} /* END foundData */
					if (foundData) {
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

	EdiInvoiceImport ediInvoiceImport = new EdiInvoiceImport();
	ediInvoiceImport.setLog(logs);
	ediInvoiceImport.setContext(context);
	ediInvoiceImport.setPageManager(pageManager);
	ediInvoiceImport.doImport();
}
finally {
	logs.stopCapture();
}
