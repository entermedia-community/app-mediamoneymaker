/*
 * Created on Oct 3, 2004
 */
package org.openedit.store.xmldb;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.openedit.repository.filesystem.StringItem;
import org.openedit.store.Category;
import org.openedit.store.CategoryArchive;
import org.openedit.store.StoreException;

import com.openedit.OpenEditException;
import com.openedit.OpenEditRuntimeException;
import com.openedit.config.Configuration;
import com.openedit.config.XMLConfiguration;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.util.PathUtilities;
import com.openedit.util.XmlUtil;

/**
 * @author cburkey
 * 
 */
public class XmlCategoryArchive extends BaseXmlArchive implements CategoryArchive
{
	private static final Log log = LogFactory.getLog(XmlCategoryArchive.class);
	protected Map fieldCatalogMap;
	protected Category fieldRootCatalog;
	protected PageManager fieldPageManager;
	protected XmlUtil fieldXmlUtil;
	
	public List listAllCategories() throws StoreException
	{
		List all = new ArrayList();
		addCategories(all, getRootCategory());
		return all;
	}

	/**
	 * @param inAll
	 * @param inRootCatalog
	 */
	private void addCategories(List inAll, Category inRootCatalog)
	{
		// TODO Auto-generated method stub
		inAll.add(inRootCatalog);
		for (Iterator iter = inRootCatalog.getChildren().iterator(); iter.hasNext();)
		{
			Category child = (Category) iter.next();
			addCategories(inAll, child);
		}
	}

	public Map getCatalogMap() 
	{
		if (fieldCatalogMap == null)
		{
			fieldCatalogMap = new HashMap();
		}
		return fieldCatalogMap;
	}

	/**
	 * @deprecated
	 */
	public Category getCatalog(String inCategory)
	{
		return getCategory(inCategory);
	}

	/*
	 * (non-javadoc)
	 * 
	 * @see com.openedit.store.CatalogReader#getCatalog(java.lang.String)
	 */
	public Category getCategory(String inCategory)
	{
		try
		{
			getRootCategory();
			return (Category) getCatalogMap().get(inCategory);
		}
		catch (Exception ex)
		{
			throw new OpenEditRuntimeException(ex);
		}
	}

	public Category getCategoryByName(String inCategoryName) throws StoreException
	{
		List catalogs = listAllCategories();
		for (Iterator iter = catalogs.iterator(); iter.hasNext();)
		{
			Category catalog = (Category) iter.next();
			if (catalog.getName().equals(inCategoryName))
			{
				return catalog;
			}
		}
		return null;
	}
	/**
	 * @deprecated
	 */
	public Category getRootCatalog() throws StoreException
	{
		return getRootCategory();
	}

	public Category getRootCategory() throws StoreException
	{
		if (fieldRootCatalog == null)
		{
			reloadCategories();
		}
		return fieldRootCatalog;
	}

//	/**
//	 * @param inCategoryId
//	 * @param inCategorydesc
//	 * @return
//	 */
//	public Category addCategory(String inCategoryId, String inCategorydesc) throws StoreException
//	{
//		Category oldCatalog = getCategory(inCategoryId);
//		if (oldCatalog == null)
//		{
//			oldCatalog = new Category();
//			oldCatalog.setId(inCategoryId);
//			oldCatalog.setName(inCategorydesc);
//			addChild(oldCatalog);
//			try
//			{
//				Page des = getPageManager().getPage("/" + getCatalogId() + "/categories/" + inCategoryId + ".html");
//				if (!des.exists())
//				{
//					StringItem item = new StringItem(des.getPath(), " ", des.getCharacterEncoding());
//					item.setMakeVersion(false);
//					des.setContentItem(item);
//					getPageManager().putPage(des);
//				}
//			}
//			catch (Exception ex)
//			{
//				throw new StoreException(ex);
//			}
//		}
//		else
//		{
//			oldCatalog.setName(inCategorydesc);
//		}
//		return oldCatalog;
//	}

//	public Category addCategory(Category inCategory) throws StoreException
//	{
//		if (inCategory == null)
//		{
//			return null;
//		}
//		boolean wasInAlready = getCatalogMap().containsKey(inCategory.getId());
//
//		getCatalogMap().put(inCategory.getId(), inCategory);
//		if (!wasInAlready && inCategory.hasChildren()) // we dont want an
//														// infinite loop so we
//														// check to see if it
//														// was in already
//		{
//			for (Iterator iter = inCategory.getChildren().iterator(); iter.hasNext();)
//			{
//				Category child = (Category) iter.next();
//				// log.info("adding " + child);
//				addCategory(child);
//			}
//		}
//		return inCategory;
//	}

	public void deleteCategory(Category inCategory) throws StoreException
	{
		getCatalogMap().remove(inCategory.getId());
		if (getRootCategory().getId().equals(inCategory.getId()))
		{
			setRootCategory(new Category("index", "Index"));
		}
		else
		{
			deleteAll(inCategory);
			if (inCategory.getParentCatalog() != null)
			{
				inCategory.getParentCatalog().removeChild(inCategory);
			}
		}
		saveAll();
	}

	protected void deleteAll(Category inCategory) throws StoreException
	{
		for (Iterator iter = inCategory.getChildren().iterator(); iter.hasNext();)
		{
			Category child = (Category) iter.next();
			child.setParentCatalog(null); // to prevent
											// ConcurrentModificationException
			deleteAll(child);
		}
	}

	public void clearCategories()
	{
		fieldCatalogMap = null;
		fieldRootCatalog = null;
	}

	public void setRootCategory(Category inRootCatalog) throws StoreException
	{
		fieldRootCatalog = inRootCatalog;
		
		//This is not used much anymore
		if (fieldRootCatalog != null)
		{
			String home = fieldRootCatalog.getProperty("categoryhome");
			if (home == null)
			{
				fieldRootCatalog.setProperty("categoryhome", "/" + getCatalogId() + "/categories/");
			}
			cacheCategory(fieldRootCatalog);
		}

	}

	public String listCatalogXml()
	{
		return "/" + getCatalogId() +  "/categories/productcategories.xml";
	}

	/*
	 * (non-javadoc)
	 * 
	 * @see com.openedit.store.CatalogArchive#saveCatalogs()
	 */
	public void saveAll() throws StoreException
	{
		try
		{
			Page catalogFile = getPageManager().getPage(listCatalogXml());

			Element root = createElement(getRootCategory());
			// lets write to a stream
			OutputStream out = getPageManager().saveToStream(catalogFile);
			getXmlUtil().saveXml(root, out, catalogFile.getCharacterEncoding());
		}
		catch (Exception e)
		{
			throw new StoreException(e);
		}

	}

	/**
	 * @param inRootCatalog
	 * @return
	 */
	protected Element createElement(Category inRootCatalog) throws OpenEditException
	{
		Element child = DocumentHelper.createElement("catalog");
		child.addAttribute("id", inRootCatalog.getId());
		child.addAttribute("name", inRootCatalog.getName());
		if (inRootCatalog.getShortDescription() != null)
		{
			child.addElement("shortdescription").setText(inRootCatalog.getShortDescription());
		}
		if (inRootCatalog.getDescription() != null)
		{
			saveLongDescription(inRootCatalog);
		}
		// add any other attributes we might have
		for (Iterator iter = inRootCatalog.getProperties().keySet().iterator(); iter.hasNext();)
		{
			String id = (String) iter.next();
			if (id != null && !"id".equals(id) && !"name".equals(id))
			{
				Element prop = child.addElement("property");
				prop.addAttribute("id", id);
				prop.setText(inRootCatalog.getProperty(id));
			}
		}
		saveOptions(inRootCatalog.getOptions(), child);
		saveRelatedCategories(inRootCatalog, child);

		for (Iterator iter = inRootCatalog.getChildren().iterator(); iter.hasNext();)
		{
			Category subcatalog = (Category) iter.next();
			Element newchild = createElement(subcatalog);
			child.add(newchild);
		}

		return child;
	}

	protected void saveLongDescription(Category inCategory) throws StoreException
	{
		try
		{
			Page fulldesc = getPageManager().getPage("/" + getCatalogId() + "/categories/" + inCategory.getId() + ".html");
			if (!fulldesc.exists())
			{
				String desc = inCategory.getDescription();
				if (desc == null || desc.trim().length() == 0)
				{
					return;
				}
				StringItem item = new StringItem(fulldesc.getPath(), desc, "UTF-8");
				fulldesc.setContentItem(item);
				getPageManager().putPage(fulldesc);
			}
		}
		catch (OpenEditException oee)
		{
			throw new StoreException(oee);
		}
	}


	public void reloadCategories() throws StoreException
	{
		getCatalogMap().clear();

		try
		{
			Page catalogFile = getPageManager().getPage(listCatalogXml());
			if (catalogFile.exists())
			{
				try
				{
					Element rootE = new XmlUtil().getXml(catalogFile.getReader(), catalogFile.getCharacterEncoding());
					XMLConfiguration rootConfig = new XMLConfiguration();
					rootConfig.populate(rootE);
	
					Category root = createCatalog(rootConfig);
					setRootCategory(root);
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
					throw new StoreException(ex);
				}
			}
			else
			{
				log.error("No catalog file found " + catalogFile.getPath());
				Category root = new Category();
				root.setId("index");
				root.setName("Index");
				setRootCategory(root);
			}
		}
		catch ( Exception ex)
		{
			throw new StoreException(ex);
		}
	}

	/**
	 * @param inRootElement
	 * @return
	 */
	protected Category createCatalog(Configuration inRootConfig) throws OpenEditException
	{
		Category cat = new Category();
		cat.setId(inRootConfig.getAttribute("id"));
		if ( cat.getId() == null)
		{
			log.error("Corrupt catalog with no id");
		}
		cat.setName(inRootConfig.getAttribute("name"));
		for (Iterator iter = inRootConfig.getAttributeNames().iterator(); iter.hasNext();)
		{
			String attrName = (String) iter.next();
			if (!attrName.equals("id") && !attrName.equals("name"))
			{
				cat.setProperty(attrName, inRootConfig.getAttribute(attrName));
			}
		}
		// Also supports property
		for (Iterator iter = inRootConfig.getChildren("property").iterator(); iter.hasNext();)
		{
			Configuration config = (Configuration) iter.next();
			cat.setProperty(config.getAttribute("id"), config.getValue());
		}

		String shortdesc = inRootConfig.getChildValue("shortdescription");
		cat.setShortDescription(shortdesc);

		loadOptions(cat, inRootConfig);
        loadRelatedCategoryIds(cat, inRootConfig);
		for (Iterator iter = inRootConfig.getChildren("catalog").iterator(); iter.hasNext();)
		{
			Configuration config = (Configuration) iter.next();
			cat.addChild(createCatalog(config));
		}
		return cat;
	}

	protected void loadOptions(Category inCategory, Configuration inConfig)
	{
		inCategory.clearOptions();
		for (Iterator iter = inConfig.getChildren("option").iterator(); iter.hasNext();)
		{
			Configuration optionConfig = (Configuration) iter.next();
			inCategory.addOption(createOption(optionConfig));
		}
	}
	protected void loadRelatedCategoryIds(Category inCategory, Configuration inProductConfig)
	{
		inCategory.clearRelatedCategoryIds();
		Configuration relatedProductsElem = inProductConfig.getChild("related-categories");
		if (relatedProductsElem != null)
		{
			for (Iterator iter = relatedProductsElem.getChildIterator("category"); iter.hasNext();)
			{
				Configuration relatedProdConfig = (Configuration) iter.next();
				inCategory.addRelatedCategoryId(relatedProdConfig.getAttribute("id"));
			}
		}
		Configuration linkedToElem = inProductConfig.getChild("linkedtocategory");
		if( linkedToElem != null)
		{
			inCategory.setLinkedToCategoryId(linkedToElem.getAttribute("id"));
		}
		else
		{
			inCategory.setLinkedToCategoryId(null);
		}
		
	}
	protected void saveRelatedCategories(Category inCategory, Element inCategoryElement)
	{
		deleteElements(inCategoryElement, "related-categories");
		if (inCategory.getRelatedCategoryIds().size()>0)
		{
			Element relatedProductsElem = inCategoryElement.addElement("related-categories");
			for (Iterator iter = inCategory.getRelatedCategoryIds().iterator(); iter.hasNext();)
			{
				String relatedProductId = (String) iter.next();
				relatedProductsElem.addElement("category").addAttribute("id", relatedProductId);
			}
		}
		deleteElements(inCategoryElement, "linkedtocategory");
		if( inCategory.getLinkedToCategoryId() != null)
		{
			Element linkedToElem = inCategoryElement.addElement("linkedtocategory");
			if( linkedToElem != null)
			{
				linkedToElem.addAttribute("id", inCategory.getLinkedToCategoryId());
			}
		}
	}
	
	/*
	 * (non-javadoc)
	 * 
	 * @see com.openedit.store.CatalogArchive#listNonUserSelectedCatalogs()
	 */
	public List listNonUserSelectedCatalogs() throws StoreException
	{
		List sublist = new ArrayList();
		for (Iterator iter = listAllCategories().iterator(); iter.hasNext();)
		{
			Category cat = (Category) iter.next();
			if (!cat.isUserSelected())
			{
				sublist.add(cat);
			}
		}
		return sublist;
	}

	public List listUserSelectedCatalogs() throws StoreException
	{
		List sublist = new ArrayList();
		for (Iterator iter = listAllCategories().iterator(); iter.hasNext();)
		{
			Category cat = (Category) iter.next();
			if (cat.isUserSelected())
			{
				sublist.add(cat);
			}
		}
		return sublist;
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	public void saveCategory(Category inCategory) throws StoreException
	{
		if (inCategory.getParentCatalog() == null && 
				!inCategory.getId().equals(getRootCategory().getId()))
			//don't want to make child of the root cat if it is the root cat
		{
			addChild(inCategory);
		}
		else
		{
			cacheCategory(inCategory);
		}
		saveAll();
	}
	public Category cacheCategory(Category inCat)
	{
		getCatalogMap().put(inCat.getId(), inCat);
		for (Iterator iterator = inCat.getChildren().iterator(); iterator.hasNext();)
		{
			Category child = (Category) iterator.next();
			cacheCategory(child);
		}
		return inCat;
	}
	/**
	 * @deprecated use saveCategory
	 */
	public void saveCatalog(Category inCategory) throws StoreException
	{
		saveCategory(inCategory);
	}

	public Category addChild(Category inCatalog) throws StoreException
	{
		getRootCategory().addChild(inCatalog);
		cacheCategory(inCatalog);
		return inCatalog;
	}

	public XmlUtil getXmlUtil()
	{
		return fieldXmlUtil;
	}

	public void setXmlUtil(XmlUtil inXmlUtil)
	{
		fieldXmlUtil = inXmlUtil;
	}
	
	public Category createCategoryTree(String inPath) throws OpenEditException
	{
		return createCategoryTree(inPath,null);
	}	
	public Category createCategoryTree(String inPath, List inNames) throws OpenEditException
	{
		
		
		if (inPath.length() < 1 )
		{
			return getRootCategory();
		}
		
		if( inPath.endsWith("/"))
		{
			inPath = inPath.substring(0,inPath.length()-1);
		}
	
		
		String catid = getCatalogId( inPath );
		Category child = getCategory(catid);
		if (child == null)
		{
			//make sure we have a parent to put it in
			child = new Category();
			child.setId(catid);
			
			if( inNames != null)
			{
				child.setName((String) inNames.remove(inNames.size()-1));
			}
			else
			{
				child.setName(PathUtilities.extractFileName(inPath));
			}
			String parentPath = PathUtilities.extractDirectoryPath(inPath);
			if( parentPath == null  || parentPath == "/")
			{
				return child;
			}
			
			Category inParentCategory = createCategoryTree(parentPath,inNames);
			inParentCategory.addChild(child);
			cacheCategory(child);
			saveAll();
		}
		return child;
	}

	
	
	
	private String getCatalogId(String inPath)
	{
		//subtract the start   /store/products/stuff/more -> stuff_more
		if (inPath.length() < 0)
		{
			return "index";
		}
		
		if (inPath.startsWith("/"))
		{
			inPath = inPath.substring(1);
		}
		inPath = inPath.replace('/', '_');
		String id = PathUtilities.extractId(inPath, true);
		return id;
	}


}
