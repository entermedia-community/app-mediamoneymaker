/*
 * Created on Mar 2, 2004
 */
package org.openedit.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.openedit.Data;
import org.openedit.data.ValuesMap;

/**
 * @author cburkey
 *
 */
public class Category implements Data
{
	protected String fieldName;
	protected String fieldId;
	protected String fieldDescription;
	protected String fieldShortDecription;
//	protected Map fieldItemMap;
	protected boolean fieldUserSelected; //Deprecated?
	protected int fieldItemCount;
	protected List fieldChildren;
	protected Category fieldParentCatalog;
	protected String fieldParentId;
	protected Map fieldImages;
	protected Map fieldLinkedFiles; //these are links to files that this catalog might include (such as PDF's)
	protected String fieldBrochure;
	protected String fieldSpecSheet;
	protected List fieldOptions;
	protected List fieldRelatedCategoryIds;
	protected String fieldLinkedToCategoryId;
	protected ValuesMap fieldProperties;
	
	public ValuesMap getProperties()
	{
		if (fieldProperties == null)
		{
			fieldProperties = new ValuesMap();
		}
		return fieldProperties;
	}

	
	
	
	public Category()
	{
	}

	public Category( String inName )
	{
		fieldName = inName;
	}
	
public Category(String inId, String inName)
	{
		setId(inId);
		if( inName != null)
		{
			setName(inName.trim());
		}
	}

	
	public List getRelatedCategoryIds() {
		if (fieldRelatedCategoryIds == null) {
			fieldRelatedCategoryIds = new ArrayList();
			
		}

		return fieldRelatedCategoryIds;
	
	}

	public void setRelatedCategoryIds(List fieldRelatedCategoryIds) {
		this.fieldRelatedCategoryIds = fieldRelatedCategoryIds;
	}


	/* This is old, we now use Lucene to find the items
	public Collection getItems()
	{
		return getItemMap().values();
	}

	public Map getItemMap()
	{
		if ( fieldItemMap == null )
		{
			fieldItemMap = new HashMap();
		}
		return fieldItemMap;
	}

	public int getNumItems()
	{
		return getItemMap().size();
	}

	public void addItem( Product inItem )
	{
		getItemMap().put( inItem.getId(), inItem );
	}

	public void removeItem( Product inItem )
	{
		getItemMap().remove( inItem );
	}

	public Product getItem( String inSkuNumber )
	{
		return (Product) getItemMap().get( inSkuNumber );
	}
*/
	public String getName()
	{
		return fieldName;
	}

	public void setName( String inString )
	{
		fieldName = inString;
	}
	
	public String getId()
	{
		return fieldId;
	}
	public void setId(String inId)
	{
		fieldId = inId;
	}
	public String toString()
	{
		return getName();
	}
	public boolean isUserSelected()
	{
		return fieldUserSelected;
	}
	public void setUserSelected(boolean inUserSelected)
	{
		fieldUserSelected = inUserSelected;
	}
	public int getItemCount() {
		return fieldItemCount;
	}
	public void setItemCount(int inItemCount) {
		fieldItemCount = inItemCount;
	}
	public boolean isContainsItems()
	{
		return getItemCount() > 0;
	}
    /**
     * @return Returns the children.
     */
    public List getChildren()
    {
    	if (fieldChildren == null)
		{
			fieldChildren = new ArrayList();
		}
        return fieldChildren;
    }
    /**
     * @param children The children to set.
     */
    public void setChildren( List inChildren )
    {
    	fieldChildren = inChildren;
    	for ( Iterator iter = inChildren.iterator(); iter.hasNext(); )
		{
			Category cat = (Category) iter.next();
		  	cat.setParentCatalog(this);
		}
    }
    public Category addChild( Category inNewChild )
    {
    	inNewChild.setParentCatalog(this);
    	//I  removed this to speed things up
//    	for (int i = 0; i < getChildren().size(); i++)
//		{
//			Category element = (Category) getChildren().get(i);
//			if ( element.getId().equals(inNewChild.getId()))
//			{
//	    		getChildren().set(i, inNewChild);
//	    		return inNewChild;
//			}
//		}
		getChildren().add(inNewChild);
    	return inNewChild;
    }
    public Category getChild(String inId)
    {
    	for (Iterator iter = getChildren().iterator(); iter.hasNext();)
		{
			Category element = (Category) iter.next();
			if ( element.getId().equals(inId))
			{
				return element;
			}
		}
    	return null;
    }
    
    public void removeChild( Category inChild )
    {
    	Category child = getChild(inChild.getId());
    	if ( child != null)
    	{
    		getChildren().remove(child);
    		child.setParentCatalog(null);
    	}
    		
		inChild.setParentCatalog( null );
    }
    public boolean hasParent(String inId)
    {
    	Category parent = this;
    	while( parent != null)
    	{
    		if ( parent.getId().equals(inId) )
    		{
    			return true;
    		}
    		parent = parent.getParentCatalog();
    	}
    	return false;
    }
    
	/**
	 * @return
	 */
	public boolean hasChildren()
	{
		return fieldChildren != null && fieldChildren.size() > 0;
	}
	public boolean hasCatalog( String inId )
	{
		if( getId().equals( inId) )
		{
			return true;
		}
		if ( hasChildren() )
		{
			for (Iterator iter = getChildren().iterator(); iter.hasNext();)
			{
				Category child = (Category) iter.next();
				if( child.hasCatalog(inId))
				{
					return true;
				}
			}
		}
		return false;
	}
	

	public boolean hasChild(String inId)
	{
		if ( hasChildren() )
		{
			for (Iterator iter = getChildren().iterator(); iter.hasNext();)
			{
				Category child = (Category) iter.next();
				if( child.getId().equals( inId) )
				{
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean isAncestorOf( Category inCatalog )
	{
		for (Iterator children = getChildren().iterator(); children.hasNext();) 
		{
			Category child = (Category) children.next();
			if (child == inCatalog)
			{
				return true;
			}
			else if (child.hasChildren() && child.isAncestorOf(inCatalog))
			{
				return true;
			}
		}
		return false;
	}
	public Category getParentCatalog()
	{
		return fieldParentCatalog;
	}
	public void setParentCatalog( Category parentCatalog )
	{
		fieldParentCatalog = parentCatalog;
		if( parentCatalog != null)
		{
			setParentId(parentCatalog.getId());
		}
	}
	
	/**
	 * Returns a list of all the ancestors of this catalog, starting at the
	 * catalog at the given level and ending at this catalog itself.
	 * 
	 * @param inStartLevel  The level at which to start listing ancestors (0 is
	 *                      the root, 1 is the first-level children, etc.)
	 * 
	 * @return  The list of ancestors of this catalog
	 */
	public List listAncestorsAndSelf( int inStartLevel )
	{
		LinkedList result = new LinkedList();
		Category catalog = this;
		while ( catalog != null )
		{
			result.addFirst( catalog );
			catalog = catalog.getParentCatalog();
		}
		return result.subList( inStartLevel, result.size() );
	}
	public List getChildrenInRows(int inColCount)
	{
		//Now break up the page into rows by dividing the count they wanted
		List children = getChildren();
		double rowscount = (double)children.size() / (double)inColCount;
		
		List rows = new ArrayList();
		for (int i = 0; i < rowscount; i++)
		{
			int start = i*inColCount;
			int end = i*inColCount + inColCount;
			List sublist = children.subList(start,Math.min( children.size(),end ));
			rows.add(sublist);
		}
		return rows;
	}
	public int getLevel()
	{
		int i = 1;
		Category parent = this;
		while(parent != null)
		{
			parent = parent.getParentCatalog();
		}
		return i;
	}
	
	public String getDescription()
	{
		return fieldDescription;
	}
	public void setDescription(String inDescription)
	{
		fieldDescription = inDescription;
	}
	public String get(String inKey)
	{
		if(inKey.equals("id"))
		{
			return getId();
		}
		String val = getProperty(inKey);
		if ( val != null)
		{
			return val;
		}
		Category parent  = getParentCatalog();
		if( parent != null)
		{
			return parent.get(inKey);
		}
		return null;
	}
	

	

	public void setProperty( String inKey, String inValue )
	{
		if ( inValue != null )
		{
			getProperties().put( inKey, inValue );
		}
		else
		{
			getProperties().remove( inKey );
		}
	}

	public void setProperty( String inKey, boolean inValue )
	{
		setProperty( inKey, String.valueOf( inValue ) );
	}
	public boolean isPropertyTrue(String inKey)
	{
		String val = getProperty(inKey);
		if ( "true".equalsIgnoreCase(val))
		{
			return true;
		}
		return false;
	}
	public String getProperty( String inKey )
	{
		return (String)getProperties().get( inKey );
	}

	/**
	 * @param inKey
	 */
	public void removeProperty(String inKey)
	{
		getProperties().remove(inKey);
	}
	public Image getImage( String inKey)
	{
		return (Image)getImages().get( inKey );
	}
	public Map getImages()
	{
		if ( fieldImages == null)
		{
			fieldImages = new HashMap(4);
		}
		return fieldImages;
	}
	public List loadImages(String inType)
	{
		List list = new ArrayList(getImages().keySet());
		Collections.sort(list);
		List images = new ArrayList();
		for (Iterator iter = list.iterator(); iter.hasNext();)
		{
			Image image = getImage((String) iter.next());
			if ( image.getType().equals(inType))
			{
				images.add(image);
			}
		}
		return images;
		
	}
	public List getImageList()
	{
		List list = new ArrayList(getImages().keySet());
		Collections.sort(list);
		List images = new ArrayList();
		for (Iterator iter = list.iterator(); iter.hasNext();)
		{
			Image image = getImage((String) iter.next());
			images.add(image);
			
		}
		return images;
	}
	
	/**
	 * Adds the given image to the list of actual images for this catalog, as
	 * returned by {@link #getImageList()}.  If there is already an image with
	 * the given image's ID, it will be replaced (in memory) with the new image.
	 * 
	 * @param inImage  The image to add
	 */
	public void addImage( Image inImage )
	{
		getImages().put(inImage.getId(), inImage);
	}
	
	public String getShortDescription()
	{
		return fieldShortDecription;
	}
	public void setShortDescription(String inShortDecription)
	{
		fieldShortDecription = inShortDecription;
	}
	
	/**
	 * This is a list of links associated with this catalog
	 * @deprecated This seems useless
	 * @return
	 */
	public List getLinkedFileList()
	{
		List list = new ArrayList(getLinkedFiles().keySet());
		Collections.sort(list);
		List files = new ArrayList();
		for (Iterator iter = list.iterator(); iter.hasNext();)
		{
			LinkedFile file = getLinkedFile( (String) iter.next());
			files.add(file);
			
		}
		return files;
	}
	public LinkedFile getLinkedFile(String inFilename)
	{
		return (LinkedFile)getLinkedFiles().get(inFilename);
	}
	public void putLinkedFile(String inFilename, LinkedFile inFile)
	{
		getLinkedFiles().put(inFilename,inFile);
	}
	public Map getLinkedFiles()
	{
		if (fieldLinkedFiles == null)
		{
			fieldLinkedFiles = new HashMap(4);
		}
		return fieldLinkedFiles;
	}

	public void clearImages()
	{
		getImages().clear();
	}

	public void clearLinkedFiles()
	{
		getLinkedFiles().clear();
	}

	public String getBrochure()
	{
		return fieldBrochure;
	}
	public void setBrochure(String inBrochure)
	{
		fieldBrochure = inBrochure;
	}
	public String getSpecSheet()
	{
		return fieldSpecSheet;
	}
	public void setSpecSheet(String inSpecSheet)
	{
		fieldSpecSheet = inSpecSheet;
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
		Map optionsMap = new HashMap();

		Category parent = this;
		while (parent != null)
		{
			if( parent.fieldOptions != null)
			{
				List catalogOptions = parent.getOptions();
	
				for (Iterator iter = catalogOptions.iterator(); iter.hasNext();)
				{
					Option option = (Option) iter.next();
					if( !optionsMap.containsKey(option.getId()))
					{
						optionsMap.put(option.getId(), option);
					}
				}
			}
			parent = parent.getParentCatalog();
		}
		
		return new ArrayList(optionsMap.values());
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
		
		if (getParentCatalog() != null)
		{
			return getParentCatalog().getOption(inOptionId);
		}
		
		return null;
		/*
		if ( getParentCatalog() != null)
		{
			return getParentCatalog().getOption(inOptionId);
		}
		*/

	}

	public void clearChildren()
	{
		getChildren().clear();
	}

	public Category getChildByName(String inCatName)
	{
		for (Iterator iter = getChildren().iterator(); iter.hasNext();)
		{
			Category cat = (Category) iter.next();
			if ( cat.getName().equals(inCatName))
			{
				return cat;
			}
		}
		return null;
	}
	public String getLink()
	{
		String path = getProperty("path");
		if( path != null)
		{
			return path;
		}
		String root = get("categoryhome");
		if(root == null)
		{
			root = "/store/categories/";
		}
		return root + getId() + ".html";
	}

	public List getParentCategories()
	{
		List paths = new ArrayList();
		Category parent = getParentCatalog();
		paths.add(this);
		while(parent != null)
		{
			paths.add(0,parent);
			parent = parent.getParentCatalog();
		}
		return paths;	
	}
	
	public void clearRelatedCategoryIds() {
		fieldRelatedCategoryIds = new ArrayList();
		
	}

	public void addRelatedCategoryId(String inId) {
		getRelatedCategoryIds().add(inId);
		
	}

	public String getLinkedToCategoryId() {
		return fieldLinkedToCategoryId;
	}

	public void setLinkedToCategoryId(String inLinkedToCategoryId) {
		fieldLinkedToCategoryId = inLinkedToCategoryId;
	}

	public String getParentId()
	{
		return fieldParentId;
	}

	public void setParentId(String inParentId)
	{
		fieldParentId = inParentId;
	}

	public String getSourcePath()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public void setSourcePath(String inSourcepath)
	{
		// TODO Auto-generated method stub
		
	}

//	public String getSourcePath()
//	{
//		StringBuffer out = new StringBuffer();
//		Category parent = this;
//		while( parent != null)
//		{
//			out.append(parent.getName());
//		}
//		
//		return null;
//	}
	
	
	

	public void setProperties(Map inProperties)
	{
		getProperties().putAll(inProperties);
	}
	
	public void setValues(String inKey, Collection<String> inValues)
	{
		getProperties().put(inKey, inValues);

	}

	@Override
	public Object getValue(String inKey)
	{
		return getProperties().get(inKey);
	}
	@Override
	public void setValue(String inKey, Object inValue)
	{
		getProperties().put(inKey, inValue);
	}
	
}
