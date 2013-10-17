/*
 * Created on Jan 17, 2005
 */
package org.openedit.store.modules;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.entermedia.MediaArchive;
import org.openedit.event.WebEventListener;
import org.openedit.money.Fraction;
import org.openedit.money.Money;
import org.openedit.store.CartItem;
import org.openedit.store.Product;
import org.openedit.store.ShippingMethod;
import org.openedit.store.Store;
import org.openedit.store.TaxRate;
import org.openedit.store.customer.Address;
import org.openedit.store.orders.EmailOrderProcessor;
import org.openedit.store.orders.Order;
import org.openedit.store.orders.OrderArchive;
import org.openedit.store.orders.OrderId;
import org.openedit.store.orders.OrderSearcher;
import org.openedit.store.orders.OrderSet;
import org.openedit.store.orders.OrderState;
import org.openedit.store.orders.Refund;
import org.openedit.store.orders.RefundItem;
import org.openedit.store.orders.RefundState;
import org.openedit.store.orders.SubmittedOrder;
import org.openedit.util.DateStorageUtil;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.modules.BaseModule;

/**
 * @author dbrown
 * 
 */
public class OrderModule extends BaseModule
{
	protected static final String ORDERMODULE = "ordermodule";
	protected static final String ORDERLIST = "orderlist";
	protected static final String ORDERIDLIST = "orderidlist";
	protected static final String ORDERLISTLASTMODIFIED = "orderlistlastmodified";
	protected static final String CUSTOMERLIST = "customerlist";
	protected static final String ITEMLIST = "itemlist";
    protected EmailOrderProcessor fieldEmailOrderProcessor;
	protected WebEventListener fieldWebEventListener;
	// protected OrderSearcher fieldOrderSearcher;
	// private Map fieldCurrentOrder;
	/*
	 * public List getCustomerList( WebPageRequest inContext ) throws Exception {
	 * getOrderList( inContext ); List orderList = (List)inContext.getPageValue(
	 * ORDERLIST ); List customerList = new ArrayList(); List idList = new
	 * ArrayList(); for ( Iterator iter = orderList.iterator(); iter.hasNext(); ) {
	 * SubmittedOrder order = (SubmittedOrder) iter.next(); String id =
	 * order.getCustomer().getUserName(); if ( idList.contains( id ) ) {
	 * continue; } idList.add(id); customerList.add( order.getCustomer() ); }
	 * inContext.putPageValue( CUSTOMERLIST, customerList ); return
	 * customerList; }
	 */

	public WebEventListener getWebEventListener()
	{
		return fieldWebEventListener;
	}
	public void setWebEventListener(WebEventListener inWebEventListener)
	{
		fieldWebEventListener = inWebEventListener;
	}
	public SubmittedOrder getOrderFromNumber(WebPageRequest inContext, List inOrderIdList,
			String inOrderNumber) throws Exception
	{
		Store store = getStore(inContext);
		if (inOrderNumber != null)
		{
			SubmittedOrder order = (SubmittedOrder)store.getOrderSearcher().searchById(inOrderNumber);
			if( order == null)
			{
				//legacy
				for (Iterator it = inOrderIdList.iterator(); it.hasNext();)
				{
					OrderId orderid = (OrderId) it.next();
					if (inOrderNumber.equals(orderid.getOrderId()))
					{
						order = store.getOrderArchive().loadSubmittedOrder(store, orderid.getUsername(),
								orderid.getOrderId());
						break;
					}
				}
			}
			return order;
		}
		return null;
	}
	public SubmittedOrder loadOrderById(WebPageRequest inContext) 
	{
		String ordernum = inContext.getRequestParameter("orderid");
		if(ordernum == null){
			ordernum = inContext.getRequestParameter("id");
		}
		String customerid = inContext.getRequestParameter("customerid");
		if (ordernum != null)
		{
			Store store = getStore(inContext);
			SubmittedOrder order = null;
			if( customerid ==null)
			{
				order = (SubmittedOrder)store.getOrderSearcher().searchById(ordernum);
			}
			else
			{
				order = store.getOrderArchive().loadSubmittedOrder(store, customerid, ordernum);
			}
			inContext.putPageValue("order",order);
			return order;
		}
		return null;
	}
	public void loadPageOfSearch(WebPageRequest inReq) throws Exception
	{
		Store store = getStore(inReq);
		store.getOrderSearcher().loadPageOfSearch(inReq);
	}

	
	public HitTracker getOrdersForUser(WebPageRequest inReq) throws Exception
	{
		String pagenum = inReq.getRequestParameter("page");
		Store store = getStore(inReq);
		if (pagenum == null)
		{
			HitTracker orders = store.getOrderSearcher().fieldSearchForUser(inReq, inReq.getUser());
			inReq.putPageValue("orderlist", orders);
			return orders;
		} else
		{
			return store.getOrderSearcher().loadPageOfSearch(inReq);
		}
	}

	public HitTracker getOrdersForAll(WebPageRequest inReq) throws Exception
	{
		Store store = getStore(inReq);
		HitTracker orders = store.getOrderSearcher().fieldSearch(inReq);
		inReq.putPageValue(ORDERIDLIST, orders);
		return orders;

	}

	public List getOrderIdList(WebPageRequest inReq) throws Exception
	{
		Store store = getStore(inReq);
		List orderIds = store.getOrderSearcher().listOrders(store, inReq);

		inReq.putPageValue(ORDERIDLIST, orderIds);
		return orderIds;

	}
	
	public void changeOrderStatus(WebPageRequest inContext) throws Exception
	{
		String id = inContext.getRequestParameter("orderstatus");
		String orderNumber = inContext.getRequestParameter("ordernumber");
		if (id != null && orderNumber != null)
		{
			SubmittedOrder order = getOrderFromNumber(inContext, getOrderIdList(inContext),
					orderNumber);
			Store store = getStore(inContext);

			OrderState state = new OrderState();
			state.setId(id);

			OrderArchive archive = store.getOrderArchive();
			archive.changeOrderStatus(state, store, order);
			order.setOrderState(state);
			store.getOrderSearcher().updateIndex(order);
		}

	}

	public void changeItemStatus(WebPageRequest inContext) throws Exception
	{
		String orderNumber = inContext.getRequestParameter("ordernumber");
		SubmittedOrder order = getOrderFromNumber(inContext, getOrderIdList(inContext), orderNumber);

		String sku[] = inContext.getRequestParameters("sku");

		for (int i = 0; i < sku.length; i++)
		{
			String id = sku[i];
			String newstatus = inContext.getRequestParameter(id + ".status");
			if (newstatus != null)
			{
				CartItem item = order.getItem(id);
				item.setStatus(newstatus);
			}

		}

		Store store = getStore(inContext);

		
		store.saveOrder(order);
		store.getOrderSearcher().updateIndex(order);
	}

	public void captureOrder(WebPageRequest inContext) throws Exception
	{
		String orderNumber = inContext.getRequestParameter("ordernumber");
		SubmittedOrder order = getOrderFromNumber(inContext, getOrderIdList(inContext), orderNumber);
		Store store = getStore(inContext);

		OrderArchive archive = store.getOrderArchive();
		archive.captureOrder(inContext, store, order);
		inContext.putPageValue("order", order);
	}

	protected Store getStore(WebPageRequest inContext)
	{
		CartModule cartm = (CartModule) getModuleManager().getModule("CartModule");
		return cartm.getStore(inContext);
	}

	protected File getOrdersDirectory(Store inStore)
	{
		return new File(inStore.getStoreDirectory(), "orders");
	}
	
	
	public Order loadOrder(WebPageRequest inRequest) throws Exception
	{
		String id = inRequest.getRequestParameter("id");
		if(id != null){
			Store store = getStore(inRequest);
			Order order = (Order) store.getOrderSearcher().searchById(id);
			inRequest.putPageValue("order", order);
			return order;
		}
		
		String path = inRequest.getPath();
		String extension = inRequest.getCurrentAction().getConfig().getAttribute( "extension" );
		if ( extension == null )
		{
			extension = "html";
		}
		if ( !extension.startsWith( "." ) )
		{
			extension = "." + extension;
		}
		if (path.endsWith( extension ))
		{
			
			String orderNumber = path.substring(path.lastIndexOf("/") + 1, path
					.lastIndexOf( extension ));
			Store store = getStore(inRequest);
			
			Order order = (Order) store.getOrderSearcher().searchById(orderNumber);
			inRequest.putPageValue("order", order);
			return order;
		}
		return null;
	}
	
	
	public void loadOrderSet(WebPageRequest inReq){
		String ordersetid = inReq.getRequestParameter("setid");
		//search for all orders in this set
		//create order set object
		//add orders to it
		
	}

	// public OrderSearcher getOrderSearcher()
	// {
	// if (fieldOrderSearcher == null)
	// {
	// setOrderSearcher(new OrderSearcher());
	// }
	// return fieldOrderSearcher;
	//
	// }
	// public void setOrderSearcher(OrderSearcher inOrderSearcher)
	// {
	// fieldOrderSearcher = inOrderSearcher;
	// }

	public void reIndexOrders(WebPageRequest inReq) throws Exception
	{
		Store store = getStore(inReq);
		store.getOrderSearcher().reIndexAll();
	}

	/*
	 * Gets an order based on the request parameter "orderid". Users with "Store
	 * Manager" permission can get any order. Other users can only get their
	 * orders.
	 * 
	 * Input: Request parameter "orderid" Output: Page value "order", if
	 * successfull
	 */
	public void selectOrder(WebPageRequest inContext) throws Exception
	{
		// order = getStore(inContext).getOrderSearcher().searchById(id);
		// .loadSubmittedOrder(getStore(inContext), inContext.getUserName(),
		// id);

		SubmittedOrder order = null;
		String id = inContext.getRequestParameter("orderid");
		if (id != null && inContext.getUser().hasPermission("oe.cart.admin"))
		{
			order = getOrderFromNumber(inContext, getOrderIdList(inContext), id);
		} else if (id != null)
		{
			order = getStore(inContext).getOrderArchive().loadSubmittedOrder(getStore(inContext),
					inContext.getUserName(), id);
		}
		if (order != null)
		{
			inContext.putSessionValue("selectedorder" + inContext.getUserName(), order);
			inContext.putPageValue("order", order);
		}
	}
	
	/**
	 * Recovers the working order for a user.
	 * 
	 * Input: nothing. Output: the working order, as a return value and page
	 * value "order". If null no page value is given.
	 */
	public SubmittedOrder getCurrentOrder(WebPageRequest inContext)
	{
		SubmittedOrder order = (SubmittedOrder) inContext.getSessionValue("selectedorder"
				+ inContext.getUserName());
		if (order != null)
		{
			inContext.putPageValue("order", order);
		}
		return order;
	}
	
	public void exportOrders(WebPageRequest inContext) throws OpenEditException
	{
		try
		{
			getOrdersForAll(inContext);
			HitTracker orders = (HitTracker) inContext.getPageValue(ORDERIDLIST);
			if (orders == null)
				return;
			StringBuffer doc = new StringBuffer();
			Store store = getStore(inContext);
			String delim = inContext.getRequestParameter("delim");
			if (delim == null)
				delim = "\t";

			doc.append("Order number");
			doc.append(delim + "Order date");
			doc.append(delim + "Customer name");
			doc.append(delim + "Address");
			doc.append(delim + "Quantity");
			doc.append(delim + "Total price");
			doc.append(delim + "Order status");
			doc.append("\n");

			for (Iterator i = orders.getAllHits(); i.hasNext();)
			{
				Document orderhit = (Document) i.next();
				SubmittedOrder order = store.getOrderArchive().loadSubmittedOrder(store,
						orderhit.get("customer"), orderhit.get("id"));
				doc.append(order.getId());
				doc.append(delim + order.getDateOrdered());
				doc.append(delim + order.getCustomer().getFirstName() + " "
						+ order.getCustomer().getLastName());
				Address address = order.getCustomer().getShippingAddress();
				if (address == null)
				{
					address = order.getCustomer().getBillingAddress();
				}
				String addressString;
				if (address.getCity() == null)
				{
					addressString = "";
				} else
				{
					addressString = address.getCity() + ", " + address.getZipCode();
				}

				doc.append(delim + addressString);
				doc.append(delim + order.getNumItems());
				doc.append(delim + order.getTotalPrice());
				doc.append(delim + order.getOrderStatus().getDescription());
				doc.append("\n");
			}

			inContext.getResponse().addHeader("Content-Type", "text/plain");
			inContext.getWriter().write(doc.toString());
			inContext.getWriter().close();
		} catch (Exception e)
		{
			throw new OpenEditException(e);
		}
	}
	
	
	public void sendReceipt(WebPageRequest inReq)
	{
		Order ord = loadOrderById(inReq);
		getEmailOrderProcessor().sendReceipt(inReq, getStore(inReq), ord);
	
	}

	public EmailOrderProcessor getEmailOrderProcessor() {
		return fieldEmailOrderProcessor;
	}

	public void setEmailOrderProcessor(
			EmailOrderProcessor fieldEmailOrderProcessor) {
		this.fieldEmailOrderProcessor = fieldEmailOrderProcessor;
	}
	
	public void prepareNewRefund(WebPageRequest inContext)
	{
		String ordernumber = inContext.getRequestParameter("ordernumber");
		Store store = getStore(inContext);
		Order order = (Order) store.getOrderSearcher().searchById(ordernumber);
		
		inContext.putPageValue("data",order);
		inContext.putPageValue("order",order);
	}
	
	public void prepareRefund(WebPageRequest inContext){
		String ordernumber = inContext.getRequestParameter("ordernumber");
		Store store = getStore(inContext);
		Order order = (Order) store.getOrderSearcher().searchById(ordernumber);
		
		resetPendingRefundStates(order);
		
		Money subtotal = new Money("0");
		Money shipping = new Money("0");
		
		Money totaltaxes = new Money("0");
		Money total = new Money("0");
		
		String [] skus = inContext.getRequestParameters("sku");
		for (String sku:skus)
		{
			//shipping-refund
			if (inContext.getRequestParameter(sku+"-refund")==null ||
					!inContext.getRequestParameter(sku+"-refund").equals("on") ||
					inContext.getRequestParameter(sku+"-refund-quantity") == null)
			{
				continue;
			}
			
			String quantity = inContext.getRequestParameter(sku+"-refund-quantity");
			if (sku.equals("shipping"))
			{
				ShippingMethod shippingmethod = order.getShippingMethod();
				Money shippingcost = shippingmethod.getCost();
				Money shippingrefund = new Money(quantity);
				if (shippingcost.doubleValue() < shippingrefund.doubleValue())
				{
//					System.out.println("OrderModule - Shipping cost too large - skipping!");
					continue;
				}
				shipping = shippingrefund;
			}
			else
			{
				int intQuantity = Integer.parseInt(quantity);
				CartItem cartItem = order.getItem(sku);
				cartItem.getRefundState().setPendingQuantity(intQuantity);
				cartItem.getRefundState().setRefundStatus(RefundState.REFUND_PENDING);
				
				Money price = cartItem.getYourPrice();
				Money totalPrice = price.multiply(intQuantity);
				cartItem.getRefundState().setPendingPrice(totalPrice);
				
				subtotal = subtotal.add(totalPrice);
				
			}
		}
		
		total = total.add(subtotal);
		ArrayList<String[]> taxes = new ArrayList<String[]>();
		//list of taxes applied to order
		Map<TaxRate,Money> taxMap = order.getTaxes();//map of TaxRate,Money
		Iterator<TaxRate> keys = taxMap.keySet().iterator();
		while(keys.hasNext())
		{
			TaxRate taxRate = keys.next();
			Fraction fraction = taxRate.getFraction();//actual tax rate, like 0.1300 for HST
			Money tally = new Money(subtotal.toShortString());
			if (!shipping.isZero() && taxRate.isApplyToShipping())
			{
				tally = tally.add(shipping);
			}
			Money amount = tally.multiply(fraction);
			totaltaxes = totaltaxes.add(amount);
			String [] taxinfo =  {taxRate.getName(),amount.toString(),fraction.toString()};
			taxes.add(taxinfo);
			
			total = total.add(amount);
		}
		
		if(!shipping.isZero())
		{
			subtotal = subtotal.add(shipping);// add the shipping cost to the subtotal
			total = total.add(shipping);//add the shipping cost to the total
		}
		
		inContext.putPageValue("data",order);
		inContext.putPageValue("order",order);
		inContext.putPageValue("subtotal", subtotal);
		inContext.putPageValue("taxes",taxes);//taxes for the items refunded (+ shipping)
		inContext.putPageValue("totaltaxes",totaltaxes);//total taxes for the items refunded (+ shipping)
		if (!shipping.isZero())
		{
			inContext.putPageValue("shipping", shipping);
		}
		inContext.putPageValue("total", total);
	}
	
	protected void resetPendingRefundStates(Order inOrder)
	{
		updateRefundStates(inOrder,null,true);
	}
	
	protected void updateRefundStates(Order inOrder, Refund inRefund, boolean reset)
	{
		Iterator<?> itr = inOrder.getItems().iterator();
		while(itr.hasNext())
		{
			CartItem cartItem = (CartItem) itr.next();
			if (reset)
			{
				if (cartItem.getRefundState().getRefundStatus().equals(RefundState.REFUND_PENDING))
				{
					cartItem.getRefundState().setPendingPrice(new Money("0"));
					cartItem.getRefundState().setPendingQuantity(0);
					cartItem.getRefundState().setRefundStatus(RefundState.REFUND_NIL);
				}
			}
			else if (inRefund!=null)
			{
				String sku = cartItem.getSku();
				Iterator<RefundItem> itr2 = inRefund.getItems().iterator();
				while (itr2.hasNext())
				{
					RefundItem refundItem = itr2.next();
					if (!refundItem.isShipping() && refundItem.getId()!=null && refundItem.getId().equals(sku))
					{
						
						if (inRefund.isSuccess())//refund has to be successful to update the quantities of the cart items
						{
							int pendingQuantity = cartItem.getRefundState().getPendingQuantity();
							int quantity = cartItem.getRefundState().getQuantity();
							cartItem.getRefundState().setQuantity(pendingQuantity+quantity);
							cartItem.getRefundState().setRefundStatus(RefundState.REFUND_SUCCESS);
						}
						else
						{
							cartItem.getRefundState().setRefundStatus(RefundState.REFUND_NIL);
						}
						cartItem.getRefundState().setPendingPrice(new Money("0"));
						cartItem.getRefundState().setPendingQuantity(0);
						break;
					}
				}
			}
		}
	}
	
	public void refundOrder(WebPageRequest inContext) {
		
		String ordernumber = inContext.getRequestParameter("ordernumber");
		Store store = getStore(inContext);
		Order order = (Order) store.getOrderSearcher().searchById(ordernumber);
		
		String subtotalstr = inContext.getRequestParameter("subtotal");
		String taxstr = inContext.getRequestParameter("tax");
		String totalstr = inContext.getRequestParameter("total");
		String shippingstr = inContext.getRequestParameter("shipping");
		
		Money subtotal = new Money(subtotalstr);
		Money tax = new Money(taxstr);
		Money total = new Money(totalstr);
		Money shipping = new Money("0");
		
		if (shippingstr != null && !shippingstr.isEmpty())
		{
			shipping = new Money(shippingstr);
		}
		
		
		//build refund
		Refund refund = new Refund();
		refund.setSubTotal(subtotal);
		refund.setTaxAmount(tax);
		refund.setTotalAmount(total);
		Iterator<CartItem> itr = order.getItems().iterator();
		while(itr.hasNext())
		{
			CartItem cartItem = itr.next();
			if (cartItem.getRefundState().getRefundStatus().equals(RefundState.REFUND_PENDING))
			{
				RefundState state = cartItem.getRefundState();
				int quantity = state.getPendingQuantity();
				Money totalprice = state.getPendingPrice();//unitprice * quantity
				
				RefundItem refundItem = new RefundItem();
				refundItem.setShipping(false);
				refundItem.setId(cartItem.getSku());
				refundItem.setQuantity(quantity);
				refundItem.setUnitPrice(cartItem.getYourPrice());
				refundItem.setTotalPrice(totalprice);
				refund.getItems().add(refundItem);
			}
		}
		//if we want shipping, need to create a RefundItem
		if (!shipping.isZero())
		{
			RefundItem shippingrefund = new RefundItem();
			shippingrefund.setShipping(true);
			shippingrefund.setQuantity(1);
			shippingrefund.setUnitPrice(shipping);
			shippingrefund.setTotalPrice(shipping);
			refund.getItems().add(shippingrefund);
		}
		
		store.getOrderProcessor().refundOrder(inContext, store, order, refund);
		
		updateRefundStates(order,refund,false);
		//update shipping total if successful
		if (!shipping.isZero() && refund.isSuccess())
		{
			String current = order.get("shippingrefundtally");
			if (current != null && !current.isEmpty())
			{
				shipping = shipping.add(new Money(current));
			}
			order.setProperty("shippingrefundtally", shipping.toShortString());
		}
		
		order.getRefunds().add(refund);
		store.saveOrder(order);
		
		inContext.putPageValue("data",order);
		inContext.putPageValue("order",order);
		inContext.putPageValue("refund",refund);
	}
	
	public void loadCurrentOrderSet (WebPageRequest inContext) {
		OrderSet orderSet = (OrderSet) inContext.getSessionValue("orderset");
		inContext.putPageValue("orderset", orderSet);
	}
	
	public void saveCurrentOrderSet( WebPageRequest inContext ) {
		OrderSet orderSet = (OrderSet) inContext.getSessionValue("orderset");
		//TO-DO: Loop through all orders and save them!
		inContext.putSessionValue("orderset", null);
	}
	
	public void removeItems( WebPageRequest inContext ) {
		String[] inProducts = inContext.getRequestParameters("product");
		List<String> products = Arrays.asList(inProducts); 
		OrderSet orderSet = (OrderSet) inContext.getSessionValue("orderset");
		for (Iterator<Order> orderIterator = orderSet.getOrders().iterator(); orderIterator.hasNext();) {
			Order order = (Order) orderIterator.next();
			if (order.getMissingItems().size() > 0) {
				for (int indx = 0; indx < products.size(); indx++) {
					String productid = (String) products.get(indx);
					if (order.getMissingItems().contains(productid)) {
						order.getMissingItems().remove(productid);
						//products.remove(indx);
					}				
				}
			}
		}
	}
	
	public void removeItem( WebPageRequest inContext ) {
		String inProduct = inContext.getRequestParameter("product");
		OrderSet orderSet = (OrderSet) inContext.getSessionValue("orderset");
		for (Iterator<Order> orderIterator = orderSet.getOrders().iterator(); orderIterator.hasNext();) {
			List<CartItem> removeList = new ArrayList<CartItem>();
			Order order = (Order) orderIterator.next();
			if (order != null) {
				for (Iterator cartItemIterator = order.getItems().iterator(); cartItemIterator.hasNext();) {
					CartItem cartItem = (CartItem) cartItemIterator.next();
					Product product = cartItem.getProduct();
					if (product.getId().equals(inProduct)) {
						removeList.add(cartItem);
						break;
					}
				}
				if (removeList.size()>0) {
					for (Iterator<CartItem> itemIter = removeList.iterator(); itemIter.hasNext();) {
						CartItem cartItem = (CartItem) itemIter.next();
						if (cartItem != null) {
							order.getCart().removeItem(cartItem);
						} else {
							System.out.println("CartItem is null");
						}
					}
				}
			}
		}
		Store store = getStore(inContext);
		orderSet.recalculateAll(store);
	}
	
	public void removeBadStockFromOrders( WebPageRequest inContext ) {
		String[] inOrders = inContext.getRequestParameters("orders");
		List<String> badOrders = Arrays.asList(inOrders);
		
		OrderSet orderSet = (OrderSet) inContext.getSessionValue("orderset");
		if (orderSet.getOutOfStockOrders().size() > 0) {
			for (Iterator<String> badOrderIterator = badOrders.iterator(); badOrderIterator.hasNext();) {
				List<CartItem> removeList = new ArrayList<CartItem>();
				String orderId = badOrderIterator.next();
				Order order = orderSet.getOrder(orderId);
				for (Iterator<CartItem> cartItemIterator = order.getItems().iterator(); cartItemIterator.hasNext();) {
					CartItem cartItem = (CartItem) cartItemIterator.next();
					Product product = cartItem.getProduct();
					if (!product.isInStock()) {
						removeList.add(cartItem);
					} else {
						Set<String> badproducts = orderSet.getOutOfStockPerOrder(order);
						if (badproducts.contains(product.getId())) {
							removeList.add(cartItem);
						}
					}
				}
				if (removeList.size()>0) {
					for (Iterator<CartItem> itemIter = removeList.iterator(); itemIter.hasNext();) {
						CartItem cartItem = (CartItem) itemIter.next();
						order.getCart().removeItem(cartItem);
					}
				}
			}
			Store store = getStore(inContext);
			orderSet.recalculateAll(store);
		}
	}

	public void removeStockFromOrder( WebPageRequest inContext ) {
		String inOrder = inContext.getRequestParameter("order");
		String inProduct = inContext.getRequestParameter("product");
		
		Store store = getStore(inContext);
		List<CartItem> removeList = new ArrayList<CartItem>();
		
		OrderSet orderSet = (OrderSet) inContext.getSessionValue("orderset");
		Order order = orderSet.getOrder(inOrder);
		if (order != null) {
			for (Iterator cartItemIterator = order.getItems().iterator(); cartItemIterator.hasNext();) {
				CartItem cartItem = (CartItem) cartItemIterator.next();
				Product product = cartItem.getProduct();
				if (product.getId().equals(inProduct)) {
					removeList.add(cartItem);
					break;
				}
			}
			if (removeList.size()>0) {
				for (Iterator<CartItem> itemIter = removeList.iterator(); itemIter.hasNext();) {
					CartItem cartItem = (CartItem) itemIter.next();
					order.getCart().removeItem(cartItem);
				}
			}
			orderSet.recalculateAll(store);
		}
	}
	
	public void removeOutOfStockItemsFromOrders( WebPageRequest inContext ) {
		String[] inOrders = inContext.getRequestParameters("orders");
		List<String> badOrders = Arrays.asList(inOrders);
		
		OrderSet orderSet = (OrderSet) inContext.getSessionValue("orderset");
		if (orderSet.getOutOfStockOrders().size() > 0) {
			for (Iterator<String> badOrderIterator = badOrders.iterator(); badOrderIterator.hasNext();) {
				List<CartItem> removeList = new ArrayList<CartItem>();
				String orderId = badOrderIterator.next();
				Order order = orderSet.getOrder(orderId);
				for (Iterator<CartItem> cartItemIterator = order.getItems().iterator(); cartItemIterator.hasNext();) {
					CartItem cartItem = (CartItem) cartItemIterator.next();
					Product product = cartItem.getProduct();
					if (!product.isInStock()) {
						removeList.add(cartItem);
					}
				}
				if (removeList.size()>0) {
					for (Iterator<CartItem> itemIter = removeList.iterator(); itemIter.hasNext();) {
						CartItem cartItem = (CartItem) itemIter.next();
						order.getCart().removeItem(cartItem);
					}
				}
			}
			Store store = getStore(inContext);
			orderSet.recalculateAll(store);
		}
	}
	public void updateOrders( WebPageRequest inContext ) {
		OrderSet orderSet = (OrderSet) inContext.getSessionValue("orderset");
		String[] inOrders = inContext.getRequestParameters("orderqty");
		
		List<String> processOrders = Arrays.asList(inOrders);
		for (Iterator<String> iterator = processOrders.iterator(); iterator.hasNext();) {
			List<CartItem> removeList = new ArrayList<CartItem>();
			String key = (String) iterator.next();
			String qty = inContext.getRequestParameter(key);
			String[] info = key.split(":");
			String orderid = info[0];
			String productid = info[1];
			Order order = orderSet.getOrder(orderid);
			for (Iterator<CartItem> cartItemIterator = order.getItems().iterator(); cartItemIterator.hasNext();) {
				CartItem cartItem = (CartItem) cartItemIterator.next();
				Product product = cartItem.getProduct();
				if (product.getId().equals(productid)) {
					int newValue = Integer.parseInt(qty);
					if (newValue > 0) {
						cartItem.setQuantity(newValue);
					} else {
						removeList.add(cartItem);
					}
					break;
				}
			}
			if (removeList.size()>0) {
				for (Iterator<CartItem> itemIter = removeList.iterator(); itemIter.hasNext();) {
					CartItem cartItem = (CartItem) itemIter.next();
					order.getCart().removeItem(cartItem);
				}
			}
		}
		Store store = getStore(inContext);
		orderSet.recalculateAll(store);
	}
	public void processOrders( WebPageRequest inContext ) {
		if (inContext.getRequestParameter("action").equalsIgnoreCase("processorder")) {
			List<String> orderList = new ArrayList<String>();
			
			MediaArchive archive = (MediaArchive) inContext.getPageValue("mediaarchive");
			String catalogid = archive.getCatalogId();
			SearcherManager searcherManager = archive.getSearcherManager();
			Searcher as400Searcher = searcherManager.getSearcher(catalogid, "as400");
			
			Store store = getStore(inContext);
			OrderSearcher orderSearcher = store.getOrderSearcher();
			OrderSet orderSet = (OrderSet) inContext.getSessionValue("orderset");
			for (Iterator<Order> orderIterator = orderSet.getOrders().iterator(); orderIterator.hasNext();) {
				Order order = (Order) orderIterator.next();
				
				//Save data to AS400 Table
				String batchID = (String) order.get("batchid");
				Data as400Record = (Data) as400Searcher.searchByField("batchid", batchID);
				if (as400Record == null) {
					as400Record = as400Searcher.createNewData();
					as400Record.setId(as400Searcher.nextId());
					as400Record.setSourcePath(batchID);
					as400Record.setProperty("batchid", batchID);
					as400Record.setProperty("user", inContext.getUser().getId());
					Date now = new Date();
					String newDate = DateStorageUtil.getStorageUtil().formatForStorage(now);
					as400Record.setProperty("date", newDate);
					as400Searcher.saveData(as400Record, inContext.getUser());
				}
				
				OrderState orderState = new OrderState();
				orderState.setId("authorized");
				orderState.setDescription("authorized");
				orderState.setOk(true);
				order.setOrderState(orderState);
				
				Date now = new Date();
				String newDate = DateStorageUtil.getStorageUtil().formatForStorage(now);
				order.setProperty("orderdate", newDate);
				
				order.setProperty("orderstatus", "authorized");
				order.setProperty("order_status", "authorized");
				
				orderSearcher.saveData(order, inContext.getUser());
				orderList.add(order.getId());
			}
			inContext.putPageValue("orderlist", orderList);
			inContext.putSessionValue("orderset", null);
		}
	}
}
