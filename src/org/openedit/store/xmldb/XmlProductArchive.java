/*
 * Created on Oct 3, 2004
 */
package org.openedit.store.xmldb;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.ReferenceMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.PropertyDetailsArchive;
import org.openedit.repository.ContentItem;
import org.openedit.repository.OutputStreamItem;
import org.openedit.repository.filesystem.StringItem;
import org.openedit.store.Category;
import org.openedit.store.CategoryArchive;
import org.openedit.store.InventoryItem;
import org.openedit.store.Product;
import org.openedit.store.ProductArchive;
import org.openedit.store.ProductPathFinder;
import org.openedit.store.RelatedFile;
import org.openedit.store.Store;
import org.openedit.store.StoreArchive;
import org.openedit.store.StoreException;

import com.openedit.OpenEditException;
import com.openedit.config.Configuration;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.users.User;
import com.openedit.util.IntCounter;
import com.openedit.util.XmlUtil;

/**
 * A product archive that stores the data about each product in an
 * <tt>.xconf</tt> file in the <tt>storehome/products</tt> directory (as
 * determined by its {@link ProductPathFinder}).
 * 
 * @author cburkey
 */
public class XmlProductArchive extends BaseXmlArchive implements ProductArchive
{
	private static final Log log = LogFactory.getLog(XmlProductArchive.class);

	protected Map fieldProducts;
	protected PageManager fieldPageManager;
	protected Store fieldStore;
	protected XmlUtil fieldXmlUtil;
	protected boolean fieldUpdateExistingRecord = true;
	protected IntCounter fieldIntCounter;
	protected StoreArchive fieldStoreArchive;
	
	public XmlProductArchive()
	{
		log.debug("Created archive");
	}

	protected Map getProducts()
	{
		if (fieldProducts == null)
		{
			// HARD means even if the object goes out of scope we still keep it
			// in the hashmap
			// until the memory runs low then things get dumped randomly
			fieldProducts = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.HARD);
			// fieldProducts = new HashMap();
		}
		return fieldProducts;
	}

	/*
	 * (non-javadoc)
	 * 
	 * @see com.openedit.store.ItemLoader#clearItems()
	 */
	public void clearProducts()
	{
		getPageManager().clearCache();
		// fieldProducts = null;
		getProducts().clear();
	}

	public void deleteProduct(Product inProduct) throws StoreException
	{
		getProducts().remove(inProduct.getSourcePath());
		try
		{
			Page page = getPageManager().getPage(buildItemUrl(inProduct));
			getPageManager().removePage(page);
			getPageManager().getRepository().remove(page.getPageSettings().getXConf());
		}
		catch (Exception ex)
		{
			throw new StoreException(ex);
		}
	}

	public void clearProduct(Product inProduct)
	{
		if (inProduct != null)
		{
			getProducts().remove(inProduct.getSourcePath());
			getPageManager().clearCache(buildItemUrl(inProduct));
			getPageManager().clearCache(buildXconfPath(inProduct));
		}
	}
	public Product getProduct(String inId) throws StoreException
	{
		if (inId == null)
		{
			return null;
		}
		
		String sourcePath = getStore().getProductSourcePathFinder().idToPath(inId);
		if( sourcePath == null)
		{
			//legacy support
			sourcePath = getStore().getProductPathFinder().idToPath(inId);
		}
		return getProductBySourcePath(sourcePath);
	}
	
	public Product getProductBySourcePath(String inSourcePath) throws StoreException
	{
		Product item = (Product) getProducts().get(inSourcePath);
		if (item == null)
		{
			item = new Product();
			item.setCatalogId(getCatalogId());
			item.setSourcePath(inSourcePath);
			String url = buildXconfPath(item);
			log.debug("Loading " + url);
			try
			{
				getProducts().put(inSourcePath, item);
				if (!populateProduct(item, url))
				{
					getProducts().remove(inSourcePath);
					log.debug("No Such product " + url);
					return null;
				}
			}
			catch (OpenEditException e)
			{
				throw new StoreException(e);
			}
		}
		return item;
	}

	protected String buildItemUrl(Product inProduct)
	{
		String catalogId = getStore().getCatalogId();
		//String idToPath = getProductPathFinder().idToPath(inId);
		String sourcePath = inProduct.getSourcePath();
		if( sourcePath == null)
		{
			log.info("This code should never be called. Candidate for deletion.");
			sourcePath = getStore().getProductSourcePathFinder().idToPath(inProduct.getId());
			if( sourcePath == null)
			{
				//legacy support
				sourcePath = getStore().getProductPathFinder().idToPath(inProduct.getId());
			}
			inProduct.setSourcePath(sourcePath);
		}

		String suffix = ".html";
		if (sourcePath.endsWith("/"))
		{
			suffix = "";
		}
		String url = "/WEB-INF/data/" + catalogId + "/products/" + sourcePath + suffix;
		return url;
	}
	
	public String buildXconfPath(Product inProduct)
	{
		if (inProduct == null || inProduct.getSourcePath() == null) 
		{
			return null;
		}
		
		String catalogId = getStore().getCatalogId();
		String sourcePath = inProduct.getSourcePath();
		String suffix = ".xconf";
		if (sourcePath.endsWith("/"))
		//Is a folder
		{
			suffix = "_site.xconf";
		}
		String path = "/WEB-INF/data/" + catalogId + "/products/" + inProduct.getSourcePath() + suffix;
		
		return path;
	}

	protected boolean populateProduct(Product inProduct, String url) throws OpenEditException, StoreException
	{
		// Speed up: Just open the xconf with XmlConfiguration
		Page productPage = getPageManager().getPage(url);
		inProduct.setSourcePage(productPage);
		// if (!productPage.exists())
		// {
		// return false;
		// }
		Configuration config = productPage.getPageSettings().getUserDefinedData();
		if (!productPage.exists() || config == null)
		{
			return false;
		}
		Configuration productConfig = config.getChild("product");
		if (productConfig == null)
		{
			return false;
		}
		String name = productConfig.getAttribute("name");
		inProduct.setName(name);

		String order = productConfig.getAttribute("ordering");
		if (order != null && order.length() > 0)
		{
			inProduct.setOrdering(Integer.parseInt(order));
		}

		inProduct.setAvailable(true);
		String avail = productConfig.getAttribute("available");
		if (avail != null)
		{
			inProduct.setAvailable(Boolean.parseBoolean(avail));
		}
		inProduct.setId(productConfig.getAttribute("id"));
		
		//url is /store/products/myfiles.uploads/test.pdf.html
		
//		String sourcepath = url.substring(1 + getCatalogId().length() + "/products/".length());
//		sourcepath = PathUtilities.extractPagePath(sourcepath); 
//		//sourcepath  /myfiles/uploads/test.pdf
//		
//		inProduct.setSourcePath(sourcepath); //Remove the /store/
		
		loadCatalogs(inProduct, productConfig);
		inProduct.setPriceSupport(createPriceSupport(productConfig));
		List children = productConfig.getChildren("keyword");
		inProduct.clearKeywords();
		if( children == null || children.size() > 0)
		{
			for (Iterator iter = children.iterator(); iter.hasNext();)
			{
				Configuration keyword = (Configuration) iter.next();
				inProduct.addKeyword(keyword.getValue());
			}
		}
		else
		{
			String keyword = productConfig.getChildValue("keywords");
			if( keyword != null)
			{
				inProduct.addKeyword(keyword);
			}
		}
		
		Configuration type = productConfig.getChild("delivery-type");
		if (type != null)
		{
			inProduct.setDeliveryType(type.getAttribute("name"));
		}

		String taxExempt = productConfig.getAttribute("taxExempt");
		if (taxExempt != null)
		{
			inProduct.setTaxExempt(Boolean.valueOf(taxExempt).booleanValue());
		}
		else
		{
			inProduct.setTaxExempt(false);
		}

		loadInventoryItems(inProduct, productConfig);

		for (Iterator iter = productConfig.getChildren("property").iterator(); iter.hasNext();)
		{
			Configuration propConfig = (Configuration) iter.next();
			inProduct.setProperty(propConfig.getAttribute("name"), propConfig.getValue());
		}

		Configuration dep = productConfig.getChild("department");
		if (dep != null)
		{
			String depid = dep.getAttribute("id");
			inProduct.setDepartment(depid);
		}

		Configuration handlingChargeLevel = productConfig.getChild("handling-charge-level");
		if (handlingChargeLevel != null)
		{
			inProduct.setHandlingChargeLevel(handlingChargeLevel.getValue());
		}
		else
		{
			inProduct.setHandlingChargeLevel(null);
		}

		loadShippingMethod(inProduct, productConfig);

		Configuration customPrice = productConfig.getChild("custom-price");
		if (customPrice != null)
		{
			inProduct.setCustomPrice("true".equalsIgnoreCase(customPrice.getValue()));
		}
		else
		{
			inProduct.setCustomPrice(false);
		}

		loadOptions(inProduct, productConfig);

	
		return true;
	}

	protected void loadInventoryItems(Product inProduct, Configuration inProductConfig)
	{
		inProduct.clearItems();
		for (Iterator iter = inProductConfig.getChildren("item").iterator(); iter.hasNext();)
		{
			Configuration itemConfig = (Configuration) iter.next();
			InventoryItem currentItem = createInventoryItem(itemConfig, inProduct);
			inProduct.addInventoryItem(currentItem);
		}
	}

	protected void loadShippingMethod(Product inProduct, Configuration inProductConfig)
	{
		Configuration shipping = inProductConfig.getChild("shipping-method");
		if (shipping != null)
		{
			String id = shipping.getAttribute("id");
			inProduct.setShippingMethodId(id);
		}
	}

	protected void loadOptions(Product inProduct, Configuration inProductConfig)
	{
		inProduct.clearOptions();
		for (Iterator iter = inProductConfig.getChildren("option").iterator(); iter.hasNext();)
		{
			Configuration optionConfig = (Configuration) iter.next();
			inProduct.addOption(createOption(optionConfig));
		}
	}

	protected void loadOptions(InventoryItem inItem, Configuration inProductConfig)
	{
		for (Iterator iter = inProductConfig.getChildren("option").iterator(); iter.hasNext();)
		{
			Configuration optionConfig = (Configuration) iter.next();
			inItem.addOption(createOption(optionConfig));
		}
	}

	
	
	
	
	
	
	public void saveProduct(Product inProduct) throws StoreException
	{
		saveProduct(inProduct, (User) null);
	}
	public void saveProduct(Product inProduct, User inUser) throws StoreException
	{
		try
		{
			// TODO: Speed check this section
			// TODO: Force users to set the sourcePath if it is not set
			log.info(inProduct);
			String url = buildItemUrl(inProduct);
			String path = getPageManager().getPageSettingsManager().toXconfPath(url);
			Document document = null;
			String encoding = "UTF-8";
			if (isUpdateExistingRecord())
			{
				// Page encode = getPageManager().getPage("/" +
				// getStore().getCatalogId() + "/products/index.html");
				// encoding = encode.getCharacterEncoding();
				ContentItem item = getPageManager().getPageSettingsManager().getRepository().get(path);

				document = loadXconfDocument(item, encoding);
				Element productelm = document.getRootElement().element("product");
				// remove old products
				if (productelm != null)
				{
					deleteElements(document.getRootElement(), "product");
				}
			}
			else
			{
				document = DocumentHelper.createDocument();
				document.setRootElement(DocumentHelper.createElement("page"));
			}
			Element rootElement = document.getRootElement();
			saveProduct(inProduct, rootElement);
			// save it to disk
			OutputStreamItem xconf = new OutputStreamItem(path);//, new ElementReader(rootElement,getXmlUtil().getWriter(encoding)), encoding);
			xconf.setMakeVersion(false);
			getPageManager().getPageSettingsManager().saveSetting(xconf); //This sets the output stream
			getXmlUtil().saveXml(rootElement, xconf.getOutputStream(), encoding);

			String id = inProduct.getId();
			if( getProducts().size() > 1000)
			{
				getProducts().clear();
			}
			getProducts().put(inProduct.getSourcePath(), inProduct);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			String msg = "Saving product " + inProduct.getSourcePath() + " failed: ";
			log.error(msg + ex);
			throw new StoreException(msg + ex);
		}
	}

	public void saveProduct(Product inProduct, Element rootElement)
	{
		Element productelm = rootElement.addElement("product");
		saveProductAttributes(inProduct, productelm);

		deleteElements(productelm, "item");
		// save items
		for (Iterator iter = inProduct.getInventoryItems().iterator(); iter.hasNext();)
		{
			InventoryItem inventory = (InventoryItem) iter.next();
			saveItem(productelm, inventory);
		}

		// save out catalogs
		saveProductCatalogs(inProduct, productelm);

		saveProductPricing(inProduct, productelm);

//		if (inProduct.getKeywords() != null && !hasProperty(rootElement, "keywords"))
//		{
//			// copy keywords to page property
//			Element pageprop = rootElement.addElement("property");
//			pageprop.addAttribute("name", "keywords");
//			pageprop.setText(inProduct.getKeywords());
//		}
		if (inProduct.getDeliveryType() != null)
		{
			Element type = productelm.addElement("delivery-type");
			type.addAttribute("name", inProduct.getDeliveryType());
		}
		// clear out any old product properties
		deleteElements(productelm, "property");
		// saves out properties
		PropertyDetails details = getPropertyDetails();
		PropertyDetail moddetail = details.getDetail("recordmodificationdate");
		if(moddetail != null && moddetail.getDateFormat() != null)
		{
			String recordmod = moddetail.getDateFormat().format(new Date());
			inProduct.setProperty("recordmodificationdate", recordmod );
		}
		
		
		for (Iterator iter = inProduct.getProperties().keySet().iterator(); iter.hasNext();)
		{
			String key = (String) iter.next();
			PropertyDetail detail = details.getDetail(key);
			String value = inProduct.getProperty(key);
			
			if( value != null && value.length() > 0)
			{
				value = value.trim();
				Element newProperty = productelm.addElement("property");
				newProperty.addAttribute("name", key);
				//newProperty.addAttribute("id", key);
				
				if( detail != null && detail.isViewType("html"))
				{
					newProperty.addCDATA(value);
				}
				else 
				{
					if( value.contains("  "))
					{
						newProperty.addCDATA(value);						
					}
					else
					{
						newProperty.setText(value);
					}
				}
			}
		}

		saveOptions(inProduct.getOptions(), productelm);

	
	
	}

	

	protected boolean hasProperty(Element inRootElement, String inName)
	{
		for (Iterator iter = inRootElement.elementIterator("property"); iter.hasNext();)
		{
			Element element = (Element) iter.next();
			String name = element.attributeValue("name");
			if (inName.equals(name))
			{
				return true;
			}
		}
		return false;
	}

	private Document loadXconfDocument(ContentItem item, String encode) throws OpenEditException, DocumentException, IOException
	{
		Document document;

		// String url = PRODUCTS_URL_PATH + "/" +
		// getProductPathFinder().idToPath(inId) + ".html";

		if (item.exists())
		{
			document = getXmlUtil().getXml(item.getInputStream(),encode).getDocument();
		}
		else
		{
			document = DocumentHelper.createDocument();
			document.setRootElement(DocumentHelper.createElement("page"));
		}
		return document;
	}

	protected void saveProductAttributes(Product inProduct, Element productelm)
	{
		String name = inProduct.getName();
		// name = SpecialCharacter.escapeSpecialCharacters(name);
		productelm.addAttribute("name", name);
		productelm.addAttribute("id", inProduct.getId());
		if( inProduct.getOrdering() > 0)
		{
			productelm.addAttribute("ordering", String.valueOf(inProduct.getOrdering()));
		}
		saveKeywords(inProduct, productelm);
		// set availability
		if ( !inProduct.isAvailable())
		{
			productelm.addAttribute("available", "false");
		}
	}

	protected void saveProductPricing(Product inProduct, Element productelm)
	{
		if (inProduct.hasProductLevelPricing())
		{
			addPrice(productelm, inProduct.getPriceSupport());
		}

		if (inProduct.isTaxExempt())
		{
			productelm.addAttribute("taxExempt", String.valueOf(inProduct.isTaxExempt()));
		}
		if (inProduct.isCustomPrice())
		{
			Element customPriceElem = productelm.addElement("custom-price");
			customPriceElem.setText(String.valueOf(inProduct.isCustomPrice()));
		}

		// handling charge level
		deleteElements(productelm, "handling-charge-level");
		if (inProduct.getHandlingChargeLevel() != null)
		{
			Element handlingChargeElem = productelm.addElement("handling-charge-level");
			handlingChargeElem.setText(inProduct.getHandlingChargeLevel());
		}
		deleteElements(productelm, "shipping-method");
		if (inProduct.getShippingMethodId() != null)
		{
			// we only support fixed costs methods for products
			Element shipping = productelm.addElement("shipping-method");
			shipping.addAttribute("id", inProduct.getShippingMethodId());
		}

	}

	protected void saveKeywords(Product inProduct, Element inProductElment)
	{
		if (inProduct.getKeywords() != null)
		{
			deleteElements(inProductElment, "keywords"); //legacy
			deleteElements(inProductElment, "keyword");
			for (Iterator iter = inProduct.getKeywords().iterator(); iter.hasNext();)
			{
				String val = (String) iter.next();
				Element keywordElement = inProductElment.addElement("keyword");
				keywordElement.addCDATA(val); // This will dodge invalid character problems
				//keywordElement.setText(val); //This may give us an exception with invalid characters
			}
		}
	}

	/**
	 * @param inProduct
	 * @param productelm
	 */
	private void saveProductCatalogs(Product inProduct, Element productelm)
	{
		// remove old catalogs
		deleteElements(productelm, "category");
		deleteElements(productelm, "catalog"); // TODO: Remove this line in 6.0
		// TODO: Save over the old category
		for (Iterator iter = inProduct.getCategories().iterator(); iter.hasNext();)
		{
			Category catalog = (Category) iter.next();
			Element cat = productelm.addElement("category");
			cat.addAttribute("id", catalog.getId());
			if (catalog.isUserSelected())
			{
				cat.addAttribute("userselected", "true");
			}
//			if (catalog == inProduct.getDefaultCatalog())
//			{
//				cat.addAttribute("default", "true");
//			}
		}
	}

	

	protected void loadCatalogs(Product inProduct, Configuration inProductConfig) throws StoreException
	{
		inProduct.clearCatalogs();
		// TODO: Remove this next chunk of code as duplicate
		for (Iterator iter = inProductConfig.getChildren("catalog").iterator(); iter.hasNext();) 
		{ 
			Configuration catalogconfig = (Configuration) iter.next();
			String catid = catalogconfig.getAttribute("id");
			Category catalog = getCatalogArchive().getCategory(catid);
			if (catalog == null)
			{
				// The reason we do not throw this exception is some sites
				// need more flexability. TODO: Add a boolean for
				// strict=true|false
				// to this object
				// throw new StoreException("No such catalog " + catid);
				log.error("Could not find a catalog named: " + catid);

				continue;
			}
			String defaultCatalog = catalogconfig.getAttribute("default");
			if ("true".equalsIgnoreCase(defaultCatalog))
			{
				inProduct.setDefaultCatalog(catalog);
			}

			inProduct.addCatalog(catalog);
		}
		// TODO: End Deprecation

		for (Iterator iter = inProductConfig.getChildren("category").iterator(); iter.hasNext();)
		{
			Configuration catalogconfig = (Configuration) iter.next();
			String catid = catalogconfig.getAttribute("id");
			Category catalog = getCatalogArchive().getCategory(catid);
			if (catalog == null)
			{
				// The reason we do not throw this exception is some sites
				// need more flexability. TODO: Add a boolean for
				// strict=true|false
				// to this object
				// throw new StoreException("No such catalog " + catid);
				log.error("Could not find a catalog named: " + catid);

				continue;
			}
			
			//Legacy support
			String defaultCatalog = catalogconfig.getAttribute("default");
			if ("true".equalsIgnoreCase(defaultCatalog))
			{
				inProduct.setDefaultCatalog(catalog);
			}
			inProduct.addCatalog(catalog);
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

	protected InventoryItem createInventoryItem(Configuration inItemConfig, Product inProduct)
	{
		InventoryItem currentItem = new InventoryItem();
		currentItem.setProduct(inProduct);
		String inventory = inItemConfig.getAttribute("inventory");
		currentItem.setQuantityInStock(Integer.parseInt(inventory));
		String sku = inItemConfig.getAttribute("sku");
		currentItem.setSku(sku);

		String backordered = inItemConfig.getAttribute("backordered");
		if (backordered != null && Boolean.parseBoolean(backordered))
		{
			currentItem.setBackOrdered(true);
		}

		currentItem.setDescription(inItemConfig.getAttribute("description"));

		// Deprecated. We now use options and properties for everything
		Configuration sizeConfig = inItemConfig.getChild("size");
		if (sizeConfig != null)
		{
			String sizeId = sizeConfig.getAttribute("id");
			if (sizeId != null)
			{
				currentItem.setSize(sizeId);
			}
		}
		Configuration colorConfig = inItemConfig.getChild("color");
		if (colorConfig != null)
		{
			String colorId = colorConfig.getAttribute("id");
			if (colorId != null)
			{
				currentItem.setColor(colorId);
			}
		}
		Configuration weightConfig = inItemConfig.getChild("weight");
		if (weightConfig != null)
		{
			String weight = weightConfig.getAttribute("value");
			if (weight != null && weight.length() > 0)
			{
				currentItem.setWeight(Double.parseDouble(weight));
			}
		}
		
		
		Configuration heightConfig = inItemConfig.getChild("height");
		if (heightConfig != null)
		{
			String height = heightConfig.getAttribute("value");
			if (height != null && height.length() > 0)
			{
				currentItem.setHeight(Double.parseDouble(height));
			}
		}
		
		
		Configuration lengthConfig = inItemConfig.getChild("length");
		if (lengthConfig != null)
		{
			String weight = lengthConfig.getAttribute("value");
			if (weight != null && weight.length() > 0)
			{
				currentItem.setLength(Double.parseDouble(weight));
			}
		}
		
		
		
		Configuration widthConfig = inItemConfig.getChild("width");
		if (widthConfig != null)
		{
			String width = widthConfig.getAttribute("value");
			if (width != null && width.length() > 0)
			{
				currentItem.setWidth(Double.parseDouble(width));
			}
		}
		
		
		
		
		
		
		
		
		currentItem.setPriceSupport(createPriceSupport(inItemConfig));

		// load up the properties
		for (Iterator iter = inItemConfig.getChildren("property").iterator(); iter.hasNext();)
		{
			Configuration propConfig = (Configuration) iter.next();
			currentItem.addProperty(propConfig.getAttribute("name"), propConfig.getValue());
		}
		loadOptions(currentItem, inItemConfig);
		return currentItem;
	}

	protected void saveItem(Element inProductElem, InventoryItem inInventoryItem)
	{
		Element ielement = inProductElem.addElement("item");

		if (inInventoryItem.isBackOrdered())
		{
			ielement.addAttribute("backordered", "true");
		}
		ielement.addAttribute("sku", inInventoryItem.getSku());
		ielement.addAttribute("inventory", String.valueOf(inInventoryItem.getQuantityInStock()));
		if (inInventoryItem.getDescription() != null)
		{
			ielement.addAttribute("description", inInventoryItem.getDescription());
		}
		// ielement.addElement("color").addAttribute("id",
		// inInventoryItem.getColor());
		// String size = inInventoryItem.getSize();
		// ielement.addElement("size").addAttribute("id", size);
		double weight = inInventoryItem.getWeight();
		ielement.addElement("weight").addAttribute("value", String.valueOf(weight));
		
		
		
		
		double height = inInventoryItem.getHeight();
		ielement.addElement("height").addAttribute("value", String.valueOf(height));
		
		double width = inInventoryItem.getWidth();
		ielement.addElement("width").addAttribute("value", String.valueOf(width));
		
		double length = inInventoryItem.getLength();
		ielement.addElement("length").addAttribute("value", String.valueOf(length));
		
		saveOptions(inInventoryItem.getOptions(), ielement);

		// Output something that looks like:
		// <price quantity="1">19.95</price>
		if (inInventoryItem.hasOwnPrice())
		{
			addPrice(ielement, inInventoryItem.getPriceSupport());
		}
		// save properties
		for (Iterator iter = inInventoryItem.getProperties().keySet().iterator(); iter.hasNext();)
		{
			String key = (String) iter.next();
			String value = inInventoryItem.getProperty(key);
			if( value != null)
			{
				Element newProperty = ielement.addElement("property");
				newProperty.addAttribute("name", key);
				newProperty.setText(value);
			}
		}

	}

	protected CategoryArchive getCatalogArchive()
	{
		return getStore().getCategoryArchive();
	}

	/*
	 * (non-javadoc)
	 * 
	 * @see com.openedit.store.ProductArchive#loadDescription(com.openedit.store.Product)
	 */
	public String loadDescription(Product inProduct) throws StoreException
	{
		String url = buildItemUrl(inProduct);
		try
		{
			Page page = getPageManager().getPage(url);
			if (page.exists())
			{
				String content = page.getContent();
				return content;
			}
			return null;
		}
		catch (Exception ex)
		{
			throw new StoreException(ex);
		}
	}

	// gets the next product ID if needed
	public synchronized String nextProductNumber() throws StoreException
	{
			String countString = String.valueOf(getIntCounter().incrementCount());
			return countString;
	}

	/*
	 * (non-javadoc)
	 * 
	 * @see com.openedit.store.ProductArchive#saveProductDescription(com.openedit.store.Product)
	 */
	public void saveProductDescription(Product inProduct, String inDescription) throws StoreException
	{
		if (inDescription == null)
		{
			return;
		}
		String url = buildItemUrl(inProduct);
		try
		{
			Page page = getPageManager().getPage(url);
			StringItem item = new StringItem(page.getPath(), inDescription, page.getCharacterEncoding());
			item.setMakeVersion(false);
			getPageManager().getRepository().put(item);
		}
		catch (Exception ex)
		{
			throw new StoreException(ex);
		}
	}
	//This is optional HTML page
	public void saveBlankProductDescription(Product inProduct) throws StoreException
	{
		try
		{
			String desc = inProduct.getDescription();
			if (desc == null)
			{
				return;
			}
			String url = buildItemUrl(inProduct);
			Page page = getPageManager().getPage(url);
			if (page.exists())
			{
				return;
			}
			StringItem item = new StringItem(page.getPath(), desc, page.getCharacterEncoding());
			item.setMakeVersion(false);
			page.setContentItem(item);
			getPageManager().putPage(page);
		}
		catch (Exception ex)
		{
			throw new StoreException(ex);
		}
	}

	public ProductPathFinder getProductPathFinder()
	{
		return getStore().getProductPathFinder();
	}
	public PropertyDetails getPropertyDetails() 
	{
		PropertyDetailsArchive fieldArchive = getStore().getFieldArchive();
		return fieldArchive.getPropertyDetailsCached("product");
	}

//	public List listAllProductIds()
//	{
//		List all = new ArrayList(500);
//
//		FileFilter filter = new FileFilter()
//		{
//			public boolean accept(File inDir)
//			{
//				String inName = inDir.getName();
//				if (inDir.isDirectory() || inName.endsWith(".xconf"))
//				{
//					if (!inName.endsWith("_site.xconf") && !inName.endsWith("_default.xconf") && !inName.equals(".xconf"))
//					{
//						return true;
//					}
//				}
//				return false;
//			}
//		};
//		File dir = new File(getStoreDirectory(), "products/");
//		if (dir.exists())
//		{
//			findFiles(dir, all, filter);
//		}
//
//		return all;
//	}

	public Store getStore()
	{
		if (fieldStore == null)
		{
			fieldStore = getStoreArchive().getStore(getCatalogId());
		}

		return fieldStore;
	}

	public void setStore(Store inStore)
	{
		fieldStore = inStore;
	}

	public File getStoreDirectory()
	{
		return getStore().getStoreDirectory();
	}

	public XmlUtil getXmlUtil()
	{
		if (fieldXmlUtil == null)
		{
			fieldXmlUtil = new XmlUtil();
		}
		return fieldXmlUtil;
	}
	public void setXmlUtil(XmlUtil inUtil)
	{
		fieldXmlUtil = inUtil;
	}

	public boolean isUpdateExistingRecord()
	{
		return fieldUpdateExistingRecord;
	}

	public void setUpdateExistingRecord(boolean inUpdateExistingRecord)
	{
		fieldUpdateExistingRecord = inUpdateExistingRecord;
	}

	public IntCounter getIntCounter()
	{
		if( fieldIntCounter == null)
		{
			fieldIntCounter = new IntCounter();
			fieldIntCounter.setLabelName("productIdCount");
			File file = new File("/WEB-INF/data/" + getStore().getCatalogId() +  "/products/product.properties");
					
			fieldIntCounter.setCounterFile(file);
		}
		return fieldIntCounter;
	}

	public void setIntCounter(IntCounter inIntCounter)
	{
		fieldIntCounter = inIntCounter;
	}

	public StoreArchive getStoreArchive()
	{
		return fieldStoreArchive;
	}

	public void setStoreArchive(StoreArchive inStoreArchive)
	{
		fieldStoreArchive = inStoreArchive;
	}

}
