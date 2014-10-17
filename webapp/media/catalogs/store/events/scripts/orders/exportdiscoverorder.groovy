package orders;

import groovy.xml.MarkupBuilder

import java.text.SimpleDateFormat

import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.event.WebEvent
import org.openedit.event.WebEventHandler
import org.openedit.event.WebEventListener
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
	Searcher as400searcher = manager.getSearcher(archive.getCatalogId(), "as400");


	int orderCount = 0;

	HitTracker orderList = ordersearcher.getAllHits();
	for (Iterator orderIterator = orderList.iterator(); orderIterator.hasNext();) {

		Data currentOrder = orderIterator.next();

		Order order = ordersearcher.searchById(currentOrder.getId());

		if (order == null) {
			throw new OpenEditException("Invalid Order");
		}

		String rogersPO = null;
		if(order.getId().startsWith("Rogers") && order.get("batchid")!=null){
			String batchid = order.get("batchid");
			Data batchentry = as400searcher.searchByField("batchid", batchid);
			if (batchentry!=null){
				String po = batchentry.get("as400po");
				if (po == null || po.trim().isEmpty()){
					log.info("Skipping Rogers order that does not have PO");
					continue;
				}
				rogersPO = po;
			}
		}

		String orderStatus = order.get("orderstatus");
		if (orderStatus == "authorized"  || ( orderStatus == "accepted"))  {
			String ediStatus = order.get("edistatus");

			orderCount++;
			String orderid = order.getId();

			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			List generatedfiles = new ArrayList();

			log.info("Distributor: " + distributor.name);
			log.info("Processing Order ID: " + currentOrder.getId());

			//Iterate through each distributor

			//Create the XML Writer object
			def writer = new StringWriter();
			def xml = new MarkupBuilder(writer);
			xml.'PurchaseOrder'()
			{
				Attributes()
				populateGroup(xml, storesearcher,  distributor, log, order, rogersPO)
			}

		

		} // end if numDistributors
	} // end distribIterator LOOP
} 