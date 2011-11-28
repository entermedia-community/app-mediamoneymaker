/*
 * Created on Aug 15, 2005
 */
package org.openedit.store.excelconvert;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Header
{
	protected Map fieldHeaderNames;

	public Map getHeaderNames()
	{
		if (fieldHeaderNames == null)
		{
			fieldHeaderNames = new HashMap();
		}
		return fieldHeaderNames;
	}
	public void setHeaders(String[] inHeaders)
	{
		for (int i = 0; i < inHeaders.length; i++)
		{
			Integer integer = new Integer(i);
			getHeaderNames().put(integer,inHeaders[i]);
		}
	}

	public int getIndex(String inName)
	{
		Map headerNames = getHeaderNames();
		for (Iterator iter = headerNames.keySet().iterator(); iter.hasNext();)
		{
			Integer index = (Integer)iter.next();
			String name = (String) headerNames.get(index);
			if ( name.equals(inName))
			{
				return index.intValue();
			}
		}
		return -1;
	}
	public String getColumn(int inIndex)
	{
		String name = (String)getHeaderNames().get(new Integer(inIndex));
		if ( name != null)
		{
			return name;
		}
		return null;
	}
	public int getSize()
	{
		return getHeaderNames().size();
	}
	public void addCol(int inCol, String inStringCellValue)
	{
		Integer integer = new Integer(inCol);
		getHeaderNames().put(integer,inStringCellValue);
		
	}

}
