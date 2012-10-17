import groovy.xml.MarkupBuilder

import java.text.SimpleDateFormat

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.money.Money
import org.openedit.repository.filesystem.StringItem

import com.openedit.BaseWebPageRequest
import com.openedit.OpenEditException
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.Page

public void init() {

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
	//Create the XML Writer object

	SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	HitTracker distributorList = distributorsearcher.getAllHits();
	List generatedfiles = new ArrayList();
	//Search through each distributor
	for (Iterator distribIterator = distributorList.iterator(); distribIterator.hasNext();)
	{

		//Get all of the hits and data for searching
		Data distributor = distribIterator.next();

		SearchQuery distribQuery = itemsearcher.createSearchQuery();
		distribQuery.addExact("rogers_order",orderid);
		distribQuery.addExact("distributor", distributor.name);
		HitTracker numDistributors = itemsearcher.search(distribQuery);//Load all of the line items for store X

		if (numDistributors.size() > 0 )
		{

			//Iterate through each distributor
			def writer = new StringWriter();
			def xml = new MarkupBuilder(writer);
			xml.'PurchaseOrder'()
			{
				Attributes()
				populateGroup(xml, storesearcher, itemsearcher, orderid, distributor, log, order, productsearcher)
			}
			// xml generation
			String fileName = "export-" + distributor.name.replace(" ", "-") + ".xml"
			Page page = pageManager.getPage("/WEB-INF/data/${catalogid}/orders/exports/${orderid}/${fileName}");
			StringItem item = new StringItem(page.getPath(), writer.toString(), "UTF-8");
			page.setContentItem(item);
			pageManager.putPage(page);
			generatedfiles.add(fileName + " created successfully.");
		} else {
			log.info("Distributor (${distributor.name}) not found for this order (${orderid})");
		} // end if numDistributors
	} // end distribIterator LOOP
	context.putPageValue("filelist", generatedfiles);
}

private void populateGroup(xml, Searcher storesearcher, Searcher itemsearcher, String orderid, Data distributor, log, Data order, Searcher productsearcher) {

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
				itemQuery.addExact("distributor", distributor.name);
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
					populateHeader(xml, storeNumber, distributor, rogersStore, orderitems, order, productsearcher)
				} else {
					log.info("No items found for this store (${storeNumber}) for this order (${orderid}) for this Distributor (${distributor.name}) ");
				} // end if orderitems
			} else {
				log.info("Store (${storeNumber}) not found for this order ${orderid}.");
			} // end if foundStore
		} //END eachstore LOOP
	} // end POGroup
}

private void populateHeader(xml, String storeNumber, Data distributor, Data rogersStore, HitTracker orderitems, Data order, Searcher productsearcher) {
	boolean production = Boolean.parseBoolean(context.findValue('productionmode'));

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

			populateDetail(xml, orderCount, orderItem, productsearcher, storeNumber)
		} // End itemIterator loop
	} // end POHeader

}

private void populateDetail(xml, int orderCount, Data orderItem, Searcher productsearcher, String storeNumber) {
	xml.PODetail()
	{
		LineItemNumber(orderCount)
		String productId = orderItem.product;
		QuantityOrdered(orderItem.quantity)

		def SEARCH_FIELD = "id";
		Data targetProduct = productsearcher.searchByField(SEARCH_FIELD, productId);
		if (targetProduct != null) {
			Money money = new Money(targetProduct.rogersprice);

			UnitPrice(money.toShortString())
			UnitOfMeasure("EA")
			Description(targetProduct.name)
			StoreNbr(storeNumber)
		}
	}
}

init();
