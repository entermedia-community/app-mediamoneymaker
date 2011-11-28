 /*
 * Created on Nov 16, 2004
 */
package org.openedit.store.edit;

import java.awt.Dimension;
import java.io.File;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.openedit.data.PropertyDetails;
import org.openedit.data.PropertyDetailsArchive;
import org.openedit.entermedia.creator.ConvertInstructions;
import org.openedit.money.Money;
import org.openedit.repository.RepositoryException;
import org.openedit.repository.filesystem.StringItem;
import org.openedit.store.Category;
import org.openedit.store.Image;
import org.openedit.store.InventoryItem;
import org.openedit.store.Option;
import org.openedit.store.Product;
import org.openedit.store.Store;
import org.openedit.store.StoreException;

import com.openedit.OpenEditException;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.util.XmlUtil;

/**
 * @author cburkey
 *
 */
public class StoreEditor
{
	protected Category fieldCurrentCatalog;
	protected Category fieldPickedCatalog;
	protected Store fieldStore;
	protected Product fieldCurrentProduct;
	protected InventoryItem fieldCurrentItem;
	protected List fieldAllOptions;
	protected PageManager fieldPageManager;
	protected List fieldImageTypes;
	
	private static final Log log = LogFactory.getLog(StoreEditor.class);
	public StoreEditor()
	{
	}

	public Category getCurrentCategory() throws StoreException
	{
		return fieldCurrentCatalog;
	}

	public void setCurrentCategory(Category inCurrentCatalog)
	{
		if ( inCurrentCatalog == null)
		{
			throw new IllegalArgumentException("Category cannot be null");
		}
		fieldCurrentCatalog = inCurrentCatalog;
		setCurrentProduct(null);
		setCurrentItem(null);
		
	}
	/**
	 * @deprecated Use getCurrentCategory()
	 */
	public Category getCurrentCatalog() throws StoreException
	{
		return getCurrentCategory();
	}
	/**
	 * @deprecated Use setCurrentCategory(Category)
	 */
	public void setCurrentCatalog(Category inCurrentCatalog)
	{
		setCurrentCategory(inCurrentCatalog);
		
	}
	public Store getStore()
	{
		return fieldStore;
	}

	public void setStore(Store inStore)
	{
		fieldStore = inStore;
	}

	/**
	 * @param inString
	 * @return
	 */
	public Category getCatalog(String inString) throws StoreException
	{
		Category catalog = getStore().getCategoryArchive().getCategory(inString);
		
		return catalog;
	}
	
	public void moveCatalogUp(Category inCatalog) throws StoreException
	{
		Category parent = inCatalog.getParentCatalog();
		List children = (List)parent.getChildren();
		Category prev = null;
		for (Iterator iter = children.iterator(); iter.hasNext();) 
		{
			Category child = (Category) iter.next();
			if (child == inCatalog)
			{
				if (prev != null)
				{
					int childIndex = children.indexOf(child);
					children.set(childIndex - 1, child);
					children.set(childIndex, prev);
				}
				break;
			}
			prev = child;
		}
		parent.setChildren(children);
		saveCatalog(parent);
	}
	public void moveCatalogDown(Category inCatalog) throws StoreException
	{
		Category parent = inCatalog.getParentCatalog();
		List children = (List)parent.getChildren();
		for (Iterator iter = children.iterator(); iter.hasNext();) 
		{
			Category child = (Category) iter.next();
			if (child == inCatalog)
			{
				if (iter.hasNext())
				{
					int childIndex = children.indexOf(child);
					children.set(childIndex, (Category) iter.next());
					children.set(childIndex + 1, child);
				}
				break;
			}
		}
		parent.setChildren(children);
		saveCatalog(parent);
	}
	public void moveCatalogBefore (Category inCatalog, Category inBeforeCatalog) throws StoreException
	{
		Category parent = inCatalog.getParentCatalog();
		
		if (inBeforeCatalog == null || inBeforeCatalog.getParentCatalog() != parent || inCatalog == inBeforeCatalog)
			return;
		
		List list = parent.getChildren();
		int toIndex = list.indexOf(inBeforeCatalog);
		if (list.indexOf(inCatalog) < toIndex)
		{
			while (list.indexOf(inCatalog) < toIndex)
				moveCatalogDown(inCatalog);
		} 
		else
		{
			while (list.indexOf(inCatalog) > toIndex)
				moveCatalogUp(inCatalog);
		}
		parent.setChildren(list);
	}
	
	public void sortCatalog (Category inCatalog)
	{
		if (inCatalog == null)
			return;
		List children;
		children = inCatalog.getChildren();
		
		Collections.sort(children, new Comparator() 
		{
			public int compare(Object o1, Object o2) {
				Category c1 = (Category)o1, c2 = (Category)o2;
				return c1.getName().compareTo(c2.getName());
			}
		});
		inCatalog.setChildren(children);
	}

	/**
	 * @param inString
	 * @param inString2
	 * @return
	 */
	public Category addNewCatalog(String inId, String inName) throws StoreException
	{
		Category newCat = new Category();
		newCat.setId(inId);
		newCat.setName(inName);
		if (getCurrentCatalog() != null)
		{
			getCurrentCatalog().addChild(newCat);
		}
		else if (getRootCatalog() != null)
		{
			getRootCatalog().addChild(newCat);
		}
		else
		{
			getStore().getCategoryArchive().setRootCategory(newCat);
		}
		getStore().getCategoryArchive().cacheCategory(newCat);
		return newCat;
		//return getStore().getCatalogArchive().addCatalog(inString, inString2);
	}

	/**
	 * @param inBlank
	 */
	public void saveCatalog(Category inBlank) throws StoreException
	{
		if ( inBlank.getParentCatalog() == null && getStore().getCategoryArchive().getRootCategory() != inBlank)
		{
			getStore().getCategoryArchive().getRootCategory().addChild(inBlank);
			getStore().getCategoryArchive().cacheCategory(inBlank);
		}
		try
		{
			Page desc = getPageManager().getPage(getStore().getStoreHome() + "/categories/" + inBlank.getId() + ".html");
			if ( !desc.exists() )
			{
				StringItem item = new StringItem(desc.getPath(), " ",desc.getCharacterEncoding() );
				desc.setContentItem(item);
				getPageManager().putPage(desc);
			}
		}
		catch ( Exception ex )
		{
			throw new StoreException(ex);
		}
		getStore().getCategoryArchive().saveCategory(inBlank);		
	}

	/**
	 * @param inCatalog
	 */
	public void deleteCatalog(Category inCatalog) throws OpenEditException
	{	
		List products = getStore().getProductsInCatalog(inCatalog);
		for (Iterator iter = products.iterator(); iter.hasNext();) 
		{
			Product element = (Product) iter.next();
			element.removeCatalog(inCatalog);
		}
		getStore().getCategoryArchive().deleteCategory(inCatalog);
		//getStore().reindexAll();
		getStore().saveProducts(products);
	}

	/**
	 * @return
	 */
	public Category getRootCatalog() throws StoreException
	{
		return getStore().getCategoryArchive().getRootCategory();
	}

	/**
	 * 
	 */
	public void clearCatalogs() throws StoreException
	{
		getStore().getCategoryArchive().clearCategories();
	}

	public void reloadCatalogs() throws StoreException
	{
		if (getCurrentCatalog() != null)
		{
			String id = getCurrentCatalog().getId();
			getStore().getCategoryArchive().reloadCategories();
			Category catalog = getCatalog(id);
			if ( catalog == null)
			{
				catalog = getRootCatalog();
			}
			setCurrentCatalog(catalog);
		}
		else
		{
			getStore().getCategoryArchive().reloadCategories();
		}
	}

	public Product createProduct()
	{
		return new Product();
	}

	public void addToCatalog(Product inProduct, Category inCatalog) throws StoreException
	{
		inProduct.addCatalog(inCatalog);
		if ( inProduct.getCategories().size() == 1)
		{
			inProduct.setDefaultCatalog(inCatalog);
		}
	}

	public void deleteProduct(Product inProduct) throws StoreException
	{
		getStore().getProductArchive().deleteProduct(inProduct);
		getStore().getProductSearch().deleteFromIndex(inProduct);

		if (getCurrentProduct() != null && inProduct.getId().equals(getCurrentProduct().getId()))
		{
			setCurrentProduct(null);
		}

	}

	public Product getProduct(String inProductId) throws StoreException
	{
		Product prod = getStore().getProduct(inProductId);
		if ( prod == null)
		{
			return null;
		}
		//load up the properties
//		List list = getStore().getProductArchive().getPropertyDetails().getDetails(); 
//		for (Iterator iter = list.iterator(); iter.hasNext();)
//		{
//			Detail detail = (Detail) iter.next();
//			if( prod.getProperty(detail.getId()) == null )
//			{
//				prod.getProperties().put(detail.getId(),""); //avoid null check
//			}
//		}
		return prod;

	}

	public void deleteItem(InventoryItem inItem) throws StoreException
	{
		Product product = inItem.getProduct();
		if (product != null)
		{
			inItem.setProduct(null);
			product.getInventoryItems().remove(inItem);
			saveProduct(product);
			//getStore().reindexAll();
		}
	}

	public void saveProduct(Product inProduct) throws StoreException
	{
		getStore().saveProduct(inProduct);
	}
	public void saveProducts(List inProducts) throws StoreException
	{
		getStore().saveProducts(inProducts);
	}

	public Product getCurrentProduct()
	{
		return fieldCurrentProduct;
	}

	public void setCurrentProduct(Product inCurrentProduct)
	{
		fieldCurrentProduct = inCurrentProduct;
		setCurrentItem(null);
	}

	/**
	 * @return
	 */
	public Product createProductWithDefaults() throws StoreException
	{
		Product product = createProduct();
		String id = getStore().getProductArchive().nextProductNumber();
		product.setId(id); //TODO: Load from serial ID
	//	product.setDescription("No description available");
		Category cat = getStore().getCategory("newproducts");
		if (cat != null)
		{
			product.addCategory(cat);
		}
		return product;

	}

	public Product createProductWithDefaults(Product inTemplateProduct, String newId) throws StoreException
	{
		
		Product product = createProduct();
		String id = getStore().getProductArchive().nextProductNumber();
		product.setId(id); //TODO: Load from serial ID
		product.setName(inTemplateProduct.getName());
		//product.setDescription(inTemplateProduct.);
		return product;

	}

	
	
	public InventoryItem createItem()
	{
		InventoryItem item = new InventoryItem();
		item.setSku("newsku");
		return item;
	}

	public InventoryItem getCurrentItem()
	{
		return fieldCurrentItem;
	}

	public void setCurrentItem(InventoryItem inCurrentItem)
	{
		fieldCurrentItem = inCurrentItem;
	}

	public Money getItemPrice(int inQuantity, InventoryItem inItem)
	{
		return inItem.getPriceSupport().getYourPriceByQuantity(inQuantity);
	}

	public void deleteImage(Image inImage)
	{
		File out = new File(getStore().getRootDirectory(), inImage.getPath());
		out.delete();
		
	}
	/**
	 * @param inType
	 * @return
	 */
	public Image getImage(String inId) throws StoreException
	{
		for (Iterator iter = getImageList().iterator(); iter.hasNext();)
		{
			Image image = (Image) iter.next();
			if (inId.equals(image.getId()) )
			{
				return image;
			}
		}
		return null;
	}

	
	/**
	 * Returns a list of possible images (not actual images).
	 * 
	 * @return  A {@link List} of {@link Image}s
	 */
	public List getImageList() throws StoreException
	{
		return getStore().getImageList();
	}
	
	public List getImageList(String type) throws StoreException
	{
		return getStore().getImageList(type);
	}
	
	/**
	 * @param inCurrentCatalog
	 * @param inLinks
	 */
	/*
	public void addCatalogAsLink(Category inCurrentCatalog, LinkTree inLinks, Link inParentLink, boolean withProducts, String[] productIds) throws OpenEditException
	{
		String parentId= null;
		if ( inParentLink != null )
		{
			parentId = inParentLink.getId();
		}
		String id = inCurrentCatalog.getId();
		id = inLinks.checkUnique(id);
		Link link = new Link();
		link.setId(id);
		link.setUrl( getStore().getStoreHome() + "/catalogs/"  + inCurrentCatalog.getId() + ".html");
		link.setText(inCurrentCatalog.getName());
		link.setUserData(inCurrentCatalog.getId());
		
		inLinks.addLink(parentId,link);

		for (Iterator iter = inCurrentCatalog.getChildren().iterator(); iter.hasNext();)
		{
			Category child = (Category) iter.next();
			addCatalogAsLink(child,inLinks, link,withProducts, productIds);
		}
		//add the products
		if ( withProducts )
		{
			if ( productIds == null)
			{								
				String query = "catalogs:( " + inCurrentCatalog.getId() + ")";

				HitTracker hits = getStore().getStoreSearcher().search( query, "ordering" );
				for (int i = 0; i < hits.getTotal(); i++)
				{
					try
					{
						Document doc = hits.get(i);
						Link plink = new Link();
						String pid = doc.get("id");
						plink.setId(id + "-" + pid);
						
						plink.setUrl(getStore().getStoreHome() + "/products/" +  pid + ".html"); //TODO: Use link finder
						plink.setText(doc.get("name"));
						plink.setUserData(pid);
						inLinks.addLink(link.getId(),plink);
					} catch ( IOException ex)
					{
						throw new OpenEditException(ex);
					}
				}
			}				
			else
			{
				for (int i = 0; i < productIds.length; i++)
				{
					Link plink = new Link();
					String pid = productIds[i];
					plink.setId(inParentLink.getId() + "-" + pid);
					Product prod = getStore().getProduct(pid);
					plink.setUrl(getStore().getStoreHome() +"/products/" +  pid + ".html"); //TODO: Use link finder
					plink.setText(prod.getName());
					plink.setUserData(pid);
					inLinks.addLink(inParentLink.getId(),plink);
				}
			}
		}		

	}
	*/
	
	
	public List getAllOptions() throws StoreException
	{
		//Replace with the PageManager lookup
		List all = new ArrayList();
		File file = new File( getStore().getRootDirectory(),getStore().getStoreHome() + "/data/alloptions.xml");
		if ( file.exists() )
		{
			Element root = null;
			try
			{
				root = new XmlUtil().getXml(file,"UTF-8");
			}
			catch ( Exception ex)
			{
				throw new StoreException(ex); 
			}
			for (Iterator iter = root.elementIterator("option"); iter.hasNext();)
			{
				Element element = (Element) iter.next();
				Option op = new Option();
				op.setId(element.attributeValue("id"));
				op.setName(element.attributeValue("name"));
				all.add( op);
			}
		}
		return all;
		
	}

	/**
	 * @param inOption
	 * @throws OpenEditException
	 */
	public void addOptionToAll(Option inOption) throws OpenEditException
	{
		
		List all = getAllOptions();
		for (Iterator iter = all.iterator(); iter.hasNext();)
		{
			Option option = (Option) iter.next();
			if ( inOption.getId().equals(option.getId()))
			{
				return;
			}
		}
		
		Page settings = getPageManager().getPage(getStore().getStoreHome() +"/data/alloptions.xml");
		
		Element root = null;
		if (settings.exists())
		{
			try
			{
				Reader read = settings.getReader();
				root = new SAXReader().read(read).getRootElement();
				//root = new XmlUtil().getXml(new FileReader(file));
			}
			catch ( Exception ex)
			{
				throw new StoreException(ex); 
			}
		}
		else
		{
			root = DocumentHelper.createElement("optionlist");
		}
		Element opt = root.addElement("option");
		opt.addAttribute("id",inOption.getId());
		opt.addAttribute("name",inOption.getName());
		//new XmlUtil().saveXml(root.getDocument(),file);
		StringItem item = new StringItem(settings.getPath(),root.asXML(),"UTF-8"); 
		settings.setContentItem(item);
		getPageManager().putPage(settings);
	}
	
	public void removeOptionFromAll(String id) throws StoreException
	{
		
		File file = new File( getStore().getStoreDirectory(), "/data/alloptions.xml");
		Element root = null;
		try
		{
			root = new XmlUtil().getXml(file,"UTF-8");
		}
		catch ( Exception ex)
		{
			throw new StoreException(ex); 
		}
		Node node = root.selectSingleNode("//option[@id='" + id +"']");
		if (node != null)
		{
			root.remove(node);
			new XmlUtil().saveXml(root.getDocument(),file);
		}
	}
	
	//this method takes the products in the current catalog, adds affixes, then saves them in the new catalog
	public void addProductAffixes( Category newCatalog, String newPrefix, String newSuffix ) throws OpenEditException
	{
		Category oldCatalog = getCurrentCatalog();
		List products = getStore().getProductsInCatalog(oldCatalog);  //this is the list of the products in the old catalog 
		String newId;
		List productsToSave = new ArrayList();  //this is the list of products that we will save at the end
		if (products != null)
		{
			for (Iterator iter = products.iterator(); iter.hasNext();) 
			{
				Product element = (Product) iter.next(); //element is an existing product
				if (newPrefix.length() > 0 || newSuffix.length() > 0)
				{
					newId = element.getId();
					newId = newPrefix + newId + newSuffix;
					Product product = copyProduct(element, newId ); //product is a new product that is a copy of an existing product
					product.addCatalog(newCatalog); //add the new product (with the new id) to the new catalog
					productsToSave.add(product); //add the new product to the list of products to save
				}
				else  //we don't have to add affixes, so update the existing product instead
				{
					element.addCatalog(newCatalog); //add the existing product to the new catalog
					productsToSave.add(element); //add the existing product to the list of products to save
				}
			}
			getStore().saveProducts(productsToSave); //save all the products that need to be
		}
	}
	

	/**
	 * @return Returns the pageManager.
	 */
	public PageManager getPageManager() 
	{
		return fieldPageManager;
	}
	/**
	 * @param inPageManager The pageManager to set.
	 */
	public void setPageManager(PageManager inPageManager) 
	{
		fieldPageManager = inPageManager;
	}

	
	public Product copyProduct(Product inProduct, String inId) 
	{
		Product product = null;
		if (inProduct != null)
		{
			product = new Product();
			product.setId(inId);
			product.setCatalogId(inProduct.getCatalogId());
			product.setName(inProduct.getName());
			product.setDescription(inProduct.getDescription());
			product.setHandlingChargeLevel(inProduct.getHandlingChargeLevel());
			product.setOptions(inProduct.getOptions());
			product.setOrdering(inProduct.getOrdering());
			product.setDepartment(inProduct.getDepartment());
			product.setPriceSupport(inProduct.getPriceSupport());
			product.setKeywords(inProduct.getKeywords());
			product.setProperties(new HashMap(inProduct.getProperties()));
			product.setInventoryItems(inProduct.getInventoryItems());
		}
		return product;
	}	
	
	public Product copyProduct(Product inProduct, String inId, String inSourcePath) 
	{
		Product product = copyProduct(inProduct, inId);
		product.setSourcePath(inSourcePath);
		if( inSourcePath.endsWith("/"))
		{
			Page sourcePage = getPageManager().getPage(getStore().getStoreHome() + "/products/" + inSourcePath + "/_site.xconf");
			product.setSourcePage(sourcePage);
		}
		else
		{
			Page sourcePage = getPageManager().getPage(getStore().getStoreHome() + "/products/" + inSourcePath + ".xconf");
			product.setSourcePage(sourcePage);
		}
		return product;
	}

	/**
	 * @param inCatalog
	 * @param inId
	 * @throws Exception
	 */
	public void changeCatalogId(Category inCatalog, String inId) throws OpenEditException 
	{
		inId = inId.replace('(', '-');
		inId = inId.replace(')', '-');
		inId = inId.replace(' ', '-');
		
		List products = getStore().getProductsInCatalog(inCatalog);
		
		PageManager pageManager = getPageManager();
//		reload = true;
		Page oldPage = pageManager.getPage(getStore().getStoreHome() +"/catalogs/" + inCatalog.getId() + ".html");
		Page newPage = pageManager.getPage(getStore().getStoreHome() +"/catalogs/" + inId + ".html");
		if (oldPage.exists() && !newPage.exists())
		{
			try
			{
				pageManager.movePage(oldPage, newPage);
			}
			catch ( RepositoryException re )
			{
				throw new OpenEditException( re );
			}
		}
		if (products != null)
		{
			for (Iterator iter = products.iterator(); iter.hasNext();) 
			{
				Product element = (Product) iter.next(); //element is an existing product
				element.removeCatalog(inCatalog); //add the new product (with the new id) to the new catalog
			}
		}
		
		inCatalog.setId( inId );
		saveCatalog( inCatalog );
				
		if (products != null)
		{
			for (Iterator iter = products.iterator(); iter.hasNext();) 
			{
				Product element = (Product) iter.next(); //element is an existing product
				element.addCatalog(inCatalog); //add the new product (with the new id) to the new catalog
			}
			getStore().saveProducts(products); //save all the products that need to be
		}
				
	}
	public List getItemProperties() throws OpenEditException
	{
		PropertyDetailsArchive archive = getStore().getSearcherManager().getPropertyDetailsArchive(getStore().getCatalogId());
		PropertyDetails props = archive.getPropertyDetails("item");
		return props.getDetails();
	}

	public Category getPickedCatalog()
	{
		return fieldPickedCatalog;
	}

	public void setPickedCatalog(Category inCatalog)
	{
		fieldPickedCatalog = inCatalog;
	}

	public void removeProductFromCatalog(Category catalog, String[] productIds) throws OpenEditException
	{
		if ( catalog == null )
		{
			throw new OpenEditException("No catalog found ");
		}
		List productsToSave = new ArrayList();
		
		for (int i = 0; i < productIds.length; i++) 
		{
			Product product = getProduct( productIds[i] );
			if ( product != null )
			{
				product.removeCatalog(catalog);
				productsToSave.add(product);
			}	
		}
		saveProducts(productsToSave);

	}
	
	public void addProductsToCatalog(String[] productid, String prefix, String suffix, Category catalog) throws OpenEditException
	{
		List productsToSave = new ArrayList();
		
		if (catalog != null)
		{
			List products = null;
			if ( productid == null)
			{
				//copy all of them
				products = getStore().getProductsInCatalog(getCurrentCatalog());
			}
			else
			{
				products = new ArrayList();
				for (int i = 0; i < productid.length; i++)
				{
					Product element = getProduct(productid[i]);
					products.add(element);
				}
			}
			for (Iterator iter = products.iterator(); iter.hasNext();)
			{
				Product element = (Product) iter.next();
				Product product = element;
				if (prefix != null || suffix != null)
				{
					if ( prefix == null) prefix = "";
					if ( suffix == null) suffix = "";
					
					product = copyProduct(element, prefix + element.getId() + suffix); 
				}
				if (product != null)
				{
					product.addCatalog(catalog);
					productsToSave.add(product);
				}
			}
			saveProducts(productsToSave);
		}
	}
	public void moveProductsToCatalog(String[] inProductid, Category inCatalog1, Category inCatalog2) throws StoreException
	{
		List productsToSave = new ArrayList();
		
		if (inCatalog1 != null && inCatalog2 != null && inCatalog1 != inCatalog2)
		{
			for (int i = 0; i < inProductid.length; i++) 
			{
				Product product = getProduct(inProductid[i]); 
				if (product != null)
				{
					product.removeCatalog(inCatalog1);
					product.addCatalog(inCatalog2);
					productsToSave.add(product);
				}
			}
			saveProducts(productsToSave);
		}


	}
	
}