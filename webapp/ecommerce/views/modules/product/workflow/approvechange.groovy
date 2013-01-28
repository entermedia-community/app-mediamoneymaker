package products

import org.openedit.Data
import org.openedit.data.PropertyDetail
import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.event.WebEvent
import org.openedit.store.Product
import org.openedit.store.Store

import com.openedit.WebPageRequest




public void init(){
	WebPageRequest inReq = context;
	MediaArchive archive = context.getPageValue("mediaarchive");
	Store store = context.getPageValue("store");
	Searcher ticketsearcher = archive.getSearcher("ticket");
	
	String productid = inReq.getRequestParameter("productid");
	String ticketid = inReq.getRequestParameter("ticketid");
	
	
	Product product = store.getProduct(productid);
	Data ticket = ticketsearcher.searchById(ticketid);
	
	details = store.getProductSearcher().getDetailsForView("product/productsubmit_for_review", context.getUserProfile());
	details.each{
		PropertyDetail detail = it;
		String value = ticket.get(detail.getId());
		if(value != null && value.length() > 0){
			product.setProperty(detail.getId(), value);
		}
	}
	ticket.setProperty("ticketstatus", "closed");
	ticket.setProperty("note", "Approved");

	
	ticketsearcher.saveData(ticket, inReq.getUser());
	store.getProductSearcher().saveData(ticket, inReq.getUser());
	
	WebEvent event = new WebEvent();
	event.setSearchType(ticketsearcher.getSearchType());
	event.setCatalogId(ticketsearcher.getCatalogId());
	event.setOperation(ticketsearcher.getSearchType() + "/saved");
	event.setProperty("dataid", ticketid);
	event.setProperty("id", ticketid);
	event.setProperty("applicationid", inReq.findValue("applicationid"));
	archive.getMediaEventHandler().eventFired(event);
	
	inReq.putPageValue("ticket", ticket);
	
}


init();