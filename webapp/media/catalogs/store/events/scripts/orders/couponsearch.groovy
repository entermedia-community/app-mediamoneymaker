import org.openedit.store.CartItem
import org.openedit.store.Coupon
import org.openedit.store.Store
import org.openedit.store.orders.Order
import org.openedit.store.orders.OrderId


public void init(){
	String couponcode = context.getRequestParameter("couponcode");
	if (couponcode != null){
		couponcode = couponcode.trim();
	}
	else {
		return;
	}
	log.info("### searching for $couponcode")
	Store store = (Store) context.getPageValue("store");
	List<String> orderIds = new ArrayList<String>();
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
			if (Coupon.isCoupon(cartItem) && cartItem.getProduct()!=null && cartItem.getProduct())
			{
				String sku = cartItem.getProduct().getInventoryItemBySku(couponcode);
				if (sku!=null && sku.trim().equalsIgnoreCase(couponcode)){
					orderIds.add(id);
					continue;
				}
			}
		}
	}
	log.info("### $orderIds");
	
}


init();
