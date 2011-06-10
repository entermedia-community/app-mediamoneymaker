/*
 * Created on Oct 12, 2004
 */
package com.openedit.store.gateway;

import org.openedit.store.CreditCardType;
import org.openedit.store.CreditPaymentMethod;
import org.openedit.store.StoreTestCase;
import org.openedit.store.gateway.EchoOrderProcessor;
import org.openedit.store.orders.Order;

import com.openedit.WebPageRequest;

/**
 * @author Dave Connerth
 */
public class EchoOrderArchiveTest extends StoreTestCase {
	public static final String ECHO_TEST_CARD_NUMBER = "4005550000000019";

	/**
	 * @param name
	 */
	public EchoOrderArchiveTest(String name) {
		super(name);
	}

	/**
	 * @throws Exception
	 */
	public void testEchoPaymentArchiveWithBadOrder() throws Exception {
		EchoOrderProcessor archiver = new EchoOrderProcessor();
		WebPageRequest context = getFixture().createPageRequest();
		Order order = createOrder();
		archiver.exportNewOrder(context, getStore(), order);
		assertFalse(order.getOrderState().isOk());
	}

	/**
	 * @throws Exception
	 */
	public void testEchoPaymentArchive() throws Exception {
		CreditPaymentMethod paymentMethod = new CreditPaymentMethod();
		CreditCardType type = new CreditCardType();
		type.setName("Visa");
		paymentMethod.setCreditCardType(type);
		paymentMethod.setCardNumber(ECHO_TEST_CARD_NUMBER);
		paymentMethod.setExpirationMonth(6);
		paymentMethod.setExpirationYear(2006);
		EchoOrderProcessor archiver = new EchoOrderProcessor();
		archiver.setAddRandomAmountToTotal(true); //this prevents duplicate trans errors
		WebPageRequest context = getFixture().createPageRequest();
		Order order = createOrder();
		order.setPaymentMethod(paymentMethod);
		archiver.exportNewOrder(context, getStore(), order);
		if (!order.getOrderState().isOk()) {
			throw new Exception(order.getOrderState().getDescription());
		}
	}
}