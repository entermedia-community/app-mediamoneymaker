package org.openedit.store.search;

import java.util.List;

import org.openedit.store.Category;
import org.openedit.store.Product;
import org.openedit.store.Store;

import com.openedit.OpenEditException;
import com.openedit.page.Page;
import com.openedit.users.User;

public interface ProductSecurityArchive {

	
	
	public List getAccessList(Product inProduct)throws OpenEditException;
	public List getAccessList(Page inPage);

	public void setStore(Store inStore);
	public void clearViewAccess(Page inPage);
	public void grantViewAccess(User inUser, Product inProduct) throws OpenEditException;
	public void grantViewAccess(User inUser, Category inCat) throws OpenEditException;
	public void grantViewAccess(String inUserName, Page inPage) throws OpenEditException;
}
