package products

import org.openedit.Data
import org.openedit.data.PropertyDetail
import org.openedit.data.PropertyDetails
import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.store.Product
import org.openedit.store.Store

import com.openedit.WebPageRequest




public void init(){
	WebPageRequest inReq = context;
	MediaArchive archive = context.getPageValue("mediaarchive");
	Store store = context.getPageValue("store");
	Searcher ticketsearcher = archive.getSearcher("tickets");
	
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
	
	
	inReq.putPageValue("ticket", ticket);
	
	
}


init();