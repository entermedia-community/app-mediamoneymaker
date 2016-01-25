/*
 * Created on Aug 16, 2005
 */
package org.openedit.store.convert;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.ModuleManager;
import org.openedit.store.CatalogConverter;
import org.openedit.store.Store;
//TODO: Make it implement an Interface
public class CompositeConverter extends CatalogConverter
{
	List fiedlCatalogImportConverters;
	ModuleManager fieldModuleManager;
	private static final Log log = LogFactory.getLog(CompositeConverter.class);
	
	public void convert(Store inStore, ConvertStatus inLog) throws Exception
	{
		//boolean converted = false;

		//TODO: Check properties file
		
		for ( Iterator iter = getCatalogConverters().iterator(); iter.hasNext(); )
		{			
			String name = (String)iter.next();
			
			if( getModuleManager().contains( name ) )
			{
				CatalogConverter converter = null;
				try
				{
					inLog.add("Starting converter: " + name );
					converter = (CatalogConverter)getModuleManager().getBean(name);
					converter.setModuleManager(getModuleManager()); //TODO: Do in Spring
				}
				catch ( Exception ex)
				{
					inLog.add("Could not load converter " + name + " " + ex);
					log.info("Could not load converter " + name + " " + ex);
					continue;
				}
				converter.convert(inStore, inLog);
			}
			else
			{
				log.info("Bean not found " + name);
			}
		}
	}
	public List getCatalogConverters()
	{
		return fiedlCatalogImportConverters;
	}

	public void setCatalogConverters(List inFiedlCatalogImportConverters)
	{
		fiedlCatalogImportConverters = inFiedlCatalogImportConverters;
	}
	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}
	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}
}
