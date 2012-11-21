package edi

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive

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
	protected Searcher itemsearcher;
	protected Searcher storesearcher;
	protected Searcher manufacturersearcher;
	protected Searcher invoicesearcher;
	protected Searcher invoiceitemssearcher;
	protected Searcher errorsearcher;
	
	public MediaUtilities() {
	}

	public void setSearchers() {
		setArchive();
		setCatalogid();
		setManager();
		setDistributorSearcher();
		setProductSearcher();
		setOrderSearcher();
		setItemSearcher();
		setStoreSearcher();
		setManufacturerSearcher();
		setInvoiceSearcher();
		setInvoiceItemsSearcher();
		setErrorSearcher();
	}

	////////////////////////////////////////////
	// GETTERS AND SETTERS for Searchers
	////////////////////////////////////////////
	
	public void setContext(WebPageRequest inContext) {
		context = inContext;
	}
	public WebPageRequest getContext() {
		return context;
	}
	
	void setCatalogid() {
		catalogid = archive.getCatalogId();
	}
	public String getCatalogid() {
		return catalogid;
	}
	
	protected void setArchive() {
		archive = context.getPageValue("mediaarchive");
	}
	public MediaArchive getArchive() {
		return archive;
	}
	
	protected void setManager() {
		manager = archive.getSearcherManager();
	}
	public SearcherManager getManager() {
		return manager;
	}
	
	protected void setDistributorSearcher() {
		distributorsearcher = manager.getSearcher(catalogid, "distributor");
	}
	public Searcher getDistributorSearcher() {
		return distributorSearcher;
	}
	
	protected void setProductSearcher() {
		productsearcher = manager.getSearcher(catalogid, "product");
	}
	public Searcher getProductSearcher() {
		return productsearcher;
	}
	
	protected void setOrderSearcher() {
		ordersearcher = manager.getSearcher(catalogid, "rogers_order");
	}
	public Searcher getOrderSearcher() {
		return ordersearcher;
	}
	
	protected void setItemSearcher() {
		itemsearcher = manager.getSearcher(catalogid, "rogers_order_item");
	}
	public Searcher getItemSearcher() {
		return itemsearcher;
	}
	
	void setStoreSearcher() {
		storesearcher = manager.getSearcher(catalogid, "store");
	}
	public Searcher getStoreSearcher() {
		return storesearcher;
	}
	
	void setManufacturerSearcher() {
		manufacturersearcher = manager.getSearcher(catalogid , "manufacturer");
	}
	public Searcher getManufacturerSearcher() {
		return manufacturersearcher;
	}

	void setInvoiceSearcher() {
		invoicesearcher = manager.getSearcher(catalogid , "invoice");
	}
	public Searcher getInvoiceSearcher() {
		return invoicesearcher;
	}
	
	void setInvoiceItemsSearcher() {
		invoiceitemssearcher = manager.getSearcher(catalogid , "invoiceitem");
	}
	public Searcher getInvoiceItemsSearcher() {
		return invoiceitemssearcher;
	}
	
	protected void setErrorSearcher() {
		errorsearcher = manager.getSearcher(catalogid, "errormessage");
	}
	public Searcher getErrorSearcher() {
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
		Searcher distributorsearcher = manager.getSearcher(archive.getCatalogId(), "distributor");
		Data targetDistributor = distributorsearcher.searchByField(SEARCH_FIELD, searchForName);

		return targetDistributor;
	}
	
	public HitTracker searchForItem( SearcherManager manager,
	MediaArchive archive, String orderID, String store, String distributor) {

		itemsearcher = manager.getSearcher(archive.getCatalogId(), "rogers_order_item");
		SearchQuery itemQuery = itemsearcher.createSearchQuery();
		itemQuery.addExact("store", storeID);
		itemQuery.addExact("rogers_order", orderid);
		itemQuery.addExact("distributor", distributor.name);
		HitTracker orderitems = itemsearcher.search(itemQuery);

		return orderitems;

	}
	public Data searchForOrder( String searchForName ) {
		Data rogersOrder = ordersearcher.searchById(searchForName);
		return rogersOrder;
	}

	public Data searchForStore( String searchForName ) {
		String SEARCH_FIELD = "store";
		Data rogersStore = storesearcher.searchByField(SEARCH_FIELD, searchForName);
		return rogersStore;
	}
	public Data searchForStoreByID( String storeID ) {
		Data rogersStore = storesearcher.searchById(storeID);
		return rogersStore;
	}
		
	public HitTracker searchForStoreInOrder( String orderid, String store_number ) {
		SearchQuery itemQuery = storesearcher.createSearchQuery();
		itemQuery.addExact("store", store_number);
		itemQuery.addExact("rogers_order", orderid);
		HitTracker orderitems = storesearcher.search(itemQuery);
		return orderitems;
	}

	public Data searchForProductbyUPC( String searchForName ) {
		String SEARCH_FIELD = "upc";
		Data product = productsearcher.searchByField(SEARCH_FIELD, searchForName);
		return product;
	}
	
	public Data searchForProductbyRogersSKU( String searchForName ) {
		String SEARCH_FIELD = "rogerssku";
		Data product = productsearcher.searchByField(SEARCH_FIELD, searchForName);
		return product;
	}
	
	public String getManufacturerID(String searchForName) {
		Data manufacturer = manufacturersearcher.searchByField("name", searchForName);
		if (manufacturer == null) {
			manufacturer = addManufacturer(searchForName);
		}
		return manufacturer.getId();
	}

	// ADD UTILITIES
	public Data addManufacturer(String manufacturerName) {
		Data manufacturer = manufacturersearcher.searchByField("name", manufacturerName);
		if (manufacturer == null) {
			manufacturer = manufacturersearcher.createNewData();
			manufacturer.setId(manufacturersearcher.nextId());
			manufacturer.setSourcePath(manufacturer.getId());
			manufacturer.setName(manufacturerName);
			manufacturersearcher.saveData(manufacturer, context.getUser());
			Data newManufacturer = manufacturersearcher.searchByField("name", manufacturerName);
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
		Data storeInfo = storesearcher.searchByField("store", storeNumber);
		if (storeInfo != null) {
			id = storeInfo.getId();
		} else {
			String inMsg = "Store cannot be found(" + storeNumber + ")";
			throw new OpenEditException(inMsg)
		}
		return id;
	}
	
	public String getStoreName(String storeID) {
		Data storeInfo = storesearcher.searchById(storeID);
		return storeInfo.getName();
	}
	
	public String getProductName(String productID) {
		Data productInfo = productsearcher.searchById(productID);
		return productInfo.getName();
	}
}
