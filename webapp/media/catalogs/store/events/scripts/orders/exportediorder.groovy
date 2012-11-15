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
import org.openedit.entermedia.publishing.PublishResult
import org.openedit.money.Money
import org.openedit.repository.filesystem.StringItem

import com.openedit.BaseWebPageRequest
import com.openedit.OpenEditException
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.Page

public void init() {

	PublishResult result = new PublishResult();
	result.setComplete(false);

	BaseWebPageRequest inReq = context;

	MediaArchive archive = inReq.getPageValue("mediaarchive");
	SearcherManager manager = archive.getSearcherManager();
	boolean production = Boolean.parseBoolean(context.findValue('productionmode'));

	//Create Searcher Object
	Searcher productsearcher = manager.getSearcher(archive.getCatalogId(), "product");
	Searcher ordersearcher = manager.getSearcher(archive.getCatalogId(), "rogers_order");
	Searcher itemsearcher = manager.getSearcher(archive.getCatalogId(), "rogers_order_item");
	Searcher storesearcher = manager.getSearcher(archive.getCatalogId(), "store");
	Searcher distributorsearcher = manager.getSearcher(archive.getCatalogId(), "distributor");

	//Read orderid from the URL
	def orderid = inReq.getRequestParameter("orderid");
	Data order = ordersearcher.searchById(orderid);

	def String ediID = inReq.getRequestParameter("ediid");

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
		}

		SearchQuery distribQuery = itemsearcher.createSearchQuery();
		distribQuery.addExact("rogers_order",orderid);
		distribQuery.addExact("distributor", distributor.id);
		HitTracker numDistributors = itemsearcher.search(distribQuery);//Load all of the line items for store X

		if (numDistributors.size() > 0 )
		{
			//Iterate through each distributor

			//Create the XML Writer object
			def writer = new StringWriter();
			def xml = new MarkupBuilder(writer);
			xml.'PurchaseOrder'()
			{
				Attributes()
				result = populateGroup(xml, storesearcher, itemsearcher, orderid, distributor, log, order, productsearcher)
			}
			if (result.isComplete()) {

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
					generatedfiles.add(fileName + " has been validated and created successfully.");
				} else {
					throw new OpenEditException("The XML did not validate.");
				}
			} else {
				log.info("ERROR: This order is invalid!" + orderid + ":" + result.getErrorMessage());
			}
		} // end if numDistributors
	} // end distribIterator LOOP
	context.putPageValue("filelist", generatedfiles);
	context.putPageValue("id", orderid)
}

private PublishResult populateGroup(xml, Searcher storesearcher, Searcher itemsearcher, String orderid, Data distributor, log, Data order, Searcher productsearcher) {

	PublishResult result = new PublishResult();
	result.setComplete(false);

	log.info("orderid: ${orderid}")
	xml.POGroup()
	{
		HitTracker allStores = storesearcher.getAllHits();
		for (Iterator eachStore = allStores.iterator(); eachStore.hasNext();)
		{

			Data rogersStore = eachStore.next();
			def String storeNumber = rogersStore.store;

			SearchQuery storeLookup = itemsearcher.createSearchQuery();
			storeLookup.addExact("store", storeNumber);
			storeLookup.addExact("rogers_order", orderid);
			HitTracker foundStore = itemsearcher.search(storeLookup);

			if (foundStore.size() > 0 )
			{
				SearchQuery itemQuery = itemsearcher.createSearchQuery();
				itemQuery.addExact("store", storeNumber);
				itemQuery.addExact("rogers_order", orderid);
				itemQuery.addExact("distributor", distributor.id);
				HitTracker orderitems = itemsearcher.search(itemQuery);//Load all of the line items for store X

				if (orderitems.size() > 0 )
				{
					if (orderitems.size() == 1) {
						log.info(" Item found!");
					} else {
						log.info(" Items(${orderitems.size()}) found!");
					}
					log.info(" - Distributor Name: ${distributor.name}");
					log.info(" - Rogers Store Number: ${storeNumber}");
					//Write Vendor/Distributor Information
					result = populateHeader(xml, storeNumber, distributor, rogersStore,
							orderitems, order, productsearcher)
				} else {
					log.info("No items found for this store (${storeNumber}) for this order (${orderid}) for this Distributor (${distributor.name}) ");
				} // end if orderitems
			} // end if foundStore
		} //END eachstore LOOP
	} // end POGroup

	return result;

}

private PublishResult populateHeader(xml, String storeNumber, Data distributor, Data rogersStore, HitTracker orderitems, Data order, Searcher productsearcher) {
	boolean production = Boolean.parseBoolean(context.findValue('productionmode'));

	PublishResult result = new PublishResult();
	result.setComplete(false);

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
				AddressType("ST")
				AddressName1(rogersStore.name)
				AddressIDQual(distributor.idQual)
				AddressIDCode(storeNumber)
				AddressLine1(rogersStore.businessaddress)
				AddressLine2(rogersStore.address2)
				AddressCity(rogersStore.businesscity)
				AddressState(rogersStore.businessprovince)
				AddressPostalCode(rogersStore.businesspostalcode)
				AddressCountry("CA")
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
				Amount(orderitems.size())
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
				ReferenceNbr(order.getId()+"-"+storeNumber)
			}

		} // end Attributes
		def orderCount = 0;
		for (Iterator itemIterator = orderitems.iterator(); itemIterator.hasNext();)
		{
			Data orderItem = itemIterator.next();
			orderCount++

			result = populateDetail(xml, orderCount, orderItem, productsearcher, storeNumber)
		} // End itemIterator loop
	} // end POHeader
	return result;

}

private PublishResult populateDetail(xml, int orderCount, Data orderItem, Searcher productsearcher, String storeNumber) {

	def int validCtr = 0;

	PublishResult result = new PublishResult();
	result.setComplete(false);

	xml.PODetail()
	{
		LineItemNumber(orderCount)
		String productId = orderItem.product;
		QuantityOrdered(orderItem.quantity)

		def SEARCH_FIELD = "id";
		Data targetProduct = productsearcher.searchByField(SEARCH_FIELD, productId);
		if (targetProduct != null) {

			def boolean valid = Boolean.parseBoolean(orderItem.validitem);
			if (valid) {
				validCtr++;
				log.info(" - Valid Product Found: " + targetProduct.getId());
				log.info(" - Manufacturer's SKU : " + targetProduct.manufacturersku);
				Money money = new Money(targetProduct.rogersprice);
				UnitPrice(money.toShortString())
				UnitOfMeasure("EA")
				Description(targetProduct.name)
				StoreNbr(orderItem.store)
				Attributes()
				{
					TblReferenceNbr()
					{
						Qualifier("VN")
						ReferenceNbr(targetProduct.manufacturersku)
					}
					TblReferenceNbr()
					{
						Qualifier("UP")
						ReferenceNbr(targetProduct.upc)
					}
				}
			} else {
				result.setErrorMessage(orderItem.store);
			}
		}
	}
	if (validCtr > 0) {
		result.setComplete(true);
	}
	return result;
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

private Data getProduct( SearcherManager searcherManager, String id ) {

	Searcher productSearcher = searcherManager.getSearcher(catalogid, "product");
	Data product = productSearcher.searchById(id);
	if (product != null) {
		return product;
	} else {
		throw new OpenEditException("Product(" + id + ") does not exist!");
	}
}

init();
