package edi

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.store.Product
import org.openedit.store.orders.Order

import com.openedit.OpenEditException
import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery

public class MediaUtilities extends EnterMediaObject {

	protected MediaArchive archive;
	protected String catalogid;
	protected WebPageRequest context;

	//Searcher Objects
	protected SearcherManager manager;
	protected Searcher distributorsearcher;
	protected Searcher productsearcher;
	protected Searcher ordersearcher;
//	protected Searcher itemsearcher;
	protected Searcher storesearcher;
	protected Searcher manufacturersearcher;
	protected Searcher invoicesearcher;
	protected Searcher invoiceitemssearcher;
	protected Searcher errorsearcher;
	
	public MediaUtilities() {
	}

	////////////////////////////////////////////
	// GETTERS AND SETTERS for Searchers
	////////////////////////////////////////////
	@Override
	public void setContext(WebPageRequest inContext) {
		context = inContext;
	}
	
	@Override
	public WebPageRequest getContext() {
		return context;
	}
	
	public String getCatalogid() {
		if (catalogid == null) {
			catalogid = getArchive().getCatalogId();
		}
		return catalogid;
	}
	
	public MediaArchive getArchive() {
		if (archive == null) {
			archive = getContext().getPageValue("mediaarchive");
		}
		return archive;
	}
	
	public SearcherManager getManager() {
		if (manager == null) {
			manager = archive.getSearcherManager();			
		}
		return manager;
	}
	
	public Searcher getDistributorSearcher() {
		if (distributorsearcher == null) {
			distributorsearcher = getManager().getSearcher(catalogid, "distributor");			
		}
		return distributorsearcher;
	}
	
	public Searcher getProductSearcher() {
		if (productsearcher == null) {
			productsearcher = getManager().getSearcher(catalogid, "product");			
		}
		return productsearcher;
	}
	
	public Searcher getOrderSearcher() {
		if (ordersearcher == null) {
			ordersearcher = getManager().getSearcher(archive.getCatalogId(), "storeOrder");
		}
		return ordersearcher;
	}
	
	public Searcher getStoreSearcher() {
		if (storesearcher == null) {
			storesearcher = getManager().getSearcher(catalogid, "store");			
		}
		return storesearcher;
	}
	
	public Searcher getManufacturerSearcher() {
		if (manufacturersearcher == null) {
			manufacturersearcher = getManager().getSearcher(catalogid , "manufacturer");
		}
		return manufacturersearcher;
	}

	public Searcher getInvoiceSearcher() {
		if (invoicesearcher == null) {
			invoicesearcher = getManager().getSearcher(catalogid , "invoice");
		}
		return invoicesearcher;
	}
	
	public Searcher getInvoiceItemsSearcher() {
		if (invoiceitemssearcher == null) {
			invoiceitemssearcher = getManager().getSearcher(catalogid , "invoiceitem");
		}
		return invoiceitemssearcher;
	}
	
	public Searcher getErrorSearcher() {
		if (errorsearcher == null) {
			errorsearcher = getManager().getSearcher(catalogid, "errormessage");
		}
		return errorsearcher;
	}
	
	////////////////////////////////////////////
	// OTHER PUBLIC METHODS
	////////////////////////////////////////////

	public Data searchForDistributor(String searchForName) {
		String SEARCH_FIELD = "name";
		Data targetDistributor = distributorsearcher.searchByField(SEARCH_FIELD, searchForName);

		return targetDistributor;
	}

	public Data searchForDistributor( String searchForName, Boolean production ) {

		String SEARCH_FIELD = "";
		if (production == true){
			SEARCH_FIELD = "headermailboxprod";
		} else {
			SEARCH_FIELD = "headermailboxtest";
		}
		Searcher distributorsearcher = getManager().getSearcher(archive.getCatalogId(), "distributor");
		Data targetDistributor = distributorsearcher.searchByField(SEARCH_FIELD, searchForName);

		return targetDistributor;
	}
	
	public Order searchForOrder( String inId ) {
		Order rogersOrder = getOrderSearcher().searchById(inId);
		if (rogersOrder == null) {
			return null;
		}
		return rogersOrder;
	}

	public Data searchForStore( String searchForName ) {
		String SEARCH_FIELD = "store";
		Data rogersStore = getStoreSearcher().searchByField(SEARCH_FIELD, searchForName);
		return rogersStore;
	}
	
	public Data searchForStoreByID( String storeID ) {
		Data rogersStore = getStoreSearcher().searchById(storeID);
		return rogersStore;
	}
		
	public HitTracker searchForStoreInOrder( String orderid, String store_number ) {
		SearchQuery itemQuery = getStoreSearcher().createSearchQuery();
		itemQuery.addExact("store", store_number);
		itemQuery.addExact("rogers_order", orderid);
		HitTracker orderitems = getStoreSearcher().search(itemQuery);
		return orderitems;
	}

	public Data searchForProductbyUPC( String searchForName ) {
		String SEARCH_FIELD = "upc";
		Data product = getProductSearcher().searchByField(SEARCH_FIELD, searchForName);
		return product;
	}
	
	public Data searchForProductbyRogersSKU( String searchForName ) {
		String SEARCH_FIELD = "rogerssku";
		Data product = getProductSearcher().searchByField(SEARCH_FIELD, searchForName);
		return product;
	}
	
	public Product searchForProduct( String productID ) {
		Product product = getProductSearcher().searchById(productID);
		return product;
	}
	
	public String getManufacturerID(String searchForName) {
		Data manufacturer = getManufacturerSearcher().searchByField("name", searchForName);
		if (manufacturer == null) {
			manufacturer = addManufacturer(searchForName);
		}
		return manufacturer.getId();
	}

	// ADD UTILITIES
	public Data addManufacturer(String manufacturerName) {
		Data manufacturer = getManufacturerSearcher().searchByField("name", manufacturerName);
		if (manufacturer == null) {
			manufacturer = getManufacturerSearcher().createNewData();
			manufacturer.setId(getManufacturerSearcher().nextId());
			manufacturer.setSourcePath(manufacturer.getId());
			manufacturer.setName(manufacturerName);
			getManufacturerSearcher().saveData(manufacturer, context.getUser());
			Data newManufacturer = getManufacturerSearcher().searchByField("name", manufacturerName);
			if (newManufacturer != null) {
				return newManufacturer;
			}
		} else {
			String inMsg = "Cannot created a duplicate Manufacturer(" + manufacturerName + ")";
			throw new OpenEditException(inMsg)
		}
	}
	
	public String getStoreID(String storeNumber) {
		String id = "";
		Data storeInfo = getStoreSearcher().searchByField("store", storeNumber);
		if (storeInfo != null) {
			id = storeInfo.getId();
		} else {
			String inMsg = "Store cannot be found(" + storeNumber + ")";
			throw new OpenEditException(inMsg)
		}
		return id;
	}
	
	public String getStoreName(String storeID) {
		Data storeInfo = getStoreSearcher().searchById(storeID);
		return storeInfo.getName();
	}
	
	public String getProductName(String productID) {
		Data productInfo = getProductSearcher().searchById(productID);
		return productInfo.getName();
	}
}
