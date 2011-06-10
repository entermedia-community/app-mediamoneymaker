package org.openedit.store.edit;

import org.openedit.store.Product;

import com.openedit.util.PathUtilities;

public class ProductSourcePathCreator
{
	public String createSourcePath(Product inProduct)
	{
		String sourcepath = inProduct.get("originalpath");
		if( sourcepath == null)
		{
			return inProduct.getSourcePath();
		}
		if( sourcepath.startsWith("\\\\"))
		{
			//cut off the share name
			int nextslash = sourcepath.indexOf('\\',2);
			sourcepath = sourcepath.substring(nextslash + 1);
		}
		else if (sourcepath.length() > 1 && sourcepath.charAt(1) == ':')
		{
			sourcepath = sourcepath.substring(3);
		}
		else if (sourcepath.startsWith("/"))
		{
			sourcepath = sourcepath.substring(1);
		}
		sourcepath = sourcepath.replace('\\','/');

		//We should not have done this, redundant:
		String cumuluscatname = inProduct.get("externalid");
		if (cumuluscatname != null && cumuluscatname.length() > 0)
		{
			sourcepath = cumuluscatname + "/" + sourcepath;
		}
		
		String ext = PathUtilities.extractPageType(sourcepath);
		if( ext == null)
		{
			ext = inProduct.get("fileformat");
			sourcepath = sourcepath + "." + ext;
		}
		
		String  pagenumber = inProduct.get("pagenumber");
		if( pagenumber != null)
		{
			sourcepath = PathUtilities.extractPagePath(sourcepath);
			sourcepath = sourcepath + "_page" + pagenumber + "." + ext;
		}
		//no ext
		
		return sourcepath;
	}
}
