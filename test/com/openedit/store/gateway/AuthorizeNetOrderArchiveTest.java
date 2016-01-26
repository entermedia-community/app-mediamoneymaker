/*
 * Created on Oct 12, 2004
 */
package com.openedit.store.gateway;

import org.openedit.WebPageRequest;
import org.openedit.store.CreditCardType;
import org.openedit.store.CreditPaymentMethod;
import org.openedit.store.StoreTestCase;
import org.openedit.store.gateway.AuthorizeNetOrderProcessor;
import org.openedit.store.orders.Order;

/**
 * @author Matthew Avery, mavery@einnovation.com
 * @author Dennis Brown
 */
public class AuthorizeNetOrderArchiveTest extends StoreTestCase
{

	public AuthorizeNetOrderArchiveTest( String name )
	{
		super( name );
	}

	public void testOrderArchive() throws Exception
	{
		AuthorizeNetOrderProcessor archiver = new AuthorizeNetOrderProcessor();
		WebPageRequest context = getFixture().createPageRequest();
		Order order = createOrder();
		archiver.exportNewOrder( context, getStore(), order );
		assertFalse( order.getOrderStatus().isOk() );
	}
	public void XtestRealOrder() throws Exception
	{
		String cburkeysrealcreditcardnumber = "443220165027XXXX";
		
		CreditPaymentMethod paymentMethod = new CreditPaymentMethod();
		CreditCardType type = new CreditCardType();
		type.setName( "Visa" );
		paymentMethod.setCreditCardType( type );
		paymentMethod.setCardNumber( cburkeysrealcreditcardnumber );
		paymentMethod.setExpirationMonth( 6 );
		paymentMethod.setExpirationYear( 2006 );

		AuthorizeNetOrderProcessor archiver = new AuthorizeNetOrderProcessor();
		WebPageRequest context = getFixture().createPageRequest();
		Order order = createOrder();
		order.setPaymentMethod(paymentMethod);
		archiver.exportNewOrder( context, getStore(), order );
		if ( ! order.getOrderState().isOk() )
		{
			throw  new Exception(order.getOrderState().getDescription());
		}

		
	}
}
