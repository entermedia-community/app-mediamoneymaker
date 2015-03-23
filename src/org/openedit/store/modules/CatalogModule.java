package org.openedit.store.modules;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.links.Link;
import org.openedit.links.LinkTree;
import org.openedit.store.Category;
import org.openedit.store.Product;
import org.openedit.store.Store;
import org.openedit.store.links.CatalogTreeRenderer;
import org.openedit.store.links.CatalogWebTreeModel;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.util.RequestUtils;
import com.openedit.util.ZipUtil;
import com.openedit.webui.tree.WebTree;

public class CatalogModule extends BaseStoreModule
{
	
	private static final Log log = LogFactory.getLog(CatalogModule.class);
	private RequestUtils fieldRequestUtils;
	public RequestUtils getRequestUtils() {
		return fieldRequestUtils;
	}
	public void setRequestUtils(RequestUtils inRequestUtils) {
		fieldRequestUtils = inRequestUtils;
	}
	/**
	 * Installs a {@link WebTree} that shows the catalog tree from a specified
	 * root catalog on down.
	 * 
	 * @param inRequest  The web page request
	 */
	public WebTree getCatalogTree( WebPageRequest inRequest ) throws OpenEditException
	{
		if( inRequest.getUser() == null)
		{
			//No user
			return null;
		}
		
		Store store = getStore( inRequest );

		String treeid = inRequest.getRequestParameter("treeid");

		String name = null;
		if( inRequest.getCurrentAction() != null)
		{
			name = inRequest.getCurrentAction().getChildValue("tree-name");
		}
		if( name == null)
		{
			name = inRequest.findValue("tree-name");
		}
		if( treeid == null)
		{
			treeid = name + "_" + store.getCatalogId() + "_" + inRequest.getUserName();
		}		
		WebTree webTree = (WebTree) inRequest.getSessionValue( treeid );
		String reload = inRequest.getRequestParameter("reload");
		if( Boolean.parseBoolean(reload))
		{
			webTree = null;
		}
		if ( webTree == null )
		{
			if( name == null)
			{
				return null;
			}
			log.info("No Category in Session, creating new " + treeid);
			String root = inRequest.findValue(name + "root");
			if( root  == null )
			{
				root = inRequest.findValue("root");
			}

			Category main = store.getCategory( root );
			if ( main == null)
			{
				log.error("No such category named " + root);
				main = store.getCategoryArchive().getRootCategory();
				
			}
			CatalogWebTreeModel model = new CatalogWebTreeModel( );
			model.setCatalogId(store.getCatalogId());
			model.setRoot(main);
			
			model.setCatalogArchive(store.getCategoryArchive());
			model.setUser(inRequest.getUser());
			model.setRequestUtils(getRequestUtils());
			webTree = new WebTree(model);
			webTree.setName(name);
			webTree.setId(treeid);

			CatalogTreeRenderer renderer = new CatalogTreeRenderer( webTree );
			renderer.setFoldersLinked( true );
			String prefix = inRequest.findValue( "url-prefix" );
			prefix = inRequest.getPage().getPageSettings().replaceProperty(prefix);
			renderer.setUrlPrefix(prefix );
			String postfix = inRequest.findValue( "url-postfix" );
			renderer.setUrlPostfix(postfix );
			webTree.setTreeRenderer(renderer);
			String home = (String) inRequest.getPageValue( "home" );
			renderer.setHome(home);
			String iconHome = (String) inRequest.findValue( "iconhome" );
			renderer.setIconHome(iconHome);
			
			//expand just the top level
/*			for (Iterator iter = main.getChildren().iterator(); iter.hasNext();)
			{
				Category child = (Category) iter.next();
				renderer.expandNode(child);
			}
*/			inRequest.putSessionValue(treeid, webTree);
			inRequest.putPageValue(webTree.getName(), webTree);
		}
		else
		{
			inRequest.putPageValue(webTree.getName(), webTree);
		}
		return webTree;
	}
	
	public void reloadTree(WebPageRequest inReq) throws OpenEditException
	{
		WebTree tree = getCatalogTree(inReq);
		getStore(inReq).getCategoryArchive().clearCategories();
		if(tree != null){
			inReq.removeSessionValue(tree.getId());
		}
		getCatalogTree(inReq);
	}

	public void exportAllProducts(WebPageRequest inReq) throws OpenEditException
	{
		Store store = getStore(inReq);
		StringWriter products = new StringWriter(); //TODO: This is a memory hog
		store.getProductExport().exportAllProducts(store, products);
		
		ZipOutputStream finalZip = new ZipOutputStream(inReq.getOutputStream());
		
		try
		{
			new ZipUtil().addTozip(products.toString(),"products.xml" ,finalZip);
			inReq.getOutputStream().flush();
			StringWriter catalogs = new StringWriter();
			store.getProductExport().exportCatalogsWithProducts(store, catalogs);
			new ZipUtil().addTozip(catalogs.toString(),"categories.xml" ,finalZip);
			finalZip.close();
			inReq.setHasRedirected(true);
		}
		catch ( IOException ex )
		{
			throw new OpenEditException(ex);
		}
	}
	public void loadCrumbs(WebPageRequest inReq) throws Exception
	{
		Category category = (Category)inReq.getPageValue("category");
		if( category == null)
		{
			Product prod = (Product)inReq.getPageValue("product");
			if( prod != null)
			{
				category = prod.getDefaultCategory();
			}
		}
		if( category != null)
		{
			String name = inReq.findValue("linktreename");
			if( name != null)
			{
				String treename = inReq.findValue("tree-name");
				String root = inReq.findValue(treename + "root");
				Store store = getStore(inReq);
				Category toplevel = store.getCategory(root);
				LinkTree tree = (LinkTree)inReq.getPageValue(name);
				if( tree != null)
				{
					tree.clearCrumbs();
					for (Iterator iterator = category.listAncestorsAndSelf( 0 ).iterator(); iterator.hasNext();)
					{
						Category parent = (Category) iterator.next();
						if( toplevel != null )
						{
							if( !toplevel.hasCatalog(parent.getId()) )
							{
								continue;
							}
						}
						tree.addCrumb( store.getStoreHome() +  "/categories/" + parent.getId() + ".html",parent.getName());
					}
					tree.setSelectedLink((Link)null);
				}
			}
		}
		
	}
}