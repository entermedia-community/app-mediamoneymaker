package edi

import java.text.SimpleDateFormat
import java.util.List;

import org.dom4j.Element
import org.entermedia.email.PostMail
import org.entermedia.email.TemplateWebEmail
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.event.WebEvent
import org.openedit.event.WebEventHandler
import org.openedit.event.WebEventListener
import org.openedit.util.DateStorageUtil
import org.openedit.store.CartItem
import org.openedit.store.Store
import org.openedit.store.Product
import org.openedit.store.orders.Order
import org.openedit.store.util.MediaUtilities
import org.openedit.store.orders.Shipment
import org.openedit.store.orders.ShipmentEntry

import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.Page
import com.openedit.page.manage.PageManager
import com.openedit.util.XmlUtil

import groovy.util.slurpersupport.GPathResult


public void doImport() {
	
	WebPageRequest inReq = context;
	MediaArchive archive = inReq.getPageValue("mediaarchive");
	String catalogID = archive.getCatalogId();

	SearcherManager manager = archive.getSearcherManager();
	Searcher userprofilesearcher = archive.getSearcher("userprofile");
	Searcher invoicesearcher = archive.getSearcher("invoice");
	Searcher itemsearcher = archive.getSearcher("invoiceitem");
	Searcher productsearcher = archive.getSearcher("product");
	Searcher distributorsearcher = archive.getSearcher("distributor");
	
	//build list of store admin emails first
	ArrayList emaillist = new ArrayList();
	HitTracker storeadmins = userprofilesearcher.fieldSearch("storeadmin", "true");
	for(Iterator detail = storeadmins.iterator(); detail.hasNext();)
	{
		Data userInfo = (Data)detail.next();
		emaillist.add(userInfo.get("email"));
	}
	log.info("storeadmins: $emaillist");
	
	XmlUtil util =  new XmlUtil();
	SimpleDateFormat ediformat = new SimpleDateFormat("yyyyMMdd");
	
	Store store = context.getPageValue("store");

	log.info("---- START Import EDI Invoice ----");

	//Read the production value
	boolean production = Boolean.parseBoolean(context.findValue('productionmode'));

	// Get XML File
	String invoiceFolder = "/WEB-INF/data/" + archive.getCatalogId() + "/incoming/invoices/";
	String processedFolder = "/WEB-INF/data/${catalogid}/processed/invoices/";
	
	PageManager pageManager = archive.getPageManager();
	List dirList = pageManager.getChildrenPaths(invoiceFolder);
	log.info("Initial directory size: " + dirList.size().toString());

	if (dirList.size() > 0) {
		
		Data ediInvoice = null;
		Data ediInvoiceItem = null;
		String distributorID = "";
		List<String> processedInvoices = new ArrayList<String>();
		
		int iterCounter = 0;
		for (Iterator iterator = dirList.iterator(); iterator.hasNext();) {
			Page page = pageManager.getPage(iterator.next());
			if(page.isFolder()){
				continue;
			}
			
			log.info("Processing " + page.getName());
			
			Element inputfile =  util.getXml(page.getInputStream(), "UTF-8");

			GPathResult invoiceNode = new XmlSlurper().parse(page.getReader());
			String distributor = invoiceNode.Attributes.TblEntityID.find {it.Qualifier == "GSSND"}.EntityValue.text();
			Data distributorData = distributorsearcher.searchByField("idcode", distributor);

			invoiceNode.InvoiceGroup.InvoiceHeader.each{
				ArrayList items = new ArrayList();
				String ponumber = it.Attributes.TblReferenceNbr.find {it.Qualifier == "PO"}.ReferenceNbr.text();
				String invoicenumber = it.InvoiceNumber.text()
				Data invoice = invoicesearcher.searchByField("invoicenumber", invoicenumber);
				
				if(invoice == null){
					invoice = invoicesearcher.createNewData();
					invoice.setId(invoicesearcher.nextId());
				} else {
					log.info("already processed invoice $invoicenumber, skipping");
					return;//continue to next because already found
				}
				Order order = null;
				if(ponumber) {
					order = (Order) mediaarchive.getData("storeOrder", ponumber);
				}
				if (order == null){
					int dash = -1;
					if ((dash = ponumber.indexOf("-"))!=-1){
						String po = ponumber.substring(0, dash).trim();
						order = (Order) archive.getData("storeOrder", po);
					}
				}
				
				invoice.setProperty("terms", it.Attributes.TblTermsOfSale.TermsDescription.text());
				String termsdate = it.Attributes.TblTermsOfSale.NetDueDate.text();
				if(termsdate != null && !termsdate.isEmpty()){
					Date date = ediformat.parse(termsdate);
					invoice.setProperty("termsdate", DateStorageUtil.getStorageUtil().formatForStorage(date) );
				}
				
				if(distributorData != null){
					invoice.setProperty("distributor", distributorData.getId());
				} else{
					invoice.setProperty("distributor","NOT FOUND: ${distributor}");
				}
				log.info("found distributor ${invoice.get('distributor')}");
				
				
				invoice.setProperty("ponumber", ponumber);
				invoice.setProperty("inputfile", page.getName());
				invoice.setProperty("invoicetotal", it.InvoiceAmount.text());
				invoice.setProperty("invoicenumber", invoicenumber);
				String invoicedate = it.Attributes.TblDate.find {it.Qualifier == "003"}.DateValue.text();
				Date date = ediformat.parse(invoicedate);
				invoice.setProperty("date",DateStorageUtil.getStorageUtil().formatForStorage(date));
				invoice.setProperty("dateadded" ,DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
				String taxAmount = it.Attributes.TblTax.TaxAmount.text();
				invoice.setProperty("taxamount", taxAmount);
				String taxType = it.Attributes.TblTax.TaxType.text();
				String taxFedID = it.Attributes.TblTax.TaxID.text();
				invoice.setProperty("provtaxamount", taxAmount);
				invoice.setProperty("fedtaxid", taxFedID);
				
				invoice.setProperty("shipping",  it.Attributes.TblAllowCharge.AllowChargeAmount.text());
				String waybill = it.Attributes.TblReferenceNbr.find{it.Qualifier == "WY"}.ReferenceNbr.text();
				Shipment shipment = null;
				if(waybill && order!=null){
				    shipment = new Shipment();
					shipment.setProperty("waybill", waybill);
					shipment.setProperty("courier", "NOT PROVIDED");
					shipment.setProperty("shipdate", DateStorageUtil.getStorageUtil().formatForStorage(date));
				}
				
				it.InvoiceDetail.each{
					Data invoiceitem = itemsearcher.createNewData();
					items.add(invoiceitem);
					invoiceitem.setId(itemsearcher.nextId());
					invoiceitem.setProperty("invoiceid", invoice.getId());
					invoiceitem.setProperty("line", it.LineItemNumber.text());
					String quantity = it.Quantity.text();
					invoiceitem.setProperty("quantity", quantity);
					String vendorCode = it.Attributes.TblReferenceNbr.find{it.Qualifier == "VN"}.ReferenceNbr.text();
					String productName = it.Attributes.TblTextMessage.find{it.Qualifier == "08"}.TextMessage.text();
					Product product = null;
					CartItem cartItem = null;
					if (order!=null){
						cartItem = order.getCartItemByProductProperty("manufacturersku", vendorCode);
						if (cartItem!=null){
							product = cartItem.getProduct();
							invoiceitem.setProperty("productid",product.getId());
						} else {
							invoiceitem.setProperty("notes", "Could not find product : ${vendorCode}");
						}
					}
					
					String itemamount = it.Attributes.TblAmount.find {it.Qualifier == "LI"}.Amount.text()
					invoiceitem.setProperty("price", it.ExtendedAmount.text());
					invoiceitem.setProperty("unitprice", itemamount);
					invoiceitem.setProperty("descrip", productName);
					invoiceitem.setProperty("vendorcode", vendorCode);
					invoiceitem.setProperty("UPC", it.Attributes.TblReferenceNbr.find {it.Qualifier == "UP"}.ReferenceNbr.text());
					//Check to see if we need to handle shipping
					if(shipment != null){
						if (product!=null){
							if (cartItem!=null && cartItem.getSku()!=null){
								ShipmentEntry entry = new ShipmentEntry();
								entry.setSku(cartItem.getSku());
								entry.setQuantity(Integer.parseInt(quantity));
								shipment.addEntry(entry);
								order.addShipment(shipment);
								order.setProperty("shippingstatus", order.isFullyShipped() ? "shipped" : "partialshipped");
								store.saveOrder(order);
								
								//append to order history
								appendShippingNoticeToOrderHistory(inReq,order,shipment.get("waybill"));
								if (order.isFullyShipped()){
									appendFullyShippedNoticeToOrderHistory(inReq,order);
								}
								
							} else {
								log.error("Warning: Unable to find ${product.getId()} in order ${order.getId()} ($cartItem)");
							}
						}//don't need to log this case: already logged error in notes above
					}
				}
				if (order != null){
					invoice.setProperty("orderid",order.getId());
					invoice.setProperty("invoicestatus", "new");
				} else {
					invoice.setProperty("invoicestatus", "error");
					invoice.setProperty("notes", "Preprocessing error: order ${ponumber} cannot be found on the system");
				}
				invoicesearcher.saveData(invoice, null);
				itemsearcher.saveAllData(items, null);
				
				//append to to order history
				appendInvoiceReceivedToOrderHistory(inReq,order,invoice);
				
				if (order!=null && !emaillist.isEmpty())
				{
					prepareEmailAndSend(context,emaillist,invoice,order);
				}
				
				
			}
			
			String destinationFile = processedFolder + page.getName();
			Page destination = pageManager.getPage(destinationFile);
			pageManager.movePage(page, destination);
		}
	}
	
	log.info("---- Finished Importing EDI Invoice ----");
}

public void prepareEmailAndSend(WebPageRequest inReq, List emails, Data invoice, Order order)
{
	log.info("preparing email for $emails");
	MediaArchive archive = inReq.getPageValue("mediaarchive");
	inReq.putPageValue("id", invoice.getId());
	inReq.putPageValue("order", order);
	inReq.putPageValue("distributorid", invoice.get("distributor"));
	String templatePage = "/ecommerce/views/modules/invoice/workflow/invoice-notification.html";
	String subject = "INVOICE has been generated: " + invoice.get("invoicenumber");
	sendEmail(archive, inReq, emails, templatePage, subject);
	log.info("Email sent to Store Admins");
}


protected void sendEmail(MediaArchive archive, WebPageRequest context, List inEmailList, String templatePage, String inSubject)
{
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

protected TemplateWebEmail getMail(MediaArchive archive)
{
	PostMail mail = (PostMail)archive.getModuleManager().getBean( "postMail");
	return mail.getTemplateWebEmail();
}

protected appendInvoiceReceivedToOrderHistory(WebPageRequest inReq,Order order, Data invoice)
{
	//record this in order history
	MediaArchive archive = inReq.getPageValue("mediaarchive");
	WebEvent event = new WebEvent();
	event.setSearchType("detailedorderhistory");
	event.setCatalogId(archive.getCatalogId());
	event.setProperty("applicationid", inReq.findValue("applicationid"));
	event.setOperation("orderhistory/appendorderhistory");
	event.setProperty("orderid", order.getId());
	event.setProperty("type","automatic");
	event.setProperty("state","invoicereceived");
	event.setProperty("invoiceid", invoice.getId());
	archive.getMediaEventHandler().eventFired(event);
}

protected appendShippingNoticeToOrderHistory(WebPageRequest inReq, Order order, String waybill)
{
	//record this in order history
	MediaArchive archive = inReq.getPageValue("mediaarchive");
	WebEvent event = new WebEvent();
	event.setSearchType("detailedorderhistory");
	event.setCatalogId(archive.getCatalogId());
	event.setProperty("applicationid", inReq.findValue("applicationid"));
	event.setOperation("orderhistory/appendorderhistory");
	event.setProperty("orderid", order.getId());
	event.setProperty("type","automatic");
	event.setProperty("state","shippingnoticereceived");
	event.setProperty("shipmentid", waybill);
	archive.getMediaEventHandler().eventFired(event);
}

protected appendFullyShippedNoticeToOrderHistory(WebPageRequest inReq, Order order)
{
	//record this in order history
	MediaArchive archive = inReq.getPageValue("mediaarchive");
	WebEvent event = new WebEvent();
	event.setSearchType("detailedorderhistory");
	event.setCatalogId(archive.getCatalogId());
	event.setProperty("applicationid", inReq.findValue("applicationid"));
	event.setOperation("orderhistory/appendorderhistory");
	event.setProperty("orderid", order.getId());
	event.setProperty("type","automatic");
	event.setProperty("state","fullyshipped");
	archive.getMediaEventHandler().eventFired(event);
}

doImport();