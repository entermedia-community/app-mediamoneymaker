package edi;

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.PublishResult
import org.openedit.event.WebEvent

import com.openedit.OpenEditException
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.page.Page
import com.openedit.page.manage.PageManager

public class ImportEDIASN extends EnterMediaObject {
	
	private static final String LOG_HEADER = " - RECORDS - ";
	private static final String FOUND = "Found"
	private static final String NOT_FOUND = "NOT FOUND!";
	private static String strMsg = "";
	
	public PublishResult importEdiXml() {
		
		PublishResult result = new PublishResult();
		result.setComplete(false);
		Util output = new Util();
		
		// Get XML File
		String fileName = "CJVUW8TH.XML"; //HARD-CODED ASN FILE
		
		String catalogid = getMediaArchive().getCatalogId();
		MediaArchive archive = context.getPageValue("mediaarchive");
		
		SearcherManager manager = archive.getSearcherManager();
		boolean production = Boolean.parseBoolean(context.findValue('productionmode'));

		PageManager pageManager = archive.getPageManager();
		Page page = pageManager.getPage("/WEB-INF/data/${catalogid}/incoming/asn/${fileName}");
		String realpath = page.getContentItem().getAbsolutePath();
		
		File xmlFIle = new File(realpath);
		if (xmlFIle.exists()) {
			strMsg = output.createTable("Section", "Values", "Status");
			
			//Create the XMLSlurper Object
			def ASN = new XmlSlurper().parse(page.getReader());
			
			//Get the distributor
			def String GSSND = ASN.Attributes.TblReferenceNbr.find {it.Qualifier == "GSSND"}.ReferenceNbr.text();
			strMsg += output.appendOutMessage("GSSND", GSSND, FOUND);
			Data distributor = searchForDistributor(manager, archive, GSSND, production);
			if (distributor != null) {
				strMsg += output.appendOutMessage("Distributor", distributor.name, FOUND);
			} else {
				throw new OpenEditException("ERROR: Distributor value is blank in ASN.");
			}

			def ASNHeaders = ASN.ASNGroup.depthFirst().grep{
				it.name() == 'ASNHeader';
			}

			ASNHeaders.each {

				//PO
				def String purchaseOrder = it.Attributes.TblReferenceNbr.find {it.Qualifier == "PO"}.ReferenceNbr.text();
				if (!purchaseOrder.isEmpty()) {
					String[] orderInfo = purchaseOrder.split("-");
					Data order = searchForOrder(manager, archive, orderInfo[0]);
					if (order != null) {
						strMsg += output.appendOutMessage("Rogers Order ", orderInfo[0], FOUND);
					} else {
						log.info("ERROR: Order(" + orderInfo[0] + ") was not found from ASN.");
						//Create web event to send an email.
						WebEvent event = new WebEvent();
						event.setSearchType("asn_processing");
						event.setCatalogId(catalogid);
						event.setProperty("orderid", orderInfo[0]);
						event.setProperty("ponumber", orderInfo[1]);
						archive.getMediaEventHandler().eventFired(event);
						strMsg += output.appendOutMessage("Rogers Order ", orderInfo[0], NOT_FOUND);
					}
					if (order.as400po.equalsIgnoreCase(orderInfo[1])) {
						strMsg += output.appendOutMessage("Purchase Order ", orderInfo[1], FOUND);
					} else {
						log.info("Purchase Order NOT FOUND!");
						//Create web event to send an email.
						WebEvent event = new WebEvent();
						event.setSearchType("asn_processing");
						event.setCatalogId(catalogid);
						event.setProperty("orderid", orderInfo[0]);
						event.setProperty("ponumber", orderInfo[1]);
						archive.getMediaEventHandler().eventFired(event);
						strMsg += output.appendOutMessage("Purchase Order ", orderInfo[1], NOT_FOUND);
					}
				}

				//ST	
				def String storeInfo = it.Attributes.TblAddress.find {it.AddressType == "ST"}.AddressName1.text();
				if (!storeInfo.isEmpty()) {
					def String[] storeValue = storeInfo.split("-");
					Data store = searchForStore(manager, archive, storeValue[0]);
					if (store != null) {
						strMsg += output.appendOutMessage("Store", store.name, FOUND);
					} else {
						log.info("ERROR: Store not found!(" + storeValue[0] + ")");
						//Create web event to send an email.
						WebEvent event = new WebEvent();
						event.setSearchType("asn_processing");
						event.setCatalogId(catalogid);
						event.setProperty("store", storeValue[0]);
						archive.getMediaEventHandler().eventFired(event);
						strMsg += output.appendOutMessage("Store", store.name, NOT_FOUND);
					}
				}
				
				//SC
				def String courier = it.Attributes.TblEntityID.find {it.Qualifier == "SC"}.EntityValue.text();
				if (!courier.isEmpty()) {
					strMsg += output.appendOutMessage("Courier", courier, FOUND);
				}

				//_BOL
				def String billOfLading = it.Attributes.TblReferenceNbr.find {it.Qualifier == "_BOL"}.ReferenceNbr.text();
				if (!billOfLading.isEmpty()) {
					strMsg += output.appendOutMessage("Bill of Lading", billOfLading, FOUND);
				}
				
				//_PRO
				def String waybill = it.Attributes.TblReferenceNbr.find {it.Qualifier == "_PRO"}.ReferenceNbr.text();
				if (!courier.isEmpty()) {
					strMsg += output.appendOutMessage("Waybill", waybill, FOUND);
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
	
	ImportEDIASN ImportEDIASN = new ImportEDIASN();
	ImportEDIASN.setLog(logs);
	ImportEDIASN.setContext(context);
	ImportEDIASN.setPageManager(pageManager);

	result = ImportEDIASN.importEdiXml();
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
