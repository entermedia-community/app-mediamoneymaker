/*
 * Created on Jan 17, 2005
 */
package org.openedit.store.modules;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.document.Document;
import org.openedit.event.WebEventListener;
import org.openedit.store.CartItem;
import org.openedit.store.Store;
import org.openedit.store.customer.Address;
import org.openedit.store.orders.EmailOrderProcessor;
import org.openedit.store.orders.Order;
import org.openedit.store.orders.OrderArchive;
import org.openedit.store.orders.OrderId;
import org.openedit.store.orders.OrderState;
import org.openedit.store.orders.SubmittedOrder;

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
			inReq.putPageValue(ORDERIDLIST, orders);
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
			List orderIdList = (List) inRequest.getPageValue("orderlist");
			if (orderIdList == null)
			{
				orderIdList = getOrderIdList(inRequest);
			}
			String orderNumber = path.substring(path.lastIndexOf("/") + 1, path
					.lastIndexOf( extension ));
			SubmittedOrder order = getOrderFromNumber(inRequest, orderIdList, orderNumber);
			inRequest.putPageValue("order", order);
			return order;
		}
		return null;
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

}
