package edi;

import java.text.SimpleDateFormat

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.PublishResult
import org.openedit.util.DateStorageUtil

import com.openedit.OpenEditException
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.page.Page
import com.openedit.page.manage.PageManager

public class ImportEDIInvoice extends EnterMediaObject {

	private static final String LOG_HEADER = " - RECORDS - ";
	private static final String ADDED = "Added";
	private static String strMsg = "";
	private static final String ATTRIBUTES = "Attributes";
	private static final String TBLREFERENCENBR = "TblReferenceNbr";
	private static final String REFERENCENBR = "ReferenceNbr";
	private static final String QUALIFIER = "Qualifier";
	private static final String ASNGROUP = "ASNGroup";
	private static final String ASNHEADER= "ASNHeader";
	private static final String TBLADDRESS = "TblAddress";

	private static final String TBLDATE = "TblDate";
	private static final String DATEVALUE = "DateValue";

	private static final String ADDRESSTYPE = "AddressType";
	private static final String ADDRESSNAME1 = "AddressName1";

	private static final String INVOICEGROUP = "InvoiceGroup";
	private static final String INVOICEHEADER = "InvoiceHeader";
	private static final String INVOICENUMBER = "InvoiceNumber";
	private static final String INVOICEAMOUNT = "InvoiceAmount";

	public PublishResult importEdiXml() {

		PublishResult result = new PublishResult();
		result.setComplete(false);
		Util output = new Util();

		// Get XML File
		//String fileName = "export-" + this.distributorName.replace(" ", "-") + ".csv";
		String fileName = "invoice_import.xml";
		String catalogid = getMediaArchive().getCatalogId();
		MediaArchive archive = context.getPageValue("mediaarchive");

		SearcherManager manager = archive.getSearcherManager();

		//Create the Invoice Searchers		
		Searcher invoicesearcher = manager.getSearcher(catalogid , "invoice");
		Searcher itemsearcher = manager.getSearcher(catalogid , "invoiceitems");

		//Read the production value
		boolean production = Boolean.parseBoolean(context.findValue('productionmode'));

		PageManager pageManager = archive.getPageManager();
		Page page = pageManager.getPage("/WEB-INF/data/${catalogid}/orders/imports/${fileName}");
		String realpath = page.getContentItem().getAbsolutePath();

		File xmlFIle = new File(realpath);
		if (xmlFIle.exists()) {
			
			strMsg = output.createTable("Section", "Values", "Status");
			
			Data newInvoice = invoicesearcher.createNewData();
			newInvoice.setId(invoicesearcher.nextId());
			newInvoice.setSourcePath(newInvoice.getId());
			strMsg += output.appendOutMessage("Invoice ID", newInvoice.getId(), ADDED);

			//Create the XMLSlurper Object
			def INVOICE = new XmlSlurper().parse(page.getReader());

			//Get the INVOICENUMBER details
			def String invoiceNumber = INVOICE."${INVOICEGROUP}"."${INVOICEHEADER}"."${INVOICENUMBER}".text();
			if (!invoiceNumber.isEmpty()) {
				newInvoice.setProperty("invoicenumber", invoiceNumber);
				strMsg += output.appendOutMessage(INVOICENUMBER, invoiceNumber, ADDED);
			} else {
				throw new OpenEditException("ERROR: " + INVOICENUMBER + " value is blank in Invoice.");
			}

			//Get the INVOICEAMOUNT details
			def String invoiceTotal = INVOICE."${INVOICEGROUP}"."${INVOICEHEADER}"."${INVOICEAMOUNT}".text();
			if (!invoiceTotal.isEmpty()) {
				newInvoice.setProperty("invoicetotal", invoiceTotal);
				strMsg += output.appendOutMessage(INVOICEAMOUNT, invoiceTotal, ADDED);
			} else {
				throw new OpenEditException("ERROR: " + INVOICEAMOUNT + " value is blank in Invoice.");
			}

			//Get the PO details
			def String PO = INVOICE."${INVOICEGROUP}"."${INVOICEHEADER}"."${ATTRIBUTES}"."${TBLREFERENCENBR}".find {it."${QUALIFIER}" == "PO"}."${REFERENCENBR}".text();
			if (!PO.isEmpty()) {
				strMsg += output.appendOutMessage("PO Section", PO, "Found");
			} else {
				throw new OpenEditException("ERROR: PO value is blank in ASN.");
			}
			//Get Rogers Order ID
			String[] orderInfo = PO.split("-");
			Data order = searchForOrder(manager, archive, orderInfo[0]);
			if (order != null) {
				newInvoice.setProperty("orderid", orderInfo[0]);
				strMsg += output.appendOutMessage("Rogers Order ", orderInfo[0], ADDED);
			} else {
				throw new OpenEditException("ERROR: Order(" + orderInfo[0] + ") was not found from ASN.");
			}
			if (order.as400po.equalsIgnoreCase(orderInfo[1])) {
				newInvoice.setProperty("ponumber", orderInfo[1]);
				strMsg += output.appendOutMessage("Purchase Order ", orderInfo[1], ADDED);
			} else {
				throw new OpenEditException("ERROR: Purchase Order(" + orderInfo[1] + ") does not match AS400 PO number (" + order.as400po + ") in order (" + orderInfo[0] + ")");
			}

			/* This works - This gets the first level store info */
			def String storeInfo = INVOICE."${INVOICEGROUP}"."${INVOICEHEADER}"."${ATTRIBUTES}"."${TBLADDRESS}".find {it."${ADDRESSTYPE}" == "ST"}."${ADDRESSNAME1}".text();
			def String[] storeValue = storeInfo.split("-");
			Data store = searchForStore(manager, archive, storeValue[0]);
			if (store != null) {
				newInvoice.setProperty("store", store.id);
				strMsg += output.appendOutMessage("Store(" + store.id + ")", store.name, ADDED);
			} else {
				throw new OpenEditException("ERROR: Store not found!");
			}

			/* This works - This gets the first level store info */
			def String invoiceDate = INVOICE."${INVOICEGROUP}"."${INVOICEHEADER}"."${ATTRIBUTES}"."${TBLDATE}".find {it."${QUALIFIER}" == "003"}."${DATEVALUE}".text();
			Date newDate = parseDate(invoiceDate);
			newInvoice.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(newDate));
			strMsg += output.appendOutMessage("Date", newInvoice.get("date"), ADDED);
			
			newInvoice.setProperty("invoicestatus", "new");

			def allInvoiceDetails = INVOICE.depthFirst().grep{
				it.name() == 'InvoiceDetail';
			}

			allInvoiceDetails.each {

				//Setup Invoice Items
				Data invoiceItem = itemsearcher.createNewData();
				invoiceItem.setId(itemsearcher.nextId());
				invoiceItem.setSourcePath(newInvoice.getId());

				invoiceItem.setProperty("invoiceid", newInvoice.getId());
				
				//Go through each invoice
				def int ctr = it.LineItemNumber.toInteger();
				strMsg += output.appendOutMessage("Line Item Number", ctr.toString(), "");

				String quantity = it.Quantity;
				strMsg += output.appendOutMessage("Quantity", quantity, "");
				invoiceItem.setProperty("quantity", quantity);

				//String extendedAmount = it.ExtendedAmount;
				//strMsg += output.appendOutMessage("ExtendedAmount", extendedAmount, "");

				it.Attributes.TblReferenceNbr.each {
					
					//Find the UPC Code
					if (it.Qualifier.text().equals("UP")) {

						String upcCode = it.ReferenceNbr.text();
						strMsg += output.appendOutMessage("UPC Code", upcCode, ADDED);
						Data product = searchForProduct( manager, archive, upcCode);
						if (product == null) {
							throw new OpenEditException("Product not found from following UPC code: " + upcCode);
						}

						strMsg += output.appendOutMessage("Product Name", product.name, ADDED);
						invoiceItem.setProperty("product", product.getId());

						strMsg += output.appendOutMessage("Price", product.rogersprice.toString(), ADDED);
						invoiceItem.setProperty("price", product.rogersprice);

					}
// PRESENTLY NOT IMPLEMENTED - distributor
//					if (it.Qualifier.text().equals("VN")) {
//
//						String vendorCode = it.ReferenceNbr.text();
//						strMsg += output.appendOutMessage("Vendor", vendorCode, "");
//						Data distributor = searchForDistributor(manager, archive, vendorCode);
//						if (distributor == null) {
//							throw new OpenEditException("Distributor not found from following vendor code: " + vendorCode);
//						}
//						strMsg += output.appendOutMessage("Distributor (" + distributor.id + ")", distributor.name, ADDED);
//						invoiceItem.setProperty("distributor", distributor.id);
//
//					}					
				}
				//add properties and save
				itemsearcher.saveData(invoiceItem, null);
			}
			strMsg += output.appendOutMessage("<a href=\"/ecommerce/views/modules/invoice/index.html\">Invoice (" + newInvoice.getId() + ")</a> has been created!");

			strMsg += output.finishTable();
			result.setCompleteMessage(strMsg);
			result.setComplete(true);
			invoicesearcher.saveData(newInvoice, null);
			//extractTestXML(page, log, result);
		} else {
			result.setErrorMessage(realpath + " does not exist!");
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

	private Data searchForOrder( SearcherManager manager,
	MediaArchive archive, String searchForName ) 
	{

		Searcher ordersearcher = manager.getSearcher(archive.getCatalogId(), "rogers_order");
		Data rogersOrder = ordersearcher.searchById(searchForName);
		return rogersOrder;

	}

	private Data searchForDistributor( SearcherManager manager,
	MediaArchive archive, String searchForName) 
	{

		String SEARCH_FIELD = "idcode";
		Searcher distributorsearcher = manager.getSearcher(archive.getCatalogId(), "distributor");
		Data targetDistributor = distributorsearcher.searchByField(SEARCH_FIELD, searchForName);

		return targetDistributor;
	}

	private Data searchForStore( SearcherManager manager,
	MediaArchive archive, String searchForName ) 
	{

		String SEARCH_FIELD = "store";
		Searcher storesearcher = manager.getSearcher(archive.getCatalogId(), "store");
		Data rogersStore = storesearcher.searchByField(SEARCH_FIELD, searchForName);

		return rogersStore;

	}

	private Data searchForProduct( SearcherManager manager,
	MediaArchive archive, String searchForName ) 
	{

		String SEARCH_FIELD = "upc";
		Searcher productsearcher = manager.getSearcher(archive.getCatalogId(), "product");
		Data product = productsearcher.searchByField(SEARCH_FIELD, searchForName);

		return product;

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
	} else {
		//ERROR: Throw exception
		context.putPageValue("errorout", result.getErrorMessage());
	}
}
finally {
	logs.stopCapture();
}
