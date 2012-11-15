package edi;

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.PublishResult

import com.openedit.OpenEditException
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.page.Page
import com.openedit.page.manage.PageManager

public class ImportEDI extends EnterMediaObject {
	
	private static final String LOG_HEADER = " - RECORDS - ";
	private static String strMsg = "";
	private static final String ATTRIBUTES = "Attributes";
	private static final String TBLREFERENCENBR = "TblReferenceNbr";
	private static final String REFERENCENBR = "ReferenceNbr";
	private static final String QUALIFiER = "Qualifier";
	private static final String ASNGROUP = "ASNGroup";
	private static final String ASNHEADER= "ASNHeader";
	private static final String TBLADDRESS = "TblAddress";
	private static final String ADDRESSTYPE = "AddressType";
	private static final String ADDRESSNAME1 = "AddressName1";

	private static final String TBLDATE = "TblDate";
	private static final String DATEVALUE = "DateValue";

	private static final String INVOICEGROUP = "InvoiceGroup";
	private static final String INVOICEHEADER = "InvoiceHeader";
	private static final String INVOICENUMBER = "InvoiceNumber";
	private static final String INVOICEAMOUNT = "InvoiceAmount";

	public PublishResult importEdiXml() {
		
		PublishResult result = new PublishResult();
		result.setComplete(false);
		OutputUtilities output = new OutputUtilities();
		
		// Get XML File
		//String fileName = "export-" + this.distributorName.replace(" ", "-") + ".csv";
		String fileName = "asn_import.xml";
		String catalogid = getMediaArchive().getCatalogId();
		MediaArchive archive = context.getPageValue("mediaarchive");
		
		SearcherManager manager = archive.getSearcherManager();
		boolean production = Boolean.parseBoolean(context.findValue('productionmode'));

		PageManager pageManager = archive.getPageManager();
		Page page = pageManager.getPage("/WEB-INF/data/${catalogid}/orders/imports/${fileName}");
		String realpath = page.getContentItem().getAbsolutePath();
		
		File xmlFIle = new File(realpath);
		if (xmlFIle.exists()) {
			strMsg = output.createTable("Section", "Values", "Status");
			
			//Create the XMLSlurper Object
			def ASN = new XmlSlurper().parse(page.getReader());
			
			//Get the PO details
			def String PO = ASN."${ATTRIBUTES}"."${TBLREFERENCENBR}".find {it."${QUALIFiER}" == "PO"}."${REFERENCENBR}".text();
			if (!PO.isEmpty()) {
				strMsg += output.appendOutMessage("PO Section", PO, "Found");
			} else {
				throw new OpenEditException("ERROR: PO value is blank in ASN.");
			}
			
			//Get Rogers Order ID
			String[] orderInfo = PO.split("-");
			Data order = searchForOrder(manager, archive, orderInfo[0]);
			if (order != null) {
				strMsg += output.appendOutMessage("Rogers Order ", orderInfo[0], "Found");
			} else {
				throw new OpenEditException("ERROR: Order(" + orderInfo[0] + ") was not found from ASN.");
			}
			if (order.as400po.equalsIgnoreCase(orderInfo[1])) {
				strMsg += output.appendOutMessage("Purchase Order ", orderInfo[1], "Found");
			} else {
				throw new OpenEditException("ERROR: Purchase Order(" + orderInfo[1] + ") does not match AS400 PO number (" + order.as400po + ") in order (" + orderInfo[0] + ")");
			}

			//Get the distributor
			def String GSSND = ASN."${ATTRIBUTES}"."${TBLREFERENCENBR}".find {it."${QUALIFiER}" == "GSSND"}."${REFERENCENBR}".text();
			strMsg += output.appendOutMessage("GSSND", GSSND, "Found");
			Data distributor = searchForDistributor(manager, archive, GSSND, production);
			if (distributor != null) {
				strMsg += output.appendOutMessage("Distributor", distributor.name, "Found");
			} else {
				throw new OpenEditException("ERROR: Distributor value is blank in ASN.");
			}
			
			boolean moreLevels = true;
			while (moreLevels) {

				/* This works - This gets the first level store info */				
				def String storeInfo = ASN."${ASNGROUP}"."${ASNHEADER}"."${ATTRIBUTES}"."${TBLADDRESS}".find {it."${ADDRESSTYPE}" == "ST"}."${ADDRESSNAME1}".text();
				def String[] storeValue = storeInfo.split("-");
				Data store = searchForStore(manager, archive, storeValue[0]);
				if (store != null) {
					strMsg += output.appendOutMessage("Store", store.name, "Found");
				} else {
					throw new OpenEditException("ERROR: Store not found!");
				}

				/*
				 * THIS IS WHAT NEEDS TO BE WORKING BUT ISN'T
				 */
//				def List allStoresInASN = ASN."${ASNGROUP}"."${ASNHEADER}"
//					."${ATTRIBUTES}"."${TBLADDRESS}"
//					.findAll {it."${ADDRESSTYPE}" == "ST"}*."${ADDRESSNAME1}"*.text();
				//def allStoresInASN = ASN.'**'.findAll {it."${ADDRESSTYPE}" == "ST"};
				
				def List allStoresInASN = ASN.depthFirst().findAll { !it.childNodes() && it."${ADDRESSTYPE}"};
				log.info("allStores size: " + allStoresInASN.size());

//				for (String storeInfo : allStoresInASN ) {
//					
//					def String[] storeValue = storeInfo.split("-");
//					Data store = searchForStore(manager, archive, storeValue[0]);
//					if (store != null) {
//						strMsg += output.appendOutMessage("Store", store.name, "Found");
//					} else {
//						throw new OpenEditException("ERROR: Store not found!");
//					}
//				}
				
				def String strNumChildren = ASN.ASNGroup.ASNHeader.NumberOfChildren.text();
				def int intNumberOfChildren = Integer.parseInt(strNumChildren);
				strMsg += output.appendOutMessage("NumberOfChildren", intNumberOfChildren.toString(), "");
				if (intNumberOfChildren > 0) {
					//
					moreLevels = false;
				} else {
					moreLevels = false;
				}
			}
			
			strMsg += output.finishTable();
			result.setCompleteMessage(strMsg);
			result.setComplete(true);
			//extractTestXML(page, log, result);
		} else {
			result.setErrorMessage(realpath + " does not exist!");
		}
		return result;
	}
	
	private Data searchForOrder( SearcherManager manager, 
		MediaArchive archive, String searchForName ) {
		
		Searcher ordersearcher = manager.getSearcher(archive.getCatalogId(), "rogers_order");
		Data rogersOrder = ordersearcher.searchById(searchForName);
		return rogersOrder;
		
	}
	
	private Data searchForDistributor( SearcherManager manager, 
		MediaArchive archive, String searchForName, Boolean production ) {
		
		String SEARCH_FIELD = "";
		if (production == true){
			SEARCH_FIELD = "headermailboxprod";
		} else {
			SEARCH_FIELD = "headermailboxtest";
		}
		Searcher distributorsearcher = manager.getSearcher(archive.getCatalogId(), "distributor");
		Data targetDistributor = distributorsearcher.searchByField(SEARCH_FIELD, searchForName);
		
		return targetDistributor;		
	}

	private Data searchForStore( SearcherManager manager, 
		MediaArchive archive, String searchForName ) {
		
		String SEARCH_FIELD = "store";
		Searcher storesearcher = manager.getSearcher(archive.getCatalogId(), "store");
		Data rogersStore = storesearcher.searchByField(SEARCH_FIELD, searchForName);
		
		return rogersStore;
		
	}
	
	private extractTestXML(Page page, ScriptLogger log, PublishResult result) {
		def records = new XmlSlurper().parse(page.getReader());
		def allRecords = records.car;
		log.info(LOG_HEADER + allRecords.size());

		StringWriter output  = new StringWriter();
		output.append("<table cellpadding=10 cellspacing=10 border=0>\n");
		output.append("<th><tr>\n");

		output.append("<td><strong>MODEL</strong></td>\n");
		output.append("<td><strong>MAKE</strong></td>\n");
		output.append("<td><strong>COUNTRY</strong></td>\n");
		output.append("</th></th>\n");

		//Itterate through each record
		for (int ctr=0; ctr < allRecords.size(); ctr++) {

			def record = records.car[ctr];
			def String carModel = record.@model.text();
			def String carMake = record.@make.text();
			def String carCountry = record.country.text();

			output.append("<tbody><tr>\n");
			List detailRow = new ArrayList();
			output.append("<td>" + carModel + "</td>\n");
			output.append("<td>" + carMake + "</td>\n");
			output.append("<td>" + carCountry + "</td>\n");
			output.append("</tr></tbody>\n");
		}
		output.append("</table>\n");
		result.setCompleteMessage(output.toString());
		result.setComplete(true)
	}
}
PublishResult result = new PublishResult();
result.setComplete(false);

logs = new ScriptLogger();
logs.startCapture();

try {
	
	ImportEDI importEDI = new ImportEDI();
	importEDI.setLog(logs);
	importEDI.setContext(context);
	importEDI.setPageManager(pageManager);

	result = importEDI.importEdiXml();
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
