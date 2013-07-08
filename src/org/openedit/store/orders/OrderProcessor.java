/*
 * Created on Oct 5, 2004
 */
package org.openedit.store.orders;

import org.openedit.store.Store;
import org.openedit.store.StoreException;

import com.openedit.WebPageRequest;


public interface OrderProcessor
{
	
	// result of approval, returned as string for display to user
	public static final String AUTHORIZATION_RESULT_STRING_KEY = "AuthorizationResultString";
	// result of approval, returned as Boolean
	public static final String AUTHORIZATION_RESULT_KEY = "AuthorizationResult";

	public void processNewOrder(WebPageRequest inContext, Store inStore, Order inOrder) throws StoreException;
	
	public void refundOrder(WebPageRequest inContext, Store inStore,  Order inOrder, Refund refund)	throws StoreException;
}