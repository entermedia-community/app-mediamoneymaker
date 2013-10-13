package edi

import java.text.SimpleDateFormat

import org.dom4j.Element
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.util.DateStorageUtil

import com.openedit.OpenEditException
import com.openedit.WebPageRequest
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

	XmlUtil util =  new XmlUtil();
	SimpleDateFormat ediformat = new SimpleDateFormat("ddMMyyyy");


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

			Element inputfile =  util.getXml(page.getInputStream(), "UTF-8");

			def INVOICE = new XmlSlurper().parse(page.getReader());
			String distributor = INVOICE.Attributes.TblEntityID.find {it.Qualifier == "GSSND"}.EntityValue.text();
			Data DISTRIB = archive.getSearcherManager().getData(archive.getCatalogId(), "distributor", distributor);


			INVOICE.InvoiceGroup.each{
				def String PO = it.InvoiceHeader.Attributes.TblReferenceNbr.find {it.Qualifier == "PO"}.ReferenceNbr.text();


				Data invoice = invoicesearcher.createNewData();
				Data order = null;
				if(PO){
					order = mediaarchive.getData("storeOrder", PO);
				}
				//				if(order == null){
				//					throw new OpenEditException("Error importing file");
				//				}
				invoice.setId(invoicesearcher.nextId());
				invoice.setProperty("distributor", distributor);
				String invoicenumber = it.InvoiceHeader.InvoiceNumber.text();
				invoice.setProperty("invoicetotal", it.InvoiceHeader.InvoiceAmount.text());
				invoice.setProperty("invoicenumber", it.InvoiceHeader.InvoiceNumber.text());
				String invoicedate = it.InvoiceHeader.Attributes.TblDate.find {it.Qualifier == "003"}.DateValue.text();
				Date date = ediformat.parse(invoicedate);
				invoice.setProperty("date" ,DateStorageUtil.getStorageUtil().formatForStorage(date));
				invoice.setProperty("dateadded" ,DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
//				taxAmount = it.Attributes.TblTax.TaxAmount.text();
//				invoice.setProperty("taxamount", taxAmount);
//				taxType = it.Attributes.TblTax.TaxType.text();
//				taxFedID = it.Attributes.TblTax.TaxID.text();
//				if (taxType == "GS") {
//					invoice.setProperty("fedtaxamount", taxAmount);
//					invoice.setProperty("fedtaxid", taxFedID);
//				} else {
//					invoice.setProperty("provtaxamount", taxAmount);
//				}
//				invoice.setProperty("shipping",  it.Attributes.TblAllowCharge.AllowChargeAmount.text());

				it.InvoiceHeader.InvoiceDetail.each{
					Data invoiceitem = itemsearcher.createNewData();
					invoiceitem.setId(itemsearcher.nextId());
					invoiceitem.setProperty("line", it.LineItemNumber.text());
					invoiceitem.setProperty("quantity", it.Quantity.text());
					vendorCode = it.Attributes.TblReferenceNbr.find {it.Qualifier == "VN"}.ReferenceNbr.text();
					if(vendorCode != null){
						log.info("Found vendor code ${vendorCode}")
					}
					cartItem = order.getCartItemByProductSku(vendorCode);
					product = cartItem.getProduct();
					if (product != null) {
						invoiceitem.setProperty("productid", product.getId());
					}
					invoiceitem.setProperty("vendorcode", vendorCode);
					invoiceitem.setProperty("UPC", it.Attributes.TblReferenceNbr.find {it.Qualifier == "UP"}.ReferenceNbr.text());

				}



			}



		}



	}
}



doImport();