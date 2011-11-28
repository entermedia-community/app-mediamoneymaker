package org.openedit.store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.openedit.Data;
import org.openedit.data.CompositeData;

public class CompositeProduct extends Product implements Data, CompositeData
{
	private static final long serialVersionUID = -7154445212382362391L;
	protected List fieldItems;
	
	public List getItems() 
	{
		if (fieldItems == null) 
		{
			fieldItems = new ArrayList();
		}

		return fieldItems;
	}

	public void setItems(List inItems) 
	{
		fieldItems = inItems;
	}
	public int size()
	{
		return getItems().size();
	}
	public Iterator iterator()
	{
		return getItems().iterator();
	}
	
	public List getKeywords()
	{
		List keywords = new ArrayList();
		if (getItems().size() > 0)
		{

			keywords.addAll(((Product) getItems().get(0)).getKeywords());

			for (Iterator iterator = getItems().iterator(); iterator.hasNext();)
			{
				Product Product = (Product) iterator.next();
				keywords.retainAll(Product.getKeywords());
			}

			return keywords;
		}
		return keywords;
	}

	public void addKeyword(String inKey)
	{
		for (Iterator iterator = getItems().iterator(); iterator.hasNext();)
		{
			Product Product = (Product) iterator.next();
			Product.addKeyword(inKey);
		}
	}

	public void removeKeyword(String inKey)
	{
		for (Iterator iterator = getItems().iterator(); iterator.hasNext();)
		{
			Product Product = (Product) iterator.next();
			Product.removeKeyword(inKey);
		}
	}

	public List getCategories()
	{
		ArrayList categories = new ArrayList();
		HashMap countMap = new HashMap();
		HashMap categoryMap = new HashMap();
		for (Iterator iterator = getItems().iterator(); iterator.hasNext();)
		{
			Product Product = (Product) iterator.next();
			for (Iterator iterator2 = Product.getCategories().iterator(); iterator2.hasNext();)
			{
				Category cat = (Category) iterator2.next();
				if (countMap.get(cat.getId()) == null)
				{
					countMap.put(cat.getId(), new Integer(1));
					categoryMap.put(cat.getId(), cat);
				}
				else
				{
					int count = ((Integer) countMap.get(cat.getId())).intValue();
					countMap.put(cat.getId(), new Integer(count + 1));
				}
			}
		}
		for (Iterator i = categoryMap.keySet().iterator(); i.hasNext();)
		{
			String catid = (String) i.next();
			int count = ((Integer) countMap.get(catid)).intValue();
			if (count == getItems().size())
			{
				categories.add(categoryMap.get(catid));
			}
		}
		return categories;
	}

	public void addCategory(Category inCategory)
	{
		for (Iterator iterator = getItems().iterator(); iterator.hasNext();)
		{
			Product p = (Product) iterator.next();
			p.addCategory(inCategory);
		}
	}

	public void removeCategory(Category inCategory)
	{
		for (Iterator iterator = getItems().iterator(); iterator.hasNext();)
		{
			Product p = (Product) iterator.next();
			p.removeCategory(inCategory);
		}
	}
	
	protected String fieldId;
	
	public void addData(Data inData)
	{
		getItems().add((Product)inData);
	}
	public String getProperty(String inKey) 
	{
		return get(inKey);
	}
	public String get(String inId)
	{	
		if (getItems().size() > 0)
		{
			//return something only if all the values match the first record
			String target = ((Data)getItems().get(0)).get(inId);
			if (target != null)
			{
				for (Iterator iterator = getItems().iterator(); iterator.hasNext();)
				{
					Data data = (Data) iterator.next();
					if (!target.equals(data.get(inId)))
					{
						return null;					//they don't agree
					}
				}
				return target;
			}
		}
		
		return null;
	}

	public String getId()
	{
		return fieldId;
	}

	public String getName()
	{
		return "Multiple Data";
	}
	
	public void setName(String inName)
	{
		//Nothing to do here
	}

	public void setId(String inNewid)
	{
		fieldId = inNewid;
	}

	public void setProperty(String inId, String inValue)
	{
		String old = get(inId);
		//check that old is not null 
		if( old == inValue)
		{
			return;
		}
		if( old != null && old.equals( inValue ) )
		{
			return;
		}
//		if (inValue != null && inValue.length() > 0)
//		{
			for (Iterator iterator = getItems().iterator(); iterator.hasNext();)
			{
				Data data = (Data) iterator.next();
				data.setProperty(inId, inValue);
			}
//		}
	}

	public String getSourcePath()
	{
		if( getItems().size() > 0)
		{
			Product first = (Product) getItems().get(0);
			return first.getSourcePath() + "multi" + size();
		}
		return null;
	}

	public void setSourcePath(String inSourcepath)
	{
		
	}
}
