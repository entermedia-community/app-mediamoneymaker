/*
 * Created on Mar 3, 2004
 *
 */
package org.openedit.store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.data.BaseData;
import org.openedit.money.Money;
import org.openedit.store.adjustments.Adjustment;
import org.openedit.store.customer.Address;
import org.openedit.store.customer.Customer;
import org.openedit.store.orders.Order;

/**
 * @author dbrown
 * 
 */
public class Cart extends BaseData
{
	private static final Log log = LogFactory.getLog(Cart.class);
	protected List fieldItems; // sku item map
	protected Customer fieldCustomer;
	protected ShippingMethod fieldShippingMethod;
	protected List fieldAdjustments;
	protected Order fieldCurrentOrder;
	protected Store fieldStore;
	protected String fieldRegion;
	protected Category fieldLastVisitedCatalog;
	protected Category fieldLastLoadedCatalog;
	protected Address fieldShippingAddress;
	public Address getBillingAddress() {
		return fieldBillingAddress;
	}

	public void setItems(List inItems) {
		fieldItems = inItems;
	}

	public void setBillingAddress(Address inBillingAddress) {
		fieldBillingAddress = inBillingAddress;
	}

	protected Address fieldBillingAddress;


	public Cart()
	{
	}

	public Address getShippingAddress() {
		return fieldShippingAddress;
	}

	public void setShippingAddress(Address inShippingAddress) {
		fieldShippingAddress = inShippingAddress;
	}

	public Cart(Store inStore)
	{
		fieldStore = inStore;
	}

	public boolean hasRegion()
	{
		return fieldRegion != null;
	}

	public String getRegion()
	{
		if (fieldRegion == null)
		{
			return ""; // for easy velocity work
		}
		return fieldRegion;
	}

	public void setRegion(String inRegion)
	{
		fieldRegion = inRegion;
	}

	public Order getCurrentOrder()
	{
		return fieldCurrentOrder;
	}

	public void setCurrentOrder(Order inCurrentOrder)
	{
		fieldCurrentOrder = inCurrentOrder;
	}

	public boolean hasZeroSubTotal()
	{
		return getSubTotal().doubleValue() < 0.01;
	}

	public boolean hasZeroTotal()
	{
		return getTotalPrice().doubleValue() < 0.01;
	}
	
	
	public boolean hasBackOrderedItems()
	{
		for (Iterator iter = getItemIterator(); iter.hasNext();)
		{
			CartItem item = (CartItem) iter.next();
			if (item.isBackOrdered())
			{
				return true;
			}
		}
		return false;
	}

	public boolean hasItemWithOption(String inOptionId)
	{
		for (Iterator iter = getItemIterator(); iter.hasNext();)
		{
			CartItem item = (CartItem) iter.next();
			if (item.hasOption(inOptionId))
			{
				return true;
			}
		}
		return false;
	}

	public List getAdjustments()
	{
		if (fieldAdjustments == null)
		{
			fieldAdjustments = new ArrayList();
		}
		return fieldAdjustments;
	}

	public void addAdjustment(Adjustment inAdjustment)
	{
		getAdjustments().add(inAdjustment);
	}

	public List getItems()
	{
		if (fieldItems == null)
		{
			fieldItems = new ArrayList();
		}
		return fieldItems;
	}

	/**
	 * These are CartItems ready for purchase
	 * 
	 * @return
	 */
	public Iterator getItemIterator()
	{
		return getItems().iterator();
	}

	public Iterator getInventoryItemsIterator()
	{
		return getInventoryItems().iterator();
	}

	/**
	 * This points to the original products in the database
	 * 
	 * @return
	 */
	public List getInventoryItems()
	{
		List items = getItems();
		List listOfInventoryItems = new ArrayList();
		for (Iterator iter = items.iterator(); iter.hasNext();)
		{
			CartItem cartItem = (CartItem) iter.next();
			InventoryItem inventoryItem = cartItem.getProduct().getInventoryItemBySku(cartItem.getSku());
			listOfInventoryItems.add(inventoryItem);
		}
		return listOfInventoryItems;
	}

	public int getNumItems()
	{
		return getItems().size();
	}

	public boolean isEmpty()
	{
		if (getItems().size() == 0)
		{
			return true;
		}
		else
		{
			boolean allZeroQuantity = true;
			for (Iterator iter = getItemIterator(); iter.hasNext();)
			{
				CartItem item = (CartItem) iter.next();
				if (item.getQuantity() > 0)
				{
					allZeroQuantity = false;
				}
			}
			return allZeroQuantity;
		}
	}

	public boolean isAdditionalShippingCosts()
	{
		return isAdditionalShippingCostsForMethod(getShippingMethod());
	}

	public boolean isAdditionalShippingCostsForMethod(ShippingMethod inShippingMethod)
	{
		if (inShippingMethod != null)
		{
			for (Iterator it = getItemIterator(); it.hasNext();)
			{
				CartItem item = (CartItem) it.next();
				String handlingChargeLevel = item.getProduct().getHandlingChargeLevel();
				if (handlingChargeLevel != null)
				{
					HandlingCharge handlingCharge = inShippingMethod.getHandlingCharge(handlingChargeLevel);
					if (handlingCharge != null && handlingCharge.isAdditionalCosts())
					{
						return true;
					}
				}
			}
		}
		return false;
	}

	/*
	 * public boolean isEuro() { return Price.REGION_EU.equals(getRegion()); }
	 */
	public Money getTotalShipping()
	{
		if (getShippingMethod() == null)
		{
			return Money.ZERO;
		}
		Money cost = getShippingMethod().getCost(this);
		return cost;
	}

	
	
	
	public Money getTotalTax()
	{
		Money totalTax = Money.ZERO;
		if (getCustomer() == null || getCustomer().getTaxRates() == null || (getCustomer().getTaxExemptId() != null && getCustomer().getTaxExemptId().trim().length() > 0) || getCustomer().getTaxRates().size() == 0)
		{
			return totalTax;
		}
		for (Iterator it = getItemIterator(); it.hasNext();)
		{
			CartItem item = (CartItem) it.next();
			if (!item.getProduct().isTaxExempt())
			{
				Money price = calculateAdjustedPrice(item);
				if (price != null)
				{
					for (Iterator iterator = getCustomer().getTaxRates().iterator(); iterator.hasNext();)
					{
						TaxRate rate = (TaxRate) iterator.next();
						totalTax = totalTax.add(price.multiply(item.getQuantity()).multiply(rate.getFraction()));

					}

				}
			}
		}
		for (Iterator iterator = getCustomer().getTaxRates().iterator(); iterator.hasNext();)
		{
			TaxRate rate = (TaxRate) iterator.next();
			if (rate.isApplyToShipping())
			{
				Money shipping = getTotalShipping();
				totalTax = totalTax.add(shipping.multiply(rate.getFraction()));
			}
		}
		return totalTax;
	}

	// used in velocity to itemize individual tax rates per line item
	public Money getTotalTax(TaxRate inTaxRate)
	{
		Money totalTax = Money.ZERO;
		if (getCustomer() == null || getCustomer().getTaxRates() == null || (getCustomer().getTaxExemptId() != null && getCustomer().getTaxExemptId().trim().length() > 0) || getCustomer().getTaxRates().size() == 0)
		{
			return totalTax;
		}
		for (Iterator it = getItemIterator(); it.hasNext();)
		{
			CartItem item = (CartItem) it.next();
			if (!item.getProduct().isTaxExempt())
			{
				Money price = calculateAdjustedPrice(item);
				if (price != null)
				{

					totalTax = totalTax.add(price.multiply(item.getQuantity()).multiply(inTaxRate.getFraction()));
					if (inTaxRate.isApplyToShipping())
					{
						Money shipping = getTotalShipping();
						totalTax = totalTax.add(shipping.multiply(inTaxRate.getFraction()));
					}

				}
			}
		}
		return totalTax;
	}

	public Money getTotalPrice()
	{
		Money totalPrice = getSubTotal();
		totalPrice = totalPrice.add(getTotalShipping());
		totalPrice = totalPrice.add(getTotalTax());
		return totalPrice;
	}

	public Money getTotalProductsAndShipping()
	{
		Money total = getSubTotal();
		total = total.add(getTotalShipping());
		return total;
	}

	public Money getSubTotal()
	{
		Money totalPrice = Money.ZERO;

		for (Iterator it = getItemIterator(); it.hasNext();)
		{
			CartItem item = (CartItem) it.next();
			Money toadd = calculateAdjustedPrice(item);
			if (toadd != null)
			{
				toadd = toadd.multiply(item.getQuantity());
				totalPrice = totalPrice.add(toadd);
			}
		}
		return totalPrice;
	}

	protected Money calculateAdjustedPrice(CartItem inItem)
	{
		for (Iterator iter = getAdjustments().iterator(); iter.hasNext();)
		{
			Adjustment adjust = (Adjustment) iter.next();
			Money money = adjust.adjust(this, inItem);
			if (money != null)
			{
				return money;
			}
		}
		return inItem.getYourPrice();
	}

	public void addItem(CartItem inItem, int inLineNumber)
	{
		getItems().add(inLineNumber, inItem);
	}

	public void addItem(CartItem inItem)
	{
		if (!getItems().contains(inItem))
		{
			getItems().add(inItem);
		}
	}

	// This is a generic adder
	public void addProduct(Product inProduct)
	{
		CartItem cartItem = new CartItem();
		cartItem.setInventoryItem(inProduct.getInventoryItem(0));
		addItem(cartItem);
	}

	public void removeAllItems()
	{
		getItems().clear();

	}

	public void removeProduct(Product inProduct)
	{
		CartItem toremoveitem = null;
		for (Iterator iter = getItems().iterator(); iter.hasNext();)
		{
			CartItem item = (CartItem) iter.next();
			if (item.getProduct() == inProduct)
			{
				toremoveitem = item;
				break;
			}
		}
		if (toremoveitem != null)
		{
			removeItem(toremoveitem);
		}
	}

	public void removeItem(CartItem inItem)
	{
		getItems().remove(inItem);
	}

	public Customer getCustomer()
	{
		return fieldCustomer;
	}

	public void setCustomer(Customer inCustomer)
	{
		fieldCustomer = inCustomer;
	}

	public ShippingMethod getShippingMethod()
	{
		return fieldShippingMethod;
	}

	public void setShippingMethod(ShippingMethod inShippingMethod)
	{
		fieldShippingMethod = inShippingMethod;
	}

	public List getAvailableShippingMethods()
	{
		List availableMethods = new ArrayList();

		for (Iterator iter = getItemIterator(); iter.hasNext();)
		{
			CartItem cartI = (CartItem) iter.next();
			String method = cartI.getInventoryItem().getProduct().getShippingMethodId();
			if (method != null)
			{
				ShippingMethod smethod = getStore().findShippingMethod(method);
				if (smethod == null)
				{
					log.error("Specified an invalid shipping method " + method);
				}
				else
				{
					availableMethods.add(smethod);
					return availableMethods;
				}
			}
		}
		// TODO: make a composite of product shipping option. Adds up the costs,
		// description and API?

		for (Iterator it = getStore().getAllShippingMethods().iterator(); it.hasNext();)
		{
			ShippingMethod method = (ShippingMethod) it.next();
			if (!method.isHidden() && method.applies(this))
			{
				availableMethods.add(method);
			}
		}
		return availableMethods;
	}

	public Store getStore()
	{
		return fieldStore;
	}

	public void setStore(Store inStore)
	{
		fieldStore = inStore;
	}

	/**
	 * @param inCartItem
	 * @return
	 */
	public CartItem findCartItemWith(InventoryItem inInventoryItem)
	{
		if (inInventoryItem == null)
		{
			return null;
		}
		for (Iterator iter = getItems().iterator(); iter.hasNext();)
		{
			CartItem item = (CartItem) iter.next();
			
			if (item.getInventoryItem() != null && item.getInventoryItem().getSku().equals(inInventoryItem.getSku()))
			{
				return item;
			}
		}

		return null;
	}

	public boolean containsProduct(String inId)
	{
		for (Iterator iter = getItems().iterator(); iter.hasNext();)
		{
			CartItem item = (CartItem) iter.next();
			if (item.getInventoryItem().getProduct().getId().equals(inId))
			{
				return true;
			}
		}
		return false;
	}


	public CartItem getItemForProduct(String inId)
	{
		for (Iterator iter = getItems().iterator(); iter.hasNext();)
		{
			CartItem item = (CartItem) iter.next();
			if (item.getInventoryItem().getProduct().getId().equals(inId))
			{
				return item;
			}
		}
		return null;
	}
	
	
	public Category getLastVisitedCatalog()
	{
		return fieldLastVisitedCatalog;
	}

	public void setLastVisitedCatalog(Category inLastVisitedCatalog)
	{
		fieldLastVisitedCatalog = inLastVisitedCatalog;
		if (inLastVisitedCatalog != null)
		{
			fieldLastLoadedCatalog = inLastVisitedCatalog;
		}
	}

	public Category getLastLoadedCatalog()
	{
		return fieldLastLoadedCatalog;
	}

	public Product findProduct(String inId)
	{
		for (Iterator iterator = getItems().iterator(); iterator.hasNext();)
		{
			CartItem item = (CartItem) iterator.next();
			if (item.getProduct() != null && item.getProduct().getId().equals(inId))
			{
				return item.getProduct();
			}
		}
		return null;
	}

	public Map getTaxes()
	{
		HashMap map = new HashMap();

		for (Iterator iterator = getCustomer().getTaxRates().iterator(); iterator.hasNext();)
		{
			TaxRate rate = (TaxRate) iterator.next();
			Money money = getTotalTax(rate);
			map.put(rate, money);

		}
		return map;
	}
	
	
	public boolean containsRecurring(){
		for (Iterator iterator = getItems().iterator(); iterator.hasNext();)
		{
			CartItem item = (CartItem) iterator.next();
			if (item.getProduct() != null && Boolean.parseBoolean(item.getProduct().get("recurring")))
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean requiresShipping(){
		int regularshipping = 0;
		Iterator<?> itr = getItems().iterator();
		while (itr.hasNext()){
			CartItem item = (CartItem) itr.next();
			Product product = item.getProduct();
			if (!Boolean.parseBoolean(product.get("electronicshipping"))){
				regularshipping++;
			}
		}
		return (regularshipping > 0);
	}
	
	public boolean canPlaceOrder(){
		return (this.getCustomer()!=null && !this.getItems().isEmpty() && this.getCustomer().getBillingAddress(false)!=null &&
				this.getCustomer().getPaymentMethod()!=null);
	}

}
