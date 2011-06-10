/*
 * Created on Jul 27, 2006
 */
package org.openedit.store.orders;

import java.util.List;
import java.util.Map;

import org.openedit.store.BaseArchive;
import org.openedit.store.Store;
import org.openedit.store.StoreException;

import com.openedit.WebPageRequest;

public abstract class BaseOrderProcessor extends BaseArchive implements OrderProcessor
{

	public void archiveOrderData(Store inStore) throws StoreException
	{
	}

	public void captureOrder(WebPageRequest inContext, Store inStore, Order inOrder) throws StoreException
	{
	}

	public void changeOrderStatus(OrderState inStatus, Store inStore, Order inOrder) throws StoreException
	{
	}

	public void exportNewOrder(WebPageRequest inContext, Store inStore, Order inOrder) throws StoreException
	{
	}

	public Map getOrderStates(Store inStore)
	{
		return null;
	}

	public List listAllOrderIds(Store inStore) throws StoreException
	{
		return null;
	}
	public SubmittedOrder loadSubmittedOrder(Store inStore, String inUserName,  String inId) throws StoreException
	{
		// TODO Auto-generated method stub
		return null;
	}
	public void saveOrder(Store inStore, Order inOrder) throws StoreException
	{
		
	}
}
