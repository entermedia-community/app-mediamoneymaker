/*
 * Created on Jun 7, 2006
 */
package org.openedit.store;

import java.util.ArrayList;
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
		// Get item info from request parameters
		List olditems = new ArrayList(inCart.getItems());
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
			String checked = inReq.getRequestParameter("remove" + counter);
			if (checked != null)
			{
				continue;
			}

			Product product = inCart.getStore().getProduct(productId);

			if (product == null)
			{
				// log
				product = findProduct(productId, olditems);
				if (product == null)
				{
					continue;
				}
			}
			if (checked != null)
			{
				inCart.removeProduct(product);
			}
			int quantity = 0;
			String quantityStr = (String) params.get("quantity" + counter);
			if(i == 0){
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

			if (quantity <= 0)
			{
				//remove the product if quantity is less than or equal to zero
				//inCart.removeProduct(product);
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

			InventoryItem inventory = product.getInventoryItemByOptions(options);
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
			if (quantityspecified)
			{
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
		String coupon = inReq.getRequestParameter("couponcode");
		if (coupon != null && coupon.length() > 0)
		{
			// make sure there is no negative product in there already then add
			// the product ID in there
			// we might have to look up the producinCartt ID up since W@W needs
			// that
			// use Lucene to search
			Store store = inCart.getStore();
			HitTracker hits = store.getProductSearcher().fieldSearch("items", coupon);
			if (hits.getTotal() > 0)
			{
				Data hit = (Data) hits.get(0);
				String productId = hit.get("id");
				Product product = store.getProduct(productId);
				if (product != null)
				{
					// Check the rules. This might have a dollar amount in it
					String min = product.getProperty("fldDesc3");
					if (min != null)
					{
						// do a check
						double val = inCart.getSubTotal().doubleValue();
						Money minmoney = new Money(min);
						if (val < minmoney.doubleValue())
						{
							inReq.putPageValue("errorMessage", "That coupon can only be used on orders of " + minmoney.toString() + " or more");
							return;
						}
					}

					InventoryItem inventoryItem = product.getInventoryItemBySku(coupon);
					if (inventoryItem == null || !inventoryItem.isInStock())
					{
						inReq.putPageValue("couponerror", true);
						return;
					}
					if (inventoryItem != null && inventoryItem.isInStock())
					{
						String percentage = inventoryItem.getProperty("percentage");

						if (percentage != null)
						{
							if (inCart.getAdjustments().size() == 0)
							{
								Double percent = Double.parseDouble(percentage);

								SaleAdjustment adjustment = new SaleAdjustment();
								adjustment.setPercentDiscount(percent);

								inCart.addAdjustment(adjustment);
								CartItem cartItem = new CartItem();
								cartItem.setInventoryItem(inventoryItem);
								inCart.addItem(cartItem);
							}
						}
						else
						{
							CartItem cartItem = new CartItem();
							cartItem.setInventoryItem(inventoryItem);
							// remove any other coupons
							for (Iterator iter = inCart.getItems().iterator(); iter.hasNext();)
							{
								CartItem olditem = (CartItem) iter.next();
								if (olditem.getYourPrice().isNegative())
								{
									inCart.removeItem(olditem);
									break;
								}
							}
							inCart.addItem(cartItem);
							/*
							 * //I need more than 50% :) if
							 * (cartItem.getYourPrice().doubleValue() * 2 <
							 * inCart .getSubTotal().doubleValue()) {
							 * inCart.addItem(cartItem); } else { inReq
							 * .putPageValue("errorMessage",
							 * "Coupons may not be more than 50% off the total price"
							 * ); }
							 */
						}
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
