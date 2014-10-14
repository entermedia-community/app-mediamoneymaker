package org.openedit.store;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.openedit.money.Money;
import org.openedit.store.adjustments.Adjustment;
import org.openedit.store.adjustments.CouponAdjustment;
import org.openedit.store.adjustments.DiscountAdjustment;
import org.openedit.store.adjustments.SaleAdjustment;
import org.openedit.store.orders.Order;
import org.openedit.store.orders.OrderId;
import org.openedit.util.DateStorageUtil;

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
	
	public List<String> getProducts(){
		List<String> list = new ArrayList<String>();
		String productid = getProductId();
		if (productid!=null && !productid.isEmpty()){
			StringTokenizer tok = new StringTokenizer(productid,"|");
			while(tok.hasMoreTokens())
			{
				list.add(tok.nextToken().trim());
			}
		}
		return list;
	}
	
	public boolean hasProductRestrictions(){
		return getProductId() != null && !getProductId().isEmpty();
	}
	
	public boolean hasMultipleProducts(){
		return (getProducts().size() > 1);
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
		int minquantity = getMininumProductQuantity();
		if ( hasProductRestrictions() || minquantity > 0 )
		{
			List<String> products = getProducts();
			boolean isproductpresent = false;
			boolean isminimumok = (minquantity > 0) ? false : true;
			for (Iterator<?> iter = inCart.getItems().iterator(); iter.hasNext();)
			{
				CartItem item = (CartItem) iter.next();
				if (isCoupon(item) || item.getProduct() == null){
					continue;
				}
				String productid = item.getProduct().getId();
				if (products.contains(productid)){
					isproductpresent = true;
					if (minquantity > 0 && item.getQuantity() < minquantity){
						continue;
					}
					isminimumok = true;
				}
			}
			
			if (!isproductpresent)
			{
				return false;
			} else {
				if (!isminimumok){
					return false;
				}
			}
		}
		return true;
	}
	
//	public void removeCartAdjustment(Cart inCart)
//	{
//		Iterator<?> itr = inCart.getAdjustments().iterator();
//		while (itr.hasNext())
//		{
//			Adjustment adjust = (Adjustment) itr.next();
//			String inventoryid = null;
//			String adjproduct = null;
//			if (adjust instanceof SaleAdjustment){
//				inventoryid = ((SaleAdjustment) adjust).getInventoryItemId();
//				adjproduct = ((SaleAdjustment) adjust).getProductId();
//			} else if (adjust instanceof DiscountAdjustment){
//				inventoryid = ((DiscountAdjustment) adjust).getInventoryItemId();
//				adjproduct = ((DiscountAdjustment) adjust).getProductId();
//			} else {
//				continue;
//			}
//			if (inventoryid!=null){
//				if (getInventoryItem().getSku()!=null && getInventoryItem().getSku().equals(inventoryid)){
//					inCart.getAdjustments().remove(adjust);//check inventory sku first
////					return;
//				}
//			}
//			if (adjproduct!=null)
//			{
//				String productid = getProductId();
//				if (productid!=null && productid.equals(adjproduct)){
//					inCart.getAdjustments().remove(adjust);
////					return;//check product id if inventory sku isn't found
//				}
//			}
//		}
//	}
	
//	public static void removeOldAdjustmentsAndCoupons(Cart inCart)
//	{
//		List<Adjustment> adjustments = new ArrayList<Adjustment>();
//		List<CartItem> coupons = new ArrayList<CartItem>();
//		Iterator<?> itr = inCart.getAdjustments().iterator();
//		while (itr.hasNext())
//		{
//			Adjustment adjustment = (Adjustment) itr.next();
//			String productid = null;
//			String couponsku = null;
//			if (adjustment instanceof SaleAdjustment)
//			{
//				productid = ((SaleAdjustment) adjustment).getProductId();
//				couponsku = ((SaleAdjustment) adjustment).getInventoryItemId();
//			}
//			else if (adjustment instanceof DiscountAdjustment)
//			{
//				productid = ((DiscountAdjustment) adjustment).getProductId();
//				couponsku = ((DiscountAdjustment) adjustment).getInventoryItemId();
//			}
//			if (productid == null || productid.isEmpty())
//			{
//				continue;
//			}
//			boolean isPresent = false;
//			Iterator<?> itr2 = inCart.getItems().iterator();
//			while(itr2.hasNext())
//			{
//				CartItem item = (CartItem) itr2.next();
//				if (Coupon.isCoupon(item) && couponsku!=null && item.getProduct().getInventoryItemBySku(couponsku)!=null)
//				{
//					coupons.add(item);
//				}
//				if (item.getProduct().getId().equals(productid))
//				{
//					isPresent = true;
//				}
//			}
//			if (!isPresent)
//			{
//				adjustments.add(adjustment);
//			}
//		}
//		for (Adjustment adjustment:adjustments)
//		{
//			inCart.getAdjustments().remove(adjustment);
//		}
//		for (CartItem coupon:coupons)
//		{
//			inCart.removeItem(coupon);
//		}
//		if (inCart.isEmpty())
//		{
//			inCart.getAdjustments().clear();
//		}
//	}
	
	public static void recalculateAdjustments(Cart inCart){
		//adds all Cart Adjustments associated with Coupon to the cart
		inCart.getAdjustments().clear();
		List<CartItem> coupons = getAllCoupons(inCart);
		for(CartItem couponitem:coupons){
			Coupon coupon = new Coupon(couponitem.getInventoryItem());
			//percentage, discount + sku (ie. coupon code)
			String sku = couponitem.getInventoryItem().getSku();
			double percentage = coupon.getPercentage();
			double discount = coupon.getDiscount();
			//go through list of affected products
			List<CartItem> affecteditems = coupon.getAllAffectedCartItems(inCart);
			for(CartItem item:affecteditems){//need to add an adjustment for each affected cart item
				String productid = item.getProduct().getId();//affected product id
				if (percentage > 0){
					SaleAdjustment adjustment = new SaleAdjustment();
					adjustment.setProductId(productid);
					adjustment.setInventoryItemId(sku);
					adjustment.setPercentDiscount(percentage);
					inCart.addAdjustment(adjustment);
				} else if (discount > 0){
					DiscountAdjustment adjustment = new DiscountAdjustment();
					adjustment.setProductId(productid);
					adjustment.setInventoryItemId(sku);
					adjustment.setDiscount(discount);
					inCart.addAdjustment(adjustment);
				} else {
					Money subtotal = inCart.getSubTotal();
					Money couponValue = coupon.getInventoryItem().getYourPrice();
					if ( (couponValue.doubleValue() * -1) > subtotal.doubleValue()){
						Money adjustedprice = subtotal.add(couponValue);
						CouponAdjustment adjustment = new CouponAdjustment();
						adjustment.setProductId(productid);
						adjustment.setInventoryItemId(sku);
						adjustment.setDiscount(adjustedprice.doubleValue());
						inCart.addAdjustment(adjustment);
					}
				}
			}
		}
	}
	
	public List<CartItem> getAllAffectedCartItems(Cart inCart){
		List<CartItem> list = new ArrayList<CartItem>();
		List<String> productIds = getProducts();
		Iterator<?> itr = inCart.getItems().iterator();
		while (itr.hasNext()){
			CartItem item = (CartItem) itr.next();
			if (Coupon.isCoupon(item) || item.getProduct() == null){
				continue;
			}
			if (productIds.contains(item.getProduct().getId())){
				int minquantity = getMininumProductQuantity();
				if (minquantity > 0 && item.getQuantity() < minquantity){
					continue;
				}
				list.add(item);
			}
		}
		return list;
	}
	
	public static List<CartItem> getAllCoupons(Cart inCart){
		List<CartItem> list = new ArrayList<CartItem>();
		Iterator<?> itr = inCart.getItems().iterator();
		while (itr.hasNext()){
			CartItem item = (CartItem) itr.next();
			if (Coupon.isCoupon(item)){
				list.add(item);
			}
		}
		return list;
	}
}
