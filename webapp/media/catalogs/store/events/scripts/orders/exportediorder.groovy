package orders;

import groovy.xml.MarkupBuilder

import java.text.SimpleDateFormat

import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.repository.filesystem.StringItem
import org.openedit.store.CartItem
import org.openedit.store.Product
import org.openedit.store.Store
import org.openedit.store.customer.Address
import org.openedit.store.orders.Order

import com.openedit.BaseWebPageRequest
import com.openedit.OpenEditException
import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker
import com.openedit.page.Page
import com.openedit.page.manage.PageManager

public class ExportEdiOrder extends EnterMediaObject {

	public void init() {

		log.info("PROCESS: ExportEdiOrder.init()");

		WebPageRequest inReq = context;

		MediaArchive archive = inReq.getPageValue("mediaarchive");
		SearcherManager manager = archive.getSearcherManager();
		boolean production = Boolean.parseBoolean(context.findValue('productionmode'));

		String catalogid = archive.getCatalogId();
		PageManager pageManager = archive.getPageManager();

		//Create Searcher Object
		Searcher productsearcher = manager.getSearcher(archive.getCatalogId(), "product");
		Searcher ordersearcher = manager.getSearcher(archive.getCatalogId(), "storeOrder");
		///Searcher itemsearcher = manager.getSearcher(archive.getCatalogId(), "rogers_order_item");
		Searcher storesearcher = manager.getSearcher(archive.getCatalogId(), "store");
		Searcher distributorsearcher = manager.getSearcher(archive.getCatalogId(), "distributor");

		//Get proper FTP info from Parameter
		String ftpID = "";
		String ftpIDProd = inReq.findValue('ftpidprod');
		String ftpIDTest = inReq.findValue('ftpidtest');
		if (production) {
			ftpID = ftpIDProd;
			if (ftpID == null) {
				ftpID = "104";
			} else if (ftpID.isEmpty()) {
				ftpID = "104";
			}
		} else {
			ftpID = ftpIDTest;
			if (ftpID == null) {
				ftpID = "103";
			} else if (ftpID.isEmpty()) {
				ftpID = "103";
			}
		}
		///////////////////////
		// FTPID OVERRIDE FOR TESTING
		///////////////////////
		ftpID = "104";
		///////////////////////

		String inMsg = "";
		Store store = null;
		try {
			store  = getContext().getPageValue("store");
			if (store != null) {
				log.info("Store loaded");
			} else {
				inMsg = "ERROR: Could not load store";
				throw new Exception(inMsg);
			}
		}
		catch (Exception e) {
			inMsg = "Exception thrown:\n";
			inMsg += "Local Message: " + e.getLocalizedMessage() + "\n";
			inMsg += "Stack Trace: " + e.getStackTrace().toString();;
			log.info(inMsg);
			throw new OpenEditException(inMsg);
		}

		int orderCount = 0;

		HitTracker orderList = ordersearcher.getAllHits();
		for (Iterator orderIterator = orderList.iterator(); orderIterator.hasNext();) {

			Data currentOrder = orderIterator.next();

			Order order = ordersearcher.searchById(currentOrder.getId());
			if (order == null) {
				throw new OpenEditException("Invalid Order");
			}
			String orderStatus = order.get("orderstatus");
			if (orderStatus == "authorized") {
				String ediStatus = order.get("edistatus");
				if (ediStatus == null || ediStatus.equals("open")) {

					orderCount++;
					String orderid = order.getId();

					SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
					HitTracker distributorList = distributorsearcher.getAllHits();
					List generatedfiles = new ArrayList();

					//Search through each distributor
					for (Iterator distribIterator = distributorList.iterator(); distribIterator.hasNext();)
					{

						//Get all of the hits and data for searching
						Data distributor = distribIterator.next();

						if (!Boolean.parseBoolean(distributor.get("useedi"))) {
							continue;
						}

						boolean includedistributor = false;
						for(Iterator i = order.getItems().iterator(); i.hasNext();){
							CartItem item = i.next();
							if(distributor.getId().equals(item.getProduct().getProperty("distributor"))) {
								includedistributor = true;
								continue;
							}
						}

						if (includedistributor)
						{
							log.info("Distributor: " + distributor.name);
							log.info("Processing Order ID: " + currentOrder.getId());

							//Iterate through each distributor

							//Create the XML Writer object
							def writer = new StringWriter();
							def xml = new MarkupBuilder(writer);
							xml.'PurchaseOrder'()
							{
								Attributes()
								populateGroup(xml, storesearcher,  distributor, log, order)
							}

							if (validateXML(writer, catalogid))
							{
								// xml generation
								String fileName = currentOrder.getId() + "-export-" + distributor.name.replace(" ", "-") + ".xml"
								Page page = pageManager.getPage("/WEB-INF/data/${catalogid}/orders/exports/${orderid}/${fileName}");

								//Get the FTP Info
								Data ftpInfo = getFtpInfo(context, catalogid, ftpID);
								if (ftpInfo == null) {
									throw new OpenEditException("Cannot get FTP Info using ${ftpID}");
								}

								//Generate EDI Header
								String ediHeader = generateEDIHeader(production, ftpInfo, distributor);

								//Create the output of the XML file
								StringBuffer bufferOut = new StringBuffer();
								bufferOut.append(ediHeader)
								bufferOut.append(writer);
								page.setContentItem(new StringItem(page.getPath(), bufferOut.toString(), "UTF-8"));

								//Write out the XML page.
								pageManager.putPage(page);
								inMsg = fileName + " has been validated and created successfully.";
								log.info(inMsg);
								generatedfiles.add(inMsg);

								order.setProperty("edistatus", "generated");
								store.getOrderArchive().saveOrder(store, order);
								ordersearcher.saveData(order, inReq.getUser());
								inMsg = "Order (" + order.getId() + ") has been updated.";
								log.info(inMsg);

							} else {
								throw new OpenEditException("The XML did not validate.");
							}

						} // end if numDistributors
					} // end distribIterator LOOP
				} else {
					inMsg = "Order Passed (" + order.getId() + "): EDIStatus is " + ediStatus;
					log.info(inMsg);
				}
			} else {
				inMsg = "Order Passed (" + order.getId() + "): Status is " + orderStatus;
				log.info(inMsg);
			}
		}
		if (orderCount == 0) {
			inMsg = "ORDERS: No orders to currently process.";
			log.info(inMsg);
		}
	}

	private void populateGroup(xml, Searcher storesearcher,  Data distributor, log, Order order) {

		if (order == null) {
			throw new OpenEditException("Invalid Order (populateGroup) (" + orderid + ")");
		}
		log.info("orderid: " + order.getId());
		xml.POGroup()
		{
			populateHeader(xml, distributor, order)
		}
	}

	private void populateHeader(xml, Data distributor,Order order) {
		boolean production = Boolean.parseBoolean(context.findValue('productionmode'));

		BaseWebPageRequest inReq = context;

		MediaArchive archive = inReq.getPageValue("mediaarchive");
		SearcherManager manager = archive.getSearcherManager();

		if (order == null) {
			throw new OpenEditException("Invalid Order (populateHeader)");
		}

		xml.POHeader()
		{
			Attributes()
			{
				TblAddress()
				{
					AddressType("VN")
					AddressName1(distributor.fullname)
					AddressIDQual(distributor.idQual)
					AddressIDCode(distributor.idcode)
				}
				TblAddress()
				{
					Address shipping = order.getShippingAddress();
					if (shipping == null) {
						throw new OpenEditException("Invalid Address (populateHeader) (" + order.getId() + ")");
					}
					AddressType("ST")
					log.info("Name: " + shipping.getName())
					AddressName1(shipping.getName())
					AddressIDQual("92")
					log.info("ID: " + shipping.getId())
					AddressIDCode(shipping.getId())
					AddressLine1(shipping.getAddress1())
					AddressLine2(shipping.getAddress2())
					AddressCity(shipping.getCity())
					AddressState(shipping.getState())
					AddressPostalCode(shipping.getZipCode())
					AddressCountry(getCountryCode(shipping.getCountry()))
				}
				//Write Billing Information
				TblAddress()
				{
					// if order is in accepted state (ie., client has requested an invoice)
					// then provide the customer's billing address 
					// otherwise provide AREA address
					
					Store store  = context.getPageValue("store");
					if (store.usesBillMeLater() && order.getOrderStatus().getId().equals("accepted")){
						
						Address billing = order.getBillingAddress();
						if (billing == null) {
							throw new OpenEditException("Invalid Billing Address (populateHeader) (" + order.getId() + ")");
						}
						AddressType("BT")
						AddressName1(billing.getName())
						AddressIDQual("92")//not sure about this
						AddressIDCode(billing.getId())
						AddressLine1(billing.getAddress1())
						AddressLine2(billing.getAddress2())
						AddressCity(billing.getCity())
						AddressState(billing.getState())
						AddressPostalCode(billing.getZipCode())
						AddressCountry(getCountryCode(billing.getCountry()))// CA
						
					} else {
						AddressType("BT")
						AddressName1("Area")
						AddressIDQual("ZZ")
						if(production) {
							AddressIDCode("AREACOMM")
						} else{
							AddressIDCode("AREACOMMT")
						}
						AddressLine1("Area Marketing")
						AddressLine2("1 Hurontario Street, Suite 220")
						AddressCity("Mississauga")
						AddressState("ON")
						AddressPostalCode("L5G 0A3")
						AddressCountry("CA")
					}
				}
				TblAmount()
				{
					Qualifier("_TLI")
					Amount(order.getItems().size())
				}
				TblAVP()
				{
					Attribute("BYCUR")
					Value("CAD")
				}
				TblAVP()
				{
					Attribute("_TM")
					Value("M")
				}
				TblDate()
				{
					Qualifier("004")
					Date now = new Date();
					SimpleDateFormat tableFormat = new SimpleDateFormat("yyyyMMdd");
					DateValue(tableFormat.format(now))
					now = null;
				}
				TblDate()
				{
					Qualifier("002")
					Date now = new Date();
					SimpleDateFormat tableFormat = new SimpleDateFormat("yyyyMMdd");
					DateValue(tableFormat.format(now))
					now = null;
				}
				TblReferenceNbr()
				{
					Qualifier("STCTL")
					ReferenceNbr("428003")
				}
				TblReferenceNbr()
				{
					Qualifier("PO")
					ReferenceNbr(order.getId())
				}

			} // end Attributes
			def orderCount = 0;
			for (Iterator itemIterator = order.getItems().iterator(); itemIterator.hasNext();)
			{
				CartItem orderItem = itemIterator.next();
				if (orderItem.getProduct().getProperty("distributor") == distributor.getId()) {
					orderCount++
					populateDetail(xml, orderCount, orderItem, order);
				}
			} // End itemIterator loop
		} // end POHeader
	}

	private void populateDetail(xml, int orderCount, CartItem orderItem, Order order) {
		
		String orderstatus = order.get("orderstatus");
		boolean isAccepted = orderstatus!=null && orderstatus.equals("accepted");
		xml.PODetail()
		{
			LineItemNumber(orderCount);
			String productId = orderItem.getProduct().getId();
			QuantityOrdered(orderItem.getQuantity().toString())

			def SEARCH_FIELD = "id";
			Product p = orderItem.getProduct();
			String saleprice = p.get("clearanceprice");
			
			if (saleprice != null && saleprice.toDouble() > 0) {
				UnitPrice(saleprice)
			} else {
				if (isAccepted){
					UnitPrice(p.getYourPrice())
				} else {
					UnitPrice(p.get("rogersprice"))
				}
			}
			UnitOfMeasure("EA")
			Description(orderItem.getProduct().getName())
			Attributes()
			{
				TblReferenceNbr()
				{
					Qualifier("VN")
					ReferenceNbr(orderItem.getProduct().get("manufacturersku"))
				}
				TblReferenceNbr()
				{
					Qualifier("UP")
					ReferenceNbr(orderItem.getProduct().get("upc"))
				}
			}
		}
	}

	private boolean validateXML( StringWriter xml, String catalogid ) {
		boolean result = false;

		String xmlString = xml.toString();

		def String xsdFilename = "/${catalogid}/configuration/xsdfiles/EZL_XMLPO_02_20_12.xsd";
		log.info("xsdFilename: ${xsdFilename}");
		Page page = pageManager.getPage(xsdFilename);
		if (page.exists()) {

			def factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			def schema = factory.newSchema(new StreamSource(page.getReader()));
			def validator = schema.newValidator();
			validator.validate(new StreamSource(new StringReader(xmlString)));
			result = true;

		} else {

			throw new OpenEditException("XSD File does not exist (${xsdFilename}.");

		}

		return result;
	}

	private Data getFtpInfo(context, catalogid, String ftpID) {
		BaseWebPageRequest inReq = context;
		MediaArchive archive = inReq.getPageValue("mediaarchive");
		SearcherManager manager = archive.getSearcherManager();
		Searcher ftpsearcher = manager.getSearcher(catalogid, "ftpinfo");
		Data ftpInfo = ftpsearcher.searchById(ftpID)
		return ftpInfo
	}

	private String generateEDIHeader ( boolean production, Data ftpInfo, Data distributor ){


		String output  = new String();
		output = ftpInfo.headericc;
		output += ftpInfo.headerfiletype;
		output += ftpInfo.headerdoctype.padRight(5);
		output += getSenderMailbox(production).padRight(18);
		output += getReceiverMailbox(distributor, production).padRight(18);
		output += generateDate();
		output += generateTime();
		output += ftpInfo.headerversion;
		output += "".padRight(14);
		output += "\n";
		if (output.length() != 81 ) {
			throw new OpenEditException("EDI Header is not the correct length (${output.length().toString()})");
		}

		log.info("EDI Header: " + output + ":Length:" + output.length());

		return output;
	}

	private String getSenderMailbox( boolean production) {

		String out = "ZZ:";
		if(production) {
			out += "AREACOMM";
		} else{
			out += "AREACOMMT";
		}
		return out;
	}

	private String getReceiverMailbox( Data distributor, boolean production) {

		String out = distributor.headerprefix + ":";
		if(production) {
			out += distributor.headermailboxprod;
		} else{
			out += distributor.headermailboxtest;
		}
		return out;

	}

	private String generateDate() {

		Date now = new Date();
		SimpleDateFormat tableFormat = new SimpleDateFormat("yyyyMMdd");
		String outDate = tableFormat.format(now);
		now = null;
		return outDate;

	}
	private String generateTime() {

		Date now = new Date();
		SimpleDateFormat tableFormat = new SimpleDateFormat("hhmmss");
		String outDate = tableFormat.format(now);
		now = null;
		return outDate;

	}
	private String getCountryCode(String inCountryName) {
		BaseWebPageRequest inReq = context;

		MediaArchive archive = inReq.getPageValue("mediaarchive");
		SearcherManager manager = archive.getSearcherManager();
		Searcher countrysearcher = manager.getSearcher(archive.getCatalogId(), "country");
		Data country = countrysearcher.searchByField("name", inCountryName);
		if (country != null) {
			return country.getId();
		} else {
			return null;
		}
	}
}


logs = new ScriptLogger();
logs.startCapture();

try {
	ExportEdiOrder export = new ExportEdiOrder();
	export.setLog(logs);
	export.setContext(context);
	export.setModuleManager(moduleManager);
	export.setPageManager(pageManager);
	export.init();
}
finally {
	logs.stopCapture();
}
