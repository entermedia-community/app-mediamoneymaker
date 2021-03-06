package org.openedit.store.search;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.openedit.Data;
import org.openedit.data.CompositeData;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.lucene.BaseLuceneSearcher;
import org.openedit.data.lucene.LuceneSearchQuery;
import org.openedit.links.Link;
import org.openedit.profile.UserProfile;
import org.openedit.store.Cart;
import org.openedit.store.Category;
import org.openedit.store.Product;
import org.openedit.store.ProductArchive;
import org.openedit.store.ProductPathFinder;
import org.openedit.store.Store;
import org.openedit.store.StoreArchive;
import org.openedit.store.StoreException;
import org.openedit.util.DateStorageUtil;

import com.openedit.OpenEditException;
import com.openedit.OpenEditRuntimeException;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.page.Page;
import com.openedit.page.PageSettings;
import com.openedit.page.manage.PageManager;
import com.openedit.users.Group;
import com.openedit.users.User;
import com.openedit.util.PathUtilities;

public class ProductLuceneSearcher extends BaseLuceneSearcher implements ProductSearcher, ProductPathFinder
{
	static final Log log = LogFactory.getLog(ProductLuceneSearcher.class);
	protected static final String CATALOGIDX = "catalogid";
	protected static final String CATEGORYID = "categoryid";
	protected Store fieldStore;
	protected DecimalFormat fieldDecimalFormatter;
	protected PageManager fieldPageManager;
	protected Map fieldProductPaths;
	private Boolean fieldUsesSearchSecurity;
	protected ProductLuceneIndexer fieldIndexer;
	protected StoreArchive fieldStoreArchive;
	
	public ProductLuceneSearcher()
	{
		setFireEvents(true);
	}

	/**
	 * @see org.openedit.store.search.ProductSearcher#fieldSearch(com.openedit.WebPageRequest,
	 *      org.openedit.store.Cart)
	 */
	public void fieldSearch(WebPageRequest inPageRequest, Cart cart) throws OpenEditException
	{

		SearchQuery search = addStandardSearchTerms(inPageRequest);
		if (search == null)
		{
			return; // Noop
		}
		// Catalog stuff
		String withincatalog = inPageRequest.getRequestParameter("department");
		Category department = null;
		if (withincatalog != null && !"all".equals(withincatalog))
		{
			department = getStore().getCategory(withincatalog);
			search.addMatches("category", withincatalog);
			//search.putInput("department", department.getId());
		}
		cart.setLastVisitedCatalog(department);
		
		String type = inPageRequest.getRequestParameter("type");
		//search.putInput("type", type);

		// Filter stuff
		String include = inPageRequest.getRequestParameter("search.includefilter");
		boolean includeFilter = Boolean.parseBoolean(include);
				search(inPageRequest, search);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openedit.store.search.ProductSearcher#searchCatalogs(com.openedit.WebPageRequest,
	 *      org.openedit.store.Cart)
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openedit.store.search.ProductSearcher#searchCatalogs(com.openedit.WebPageRequest,
	 *      org.openedit.store.Cart)
	 */
	public void searchCatalogs(WebPageRequest inPageRequest, Cart cart) throws Exception
	{
		SearchQuery search = createSearchQuery();

		Category catalog = null;
		String catalogId = inPageRequest.getRequestParameter(CATEGORYID);
		if (catalogId == null)
		{
			Page page = inPageRequest.getPage();
			catalogId = page.get(CATEGORYID);
		}
		if (catalogId == null)
		{
			catalog = (Category) inPageRequest.getPageValue("category");
		}

		if (catalog == null && catalogId == null)
		{
			// get it from the path?
			String path = inPageRequest.getPath();

			catalogId = PathUtilities.extractPageName(path);
			if (catalogId.endsWith(".draft"))
			{
				catalogId = catalogId.replace(".draft", "");
			}
		}

		// Why the content page? Page page = inPageRequest.getContentPage();
		if (catalog == null)
		{
			catalog = getStore().getCategory(catalogId);
		}
		if (catalog == null)
		{
			if (inPageRequest.getContentPage() == inPageRequest.getPage())
			{
				String val = inPageRequest.findValue("showmissingcategories");
				if (!Boolean.parseBoolean(val))
				{
					inPageRequest.redirect(getStore().getStoreHome() + "/search/nosuchcatalog.html");
				}
			}
			log.error("No such catalog " + catalogId);
			return;
		}
		else
		{
			catalogId = catalog.getId();
		}
		inPageRequest.putPageValue("catalog", catalog); // @deprecated
		inPageRequest.putPageValue("category", catalog); // @deprecated

		cart.setLastVisitedCatalog(catalog); // this is prone
		String actualid = catalogId;
		if (catalog.getLinkedToCategoryId() != null)
		{
			actualid = catalog.getLinkedToCategoryId();
		}

		search.addMatches("category", actualid);

		boolean selected = true;
		if (catalog.getParentCatalog() == null)
		{
			selected = false; // The top level catalog does not count as a
			// selection
		}

		Link crumb = getStore().buildLink(catalog, inPageRequest.findValue("url-prefix"));
		inPageRequest.putSessionValue("crumb", crumb);

		String sortBy = catalog.get("sortfield");
		search.setSortBy(sortBy);

		cachedSearch(inPageRequest, search);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openedit.store.search.ProductSearcher#searchExactCatalogs(com.openedit.WebPageRequest,
	 *      org.openedit.store.Cart)
	 */
	public void searchExactCatalogs(WebPageRequest inPageRequest, Cart cart) throws Exception
	{
		SearchQuery search = new LuceneSearchQuery();

		String catalogId = inPageRequest.getRequestParameter(CATEGORYID);
		if (catalogId == null)
		{
			Page page = inPageRequest.getPage();
			catalogId = page.get(CATEGORYID);
		}
		if (catalogId == null)
		{
			// get it from the path?
			String path = inPageRequest.getPath();

			catalogId = PathUtilities.extractPageName(path);
			if (catalogId.endsWith(".draft"))
			{
				catalogId = catalogId.replace(".draft", "");
			}
		}

		// Why the content page? Page page = inPageRequest.getContentPage();

		Category catalog = getStore().getCategory(catalogId);
		if (catalog == null)
		{
			if (inPageRequest.getContentPage() == inPageRequest.getPage())
			{
				String val = inPageRequest.findValue("showmissingcategories");
				if (!Boolean.parseBoolean(val))
				{
					inPageRequest.redirect(getStore().getStoreHome() + "/search/nosuchcatalog.html");
				}
			}
			log.error("No such catalog " + catalogId);
			return;
		}
		inPageRequest.putPageValue("catalog", catalog); // @deprecated
		inPageRequest.putPageValue("category", catalog); // @deprecated

		cart.setLastVisitedCatalog(catalog); // this is prone
		// to error

		// boolean includechildren = false;
		// if (catalog.getParentCatalog() == null) // this is the root level
		// {
		// includechildren = true; // since products dont mark themself in the
		// // index catalog
		// }
		String actualid = catalogId;
		if (catalog.getLinkedToCategoryId() != null)
		{
			actualid = catalog.getLinkedToCategoryId();
		}

		search.addMatches("category-exact", actualid);

		boolean selected = true;
		if (catalog.getParentCatalog() == null)
		{
			selected = false; // The top level catalog does not count as a
			// selection
		}

		Link crumb = getStore().buildLink(catalog, inPageRequest.findValue("url-prefix"));
		inPageRequest.putSessionValue("crumb", crumb);

		String sortBy = catalog.get("sortfield");
		search.setSortBy(sortBy);

		cachedSearch(inPageRequest, search);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openedit.store.search.ProductSearcher#searchStore(com.openedit.WebPageRequest,
	 *      org.openedit.store.Cart)
	 */
	public HitTracker searchStore(WebPageRequest inPageRequest, Cart cart) throws Exception
	{
		String query = inPageRequest.getRequestParameter("query");
		String withincatalog = inPageRequest.getRequestParameter("department");
		if (query == null && withincatalog == null)
		{
			return null;
		}
		inPageRequest.removeSessionValue("crumb");

		SearchQuery search = createSearchQuery();
		search.setAndTogether(true);
//		search.putInput("query", query);

		if (query != null)
		{
			if (query.indexOf(":") == -1)
			{
				search.addStartsWith("description", query);
			}
			else
			{
				String[] pair = query.split(":");
				if (pair.length == 2) {
					search.addMatches(pair[0], pair[1]);
				} else {
					for (int ctr = 0; ctr < pair.length; ctr++) {
						if (ctr > 0) {
							String queryValue = pair[ctr];
							search.addMatches(pair[0], queryValue);
						}
					}
				}
			}
		}

		Category department = null;
		if (withincatalog != null && !"all".equals(withincatalog))
		{
			department = getStore().getCategory(withincatalog);
			search.addMatches("category");

		}
		cart.setLastVisitedCatalog(department);
		String filter = createFilter(inPageRequest, department != null);
		search.addQuery("filter", filter);
		
		String ordering = null;
		if (inPageRequest.getCurrentAction() != null)
		{
			Category catalog = getStore().getCategory(withincatalog);
			if (catalog != null)
			{
				ordering = catalog.get("sortfield");
			}
			if (ordering == null || ordering.length() < 1)
			{
				ordering = inPageRequest.getCurrentAction().getChildValue("sortfield");
			}
		}
		String sort = inPageRequest.getRequestParameter("sortby");
		search.setSortBy(sort);
		return cachedSearch(inPageRequest, search);
	}

	public String createFilter(WebPageRequest inPageRequest, boolean selected)
	{
		String filter = null;
		String filteren = inPageRequest.getPage().get("filtersearchenabled"); // Used
		// for
		// series
		// search
	
		return filter;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openedit.store.search.ProductSearcher#updateIndex(org.openedit.store.Product)
	 */
	public void updateIndex(Product inProduct) throws StoreException
	{
		List all = new ArrayList(1);
		all.add(inProduct);
		updateIndex(all, false);
		clearIndex();  //Does not flush because it will flush if needed anyways on a search

	}

//	public Analyzer getAnalyzer()
//	{
//		if (fieldAnalyzer == null)
//		{
////			CompositeAnalyzer composite = new CompositeAnalyzer();
////			composite.setAnalyzer("description", new StemmerAnalyzer());
////			composite.setAnalyzer("id", new NullAnalyzer());
////			composite.setAnalyzer("foldersourcepath", new NullAnalyzer());
////			RecordLookUpAnalyzer record = new RecordLookUpAnalyzer();
////			record.setUseTokens(false);
////			composite.setAnalyzer("cumulusid", record);
////			composite.setAnalyzer("name_sortable", record);
////			fieldAnalyzer = composite;
//			
//			Map analyzermap = new HashMap();
//			analyzermap.put("description",  new EnglishAnalyzer(Version.LUCENE_36));
//			//composite.setAnalyzer("description", new StemmerAnalyzer());
//			
//			analyzermap.put("id", new NullAnalyzer());
//			analyzermap.put("foldersourcepath", new NullAnalyzer());
//			analyzermap.put("sourcepath", new NullAnalyzer());
//			RecordLookUpAnalyzer record = new RecordLookUpAnalyzer();
//			record.setUseTokens(false);
//			analyzermap.put("cumulusid", record);
//			analyzermap.put("name_sortable", record);
//			PerFieldAnalyzerWrapper composite = new PerFieldAnalyzerWrapper( new RecordLookUpAnalyzer() , analyzermap);
//
//			fieldAnalyzer = composite;
//			
//			
//		}
//		return fieldAnalyzer;
//	}


	protected ProductLuceneIndexer getIndexer()
	{
		if (fieldIndexer == null)
		{
			fieldIndexer = new ProductLuceneIndexer();
			fieldIndexer.setAnalyzer(getAnalyzer());
			fieldIndexer.setStore(getStore());
			fieldIndexer.setSearcherManager(getSearcherManager());
			fieldIndexer.setUsesSearchSecurity(doesIndexSecurely());
			fieldIndexer.setNumberUtils(getNumberUtils());
			fieldIndexer.setRootDirectory(getRootDirectory());
		}
		return fieldIndexer;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openedit.store.search.ProductSearcher#updateIndex(java.util.List,
	 *      boolean)
	 */
	public synchronized void updateIndex(List inProducts, boolean inOptimize) throws StoreException
	{
		if( log.isDebugEnabled())
		{
			log.debug("update index");
		}

		try
		{
			PropertyDetails details = getProductArchive().getPropertyDetails();

			for (Iterator iter = inProducts.iterator(); iter.hasNext();)
			{
				Product product = (Product) iter.next();
				Document doc = getIndexer().populateProduct(getIndexWriter(), product, false, details);
				getIndexer().updateFacets(details, doc, getTaxonomyWriter(), getFacetConfig());
				getProductPaths().put( product.getId(), product.getSourcePath()); //This might use up mem. Need to fix
				//getProductPaths().remove( product.getId() );
			}
			

			if (inOptimize)
			{
				flush();
			}
			else if (inProducts.size() > 100)
			{
				flush();
			}
		}
		catch (Exception ex)
		{
			if (ex instanceof StoreException)
			{
				throw (StoreException) ex;
			}
			throw new StoreException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openedit.store.search.ProductSearcher#reIndexAll()
	 */
	public void reIndexAll(IndexWriter inWriter, TaxonomyWriter inTaxonomyWriter) throws OpenEditException
	{
		
		try
		{

			log.info("Listing all products");

			indexAll(inWriter, inTaxonomyWriter);

			
			
		}
		catch (IOException ex)
		{
			throw new StoreException(ex);
		}
		finally
		{
			//fieldRunningReindex = false;
		}
	}

	protected void indexAll(IndexWriter writer,TaxonomyWriter inTaxonomyWriter) throws IOException, OpenEditException
	{
		
		
		try
		{
			ProductLuceneIndexAll reindexer = new ProductLuceneIndexAll();
			reindexer.setWriter(writer);
			reindexer.setRootPath("/WEB-INF/data/" + getCatalogId() + "/products/");
			reindexer.setPageManager(getPageManager());
			reindexer.setProductArchive(getProductArchive());
			reindexer.setIndexer(getIndexer());
			reindexer.setTaxonomyWriter(getTaxonomyWriter());
			reindexer.setFacetConfig(getFacetConfig());
			reindexer.process();
			

			getProductPaths().clear();

			log.info("Reindex started on with " + reindexer.getExecCount() + " products");
			

		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}
		
		// HitCollector
		log.info("Reindex done");
	}
	
	private boolean doesSearchSecurely(WebPageRequest inReq)
	{
		if (inReq.getUser().hasPermission("oe.archive.administration"))
		{
			return false;
		}
		
		return doesIndexSecurely();
	}

	private boolean doesIndexSecurely()
	{
		if (fieldUsesSearchSecurity == null)
		{
			PageSettings settings = getPageManager().getPageSettingsManager().getPageSettings("/" + getCatalogId() + "/products/");
			String val = settings.getPropertyValue("usessearchsecurity", null);
			if (val != null && Boolean.valueOf(val).booleanValue())
			{
				fieldUsesSearchSecurity = Boolean.TRUE;
			}
			else
			{
				fieldUsesSearchSecurity = Boolean.FALSE;
			}
		}
		return fieldUsesSearchSecurity.booleanValue();
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openedit.store.search.ProductSearcher#getProductArchive()
	 */
	public ProductArchive getProductArchive()
	{
		return getStore().getProductArchive();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openedit.store.search.ProductSearcher#getStore()
	 */
	public Store getStore()
	{
		if (fieldStore == null)
		{
			fieldStore = getStoreArchive().getStore(getCatalogId());
		}
		return fieldStore;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openedit.store.search.ProductSearcher#setStore(org.openedit.store.Store)
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openedit.store.search.ProductSearcher#setStore(org.openedit.store.Store)
	 */
	public void setStore(Store inStore)
	{
		fieldStore = inStore;
		setCatalogId(inStore.getCatalogId());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openedit.store.search.ProductSearcher#deleteFromIndex(org.openedit.store.Product)
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openedit.store.search.ProductSearcher#deleteFromIndex(org.openedit.store.Product)
	 */
	public void deleteFromIndex(Product inProduct) throws StoreException
	{
		deleteFromIndex(inProduct.getId());
	}

	public void deleteData(Data inData)
	{
		if(!(inData instanceof Product)){
			inData = getProductArchive().getProductBySourcePath(inData.get("sourcepath"));
		}
		if(inData != null){
			getProductArchive().deleteProduct((Product)inData);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openedit.store.search.ProductSearcher#deleteFromIndex(java.lang.String)
	 */
	public void deleteFromIndex(String inId) throws StoreException
	{
		// TODO Auto-generated method stub
		log.info("delete from index " + inId);

		try
		{
			String id = inId.toLowerCase(); // Since it is tokenized
			Term term = new Term("id", id);

			getIndexWriter().deleteDocuments(term);
			clearIndex();
		}
		catch (IOException ex)
		{
			throw new StoreException(ex);
		}
	}
	public void deleteFromIndex(HitTracker inOld)
	{
		if( inOld.size() == 0)
		{
			return;
		}
		Term[] all = new Term[inOld.getTotal()];
		for (int i = 0; i < all.length; i++)
		{
			Object hit = (Object) inOld.get(i);
			String id = inOld.getValue(hit, "id");
			Term term = new Term("id", id);
			all[i] = term;
		}
		try
		{
			getIndexWriter().deleteDocuments(all);
		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}
		
	}

	
	// // this is actually a date time
	// public SimpleDateFormat getLuceneDateFormat() {
	// if (fieldLuceneDateFormat == null) {
	// fieldLuceneDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
	// }
	// return fieldLuceneDateFormat;
	// }

	// public boolean isAddProductsToParentCatalog() {
	// return fieldAddProductsToParentCatalog;
	// }
	//
	// public void setAddProductsToParentCatalog(boolean
	// inAddProductsToParentCatalog) {
	// fieldAddProductsToParentCatalog = inAddProductsToParentCatalog;
	// }

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openedit.store.search.ProductSearcher#getAllHits()
	 */
//	public HitTracker getAllHits(WebPageRequest inReq)
//	{
//		SearchQuery query = new SearchQuery();
//		query.addMatches("id", "*");
//		if( inReq == null)
//		{
//			return search(query);
//		}
//		else
//		{
//			return cachedSearch(inReq, query);
//		}
//	}

	public void saveData(Data inData, User inUser)
	{
		try
		{
			if (inData instanceof Product)
			{
				Product product = (Product) inData;
				if(product.getId() == null){
					product.setId(getProductArchive().nextProductNumber());
				}
				if(product.getSourcePath() == null){
					product.setSourcePath(product.getId());
				}
				if (product.get("createdon") == null){
					String date = DateStorageUtil.getStorageUtil().formatForStorage(new Date());
					product.setProperty("createdon", date);
				}
				getProductArchive().saveProduct(product);
				updateIndex(product);
			}
			else if (inData instanceof CompositeData)
			{
				saveCompositeData((CompositeData)inData, inUser);
			}
			
		}
		catch (StoreException e)
		{
			throw new OpenEditRuntimeException(e);
		}
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	// public Product getProduct(String inId)
	// {
	// return getProductArchive().getProduct(inId);
	// }

	public String idToPath(String inProductId)
	{
		String path = (String) getProductPaths().get(inProductId);
		if (path == null)
		{
			SearchQuery query = createSearchQuery();
			query.addExact("id", inProductId);
			
			HitTracker hit = search(query);
			if (hit.size() > 0)
			{
				path = (String) ((Data) hit.get(0)).get("sourcepath");
				getProductPaths().put(inProductId, path);
			}
		}
		return path;
	}

	public Map getProductPaths()
	{
		if (fieldProductPaths == null)
		{
			fieldProductPaths = new HashMap();
		}
		return fieldProductPaths;
	}
	
//	public CompositeData searchMultiple(String[] inIds)
//	{
//		CompositeData composite = new CompositeData();
//		for (int i = 0; i < inIds.length; i++)
//		{
//			Data data = (Data)searchById(inIds[i]);
//			composite.addData(data);
//		}
//		return composite;
//	}

	public Object searchById(String inId)
	{
//		if (inId.contains(":"))
//		{
//			return searchMultiple(inId.split(":"));
//		}
		Product p = getStore().getProduct(inId);
		if(p != null){
			return p;
		}
		String path = idToPath(inId);
		if( path == null)
		{
			return null;
		}
		return getProductArchive().getProductBySourcePath(path);
	}

	public StoreArchive getStoreArchive()
	{
		return fieldStoreArchive;
	}

	public void setStoreArchive(StoreArchive inStoreArchive)
	{
		fieldStoreArchive = inStoreArchive;
	}

	
	public Data createNewData() {
	return new Product();
	}
	
	
	@Override
	public void saveAllData(Collection inAll, User inUser) {
		for (Iterator iterator = inAll.iterator(); iterator.hasNext();) {
			Data object = (Data) iterator.next();
			saveData(object, null);
		}
	}
	
	
	
	public HitTracker cachedSearch(WebPageRequest inPageRequest, SearchQuery inSearch) throws OpenEditException
	{
		//modify in query if we are using search security
		addShowOnly(inPageRequest, inSearch);
		if(doesIndexSecurely() && !inSearch.isSecurityAttached())
		{
			//TODO: This should be in a child query with 	child.setFilter(true);
			
			//viewasset = "admin adminstrators guest designers"
			//goal: current query && (viewasset.contains(username) || viewasset.contains(group0) || ... || viewasset.contains(groupN))
			User currentUser = inPageRequest.getUser();
			StringBuffer buffer = new StringBuffer("true "); //true is for wide open searches
			if (currentUser != null)
			{
				UserProfile profile = inPageRequest.getUserProfile();
				if( profile != null)
				{
					//Get the libraries
					Collection libraries = profile.getCombinedLibraries();
					if( libraries != null)
					{
						for (Iterator iterator = libraries.iterator(); iterator	.hasNext();) 
						{
							String library = (String) iterator.next();
							buffer.append( " library_" + library);
						}
					}
					//addSettingsGroupFilters( inPageRequest,inSearch);

//					if(profile.getSettingsGroup() != null)
//					{
//						buffer.append( " sgroup" + profile.getSettingsGroup().getId() );
//					}
//					String value = profile.getValue("assetadmin");
//					if( Boolean.parseBoolean(value) )
//					{
//						buffer.append(" profileassetadmin");
//					}
//					value = profile.getValue("viewassets");
//					if( Boolean.parseBoolean(value) )
//					{
//						buffer.append(" profileviewassets");
//					}
					if(profile.get("distributor") != null){
						buffer.append(" distributor_" + profile.get("distributor"));
					}
				}
//				if(currentUser.getProperty("zone") != null)
//				{
//					buffer.append(" zone" + currentUser.getProperty("zone"));
//				}
				for (Iterator iterator = currentUser.getGroups().iterator(); iterator.hasNext();)
				{
					String allow = ((Group)iterator.next()).getId();
					buffer.append(" group_" + allow);
				}
				buffer.append(" user_" + currentUser.getUserName());
				
			}
			inSearch.addOrsGroup("viewproduct", buffer.toString().toLowerCase());
			inSearch.setSecurityAttached(true);
		}
		//add user profile search filters
		addUserProfileSearchFilters( inPageRequest,inSearch);

		HitTracker hits = super.cachedSearch(inPageRequest, inSearch);

		return hits;
	}
	
	public void addUserProfileSearchFilters(WebPageRequest inReq, SearchQuery search) 
	{
		if( inReq.getUserProfile() == null)
		{
			return;
		}
		String filters = inReq.getUserProfile().get("productsearchfilters");
		if (filters != null && !filters.isEmpty())
		{
			addShowOnlyFilter(inReq,filters,search);
		}
	}
	
	public String nextId() {
	return getProductArchive().nextProductNumber();
}	
	
	
	protected FacetsConfig fieldFacetConfig;

	public FacetsConfig getFacetConfig()
	{
		if (fieldFacetConfig == null)
		{
			fieldFacetConfig =  new FacetsConfig();
			//config.setHierarchical("Publish Date", true);
			//config.setIndexFieldName("author", "facet_author");
			for (Iterator iterator = getPropertyDetails().iterator(); iterator.hasNext();)
			{
				PropertyDetail detail = (PropertyDetail) iterator.next();
				if( detail.isMultiValue() || detail.getId().equals("category"))
				{
					fieldFacetConfig.setMultiValued(detail.getId(), true);
				}
			}

		}
		return fieldFacetConfig;
	}

	public void setFacetConfig(FacetsConfig inFacetConfig)
	{
		fieldFacetConfig = inFacetConfig;
	}
	
}
