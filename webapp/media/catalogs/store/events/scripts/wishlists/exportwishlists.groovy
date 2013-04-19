package wishlists
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openedit.Data
import org.openedit.data.*
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.util.CSVWriter
import org.openedit.repository.filesystem.StringItem
import org.openedit.store.CartItem
import org.openedit.store.InventoryItem;
import org.openedit.store.Product
import org.openedit.store.orders.Order
import org.openedit.store.util.MediaUtilities

import com.openedit.OpenEditException
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.GroovyScriptRunner
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker
import com.openedit.page.Page


public class ExportWishLists extends EnterMediaObject {
	
	String outputFile;
	ArrayList<String> goodOrderList;
	
	public void setOutputFile( String inItem ) {
		if (outputFile == null) {
			outputFile = "";
		}
		this.outputFile = inItem;
	}
	
	public ArrayList<String> getOutputFile() {
		if (outputFile == null) {
			outputFile = "";
		}
		return this.outputFile;
	}
	
	public void addToGoodOrderList( String inItem ) {
		if (goodOrderList == null) {
			goodOrderList = new ArrayList<String>();
		}
		this.goodOrderList.add(inItem);
	}
	
	public ArrayList<String> getGoodOrderList() {
		if (goodOrderList == null) {
			goodOrderList = new ArrayList<String>();
		}
		return this.goodOrderList;
	}
	
	public boolean existsInGoodOrderList( String inItem ) {
		if (getGoodOrderList().contains(inItem)) {
			return true;
		} else {
			return false;
		}
	}
	
	public void init(){
		
		//Get Media Info
		Log log = LogFactory.getLog(GroovyScriptRunner.class);

		MediaArchive archive = context.getPageValue("mediaarchive");
		SearcherManager manager = archive.getSearcherManager();
		String catalogid = archive.getCatalogId();
		
		MediaUtilities media = new MediaUtilities();
		media.setContext(context);

		// Create the Searcher Objects to read values!
		SearcherManager searcherManager = archive.getSearcherManager();
		Searcher wishlistsearcher = searcherManager.getSearcher(catalogid, "wishlist");
		Searcher wishlistitemsearcher = searcherManager.getSearcher(catalogid, "wishlistitems");
		Searcher productsearcher = media.getProductSearcher();
		Searcher usersearcher = searcherManager.getSearcher("system","user");
		
		String sessionid = media.getContext().getRequestParameter("hitssessionid");
		if (sessionid == null) {
			return;
		}
		HitTracker wishLists = media.getContext().getSessionValue(sessionid);
		if (wishLists == null) {
			return;
		}
		log.info("Found # of WishLists:" + wishLists.getSelectedHits().size());
		
		//Create the CSV Writer Objects
		StringWriter output  = new StringWriter();
		CSVWriter writer  = new CSVWriter(output, (char)',');
		
		List headerRow = new ArrayList();
		headerRow = writeHeader(headerRow);
		writeRowToWriter(headerRow, writer);
		
		for (Iterator listIterator = wishLists.iterator(); listIterator.hasNext();) {
			//Get the first Item
			Data item = (Data) listIterator.next();
			
			//Load the wishList
			Data wishList = wishlistsearcher.searchById(item.getId());
			String listID = wishList.getId();
			log.info("ListID: " + listID);
			//Load the wishListItems per WishList			
			HitTracker wishListItems = wishlistitemsearcher.fieldSearch("wishlist", listID);
			for (int intCtr = 0; intCtr < wishListItems.size(); intCtr++) {
				//Iterate through each item
				//Create the List
				List dataRow = new ArrayList();
				
				//Add Wish List Info
				dataRow.add(wishList.getName())
				Data createdBy = usersearcher.searchById(wishList.get("userid"));
				dataRow.add(createdBy.get("name"));
				dataRow.add(wishList.get("store"));
				dataRow.add(wishList.get("dealer"));
				dataRow.add(wishList.get("creationdate"));
				dataRow.add(wishList.get("status"));
				dataRow.add(wishList.get("notes"));
				
				//Add Wish List Item Info
				Data wishListItem = wishListItems[intCtr];
				String itemID = wishListItem.getId();
				log.info(" - ItemID: " + itemID);

				String productID = wishListItem.get("product");
				Product product = productsearcher.searchById(productID);
				if (product == null) {
					throw new OpenEditException("Product not found: " + productID);
				}
				dataRow.add(product.getId());
				dataRow.add(product.getName());
				dataRow.add(wishListItem.get("quantity"));
				InventoryItem inv = product.getInventoryItem(0);
				dataRow.add(inv.quantityInStock.toString());
				dataRow.add(product.get("rogersprice"));
				dataRow.add(product.get("msrp"));
				dataRow.add(product.get("fidomsrp"));
				
				//Write the data
				writeRowToWriter(dataRow, writer);
			}
		}
		
		writer.close();
		
		String finalout = output.toString();
		context.putPageValue("exportcsv", finalout);
	}
	
	private List writeHeader( List inList ) {
		inList.add("NAME");
		inList.add("CREATED_BY");
		inList.add("STORE");
		inList.add("DEALER");
		inList.add("CREATED_DATE");
		inList.add("STATUS");
		inList.add("NOTES");
		inList.add("PRODUCT_ID");
		inList.add("PRODUCT_NAME");
		inList.add("QTY_ORDERED");
		inList.add("QTY_IN_STOCK");
		inList.add("ROGERS_PRICE");
		inList.add("MSRP");
		inList.add("FIDO_MSRP");
		return inList;
	}

	private void writeRowToWriter( List inRow, CSVWriter writer) {
		
		String[] nextrow = new String[inRow.size()];
		for ( int headerCtr=0; headerCtr < inRow.size(); headerCtr++ ){
			nextrow[headerCtr] = inRow.get(headerCtr);
		}
		try	{
			writer.writeNext(nextrow);
		}
		catch (Exception e) {
			log.info(e.message);
			log.info(e.getStackTrace().toString());
		}
	}

	private void writeOrderToFile(Page page, StringWriter output, String fileName) {
		
		page.setContentItem(new StringItem(page.getPath(), output.toString(), "UTF-8"));

		//Write out the CSV file.
		pageManager.putPage(page);
		if (!page.exists()) {
			String strMsg = "ERROR:" + fileName + " was not created.";
			log.info(strMsg);
		}
	}
}
boolean result = false;

logs = new ScriptLogger();
logs.startCapture();

try {
	ExportWishLists exportWishList = new ExportWishLists();
	exportWishList.setLog(logs);
	exportWishList.setContext(context);
	exportWishList.setModuleManager(moduleManager);
	exportWishList.setPageManager(pageManager);

	exportWishList.init();
}
finally {
	logs.stopCapture();
}
