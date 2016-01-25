/*
 * Created on Mar 2, 2004
 */
package org.openedit.store;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.OpenEditRuntimeException;
import org.openedit.data.BaseData;
import org.openedit.money.Money;
import org.openedit.page.Page;

/**
 * @author cburkey
 *
 */
public class Product extends BaseData implements Data
{
	protected String fieldId;
	protected String fieldName;
	protected String fieldSourcePath;
	protected String fieldCatalogId;
	protected Page fieldSourcePage;
	protected String fieldDescription;
	protected String fieldHandlingChargeLevel;
	protected List fieldCategories;
	protected List fieldInventoryItems;
	protected Category fieldDefaultCategory;
	protected Map fieldProperties;
	protected String fieldDepartment;
	protected PriceSupport fieldPriceSupport;
	protected List fieldKeywords;
	protected boolean fieldAvailable = true;
	protected List fieldOptions;
	protected boolean fieldCustomPrice = false;
	protected boolean fieldTaxExempt = false;
	protected int fieldOrdering = -1; //the order that these product should be shown in a list
	protected String fieldShippingMethodId;
	protected Collection fieldRelatedProducts;
	protected List fieldRelatedFiles;
	protected String fieldDeliveryType;
	
	private static final Log log = LogFactory.getLog(Product.class);


	public Product()
	{
	}
	
	public Product( String inName )
	{
		setName(inName);
	}

	

	public List getRelatedFiles() {
		if (fieldRelatedFiles == null) {
			fieldRelatedFiles = new ArrayList();
			
		}

		return fieldRelatedFiles;
	}

	public void setRelatedFiles(List relatedFiles) {
		fieldRelatedFiles = relatedFiles;
	}

	public int getOrdering()
	{
		return fieldOrdering;
	}
	public void setOrdering(int inOrdering)
	{
		fieldOrdering = inOrdering;
	}

	public String getName()
	{
		return fieldName;
	}

	public void setName( String inName )
	{
		if( inName != null )
		{
			fieldName = inName.trim();
		}
		else
		{
			fieldName = null;
		}
	}
/**
 * This is an optional field
 * @return
 */
	
	public String getShortDescription()
	{
		
		return getProperty("shortdescription");
	}

	public void setShortDescription( String inDescription )
	{
		addProperty("shortdescription",inDescription);
	}

	public String toString()
	{
		if(getShortDescription() != null){
		return getShortDescription();
		}
		else if(getName() != null){
			return getName();
			
		}
		else return getId();
	}

	public String getId()
	{
		return fieldId;
	}

	public void setId( String inString )
	{
		fieldId = inString;
	}
	
	/**
	 * This will look in all the category objects if needed
	 */
	
	public String get(String inAttribute)
	{
		if( "name".equals(inAttribute ) )
		{
			return getName();
		}
		if( "id".equals(inAttribute ) )
		{
			return getId();
		}
		if ("sourcepath".equals(inAttribute))
		{
			return getSourcePath();
		}
		if ("catalogid".equals(inAttribute))
		{
			return getCatalogId();
		}
		if ("instock".equals(inAttribute)){
			return String.valueOf(isInStock());
		}
		String value = (String)getProperties().get(inAttribute);
//		if ( value instanceof PageProperty)
//		{
//			PageProperty prop = (PageProperty)value;
//			return prop.getValue();
//		}
		if ( value == null)
		{
			//Loop over all the catalogs and look for hit
			if (getDefaultCatalog() != null)
			{
				value = getDefaultCatalog().get(inAttribute);
			}	
			if( value == null)
			{
				for (Iterator iter = getCategories().iterator(); iter.hasNext();)
				{
					Category cat = (Category) iter.next();
					if( cat != getDefaultCatalog())
					{
						value = cat.get(inAttribute);
						if( value != null)
						{
							return value;
						}
					}
				}
			}
		}
		return value;
	}
	/**
	 * @deprecated use setProperty
	 * @param inValue
	 */
	
	public void addProperty(String inKey, String inValue)
	{
		setProperty(inKey, inValue);
	}
	
	public void removeProperties(String[] inKeys)
	{
		for(int i=0;i<inKeys.length;i++)
		{
			removeProperty(inKeys[i]);			
		}
	}
	
	public void removeProperty(String inKey)
	{
		if (inKey != null && inKey.length() > 0)
		{
			getProperties().remove(inKey);
		}
	}

	/**
	 * @param inCatid
	 */
	public void addCatalog(Category inCatid)
	{
		addCategory(inCatid);
	}
	public void addCategory(Category inCatid)
	{
		if ( inCatid == null)
		{
			throw new IllegalArgumentException("Catalogs cannot be null");
		}
		if ( !isInCatalog( inCatid ) )
		{
			getCategories().add(inCatid);
		}
	}
	
	/**
	 * @deprecated use removeCategory instead.
	 */
	public void removeCatalog(Category inCatid)
	{
		removeCategory(inCatid);
	}
	
	public void removeCategory( Category inCategory )
	{
		Category found = null;
		for (Iterator iter = getCategories().iterator(); iter.hasNext();)
		{
			Category element = (Category) iter.next();
			if ( element.getId().equals(inCategory.getId()))
			{
				found = element;
				break;
			}
		}
		if ( found != null)
		{
			getCategories().remove( found );
		}
	}
	/**
	 * 
	 * @deprecated
	 */
	public List getCatalogs()
	{
		return getCategories();
	}
	public List getCategories()
	{
		if (fieldCategories == null)
		{
			fieldCategories = new ArrayList();		
		}
		return fieldCategories;
	}
	public boolean isInCatalog ( Category inCategory ) {
		return isInCategory(inCategory);
	}
	public boolean isInCategory( Category inCat )
	{
		return isInCategory( inCat.getId() );
	}
	public boolean isInCategory ( String inCategoryId ) {
		for (Iterator iter = getCategories().iterator(); iter.hasNext();)
		{
			Category element = (Category) iter.next();
			if ( element.getId().equals(inCategoryId))
			{
				return true;
			}
		}
		return false;
	}
	public Option getDefaultColor()
	{
		if ( hasColor())
		{
			return (Option) getColors().iterator().next();
		}
		return null;
	}
	public Option getDefaultSize()
	{
		if ( hasSize())
		{
			return (Option) getSizesSorted().iterator().next();
		}
		return null;
	}
	
	public List getSizes()
	{
		List sizes = new ArrayList();
		for ( Iterator iter = getInventoryItems().iterator(); iter.hasNext(); )
		{
			InventoryItem item = (InventoryItem) iter.next();
			if ( item.hasSize() )
			{
				Option size = item.getSize();
				if (  !sizes.contains( size ) )
				{
					sizes.add( item.getSize() );
				}
			}
		}
		return sizes;
	}
	
	public InventoryItem getInventoryItemBySize(Option inSize){
		for ( Iterator iter = getInventoryItems().iterator(); iter.hasNext(); )
		{
			InventoryItem item = (InventoryItem) iter.next();
			if ( item.hasSize() )
			{
				Option size = item.getSize();
				if(inSize.equals(size)){
					return item;
				}
			}
		}
		return null;
	}
	

	public List getSizesSorted()
	{
		List sizes = getSizes();
		Collections.sort(sizes);
		return sizes;
	}
	
	public List getColors()
	{
		List colors = new ArrayList();
		for ( Iterator iter = getInventoryItems().iterator(); iter.hasNext(); )
		{
			InventoryItem item = (InventoryItem) iter.next();
			if ( item.hasColor() && !colors.contains( item.getColor() ) )
			{
				colors.add( item.getColor() );
			}
		}
		return colors;
	}
	/**
	 * List all the colors available in this size
	 * @param inSize
	 * @return
	 */
	public List colorsInSize(Option inSize)
	{
		if ( inSize == null || "na".equals(inSize.getValue() ) )
		{
			return getColors();
		}
		List colors = new ArrayList();
		for (Iterator iter = getInventoryItems().iterator(); iter.hasNext();)
		{
			InventoryItem item = (InventoryItem) iter.next();
			//if( item.isInStock())
			//{
				boolean add = false;
				if ( item.getSize() == null)
				{
					add = true;
				}
				else if ( inSize.equals(item.getSize()) )
				{
					add = true;
				}
				if ( item.getColor() == null || colors.contains(item.getColor() ) )
				{
					add = false;
				}
				if ( add )
				{
					colors.add(item.getColor());
				}
			//}
		}
		return colors;
	}
	public List getInventoryItems()
	{
		if (fieldInventoryItems == null)
		{
			fieldInventoryItems = new ArrayList();		
		}
		return fieldInventoryItems;
	}
	public void setInventoryItems(List inItems)
	{
		fieldInventoryItems = inItems;
	}
	public boolean hasSizes()
	{
		return getSizes().size() > 1;
	}
	public boolean hasColors()
	{
		return getColors().size() > 1;
	}
	
	public boolean hasSize()
	{
		if ( getSizes().size() == 0)
		{
			return false;
		}
		return true;
	}
	
	public boolean hasColor()
	{
		if ( getColors().size() == 0)
		{
			return false;
		}
		return true;
	}
	
	/**
	 * @deprecated Use getDefaultCategory instead.
	 */
	public Category getDefaultCatalog()
	{
		return getDefaultCategory();
	}
	
	public Category getDefaultCategory()
	{
		if ( fieldDefaultCategory == null && getCategories().size() >= 1)
		{
			//grab the one
			return (Category)getCategories().iterator().next();
		}
		return fieldDefaultCategory;
	}
	
	public Collection getRelatedCatalogs()
	{
		Category cat = getDefaultCatalog();
		if ( cat.getParentCatalog() != null )
		{
			return cat.getParentCatalog().getChildren();			
		}
		else
		{
			List list = new ArrayList();
			list.add( cat );
			return list;
		}
	}
	
	/**
	 * @deprecated use setDefaultCategory instead.
	 */
	public void setDefaultCatalog(Category inDefaultCatalog)
	{
		setDefaultCategory(inDefaultCatalog);
	}
	public void setDefaultCategory(Category inDefaultCategory)
	{
		fieldDefaultCategory = inDefaultCategory;
	}
	public Map getProperties()
	{
		if ( fieldProperties == null)
		{
			fieldProperties = ListOrderedMap.decorate(new HashMap());
		}
		return fieldProperties;
	}
	public String getProperty( String inKey )
	{
		if ("id".equals(inKey))
		{
			return getId();
		}
		if ("name".equals(inKey))
		{
			return getName();
		}
		if ("sourcepath".equals(inKey))
		{
			return getSourcePath();
		}
		String value = (String)getProperties().get( inKey );
		return value;
	}
	public void setProperties(Map inAttributes)
	{
		fieldProperties = inAttributes;
	}
	public void putAttribute(String inKey, String inValue)
	{
		setProperty(inKey, inValue);
	}
	
	public void setRogersAS400Id(String inValue)
	{
		setProperty("rogersas400id",inValue);
	}
	
	public void setFidoAS400Id(String inValue)
	{
		setProperty("fidoas400id",inValue);
	}
	
	public String getRogersAS400Id()
	{
		return get("rogersas400id");
	}
	
	public String getFidoAS400Id()
	{
		return get("fidoas400id");
	}
	
	public void setProperty(String inKey, String inValue)
	{
		if( "id".equals(inKey))
		{
			setId(inValue);
		}
		else if( "name".equals(inKey))
		{
			setName(inValue);
		}
		else if( "sourcepath".equals(inKey))
		{
			setSourcePath(inValue);
		}
		else
		{
			if (inValue != null)
			{
				getProperties().put(inKey, inValue);
			}
			else
			{
				getProperties().remove(inKey);
			}
		}
	}
	
	/**
	 * 
	 */
	public void clearUserCatalogs()
	{
		Category[] copy = (Category[])getCategories().toArray(new Category[getCategories().size()]);
		for (int i = 0; i < copy.length; i++)
		{
			if ( copy[i].isUserSelected() )
			{
				removeCatalog(copy[i]);
			}
		}
	}
	public void clearItems()
	{
		if ( fieldInventoryItems != null)
		{
			getInventoryItems().clear();
		}
	}
	public String getDepartment() {
		return fieldDepartment;
	}
	public void setDepartment(String fieldDepartment) {
		this.fieldDepartment = fieldDepartment;
	}
	
	public boolean isInStock()
	{
		for ( Iterator iter = getInventoryItems().iterator(); iter.hasNext(); )
		{
			InventoryItem item = (InventoryItem) iter.next();
			if ( item.isInStock() )
			{
				return true;
			}
		}
		return false;
	}
	public boolean isPartlyOutOfStock()
	{
		for ( Iterator iter = getInventoryItems().iterator(); iter.hasNext(); )
		{
			InventoryItem item = (InventoryItem) iter.next();
			if ( !item.isInStock() )
			{
				return true;
			}
		}
		return false;
	}

	public boolean isOnSale()
	{
		if ( hasProductLevelPricing() )
		{
			return getPriceSupport().isOnSale();
		}
		InventoryItem item = getInventoryItem(0);
		if ( item != null)
		{
			return item.isOnSale();
		}
/*		for ( Iterator iter = getInventoryItems().iterator(); iter.hasNext(); )
		{
			Item item = (Item) iter.next();
			if ( item.isOnSale() )
			{
				return true;
			}
		}
*/		return false;
	}
	public void addInventoryItem( InventoryItem inItem )
	{
		inItem.setProduct( this );
		getInventoryItems().add( inItem );
	}

	
	/**
	 * @param inI
	 * @return
	 */
	public InventoryItem getInventoryItem(int inI)
	{
		if( getInventoryItems().size() > inI)
		{
			return (InventoryItem)getInventoryItems().get(inI);
		}
		return null;
	}
	public InventoryItem getInventoryItemBySku( String inSku)
	{
		for (Iterator iter = getInventoryItems().iterator(); iter.hasNext();)
		{
			InventoryItem element = (InventoryItem) iter.next();
			if ( element.getSku().equals(inSku))
			{
				return element;
			}
//			if (element.getProduct().getId().equals(inSku)){
//				return element;
//			}
		}
		return null;
	}
	public InventoryItem getInventoryItemByOptions(Collection inOptions)
	{
		for (Iterator iter = getInventoryItems().iterator(); iter.hasNext();)
		{
			InventoryItem element = (InventoryItem) iter.next();
			if ( element.isExactMatch(inOptions))
			{
				return element;
			}
		}
		return null;
	}

	public InventoryItem getCloseInventoryItemByOptions(Set inOptions)
	{
		//First check for exact match. Then size color only, then size only then color only
		for (Iterator iter = getInventoryItems().iterator(); iter.hasNext();)
		{
			InventoryItem element = (InventoryItem) iter.next();
			if ( element.isExactMatch((inOptions) ) )
			{
				return element;
			}
		}
		//size color
		for (Iterator iter = getInventoryItems().iterator(); iter.hasNext();)
		{
			InventoryItem element = (InventoryItem) iter.next();
			if ( element.isCloseMatch((inOptions) ) )
			{
				return element;
			}
		}
		for (Iterator iter = getInventoryItems().iterator(); iter.hasNext();)
		{
			InventoryItem element = (InventoryItem) iter.next();
			if ( element.isAnyMatch( inOptions))
			{
				return element;
			}
		}
		if( getInventoryItemCount() > 0)
		{
			return (InventoryItem)getInventoryItems().get(0);
		}
		return null;
	}

	public boolean hasProductLevelPricing()
	{
		return fieldPriceSupport != null;
	}
	
	
	public PriceSupport getPriceSupport()
	{
		return fieldPriceSupport;
	}
	public void setPriceSupport( PriceSupport priceSupport )
	{
		fieldPriceSupport = priceSupport;
	}
	public Money getRetailUnitPrice()
	{
		//return getPriceSupport().getPrice();
		return getPriceSupport().getRetailPrice();
		
	}
	
	//** this is the lowest price avialable
	public Money getYourPrice()
	{
		if( hasProductLevelPricing() )
		{
			Money price  = getPriceSupport().getYourPriceByQuantity( 1 );
			return price;
		}
		else if (getInventoryItemCount() > 0)
		{
			// ask cb
			Money price = getInventoryItem(0).getYourPrice();
			if (price != null)
			{
				return price;
			}
			else
			{
				return Money.ZERO;
			}
		}
		else
		{
			return Money.ZERO;
		}
	}
	
	public Money getRetailPrice()
	{
		Money price  = null;
		if( hasProductLevelPricing() )
		{
			price = getPriceSupport().getRetailPrice();
		} 
		else if (getInventoryItemCount() > 0)
		{
			price = getInventoryItem(0).getRetailPrice();
		}
		else
		{
			price = Money.ZERO;
		}
		return price;
	}
	
	
	/**
	 * @param inQuantity
	 * @param money
	 */
	public void addTierPrice( int inQuantity, Price inPrice )
	{
		if (getPriceSupport() == null)
		{
			setPriceSupport(new PriceSupport());
		}
		getPriceSupport().addTierPrice( inQuantity, inPrice );
	}
	public boolean hasKeywords()
	{
		return fieldKeywords != null && fieldKeywords.size() > 0;
	}
	public List getKeywords()
	{
		if ( fieldKeywords == null)
		{
			fieldKeywords = new ArrayList();
		}
		return fieldKeywords;
	}
	public boolean isAvailable()
	{
		return fieldAvailable;
	}
	public void setAvailable(boolean inAvailable)
	{
		fieldAvailable = inAvailable;
	}

	/**
	 * @return
	 */
	public int getInventoryItemCount()
	{
		return getInventoryItems().size();
	}
	/**
	 * 
	 */
	public void clearCatalogs()
	{
		getCategories().clear();
	}

	public List getOptions()
	{
		if (fieldOptions == null)
		{
			fieldOptions = new ArrayList();
		}
		return fieldOptions;
	}
	
	public List getAllOptions()
	{
		List optionsMap = new ArrayList();
		for (Iterator iter = getOptions().iterator(); iter.hasNext();)
		{
			Option option = (Option) iter.next();
			optionsMap.add(option);
		}
		if( getDefaultCatalog() != null)
		{
			List catalogOptions = getDefaultCatalog().getAllOptions();
			for (Iterator iter = catalogOptions.iterator(); iter.hasNext();)
			{
				Option option = (Option) iter.next();
				optionsMap.add(option);
			}
		}			
		return optionsMap;
	}

	public void setOptions(List inOptions)
	{
		fieldOptions = inOptions;
	}

	public void addOption(Option inOption)
	{
		removeOption(inOption.getId());
		getOptions().add(inOption);
	}
	
	public void removeOption(String id)
	{
		List options = getOptions();
		for (int i = 0; i < options.size(); i++) 
		{
			Option option = (Option)options.get(i);
			if (option.getId().equals( id ) )
			{
				getOptions().remove(i);
			}
		}
	}

	public void clearOptions()
	{
		getOptions().clear();
	}

	public Option getOption(String inOptionId)
	{
		for (Iterator it = getOptions().iterator(); it.hasNext();)
		{
			Option option = (Option)it.next();
			if (inOptionId.equals(option.getId()))
			{
				return option;
			}
		}
		
		if (getDefaultCatalog() != null)
		{
			return getDefaultCatalog().getOption(inOptionId);
		}	

		
		return null;
	}

	public String getHandlingChargeLevel()
	{
		return fieldHandlingChargeLevel;
	}

	public void setHandlingChargeLevel(String inHandlingChargeLevel)
	{
		fieldHandlingChargeLevel = inHandlingChargeLevel;
	}
	
	public boolean isCustomPrice()
	{
		return fieldCustomPrice;
	}

	public void setCustomPrice(boolean inCustomPrice)
	{
		fieldCustomPrice = inCustomPrice;
	}

	public boolean isTaxExempt()
	{
		return fieldTaxExempt;
	}
	public void setTaxExempt(boolean inTaxExempt)
	{
		fieldTaxExempt = inTaxExempt;
	}
	public boolean hasProperty(String inKey)
	{
		String value = getProperty(inKey);
		if ( value != null)
		{
			return true;
		}
		for (Iterator iter = getInventoryItems().iterator(); iter.hasNext();)
		{
			InventoryItem item = (InventoryItem) iter.next();
			value = item.getProperty(inKey);
			if ( value != null)
			{
				return true;
			}
			List prop = item.getPropertiesStartingWith(inKey);
			if ( prop.size() > 0)
			{
				return true;
			}
		}
		return false;
	}
	//this is a fixed method, this is optional since the cart has a range of methods available	
	public String getShippingMethodId()
	{
		return fieldShippingMethodId;
	}
	public void setShippingMethodId(String inShippingMethodId)
	{
		fieldShippingMethodId = inShippingMethodId;
	}

	
	
	
	
	public String getDescription()
	{
		return fieldDescription;
	}

	public void setDescription(String inDescription)
	{
		fieldDescription = inDescription;
	}

	public void addKeyword(String inString)
	{
		if( inString == null)
		{
			log.debug("Null keyword");
		}
		else
		{
			getKeywords().add(inString);
		}
	}
	public void removeKeyword(String inKey)
	{
		getKeywords().remove(inKey);
	}


	public String getDeliveryType()
	{
		return fieldDeliveryType;
	}

	public void setDeliveryType(String inDeliveryType)
	{
		fieldDeliveryType = inDeliveryType;
	}

	public Date getDate(String inField, String inDateFormat)
	{
		String date = getProperty(inField);
		if( date != null)
		{
			SimpleDateFormat format = new SimpleDateFormat(inDateFormat);
			try
			{
				return format.parse(date);
			} catch (ParseException e)
			{
				throw new OpenEditRuntimeException(e);
			}
		}
		return null;
	}

  
	public void setKeywords(List inKeywords)
	{
		fieldKeywords = inKeywords;
	}

	public void clearKeywords()
	{
		if( fieldKeywords != null)
		{
			fieldKeywords.clear();
		}
	}
	
	public void incrementProperty(String property, int delta) throws Exception
	{
		String currentValue = getProperty(property);
		int current = Integer.parseInt(currentValue);
		current = current + delta;
		setProperty(property, Integer.toString(current));	
	}

	public boolean hasRelatedProducts()
	{
		return getRelatedProducts().size() >0;
	}
	
	public Product copy(String newId)
	{
		if (newId == null)
		{
			newId = getId();
		}
		Product product = new Product();
		product.setId(newId);
		product.setName(getName());
		product.setDescription(getDescription());
		product.setHandlingChargeLevel(getHandlingChargeLevel());
		product.setOptions(getOptions());
		product.setOrdering(getOrdering());
		product.setDepartment(getDepartment());
		product.setPriceSupport(getPriceSupport());
		product.setKeywords(getKeywords());
		product.setProperties(new HashMap(getProperties()));
		product.setAvailable(isAvailable() );

		product.setDefaultCatalog(getDefaultCatalog());
		product.setDeliveryType(getDeliveryType());
		product.setShippingMethodId(getShippingMethodId());
		product.setCustomPrice(isCustomPrice());
		product.setTaxExempt(isTaxExempt());
		
		product.setInventoryItems(null);
		int count = 1;
	    for (Iterator iterator = getInventoryItems().iterator(); iterator.hasNext();) {
			InventoryItem newItem, item = (InventoryItem) iterator.next();
			newItem=item.copy();
			newItem.setProduct(product);
			newItem.setSku(product.getId() + "-"+count);
			product.addInventoryItem(newItem);
		}
	    
		Collection catalogs = getCategories();
		for (Iterator iter = catalogs.iterator(); iter.hasNext();) 
			{
				Category element = (Category) iter.next();
				product.addCatalog(element);
		}
			    
		product.setOptions(null);
	    for (Iterator iterator = getOptions().iterator(); iterator.hasNext();) {
			Option newOption = ((Option) iterator.next()).copy();
			product.addOption(newOption);
		}
	    
		product.setKeywords(null);
	    for (Iterator iterator = getKeywords().iterator(); iterator.hasNext();) {
			product.addKeyword((String)iterator.next());
		}

	
		return product;
	}
	
	public Product copy() {
		return copy(null);
	}

	public void setCategories(List inCatalogs) {
		fieldCategories = inCatalogs;
	}

	public void setOriginalImagePath( String inPath )
	{
		setProperty( "originalpath", inPath );
	}

	public String getSourcePath()
	{
		return fieldSourcePath;
	}

	public void setSourcePath(String inSourcePath)
	{
		fieldSourcePath = inSourcePath;
	}
	
	public Page getSourcePage()
	{
		return fieldSourcePage;
	}

	public void setSourcePage(Page inSourcePage)
	{
		fieldSourcePage = inSourcePage;
	}
	
	public String getSaveAsName()
	{
		String name = getName();
		if (name.indexOf(".") == -1)
		{
			String ext = getProperty("fileformat");
			if (ext == null && getSourcePath().indexOf('.') != -1)
			{
				ext = getSourcePath().substring(getSourcePath().lastIndexOf('.') + 1);
			}
			if (ext != null)
			{
				name = name + "." + ext;
			}
		}
		return name;
	}



	public Collection getRelatedProducts() 
	{
		if (fieldRelatedProducts == null) 
		{
			fieldRelatedProducts = new ArrayList();
		}
		return fieldRelatedProducts;
	}


	public void clearRelatedProducts() {
		setRelatedProducts(null);
		
	}

	public void setRelatedProducts(Collection inRelatedProducts) {
		fieldRelatedProducts = inRelatedProducts;
	}

	
	
	public String getMediaName()
	{
		String primaryImageName = getProperty("primaryimagename");
		if( primaryImageName == null)
		{
			primaryImageName = getName();
		}
//		if( primaryImageName == null)
//		{
//			primaryImageName = getId(); //last resort?
//		}
		return primaryImageName;
	}	
	public String getPrimaryImagePath()
	{
		String primaryImageName = getProperty("primaryimagename");
		String sourcePath = getSourcePath();
		
		if (primaryImageName != null && sourcePath.endsWith("/"))
		{
			return sourcePath + primaryImageName;
		}
		else
		{
			return sourcePath;
		}
	}
	public String getCatalogId()
	{
		return fieldCatalogId;
	}
	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}

	public boolean isCoupon() {
		return (getProperty("producttype")!=null && getProperty("producttype").equals("coupon"));
	}

	
	//TODO: Add these methods to the Data interface
//		public Collection getValues(String inPreference)
//		{
//			String val = get(inPreference);
//			
//			if (val == null)
//				return null;
//			
//			String[] vals = val.split("\\s+");
//
//			Collection collection = Arrays.asList(vals);
//			//if null check parent
//			return collection;
//		}
//		
//		public void setValues(String inKey, Collection<String> inValues)
//		{
//			StringBuffer values = new StringBuffer();
//			for (Iterator iterator = inValues.iterator(); iterator.hasNext();)
//			{
//				String detail = (String) iterator.next();
//				values.append(detail);
//				if( iterator.hasNext())
//				{
//					values.append(" ");
//				}
//			}
//			setProperty(inKey,values.toString());
//		}
	
}