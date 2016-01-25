package org.openedit.store.search;

import org.openedit.repository.ContentItem;
import org.openedit.util.PathProcessor;
import org.openedit.util.PathUtilities;

public abstract class ProductProcessor extends PathProcessor
{

	protected String makeSourcePath(ContentItem inItem)
	{
		String path = inItem.getPath();
		path = path.substring(getRootPath().length());
		if (path.endsWith(".xconf"))
		{
			path = path.substring(0, path.length() - ".xconf".length());
		}
		path = path.replace('\\', '/');
		return path;
	}
	
	public boolean acceptFile(ContentItem inFile)
	{
		String path = inFile.getPath();
		String name = PathUtilities.extractFileName(path);
		if (name.endsWith(".xconf"))
		{
			if (name.equals("_site.xconf") || name.equals("_default.xconf")
					|| name.equals(".xconf"))
			{
				return false;
			}
			return true;
		}
		return false;
	}
	
	public boolean acceptDir(ContentItem inDir)
	{
		String sourcePath = makeSourcePath(inDir);
		return (super.acceptDir(inDir) 
				&& !sourcePath.equals("images")
				&& !sourcePath.equals("search"));
	}

}
