package org.openedit.store;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.openedit.money.Money;
import org.openedit.store.adjustments.Adjustment;
import org.openedit.store.adjustments.DiscountAdjustment;
import org.openedit.store.adjustments.SaleAdjustment;
import org.openedit.store.orders.OrderId;
import org.openedit.util.DateStorageUtil;
import org.openedit.store.orders.Order;

import com.openedit.WebPageRequest;

public class Coupon {
	
	protected InventoryItem fieldInventoryItem;
	
	public Coupon(InventoryItem inItem)
	{
		setInventoryItem(inItem);
	}
	
	public static boolean isCoupon(CartItem inCartItem)
	{
		if (inCartItem == null) return false;
		return isCoupon(inCartItem.getInventoryItem());
	}
	
	public static boolean isCoupon(InventoryItem inItem)
	{
		if (inItem == null) return false;
		return isCoupon(inItem.getProduct());
	}
	
	public static boolean isCoupon(Product inProduct)
	{
		if (inProduct == null) return false;
		return (inProduct.getProperty("producttype")!=null && inProduct.getProperty("producttype").equals("coupon"));
	}
	
	public boolean isSingleValueCoupon()
	{
		if (getPercentage() > 0)
		{
			return false;
		}
		if (getDiscount() > 0)
		{
			return false;
		}
		return true;
	}
	
	public void setInventoryItem(InventoryItem inItem)
	{
		fieldInventoryItem = inItem;
	}
	
	public InventoryItem getInventoryItem()
	{
		return fieldInventoryItem;
	}
	
	public double getPercentage()
	{
		double percentage = 0.0d;
		if (getInventoryItem().getProperty("percentage")!=null && !getInventoryItem().getProperty("percentage").isEmpty())
		{
			percentage = Double.parseDouble(getInventoryItem().getProperty("percentage"));
		}
		return percentage;
	}
	
	public double getDiscount() {
		double discount = 0.0d;
		if (getInventoryItem().getProperty("discount")!=null && !getInventoryItem().getProperty("discount").isEmpty())
		{
			discount = Double.parseDouble(getInventoryItem().getProperty("discount"));
		}
		return discount;
	}
	
	public String getProductId()
	{
		String productid = getInventoryItem().getProperty("product");
		return productid;
	}
	
	public int getMininumProductQuantity()
	{
		int minquantity = -1;
		if (getInventoryItem().getProperty("minquantity")!=null && !getInventoryItem().getProperty("minquantity").isEmpty())
		{
			minquantity = Integer.parseInt(getInventoryItem().getProperty("minquantity"));
		}
		return minquantity;
	}
	
	public double getMinimumSubtotal()
	{
		double minsubtotal = 0.0d;
		if (getInventoryItem().getProperty("minsubtotal") !=null && !getInventoryItem().getProperty("minsubtotal").isEmpty())
		{
			minsubtotal = Double.parseDouble(getInventoryItem().getProperty("minsubtotal"));
		}
		return minsubtotal;
	}
	
	public Date getCouponExpiry()
	{
		Date expiry = DateStorageUtil.getStorageUtil().parseFromStorage(getInventoryItem().getProperty("expirydate"));
		return expiry;
	}
	
	public boolean isAcceptsMultiple()
	{
		boolean multiple = Boolean.parseBoolean(getInventoryItem().getProperty("acceptmultiple"));
		return multiple;
	}
	
	public boolean isOnePerUser()
	{
		boolean oneperuser = Boolean.parseBoolean(getInventoryItem().getProperty("oneperuser"));
		return oneperuser;
	}
	
	public String getUserRestriction()
	{
		String user = getInventoryItem().getProperty("restricttouser");
		return user;
	}
	
	public List<String> getSiteRestrictions()
	{
		List<String> list = new ArrayList<String>();
		String sites = getInventoryItem().getProperty("siteredemption");
		if (sites!=null && !sites.isEmpty()){
			StringTokenizer tok = new StringTokenizer(sites,"|");
			while(tok.hasMoreTokens())
			{
				list.add(tok.nextToken().trim());
			}
		}
		return list;
	}
	
	public boolean isSiteAllowed(WebPageRequest inReq)
	{
		List<String> sites = getSiteRestrictions();
		if (!sites.isEmpty())
		{
			StringBuffer buf = inReq.getRequest().getRequestURL();
			Iterator<String> itr = sites.iterator();
			boolean isFound = false;
			while (itr.hasNext())
			{
				if (buf.toString().contains(itr.next()))
				{
					isFound = true;
					break;
				}
			}
			return isFound;
		}
		return true;
	}
	
	public boolean hasExpired()
	{
		Date expiry = getCouponExpiry();
		return (expiry != null && (new Date()).after(expiry));
	}
	
	public boolean isInStock()
	{
		return getInventoryItem().isInStock();
	}
	
	public boolean isCustomerOk(Cart inCart)
	{
		String user = getUserRestriction();
		return ( user == null || user.isEmpty() || (inCart.getCustomer()!=null && inCart.getCustomer().getId().equals(user)) );
	}
	
	public boolean hasCustomerUsedCoupon(WebPageRequest inReq, Cart inCart)
	{
		if (!isOnePerUser())
		{
			return false;
		}
		String inventoryItemSku = this.getInventoryItem().getSku();
		if (inventoryItemSku == null)
		{
			return false;
		}
		if (inCart.getCustomer()==null)
		{
			return false;
		}
		String user = inCart.getCustomer().getId();
		Store store = (Store) inReq.getPageValue("store");
		List<?> ids = store.getOrderArchive().listAllOrderIds(store);
		for(Object id:ids){
			Order order = (Order) store.getOrderSearcher().searchById(((OrderId)id).getOrderId());
			if (order == null || order.getItems() == null)
			{
				continue;
			}
			List<?> cartItems = order.getItems();
			for(Object item:cartItems){
				CartItem cartItem = (CartItem) item;
				if (isCoupon(cartItem) && cartItem.getInventoryItem()!=null)
				{
					String sku = cartItem.getInventoryItem().getSku();
					if (sku!=null && sku.equals(inventoryItemSku))
					{
						if (order.getCustomer().getId().equals(user))
						{
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	
	public boolean isCartSubtotalOk(Cart inCart)
	{
		double minsubtotal = getMinimumSubtotal();
		Money minmoney = new Money(minsubtotal);
		Money subtotal = inCart.getSubtotalWithoutCoupons();
		return (subtotal.doubleValue() >= minmoney.doubleValue());
	}

	public boolean isCartItemsOk(Cart inCart) {
		String productid = getProductId();
		int minquantity = getMininumProductQuantity();
		boolean checkproduct = ( productid!=null && !productid.isEmpty() );
		boolean checkquantity = ( minquantity > 0 ); 
		if ( checkproduct || checkquantity )
		{
			boolean isproductpresent = false;
			for (Iterator iter = inCart.getItems().iterator(); iter.hasNext();)
			{
				CartItem olditem = (CartItem) iter.next();
				if (isProductMatch(olditem))
				{
					isproductpresent = true;
					if (!isProductQuantityOk(olditem))
					{
						return false;
					}
				}
			}
			if (!isproductpresent)
			{
				return false;
			}
		}
		return true;
	}
	
	public boolean isProductMatch(CartItem inItem)
	{
		String productid = getProductId();
		if (isCoupon(inItem) || productid == null || productid.isEmpty())
		{
			return false;
		}
		return inItem.getProduct()!=null && inItem.getProduct().getId().equals(productid);
	}
	
	public boolean isProductQuantityOk(CartItem inItem)
	{
		int minquantity = getMininumProductQuantity();
		if (isProductMatch(inItem) && minquantity > 0)
		{
			return (inItem.getQuantity() >= minquantity);
		}
		return true;
	}
	
	public void removeCartAdjustment(Cart inCart)
	{
		Iterator<?> itr = inCart.getAdjustments().iterator();
		while (itr.hasNext())
		{
			Adjustment adjust = (Adjustment) itr.next();
			String inventoryid = null;
			String adjproduct = null;
			if (adjust instanceof SaleAdjustment){
				inventoryid = ((SaleAdjustment) adjust).getInventoryItemId();
				adjproduct = ((SaleAdjustment) adjust).getProductId();
			} else if (adjust instanceof DiscountAdjustment){
				inventoryid = ((DiscountAdjustment) adjust).getInventoryItemId();
				adjproduct = ((DiscountAdjustment) adjust).getProductId();
			} else {
				continue;
			}
			if (inventoryid!=null){
				if (getInventoryItem().getSku()!=null && getInventoryItem().getSku().equals(inventoryid)){ //.getProperty("sku")
					inCart.getAdjustments().remove(adjust);//check inventory sku first
					return;
				}
			}
			if (adjproduct!=null)
			{
				String productid = getProductId();
				if (productid!=null && productid.equals(adjproduct)){
					inCart.getAdjustments().remove(adjust);
					return;//check product id if inventory sku isn't found
				}
			}
		}
	}	
}
