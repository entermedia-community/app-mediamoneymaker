/*
 * Created on Jan 17, 2005
 */
package org.openedit.store.orders;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Properties;

import org.openedit.store.Cart;
import org.openedit.store.Store;
import org.openedit.store.StoreException;

/**
 * @author dbrown
 *
 */
public class BaseOrderGenerator implements OrderGenerator
{
	public BaseOrderGenerator()
	{
	}

	public Order createNewOrder(Store inStore, Cart inCart) throws StoreException
	{
		Order order = new Order();
		order.setShippingMethod(inCart.getShippingMethod());
		order.setTotalPrice(inCart.getTotalPrice());
		order.setSubTotal(inCart.getSubTotal());
		order.setTotalTax(inCart.getTotalTax());
		order.setTaxes(inCart.getTaxes());
		order.setTotalShipping(inCart.getTotalShipping());
		order.setItems(new ArrayList( inCart.getItems() ) );
		order.setAdjustments(inCart.getAdjustments());
		order.setCustomer(inCart.getCustomer());
		order.setPaymentMethod(inCart.getCustomer().getPaymentMethod());
		order.setShippingMethod(inCart.getShippingMethod());
		order.setId(nextOrderNumber(inStore));
		OrderState state = inStore.getOrderState(Order.ACCEPTED);
		state.setOk(true);
		if( state == null)
		{
			throw new StoreException("No such order state " + Order.ACCEPTED);
		}
		order.setOrderState(state);
		return order;
	}

	//	 Should be "WEB#########"
	public String nextOrderNumber( Store inStore ) throws StoreException
	{
		File orderProperties = new File( inStore.getStoreDirectory(), "orders/order.properties" );
		orderProperties.getParentFile().mkdirs();
		Properties props = new Properties();
		try
		{
			if ( orderProperties.exists() )
			{
				props.load( new FileInputStream( orderProperties ) );
			}
			String countString = props.getProperty("count");
			int count = 0;
			if ( countString != null )
			{
				count = Integer.valueOf( countString ).intValue();
			}
			count++;
			countString = String.valueOf(count);
			props.setProperty( "count", countString );
			props.store( new FileOutputStream( orderProperties ), "Order properties count" );
			//no more than 12 total characters
			StringBuffer id = new StringBuffer("WEB");

			int zeros = 10 - 3 - countString.length();
			for (int i = 0; i < zeros; i++)
			{
				id.append( "0");
			}
			id.append(countString);
			return id.toString();
		} catch ( Exception ex )
		{
			if ( ex instanceof StoreException)
			{
				throw (StoreException)ex;
			}
			else
			{
				throw new StoreException(ex);
			}
		}
	}
}
