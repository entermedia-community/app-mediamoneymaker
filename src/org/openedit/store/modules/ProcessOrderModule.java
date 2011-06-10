/* Generated by Together */

package org.openedit.store.modules;

import org.openedit.event.WebEventListener;
import org.openedit.store.Product;
import org.openedit.store.Store;
import org.openedit.store.StoreException;
import org.openedit.store.process.ElectronicOrderManager;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.users.User;

public class ProcessOrderModule extends BaseStoreModule {

	protected ElectronicOrderManager fieldElectronicOrderManagement;
	protected WebEventListener fieldWebEventListener;

	

	public WebEventListener getWebEventListener()
	{
		return fieldWebEventListener;
	}




	public void setWebEventListener(WebEventListener inWebEventListener)
	{
		fieldWebEventListener = inWebEventListener;
	}




	public void updateUserAccess(WebPageRequest inRequest) throws OpenEditException, StoreException{
		Store store = getStore(inRequest);
		
	String productid = inRequest.getRequestParameter("productid");
	Product product = store.getProduct(productid);
	String[] users = inRequest.getRequestParameters("user");
	String permission = inRequest.getRequestParameter("permission");
	
	for (int i = 0; i < users.length; i++) {
		User user = getUserManager().getUser(users[i]);
		//getSecurityArchive().addUserPermission(user, store, product,permission);
	
	}
		
	}




	public ElectronicOrderManager getElectronicOrderManagement() {
		return fieldElectronicOrderManagement;
	}




	public void setElectronicOrderManagement(ElectronicOrderManager inElectronicOrderManagement) {
		fieldElectronicOrderManagement = inElectronicOrderManagement;
	}

}
