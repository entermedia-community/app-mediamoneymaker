/*
 * Created on Jan 17, 2005
 */
package org.openedit.store.orders;

import org.openedit.store.Cart;
import org.openedit.store.Store;
import org.openedit.store.StoreException;

/**
 * @author dbrown
 *
 */
public interface OrderGenerator
{
	public Order createNewOrder(Store inStore, Cart inCart) throws StoreException;

	public String nextOrderNumber( Store inStore ) throws StoreException;
}
