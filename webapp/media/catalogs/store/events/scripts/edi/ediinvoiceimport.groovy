package edi

import java.text.SimpleDateFormat

import org.dom4j.Element
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.util.DateStorageUtil

import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.Page
import com.openedit.page.manage.PageManager
import com.openedit.util.XmlUtil



public void doImport() {

	//		MediaUtilities media = new MediaUtilities();
	//		media.setContext(context);

	WebPageRequest inReq = context;
	MediaArchive archive = inReq.getPageValue("mediaarchive");
	String catalogID = archive.getCatalogId();

	SearcherManager manager = archive.getSearcherManager();
	Searcher userprofilesearcher = archive.getSearcher("userprofile");
	Searcher invoicesearcher = archive.getSearcher("invoice");
	Searcher itemsearcher = archive.getSearcher("invoiceitem");
	Searcher productsearcher = archive.getSearcher("product");
	Searcher distributorsearcher = archive.getSearcher("distributor");
	
	XmlUtil util =  new XmlUtil();
	SimpleDateFormat ediformat = new SimpleDateFormat("yyyyMMdd");


	log.info("---- START Import EDI Invoice ----");

	def String SEARCH_FIELD = "";
	//Read the production value
	boolean production = Boolean.parseBoolean(context.findValue('productionmode'));

	// Get XML File
	//String fileName = "export-" + this.distributorName.replace(" ", "-") + ".csv";
	String invoiceFolder = "/WEB-INF/data/" + archive.getCatalogId() + "/incoming/invoices/";
	PageManager pageManager = archive.getPageManager();
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
			if(page.getName().contains("DIH")){
				log.info("Starting this invoice");
			}
			
			Element inputfile =  util.getXml(page.getInputStream(), "UTF-8");

			def INVOICE = new XmlSlurper().parse(page.getReader());
			String distributor = INVOICE.Attributes.TblEntityID.find {it.Qualifier == "GSSND"}.EntityValue.text();
			Data DISTRIB = distributorsearcher.searchByField("idcode", distributor);
			

			INVOICE.InvoiceGroup.InvoiceHeader.each{
				ArrayList items = new ArrayList();
				def String PO = it.Attributes.TblReferenceNbr.find {it.Qualifier == "PO"}.ReferenceNbr.text();
				
				String invoicenumber = it.InvoiceNumber.text()
				Data invoice = invoicesearcher.searchByField("invoicenumber", invoicenumber);
				if(invoice == null){
				 invoice = invoicesearcher.createNewData();
				} else{
				invoice = invoicesearcher.searchById(invoice.getId());
				}
				Data order = null;
				if(PO){
					order = mediaarchive.getData("storeOrder", PO);
				}
				//				if(order == null){
				//					throw new OpenEditException("Error importing file");
				//				}
				invoice.setId(invoicesearcher.nextId());
				invoice.setProperty("terms", it.Attributes.TblTermsOfSale.TermsDescription.text());
				String termsdate = it.Attributes.TblTermsOfSale.NetDueDate.text();
				if(termsdate != null){
					Date date = ediformat.parse(termsdate);
					invoice.setProperty("termsdate", DateStorageUtil.getStorageUtil().formatForStorage(date) );
				}
				
				
				if(DISTRIB != null){
				invoice.setProperty("distributor", DISTRIB.getId());
				} else{
				invoice.setProperty("distributor","NOT FOUND: ${distributor}");
				}
				invoice.setProperty("ponumber", PO);
				invoice.setProperty("inputfile", page.getName());
				invoice.setProperty("invoicetotal", it.InvoiceAmount.text());
				invoice.setProperty("invoicenumber", invoicenumber);
				String invoicedate = it.Attributes.TblDate.find {it.Qualifier == "003"}.DateValue.text();
				Date date = ediformat.parse(invoicedate);
				invoice.setProperty("date" ,DateStorageUtil.getStorageUtil().formatForStorage(date));
				invoice.setProperty("dateadded" ,DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
				taxAmount = it.Attributes.TblTax.TaxAmount.text();
				invoice.setProperty("taxamount", taxAmount);
				taxType = it.Attributes.TblTax.TaxType.text();
				taxFedID = it.Attributes.TblTax.TaxID.text();
				if (taxType == "GS") {
					invoice.setProperty("fedtaxamount", taxAmount);
					invoice.setProperty("fedtaxid", taxFedID);
				} else {
					invoice.setProperty("provtaxamount", taxAmount);
				}
				invoice.setProperty("shipping",  it.Attributes.TblAllowCharge.AllowChargeAmount.text());

				it.InvoiceDetail.each{
					Data invoiceitem = itemsearcher.createNewData();
					items.add(invoiceitem);
					invoiceitem.setId(itemsearcher.nextId());
					invoiceitem.setProperty("invoiceid", invoice.getId());
					invoiceitem.setProperty("line", it.LineItemNumber.text());
					invoiceitem.setProperty("quantity", it.Quantity.text());
					String vendorCode = it.Attributes.TblReferenceNbr.find {it.Qualifier == "VN"}.ReferenceNbr.text();
					String productName = it.Attributes.TblTextMessage.find {it.Qualifier == "08"}.TextMessage.text();
					
					if(vendorCode != null){
						log.info("Found vendor code ${vendorCode}")
					}
					SearchQuery q = productsearcher.createSearchQuery();
					q.addExact("manufacturersku", vendorCode);
					if(DISTRIB != null){
						q.addExact("distributor", DISTRIB.getId());
					}
					HitTracker hits = productsearcher.search(q);
					if(hits.size() == 0){
						invoiceitem.setProperty("notes", "Could not find product : ${vendorCode}");

					}
					else if(hits.size() == 1){
						Data hit = hits.get(0);
						invoiceitem.setProperty("productid", hit.id);
					}
					else{
						Data hit = hits.get(0);
						invoiceitem.setProperty("productid", hit.id);
						invoiceitem.setProperty("notes", "Found multiple matches for this product.  Used first.");
					}
//					cartItem = order.getCartItemByProductSku(vendorCode);
//					product = cartItem.getProduct();
//					if (product != null) {
//						invoiceitem.setProperty("productid", product.getId());
//					}
					
					String itemamount = it.Attributes.TblAmount.find {it.Qualifier == "LI"}.Amount.text()
					
					invoiceitem.setProperty("price", it.ExtendedAmount.text());
					invoiceitem.setProperty("unitprice", itemamount);
					
					invoiceitem.setProperty("descrip", productName);
					invoiceitem.setProperty("vendorcode", vendorCode);
					invoiceitem.setProperty("UPC", it.Attributes.TblReferenceNbr.find {it.Qualifier == "UP"}.ReferenceNbr.text());

				}

				invoicesearcher.saveData(invoice, null);
				itemsearcher.saveAllData(items, null);

			}



		}



	}
}



doImport();