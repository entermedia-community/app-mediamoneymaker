package products

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.store.Product
import org.openedit.util.DateStorageUtil

import com.openedit.WebPageRequest




public void init(){
	WebPageRequest inReq = context;
	MediaArchive archive = context.getPageValue("mediaarchive");
	Searcher ticketsearcher = archive.getSearcher("tickets");
	Product product = inReq.getPageValue("product");
	
	Data ticket = ticketsearcher.createNewData();
	ticket.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
	ticket.setId(ticketsearcher.nextId());
	ticket.setProperty("owner", inReq.getUserProfile().getId());
	ticket.setProperty("product", product.getId());
	ticket.setProperty("tickettype", "productchangerequest");
	ticket.setSourcePath("${inReq.getUserProfile().getId()}");
	
	//THESE FIELDS ARE ACTUALLY PRODUCT FIELDS!
	
	String[] fields = inReq.getRequestParameters("field");
	ticketsearcher.updateData(inReq, fields, ticket);
	ticketsearcher.saveData(ticket, inReq.getUser());
	inReq.putPageValue("ticket", ticket);
	
}	


init();