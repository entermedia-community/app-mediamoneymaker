package org.openedit.store.modules;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.links.Link;
import org.openedit.links.LinkTree;
import org.openedit.store.Product;
import org.openedit.store.Store;
import org.openedit.store.search.ProductSearcher;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.SearchQuery;
import com.openedit.page.Page;
import com.openedit.util.PathUtilities;

public class FolderModule extends BaseStoreModule
{
	private static final Log log = LogFactory.getLog(FolderModule.class);

	public void loadFolderDetails(WebPageRequest inReq)
			throws OpenEditException
	{
		String sourcePath = getSourcePath(inReq);
		String catalogid = getStore(inReq).getCatalogId();
		String folderPath = "/" + catalogid + "/products/" + sourcePath;
		Page folder = getPageManager().getPage(folderPath);
		inReq.putPageValue("properties", folder.getPageSettings()
				.getProperties());
		String foldername = PathUtilities.extractDirectoryName(sourcePath);
		inReq.putPageValue("foldername", foldername);
		inReq.putPageValue("sourcepath", sourcePath);
	}

	public void loadFolderCrumbs(WebPageRequest inReq)
	{
		Store store = getStore(inReq);
		String sourcePath = getSourcePath(inReq);
		sourcePath = PathUtilities.extractDirectoryPath(sourcePath);
		

		String name = inReq.findValue("linktreename");
		if (name != null)
		{
			LinkTree tree = (LinkTree) inReq.getPageValue(name);
			if (tree != null)
			{
				tree.clearCrumbs();

				while (sourcePath.length() > 0)
				{
					tree.prependCrumb(
							store.getStoreHome() + "/folders/index.html?sourcepath=" + sourcePath + "/",
							PathUtilities.extractPageName(sourcePath));
					sourcePath = PathUtilities.extractDirectoryPath(sourcePath);
				}
				tree.prependCrumb(store.getStoreHome() + "/folders/index.html", "folders");

				tree.setSelectedLink((Link) null);
			}
		}
	}

	public void searchFolder(WebPageRequest inReq) throws Exception
	{
		Store store = getStore(inReq);
		String catalogid = store.getCatalogId();
		String sourcePath = getSourcePath(inReq);
		
		if ( !sourcePath.endsWith("/") ) // It's a file
		{
			sourcePath = PathUtilities.extractDirectoryPath(sourcePath)+"/";
		}

		inReq.removeSessionValue("crumb");

		ProductSearcher productSearcher = store.getProductSearcher();
		SearchQuery search = productSearcher.createSearchQuery();
		search.setAndTogether(true);
		search.addMatches("foldersourcepath","\"" + sourcePath + "\"");

		String filter = productSearcher.createFilter(inReq, false);
		search.addQuery("filter", filter);

		search.setSortBy("typeUp,nameUp");
		productSearcher.cachedSearch(inReq, search);
	}

	/**
	 *** Get the sourcePath of a folder when visiting /catalogid/folders/$sourcepath/index.html
	 * Update: Gets the sourcepath from a request parameter. Defaults to "/".
	 * @param inReq
	 * @return
	 */
	public String getSourcePath(WebPageRequest inReq)
	{
		String sourcePath = inReq.findValue("sourcepath");
		if (sourcePath == null)
		{
			sourcePath = "/";
		}
		return sourcePath;
		
//		Store store = getStore(inReq);
//
//		String catalogid = store.getCatalogId();
//		String path = PathUtilities.extractDirectoryPath(inReq.getPath()) + "/";
//		String root = inReq.findValue("rootfolder");
//		if( root == null )
//		{
//			root = "/" + catalogid + "/folders/";
//		}
//
//		String sourcePath;
//		
//		if (root.length() >= path.length())
//		{
//			sourcePath = "/";
//		}
//		else
//		{
//			sourcePath = path.substring(root.length());
//		}
//		return sourcePath;

	}

	public void indexFolder(WebPageRequest inReq)
	{
		Store store = getStore(inReq);
		String sourcePath = inReq.getRequestParameter("sourcepath");
		if (sourcePath == null)
		{
			String path = (String)inReq.getPageValue("folderpath");
			String base = store.getStoreHome() + "/products/";
			sourcePath = path.substring(base.length());
			
		}
		
		Product product = store.getProductArchive().getProductBySourcePath(sourcePath);
		
		if (product == null)
		{
			product = store.createProduct(sourcePath);
		}
		
		store.getProductSearcher().updateIndex(product);
	}
}
