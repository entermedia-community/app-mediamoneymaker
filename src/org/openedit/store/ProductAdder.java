/*
 * Created on Jun 7, 2006
 */
package org.openedit.store;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.money.Money;
import org.openedit.store.adjustments.Adjustment;
import org.openedit.store.adjustments.CouponAdjustment;
import org.openedit.store.adjustments.DiscountAdjustment;
import org.openedit.store.adjustments.SaleAdjustment;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.users.User;

public class ProductAdder
{
	private static final Log log = LogFactory.getLog(ProductAdder.class);

	protected static final String PRODUCTID = "productid";

	public void addItem(WebPageRequest inReq, Cart inCart, boolean inReload) throws StoreException
	{
		List olditems = new ArrayList(inCart.getItems());
		// Get item info from request parameters
		if (inReload)
		{
			inCart.removeAllItems();
		}
		int productCount = computeProductCount(inReq);
		Map params = inReq.getParameterMap();
		for (int i = 0; i < productCount + 2; i++)
		{
			String counter = null;
			if (i == 0)
			{
				counter = "";
			}
			else
			{
				counter = "." + i;
			}
			String productId = (String) params.get("productid" + counter);
			if (productId == null)
			{
				continue;
			}
			Product product = inCart.getStore().getProduct(productId);
			if (product == null)
			{
				product = findProduct(productId, olditems);
				if (product == null)
				{
					continue;
				}
			}
			String checked = inReq.getRequestParameter("remove" + counter);
			if (checked != null)
			{
				inCart.removeProduct(product);
				if (product.isCoupon())
				{
					InventoryItem item = findInventoryItem(product,olditems);
					if (item!=null)//removed so update adjustment
					{
						Coupon removedCoupon = new Coupon(item);
						removedCoupon.removeCartAdjustment(inCart);
					}
				}
				continue;
			}
			int quantity = 0;
			String quantityStr = (String) params.get("quantity" + counter);
			if(i == 0)
			{
				quantity = 1;
			}
			boolean quantityspecified = false;
			// Quantity
			if (quantityStr != null && !quantityStr.equals("quantity") && quantityStr.length() != 0)
			{
				if (!quantityStr.contains(".")) {
					quantity = Integer.parseInt(quantityStr);
					quantityspecified = true;
				} else {
					String[] arr = quantityStr.split("\\.");
					quantity = Integer.parseInt(arr[0]);
				}
			}
			else if (quantityStr != null && quantityStr.length() == 0)
			{
				quantity = 0;
			}

			if (quantity <= 0 && !product.isCoupon())
			{
				//remove the product if quantity is less than or equal to zero
//				inCart.removeProduct(product); //DON'T DO THIS!
				continue;
			}
			// Look for any options being passed to us. Option can be a size,
			// color, count, hosting
			Set options = readOptions(counter, params, product);
			Map properties = readProperties(counter, params, product);
			// legacy way
			String size = (String) params.get("size" + counter);
			if (size != null)
			{
				Option option = makeOption(product, "size", size);
				options.add(option);
			}
			String color = (String) params.get("color" + counter);
			if (color != null && color.length() > 0)
			{
				Option option = makeOption(product, "color", color);
				options.add(option);
			}
			
			InventoryItem inventory  = null;
			if (product.isCoupon())
			{
				inventory = findInventoryItem(product,olditems);
				if (inventory!=null && checked!=null)//removed so update adjustment
				{
					Coupon removedCoupon = new Coupon(inventory);
					removedCoupon.removeCartAdjustment(inCart);
				}
			} else {
				inventory = product.getInventoryItemByOptions(options);
				if (inventory == null)
				{
					inventory = product.getCloseInventoryItemByOptions(options);
				}
				if (inventory == null && product.getInventoryItemCount() == 0)
				{
					inventory = new InventoryItem();
					String sku = product.getId() + "-1";
					inventory.setSku(sku);
					inventory.setProduct(product);
				}
			}

			boolean alwaysadd = false;
			String forcenew = inReq.getRequestParameter("forcenew");
			if (forcenew != null)
			{
				alwaysadd = Boolean.parseBoolean(forcenew);
			}
			CartItem cartItem = null;
			CartItem existing = inCart.findCartItemWith(inventory);
			if (!alwaysadd)
			{
				cartItem = inCart.findCartItemWith(inventory);
			}

			if (cartItem == null )
			{
				cartItem = new CartItem();
				if(i == 0){
					quantityspecified = true;
				}
			}
			cartItem.setInventoryItem(inventory);
			int oldquantity = cartItem.getQuantity();
			if (quantityspecified && inReload)
			{
				cartItem.setQuantity(quantity);
			}
			else if(quantityspecified && existing == null){
				cartItem.setQuantity(quantity);
			}
			else
			{
				cartItem.setQuantity(quantity + oldquantity);
			}
			
			
			cartItem.setOptions(options);
			cartItem.setProperties(properties);

			// product.setproperty(title); (if title is a property on the
			// product...the value should come from the options)

			// Setup shipping options for each item
			String address = inReq.getRequestParameter("address" + counter);
			if (address != null)
			{
				cartItem.setShippingPrefix(address);
			}
			inCart.addItem(cartItem);

			// Adds any other required options
			List all = cartItem.getInventoryItem().getAllOptions();
			for (Iterator iter = all.iterator(); iter.hasNext();)
			{
				Option remaining = (Option) iter.next();
				if (remaining.isRequired())
				{
					if (!cartItem.hasOption(remaining.getId()))
					{
						cartItem.addOption(remaining);
					}
				}
			}

			String[] fields = inReq.getRequestParameters("field");
			if (fields != null)
			{
				for (int j = 0; j < fields.length; j++)
				{
					String field = fields[j];
					String val = inReq.getRequestParameter(field + ".value" + counter);
					if (val != null)
					{
						cartItem.setProperty(field, val);
					}
				}
			}
			// If product supports custom price, then get price from user
			if (product.isCustomPrice())
			{
				String priceStr = (String) params.get("price" + counter);
				if (priceStr != null)
				{
					priceStr = priceStr.trim();
					cartItem.setYourPrice(new Money(priceStr));
				}
			}
		}
		//remove any old adjustments
		Coupon.removeOldAdjustmentsAndCoupons(inCart);
		//once products are updated check coupon dependencies
		Iterator<Coupon> itr = getCoupons(inCart).iterator();
		while(itr.hasNext())
		{
			Coupon coupon = itr.next();
			if (coupon.hasCustomerUsedCoupon(inReq, inCart))
			{
				removeCoupon(inCart,coupon);
				inReq.putPageValue("errorMessage", "Coupon ("+coupon.getInventoryItem().getProduct()+") removed (number of uses restriction)");
				inReq.putPageValue("couponerror", true);
			}
			else if (!coupon.isCartSubtotalOk(inCart))
			{
				removeCoupon(inCart,coupon);
				inReq.putPageValue("errorMessage", "Coupon ("+coupon.getInventoryItem().getProduct()+") removed (subtotal restriction)");
				inReq.putPageValue("couponerror", true);
			} 
			else if (!coupon.isCartItemsOk(inCart))
			{
				removeCoupon(inCart,coupon);
				inReq.putPageValue("errorMessage", "Coupon ("+coupon.getInventoryItem().getProduct()+") removed (product restriction)");
				inReq.putPageValue("couponerror", true);
			}
		}
		
		
	}
	
	private void removeCoupon(Cart inCart, Coupon inCoupon)
	{
		Iterator itr = inCart.getInventoryItems().iterator();
		while (itr.hasNext())
		{
			InventoryItem item = (InventoryItem) itr.next();
			if (Coupon.isCoupon(item) && item == inCoupon.getInventoryItem())
			{
				CartItem cartitem = inCart.findCartItemWith(item);
				if (cartitem!=null)
				{
					inCart.removeItem(cartitem);
				}
				inCoupon.removeCartAdjustment(inCart);
				return;
			}
		}
	}
	
	private List<Coupon> getCoupons(Cart inCart)
	{
		List<Coupon> coupons = new ArrayList<Coupon>();
		Iterator itr = inCart.getInventoryItems().iterator();
		while (itr.hasNext())
		{
			InventoryItem item = (InventoryItem) itr.next();
			if (Coupon.isCoupon(item))
			{
				coupons.add(new Coupon(item));
			}
		}
		return coupons;
	}

	private Product findProduct(String inProductId, List inOlditems)
	{
		for (Iterator iterator = inOlditems.iterator(); iterator.hasNext();)
		{
			CartItem item = (CartItem) iterator.next();
			if (item.getProduct() != null && item.getProduct().getId() != null && item.getProduct().getId().equals(inProductId))
			{
				return item.getProduct();
			}
		}
		return null;
	}
	
	private InventoryItem findInventoryItem(Product inProduct, List inOlditems)
	{
		String productid = inProduct.getId();
		for (Iterator iterator = inOlditems.iterator(); iterator.hasNext();)
		{
			CartItem item = (CartItem) iterator.next();
			if (item.getProduct() != null && item.getProduct().getId() != null && item.getProduct().getId().equals(productid))
			{
				return item.getInventoryItem();
			}
		}
		return null;
	}

	protected Set readOptions(String code, Map params, Product product)
	{
		Set options = new HashSet();
		for (Iterator iter = params.keySet().iterator(); iter.hasNext();)
		{
			String paramid = (String) iter.next();
			String oid = null;
			String start = "option" + code + ".";
			if (paramid.startsWith(start))
			{
				oid = paramid.substring(start.length());
				String userinput = (String) params.get(paramid);
				if (userinput != null && userinput.trim().length() == 0)
				{
					userinput = null;
				}
				Option option = makeOption(product, oid, userinput);
				if (option.isRequired() || userinput != null)
				{
					options.add(option);
				}
			}
		}

		for (Iterator iter = params.keySet().iterator(); iter.hasNext();)
		{
			String paramid = (String) iter.next();
			String start = "optiongroup" + code; // the values are the option
			// ID's
			if (paramid.startsWith(start))
			{
				String optionid = (String) params.get(paramid);
				Option option = makeOption(product, optionid, null);
				options.add(option);
			}
		}

		return options;
	}

	private Option makeOption(Product product, String oid, String value)
	{
		Option option = new Option();
		option.setId(oid);
		option.setName(oid);
		option.setValue(value);
		Option realoption = product.getOption(oid);
		if (realoption != null)
		{
			option.setName(realoption.getName());
			option.setPriceSupport(realoption.getPriceSupport());
			option.setRequired(realoption.isRequired());
			option.setDataType(realoption.getDataType());
		}
		return option;
	}

	public boolean isMultiProductMode(WebPageRequest inReq) throws Exception
	{
		String productId = inReq.getRequestParameter(PRODUCTID);
		if (productId != null)
		{
			return false;
		}
		else
		{
			productId = inReq.getRequestParameter("productid.1");
			if (productId != null)
			{
				return true;
			}
			else
			{
				throw new Exception("Product ID not found.");
			}
		}
	}

	public int computeProductCount(WebPageRequest inReq) throws StoreException
	{
		// One product
		String productId = inReq.getRequestParameter(PRODUCTID);
		if (productId != null)
		{
			return 1;
		}
		else
		{
			// Multiple products
			int count = 0;
			String productId1 = inReq.getRequestParameter("productid." + (count + 1));
			while (productId1 != null)
			{
				count++;
				productId1 = inReq.getRequestParameter("productid." + (count + 1));
			}
			return count;
		}
	}

	/**
	 * This should return null if no match
	 * 
	 * @param inSize
	 * @param inColor
	 * @return
	 */
	public void setInventoryItem(CartItem inItem, Product inProduct) throws StoreException
	{
		if (inProduct.getInventoryItemCount() == 0)
		{
			// This item is very simple
			CartItem item = new CartItem();
			InventoryItem inven = new InventoryItem();
			inven.setSku("noinventory");
			inven.setProduct(inProduct);
			item.setInventoryItem(inven);
			return;
		}

		// CartItem cartItem = new CartItem();
		// cartItem.setQuantity(inQuantity);

		// try for exact match of what they ordered
		for (Iterator iter = inProduct.getInventoryItems().iterator(); iter.hasNext();)
		{
			InventoryItem inventoryItem = (InventoryItem) iter.next();
			if (inventoryItem.isExactMatch(inItem.getOptions()))
			{
				inItem.setInventoryItem(inventoryItem);
				return;
			}
		}
		// try just the same size
		for (Iterator iter = inProduct.getInventoryItems().iterator(); iter.hasNext();)
		{
			InventoryItem inventoryItem = (InventoryItem) iter.next();
			if (inventoryItem.isCloseMatch(inItem.getOptions()))
			{
				inItem.setInventoryItem(inventoryItem);
				return;
			}
		}

		// take anything on the list. this should not happen
		// log.error("Not hits on item");
		InventoryItem item = (InventoryItem) inProduct.getInventoryItems().get(0);
		inItem.setInventoryItem(item);
		return;
	}

	public void updateCart(WebPageRequest inReq, Cart inCart) throws StoreException
	{
		String reload = inReq.getRequestParameter("reloadcart");
		if (Boolean.parseBoolean(reload))
		{
			addItem(inReq, inCart, true);
		}
		else
		{
			addItem(inReq, inCart, false);
		}
	}

	public void addCoupon(WebPageRequest inReq, Cart inCart) throws Exception
	{
		String couponcode = inReq.getRequestParameter("couponcode");
		if (couponcode != null && couponcode.length() > 0)
		{
			couponcode = couponcode.trim();
			// make sure there is no negative product in there already then add
			// the product ID in there
			// we might have to look up the producinCartt ID up since W@W needs
			// that
			// use Lucene to search
			Store store = inCart.getStore();
			HitTracker hits = store.getProductSearcher().fieldSearch("items", couponcode);
			if (hits.getTotal() > 0)
			{
				Data hit = (Data) hits.get(0);
				String productId = hit.get("id");
				Product product = store.getProduct(productId);
				if (product != null)
				{
					InventoryItem inventoryItem = product.getInventoryItemBySku(couponcode);
					Coupon coupon = new Coupon(inventoryItem);
					if (inventoryItem == null || !coupon.isInStock())
					{
						inReq.putPageValue("errorMessage", "That coupon is no longer available.");
						inReq.putPageValue("couponerror", true);
						return;
					}
					
					//percentage, discount, product, minquantity, minsubtotal, expirydate, acceptmultiple, restricttouser
					String facevalue = inventoryItem.getPriceSupport() == null ? "0" : inventoryItem.getPriceSupport().getYourPriceByQuantity(1).toShortString();
					double percentage = coupon.getPercentage();
					double discount = coupon.getDiscount();
					String productid = coupon.getProductId();
					int minquantity = coupon.getMininumProductQuantity();
					double minsubtotal = coupon.getMinimumSubtotal();
					Date expiry = coupon.getCouponExpiry();
					boolean multiple = coupon.isAcceptsMultiple();
					boolean oneperuser = coupon.isOnePerUser();
					String restrict = coupon.getUserRestriction();
					List<String> sites = coupon.getSiteRestrictions();
					
					log.info("Coupon Details - Face Value: "+facevalue+", Discount: "+discount+", Precentage: "+percentage+", Product: "+productid+", Min quantity: "+minquantity+", Min Subtotal: "+minsubtotal+", Expiry: "+expiry+" ("+inventoryItem.getProperty("expirydate")+"), Multiple: "+multiple+", On per user? "+oneperuser+", User: "+restrict+", Restricted to Sites: "+sites);
					
					if (!coupon.isSiteAllowed(inReq))
					{
						inReq.putPageValue("errorMessage", "That coupon cannot be used on this site.");
						inReq.putPageValue("couponerror", true);
						return;
					}
					if (coupon.hasCustomerUsedCoupon(inReq, inCart))
					{
						inReq.putPageValue("errorMessage", "That coupon can only be used by you once.");
						inReq.putPageValue("couponerror", true);
						return;
					}
					if (!coupon.isCustomerOk(inCart))
					{
						inReq.putPageValue("errorMessage", "That coupon is restricted only to certain customers.");
						inReq.putPageValue("couponerror", true);
						return;
					}
					if (coupon.hasExpired())
					{
						inReq.putPageValue("errorMessage", "That coupon has expired.");
						inReq.putPageValue("couponerror", true);
						return;
					}
					if (!coupon.isAcceptsMultiple())//more here...
					{
						for (Iterator iter = inCart.getItems().iterator(); iter.hasNext();)
						{
							CartItem olditem = (CartItem) iter.next();
							if (Coupon.isCoupon(olditem))
							{
								inReq.putPageValue("errorMessage", "That coupon is restricted to only one per cart.");
								inReq.putPageValue("couponerror", true);
								return;
							}
						}
					}
					//check minimum subtotal
					if (!coupon.isCartSubtotalOk(inCart))//doesn't seem right: maybe minimum subtotal of a product?
					{
						inReq.putPageValue("errorMessage", "That coupon can only be used on orders of " + new Money(minsubtotal) + " or more");
						inReq.putPageValue("couponerror", true);
						return;
					}
					// check product and minimum quantity
					if (!coupon.isCartItemsOk(inCart))
					{
						StringBuilder buf = new StringBuilder();
						buf.append("That coupon is restricted to only certain products ")
							.append(coupon.getProducts().toString().replace("[","(").replace("]", ")"));
						if (minquantity > 0)
						{
							buf.append(" with a minimum quantity of "+minquantity);
						}
						inReq.putPageValue("errorMessage", buf.toString());
						inReq.putPageValue("couponerror", true);
						return;
					}
					//percentage
					if (percentage > 0)
					{
						SaleAdjustment adjustment = new SaleAdjustment();
						if (productid!=null && !productid.isEmpty()) 
						{
							if (coupon.hasMultipleProducts()){
								adjustment.setProducts(productid);
								String affectedProduct = adjustment.findAdjustedProductId(inCart);
								adjustment.setProductId(affectedProduct);
							} else {
								adjustment.setProductId(productid);
							}
						}
						adjustment.setInventoryItemId(couponcode);
						adjustment.setPercentDiscount(percentage);
						inCart.addAdjustment(adjustment);
						
						CartItem cartItem = new CartItem();
						cartItem.setInventoryItem(inventoryItem);
						inCart.addItem(cartItem);
					}
					else if (discount > 0)
					{
						DiscountAdjustment adjustment = new DiscountAdjustment();
						if (productid!=null && !productid.isEmpty()) 
						{
							if (coupon.hasMultipleProducts()){
								adjustment.setProducts(productid);
								String affectedProduct = adjustment.findAdjustedProductId(inCart);
								adjustment.setProductId(affectedProduct);
							} else {
								adjustment.setProductId(productid);
							}
						}
						adjustment.setInventoryItemId(couponcode);
						adjustment.setDiscount(discount);
						inCart.addAdjustment(adjustment);
						
						CartItem cartItem = new CartItem();
						cartItem.setInventoryItem(inventoryItem);
						inCart.addItem(cartItem);
					}
					else
					{
						Money subtotal = inCart.getSubTotal();
						Money couponValue = inventoryItem.getYourPrice();
						
						//need to add couponadjustment here
						if ( (couponValue.doubleValue() * -1) > subtotal.doubleValue()){
							List<Adjustment> adjustments = inCart.getAdjustments();
							boolean addAdjustment = true;
							for (Adjustment adjust: adjustments){
								if (adjust instanceof CouponAdjustment){
									CouponAdjustment discadj = (CouponAdjustment) adjust;
									if (couponcode.equals(discadj.getInventoryItemId())){
										addAdjustment = false;
										break;
									}
								}
							}
							if (addAdjustment){
								Money adjustedprice = subtotal.add(couponValue);
								CouponAdjustment adjustment = new CouponAdjustment();
								if (coupon.hasMultipleProducts()){
									adjustment.setProducts(productid);
									String affectedProduct = adjustment.findAdjustedProductId(inCart);
									adjustment.setProductId(affectedProduct);
								} else {
									adjustment.setProductId(productid);
								}
								adjustment.setInventoryItemId(couponcode);
								adjustment.setDiscount(adjustedprice.doubleValue());
								inCart.addAdjustment(adjustment);
							}
						}
						
						CartItem cartItem = new CartItem();
						cartItem.setInventoryItem(inventoryItem);
						inCart.addItem(cartItem);
					}
				}
			}
			else
			{
				// no such coupon code
				inReq.putPageValue("errorMessage", "No such product");
			}
			// loop over the cart and make sure there is only one negative price
			// in it

		}
	}

	public Product createProductFromTemplate(User inUser, Store inStore, Product inTemplate) throws StoreException
	{
		String newId = inStore.getProductArchive().nextProductNumber();
		Product product = inTemplate.copy(newId);
		// product.setInventoryItems(null);

		if (inUser != null)
		{
			product.setProperty("user", inUser.getUserName());

		}
		inStore.saveProduct(product);
		return product;
	}

	protected Map readProperties(String code, Map params, Product product)
	{
		Map properties = new Hashtable();
		String start = "property" + code + ".";

		for (Iterator iter = params.keySet().iterator(); iter.hasNext();)
		{
			String paramid = (String) iter.next();
			String pid = null;

			if (paramid.startsWith(start))
			{
				pid = paramid.substring(start.length());
				String userinput = (String) params.get(paramid);
				if (userinput != null && userinput.trim().length() == 0)
				{
					userinput = null;
				}
				// Option option = makeOption(product, oid, userinput);

				if (userinput != null)
				{
					properties.put(pid, userinput);
				}
			}
		}

		/* Does this make sense? */
		/*
		 * start = "propertygroup" + code ; //the values are the option ID's for
		 * (Iterator iter = params.keySet().iterator(); iter.hasNext();) {
		 * String paramid = (String)iter.next(); if( paramid.startsWith(start))
		 * { String optionid = (String)params.get(paramid); Option option =
		 * makeOption(product, optionid, null); options.add(option); } }
		 */

		return properties;
	}
}
