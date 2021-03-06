/*
 * Created on Oct 7, 2004
 */
package org.openedit.store.orders;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.data.BaseData;
import org.openedit.money.Fraction;
import org.openedit.money.Money;
import org.openedit.store.Cart;
import org.openedit.store.CartItem;
import org.openedit.store.Coupon;
import org.openedit.store.CreditPaymentMethod;
import org.openedit.store.InventoryItem;
import org.openedit.store.PaymentMethod;
import org.openedit.store.Product;
import org.openedit.store.ShippingMethod;
import org.openedit.store.adjustments.Adjustment;
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

	private static final Log log = LogFactory.getLog(Order.class);

	public final static String ACCEPTED = "accepted";
	public final static String AUTHORIZED = "authorized";
	public final static String CAPTURED = "captured";
	public final static String COMPLETED = "completed";
	public final static String REJECTED = "rejected";

	protected Customer fieldCustomer;
	protected List fieldItems;
	protected List fieldAdjustments;
	protected String fieldId;
	
	public boolean hasProfitShare(){
		Iterator<?> itr = this.getItems().iterator();
		while(itr.hasNext()){
			CartItem item = (CartItem) itr.next();
			if (item.getProduct()!=null){
				String fee = item.getProduct().get("partnershipfee");
				if (fee!=null && !fee.isEmpty()){
					log.info("hasprofitshare: found "+fee+" for order #"+this.getId());
					return true;
				}
			}
		}
		return false;
	}

	protected TreeSet<String> fieldMissingItems;

	public TreeSet<String> getMissingItems()
	{
		if (fieldMissingItems == null)
		{
			fieldMissingItems = new TreeSet<String>();
		}
		return fieldMissingItems;
	}

	public void addMissingItem(String inProductId)
	{
		getMissingItems().add(inProductId);
	}

	public void setMissingItems(TreeSet<String> inMissingItems)
	{
		fieldMissingItems = inMissingItems;
	}

	protected ShippingMethod fieldShippingMethod;
	protected Money fieldTotalShipping;

	protected Map fieldTaxes;
	protected List fieldShipments;

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

	protected List<Refund> fieldRefunds;

	public Address getShippingAddress()
	{
		return fieldShippingAddress;
	}

	public void setShippingAddress(Address inShippingAddress)
	{
		fieldShippingAddress = inShippingAddress;
	}

	public Address getBillingAddress()
	{
		return fieldBillingAddress;
	}

	public void setBillingAddress(Address inBillingAddress)
	{
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

	public List getAdjustments()
	{
		if (fieldAdjustments == null)
		{
			fieldAdjustments = new ArrayList();
		}
		return fieldAdjustments;
	}

	public void setAdjustments(List inAdjustments)
	{
		fieldAdjustments = inAdjustments;
	}

	public void copyAdjustments(Cart inCart)
	{
		if (inCart.getAdjustments() == null || inCart.getAdjustments().isEmpty())
		{
			return;
		}
		getAdjustments().clear();// clear them first
		Iterator itr = inCart.getAdjustments().iterator();
		while (itr.hasNext())
		{
			Adjustment adjument = (Adjustment) itr.next();
			getAdjustments().add(adjument);
		}
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
	
	public Money getSurcharge(){
		//surcharge handles either a surcharge on subtotal or a subcharge on total
		//only one percentage should be provided
		//subtotal is checked first
		Money tally = null;
		double percentage = 0.0d;
		String surchargestr = get("percentsurchargesubtotal");
		if (surchargestr != null){
			tally = getSubTotal();
		} else {
			surchargestr = get("percentsurchargetotal");
			if (surchargestr != null){
				tally = getTotalPrice();
			}
		}
		if (surchargestr != null){
			try{
				percentage = Double.parseDouble(surchargestr);
			}catch (Exception e){
				log.error(e.getMessage(), e);
			}
		}
		if (percentage > 0 && tally != null){
			Fraction fraction = new Fraction(percentage);
			return tally.multiply(fraction);
		}
		return null;
	}
	
	public Money getNonTaxableSubtotal(){
		Money amount = new Money("0");
		Iterator<?> itr = getItems().iterator();
		while(itr.hasNext()){
			CartItem item = (CartItem) itr.next();
			if (item == null || item.getProduct() == null || Coupon.isCoupon(item)){
				continue;
			}
			String taxexempt = item.getProduct().get("taxexemptamount");
			if (taxexempt == null || taxexempt.isEmpty()){
				continue;
			}
			amount = amount.add(new Money(taxexempt));
		}
		return amount;
	}
	
	public Money getTaxableSubtotal(){
		Money nontaxable = getNonTaxableSubtotal();
		Money subtotal = getSubTotal();
		return subtotal.subtract(nontaxable);
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
	 * <p>
	 * FIXME: Should we auto-generate an ID?
	 */
	public Order()
	{
		fieldDate = new Date();
	}

	/**
	 * Creates a new order with the given ID and date.
	 * 
	 * @param inId
	 *            The order ID
	 * @param inDate
	 *            The order date
	 */
	public Order(String inId, Date inDate)
	{
		setId(inId);
		fieldDate = inDate;
	}

	/**
	 * Returns the date and time at which the order was made.
	 * 
	 * @return The order date
	 */
	public Date getDate()
	{
		return fieldDate;
	}

	/**
	 * Sets the date and time at which the order was made.
	 * 
	 * @param inDate
	 *            The order date
	 */
	public void setDate(Date inDate)
	{
		fieldDate = inDate;
	}

	/**
	 * Returns the order ID.
	 * 
	 * @return The order ID
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
	 * Sets the order ID. This should not normally be called. Instead, use a
	 * constructor that accepts an ID.
	 * 
	 * @param inId
	 *            The order ID
	 */
	public void setId(String inId)
	{
		fieldId = inId;
	}

	/**
	 * Returns the method that the customer used to pay for this order -- e.g. a
	 * Visa credit card with number 4111-1111-1111-1111.
	 * 
	 * @return The payment method
	 */
	public PaymentMethod getPaymentMethod()
	{
		if (fieldPaymentMethod == null)
		{
			fieldPaymentMethod = new CreditPaymentMethod();
		}
		return fieldPaymentMethod;
	}

	/**
	 * Sets the method used to pay for this order.
	 * 
	 * @param inPaymentMethod
	 *            The payment method
	 */
	public void setPaymentMethod(PaymentMethod inPaymentMethod)
	{
		fieldPaymentMethod = inPaymentMethod;
	}

	public String toString()
	{
		return "Order: " + getId();
	}

	/**
	 * @deprecated Use getOrderStatus instead.
	 */
	public OrderState getOrderState() // TODO: Rename to getOrderStatus
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

	public boolean isCancelled()
	{
		return (getOrderStatus() != null && "cancelled".equals(getOrderStatus().getId()));
	}

	public boolean canCancelOrder()
	{
		boolean canCancel = false;
		if (!isCancelled())
		{
			OrderState status = getOrderStatus();// authorized
			if ("authorized".equals(status.getId()))
			{
				if (isFullyRefunded() /*&& !hasPartialShipments()*/)
				{
					canCancel = true;
				}
			}
			else if ("accepted".equals(status.getId()))
			{
				canCancel = true;
			}
//			else
//			{
//				canCancel = !hasPartialShipments();
//			}
		}
		return canCancel;
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
		if (fieldItems == null)
		{
			fieldItems = new ArrayList();
		}
		return fieldItems;
	}
	
	public List getItemsSorted(){
		return getItemsSorted("name");
	}
	
	public List getItemsSorted(final String inCartField){
		if (inCartField == null){
			return getItems();
		}
		List<CartItem> sorted = new ArrayList<CartItem>();
		Iterator itr = getItems().iterator();
		while(itr.hasNext()){
			CartItem item = (CartItem) itr.next();
			sorted.add(item);
		}
		Collections.sort(sorted, new Comparator<CartItem>(){
			@Override
			public int compare(CartItem o1, CartItem o2) {
				String val1 = o1.get(inCartField);
				String val2 = o2.get(inCartField);
				if (val1 == null && val2 == null){
					return 0;
				}
				if (val1 == null && val2!=null){
					return 1;
				}
				if (val1 !=null && val2 == null){
					return -1;
				}
				return val1.compareTo(val2);
			}});
		return sorted;
	}

	public void setItems(List inItems)
	{
		fieldItems = inItems;
	}

	public void addItem(CartItem inItem)
	{
		if (fieldItems == null)
		{
			fieldItems = new ArrayList();
		}
		getItems().add(inItem);
	}

	public CartItem getItem(String inSku)
	{
		for (Iterator<CartItem> iterator = getItems().iterator(); iterator.hasNext();)
		{
			CartItem item = (CartItem) iterator.next();
			if (item.getSku().equals(inSku))
			{
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
		SubmittedOrder order = (SubmittedOrder) inOrder;
		return order.getOrderNumber().compareTo(getOrderNumber());
	}

	public int getNumItems()
	{
		int numItems = 0;
		for (Iterator it = getItems().iterator(); it.hasNext();)
		{
			CartItem item = (CartItem) it.next();
			numItems += item.getQuantity();
		}
		return numItems;
	}

	public int sumColumn(String colName)
	{
		int sum = 0;
		for (Iterator it = getItems().iterator(); it.hasNext();)
		{
			CartItem item = (CartItem) it.next();
			String stringVal = item.get(colName);
			if (stringVal != null && stringVal.length() != 0)
			{
				int val = Integer.parseInt(item.get(colName));
				sum += val;
			}
		}
		return sum;
	}

	public String get(String inId)
	{
		if ("id".equals(inId))
		{
			return getId();
		}
		if ("orderdate".equals(inId) && getDate() != null)
		{
			return DateStorageUtil.getStorageUtil().formatForStorage(getDate());
		}
		if ("customer".equals(inId) && getCustomer() != null)
		{
			return getCustomer().getId();
		}
		if ("orderstatus".equals(inId) && getOrderStatus() != null)
		{
			return getOrderStatus().getId();
		}
		if ("subtotal".equals(inId))
		{
			return getSubTotal() != null ? getSubTotal().toShortString().replace(",", "") : "0";
		}
		if ("totalshipping".equals(inId))
		{
			return getTotalShipping() != null ? getTotalShipping().toShortString().replace(",", "") : "0";
		}
		if ("tax".equals(inId))
		{
			return getTax() != null ? getTax().toShortString().replace(",", "") : "0";
		}
//		if ("percentsurchargetotal".equals(inId))
//		{
//			Double surcharge = getSurchagePercentTotal();
//			if (surcharge != null){
//				return String.format("%.3f",surcharge.doubleValue());
//			}
//			return null;
//		}
//		if ("percentsurchargesubtotal".equals(inId))
//		{
//			Double surcharge = getSurchargePercentSubtotal();
//			if (surcharge != null){
//				return String.format("%.3f",surcharge.doubleValue());
//			}
//			return null;
//		}
		if ("total".equals(inId))
		{
			return getTotalPrice() != null ? getTotalPrice().toShortString().replace(",", "") : "0";
		}
		if ("paymentmethod".equals(inId) && getPaymentMethod() != null)
		{
			return getPaymentMethod().getType();
		}
		if ("cardnumbermasked".equals(inId) && getPaymentMethod() != null && getPaymentMethod() instanceof CreditPaymentMethod)
		{
			return ((CreditPaymentMethod) getPaymentMethod()).getMaskedCardNumber();
		}
		if ("cardexpirydate".equals(inId) && getPaymentMethod() != null && getPaymentMethod() instanceof CreditPaymentMethod)
		{
			return ((CreditPaymentMethod) getPaymentMethod()).getExpirationDateString();
		}

		if (inId.startsWith("shipping-") || inId.startsWith("billing-"))
		{
			String[] splits = inId.split("-");
			if (splits.length == 2)
			{
				if (splits[0].equals("shipping") && getShippingAddress() != null)
				{
					String val = getShippingAddress().get(splits[1]);
					return val;
				}
				if (splits[0].equals("billing") && getBillingAddress() != null)
				{
					String val = getBillingAddress().get(splits[1]);
					return val;
				}
			}
		}

		if ("distributor".equals(inId))
		{

			StringBuffer buffer = new StringBuffer();
			for (Iterator iterator = getItems().iterator(); iterator.hasNext();)
			{
				CartItem item = (CartItem) iterator.next();
				Product p = item.getProduct();
				String distributor = p.getProperty("distributor");
				if (distributor != null && !buffer.toString().contains(distributor))
				{
					buffer.append(p.get("distributor"));
					buffer.append(" ");

				}

			}
			return buffer.toString();
		}

		if ("totalrefunds".equals(inId))
		{
			Money refundamount = calculateRefundTotal();
			return refundamount.toShortString();
		}
		
		if ("hasprofitshare".equals(inId))
		{
			return hasProfitShare()+"";
		}

		return (String) getProperties().get(inId);
	}

	public Map getProperties()
	{
		if (fieldProperties == null)
		{
			fieldProperties = ListOrderedMap.decorate(new HashMap());
		}
		return fieldProperties;
	}

	public void setProperty(String inKey, String inVal)
	{
		if ("distributor".equals(inKey))
		{
			return;
		}
		getProperties().put(inKey, inVal);
	}

	public Map getTaxes()
	{
		return fieldTaxes;
	}

	public void setTaxes(Map inTaxes)
	{
		fieldTaxes = inTaxes;
	}

	// SHIPMENT INFO //
	public void addShipment(Shipment inShipment)
	{
		getShipments().add(inShipment);
	}

	public List getShipments()
	{
		if (fieldShipments == null)
		{
			fieldShipments = new ArrayList();
		}
		return fieldShipments;
	}

	public void setShipments(List inShipments)
	{
		fieldShipments = inShipments;
	}

	public double getQuantityAvailableForShipment(CartItem cartItem)
	{
		if (cartItem.getProduct() != null && cartItem.getProduct().isCoupon())
		{
			return 0;
		}
		double quantity = cartItem.getQuantity();
		double quantityShipped = getQuantityShipped(cartItem);
		double quantityRefunded = getQuantityRefunded(cartItem);
		double quantityToBeShipped = quantity - quantityRefunded;
		return quantityToBeShipped - quantityShipped;
	}

	public boolean isFullyShipped(CartItem cartItem)
	{
		if (cartItem.getProduct() != null && cartItem.getProduct().isCoupon())
		{
			return true;
		}
		double quantity = cartItem.getQuantity();
		double quantityShipped = getQuantityShipped(cartItem);
		double quantityRefunded = getQuantityRefunded(cartItem);
		double quantityToBeShipped = quantity - quantityRefunded;
		// log.info("order "+this.getId()+" shipping details: quantity="+quantity+", amount shipped="+quantityShipped+", amount refunded: "+quantityRefunded+", amount to be shipped: "+quantityToBeShipped);
		// This assumes that refunds can only occur before a shipment is sent
		return (quantityToBeShipped == quantityShipped);
	}

	public int getQuantityRefunded(CartItem cartItem)
	{
		List<Refund> refunds = getRefunds();
		int tally = 0;
		if (refunds != null)
		{
			for (Refund refund : refunds)
			{
				if (refund.isSuccess())
				{
					List<RefundItem> refunditems = refund.getItems();
					for (RefundItem refunditem : refunditems)
					{
						if (refunditem.getId() != null && refunditem.getId().equals(cartItem.getSku()))
						{
							int quantityRefunded = refunditem.getQuantity();
							tally += quantityRefunded;
						}
					}
				}
			}
		}
		return tally;
	}

	public int getQuantityShipped(CartItem inItem)
	{
		int total = 0;
		for (Iterator iterator = getShipments().iterator(); iterator.hasNext();)
		{
			Shipment shipment = (Shipment) iterator.next();
			for (Iterator iterator2 = shipment.getShipmentEntries().iterator(); iterator2.hasNext();)
			{
				ShipmentEntry entry = (ShipmentEntry) iterator2.next();
				if (entry.getSku().equals(inItem.getSku()))
				{
					total += entry.getQuantity();
				}
			}
		}
		return total;
	}

	public boolean isFullyShipped()
	{
		@SuppressWarnings("unchecked")
		Iterator<CartItem> itr = ((List<CartItem>) getItems()).iterator();
		while (itr.hasNext())
		{
			CartItem cartItem = itr.next();
			if (!isFullyShipped(cartItem))
			{
				return false;
			}
		}
		return true;
	}

	public boolean hasPartialShipments()
	{
		boolean hasPartialShipments = false;
		@SuppressWarnings("unchecked")
		Iterator<CartItem> itr = ((List<CartItem>) getItems()).iterator();
		while (itr.hasNext())
		{
			CartItem cartItem = itr.next();
			int quantityShipped = getQuantityShipped(cartItem);
			if (quantityShipped > 0)
			{
				hasPartialShipments = true;
				break;
			}
		}
		return hasPartialShipments;
	}

	public Shipment getShipmentByWaybill(String inWaybill)
	{
		Shipment shipment = null;
		for (Iterator iterator = getShipments().iterator(); iterator.hasNext();)
		{
			Shipment checkShipment = (Shipment) iterator.next();
			String waybill = checkShipment.get("waybill");
			if (waybill != null)
			{
				if (waybill.equals(inWaybill))
				{
					shipment = checkShipment;
					break;
				}
			}
		}
		return shipment;
	}

	public boolean containsShipmentByWaybill(String inWaybill)
	{
		return getShipmentByWaybill(inWaybill) != null;
	}

	public List getCartItemsByProductProperty(String inKey, String inValue)
	{
		ArrayList list = new ArrayList();
		for (Iterator iterator = getItems().iterator(); iterator.hasNext();)
		{
			CartItem item = (CartItem) iterator.next();
			Product p = item.getProduct();
			if (p.isCoupon())
			{
				continue;
			}

			if (p.getValues(inKey).size() > 0)
			{
				Collection values = p.getValues(inKey);
				if (values.contains(inValue))
				{
					list.add(item);
				}
			}
		}
		return list;
	}

	public CartItem getCartItemByProductProperty(String inKey, String inValue)
	{
		CartItem item = null;
		for (Iterator iterator = getItems().iterator(); iterator.hasNext();)
		{
			CartItem foundItem = (CartItem) iterator.next();
			Product p = foundItem.getProduct();
			if (p != null && p.get(inKey) != null && p.get(inKey).equals(inValue))
			{
				item = foundItem;
				break;
			}
		}
		return item;
	}

	public CartItem getCartItemByProductSku(String inValue)
	{
		CartItem item = null;
		for (Iterator iterator = getItems().iterator(); iterator.hasNext();)
		{
			CartItem foundItem = (CartItem) iterator.next();
			Product p = foundItem.getProduct();
			InventoryItem i = p.getInventoryItemBySku(inValue);
			if (i != null)
			{
				item = foundItem;
				break;
			}
		}
		return item;
	}

	public CartItem getCartItemByProductID(String inID)
	{
		CartItem item = null;
		for (Iterator iterator = getItems().iterator(); iterator.hasNext();)
		{
			CartItem foundItem = (CartItem) iterator.next();
			Product p = foundItem.getProduct();
			if (p.getId().equals(inID))
			{
				item = foundItem;
				break;
			}
		}
		return item;
	}

	public boolean containsItemByProductProperty(String inKey, String inValue)
	{
		return getCartItemByProductProperty(inKey, inValue) != null;
	}

	public boolean containsProduct(Product product)
	{
		boolean exists = false;
		for (Iterator iterator = getItems().iterator(); iterator.hasNext();)
		{
			CartItem item = (CartItem) iterator.next();
			if (item.getProduct().equals(product))
			{
				exists = true;
				break;
			}
		}
		return exists;
	}

	public List<Refund> getRefunds()
	{
		if (fieldRefunds == null)
		{
			fieldRefunds = new ArrayList<Refund>();
		}
		return fieldRefunds;
	}

	public void addRefund(Refund r)
	{
		getRefunds().add(r);
	}

	public Money calculateRefundTotal()
	{
		return calculateRefund("total");
	}

	public Money calculateRefundTax()
	{
		return calculateRefund("tax");
	}

	public Money calculateRefundSubtotal()
	{
		return calculateRefund("subtotal");
	}
	
	public Money calculateRefundNontaxableSubtotal()
	{
		return calculateRefund("nontaxablesubtotal");
	}
	
	public Money calculateRefundTaxableSubtotal()
	{
		Money nontaxable = calculateRefundNontaxableSubtotal();
		Money subtotal = calculateRefundSubtotal();
		return subtotal.subtract(nontaxable);
	}

	public Money calculateRefundShipping()
	{
		String ship = get("shippingrefundtally");
		Money amount = new Money("0");
		if (ship != null && !ship.isEmpty())
		{
			amount = new Money(ship);
		}
		return amount;
	}

	protected Money calculateRefund(String inType)
	{
		Money amount = new Money("0");
		List<Refund> refunds = getRefunds();
		for (Refund refund : refunds)
		{
			if (!refund.isSuccess())
			{
				continue;
			}
			if (inType.equals("total"))
			{
				amount = amount.add(refund.getTotalAmount());
			}
			else if (inType.equals("tax"))
			{
				amount = amount.add(refund.getTaxAmount());
			}
			else if (inType.equals("subtotal"))
			{
				amount = amount.add(refund.getSubTotal());
			}
			else if (inType.equals("nontaxablesubtotal"))
			{
				Iterator<RefundItem> itr = refund.getItems().iterator();
				while(itr.hasNext()){
					RefundItem item = itr.next();
					String productid = item.getId();
					CartItem cartitem = getCartItemByProductID(productid);
					if (cartitem == null){
						cartitem = getCartItemByProductSku(productid);
					}
					if (cartitem == null){
						cartitem = getItem(productid);
					}
					if (cartitem == null || cartitem.getProduct() == null ||  Coupon.isCoupon(cartitem)){
						continue;
					}
					String taxexempt = cartitem.getProduct().get("taxexemptamount");
					if (taxexempt != null && !taxexempt.isEmpty()){
						Money money = new Money(taxexempt);
						amount = amount.add(money);
					}
				}
			}
		}
		return amount;
	}

	public Money calculateTotalAfterRefunds()
	{
		Money refunds = calculateRefundTotal();
		Money total = getTotalPrice();
		if (refunds.isZero())
		{
			return total;
		}
		return total.subtract(refunds);
	}

	public boolean isFullyRefunded()
	{
		Money money = calculateTotalAfterRefunds();
		return money.isZero();
	}

	public static RefundItem calculateRefundInfoForCartItem(Order inOrder, CartItem inItem)
	{
		RefundItem refundItem = new RefundItem();
		refundItem.setId(inItem.getSku());
		Money tally = new Money("0");
		int quantity = 0;
		List<Refund> refunds = inOrder.getRefunds();
		if (inItem.getSku() != null && refunds != null)
		{
			for (Refund refund : refunds)
			{
				if (!refund.isSuccess())
				{
					continue;
				}
				Iterator<RefundItem> itr = refund.getItems().iterator();
				while (itr.hasNext())
				{
					RefundItem item = itr.next();
					String refundId = item.getId();
					if (refundId != null && inItem.getSku().equals(refundId))
					{
						tally = tally.add(item.getTotalPrice());
						quantity += item.getQuantity();
						// can't include unit price because it would vary
						// can however calculate an average??
						break;
					}
				}
			}
		}
		refundItem.setQuantity(quantity);
		refundItem.setTotalPrice(tally);
		return refundItem;
	}

	public boolean containsRecurring()
	{
		return getCart().containsRecurring();
	}

	public boolean requiresShipping()
	{
		return getCart().requiresShipping();
	}

	public Money getPriceAdjustment(CartItem inCartItem)
	{
		Money priceadjustment = new Money();
		if (inCartItem.getProduct() != null && !inCartItem.getProduct().isCoupon() && getAdjustments() != null && !getAdjustments().isEmpty())
		{
			Iterator itr = getAdjustments().iterator();
			while (itr.hasNext())
			{
				Adjustment adjustment = (Adjustment) itr.next();
				if (adjustment.getProductId() == null || !adjustment.getProductId().equals(inCartItem.getProduct().getId()))
				{
					continue;
				}
				priceadjustment = adjustment.adjust(inCartItem);
				break;
			}
		}
		return priceadjustment;
	}

	public Money getTotalPrice(CartItem inCartItem)
	{
		Money total = inCartItem.getTotalPrice();
		Money adjustment = getPriceAdjustment(inCartItem);
		if (adjustment.isZero())
		{
			return total;
		}
		return adjustment.multiply(inCartItem.getQuantity());
	}

	public Money getCouponPrice(CartItem inCartItem)
	{
		Money price = new Money();
		if (inCartItem.getProduct() != null && inCartItem.getProduct().isCoupon())
		{
			// cartitem is a coupon so we need to provide a price IF it is a
			// single value coupon
			if (new Coupon(inCartItem.getInventoryItem()).isSingleValueCoupon())
			{
				price = inCartItem.getYourPrice();
			}
		}
		return price;
	}

	public Date getLastDateShipped(CartItem inItem)
	{
		Date latest = null;
		for (Iterator iterator = getShipments().iterator(); iterator.hasNext();)
		{
			Shipment shipment = (Shipment) iterator.next();
			for (Iterator iterator2 = shipment.getShipmentEntries().iterator(); iterator2.hasNext();)
			{
				ShipmentEntry entry = (ShipmentEntry) iterator2.next();

				if (entry.getSku().equals(inItem.getSku()))
				{
					Date entrydate = DateStorageUtil.getStorageUtil().parseFromStorage(shipment.get("shipdate"));
					if (entrydate != null)
					{
						if (latest == null)
						{
							latest = entrydate;

						}
						else if (latest.before(entrydate))
						{
							latest = entrydate;
						}

					}
				}

			}
		}
		return latest;
	}
	
	/**
	 * 
	 * @param inField
	 * @return
	 */
	public Date getDate(String inField){
		String value = get(inField);
		value = DateStorageUtil.getStorageUtil().checkFormat(value);
		Date date = DateStorageUtil.getStorageUtil().parseFromStorage(value);
		return date;
	}

}