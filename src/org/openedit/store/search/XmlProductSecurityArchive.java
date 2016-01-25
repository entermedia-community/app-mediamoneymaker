package org.openedit.store.search;

import java.util.ArrayList;
import java.util.List;

import org.openedit.OpenEditException;
import org.openedit.page.Page;
import org.openedit.page.Permission;
import org.openedit.page.manage.PageManager;
import org.openedit.store.Category;
import org.openedit.store.Product;
import org.openedit.store.Store;
import org.openedit.users.User;
import org.openedit.util.strainer.BlankFilter;
import org.openedit.util.strainer.Filter;
import org.openedit.util.strainer.GroupFilter;
import org.openedit.util.strainer.OrFilter;
import org.openedit.util.strainer.UserFilter;

public class XmlProductSecurityArchive implements ProductSecurityArchive
{

	protected PageManager fieldPageManager;
	protected Store fieldStore;

	public Store getStore()
	{
		return fieldStore;
	}

	public void setStore(Store inStore)
	{
		fieldStore = inStore;
	}

	public List getAccessList(Page inPage)
	{
		Permission permission = inPage.getPermission("view");
		ArrayList users = new ArrayList();
		if (permission != null && permission.getRootFilter() != null)
		{
			collectUsers(users, permission.getRootFilter());
		}
		return users;
	}

	public List getAccessList(Product inProduct) throws OpenEditException
	{
		Store store = getStore();
		String path = inProduct.getSourcePath();
		//$home$cataloghome/products/${store.productPathFinder.idToPath($cell.id
		// )}.html
		Page page = getPageManager().getPage(
				store.getStoreHome() + "/products/" + path + ".html");

		List users = getAccessList(page);
		return users;
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	private void collectUsers(List add, Filter inRoot)
	{

		Filter[] filters = inRoot.getFilters();
		if (filters == null)
		{
			if (inRoot instanceof UserFilter)
			{
				String username = ((UserFilter) inRoot).getUsername();
				add.add(username);
			}
			else if (inRoot instanceof GroupFilter)
			{
				String groupid = ((GroupFilter) inRoot).getGroupId();
				add.add(groupid);
			}
			else if (inRoot instanceof BlankFilter)
			{
				add.add("blank");
			}
		}
		else
		{
			for (int i = 0; i < filters.length; i++)
			{
				Filter filter = filters[i];
				collectUsers(add, filter);
			}

		}

	}

	
	
	public void grantViewAccess(User inUser, Product inProduct)
			throws OpenEditException
	{
		String path = getStore().getProductPathFinder().idToPath(
				inProduct.getId());
		//$home$cataloghome/products/${store.productPathFinder.idToPath($cell.id
		// )}.html
		Page page = getPageManager().getPage(
				getStore().getStoreHome() + "/products/" + path + ".html");

		Permission permission = page.getPermission("view");
		if (permission == null)
		{
			permission = new Permission();
			permission.setName("view");

			page.getPageSettings().addPermission(permission);
		}
		// find and "and" filter
		Filter rootFilter = permission.getRootFilter();
		if (rootFilter == null || rootFilter.getType() != "or")
		{
			rootFilter = new OrFilter();
			permission.setRootFilter(rootFilter);
		}

		// Filter orFilter= null;
		// for (int i = 0; i < filters.length; i++) {
		// if(filters[i].getType().equals("or"));{
		// orFilter = filters[i];
		// }
		// }
		// if(orFilter == null){
		// orFilter = new OrFilter();
		// permission.getRootFilter().addFilter(orFilter);
		// }
		UserFilter filter = new UserFilter();
		filter.setUsername(inUser.getUserName());
		rootFilter.addFilter(filter);

		getPageManager().getPageSettingsManager().saveSetting(
				page.getPageSettings());
		// update the index
		getStore().saveProduct(inProduct);
	}

	public void grantViewAccess(User inUser, Category inCat)
			throws OpenEditException
	{
		//$home$cataloghome/products/${store.productPathFinder.idToPath($cell.id
		// )}.html
		Page page = getPageManager().getPage(
				getStore().getStoreHome() + "/categories/" + inCat.getId()
						+ ".html");

		Permission permission = page.getPermission("view");
		if (permission == null)
		{
			permission = new Permission();
			permission.setName("view");
			page.getPageSettings().addPermission(permission);
		}
		// find and "or" filter
		Filter rootFilter = permission.getRootFilter();
		if (rootFilter == null || rootFilter.getType() != "or")
		{
			rootFilter = new OrFilter();
			permission.setRootFilter(rootFilter);
		}

		// Filter orFilter= null;
		// for (int i = 0; i < filters.length; i++) {
		// if(filters[i].getType().equals("or"));{
		// orFilter = filters[i];
		// }
		// }
		// if(orFilter == null){
		// orFilter = new OrFilter();
		// permission.getRootFilter().addFilter(orFilter);
		// }
		UserFilter filter = new UserFilter();
		filter.setUsername(inUser.getUserName());
		rootFilter.addFilter(filter);

		getPageManager().getPageSettingsManager().saveSetting(
				page.getPageSettings());
		// update the index
		// getStore().saveProduct(inProduct);

	}

	public void clearViewAccess(Page inPage)
	{
		Permission permission = inPage.getPageSettings().getLocalPermission(
		"view");
		if (permission != null)
		{
			inPage.getPageSettings().removePermission(permission);
			getPageManager().getPageSettingsManager().saveSetting(
					inPage.getPageSettings());
		}
	}
	
	public void grantViewAccess(String inUserName, Page inPage)
			throws OpenEditException
	{
		Permission permission = inPage.getPageSettings().getLocalPermission(
				"view");
		if (permission == null)
		{
			permission = new Permission();
			permission.setName("view");
			inPage.getPageSettings().addPermission(permission);
		}
		Filter rootFilter = permission.getRootFilter();
		if (rootFilter == null)
		{
			rootFilter = new OrFilter();
			permission.setRootFilter(rootFilter);
		}

		if (!rootFilter.getType().equalsIgnoreCase("or"))
		{
			permission.setRootFilter(new OrFilter());
			permission.getRootFilter().addFilter(rootFilter);
		}

		// Filter orFilter= null;
		// for (int i = 0; i < filters.length; i++) {
		// if(filters[i].getType().equals("or"));{
		// orFilter = filters[i];
		// }
		// }
		// if(orFilter == null){
		// orFilter = new OrFilter();
		// permission.getRootFilter().addFilter(orFilter);
		// }
		UserFilter filter = new UserFilter();
		filter.setUsername(inUserName);
		rootFilter.addFilter(filter);
		getPageManager().getPageSettingsManager().saveSetting(
				inPage.getPageSettings());
		// update the index
		// getStore().saveProduct(inProduct);

	}

}
