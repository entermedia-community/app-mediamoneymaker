/*
 * Created on Mar 8, 2005
 */
package com.openedit.store.orders;

import org.openedit.WebPageRequest;
import org.openedit.store.Cart;
import org.openedit.store.CartItem;
import org.openedit.store.Store;
import org.openedit.store.StoreTestCase;
import org.openedit.store.modules.CartModule;
import org.openedit.store.orders.Order;

/**
 * @author cburkey
 *
 */
public class OrderExportTest extends StoreTestCase
{
	
	/**
	 * @param inArg0
	 */
	public OrderExportTest(String inArg0)
	{
		super(inArg0);
	}
	public void testConvertion() throws Exception
	{
		/*
			CurrencyConvert con = new CurrencyConvert();
			Fraction result = con.getMultiplier("euro","usa");
			assertTrue( result.doubleValue() > 1.2);
*/
	}
	public void testElectronicOrder() throws Exception
	{
		CartModule cartModule = (CartModule)getStaticFixture().getModuleManager().getModule("CartModule");

		WebPageRequest context = getStaticFixture().createPageRequest();//"/store/index.html");
		Store store = cartModule.getStore(context);

		context.setRequestParameter( "productid", "1" );
		cartModule.updateCart( context );

		Cart cart = cartModule.getCart(context);
		cart.setCustomer(createCustomer());
		
		cartModule.processOrder(context);

		//make sure it was marked as classified
		Order order = (Order)context.getPageValue("order");
		assertNotNull(order);
		
		CartItem item = (CartItem)order.getItems().get(0);
		assertEquals( "electronic-delivery", item.getProduct().getShippingMethodId() );

		//assertEquals("/store/electronic/electronic-delivery.html",context.getPageValue("redirect"));
	}
}
