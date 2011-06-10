package org.openedit.store.products;

import java.util.HashMap;
import java.util.Map;

import org.openedit.Data;

public class RelatedProduct implements Data 
{
	protected Map fieldProperties;
	
	
	
	public String getType() {
		return get("type");
	}
	public void setType(String inType) {
		setProperty("type",inType);
	}
	
	public void setProductId(String inProductId) {
		setProperty("productid", inProductId);
	}
	
	public String getProductId() {
		return get("productid");
	}
	
	//why do we need this?
	//Because we're using ArrayList.contains(), which uses this to compare objects
	public boolean equals(Object inObject)
	{
		if(inObject instanceof RelatedProduct)
		{
			RelatedProduct p = (RelatedProduct)inObject;
			
			// What about the "relatedtoproductid" ?
			if (getProductId() != null && getProductId().equals(p.getProductId()))
			{
				if (getType().equals(p.getType()))
				{
					if( getRelatedToCatalogId().equals( p.getRelatedToCatalogId()))
					{
						return getRelatedToProductId().equals(p.getRelatedToProductId());
					}
				}
			}
			
		}
		return false;
	}
	public String getRelatedToProductId()
	{
		return get("relatedtoproductid");
	}
	public void setRelatedToProductId(String inRelatedToProductId)
	{
		setProperty("relatedtoproductid",inRelatedToProductId);
	}

	public String get(String inId)
	{
		return (String)getProperties().get(inId);
	}

	public String getId()
	{
		return get("id");
	}

	public String getName()
	{
		return getType();
	}

	public void setName(String inName)
	{
		
	}
	
	public void setId(String inNewid)
	{
		setProperty("id",inNewid);
	}

	public void setProperty(String inId, String inValue)
	{
		getProperties().put(inId, inValue);
	}

	public Map getProperties()
	{
		if (fieldProperties == null)
		{
			fieldProperties = new HashMap(3);
		}
		return fieldProperties;
	}


	public String toString()
	{
		return getProductId() + " related to " + getRelatedToProductId() + "(" + getType() + ")";
	}
	public void setRelatedToCatalogId(String inId)
	{
		setProperty("relatedtocatalogid",inId);
		
	}
	public String getRelatedToCatalogId()
	{
		return get("relatedtocatalogid");
		
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

}
