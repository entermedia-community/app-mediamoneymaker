package org.openedit.store.util;

import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.entermedia.MediaArchive;
import org.openedit.store.Product;
import org.openedit.store.orders.Order;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.entermedia.scripts.EnterMediaObject;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;

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
	protected Searcher usersearcher;
	protected Searcher userprofilesearcher;
	
	public MediaUtilities() {
	}

	////////////////////////////////////////////
	// GETTERS AND SETTERS for Searchers
	////////////////////////////////////////////
	public String getCatalogid() {
		if (catalogid == null) {
			catalogid = getArchive().getCatalogId();
		}
		return catalogid;
	}
	
	public MediaArchive getArchive() {
		if (archive == null) {
			archive = (MediaArchive) getContext().getPageValue("mediaarchive");
		}
		return archive;
	}
	
	public SearcherManager getManager() {
		if (manager == null) {
			manager = (SearcherManager) getArchive().getSearcherManager();			
		}
		return manager;
	}
	
	public Searcher getDistributorSearcher() {
		if (distributorsearcher == null) {
			distributorsearcher = (Searcher) getManager().getSearcher(getCatalogid(), "distributor");			
		}
		return distributorsearcher;
	}
	
	public Searcher getProductSearcher() {
		if (productsearcher == null) {
			productsearcher = (Searcher) getManager().getSearcher(getCatalogid(), "product");	
		}
		return productsearcher;
	}
	
	public Searcher getOrderSearcher() {
		if (ordersearcher == null) {
			ordersearcher = (Searcher) getManager().getSearcher(getArchive().getCatalogId(), "storeOrder");
		}
		return ordersearcher;
	}
	
	public Searcher getStoreSearcher() {
		if (storesearcher == null) {
			storesearcher = (Searcher) getManager().getSearcher(getCatalogid(), "store");			
		}
		return storesearcher;
	}
	
	public Searcher getManufacturerSearcher() {
		if (manufacturersearcher == null) {
			manufacturersearcher = (Searcher) getManager().getSearcher(getCatalogid(), "manufacturer");
		}
		return manufacturersearcher;
	}

	public Searcher getInvoiceSearcher() {
		if (invoicesearcher == null) {
			invoicesearcher = (Searcher) getManager().getSearcher(getCatalogid(), "invoice");
		}
		return invoicesearcher;
	}
	
	public Searcher getInvoiceItemsSearcher() {
		if (invoiceitemssearcher == null) {
			invoiceitemssearcher = (Searcher) getManager().getSearcher(getCatalogid(), "invoiceitem");
		}
		return invoiceitemssearcher;
	}
	
	public Searcher getUserSearcher() {
		if (usersearcher == null) {
			usersearcher = (Searcher) getManager().getSearcher("system", "user");
		}
		return usersearcher;
	}
	
	public Searcher getUserProfileSearcher() {
		if (userprofilesearcher == null) {
			userprofilesearcher = (Searcher) getArchive().getSearcher("userprofile");
		}
		return userprofilesearcher;
	}
	
	////////////////////////////////////////////
	// OTHER PUBLIC METHODS
	////////////////////////////////////////////

	public Data searchForDistributor(String searchForName) {
		String SEARCH_FIELD = "name";
		Data targetDistributor = (Data) getDistributorSearcher().searchByField(SEARCH_FIELD, searchForName);

		return targetDistributor;
	}

	public Data searchForDistributor( String searchForName, Boolean production ) {

		String SEARCH_FIELD = "";
		if (production == true){
			SEARCH_FIELD = "headermailboxprod";
		} else {
			SEARCH_FIELD = "headermailboxtest";
		}
		Data targetDistributor = (Data) getDistributorSearcher().searchByField(SEARCH_FIELD, searchForName);

		return targetDistributor;
	}
	
	public Order searchForOrder( String inId ) {
		Order rogersOrder = (Order) getOrderSearcher().searchById(inId);
		if (rogersOrder == null) {
			return null;
		}
		return rogersOrder;
	}

	public Data searchForStore( String searchForName ) {
		String SEARCH_FIELD = "store";
		Data rogersStore = (Data) getStoreSearcher().searchByField(SEARCH_FIELD, searchForName);
		return rogersStore;
	}
	
	public Data searchForStoreByID( String storeID ) {
		Data rogersStore = (Data) getStoreSearcher().searchById(storeID);
		return rogersStore;
	}
		
	public HitTracker searchForStoreInOrder( String orderid, String store_number ) {
		SearchQuery itemQuery = getStoreSearcher().createSearchQuery();
		itemQuery.addExact("store", store_number);
		itemQuery.addExact("rogers_order", orderid);
		HitTracker orderitems = (HitTracker) getStoreSearcher().search(itemQuery);
		return orderitems;
	}

	public Product searchForProduct( String productID ) {
		Product product = (Product) getProductSearcher().searchById(productID);
		return product;
	}
	public Data searchForProductByField( String inSearchField, String searchForName ) {
		Data product = (Data) getProductSearcher().searchByField(inSearchField, searchForName);
		return product;
	}

	public String getManufacturerID(String searchForName) {
		Data manufacturer = (Data) getManufacturerSearcher().searchByField("name", searchForName);
		if (manufacturer == null) {
			manufacturer = (Data) addManufacturer(searchForName);
		}
		return manufacturer.getId();
	}

	// ADD UTILITIES
	public Data addManufacturer(String manufacturerName) {
		Data manufacturer = (Data) getManufacturerSearcher().searchByField("name", manufacturerName);
		if (manufacturer == null) {
			manufacturer = getManufacturerSearcher().createNewData();
			manufacturer.setId(getManufacturerSearcher().nextId());
			manufacturer.setSourcePath(manufacturer.getId());
			manufacturer.setName(manufacturerName);
			getManufacturerSearcher().saveData(manufacturer, getContext().getUser());
		}
		return manufacturer;
	}
	
	public String getStoreID(String storeNumber) {
		String id = "";
		Data storeInfo = (Data) getStoreSearcher().searchByField("store", storeNumber);
		if (storeInfo != null) {
			id = storeInfo.getId();
		} else {
			String inMsg = "Store cannot be found(" + storeNumber + ")";
			throw new OpenEditException(inMsg);
		}
		return id;
	}
	
	public String getStoreName(String storeID) {
		Data storeInfo = (Data) getStoreSearcher().searchById(storeID);
		return storeInfo.getName();
	}
	
	public String getProductName(String productID) {
		Data productInfo = (Data) getProductSearcher().searchById(productID);
		return productInfo.getName();
	}
}
