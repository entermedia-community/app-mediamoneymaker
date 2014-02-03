package com.openedit.store.gateway;

import java.util.Date;

import org.openedit.money.Money;
import org.openedit.store.Cart;
import org.openedit.store.CartItem;
import org.openedit.store.CreditPaymentMethod;
import org.openedit.store.InventoryItem;
import org.openedit.store.Product;
import org.openedit.store.Store;
import org.openedit.store.StoreTestCase;
import org.openedit.store.customer.Customer;
import org.openedit.store.gateway.MonerisOrderProcessor;
import org.openedit.store.modules.CartModule;
import org.openedit.store.orders.Order;
import org.openedit.store.orders.OrderState;
import org.openedit.store.orders.Refund;
import org.openedit.store.orders.SubmittedOrder;

import com.openedit.WebPageRequest;

public class MonerisTest extends StoreTestCase {
	
	public void testPlaceOrderThenRefund() throws Exception {
		MonerisOrderProcessor processor = (MonerisOrderProcessor) getFixture().getModuleManager().getBean("monerisOrderProcessor");
		Money money = new Money("10.00");
		Store store = getStore();
		if (store.get("gateway") == null || !store.get("gateway").equals("moneris")){
			store.setProperty("gateway","moneris");
			store.setProperty("moneris_host","esqa.moneris.com");
			store.setProperty("moneris_store_id","store5");
			store.setProperty("moneris_api_token","yesguy");
		}
		CartModule cartModule = (CartModule) getFixture().getModuleManager().getModule("CartModule");
		WebPageRequest context = getFixture().createPageRequest();
		Cart cart = cartModule.getCart(context);
		cart.setCustomer(new Customer(context.getUser()));
		Product product = new Product();
		product.setId("1234");
		product.addInventoryItem(new InventoryItem("sku_1234_5"));
		CartItem item = new CartItem();
		item.setProduct(product);
		item.setQuantity(1);
		cart.addItem(item);
		
		Order order = store.getOrderGenerator().createNewOrder(store, cart);
		order.setProperty("notes", "This is a note");
		order.setTotalPrice(money);
		
		CreditPaymentMethod cc = new CreditPaymentMethod();
		cc.setCardNumber("4242424242424242");
		cc.setExpirationMonth(12);
		cc.setExpirationYear(2014);
		cc.setCardVerificationCode("099");//for testing cvd
		order.setPaymentMethod(cc);
		store.saveOrder(order);
		
		processor.processNewOrder(context, store, order);
		
		OrderState state = order.getOrderStatus();
		assertNotNull(state);
		assertTrue(state.isOk());
		assertNotNull(state.getDescription());
		
		testRefund(order);
	}
	
	protected void testRefund(Order inOrder) throws Exception {
		MonerisOrderProcessor processor = (MonerisOrderProcessor) getFixture().getModuleManager().getBean("monerisOrderProcessor");
		Store store = getStore();
		WebPageRequest context = getFixture().createPageRequest();
		Refund refund = new Refund();
		refund.setTotalAmount(inOrder.getTotalPrice());
		processor.refundOrder(context, store, inOrder, refund);
		assertTrue(refund.isSuccess());
		assertNotNull(refund.getTransactionId());
		assertNotNull(refund.getAuthorizationCode());
	}

}
