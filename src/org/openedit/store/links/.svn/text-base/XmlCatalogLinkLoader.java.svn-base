package org.openedit.store.links;

import java.util.Iterator;

import org.dom4j.Element;
import org.openedit.links.Link;
import org.openedit.links.XmlLinkLoader;
import org.openedit.store.Category;
import org.openedit.store.Store;

import com.openedit.OpenEditException;

public class XmlCatalogLinkLoader extends XmlLinkLoader
{
	protected Store fieldStore;

	public Store getStore()
	{
		return fieldStore;
	}

	public void setStore(Store inStore)
	{
		fieldStore = inStore;
	}
	
	//Overrride this parent method
	protected void checkLink(Element inElement, Link inLink) throws OpenEditException
	{
		Category parent = getStore().getCatalog(inLink.getId());
		if (parent != null)
		{
			//inLink.setPath(parent.getLink());		
			for (Iterator iter = parent.getChildren().iterator(); iter.hasNext();)
			{
				Category child = (Category) iter.next();
				Link childLink = makeCatalogLink(child);
				inLink.addChild(childLink);
			}
		}
	}
	
	private Link makeCatalogLink(Category inCatalog)
	{
		Link link = new Link();
		link.setId(inCatalog.getId());
		link.setText(inCatalog.getName());
		link.setPath(inCatalog.getLink());
		
		for (Iterator iter = inCatalog.getChildren().iterator(); iter.hasNext();)
		{
			Category childCat = (Category) iter.next();
			Link childLink = makeCatalogLink(childCat);
			link.addChild(childLink);
		}
		return link;
	}
	
}
