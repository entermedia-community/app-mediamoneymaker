package edi;

import java.text.SimpleDateFormat

import org.openedit.Data
import org.openedit.entermedia.publishing.PublishResult
import org.openedit.event.WebEvent
import org.openedit.store.InventoryItem
import org.openedit.store.Product
import org.openedit.store.Store
import org.openedit.util.DateStorageUtil

import com.openedit.OpenEditException
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.Page
import com.openedit.page.manage.PageManager

public class ImportEDIInventory extends EnterMediaObject {

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
		result.setErrorMessage("");
		String foundErrors = "";
		result.setComplete(false);

		OutputUtilities output = new OutputUtilities();
		
		MediaUtilities media = new MediaUtilities();
		media.setContext(context);
		media.setSearchers();

		log.info("---- START Import EDI Inventory ----");
		
		boolean production = Boolean.parseBoolean(context.findValue('productionmode'));

		String asnFolder = "/WEB-INF/data/" + media.getCatalogid() + "/incoming/inventory/";
		PageManager pageManager = media.getArchive().getPageManager();
		List dirList = pageManager.getChildrenPaths(asnFolder);
		def int dirSize = dirList.size();
		log.info("Initial directory size: " + dirSize.toString());
		
		def int fileCtr = 0;

		if (dirList.size() > 0) {
					
			for (Iterator iterator = dirList.iterator(); iterator.hasNext();) {
				Page page = pageManager.getPage(iterator.next());
				log.info("Processing " + page.getName());

				String realpath = page.getContentItem().getAbsolutePath();

				File xmlFIle = new File(realpath);
				if (xmlFIle.exists()) {
					
					//Create the XMLSlurper Object
					def InventoryInquiryAdvice = new XmlSlurper().parse(page.getReader());
					
					foundErrors = "";

					def INQGROUP = InventoryInquiryAdvice.depthFirst().grep{
						it.name() == 'INQGroup';
					}
					log.info("Found INQGroups: " + INQGROUP.size().toString());

					INQGROUP.each {

						def INQHEADER = it.depthFirst().grep{
							it.name() == 'INQHeader';
						}
						log.info("Found INQHeaderS: " + INQHEADER.size().toString());
	
						INQHEADER.each {
							
							//Set the Purchase Order Found Flag to false
							def boolean foundFlag = false;
	
							def INQDETAIL = it.depthFirst().grep{
								it.name() == 'INQDetail';
							}
							log.info("Found INQDetails: " + INQDETAIL.size().toString());
		
							INQDETAIL.each {
								
								def String quantity = it.Quantity.text();
								if (quantity != null) {
									def String vendorCode = it.Attributes.TblReferenceNbr.find {it.Qualifier == "VN"}.ReferenceNbr.text();
									if (!vendorCode.isEmpty()) {
										foundFlag = true;
										Data product = media.searchForProductbyRogersSKU(vendorCode);
										if (product != null) {
											foundFlag = true;
											productID = product.getId();
											result = updateInventoryItem(productID, quantity, store);
											
										} else {
											String inMsg = "Product(" + vendorCode + ") could not be found from Inventory file.";
											log.info(inMsg);
											foundErrors += output.appendList(inMsg);
										}
									} else {
										String inMsg = "Vendor Code cannot be found in Inventory XML File(" + page.getName() + ")";
										log.info(inMsg);
										foundErrors += output.appendList(inMsg);
									}
								} else {
									String inMsg = "Quantity cannot be found in Inventory XML File(" + page.getName() + ")";
									log.info(inMsg);
									foundErrors += output.appendList(inMsg);
								}
							}
						}
					}
					
					if (foundErrors.length() == 0 || foundErrors == null) {
						PublishResult move = movePageToProcessed(pageManager, page, media.getCatalogid(), true);
						if (move.isComplete()) {
							String inMsg = "Inventory File (" + page.getName() + ") moved to processed.";
							log.info(inMsg);
							fileCtr++;
						} else {
							String inMsg = "Inventory File (" + page.getName() + ") failed to move to processed.";
							log.info(inMsg);
							foundErrors += output.appendList(inMsg);
							result.setErrorMessage(result.getErrorMessage() + "\n" + inMsg);
						}
					} else {
						result.setErrorMessage(result.getErrorMessage() + "\n" + foundErrors);
						result = movePageToProcessed(pageManager, page, media.getCatalogid(), false);
						if (result.complete) {
							String inMsg = "Inventory File (" + page.getName() + ") moved to ERROR.";
							log.info(inMsg);
							foundErrors += output.appendList(inMsg);
							result.setErrorMessage(result.getErrorMessage() + "\n" + inMsg);
						} else {
							String inMsg = "Inventory File (" + page.getName() + ") failed to move to ERROR.";
							log.info(inMsg);
							foundErrors += output.appendList(inMsg);
							result.setErrorMessage(result.getErrorMessage() + "\n" + inMsg);
						}
					}
				} else {
					result.setErrorMessage(realpath + " does not exist!");
				}
			}
			if (result.getErrorMessage() != null) {
				if (result.getErrorMessage().length() > 0) {
					result.setErrorMessage(foundErrors + "\n");
					log.info("ERROR: The folowwing errors occurred:");
					log.info(result.getErrorMessage());
					//Create web event to send an email.
					WebEvent event = new WebEvent();
					event.setSearchType("inventory_processing");
					event.setCatalogId(media.getCatalogid());
					event.setProperty("error", result.getErrorMessage());
					media.getArchive().getMediaEventHandler().eventFired(event);
				} else {
					String inMsg = "Completed processing (" + fileCtr.toString() + ") all inventory (" + dirSize.toString() + ") files.";
					log.info(inMsg);
					result.setCompleteMessage(inMsg);
					result.setComplete(true);
				}
			} else {
				String inMsg = "Completed processing (" + fileCtr.toString() + ") all inventory (" + dirSize.toString() + ") files.";
				log.info(inMsg);
				result.setCompleteMessage(inMsg);
				result.setComplete(true);
			}
		} else {
			String inMsg = "INVENTORY: There are no files to process at this time.";
			log.info(inMsg);
			result.setCompleteMessage(inMsg);
			result.setComplete(true);
		}
		return result;
	}

	private PublishResult movePageToProcessed(PageManager pageManager, Page page,
	String catalogid, boolean saved) {

		PublishResult move = new PublishResult();
		move.setComplete(false);

		String processedFolder = "/WEB-INF/data/${catalogid}/processed/inventory/";
		if (!saved) {
			processedFolder += "errors/";
		}

		String destinationFile = processedFolder + page.getName();
		Page destination = pageManager.getPage(destinationFile);
		pageManager.movePage(page, destination);
		if (destination.exists()) {
			move.setCompleteMessage(page.getName() + " has been moved to " + destinationFile);
			move.setComplete(true);
		} else {
			move.setErrorMessage(page.getName() + " could not be moved.");
		}
		return move;
	}
	
	public PublishResult updateInventoryItem( String productID, String quantity, Store store ) throws Exception {

		PublishResult result = new PublishResult();
		result.setComplete(false);
		
		MediaUtilities media = new MediaUtilities();
		media.setContext(context);
		media.setSearchers();

		try {
	
			Product product = store.getProduct(productID);
			if (product != null) {
				InventoryItem productInventory = null
				productInventory = product.getInventoryItem(0);
				if (productInventory == null) {
					//Need to create the Inventory Item
					productInventory = new InventoryItem();
					productInventory.setSku(product.getId());
					product.addInventoryItem(productInventory);
				}
				productInventory.setQuantityInStock(Integer.parseInt(quantity));
				
				media.getProductSearcher().saveData(product, media.getContext().getUser());
				result.setComplete(true);
			} else {
				result.setErrorMessage("Product( " + productID + ") could not be found");
			}
		} catch(Exception e) {
			result.setErrorMessage(e.getMessage() + ":" + e.getLocalizedMessage())
		}
		return result;
	}
}
PublishResult result = new PublishResult();
result.setComplete(false);

logs = new ScriptLogger();
logs.startCapture();

try {

	ImportEDIInventory importInventory = new ImportEDIInventory();
	importInventory.setLog(logs);
	importInventory.setContext(context);
	importInventory.setPageManager(pageManager);
	
	Store store = context.getPageValue("store");
	
	result = importInventory.doImport(store);
	if (result.isComplete()) {
		//Output value to CSV file!
		context.putPageValue("export", result.getCompleteMessage());
		String errMsg = "";
		if (result.getErrorMessage() != null) {
			errMsg = result.getErrorMessage();
		}
		if (errMsg.length() > 0) {
			context.putPageValue("errorout", result.getErrorMessage());
		}
	} else {
		//ERROR: Throw exception
		context.putPageValue("errorout", result.getErrorMessage());
	}
}
finally {
	logs.stopCapture();
}
