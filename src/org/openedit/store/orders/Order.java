/*
 * Created on Oct 7, 2004
 */
package org.openedit.store.orders;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.ListOrderedMap;
import org.openedit.data.BaseData;
import org.openedit.money.Money;
import org.openedit.store.Cart;
import org.openedit.store.CartItem;
import org.openedit.store.CreditPaymentMethod;
import org.openedit.store.PaymentMethod;
import org.openedit.store.ShippingMethod;
import org.openedit.store.customer.Address;
import org.openedit.store.customer.Customer;
import org.openedit.util.DateStorageUtil;

/**
 * An order for a number of products contained in a shopping cart at a certain
 * time.
 * 
 * @author Eric Galluzzo, egalluzzo@einnovation.com
 */
public class Order extends BaseData implements Comparable 
{
	public final static String ACCEPTED = "accepted";
	public final static String AUTHORIZED = "authorized";
	public final static String CAPTURED = "captured";
	public final static String COMPLETED = "completed";
	public final static String REJECTED = "rejected";
		
	protected String fieldId;
	protected Customer fieldCustomer;
	protected List fieldItems;
	protected List fieldAdjustments;

	protected ShippingMethod fieldShippingMethod;
	protected Money fieldTotalShipping;
	
	protected Map fieldTaxes;
	
	
	protected Money fieldTax;
	protected Money fieldSubTotal;
	protected Money fieldTotalPrice;

	protected PaymentMethod fieldPaymentMethod;
	protected Date fieldDate;
	protected OrderState fieldOrderState;
	protected Map fieldProperties;
	
	protected Cart fieldCart;
	protected Address fieldShippingAddress;
	protected Address fieldBillingAddress;
	
	
	
	
	
	public Address getShippingAddress() {
		return fieldShippingAddress;
	}
	public void setShippingAddress(Address inShippingAddress) {
		fieldShippingAddress = inShippingAddress;
	}
	public Address getBillingAddress() {
		return fieldBillingAddress;
	}
	public void setBillingAddress(Address inBillingAddress) {
		fieldBillingAddress = inBillingAddress;
	}
	public Cart getCart()
	{
	if (fieldCart == null)
	{
		fieldCart = new Cart();
		for (Iterator iterator = getItems().iterator(); iterator.hasNext();)
		{
			CartItem item = (CartItem) iterator.next();
			fieldCart.addItem(item);
			
		}
		
	}

	return fieldCart;
	}
	public void setCart(Cart inCart)
	{
		fieldCart = inCart;
	}
	public List getAdjustments() {
		return fieldAdjustments;
	}
	public void setAdjustments(List inAdjustments) {
		fieldAdjustments = inAdjustments;
	}
	
	
	public Money getTotalShipping()
	{
		return fieldTotalShipping;
	}
	public void setTotalShipping(Money inTotalShipping)
	{
		fieldTotalShipping = inTotalShipping;
	}
	public ShippingMethod getShippingMethod()
	{
		return fieldShippingMethod;
	}
	public void setShippingMethod(ShippingMethod inShippingMethod)
	{
		fieldShippingMethod = inShippingMethod;
	}
	public Money getSubTotal()
	{
		return fieldSubTotal;
	}
	public void setSubTotal(Money inSubtotal)
	{
		fieldSubTotal = inSubtotal;
	}
	public Money getTax()
	{
		return fieldTax;
	}
	public void setTotalTax(Money inTax)
	{
		fieldTax = inTax;
	}
	
	/**
	 * Creates a new order with no ID and today's date.
	 * 
	 * <p>FIXME: Should we auto-generate an ID?
	 */
	public Order()
	{
		fieldDate = new Date();
	}

	/**
	 * Creates a new order with the given ID and date.
	 * 
	 * @param inId    The order ID
	 * @param inDate  The order date
	 */
	public Order( String inId, Date inDate )
	{
		fieldId = inId;
		fieldDate = inDate;
	}
	/**
	 * Returns the date and time at which the order was made.
	 * 
	 * @return  The order date
	 */
	public Date getDate()
	{
		return fieldDate;
	}

	/**
	 * Sets the date and time at which the order was made.
	 * 
	 * @param inDate  The order date
	 */
	public void setDate( Date inDate )
	{
		fieldDate = inDate;
	}

	/**
	 * Returns the order ID.
	 * 
	 * @return  The order ID
	 */
	public String getId()
	{
		return fieldId;
	}
	public String getOrderNumber()
	{
		return getId();
	}

	/**
	 * Sets the order ID.  This should not normally be called.  Instead, use a
	 * constructor that accepts an ID.
	 * 
	 * @param inId  The order ID
	 */
	public void setId( String inId )
	{
		fieldId = inId;
	}

	/**
	 * Returns the method that the customer used to pay for this order -- e.g. a
	 * Visa credit card with number 4111-1111-1111-1111.
	 * 
	 * @return  The payment method
	 */
	public PaymentMethod getPaymentMethod()
	{
		if ( fieldPaymentMethod == null )
		{
			fieldPaymentMethod = new CreditPaymentMethod();
		}
		return fieldPaymentMethod;
	}

	/**
	 * Sets the method used to pay for this order.
	 * 
	 * @param inPaymentMethod  The payment method
	 */
	public void setPaymentMethod( PaymentMethod inPaymentMethod )
	{
		fieldPaymentMethod = inPaymentMethod;
	}

	public String toString()
	{
		return "Order " + getId() + " on " + getDate() + ":\n" 			+ getPaymentMethod();
	}
	
	/**
	 * @deprecated Use getOrderStatus instead.
	 */
	public OrderState getOrderState() //TODO: Rename to getOrderStatus
	{
		return getOrderStatus();
	}
	public OrderState getOrderStatus()
	{
		return fieldOrderState;
	}
	public void setOrderState(OrderState inOrderState)
	{
		fieldOrderState = inOrderState;
	}
	public Customer getCustomer()
	{
		return fieldCustomer;
	}
	public void setCustomer(Customer inCustomer)
	{
		fieldCustomer = inCustomer;
	}
	public List getItems()
	{
		return fieldItems;
	}
	public void setItems(List inItems)
	{
		fieldItems = inItems;
	}
	public void addItem(CartItem inItem)
	{
		if( fieldItems == null)
		{
			fieldItems = new ArrayList();
		}
		getItems().add( inItem);
	}
	
	public CartItem getItem(String inSku){
		for (Iterator iterator = getItems().iterator(); iterator.hasNext();)
		{
			CartItem item = (CartItem) iterator.next();
			if(item.getSku().equals(inSku)){
				return item;
			}
		}
		return null;
	}
	
	
	public Money getTotalPrice()
	{
		return fieldTotalPrice;
	}
	public void setTotalPrice(Money inTotalPrice)
	{
		fieldTotalPrice = inTotalPrice;
	}
	
	public int compareTo(Object inOrder)
	{
		SubmittedOrder order = (SubmittedOrder)inOrder;
		return order.getOrderNumber().compareTo(getOrderNumber());
	}
	public int getNumItems()
	{
		int numItems = 0;
		for (Iterator it = getItems().iterator(); it.hasNext();)
		{
			CartItem item = (CartItem)it.next();
			numItems += item.getQuantity();
		}
		return numItems;
	}
	public int sumColumn(String colName)
	{
		int sum = 0;
		for(Iterator it = getItems().iterator(); it.hasNext();)
		{
			CartItem item = (CartItem)it.next();
			String stringVal = item.get(colName);
			if( stringVal != null && stringVal.length() != 0)
			{
				int val = Integer.parseInt(item.get(colName));
				sum += val;
			}
		}
		return sum;
	}
	public String get(String inId)
	{
		if("id".equals(inId)){
			return getId();
		}
		if("orderdate".equals(inId)){
			return DateStorageUtil.getStorageUtil().formatForStorage(getDate());
		}
		if("customer".equals(inId)){
			return getCustomer().getId();
		}
		if("orderstatus".equals(inId)){
			return getOrderStatus().getId();
		}
			
		return (String)getProperties().get(inId);
	}
	public Map getProperties()
	{
		if( fieldProperties == null)
		{
			fieldProperties = ListOrderedMap.decorate(new HashMap());
		}
		return fieldProperties;
	}
	public void setProperty(String inKey, String inVal)
	{
		getProperties().put(inKey, inVal);	
	}
	public Map getTaxes() {
		return fieldTaxes;
	}
	public void setTaxes(Map inTaxes) {
		fieldTaxes = inTaxes;
	}
	

}