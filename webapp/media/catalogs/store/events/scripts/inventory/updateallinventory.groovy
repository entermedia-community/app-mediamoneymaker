package inventory;

import java.text.SimpleDateFormat

import org.apache.commons.lang.text.StrMatcher;
import org.openedit.Data
import org.openedit.entermedia.publishing.PublishResult
import org.openedit.event.WebEvent
import org.openedit.store.InventoryItem
import org.openedit.store.Product
import org.openedit.store.Store
import org.openedit.store.util.MediaUtilities;
import org.openedit.util.DateStorageUtil

import com.openedit.OpenEditException
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.Page
import com.openedit.page.manage.PageManager

public class UpdateAllInventory extends EnterMediaObject {

	private static final String LOG_HEADER = " - RECORDS - ";
	private static final String FOUND = "Found"
	private static final String NOT_FOUND = "NOT FOUND!";

	private String orderID;
	private String purchaseOrder;
	private String distributorID;
	private String storeID;
	private String carrier;
	private String waybill;
	private String quantityShipped;
	private String productID;
	private Date dateShipped;

	public PublishResult doImport(Store store) {

		PublishResult result = new PublishResult();
		result.setComplete(false);

		MediaUtilities media = new MediaUtilities();
		media.setContext(context);
		if (media == null) {
			throw new OpenEditException("MediaUtilities is null!");
		}

		log.info("---- START Update Inventory ----");

		HitTracker hits = media.getProductSearcher().getAllHits();
		for (Iterator hit = hits.iterator(); hit.hasNext();) {
			Data p = hit.next();
			Product product =  media.searchForProduct(p.getId());
			if (product != null) {
				String quantity = "100";
				productID = product.getId();
				//log.info("ID to be updated: " + productID);
				PublishResult update = updateInventoryItem(product, quantity, media);
				if (update.isComplete()) {
					//log.info("Product(" + productID + ") updated to " + quantity + ".");
				} else {
					log.info(update.getErrorMessage());
				}
			}
		}
		return result;
	}

	public PublishResult updateInventoryItem( Product product, String quantity, MediaUtilities mediaUtilities) throws Exception {

		PublishResult result = new PublishResult();
		result.setComplete(false);
//		log.info("Looking up Product: " + productID);
		
		int inventoryCount = product.getInventoryItemCount();
		if (inventoryCount == 0) {
			InventoryItem productInventory = null;
			productInventory = product.getInventoryItem(0);
			if (productInventory == null) {
				//Need to create the Inventory Item
				productInventory = new InventoryItem();
				productInventory.setSku(product.getId());
				product.addInventoryItem(productInventory);
			} else {
				String sku = productInventory.getSku();
				if (sku == null) {
					product.clearItems();
					productInventory = new InventoryItem();
					productInventory.setSku(product.getId());
					product.addInventoryItem(productInventory);
				} else {
					if (!sku.equals(product.getId())) {
						product.clearItems();
						productInventory = new InventoryItem();
						productInventory.setSku(product.getId());
						product.addInventoryItem(productInventory);
					}
				}
			}
			productInventory.setQuantityInStock(Integer.parseInt(quantity));
			Date now = new Date();
			product.setProperty("inventoryupdated", DateStorageUtil.getStorageUtil().formatForStorage(now));
			try {
				mediaUtilities.getProductSearcher().saveData(product, mediaUtilities.getContext().getUser());
				result.setComplete(true);
			} catch (Exception e) {
				result.setErrorMessage("ERROR:" + product.getId() + ":" + e.getMessage() + ":" + e.getLocalizedMessage())
				log.info(e.getMessage());
				result.setComplete(false);
			}
		} else {
			InventoryItem productInventory = null;
			productInventory = product.getInventoryItem(0);
			if (productInventory == null) {
				//Need to create the Inventory Item
				productInventory = new InventoryItem();
				productInventory.setSku(product.getId());
				product.addInventoryItem(productInventory);
			} else {
				String sku = productInventory.getSku();
				if (sku == null) {
					product.clearItems();
					productInventory = new InventoryItem();
					productInventory.setSku(product.getId());
					product.addInventoryItem(productInventory);
				} else {
					if (!sku.equals(product.getId())) {
						log.info("Mismatch: " + sku + ":" + product.getId());
						product.clearItems();
						productInventory = new InventoryItem();
						productInventory.setSku(product.getId());
						product.addInventoryItem(productInventory);
						sku = productInventory.getSku();
						if (!sku.equals(product.getId())) {
							return;
						}
					}
				}
			}
			productInventory.setQuantityInStock(Integer.parseInt(quantity));
			Date now = new Date();
			product.setProperty("inventoryupdated", DateStorageUtil.getStorageUtil().formatForStorage(now));
			try {
				mediaUtilities.getProductSearcher().saveData(product, mediaUtilities.getContext().getUser());
				result.setComplete(true);
			} catch (Exception e) {
				result.setErrorMessage("ERROR:" + product.getId() + ":" + e.getMessage() + ":" + e.getLocalizedMessage())
				log.info(e.getMessage());
				result.setComplete(false);
			}
		}
		return result;
	}
}
PublishResult result = new PublishResult();
result.setComplete(false);

logs = new ScriptLogger();
logs.startCapture();

try {

	log.info("---- START Udpate All Inventory Processing --- .");

	UpdateAllInventory updateInventory = new UpdateAllInventory();
	updateInventory.setLog(logs);
	updateInventory.setContext(context);
	updateInventory.setPageManager(pageManager);
	Store store = context.getPageValue("store");

	result = updateInventory.doImport(store);
	if (result.isComplete()) {
		String strMsg = "---- SUCCESS Update All Inventory --- .";
		context.putPageValue("export", strMsg);
		log.info(strMsg);
	} else {
		String strMsg = "---- ERROR Update All Inventory --- .";
		context.putPageValue("export", strMsg);
		log.info(strMsg);
	}
	log.info("---- END Udpate All Inventory Processing --- .");
}
finally {
	logs.stopCapture();
}
