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
import org.openedit.store.customer.Address
import org.openedit.store.orders.Order
import org.openedit.store.orders.OrderState

import com.openedit.BaseWebPageRequest
import com.openedit.OpenEditException
import com.openedit.hittracker.HitTracker
import com.openedit.page.Page
import com.openedit.page.manage.PageManager

public void init() {

	BaseWebPageRequest inReq = context;

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

	//Read orderid from the URL
	def orderid = inReq.getRequestParameter("orderid");
	Order order = ordersearcher.searchById(orderid);
	if (order == null) {
		throw new OpenEditException("Invalid Order(" + orderid + ")");
	}

	def String ediID = "";
	//ediID = inReq.getRequestParameter("ediid");

	SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	HitTracker distributorList = distributorsearcher.getAllHits();
	List generatedfiles = new ArrayList();
	//Search through each distributor
	for (Iterator distribIterator = distributorList.iterator(); distribIterator.hasNext();)
	{

		//Get all of the hits and data for searching
		Data distributor = distribIterator.next();
		log.info("Distributor: " + distributor.name);

		if (!Boolean.parseBoolean(distributor.useedi)) {
			continue;
		} else {
			ediID = distributor.getId();
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
			//Iterate through each distributor

			//Create the XML Writer object
			def writer = new StringWriter();
			def xml = new MarkupBuilder(writer);
			xml.'PurchaseOrder'()
			{
				Attributes()
				populateGroup(xml, storesearcher,  distributor, log, order)
			}

			if (validateXML(writer))
			{
				// xml generation
				String fileName = "export-" + distributor.name.replace(" ", "-") + ".xml"
				Page page = pageManager.getPage("/WEB-INF/data/${catalogid}/orders/exports/${orderid}/${fileName}");

				//Get the FTP Info
				Data ftpInfo = getFtpInfo(context, catalogid, ediID);
				if (ftpInfo == null) {
					throw new OpenEditException("Cannot get FTP Info using ${ediID}");
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
				String inMsg = fileName + " has been validated and created successfully.";
				log.info(inMsg);
				generatedfiles.add(inMsg);
			} else {
				throw new OpenEditException("The XML did not validate.");
			}

		} // end if numDistributors
	} // end distribIterator LOOP
	OrderState inOrderState = new OrderState();
	inOrderState.setOk(true);
	order.setOrderState(inOrderState);
	
	order.setProperty("orderstatus", "readytosend");
	
	context.putPageValue("filelist", generatedfiles);
	context.putPageValue("id", orderid)
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
				if (shipping.getName() != null) {
					AddressName1(shipping.name)
				}
				AddressIDQual("92")
				AddressIDCode(shipping.id);
				AddressLine1(shipping.address1)
				AddressLine2(shipping.address2)
				AddressCity(shipping.city)
				AddressState(shipping.state)
				AddressPostalCode(shipping.zipCode)
				AddressCountry(shipping.country)
			}
			//Write Billing Information
			TblAddress()
			{
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
			orderCount++
			populateDetail(xml, orderCount, orderItem)
		} // End itemIterator loop
	} // end POHeader
}

private void populateDetail(xml, int orderCount, CartItem orderItem) {

	xml.PODetail()
	{
		LineItemNumber(orderCount)
		String productId = orderItem.getProduct().getId();
		QuantityOrdered(orderItem.getQuantity().toString())

		def SEARCH_FIELD = "id";
		UnitPrice(orderItem.getYourPrice().toShortString())
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

private boolean validateXML( StringWriter xml ) {
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


init();
