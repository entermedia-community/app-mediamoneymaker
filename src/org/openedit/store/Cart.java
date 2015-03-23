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
public class Cart extends BaseData {
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
	protected int fieldIdCounter = 0;

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

	public Cart() {
	}

	public Address getShippingAddress() {
		return fieldShippingAddress;
	}

	public void setShippingAddress(Address inShippingAddress) {
		fieldShippingAddress = inShippingAddress;
	}

	public Cart(Store inStore) {
		fieldStore = inStore;
	}

	public boolean hasRegion() {
		return fieldRegion != null;
	}

	public String getRegion() {
		if (fieldRegion == null) {
			return ""; // for easy velocity work
		}
		return fieldRegion;
	}

	public void setRegion(String inRegion) {
		fieldRegion = inRegion;
	}

	public Order getCurrentOrder() {
		return fieldCurrentOrder;
	}

	public void setCurrentOrder(Order inCurrentOrder) {
		fieldCurrentOrder = inCurrentOrder;
	}

	public boolean hasZeroSubTotal() {
		return getSubTotal().doubleValue() < 0.01;
	}

	public boolean hasZeroTotal() {
		return getTotalPrice().doubleValue() < 0.01;
	}

	public boolean hasBackOrderedItems() {
		for (Iterator iter = getItemIterator(); iter.hasNext();) {
			CartItem item = (CartItem) iter.next();
			if (item.isBackOrdered()) {
				return true;
			}
		}
		return false;
	}

	public boolean hasItemWithOption(String inOptionId) {
		for (Iterator iter = getItemIterator(); iter.hasNext();) {
			CartItem item = (CartItem) iter.next();
			if (item.hasOption(inOptionId)) {
				return true;
			}
		}
		return false;
	}

	public List getAdjustments() {
		if (fieldAdjustments == null) {
			fieldAdjustments = new ArrayList();
		}
		return fieldAdjustments;
	}

	public void addAdjustment(Adjustment inAdjustment) {
		getAdjustments().add(inAdjustment);
	}

	public List getItems() {
		if (fieldItems == null) {
			fieldItems = new ArrayList();
		}
		return fieldItems;
	}

	/**
	 * These are CartItems ready for purchase
	 * 
	 * @return
	 */
	public Iterator getItemIterator() {
		return getItems().iterator();
	}

	public Iterator getInventoryItemsIterator() {
		return getInventoryItems().iterator();
	}

	/**
	 * This points to the original products in the database
	 * 
	 * @return
	 */
	public List getInventoryItems() {
		List items = getItems();
		List listOfInventoryItems = new ArrayList();
		for (Iterator iter = items.iterator(); iter.hasNext();) {
			CartItem cartItem = (CartItem) iter.next();
			if (cartItem == null || cartItem.getProduct() == null
					|| cartItem.getSku() == null) {
				continue;// should remove if these problems are there??
			}
			InventoryItem inventoryItem = cartItem.getProduct()
					.getInventoryItemBySku(cartItem.getSku());
			listOfInventoryItems.add(inventoryItem);
		}
		return listOfInventoryItems;
	}

	public int getNumItems() {
		return getItems().size();
	}

	public boolean isEmpty() {
		if (getItems().size() == 0) {
			return true;
		} else {
			boolean allZeroQuantity = true;
			for (Iterator iter = getItemIterator(); iter.hasNext();) {
				CartItem item = (CartItem) iter.next();
				if (item.getQuantity() > 0) {
					allZeroQuantity = false;
				}
			}
			return allZeroQuantity;
		}
	}

	public boolean isAdditionalShippingCosts() {
		return isAdditionalShippingCostsForMethod(getShippingMethod());
	}

	public boolean isAdditionalShippingCostsForMethod(
			ShippingMethod inShippingMethod) {
		if (inShippingMethod != null) {
			for (Iterator it = getItemIterator(); it.hasNext();) {
				CartItem item = (CartItem) it.next();
				String handlingChargeLevel = item.getProduct()
						.getHandlingChargeLevel();
				if (handlingChargeLevel != null) {
					HandlingCharge handlingCharge = inShippingMethod
							.getHandlingCharge(handlingChargeLevel);
					if (handlingCharge != null
							&& handlingCharge.isAdditionalCosts()) {
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
	public Money getTotalShipping() {
		if (getShippingMethod() == null) {
			return Money.ZERO;
		}
		Money cost = getShippingMethod().getCost(this);
		return cost;
	}

	public Money getTotalTax() {
		Money totalTax = Money.ZERO;
		if (getCustomer() == null
				|| getCustomer().getTaxRates() == null
				|| (getCustomer().getTaxExemptId() != null && getCustomer()
						.getTaxExemptId().trim().length() > 0)
				|| getCustomer().getTaxRates().size() == 0) {
			return totalTax;
		}
		for (Iterator it = getItemIterator(); it.hasNext();) {
			CartItem item = (CartItem) it.next();
			if (!item.getProduct().isTaxExempt()) {
				Money price = calculateAdjustedPrice(item);
				if (price != null) {
					if (item.hasTaxExemptAmount()) {
						price = price.subtract(item.getTaxExemptAmount());
					}
					for (Iterator iterator = getCustomer().getTaxRates()
							.iterator(); iterator.hasNext();) {
						TaxRate rate = (TaxRate) iterator.next();
						totalTax = totalTax.add(price.multiply(
								item.getQuantity())
								.multiply(rate.getFraction()));

					}

				}
			}
		}
		for (Iterator iterator = getCustomer().getTaxRates().iterator(); iterator
				.hasNext();) {
			TaxRate rate = (TaxRate) iterator.next();
			if (rate.isApplyToShipping()) {
				Money shipping = getTotalShipping();
				totalTax = totalTax.add(shipping.multiply(rate.getFraction()));
			}
		}
		return totalTax;
	}

	// used in velocity to itemize individual tax rates per line item
	public Money getTotalTax(TaxRate inTaxRate) {
		Money totalTax = Money.ZERO;
		if (getCustomer() == null
				|| getCustomer().getTaxRates() == null
				|| (getCustomer().getTaxExemptId() != null && getCustomer()
						.getTaxExemptId().trim().length() > 0)
				|| getCustomer().getTaxRates().size() == 0) {
			return totalTax;
		}
		for (Iterator it = getItemIterator(); it.hasNext();) {
			CartItem item = (CartItem) it.next();
			if (!item.getProduct().isTaxExempt()) {
				Money price = calculateAdjustedPrice(item);
				if (price != null) {
					if (item.hasTaxExemptAmount()) {
						price = price.subtract(item.getTaxExemptAmount());
					}
					totalTax = totalTax.add(price.multiply(item.getQuantity())
							.multiply(inTaxRate.getFraction()));
				}
			}
		}
		if (inTaxRate.isApplyToShipping()) {
			Money shipping = getTotalShipping();
			totalTax = totalTax.add(shipping.multiply(inTaxRate.getFraction()));
		}
		return totalTax;
	}

	public Money getTaxExemptAmount() {
		Money exemptamount = Money.ZERO;
		if (getCustomer() == null
				|| getCustomer().getTaxRates() == null
				|| (getCustomer().getTaxExemptId() != null && getCustomer()
						.getTaxExemptId().trim().length() > 0)
				|| getCustomer().getTaxRates().size() == 0) {
			return exemptamount;
		}
		for (Iterator it = getItemIterator(); it.hasNext();) {
			CartItem item = (CartItem) it.next();

			if (item.hasTaxExemptAmount()) {
				exemptamount = exemptamount.add(item.getTaxExemptAmount());
			}

		}

		return exemptamount;
	}

	public Money getTotalPrice() {
		Money totalPrice = getSubTotal();
		Money shipping = getTotalShipping();
		Money tax = getTotalTax();
		totalPrice = totalPrice.add(shipping);
		totalPrice = totalPrice.add(tax);
		return totalPrice;
	}

	public Money getTotalProductsAndShipping() {
		Money total = getSubTotal();
		total = total.add(getTotalShipping());
		return total;
	}

	public Money getSubTotal() {
		Money totalPrice = Money.ZERO;

		for (Iterator it = getItemIterator(); it.hasNext();) {
			CartItem item = (CartItem) it.next();
			Money toadd = calculateAdjustedPrice(item);
			if (toadd != null) {
				toadd = toadd.multiply(item.getQuantity());
				totalPrice = totalPrice.add(toadd);
			}
		}
		return totalPrice;
	}

	public Money getSubtotalWithoutCoupons() {
		Money totalPrice = Money.ZERO;
		for (Iterator it = getItemIterator(); it.hasNext();) {
			CartItem item = (CartItem) it.next();
			if (Coupon.isCoupon(item)) {
				continue;
			}
			Money toadd = item.getYourPrice();// get non-adjusted price
			if (toadd != null) {
				toadd = toadd.multiply(item.getQuantity());
				totalPrice = totalPrice.add(toadd);
			}
		}
		return totalPrice;
	}

	protected Money calculateAdjustedPrice(CartItem inItem) {
		Money adjustedprice = null;
		for (Iterator<?> iter = getAdjustments().iterator(); iter.hasNext();) {
			Adjustment adjust = (Adjustment) iter.next();
			Money money = adjust.adjust(this, inItem);
			if (money != null) {
				adjustedprice = money;
				break;
			}
		}
		if (adjustedprice == null) {
			adjustedprice = inItem.getYourPrice();
		}
		return adjustedprice;
	}

	public void addItem(CartItem inItem, int inLineNumber) {
		if (inItem.getId() == null) {
			inItem.setId(nextId());
		}

		getItems().add(inLineNumber, inItem);
	}

	private String nextId() {
		return String.valueOf(fieldIdCounter++);
	}

	public void addItem(CartItem inItem) {
		if (inItem.getYourPrice() != null && inItem.getYourPrice().isNegative()) {
			inItem.setQuantity(1);
		}
		if (inItem.getId() == null) {
			inItem.setId(nextId());
		}
		if (!getItems().contains(inItem)) {
			getItems().add(inItem);
		}
	}

	// This is a generic adder
	public void addProduct(Product inProduct) {
		CartItem cartItem = new CartItem();
		cartItem.setInventoryItem(inProduct.getInventoryItem(0));
		addItem(cartItem);
	}

	public void removeAllItems() {
		getItems().clear();

	}

	public void removeProduct(Product inProduct) {
		CartItem toremoveitem = null;
		for (Iterator iter = getItems().iterator(); iter.hasNext();) {
			CartItem item = (CartItem) iter.next();
			if (item.getProduct() == inProduct) {
				toremoveitem = item;
				break;
			}
		}
		if (toremoveitem != null) {
			removeItem(toremoveitem);
		}
	}

	public void removeItem(CartItem inItem) {
		getItems().remove(inItem);
	}

	public Customer getCustomer() {
		return fieldCustomer;
	}

	public void setCustomer(Customer inCustomer) {
		fieldCustomer = inCustomer;
	}

	public ShippingMethod getShippingMethod() {
		return fieldShippingMethod;
	}

	public void setShippingMethod(ShippingMethod inShippingMethod) {
		fieldShippingMethod = inShippingMethod;
	}

	public List getAvailableShippingMethods() {
		List availableMethods = new ArrayList();

		for (Iterator iter = getItemIterator(); iter.hasNext();) {
			CartItem cartI = (CartItem) iter.next();
			String method = cartI.getInventoryItem().getProduct()
					.getShippingMethodId();
			if (method != null) {
				ShippingMethod smethod = getStore().findShippingMethod(method);
				if (smethod == null) {
					log.error("Specified an invalid shipping method " + method);
				} else {
					availableMethods.add(smethod);
					return availableMethods;
				}
			}
		}
		// TODO: make a composite of product shipping option. Adds up the costs,
		// description and API?

		for (Iterator it = getStore().getAllShippingMethods().iterator(); it
				.hasNext();) {
			ShippingMethod method = (ShippingMethod) it.next();
			if (!method.isHidden() && method.applies(this)) {
				availableMethods.add(method);
			}
		}
		return availableMethods;
	}

	public Store getStore() {
		return fieldStore;
	}

	public void setStore(Store inStore) {
		fieldStore = inStore;
	}

	/**
	 * @param inCartItem
	 * @return
	 */
	public CartItem findCartItemWith(InventoryItem inInventoryItem) {
		if (inInventoryItem == null) {
			return null;
		}
		for (Iterator iter = getItems().iterator(); iter.hasNext();) {
			CartItem item = (CartItem) iter.next();

			String sku = item.getInventoryItem().getSku();
			String sku2 = inInventoryItem.getSku();
			if (item.getInventoryItem() != null && sku.equals(sku2)) {
				return item;
			}
		}

		return null;
	}

	public boolean containsProduct(String inId) {
		for (Iterator iter = getItems().iterator(); iter.hasNext();) {
			CartItem item = (CartItem) iter.next();
			if (item.getInventoryItem().getProduct().getId().equals(inId)) {
				return true;
			}
		}
		return false;
	}

	public CartItem getItemForProduct(String inId) {
		for (Iterator iter = getItems().iterator(); iter.hasNext();) {
			CartItem item = (CartItem) iter.next();
			if (item.getInventoryItem().getProduct().getId().equals(inId)) {
				return item;
			}
		}
		return null;
	}

	public Category getLastVisitedCatalog() {
		return fieldLastVisitedCatalog;
	}

	public void setLastVisitedCatalog(Category inLastVisitedCatalog) {
		fieldLastVisitedCatalog = inLastVisitedCatalog;
		if (inLastVisitedCatalog != null) {
			fieldLastLoadedCatalog = inLastVisitedCatalog;
		}
	}

	public Category getLastLoadedCatalog() {
		return fieldLastLoadedCatalog;
	}

	public Product findProduct(String inId) {
		for (Iterator iterator = getItems().iterator(); iterator.hasNext();) {
			CartItem item = (CartItem) iterator.next();
			if (item.getProduct() != null
					&& item.getProduct().getId().equals(inId)) {
				return item.getProduct();
			}
		}
		return null;
	}

	public Map getTaxes() {
		HashMap map = new HashMap();
		if (getCustomer() != null && getCustomer().getTaxRates() != null) {
			for (Iterator iterator = getCustomer().getTaxRates().iterator(); iterator
					.hasNext();) {
				TaxRate rate = (TaxRate) iterator.next();
				Money money = getTotalTax(rate);
				map.put(rate, money);

			}
		}
		return map;
	}

	public boolean containsRecurring() {
		for (Iterator iterator = getItems().iterator(); iterator.hasNext();) {
			CartItem item = (CartItem) iterator.next();
			if (item.getProduct() != null
					&& Boolean.parseBoolean(item.getProduct().get("recurring"))) {
				return true;
			}
		}
		return false;
	}

	public boolean requiresShipping() {
		if (Boolean.parseBoolean(getStore().get("skipshipping"))) {
			return false;
		}
		int regularshipping = 0;
		Iterator<?> itr = getItems().iterator();
		while (itr.hasNext()) {
			CartItem item = (CartItem) itr.next();
			Product product = item.getProduct();
			if (!Boolean.parseBoolean(product.get("electronicshipping"))) {
				regularshipping++;
			}
		}
		return (regularshipping > 0);
	}

	public boolean canPlaceOrder() {
		if (getItems() == null || getItems().isEmpty() || getCustomer() == null) {
			log.info("Cannot place order: no customer or orders");
			return false;
		}
		if (getCustomer().getAddressList() == null
				|| getCustomer().getAddressList().isEmpty()) {
			log.info("Cannot place order: no addresses");
			return false;
		}
		if (getCustomer().getBillingAddress() == null
				|| !getCustomer().getBillingAddress().isComplete()) {
			log.info("Cannot place order: no billing address");
			return false;
		}
		if (getCustomer().getPaymentMethod() == null) {
			log.info("Cannot place order: no method of payment");
			return false;
		}
		if (getCustomer().getTaxRates() == null
				|| getCustomer().getTaxRates().isEmpty()) {
			if (getCustomer().getTaxExemptId() == null
					|| getCustomer().getTaxExemptId().isEmpty()) {
				log.info("Cannot place order: no tax rate and not tax exempt");
				return false;
			}
		}
		log.info("Can proceed to place order");
		return true;
	}

	public boolean hasProductsWithPartialPayments() {
		Iterator<?> itr = getItems().iterator();
		while (itr.hasNext()) {
			CartItem item = (CartItem) itr.next();
			Product product = item.getProduct();
			if (Boolean.parseBoolean(product.get("allowspartialpayments"))) {
				return true;
			}
		}
		return false;
	}

	public void updatePartialPayment(String inProductId, String inFrequency,
			String inOccurrences) {
		Iterator<?> itr = getItems().iterator();
		while (itr.hasNext()) {
			CartItem item = (CartItem) itr.next();
			Product product = item.getProduct();
			if (product.getId() != null && product.getId().equals(inProductId)) {
				item.setProperty("occurrences", inOccurrences);
				item.setProperty("frequency", inFrequency);
				return;
			}
		}
	}

	public void removeById(String id) {
		CartItem toremove = null;
		for (Iterator<?> iterator = getItemIterator(); iterator.hasNext();) {
			CartItem item = (CartItem) iterator.next();
			if (id.equals(item.getId())) {
				toremove = item;
				break;
			}
		}
		if (toremove != null) {
			removeItem(toremove);
		}

	}

	public Money getPriceAdjustment(CartItem inCartItem) {
		Money priceadjustment = new Money();
		if (inCartItem.getProduct() != null
				&& !inCartItem.getProduct().isCoupon()
				&& getAdjustments() != null && !getAdjustments().isEmpty()) {
			Iterator itr = getAdjustments().iterator();
			while (itr.hasNext()) {
				Adjustment adjustment = (Adjustment) itr.next();
				if (adjustment.getProductId() == null
						|| !adjustment.getProductId().equals(
								inCartItem.getProduct().getId())) {
					continue;
				}
				priceadjustment = adjustment.adjust(inCartItem);
				break;
			}
		}
		return priceadjustment;
	}

	public Money getTotalPrice(CartItem inCartItem) {
		Money total = inCartItem.getTotalPrice();
		Money adjustment = getPriceAdjustment(inCartItem);
		if (adjustment.isZero()) {
			return total;
		}
		return adjustment.multiply(inCartItem.getQuantity());
	}

	public Money getCouponPrice(CartItem inCartItem) {
		Money price = new Money();
		if (inCartItem.getProduct() != null
				&& inCartItem.getProduct().isCoupon()) {
			// cartitem is a coupon so we need to provide a price IF it is a
			// single value coupon
			if (new Coupon(inCartItem.getInventoryItem()).isSingleValueCoupon()) {
				price = inCartItem.getYourPrice();
			}
		}
		return price;
	}

}
