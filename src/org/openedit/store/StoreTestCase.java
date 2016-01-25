/*
 * Created on Sep 23, 2004
 */
package org.openedit.store;

import java.util.ArrayList;

import org.openedit.BaseTestCase;
import org.openedit.TestFixture;
import org.openedit.money.Fraction;
import org.openedit.money.Money;
import org.openedit.store.customer.Address;
import org.openedit.store.customer.Customer;
import org.openedit.store.orders.Order;
import org.openedit.store.shipping.FixedCostShippingMethod;

/**
 * @author Matthew Avery, mavery@einnovation.com
 */
public abstract class StoreTestCase extends BaseTestCase
{
	protected Store fieldStore;
	protected static TestFixture fieldStaticFixture;
	
	public TestFixture getFixture()
	{
		if( fieldStaticFixture == null )
		{
			fieldStaticFixture = super.getFixture();
		}
		return fieldStaticFixture;
	}
	public StoreTestCase()
	{
		// TODO Auto-generated constructor stub
	}
	
	public StoreTestCase( String arg0 )
	{
		super( arg0 );
	}
	protected Store getStore() throws Exception
	{
		if ( fieldStore == null)
		{
			StoreArchive reader = (StoreArchive)getStaticFixture().getModuleManager().getBean("storeArchive");
			fieldStore = reader.getStore("store");
		}
		return fieldStore;
	}
	protected Price createPrice( double inValue )
	{
		Price price = new Price();
		price.setRetailPrice( new Money( inValue ) );
		return price;
	}
	protected CartItem createCheapToyCartItem() throws StoreException
	{
		Product cheapToyProduct = new Product( "cheap toy" );
		final double CHEAP_TOY_PRICE = 1.99;
		cheapToyProduct.setName("Cheap Toy");
		cheapToyProduct.setId("1002");
		cheapToyProduct.setPriceSupport(new PriceSupport());
		cheapToyProduct.addTierPrice(1, createPrice( CHEAP_TOY_PRICE ) );
	
		InventoryItem item = new InventoryItem();
		item.setQuantityInStock(10000);
		item.setSku("5432145dde");
		
		cheapToyProduct.addInventoryItem(item);
		CartItem cheapToy = new CartItem();
		cheapToy.setQuantity(5);
		cheapToy.setInventoryItem(item);
		return cheapToy;
	}
	protected CartItem createRecurringCartItem(String name, double price) throws StoreException{
		Product cheapToyProduct = new Product( name + " (price="+price+")");
		cheapToyProduct.setProperty("recurring", "true");
		cheapToyProduct.setName(name+ " (price="+price+")");
		cheapToyProduct.setId("1003");
		cheapToyProduct.setPriceSupport(new PriceSupport());
		cheapToyProduct.addTierPrice(1, createPrice( price ) );
	
		InventoryItem item = new InventoryItem();
		item.setQuantityInStock(10000);
		item.setSku("40988alkjdi");
		
		cheapToyProduct.addInventoryItem(item);
		CartItem cheapToy = new CartItem();
		cheapToy.setQuantity(1);
		cheapToy.setInventoryItem(item);
		return cheapToy;
	}
	protected Customer createCustomer()
	{
		Customer customer = new Customer();
		Address address = customer.getBillingAddress();
		address.setAddress1( "5052 Gray Rd");
		address.setCity("Cincinnati");
		address.setState("OH");
		customer.setFirstName("Christopher");
		customer.setLastName( "Burkey");
		customer.setEmail( "cburkey@openedit.org");
		address.setCountry("USA");
		customer.setPhone1("513-542-3401");
		customer.setUserName( "cburkey" );
		address.setZipCode("45232");
		TaxRate rate = new TaxRate();
		rate.setName("GST");
		rate.setFraction(new Fraction( 0.07 ));
		ArrayList rates = new ArrayList();
		rates.add(rate);
		customer.setTaxRates( rates );
		customer.setPassword("non-blank-password");
		return customer;
	}
	protected ShippingMethod createShippingMethod()
	{
		FixedCostShippingMethod method = new FixedCostShippingMethod();
		method.setCost( new Money(6.50) );
		method.setDescription( "UPS ground" );
		method.setId("UPS1");
		return method;
	}
	protected PaymentMethod createPaymentMethod()
	{
		CreditPaymentMethod paymentMethod = new CreditPaymentMethod();
		CreditCardType type = new CreditCardType();
		type.setName( "Mastercard" );
		paymentMethod.setCreditCardType( type );
		paymentMethod.setCardNumber( "5555444455554444" );
		paymentMethod.setExpirationMonth( 12 );
		paymentMethod.setExpirationYear( 2004 );
		return paymentMethod;
	}
	protected Cart createCart() throws StoreException
	{
		Cart cart = new Cart();
		
		cart.addItem( createCheapToyCartItem() );
		cart.setCustomer( createCustomer() );
		cart.setShippingMethod( createShippingMethod() );
		return cart;
	}
	protected Order createOrder() throws Exception
	{
		Order order = getStore().getOrderGenerator().createNewOrder(getStore(),createCart());
		order.setId( "TESTORDER1" );
		order.setPaymentMethod( createPaymentMethod() );
		return order;
	}

}
