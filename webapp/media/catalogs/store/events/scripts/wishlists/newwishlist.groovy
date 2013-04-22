package wishlists
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openedit.Data
import org.openedit.data.*
import org.openedit.entermedia.MediaArchive
import org.openedit.profile.UserProfile
import org.openedit.store.Cart
import org.openedit.store.CartItem
import org.openedit.store.Store
import org.openedit.store.util.MediaUtilities
import org.openedit.util.DateStorageUtil

import com.openedit.WebPageRequest;
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.GroovyScriptRunner
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker
import com.openedit.users.User


public class NewWishList extends EnterMediaObject {
	
	Map<String, Integer> prodValues;
	
	public void putValue( String inProd, Integer inQuantity ) {
		Integer qty = prodValues.get(inProd);
		prodValues.put(inProd, (qty == null) ? inQuantity : qty+inQuantity);
	}
	
	public void init(){
		
		//Get Media Info
		Log log = LogFactory.getLog(GroovyScriptRunner.class);
		
		WebPageRequest inReq = context;

		MediaArchive archive = context.getPageValue("mediaarchive");
		SearcherManager manager = archive.getSearcherManager();
		String catalogid = archive.getCatalogId();
		
		MediaUtilities media = new MediaUtilities();
		media.setContext(context);

		String action = media.getContext().getRequestParameter("action");
		if (!action.equals("doMerge")) {
			String errorOut = "You have reached this page in error";
			context.putPageValue("action", "error");
			context.putPageValue("errorout", errorOut); 
			return;
		}
		
		//Get Name from form
		String name = media.getContext().getRequestParameter("name");
		String remove = media.getContext().getRequestParameter("remove");
		log.info("New Name: " + name);
		
		// Create the Searcher Objects to read values!
		SearcherManager searcherManager = archive.getSearcherManager();
		Searcher wishlistsearcher = searcherManager.getSearcher(catalogid, "wishlist");
		Searcher wishlistitemsearcher = searcherManager.getSearcher(catalogid, "wishlistitems");
		Searcher productsearcher = media.getProductSearcher();
		Searcher usersearcher = searcherManager.getSearcher("system","user");
		
		String sessionid = media.getContext().getRequestParameter("sessionid");
		if (sessionid == null) {
			context.putPageValue("errorout", "No sessions.");
			return;
		}
		HitTracker hits = media.getContext().getSessionValue(sessionid);
		if (hits == null) {
			context.putPageValue("errorout", "No wishlists provided.");
			return;
		}
		ArrayList wishLists = hits.getSelectedHits();
		log.info("Found # of WishLists:" + wishLists.size());
		
		prodValues = new HashMap<String, Integer>();
		
		for (Iterator listIterator = wishLists.iterator(); listIterator.hasNext();) {
			//Get the first Item
			Data wishList = (Data) listIterator.next();
			
			//Load the wishList
			String listID = wishList.getId();
			log.info("ListID: " + listID);
			//Load the wishListItems per WishList			
			HitTracker wishListItems = wishlistitemsearcher.fieldSearch("wishlist", listID);
			for (int intCtr = 0; intCtr < wishListItems.size(); intCtr++) {
				
				//Add Wish List Item Info
				Data wishListItem = wishListItems[intCtr];
				String itemID = wishListItem.getId();
				log.info(" - ItemID: " + itemID);

				String productID = wishListItem.get("product");
				Integer quantity = Integer.parseInt(wishListItem.get("quantity"));
				putValue(productID, quantity);
			}
		}
		if (prodValues != null) {
			UserProfile profile = inReq.getUserProfile();
			String parentid = profile.get("parentprofile");
			User user = inReq.getUser();
			Data wishlist = wishlistsearcher.createNewData();
			wishlist.setProperty("userid", user.getId());
			wishlist.setProperty("creationdate",DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
			wishlist.setProperty("profileid", profile.getId());
			wishlist.setProperty("dealer", profile.get("dealer"));
			wishlist.setProperty("address", profile.get("address"));
	
			wishlist.setProperty("store", profile.get("store"));
			wishlist.setId(wishlistsearcher.nextId());
			if (name != null) {
				wishlist.setProperty("name", name);
			} else {
				wishlist.setProperty("name", "WISHLIST" + wishlist.getId());
			}
			String[] fields = inReq.getRequestParameters("field");
			wishlistsearcher.updateData(inReq, fields, wishlist);
			wishlist.setProperty("wishstatus", "pending");
			wishlistsearcher.saveData(wishlist, user);
			
			for( String key : prodValues.keySet()) {
				Data listitem = wishlistitemsearcher.createNewData();
				listitem.setId(wishlistitemsearcher.nextId());
				listitem.setProperty("product", key);
				listitem.setProperty("quantity", String.valueOf(prodValues.get(key)));
				listitem.setProperty("wishlist", wishlist.getId());
				wishlistitemsearcher.saveData(listitem, inReq.getUser());
			}
			context.putPageValue("newwishlist", wishlist);
			context.putPageValue("action", action);
		} else {
			context.putPageValue("errorout", "No values in wishlists provided.");
		}
		if ((remove !=null ) && remove.equalsIgnoreCase("yes")) {
			for (Iterator listIterator = wishLists.iterator(); listIterator.hasNext();) {
				Data item = (Data) listIterator.next();
				wishlistsearcher.delete(item, context.getUser());
			}
		}
	}
}
boolean result = false;

logs = new ScriptLogger();
logs.startCapture();

try {
	NewWishList newWishList = new NewWishList();
	newWishList.setLog(logs);
	newWishList.setContext(context);
	newWishList.setModuleManager(moduleManager);
	newWishList.setPageManager(pageManager);

	newWishList.init();
}
finally {
	logs.stopCapture();
}
