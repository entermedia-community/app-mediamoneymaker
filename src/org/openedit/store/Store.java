/*
 * Created on Mar 2, 2004
 */
package org.openedit.store;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.openedit.Data;
import org.openedit.data.PropertyDetailsArchive;
import org.openedit.data.SearcherManager;
import org.openedit.event.WebEvent;
import org.openedit.event.WebEventListener;
import org.openedit.links.Link;
import org.openedit.repository.ContentItem;
import org.openedit.store.convert.ConvertStatus;
import org.openedit.store.customer.Customer;
import org.openedit.store.orders.Order;
import org.openedit.store.orders.OrderArchive;
import org.openedit.store.orders.OrderGenerator;
import org.openedit.store.orders.OrderProcessor;
import org.openedit.store.orders.OrderSearcher;
import org.openedit.store.orders.OrderState;
import org.openedit.store.process.ElectronicOrderManager;

import org.openedit.store.search.ProductSearcher;
import org.openedit.store.search.ProductSecurityArchive;
import org.openedit.store.search.SearchFilterArchive;
import org.openedit.xml.XmlArchive;
import org.openedit.xml.XmlFile;

import com.openedit.ModuleManager;
import com.openedit.OpenEditException;
import com.openedit.OpenEditRuntimeException;
import com.openedit.WebPageRequest;
import com.openedit.config.Configuration;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.page.Page;
import com.openedit.page.manage.MimeTypeMap;
import com.openedit.page.manage.PageManager;
import com.openedit.users.User;
import com.openedit.util.PathUtilities;
import com.openedit.web.Crumb;

/**
 * @author cburkey
 * 
 */
public class Store {
	protected File fieldRootDirectory;
	protected File fieldStoreDirectory;
	protected long fieldLastModified;
	protected Set fieldCreditCardTypes;
	protected String fieldSmtpServer;
	protected List fieldToAddresses;
	protected List fieldNotifyAddresses;
	protected String fieldFromAddress;
	protected String fieldEmailLayout;
	protected String fieldOrderLayout;
	protected String fieldStatusEmailLayout;
	protected String fieldHostName;
	protected String fieldName; // this is the name of the store
	protected String fieldCatalogId;
	protected Map fieldItems;
	protected ProductArchive fieldProductArchive;
	protected ProductArchive fieldMirrorProductArchive;
	protected List fieldTaxRates;
	protected ProductSearcher fieldProductSearch;
	protected List fieldAllShippingMethods;
	private static final Log log = LogFactory.getLog(Store.class);
	protected CatalogConverter fieldImportConverter;
	protected CategoryArchive fieldCategoryArchive;
	protected CustomerArchive fieldCustomerArchive;
	protected OrderArchive fieldOrderArchive;
	protected OrderProcessor fieldOrderProcessor;
	protected OrderSearcher fieldOrderSearch;
	protected OrderGenerator fieldOrderGenerator;
	protected ProductExport fieldProductExport;
	protected boolean fieldAssignShippingMethod;
	protected boolean fieldAllowSpecialRequest;
	protected boolean fieldCouponsAccepted;
	protected boolean fieldUsesPoNumbers;
	protected boolean fieldDisplayTermsConditions;
	protected boolean fieldUsesBillMeLater;
	protected boolean fieldAllowDuplicateAccounts;
	protected boolean fieldAutoCapture;
	protected boolean fieldUseSearchSecurity;
	protected ProductPathFinder fieldProductPathFinder;
	protected Configuration fieldConfiguration;
	protected XmlArchive fieldXmlArchive;
	protected PropertyDetailsArchive fieldFieldArchive;
	protected ElectronicOrderManager fieldElectronicOrderManager;
	protected SearcherManager fieldSearcherManager;
	protected List fieldImageList;
	protected PageManager fieldPageManager;
	protected StoreMediaManager fieldStoreMediaManager;
	
	
	
	public PageManager getPageManager() {
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager) {
		fieldPageManager = inPageManager;
	}

	protected SearchFilterArchive fieldSearchFilterArchive;
	protected ProductSecurityArchive fieldProductSecurityArchive;
	protected ModuleManager fieldModuleManager;
	protected MimeTypeMap fieldMimeTypeMap;
	protected WebEventListener fieldWebEventListener;
	
	
	public String getLinkToThumb(String sourcepath){
		return "/" + getCatalogId() + "/downloads/preview/thumb/productimages/" + sourcepath + "/thumb.jpg";
	}
	public String getLinkToMedium(String sourcepath){
		return "/" + getCatalogId() + "/downloads/preview/medium/productimages/" + sourcepath + "/thumb.jpg";
	}
	public ProductSecurityArchive getProductSecurityArchive() {
		return fieldProductSecurityArchive;
	}

	public StoreMediaManager getStoreMediaManager() {
		
		return fieldStoreMediaManager;
	}

	public void setStoreMediaManager(StoreMediaManager inStoreMediaManager) {
		inStoreMediaManager.setStore(this);
		fieldStoreMediaManager = inStoreMediaManager;
	}

	public void setProductSecurityArchive(ProductSecurityArchive inProductSecurityArchive) {
		fieldProductSecurityArchive = inProductSecurityArchive;
		inProductSecurityArchive.setStore(this);
		
	}

	public ElectronicOrderManager getElectronicOrderManager() {
		return fieldElectronicOrderManager;
	}

	public void setElectronicOrderManager(ElectronicOrderManager inElectronicOrderManager) {
		fieldElectronicOrderManager = inElectronicOrderManager;
	}

	public Store() {
		log.info("Created new store");
	}

	public Set getCreditCardTypes() {
		if (fieldCreditCardTypes == null) {
			fieldCreditCardTypes = new TreeSet();
		}
		return fieldCreditCardTypes;
	}
	
	/**
	 * Use getCategory
	 * @deprecated
	 * @param inCatalogId
	 * @return
	 * @throws StoreException
	 */
	public Category getCatalog(String inCatalogId) throws StoreException {
		return getCategory(inCatalogId);
	}

	public Category getCategory(String inCatalogId) throws StoreException {
		CategoryArchive archive = getCategoryArchive();
		if (archive != null) {
			return archive.getCategory(inCatalogId);
		} else {
			log.error("Archive is not set");
			return null;
		}
	}

	public Category getCategoryByName(String inCatname) throws StoreException {
		CategoryArchive archive = getCategoryArchive();
		if (archive != null) {
			return archive.getCategoryByName(inCatname);
		} else {
			log.error("Archive is not set");
			return null;
		}
	}

	public void addCreditCardType(CreditCardType inCreditCardType) {
		getCreditCardTypes().add(inCreditCardType);
	}

	public void removeCreditCardType(CreditCardType inCreditCardType) {
		getCreditCardTypes().remove(inCreditCardType);
	}

	public CreditCardType getCreditCardType(String inString) {
		for (Iterator it = getCreditCardTypes().iterator(); it.hasNext();) {
			CreditCardType creditCardType = (CreditCardType) it.next();
			if (inString.equals(creditCardType.getId())) {
				return creditCardType;
			}
		}
		return null;
	}

	public String getStoreHome() {
		return "/" + getCatalogId();
	}

	public String 	getCatalogId() {
		return fieldCatalogId;
	}

	public void setCatalogId(String inString) {
		fieldCatalogId = inString;
	}

	// We have a chicken and egg problem here.
	// We need the storeloader to actually do the clear

	public void clear() throws Exception {
		getTaxRates().clear();
		clearProducts();
	}

	public List getTaxRates() {
	if (fieldTaxRates == null) {
		fieldTaxRates = new ArrayList();
		
	}

	return fieldTaxRates;
	}

	public void setTaxRates(List inTaxRates) {
		fieldTaxRates = inTaxRates;
	}

	public void clearProducts() {
		if (fieldProductArchive != null) {
			getProductArchive().clearProducts();
		}
	}

	public String getSmtpServer() {
		return fieldSmtpServer;
	}

	public List getToAddresses() {
		if (fieldToAddresses == null) {
			fieldToAddresses = new ArrayList();
		}
		return fieldToAddresses;
	}

	public void setSmtpServer(String inString) {
		fieldSmtpServer = inString;
	}

	public void setToAddresses(List inList) {
		fieldToAddresses = inList;
	}

	public void addToAddress(String inToAddress) {
		if (inToAddress.indexOf(',') != -1)
		{
			String[] addresses = inToAddress.split(",");
			for (int i = 0; i < addresses.length; i++)
			{
				if (!getToAddresses().contains(addresses[i].trim()))
				{
					getToAddresses().add(addresses[i].trim());
				}
			}
		} 
		else 
		{
			if (!getToAddresses().contains(inToAddress))
			{
				getToAddresses().add(inToAddress);
			}
		}
	}

	public void addNotifyAddress(String inToAddress) {
		getNotifyAddresses().add(inToAddress);
	}

	public String getEmailLayout() {
		return fieldEmailLayout;
	}

	public void setEmailLayout(String inEmailLayout) {
		fieldEmailLayout = inEmailLayout;
	}

	public String getFromAddress() {
		return fieldFromAddress;
	}

	public void setFromAddress(String inFromAddress) {
		fieldFromAddress = inFromAddress;
	}

	
	public void putTaxRate(TaxRate inRate) {
		getTaxRates().add(inRate);
	}

	

	public String getOrderLayout() {
		return fieldOrderLayout;
	}

	public void setOrderLayout(String inThanksLayout) {
		fieldOrderLayout = inThanksLayout;
	}
	public void setStatusEmailLayout(String inStatusLayout){
		fieldStatusEmailLayout = inStatusLayout;
	}
	public String getStatusEmailLayout(){
		return fieldStatusEmailLayout;
	}

	
	public Product getProduct(String inItem) throws StoreException {
		return getProductArchive().getProduct(inItem);
	}

	public ProductArchive getProductArchive() 
	{
		if( fieldProductArchive == null)
		{
			fieldProductArchive = (ProductArchive)getModuleManager().getBean(getCatalogId(), "productArchive");
		}
		return fieldProductArchive;
	}

	public void setProductArchive(ProductArchive inArchive) {
		fieldProductArchive = inArchive;
		if (inArchive != null) {
			inArchive.setStore(this);
		}

		// inStore.getCatalogArchive().setStore( inStore);
		// inStore.getProductArchive().setStore(inStore);
		// inStore.getStoreSearcher().setStore(inStore);

	}

	/**
	 * @param inItem
	 */
	public void saveProduct(Product inProduct) throws StoreException 
	{
		saveProduct(inProduct, null);
	}
	public void saveProduct(Product inProduct, User inUser) throws StoreException {
		getProductArchive().saveProduct(inProduct);
		if (getMirrorProductArchive() != null) {
			getMirrorProductArchive().saveProduct(inProduct, inUser);
		}
		//getProductArchive().saveBlankProductDescription(inProduct);
		getProductSearcher().updateIndex(inProduct);
	}

	public void saveProducts(List inProducts) throws StoreException {
		for (Iterator iter = inProducts.iterator(); iter.hasNext();) {
			Product product = (Product) iter.next();
			getProductArchive().saveProduct(product);
			if (getMirrorProductArchive() != null) {
				getMirrorProductArchive().saveProduct(product);
			}
			String desc = product.getDescription();
			getProductArchive().saveProductDescription(product, desc);
		}
		getProductSearcher().updateIndex(inProducts, true);
	}

	public void save() throws StoreException {
		getCategoryArchive().saveAll();
	}

	protected FileFilter xconfFilter() {
		return new FileFilter() {
			public boolean accept(File inFile) {
				if (inFile.isDirectory()
						|| inFile.getName().endsWith("_default.xconf")) {
					return false;
				}
				if (inFile.getName().endsWith("xconf")) {
					return true;
				}
				return false;
			}
		};
	}

//	public List listAllKnownProductIds() {
//	
//		return getProductArchive().listAllProductIds();
//	}

	// returns all of the related products in a map keyed by the catalogs

	

	

	/**
	 * 
	 */
	public void reindexAll() throws OpenEditException {
		getProductArchive().clearProducts(); // lets hope they are all saved
		// before we delete em
		getProductSearcher().reIndexAll();
		getOrderSearcher().reIndexAll();

	}

	public HitTracker search(String inSearch) throws OpenEditException 
	{
		SearchQuery query = getProductSearcher().createSearchQuery();
		query.addMatches("description",inSearch);
		return getProductSearcher().search(query);
	}
	
	public ProductSearcher getProductSearcher() 
	{
		if( fieldProductSearch == null)
		{
			fieldProductSearch = (ProductSearcher)getSearcherManager().getSearcher(getCatalogId(), "product");
		}
		return fieldProductSearch;
	}
	/**
	 * @deprecated
	 */
	public ProductSearcher getProductSearch() {
		return getProductSearcher();
	}

	public ShippingMethod findShippingMethod(String inId) {
		for (Iterator iter = getAllShippingMethods().iterator(); iter.hasNext();) {
			ShippingMethod method = (ShippingMethod) iter.next();
			if (method.getId().equals(inId)) {
				return method;
			}
		}
		return null;
	}

	public List getAllShippingMethods() {
		if (fieldAllShippingMethods == null) {
			fieldAllShippingMethods = new ArrayList();

		}

		return fieldAllShippingMethods;
	}

	public void setAllShippingMethods(List inAllShippingMethods) {
		fieldAllShippingMethods = inAllShippingMethods;
	}

	public File getStoreDirectory() {
		if (fieldStoreDirectory == null) {
			PageManager manager = (PageManager)getModuleManager().getBean("pageManager");
			ContentItem item = manager.getRepository().get("/" + getCatalogId());
			fieldStoreDirectory = new File(item.getAbsolutePath());
		}
		return fieldStoreDirectory;
	}

	public void setStoreDirectory(File inHome) {
		fieldStoreDirectory = inHome;
	}

	public CatalogConverter getCatalogImportConverter()
	{
		if( fieldImportConverter == null)
		{
			fieldImportConverter = (CatalogConverter)getModuleManager().getBean(getCatalogId(),"storeCatalogImportConverter");
		}
		return fieldImportConverter;
	}

	public void setCatalogImportConverter(
			CatalogConverter inCatalogImportConverter) {
		fieldImportConverter = inCatalogImportConverter;
	}

	/**
	 * 
	 */
	public synchronized ConvertStatus convertCatalog(User inUser, boolean inForce)
			throws Exception {
		ConvertStatus errors = new ConvertStatus();
		errors.setUser(inUser);
		errors.setForcedConvert(inForce);
		errors.add("conversion started on " + getCatalogId());
		if (getCatalogImportConverter() != null) {
			getCatalogImportConverter().convert(this, errors);
		}
		if (errors.isReindex()) {
			log.info("Convertion completed");
			getProductArchive().clearProducts();
			getCategoryArchive().reloadCategories();
			errors.add("reindexing " + getCatalogId());
			reindexAll();
			getProductArchive().clearProducts();
			return errors;
		} else {
			errors.add("No inventory changes found");
			return errors;
		}
	}
	/**
	 * @deprecated
	 */
	public CategoryArchive getCatalogArchive() {
		return getCategoryArchive();
	}

	public CategoryArchive getCategoryArchive()
	{
		if( fieldCategoryArchive == null)
		{
			fieldCategoryArchive = (CategoryArchive)getModuleManager().getBean(getCatalogId(), "storeCategoryArchive");
		}
		return fieldCategoryArchive;
	}

	public void setCategoryArchive(CategoryArchive inCatalogArchive) {
		fieldCategoryArchive = inCatalogArchive;
	}

	public CustomerArchive getCustomerArchive() 
	{
		if( fieldCustomerArchive == null)
		{
			fieldCustomerArchive = (CustomerArchive)getModuleManager().getBean(getCatalogId(), "customerArchive");
		}
		return fieldCustomerArchive;
	}

	public void setCustomerArchive(CustomerArchive inCustomerArchive) {
		fieldCustomerArchive = inCustomerArchive;
	}

	/**
	 * Save the order to the order archive system
	 * 
	 * @param inCart
	 */
	public void saveOrder(Order inOrder)
			throws StoreException {
		getCustomerArchive().saveAndExportCustomer(inOrder.getCustomer());

		OrderArchive orderArchive = getOrderArchive();
		orderArchive.saveOrder(this, inOrder);
		WebEvent event = new WebEvent();
		event.setCatalogId(getCatalogId());
		event.setSource(this);
		event.setProperty("user", inOrder.getCustomer().getId());
		event.setOperation("orderprocessed");
		event.setProperty("orderid", inOrder.getId());
		getWebEventListener().eventFired(event);
		
		// save the state again
		// orderArchive.changeOrderStatus(inOrder.getOrderState(), this,
		// inOrder);
		getOrderSearcher().updateIndex(inOrder);
	}

	public OrderArchive getOrderArchive()
	{
		if( fieldOrderArchive == null)
		{
			fieldOrderArchive = (OrderArchive)getModuleManager().getBean(getCatalogId(), "orderArchive");
		}
		return fieldOrderArchive;
	}

	public void setOrderArchive(OrderArchive inOrderArchive) {
		fieldOrderArchive = inOrderArchive;
	}

	public void saveCustomer(Customer customer) throws StoreException {
		getCustomerArchive().saveCustomer(customer);
	}

	/**
	 * @return
	 */
	public File getRootDirectory() {
		return fieldRootDirectory;
	}

	public void setRootDirectory(File inRoot) {
		fieldRootDirectory = inRoot;
	}

	public OrderGenerator getOrderGenerator() {
		return fieldOrderGenerator;
	}

	public void setOrderGenerator(OrderGenerator inOrderGenerator) {
		fieldOrderGenerator = inOrderGenerator;
	}

	public boolean isAssignShippingMethod() {
		return fieldAssignShippingMethod;
	}

	public void setAssignShippingMethod(boolean inAssignShippingMethod) {
		fieldAssignShippingMethod = inAssignShippingMethod;
	}

	/**
	 * These are used to notify people of an email
	 * 
	 * @return
	 */

	public List getNotifyAddresses() {
		if (fieldNotifyAddresses == null) {
			fieldNotifyAddresses = new ArrayList();
		}
		return fieldNotifyAddresses;
	}

	public void setNotifyAddresses(List inBccAddresses) {
		fieldNotifyAddresses = inBccAddresses;
	}

	public String getHostName() {
		return fieldHostName;
	}

	public void setHostName(String inHostName) {
		fieldHostName = inHostName;
	}

	public boolean areCouponsAccepted() {
		return fieldCouponsAccepted;
	}

	public void setCouponsAccepted(boolean inCouponsAccepted) {
		fieldCouponsAccepted = inCouponsAccepted;
	}

	public boolean usesPoNumbers() {
		return fieldUsesPoNumbers;
	}

	public void setUsesPoNumbers(boolean inUsesPoNumbers) {
		fieldUsesPoNumbers = inUsesPoNumbers;
	}

	public boolean displayTermsConditions() {
		return fieldDisplayTermsConditions;
	}

	public void setDisplayTermsConditions(boolean inDisplayTermsConditions) {
		fieldDisplayTermsConditions = inDisplayTermsConditions;
	}

	public boolean usesBillMeLater() {
		return fieldUsesBillMeLater;
	}

	public void setUsesBillMeLater(boolean inUsesBillMeLater) {
		fieldUsesBillMeLater = inUsesBillMeLater;
	}

	public boolean getAllowDuplicateAccounts() {
		return fieldAllowDuplicateAccounts;
	}

	public void setAllowDuplicateAccounts(boolean inAllowDuplicateAccounts) {
		fieldAllowDuplicateAccounts = inAllowDuplicateAccounts;
	}

	public ProductPathFinder getProductPathFinder() {
		return fieldProductPathFinder;
	}

	public void setProductPathFinder(ProductPathFinder inProductPathFinder) {
		fieldProductPathFinder = inProductPathFinder;
	}
	public ProductPathFinder getProductSourcePathFinder()
	{
		return getProductSearcher();
	}
//	
//	public String showThumb(String inId) {
//		StringBuffer path = new StringBuffer(getStoreHome());
//		path.append("/products/images/thumb/");
//		path.append(getProductPathFinder().idToPath(inId));
//		Image thumb = getImageCreator().getImage("thumb");
//		path.append(thumb.getPostfix());
//		path.append(".jpg");
//		return path.toString();
//	}

//	public String showMedium(String inId) {
//		StringBuffer path = new StringBuffer(getStoreHome());
//		path.append("/products/images/medium/");
//		path.append(getProductPathFinder().idToPath(inId));
//		
//		Image med = getImageCreator().getImage("medium");
//		path.append(med.getPostfix());
//		path.append(".jpg");
//		return path.toString();
//	}

	public HitTracker search(String inFinalq, String inOrdering)
			throws OpenEditException 
			{
		SearchQuery q = getProductSearcher().createSearchQuery();
		q.addMatches("description",inFinalq);
		q.setSortBy(inOrdering);

		HitTracker hits = getProductSearcher().search(q);

		return hits;
	}

	public boolean isAllowSpecialRequest() {
		return fieldAllowSpecialRequest;
	}

	public void setAllowSpecialRequest(boolean inAllowSpecialRequest) {
		fieldAllowSpecialRequest = inAllowSpecialRequest;
	}

	public List getProductsInCatalog(Category inCatalog)
			throws OpenEditException {
		if (inCatalog == null) {
			return null;
		}
		List products = new ArrayList();
		SearchQuery q = getProductSearcher().createSearchQuery();
		q.addMatches("category",inCatalog.getId());
		
		HitTracker hits = getProductSearcher().search(q);
		if (hits == null) {
			return products;
		}
		for (Iterator it = hits.iterator(); it.hasNext();) {
			Data doc = (Data) it.next();
			String id = doc.get("id");
			Product product = getProduct(id);
			if (product != null) {
				products.add(product);
			} else {
				log.info("Cannot find product with id " + id);
			}
		}
		return products;
	}
//This should be in ProductSearcheror in Velocity
//	public Product getRandomProduct(String inCatalogId) throws OpenEditException {
//		if (inCatalogId == null) {
//			return null;
//		}
//		
//		String query = "category:(" + inCatalogId + ")";
//		HitTracker hits = getProductSearch().search(query, null);
//		if (hits == null) {
//			return null;
//		}
//		int total = hits.getTotal();
//	    int num = getRandom().nextInt(total);
//	    Document doc;
//		try {
//			doc = (Document) hits.get(num);
//			String id = doc.get("id");
//			Product product = getProduct(id);
//		    return product;
//		} catch (IOException e) {
//			return null;
//		}
//	  
//	}

	public Crumb buildCrumb(Category inCatalog) {
		Crumb crumb = new Crumb();
		if (inCatalog == null) {
			crumb.setText("");
			return crumb;
		}
		crumb.setPath(getStoreHome() + "/categories/" + inCatalog.getId()
				+ ".html");
		crumb.setText(inCatalog.getName());
		if (inCatalog.getParentCatalog() != null) {
			crumb.setParent(buildCrumb(inCatalog.getParentCatalog()));
		}
		return crumb;
	}
	
	public Link buildLink(Category inCatalog) {
		return buildLink(inCatalog, null);
	}

	public Link buildLink(Category inCatalog, String prefix) {
		Link link = new Link();
		
		if (inCatalog == null) {
			link.setText("");
			return link;
		}
		if (prefix == null)
		{
			prefix = getStoreHome() + "/categories/";
		}
		link.setPath( prefix + inCatalog.getId()
				+ ".html");
		link.setText(inCatalog.getName());
		if (inCatalog.getParentCatalog() != null) {
			link.setParentLink(buildLink(inCatalog.getParentCatalog(), prefix));
		}
		return link;
	}
	
	public String getName() {
		return fieldName;
	}

	public void setName(String inName) {
		fieldName = inName;
	}

	public boolean isAutoCapture() {
		return fieldAutoCapture;
	}

	public void setAutoCapture(boolean inAutoCapture) {
		fieldAutoCapture = inAutoCapture;
	}

	public OrderState getOrderState(String inState) {
		OrderState state = (OrderState) getOrderArchive().getOrderStates(this)
				.get(inState);
		if (state == null) {
			log.error("Missing order states " + inState + " have "
					+ getOrderArchive().getOrderStates(this) + " on class "
					+ getOrderArchive());
			return null;
		}
		return state.copy();
	}

	public ProductExport getProductExport() {
		return fieldProductExport;
	}

	public void setProductExport(ProductExport inProductExport) {
		fieldProductExport = inProductExport;
	}

	public ProductArchive getMirrorProductArchive() {
		return fieldMirrorProductArchive;
	}

	public void setMirrorProductArchive(ProductArchive inMirrorProductArchive) {
		fieldMirrorProductArchive = inMirrorProductArchive;
		if (fieldMirrorProductArchive != null) {
			fieldMirrorProductArchive.setStore(this);
		}
	}

	public long getLastModified() {
		return fieldLastModified;
	}

	public void setLastModified(long inLastModified) {
		fieldLastModified = inLastModified;
	}

	protected Configuration getConfiguration() {
		return fieldConfiguration;
	}

	public void setConfiguration(Configuration inConfiguration) {
		fieldConfiguration = inConfiguration;
	}

	public String getConfigValue(String inKey) {
		String val = getConfiguration().getChildValue(inKey);
		return val;
	}

	// public Types getFields(String inName) throws OpenEditException {
	// XmlFile settings = loadXml("fields", inName, "property");
	// if (settings.isExist()) {
	// return settings;
	// } else {
	// return null;
	// }
	// }
	/**
	 * 
	 */
	public HitTracker getProperties(String inName) throws OpenEditException {
		// XmlFile settings = loadXml("lists", inName, "property");
		// return settings;
		return getSearcherManager().getList(getCatalogId(), inName);
	}

	/**
	 * @deprecated Use store.getProductDetails()
	 * @param inType
	 * @param inUser
	 * @return
	 * @throws Exception
	 */
	public List getDataProperties(String inScreenName, User inUser)
			throws Exception {
		return getProductDetails(inScreenName, inUser);
	}

	/**
	 * This returns a particular set of properties, which is a subset of the
	 * properties found in store/data/productproperties.xml
	 * 
	 * @param inSubsetProperties
	 *            Identifies the file to look in for the subset of properties to
	 *            take from those found in productproperties.xml The file is
	 *            store/data/fields/properties{inSubsetProperties}.xml
	 * @param inUser
	 * @return A List of Details
	 */
	public List getProductDetails(String inPropertiesName, User inUser)
			throws Exception {
		return getFieldArchive().getDataProperties("product", inPropertiesName,
				inUser);
	}


	private XmlFile loadXml(String basedir, String inName, String inType)
			throws OpenEditException {
		String path = "/" + getCatalogId() + "/data/" + basedir + "/" + inName + ".xml";
		XmlFile settings = getXmlArchive().getXml(path, inType);
		return settings;
	}

	public XmlArchive getXmlArchive() {
		return fieldXmlArchive;
	}

	public void setXmlArchive(XmlArchive inXmlArchive) {
		fieldXmlArchive = inXmlArchive;
	}

	public List listAllSortByFields() {
		return getProductArchive().getPropertyDetails().findIndexProperties();
	}
	
	public PropertyDetailsArchive getFieldArchive() {
		return getProductSearcher().getPropertyDetailsArchive();
	}


//	public List getExactProductsInCategory(Category inCatalog)
//			throws OpenEditException {
//		if (inCatalog == null) {
//			return null;
//		}
//		List products = new ArrayList();
//		String query = "category-exact:(" + inCatalog.getId() + ")";
//		HitTracker hits = getProductSearch().search(query, null);
//		if (hits == null) {
//			return products;
//		}
//		for (Iterator it = hits.getAllHits(); it.hasNext();) {
//			Document doc = (Document) it.next();
//			String id = doc.get("id");
//			Product product = getProduct(id);
//			if (product != null) {
//				products.add(product);
//			} else {
//				log.info("Cannot find product with id " + id);
//			}
//		}
//		return products;
//	}

	public OrderSearcher getOrderSearcher() 
	{
		if( fieldOrderSearch == null)
		{
			fieldOrderSearch = (OrderSearcher)getSearcherManager().getSearcher(getCatalogId(), "storeOrder");
			
			//fieldOrderSearch.setStore(this); //TODO: Remove this need for a store use catalogid
			
			fieldOrderSearch.setOrderArchive(getOrderArchive()); //TODO: Make sure we set the orderarchive catalogid
		}
		return fieldOrderSearch;
	}

	public void clearSystemWideFilter(String inDataLevel)
			throws OpenEditException {

		XmlFile settings = null;
		if (inDataLevel != null) {
			settings = loadXml("systemfilters", "searchfilter" + inDataLevel,
					"filter");
			if (settings.isExist()) {
				getXmlArchive().deleteXmlFile(settings);
			}

		}

	}

//	public void updateSystemWideFilter(String datalevel, String value,
//			String filtertype, String filterid) throws OpenEditException {
//		XmlFile systemfilters = loadSystemWideFilters(datalevel);
//		Element child = systemfilters.addNewElement();
//		child.addAttribute("id", filterid);
//		child.setText(filtertype + ":" + value);
//		getXmlArchive().saveXml(systemfilters, null);
//	}

	public void processOrder(WebPageRequest inPageRequest, Order order)
			throws StoreException {
		getOrderProcessor().processNewOrder(inPageRequest, this, order);
		

	}

	public WebEventListener getWebEventListener()
	{
		return fieldWebEventListener;
	}

	public void setWebEventListener(WebEventListener inWebEventListener)
	{
		fieldWebEventListener = inWebEventListener;
	}

	public OrderProcessor getOrderProcessor() {
		return fieldOrderProcessor;
	}

	public void setOrderProcessor(OrderProcessor inOrderProcessor) {
		fieldOrderProcessor = inOrderProcessor;
	}
	public boolean isUseSearchSecurity() {
		return fieldUseSearchSecurity;
	}

	public void setUseSearchSecurity(boolean inUseSearchSecurity) {
		fieldUseSearchSecurity = inUseSearchSecurity;
	}
	
	public void reloadSearchIndex(){
		getProductSearch().clearIndex();
	}

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}
	/**
	 * Returns a list of potential (not actual) images, valid for any catalog.
	 * 
	 * @return A {@link List} of {@link Image}s
	 */
	public List getImageList()
	{
		if (fieldImageList == null)
		{

			List arrayList = new ArrayList();
			try
			{
				String path = "/" + getCatalogId() + "/data/imagelist.xml";
				XmlFile config = getXmlArchive().getXml(path,"image");
				if( config.isExist())
				{
					Element root = config.getRoot();
					for (Iterator iter = root.elementIterator("image"); iter.hasNext();)
					{
						Element type = (Element) iter.next();
						String name = type.attributeValue("name");
						String id = type.attributeValue("id");

						String postfix = type.attributeValue("postfix");
						int width = Integer.parseInt(type.attributeValue("width"));
						int height;
						String heightString = type.attributeValue("height");
						if (heightString != null && heightString.length() > 0)
						{
							height = Integer.parseInt(heightString);

						}
						else
						{
							height = 10000;
						}
						String sizetype = type.attributeValue("type");
						Image thumb = new Image(name, width, height, postfix);
						thumb.setId(id);
						thumb.setType(sizetype);
						arrayList.add(thumb);
					}
				}
			}
			catch (Exception ex)
			{
				throw new OpenEditRuntimeException(ex);
			}
			fieldImageList = arrayList;
		}
		return fieldImageList;
	}

	public List getImageList(String inType) throws StoreException
	{
		List r = new ArrayList();
		List items = getImageList();
		for (Iterator iter = items.iterator(); iter.hasNext();)
		{
			Image image = (Image) iter.next();
			if (image.getType().equals(inType))
			{
				r.add(image);
			}
		}
		return r;
	} 
	public String getLinkToProduct(String inSourcePath)
	{
		return getLinkToProduct(inSourcePath, "/" + getCatalogId() + "/products");
	}
	public String getLinkToProduct(String inSourcePath, String productrooot)
	{
		//TODO: 
	 	if (inSourcePath.endsWith("/"))
	 	{
			return productrooot + "/" + inSourcePath + "index.html";
	 	}
	 	else
	 	{
			return productrooot + "/" + inSourcePath + ".html";
	 	}
	}
	
//		public String getLinkToThumb(String inSourcePath)
//		{
//			return getImageCreator().getLinkToThumb(getCatalogId(), inSourcePath);
//		}
//		public String getLinkToMedium(String inSourcePath)
//		{
//			return getImageCreator().getLinkToMedium(getCatalogId(), inSourcePath, null);
//		}
	
	/**
	 * 
	 * @param inSourcePath
	 * @param inFileName
	 * @param inExtension
	 * @deprecated use getLinkToThumb(String)
	 * @return
	 */
	
	

	public Collection listAllKnownProductIds()
	{
		return getProductSearcher().getAllHits();
	}



	public String getSourcePathForPage(WebPageRequest inPage)
	{
		String sourcePath = null;
		String productrootfolder = inPage.getPage().get("productrootfolder");
		//log.info(inPage.getPathUrl());
		if( productrootfolder != null)
		{
			sourcePath = inPage.getPath().substring(productrootfolder.length() + 1);

			String orig = inPage.getPage().get("sourcepathhasfilename");
			if( Boolean.parseBoolean(orig))
			{
				//Take off the extra test.eps part
				sourcePath = PathUtilities.extractDirectoryPath(sourcePath);
			}
			else
			{
				//Take off the extra extension
				sourcePath = PathUtilities.extractPagePath(sourcePath);
			}
		}
		if(sourcePath.endsWith("folder")){
		 sourcePath = PathUtilities.extractDirectoryPath(sourcePath);
		 sourcePath = sourcePath + "/"; 
		}
		return sourcePath;
	}

	public Product getProductBySourcePath(String inSourcePath)
	{
		return getProductArchive().getProductBySourcePath(inSourcePath);
	}
	
	/*
	 * Creates a product at the specified sourcePath, but does not save or index.
	 */
	
	public Product createProduct(String inSourcePath)
	{
		Product product = new Product();
		product.setCatalogId(getCatalogId());
		String id = inSourcePath.toLowerCase(); 
		id = id.replace('/', '_'); //For ian
		id = PathUtilities.extractId( id , true); 
		product.setId(id);
		product.setSourcePath(inSourcePath);
		String name;
		if (inSourcePath.endsWith("/"))
		{
			name = PathUtilities.extractDirectoryName(inSourcePath);
		}
		else
		{
			name = PathUtilities.extractFileName(inSourcePath);
		}
		product.setName(name);
		String xconfpath = getProductArchive().buildXconfPath(product);
		Page page = getPageManager().getPage(xconfpath);
		product.setSourcePage(page);
		return product;
	}

	public Product getProductBySourcePath(Page inPage)
	{
		String productrootfolder = inPage.get("productrootfolder");
		//log.info(inPage.getPathUrl());
		if( productrootfolder == null || productrootfolder.length() >= inPage.getPath().length() )
		{
			return null;
		}
		if( !inPage.getPath().startsWith(productrootfolder))
		{
			return null;
		}
		String	sourcePath = inPage.getPath().substring(productrootfolder.length() + 1);
		if (sourcePath.endsWith("index.html"))
		{
			sourcePath = PathUtilities.extractDirectoryPath(sourcePath) + "/"; //folder product
		}
		else
		{
			sourcePath = PathUtilities.extractPagePath(sourcePath);
		}
		
		
		Product product = getProductBySourcePath(sourcePath);
		return product;
	}

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}



	public List getTaxRatesFor(String inState) {
		ArrayList list = new ArrayList();
		for (Iterator iterator = getTaxRates().iterator(); iterator.hasNext();) {
			TaxRate rate = (TaxRate) iterator.next();
			if(rate.getState().equalsIgnoreCase(inState)){
				list.add(rate);
			}
		}
		return list;
	
	}
}
