/*
 * Created on Dec 22, 2004
 */
package org.openedit.store.edit;

import java.util.Iterator;

import org.openedit.store.Category;
import org.openedit.store.StoreException;


/**
 * @author cburkey
 *
 */
public class CatalogToHtml
{
	protected Category fieldRootCatalog;
	
	public Category getRootCatalog()
	{
		return fieldRootCatalog;
	}
	public void setRootCatalog(Category inRootCatalog)
	{
		fieldRootCatalog = inRootCatalog;
	}
	public String getCatalogsAsHtml(String inSelectedCatalogId) throws StoreException
	{
		StringBuffer sb = new StringBuffer();
		appendCatalogsAsHtml(sb, getRootCatalog(), 0, inSelectedCatalogId);
		return sb.toString();
	}

	protected void appendCatalogsAsHtml(StringBuffer inBuffer, Category inCatalog, int inLevel,
		String inSelectedCatalogId)
	{
		if (inCatalog == null)
		{
			return;
		}
		inBuffer.append("<option value=\"");
		inBuffer.append(inCatalog.getId());
		inBuffer.append('\"');
		if (inCatalog.getId() != null && inCatalog.getId().equals(inSelectedCatalogId))
		{
			inBuffer.append(" selected");
		}
		inBuffer.append('>');
		if (inLevel > 0)
		{
			for (int n = 0; n < inLevel; n++)
			{
				inBuffer.append("&nbsp;&nbsp;&nbsp;");
			}
			inBuffer.append("- ");
		}
		inBuffer.append(inCatalog.getName());
		inBuffer.append("</option>");

		for (Iterator it = inCatalog.getChildren().iterator(); it.hasNext();)
		{
			Category child = (Category) it.next();
			appendCatalogsAsHtml(inBuffer, child, inLevel + 1, inSelectedCatalogId);
		}
	}
}
