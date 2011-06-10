/*
 * Created on Oct 5, 2004
 */
package org.openedit.store.orders;

import java.util.List;
import java.util.Map;

import org.openedit.store.Store;
import org.openedit.store.StoreException;

import com.openedit.WebPageRequest;

/**
 * An interface implemented by classes that can write orders to and read orders
 * from persistent storage.
 * 
 * @author cburkey
 */
public interface OrderArchive
{
	
	// result of approval, returned as string for display to user
	public static final String AUTHORIZATION_RESULT_STRING_KEY = "AuthorizationResultString";
	// result of approval, returned as Boolean
	public static final String AUTHORIZATION_RESULT_KEY = "AuthorizationResult";

	public void archiveOrderData( Store inStore ) throws StoreException;
	
	public void captureOrder( WebPageRequest inContext, Store inStore,
		Order inOrder ) throws StoreException;
	
	public void changeOrderStatus( OrderState inStatus, Store inStore,
			Order inOrder ) throws StoreException;
	
	public Map getOrderStates(Store inStore);

	public List listAllOrderIds(Store inStore);

	public SubmittedOrder loadSubmittedOrder(Store inStore, String inUserName, String inId) throws StoreException;
	
	public void saveOrder(Store inStore, Order inOrder) throws StoreException;
}