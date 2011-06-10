/*
 * Created on Sep 15, 2004
 */
package org.openedit.store;

import java.util.List;

import com.openedit.ModuleManager;
import com.openedit.util.PathUtilities;

/**
 * @author cburkey
 *
 */
public abstract class CatalogConverter extends Converter
{
	protected ModuleManager fieldModuleManager;
	/*
	protected ProductArchive fieldProductArchive;
	
	public File productDirectory()
	{
		return new File( getStore().getStoreDirectory(),"products");
	}
*/
	/**
	 * 
	 * @param inOutputAllProducts
	 */
	protected void saveOutput(Store inStore, List inOutputAllProducts ) throws Exception
	{
		for (int i = 0; i < inOutputAllProducts.size(); i++)
		{			
			Product product = (Product) inOutputAllProducts.get(i);
			//product
			if ( product.getOrdering() == -1)
			{
				product.setOrdering(i);
			}
			product.setAvailable(true);
			inStore.getProductArchive().saveProduct( product );
			//inStore.getProductArchive().saveBlankProductDescription(product);
		}
	}
	
	//This was used to break up a description into two parts
	//this it is not used anymore
	public String parseDescription( String inString )
	{
		int start = inString.indexOf("[[");
		int end = inString.indexOf("]]");
		if ( start == -1 || end == -1 )
		{
			return inString;
		}
		else
		{
			StringBuffer out = new StringBuffer(inString.substring(0,start) );
			out.append( inString.substring(end+2,inString.length()));
			return out.toString().trim();
		}
	}
	
	public String parseKeywords( String inString )
	{
		int start = inString.indexOf("[[");
		int end = inString.indexOf("]]");
		if ( start == -1 || end == -1 )
		{
			return null;
		}
		else
		{
			return inString.substring(start+2,end).trim();
		}
	}
	public String extractId( String inName, boolean inAllowUnderstores)
	{
		inName = inName.trim();
		return PathUtilities.extractId(inName, inAllowUnderstores);
	}
	public String extractProductId(String name)
	{
		name = name.replace(" ","sp");
		name = name.replace("&","amp");
		name = name.replace("(","lp");
		name = name.replace(")","rp");
		name = name.replace(".","dot");
		name = name.replace("_","und");
		name = name.replace("+","plus");
		name = name.replace("-","min");
		name = extractId(name, false); 
		return name;
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
