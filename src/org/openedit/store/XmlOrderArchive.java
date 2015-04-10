/*
 * Created on Jan 17, 2005
 */
package org.openedit.store;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Attribute;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.entermedia.email.PostMail;
import org.openedit.Data;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetailsArchive;
import org.openedit.money.Fraction;
import org.openedit.money.Money;
import org.openedit.store.adjustments.Adjustment;
import org.openedit.store.adjustments.CouponAdjustment;
import org.openedit.store.adjustments.DiscountAdjustment;
import org.openedit.store.adjustments.FixedPriceAdjustment;
import org.openedit.store.adjustments.SaleAdjustment;
import org.openedit.store.customer.Address;
import org.openedit.store.customer.Customer;
import org.openedit.store.orders.AbstractXmlOrderArchive;
import org.openedit.store.orders.Order;
import org.openedit.store.orders.OrderArchive;
import org.openedit.store.orders.OrderId;
import org.openedit.store.orders.OrderState;
import org.openedit.store.orders.Refund;
import org.openedit.store.orders.RefundItem;
import org.openedit.store.orders.RefundState;
import org.openedit.store.orders.Shipment;
import org.openedit.store.orders.ShipmentEntry;
import org.openedit.store.orders.SubmittedOrder;
import org.openedit.util.DateStorageUtil;

import com.openedit.OpenEditException;
import com.openedit.OpenEditRuntimeException;
import com.openedit.page.Page;
import com.openedit.users.User;
import com.openedit.users.UserManager;
import com.openedit.users.filesystem.FileSystemUser;
import com.openedit.util.PathUtilities;
import com.openedit.util.StringEncryption;
import com.openedit.util.XmlUtil;

/**
 * TODO: Make this have a getCatalogId() method
 * 
 */
public class XmlOrderArchive extends AbstractXmlOrderArchive implements
		OrderArchive {
	private static final Log log = LogFactory.getLog(XmlOrderArchive.class);

	protected static final String ORDERS_FILENAME = "orders.xml";
	protected PostMail postMail;
	protected XmlUtil fieldXmlUtil;
	protected PropertyDetailsArchive fieldFieldArchive;
	protected StringEncryption fieldStringEncryption;
	protected UserManager fieldUserManager;
	

	public PostMail getPostMail() {
		return postMail;
	}

	public void setPostMail(PostMail postMail) {
		this.postMail = postMail;
	}

	public Map getOrderStates(Store inStore) {
		Map list = ListOrderedMap.decorate(new HashMap());
		try {
			Collection types = inStore.getProperties("orderstatus");
			for (Iterator iterator = types.iterator(); iterator.hasNext();) {
				Data key = (Data) iterator.next();
				//Element element = key.getElement();
				OrderState state = new OrderState();
				state.setId(key.getId());
				state.setDescription(key.getName());

				list.put(state.getId(), state);
			}
		} catch (OpenEditException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// OrderState state = new OrderState();
		// state.setId(Order.ACCEPTED);
		// state.setDescription("Accepted");
		// list.put(state.getId(),state);
		//
		// state = new OrderState();
		// state.setId(Order.AUTHORIZED);
		// state.setDescription("Authorized");
		// list.put(state.getId(),state);
		//
		//
		// state = new OrderState();
		// state.setId(Order.COMPLETED);
		// state.setDescription("Accepted");
		// list.put(state.getId(),state);
		//
		// state = new OrderState();
		// state.setId(Order.CAPTURED);
		// state.setDescription("Captured");
		// list.put(state.getId(),state);
		//
		// state = new OrderState();
		// state.setId(Order.REJECTED);
		// state.setDescription("Rejected");
		// list.put(state.getId(),state);

		return list;
	}

	public void archiveOrderData(Store inStore) throws StoreException {
		// Does nothing, this is mainly for RetailPro.
	}

	public void saveOrder(Store inStore, Order inOrder) throws StoreException {
		try {
			Customer customer = inOrder.getCustomer();
			log.info("Exporting order " + inOrder.getId() + " to XML");
			File customerOrdersFile = getOrderFile(inStore, inOrder);
			log.debug("Using " + customerOrdersFile.getPath());
			Element orderElem = DocumentHelper.createDocument().addElement(
					"order");
			// add node for order, with basic information about it
			orderElem.addAttribute("order_number", inOrder.getId());
			orderElem.addAttribute("date", DateStorageUtil.getStorageUtil()
					.formatForStorage(inOrder.getDate()));

			ShippingMethod shippingMethod = inOrder.getShippingMethod();
			if (shippingMethod != null) {
				orderElem.addAttribute("shipping_method",
						shippingMethod.getId());
			}
			Money sub = inOrder.getSubTotal();
			if (sub != null) {
				orderElem.addAttribute("subtotal", sub.toShortString());
				orderElem.addAttribute("tax", inOrder.getTax().toShortString());
				Money shippingcost = inOrder.getTotalShipping();
				if (shippingcost != null) {
					orderElem.addAttribute("shipping_cost",
							shippingcost.toShortString());
				}
				orderElem.addAttribute("total", inOrder.getTotalPrice()
						.toShortString());
			}

			Address shipping = inOrder.getShippingAddress();
			if (shipping != null) {
				Element shippingElem = orderElem.addElement("shipping-address");
				shippingElem.addAttribute("id", shipping.getId());
				shippingElem.addAttribute("name", shipping.getName());
				shippingElem.addAttribute("address1", shipping.getAddress1());
				shippingElem.addAttribute("address2", shipping.getAddress2());
				shippingElem.addAttribute("city", shipping.getCity());
				shippingElem.addAttribute("country", shipping.getCountry());
				shippingElem.addAttribute("state", shipping.getState());
				shippingElem.addAttribute("zip", shipping.getZipCode());
				shippingElem.addAttribute("description",
						shipping.getDescription());
			}

			Address billing = inOrder.getBillingAddress();
			if (billing != null) {
				Element shippingElem = orderElem.addElement("billing-address");

				shippingElem.addAttribute("address1", billing.getAddress1());
				shippingElem.addAttribute("address2", billing.getAddress2());
				shippingElem.addAttribute("city", billing.getCity());
				shippingElem.addAttribute("country", billing.getCountry());
				shippingElem.addAttribute("state", billing.getState());
				shippingElem.addAttribute("zip", billing.getZipCode());
				shippingElem.addAttribute("description",
						billing.getDescription());
			}

			// add detailed tax information
			Map taxes = inOrder.getTaxes();
			if (taxes != null) {
				for (Iterator iterator = inOrder.getTaxes().keySet().iterator(); iterator
						.hasNext();) {
					TaxRate rate = (TaxRate) iterator.next();
					Money money = (Money) taxes.get(rate);
					Element taxentry = orderElem.addElement("taxentry");
					taxentry.addAttribute("name", rate.getName());
					taxentry.addAttribute("state", rate.getState());
					taxentry.addAttribute("rate", rate.getFraction().toString());
					taxentry.addAttribute("amount", money.toShortString());
					taxentry.addAttribute("shipping",
							String.valueOf(rate.isApplyToShipping()));
				}
			}
			
			//add refunds
			Iterator<Refund> itr = inOrder.getRefunds().iterator();
			while(itr.hasNext())
			{
				Refund refund = itr.next();
				Element entry = orderElem.addElement("refund");
				
				for (Iterator i = refund.getProperties().keySet().iterator(); i
						.hasNext();) {
					String key = i.next().toString();
					if (key == null || key.trim().length() == 0
							|| inOrder.get(key) == null
							|| inOrder.get(key).trim().length() == 0) {
						continue;
					}
					Element newProperty = entry.addElement("property");
					newProperty.addAttribute("name", key);
					newProperty.setText(inOrder.get(key));
				}
				
				
				entry.addAttribute("date", DateStorageUtil.getStorageUtil().formatForStorage(refund.getDate()));
				entry.addAttribute("success",String.valueOf(refund.isSuccess()));
				if (refund.isSuccess())
				{
					entry.addAttribute("transactionid",refund.getTransactionId());
					entry.addAttribute("subtotal", refund.getSubTotal().toShortString());
					entry.addAttribute("tax", refund.getTaxAmount().toShortString());
					entry.addAttribute("total", refund.getTotalAmount().toShortString());
					//include message in refund
					if (refund.getMessage()!=null){
						entry.addAttribute("message",refund.getMessage());
					}
				}
				else
				{
					entry.addAttribute("message",refund.getMessage());
				}
				Iterator<RefundItem> itr2 = refund.getItems().iterator();
				while(itr2.hasNext())
				{
					RefundItem ritem = itr2.next();
					Element refunditem = entry.addElement("refunditem");
					refunditem.addAttribute("shipping", ""+ritem.isShipping());
					refunditem.addAttribute("sku", ritem.isShipping() ? "" : ritem.getId());
					refunditem.addAttribute("quantity", String.valueOf(ritem.getQuantity()));
					refunditem.addAttribute("totalprice", ritem.getTotalPrice().toShortString());
					refunditem.addAttribute("unitprice", ritem.getUnitPrice().toShortString());
				}
			}
			
			
			List<?> adjustments = inOrder.getAdjustments();
			if (adjustments != null) {
				for (Iterator iterator = adjustments.iterator(); iterator
						.hasNext();) {
					Adjustment adjustment = (Adjustment) iterator.next();
					if (adjustment instanceof SaleAdjustment)
					{
						SaleAdjustment a = (SaleAdjustment) adjustment;//multiple
						String val = a.getPercentage().getFraction().toString();
						String inventoryid = a.getInventoryItemId();
						String productid = a.getProductId();
						Element e = orderElem.addElement("adjustment");
						e.addAttribute("type", "saleadjustment");
						e.addAttribute("value", val);
						if (inventoryid!=null) e.addAttribute("inventoryid", inventoryid);
						if (productid!=null) e.addAttribute("productid", productid);
					}
					else if (adjustment instanceof DiscountAdjustment)
					{
						DiscountAdjustment a = (DiscountAdjustment) adjustment;//multiple
						String val = String.valueOf(a.getDiscount().doubleValue());
						String inventoryid = a.getInventoryItemId();
						String productid = a.getProductId();
						Element e = orderElem.addElement("adjustment");
						e.addAttribute("type", "discountadjustment");
						e.addAttribute("value", val);
						if (inventoryid!=null) e.addAttribute("inventoryid", inventoryid);
						if (productid!=null) e.addAttribute("productid", productid);
					}
					else if (adjustment instanceof CouponAdjustment)
					{
						CouponAdjustment a = (CouponAdjustment) adjustment;//multiple
						String val = String.valueOf(a.getDiscount().doubleValue());
						String inventoryid = a.getInventoryItemId();
						String productid = a.getProductId();
						Element e = orderElem.addElement("adjustment");
						e.addAttribute("type", "couponadjustment");
						e.addAttribute("value", val);
						if (inventoryid!=null) e.addAttribute("inventoryid", inventoryid);
						if (productid!=null) e.addAttribute("productid", productid);
					}
					else if (adjustment instanceof FixedPriceAdjustment)
					{
						FixedPriceAdjustment a = (FixedPriceAdjustment) adjustment;//multiple
						String val = String.valueOf(a.getDiscount().doubleValue());
						String inventoryid = a.getInventoryItemId();
						String productid = a.getProductId();
						Element e = orderElem.addElement("adjustment");
						e.addAttribute("type", "fixedpriceadjustment");
						e.addAttribute("value", val);
						if (inventoryid!=null) e.addAttribute("inventoryid", inventoryid);
						if (productid!=null) e.addAttribute("productid", productid);
					}
					else {
						//more types of adjustments here
					}
				}
			}
			// add customer information, including address
			// add saving code for entries
			Element shippingdetails = orderElem.addElement("shipping");
			for (Iterator iterator = inOrder.getShipments().iterator(); iterator
					.hasNext();) {
				Shipment shipment = (Shipment) iterator.next();
				Element shipElem = shippingdetails.addElement("shipment");
				for (Iterator iterator2 = shipment.getProperties().keySet()
						.iterator(); iterator2.hasNext();) {
					String key = (String) iterator2.next();
					String value = shipment.get(key);
					shipElem.addAttribute(key, value);
				}
				for (Iterator iterator3 = shipment.getShipmentEntries()
						.iterator(); iterator3.hasNext();) {
					ShipmentEntry entry = (ShipmentEntry) iterator3.next();
					Element shipEntry = shipElem.addElement("shipping-entry");
					shipEntry.addAttribute("sku", entry.getSku());
					shipEntry.addAttribute("quantity",String.valueOf(entry.getQuantity()));
					for (Iterator iterator4 = entry.getProperties().keySet()
							.iterator(); iterator4.hasNext();) {
						String key2 = (String) iterator4.next();
						String value2 = shipment.get(key2);
						if ("sku".equals(key2) || "quantity".equals(key2)){
							continue;
						}
						shipEntry.addAttribute(key2, value2);
					}
				}

			}

			Element customerElem = orderElem.addElement("customer");
			customerElem.addAttribute("customerid", customer.getUserName());
			customerElem.addAttribute("title", customer.getTitle());
			customerElem.addAttribute("first_name", customer.getFirstName());
			customerElem.addAttribute("last_name", customer.getLastName());
			customerElem.addAttribute("company", customer.getCompany());
			customerElem.addAttribute("phone1", customer.getPhone1());
			customerElem.addAttribute("email", customer.getEmail());
			customerElem.addAttribute("fax", customer.getFax());

//			Element ta = orderElem.addElement("customer");
//			customerElem.addAttribute("customerid", customer.getUserName());
//			customerElem.addAttribute("title", customer.getTitle());
//			customerElem.addAttribute("first_name", customer.getFirstName());
//			customerElem.addAttribute("last_name", customer.getLastName());
//			customerElem.addAttribute("company", customer.getCompany());
//			customerElem.addAttribute("phone1", customer.getPhone1());
//			customerElem.addAttribute("email", customer.getEmail());
//			customerElem.addAttribute("fax", customer.getFax());

			String user1 = customer.getUserField1();
			if (user1 != null) {
				customerElem.addAttribute("userfield1", user1);
			}
			String user2 = customer.getUserField2();
			if (user2 != null) {
				customerElem.addAttribute("userfield2", user2);
			}
			// for (Iterator iterator = customer.getAddressList().iterator();
			// iterator
			// .hasNext();) {
			// Address address = (Address) iterator.next();
			// appendAddress(customerElem, address);
			// }
			/*
			 * if ( customer.getBillingAddress() != null &&
			 * customer.getBillingAddress().getAddress1() != null ) {
			 * appendAddress( customerElem, customer.getBillingAddress(),
			 * "billing" ); } if ( customer.getShippingAddress() != null &&
			 * customer.getShippingAddress().getAddress1() != null ) {
			 * appendAddress( customerElem, customer.getShippingAddress(),
			 * "shipping" ); }
			 */
			// add items
			for (Iterator it = inOrder.getItems().iterator(); it.hasNext();) {
				CartItem item = (CartItem) it.next();
				appendItem(orderElem, inStore, item);
			}

			// add payment method
			PaymentMethod paymentMethod = (PaymentMethod) inOrder
					.getPaymentMethod();
			if (paymentMethod != null && paymentMethod instanceof CreditPaymentMethod) {
				CreditPaymentMethod m = (CreditPaymentMethod) paymentMethod;
				Element paymentElem = orderElem.addElement("payment_method");
				paymentElem.addAttribute("payment_type", "credit");
				if (paymentMethod instanceof PurchaseOrderMethod) {
					paymentElem
							.addAttribute("po_number",
									((PurchaseOrderMethod) paymentMethod)
											.getPoNumber());
				}
				paymentElem.addAttribute("card_type", m
						.getCreditCardType().getId());
				paymentElem.addAttribute("card_number",
						encrypt(m.getCardNumber()));
				/* THIS IS A HUGE NO NO!!!! */
				// paymentElem.addAttribute("card_verification_code",
				// encrypt(paymentMethod.getCardVerificationCode()));

				paymentElem.addAttribute("expiration_date",
						m.getExpirationDateString());
				paymentElem.addAttribute("cardholder_name", m.getCardHolderName() == null ? "" : m.getCardHolderName());
				
				boolean bill = m.getBillMeLater();
				paymentElem.addAttribute("bill_me_later", String.valueOf(bill));
				paymentElem.addAttribute("note", m.getNote());
			}
			
			else if (paymentMethod != null && paymentMethod instanceof PrepaidPaymentMethod){
				
				
				PrepaidPaymentMethod m = (PrepaidPaymentMethod) paymentMethod;
				Element paymentElem = orderElem.addElement("payment_method");
				paymentElem.addAttribute("payment_type", "prepaid");
						
				paymentElem.addAttribute("note", m.getPrepaidCode());
				
				
			}
			// inOrder.getOrderStatus().setOk(true); Why in the world would we
			// reset this if it was just rejected?
			if ("received".equals(inOrder.getOrderStatus().getId())) {
				String status = "accepted";
				inOrder.getOrderStatus().setId(status);
				inOrder.getOrderStatus().setDescription("Order accepted");
			}
			orderElem.addAttribute("order_status", inOrder.getOrderStatus()
					.getId());

			for (Iterator i = inOrder.getProperties().keySet().iterator(); i
					.hasNext();) {
				String key = i.next().toString();
				if (key == null || key.trim().length() == 0
						|| inOrder.get(key) == null
						|| inOrder.get(key).trim().length() == 0) {
					continue;
				}
				Element newProperty = orderElem.addElement("property");
				newProperty.addAttribute("name", key);
				newProperty.setText(inOrder.get(key));
			}
			writeXmlFile(orderElem, customerOrdersFile);
		} catch (Exception e) {
			log.error(
					"Could not archive this order request:\n" + inOrder.getId(),
					e);
			inOrder.getOrderStatus().setDescription(
					"Order could not be sent: " + e.getMessage());
			inOrder.getOrderStatus().setOk(false);
			if (e instanceof StoreException) {
				throw (StoreException) e;
			}
			throw new StoreException(e);
		}
	}

	public void changeOrderStatus(OrderState inStatus, Store inStore,
			Order inOrder) throws StoreException {
		// inOrder.getOrderState().setDescription(status);

		// Quick way to update the data TODO: Use standard save API instead
		File orderFile = new File(getOrdersDirectory(inStore), inOrder
				.getCustomer().getUser().getUserName()
				+ "/" + inOrder.getOrderNumber() + ".xml");

		Element orderElem = getRootElement(orderFile, "order");
		orderElem.addAttribute("order_status", inStatus.getId());
		writeXmlFile(orderElem, orderFile);

	}

	protected void appendItem(Element inOrderElem, Store inStore,
			CartItem inItem) {
		Element itemElem = inOrderElem.addElement("item");
		itemElem.addAttribute("sku", inItem.getSku());
		itemElem.addAttribute("product_id", inItem.getProduct().getId());
		itemElem.addAttribute("quantity", String.valueOf(inItem.getQuantity()));
		itemElem.addAttribute("price", inItem.getYourPrice().toShortString());
		if (inItem.getWholesalePrice()!=null)
		{
			itemElem.addAttribute("wholesaleprice", inItem.getWholesalePrice().toShortString());
		}
		// itemElem.addAttribute("shipto", inItem.getShippingPrefix());
		itemElem.addAttribute("status", inItem.getStatus());

		for (Iterator it = inItem.getOptions().iterator(); it.hasNext();) {
			Option option = (Option) it.next();
			Element optionElem = itemElem.addElement("option");
			optionElem.addAttribute("id", option.getId());
			optionElem.addAttribute("value", option.getValue());
			optionElem.addAttribute("name", option.getName());
			optionElem.addAttribute("datatype", option.getDataType());
		}
		
		//if there is a refund state then persist it
		//when a refund is issued, it goes from pending to success or nil (meaning failure or rejected)
		//so update the quantity when a refund success is found
		if (inItem.getRefundState().getRefundStatus().equals(RefundState.REFUND_SUCCESS))
		{
			Element entry = itemElem.addElement("refundstate");
			int quantity = inItem.getRefundState().getQuantity();
			entry.addAttribute("quantity", String.valueOf(quantity));
		}
		//load defined properties
        if (inItem.getProperties() != null) 
        {
			for (Iterator it = inItem.getProperties().keySet().iterator(); it
					.hasNext();) {
				String key = (String) it.next();
				String value = (String) inItem.getProperties().get(key);
				if (value != null) {
					Element extraInfoElem = itemElem.addElement("property");
					extraInfoElem.addAttribute("name", key);
					extraInfoElem.setText(value);
					log.info("adding name="+key+", value="+value);
				}
			}
		}
		//add those properties that should be saved with order
		Collection<?> details = inStore.getProductSearcher().getPropertyDetails();
        if (details!=null && details.size() > 0)
        {
			for (Iterator<?> iterator = details.iterator(); iterator.hasNext();)
			{
				PropertyDetail detail = (PropertyDetail) iterator.next();
				String save = detail.get("savewithorder");
				if (Boolean.parseBoolean(save))
				{
					String value = inItem.getProduct().get(detail.getId());
					if (value == null){
						value = detail.get("savewithorderdefault");
					}
					if (value!=null)
					{
						List<?> props = itemElem.elements("property");
						boolean isfound = false;
						for(Object prop:props)
						{
							Element e = (Element) prop;
							if (e.attributeValue("name")!=null && e.attributeValue("name").equals("product_"+detail.getId()))
							{
								//don't overwrite original value!
								isfound = true;
								break;
							}
						}
						if (!isfound)
						{
							Element extraInfoElem = itemElem.addElement("property");
							extraInfoElem.addAttribute("name", "product_"+detail.getId());
							extraInfoElem.setText(value);
							Element extraInfoElem2 = itemElem.addElement("property");
							extraInfoElem2.addAttribute("name", "product_"+detail.getId()+"_timestamp");
							extraInfoElem2.setText(DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
							log.info("saving product snapshot: name=product_"+detail.getId()+", value="+value+"; including timestamp property as \"product_"+detail.getId()+"_timestamp\"");
						}
					}
				}
			}
        }
        if (Boolean.parseBoolean(inStore.get("saveinstock")))
        {
        	String sku = inItem.getSku();
        	List<?> props = itemElem.elements("property");
			for(Object prop:props)
			{
				Element e = (Element) prop;
				if (e.attributeValue("name")!=null && e.attributeValue("name").equals("instock"))
				{
					return;
				}
			}
			Product product = inItem.getProduct();
    		InventoryItem inventory = product.getInventoryItemBySku(sku);
    		if (inventory!=null)
    		{
    			int stock = inventory.getQuantityInStock();
				Element instock = itemElem.addElement("property");
				instock.addAttribute("name", "instock");
				instock.setText(String.valueOf(stock));
				log.info("saving the remaining stock for \""+sku+"\", #"+stock);
    		}
        }
	}

	protected void appendAddress(Element inCustomerElem, Address inAddress) {
		Element addressElem = inCustomerElem.addElement("address");
		addressElem.addAttribute("type", inAddress.getPrefix());
		addressElem.addAttribute("address1", inAddress.getAddress1());
		addressElem.addAttribute("address2", inAddress.getAddress2());
		addressElem.addAttribute("city", inAddress.getCity());
		addressElem.addAttribute("state", inAddress.getState());
		addressElem.addAttribute("zip_code", inAddress.getZipCode());
		addressElem.addAttribute("country", inAddress.getCountry());
	}

	protected File getOrderFile(Store inStore, Order inOrder) {
		return new File(getOrdersDirectory(inStore), inOrder.getCustomer()
				.getUserName() + "/" + inOrder.getId() + ".xml");
	}

	public List listAllOrderIds(Store inStore) {
		List orders = new ArrayList();
		findOrders(inStore, getOrdersDirectory(inStore), orders);
		return orders;
	}

	// public List listOrders( Store inStore, WebPageRequest inContext ) throws
	// StoreException
	// {
	// File ordersFile = getOrdersDirectory(inStore);
	// List allorders = new ArrayList();
	// try
	// {
	// findOrders( inStore, ordersFile ,allorders);
	// }
	// catch ( Exception ex)
	// {
	// throw new StoreException(ex);
	// }
	// Collections.sort( allorders);
	// return allorders;
	// }

	// public List listOrdersForUser( Store inStore, User inUser ) throws
	// StoreException
	// {
	// File ordersFile = getOrdersDirectory(inStore);
	// List allorders = new ArrayList();
	// File order = null;
	// try
	// {
	// order = new File( ordersFile , inUser.getUserName());
	// findOrders( inStore, order ,allorders);
	// }
	// catch ( Exception ex)
	// {
	// log.error("Could not parse " + order);
	// throw new StoreException(ex);
	// }
	// Collections.sort( allorders);
	// return allorders;
	// }

	protected void findOrders(Store inStore, File inFile, List inAllorders) {
		File[] listing = inFile.listFiles(new FilenameFilter() {
			public boolean accept(File inDir, String inName) {
				if (inName.equals("CVS")) {
					return false;
				}
				return inDir.isDirectory() || inName.endsWith(".xml");
			}
		});
		if (listing != null) {
			for (int i = 0; i < listing.length; i++) {
				File one = listing[i];
				if (one.isDirectory()) {
					findOrders(inStore, one, inAllorders);
				} else {
					try {
						OrderId order = new OrderId();
						order.setOrderId(PathUtilities.extractPageName(one
								.getName()));
						order.setUsername(inFile.getName());
						inAllorders.add(order);
					} catch (Exception ex) {
						log.error("Could not load " + one);
						throw new OpenEditRuntimeException(ex);
					}
				}
			}
		}
	}

	public SubmittedOrder loadSubmittedOrder(Store inStore, String inUserName,
			String inOrderId) throws StoreException {

	
		
		File input = new File(getOrdersDirectory(inStore), inUserName + "/"
				+ inOrderId + ".xml");
		if (!input.exists()) {
			return null;
		}
		Element orderElement;
		try {
			orderElement = getXmlUtil().getXml(new FileReader(input), "UTF-8");
		} catch (FileNotFoundException e) {
			throw new StoreException(e);
		}
		SubmittedOrder order = new SubmittedOrder();
		loadSubmittedOrder(inStore, orderElement, order);

		return order;
	}

	protected void loadSubmittedOrder(Store inStore, Element inOrderElement,
			SubmittedOrder inOrder) throws StoreException {
		inOrder.setId(inOrderElement.attributeValue("order_number"));
		inOrder.setShippingMethod(makeShippingMethod(inStore,
				inOrderElement.attributeValue("shipping_method")));
		String subtotal = inOrderElement.attributeValue("subtotal");
		if (subtotal != null) {
			inOrder.setSubTotal(new Money(subtotal));
		}
		String tax = inOrderElement.attributeValue("tax");
		if (tax != null) {
			inOrder.setTotalTax(new Money(tax));
		}
		String cost = inOrderElement.attributeValue("shipping_cost");
		if (cost != null) {
	//		inOrder.setShippingCost(new Money(cost));
			inOrder.setTotalShipping(new Money(cost));
		}
		String total = inOrderElement.attributeValue("total");
		if (total != null) {
			inOrder.setTotalPrice(new Money(total));
		} else if (inOrder.getSubTotal() != null) {
			Money totalp = inOrder.getSubTotal().add(inOrder.getTax());
			totalp = totalp.add(inOrder.getTotalShipping());
			inOrder.setTotalPrice(totalp);
		}

		// load taxes
		HashMap taxes = new HashMap();
		for (Iterator it = inOrderElement.elementIterator("taxentry"); it
				.hasNext();) {
			TaxRate rate = new TaxRate();
			Element taxentry = (Element) it.next();
			String amount = taxentry.attributeValue("amount");
			Money money = new Money(amount);
			rate.setFraction(new Fraction(taxentry.attributeValue("rate")));
			rate.setName(taxentry.attributeValue("name"));
			rate.setState(taxentry.attributeValue("state"));
			rate.setApplyToShipping(Boolean.parseBoolean(taxentry
					.attributeValue("shipping")));
			taxes.put(rate, money);
		}
		inOrder.setTaxes(taxes);

		List<Adjustment> adjustments = new ArrayList<Adjustment>();

		for (Iterator it = inOrderElement.elementIterator("adjustment"); it
				.hasNext();) {
			
			Element element = (Element) it.next();
			String type = element.attributeValue("type");
			String productid = element.attributeValue("productid");
			String inventoryid = element.attributeValue("inventoryid");
			String value = element.attributeValue("value");
			if (type == null || type.isEmpty())//backwards compatible
			{
				type = "saleadjustment";
				value = element.getText();
			}
			if (type.equals("saleadjustment"))
			{
				SaleAdjustment adjustment = new SaleAdjustment();
				if (inventoryid!=null) adjustment.setInventoryItemId(inventoryid);
				if (productid!=null) adjustment.setProductId(productid);
				adjustment.setPercentage(Double.parseDouble(value));
				adjustments.add(adjustment);
			}
			else if (type.equals("discountadjustment"))
			{
				DiscountAdjustment adjustment = new DiscountAdjustment();
				if (inventoryid!=null) adjustment.setInventoryItemId(inventoryid);
				if (productid!=null) adjustment.setProductId(productid);
				adjustment.setDiscount(Double.parseDouble(value));
				adjustments.add(adjustment);
			} 
			else if (type.equals("couponadjustment"))
			{
				CouponAdjustment adjustment = new CouponAdjustment();
				if (inventoryid!=null) adjustment.setInventoryItemId(inventoryid);
				if (productid!=null) adjustment.setProductId(productid);
				adjustment.setDiscount(Double.parseDouble(value));
				adjustments.add(adjustment);
			}
			else if (type.equals("fixedpriceadjustment"))
			{
				FixedPriceAdjustment adjustment = new FixedPriceAdjustment();
				if (inventoryid!=null) adjustment.setInventoryItemId(inventoryid);
				if (productid!=null) adjustment.setProductId(productid);
				adjustment.setDiscount(Double.parseDouble(value));
				adjustments.add(adjustment);
			}
			else {
				//more adjustments here
			}
			
		}
		inOrder.setAdjustments(adjustments);

		// OrderState state = new OrderState();
		// state.setOk(true);
		// state.setDescription(inOrderElement.attributeValue("orderstatus"));
		// state.setId(inOrderElement.attributeValue("orderstatus"));
		String state = inOrderElement.attributeValue("order_status");
		OrderState ostate = inStore.getOrderState(state);
		if (ostate == null) {
			ostate = new OrderState();

			ostate.setDescription(state);
			ostate.setId(state);
		}
		ostate.setOk(true);
		inOrder.setOrderState(ostate);

		try {
			String formated = inOrderElement.attributeValue("date");
			Date date = DateStorageUtil.getStorageUtil().parseFromStorage(
					formated);
			inOrder.setDate(date);
			inOrder.setDateOrdered(formated);
		} catch (Exception ex) {
			throw new StoreException(ex);
		}
		
		//load refunds
		for (Iterator<?> it = inOrderElement.elementIterator("refund"); it.hasNext();) {
			Element entry = (Element) it.next();
			Refund refund = new Refund();
			String formated = entry.attributeValue("date");
			Date date = DateStorageUtil.getStorageUtil().parseFromStorage(formated);
			refund.setDate(date);
			refund.setSuccess(entry.attributeValue("success").equals("true"));
			for (Iterator rf = entry.elementIterator("property"); rf
					.hasNext();) {
				Element propElement = (Element) rf.next();
				refund.setProperty(propElement.attributeValue("name"),
						propElement.getText());
			}
			
			if (refund.isSuccess())
			{
				refund.setTransactionId(entry.attributeValue("transactionid"));
				refund.setSubTotal(new Money(entry.attributeValue("subtotal")));
				refund.setTaxAmount(new Money(entry.attributeValue("tax")));
				refund.setTotalAmount(new Money(entry.attributeValue("total")));
				if (entry.attributeValue("message")!=null){
					refund.setMessage(entry.attributeValue("message"));
				}
			}
			else
			{
				refund.setMessage(entry.attributeValue("message"));
			}
			for (Iterator<?> it2 = entry.elementIterator("refunditem"); it2.hasNext(); )
			{
				Element entry2 = (Element) it2.next();
				RefundItem refunditem = new RefundItem();
				refunditem.setShipping(entry2.attributeValue("shipping")!=null && entry2.attributeValue("shipping").equals("true") ? true : false);
				refunditem.setId( entry2.attributeValue("sku"));
				refunditem.setQuantity(Integer.parseInt(entry2.attributeValue("quantity")));
				refunditem.setTotalPrice(new Money(entry2.attributeValue("totalprice")));
				refunditem.setUnitPrice(new Money(entry2.attributeValue("unitprice")));
				refund.getItems().add(refunditem);
			}
			inOrder.addRefund(refund);
		}
 
		Customer customer = new Customer();
		Element customerElem = inOrderElement.element("customer");
		String username = customerElem.attributeValue("customerid");

		User user = new FileSystemUser();
		user.setVirtual(true);
		user.setUserName(username);
		customer.setUser(user);

		inOrder.setCustomer(customer);
		customer.setTitle(customerElem.attributeValue("title"));
		customer.setFirstName(customerElem.attributeValue("first_name"));
		customer.setLastName(customerElem.attributeValue("last_name"));
		customer.setCompany(customerElem.attributeValue("company"));
		customer.setPhone1(customerElem.attributeValue("phone1"));
		customer.setUserField1(customerElem.attributeValue("userfield1"));
		customer.setUserField2(customerElem.attributeValue("userfield2"));
		customer.setEmail(customerElem.attributeValue("email"));
		customer.setFax(customerElem.attributeValue("fax"));
		// for (Iterator it = customerElem.elementIterator("address"); it
		// .hasNext();) {
		// Element addressElem = (Element) it.next();
		// Address address = makeAddress(addressElem);
		// customer.addAddress(address);
		// }

		Element shippingaddress = inOrderElement.element("shipping-address");
		if (shippingaddress != null) {
			Address shipping = new Address();
			shipping.setId(shippingaddress.attributeValue("id"));
			
			String name = shippingaddress.attributeValue("name");
			if(name != null){
				shipping.setName(name);
			}
			shipping.setAddress1(shippingaddress.attributeValue("address1"));
			shipping.setAddress2(shippingaddress.attributeValue("address2"));
			shipping.setCity(shippingaddress.attributeValue("city"));
			shipping.setCountry(shippingaddress.attributeValue("country"));
			shipping.setState(shippingaddress.attributeValue("state"));
			shipping.setZipCode(shippingaddress.attributeValue("zip"));
			shipping.setDescription(shippingaddress
					.attributeValue("description"));
			inOrder.setShippingAddress(shipping);
		}

		Element billingaddress = inOrderElement.element("billing-address");
		if (billingaddress != null) {
			Address billing = new Address();
			billing.setAddress1(billingaddress.attributeValue("address1"));
			billing.setAddress2(billingaddress.attributeValue("address2"));
			billing.setCity(billingaddress.attributeValue("city"));
			billing.setCountry(billingaddress.attributeValue("country"));
			billing.setState(billingaddress.attributeValue("state"));
			billing.setZipCode(billingaddress.attributeValue("zip"));
			billing.setDescription(billingaddress
					.attributeValue("description"));
			inOrder.setBillingAddress(billing);
		}

		
		
//		Element shippingdetails = orderElem.addElement("shipping");
//		for (Iterator iterator = inOrder.getShipments().iterator(); iterator
//				.hasNext();) {
//			Shipment shipment = (Shipment) iterator.next();
//			Element shipElem = orderElem.addElement("shipment");
//			for (Iterator iterator2 = shipment.getProperties().keySet()
//					.iterator(); iterator2.hasNext();) {
//				String key = (String) iterator2.next();
//				String value = shipment.get(key);
//				shipElem.addAttribute(key, value);
//			}
//			for (Iterator iterator3 = shipment.getShipmentEntries()
//					.iterator(); iterator3.hasNext();) {
//				ShipmentEntry entry = (ShipmentEntry) iterator3.next();
//				Element shipEntry = shipElem.addElement("shipping-entry");
//				shipEntry.addAttribute("productid", entry.getItem()
//						.getProduct().getId());
//				shipEntry.addAttribute("quantity",
//						String.valueOf(entry.getQuantity()));
//				for (Iterator iterator4 = entry.getProperties().keySet()
//						.iterator(); iterator4.hasNext();) {
//					String key2 = (String) iterator4.next();
//					String value2 = shipment.get(key2);
//					shipEntry.addAttribute(key2, value2);
//				}
//			}
//
//		}
		
		
		
		List items = new ArrayList();
		for (Iterator it = inOrderElement.elementIterator("item"); it.hasNext();) {
			Element itemElement = (Element) it.next();
			items.add(makeItem(inStore, itemElement));
		}
		inOrder.setItems(items);

		Element paymentMethodElem = inOrderElement.element("payment_method");
		inOrder.setPaymentMethod(makePaymentMethod(inStore, paymentMethodElem));

		// //Backwards compatability. Can be removed
		for (Iterator it = inOrderElement.elementIterator("extra_info"); it
				.hasNext();) {
			Element propElement = (Element) it.next();
			inOrder.setProperty(propElement.attributeValue("key"),
					propElement.attributeValue("value"));
		}
		for (Iterator it = inOrderElement.elementIterator("property"); it
				.hasNext();) {
			Element propElement = (Element) it.next();
			inOrder.setProperty(propElement.attributeValue("name"),
					propElement.getText());
		}
		
		
		Element shippingdetails = inOrderElement.element("shipping");
		if(shippingdetails != null){
			
			for (Iterator iterator = shippingdetails.elementIterator("shipment"); iterator.hasNext();) {
				Shipment shipment = new Shipment();
				Element shipmentelem = (Element) iterator.next();
				
				for (Iterator iterator2 = shipmentelem.attributeIterator(); iterator2.hasNext();) {
					Attribute object = (Attribute) iterator2.next();
					String key = object.getName();
					String val = object.getText();
					shipment.setProperty(key, val);
				}
				
				
				for (Iterator iterator3 = shipmentelem.elementIterator("shipping-entry"); iterator3.hasNext();) {
					Element details = (Element) iterator3.next();
					ShipmentEntry entry = new ShipmentEntry();
					String sku = details.attributeValue("sku");
					String quantity = details.attributeValue("quantity");
//					entry.setCartItem(inOrder.getItem(sku));
					entry.setSku(sku);
					entry.setQuantity(Integer.parseInt(quantity));
					for (Iterator iterator2 = details.attributeIterator(); iterator2.hasNext();) {
						Attribute object = (Attribute) iterator2.next();
						String key = object.getName();
						String val = object.getText();
						entry.setProperty(key, val);
					}
					shipment.addEntry(entry);
				}
				inOrder.addShipment(shipment);
				
				
			}
			
		}
		
		
	}

	protected CartItem makeItem(Store inStore, Element inItemElem)
			throws StoreException {
		CartItem item = new CartItem();
		// item.setSku(inItemElem.attributeValue("sku"));
		item.setQuantity(Integer.parseInt(inItemElem.attributeValue("quantity")));
		item.setYourPrice(new Money(inItemElem.attributeValue("price")));
		item.setWholesalePrice(inItemElem.attributeValue("wholesaleprice")==null ? null : new Money(inItemElem.attributeValue("wholesaleprice")));
		item.setShippingPrefix(inItemElem.attributeValue("shipto"));
		item.setStatus(inItemElem.attributeValue("status"));
		for (Iterator it = inItemElem.elementIterator("option"); it.hasNext();) {
			Element optionElem = (Element) it.next();
			String optionId = optionElem.attributeValue("id");
			Option option = new Option();
			option.setId(optionId);
			option.setValue(optionElem.attributeValue("value"));
			option.setName(optionElem.attributeValue("name"));
			option.setDataType(optionElem.attributeValue("datatype"));
			item.addOption(option);
		}
		// TODO: Delete this function in OE 6.0
		// for (Iterator it = inItemElem.elementIterator("extra_info"); it
		// .hasNext();) {
		// Element extraInfoElem = (Element) it.next();
		// String key = extraInfoElem.attributeValue("key");
		// String value = extraInfoElem.attributeValue("value");
		// item.putProperty(key, value);
		// }
		for (Iterator it = inItemElem.elementIterator("property"); it.hasNext();) {
			Element extraInfoElem = (Element) it.next();
			String key = extraInfoElem.attributeValue("name");
			String value = extraInfoElem.getText();
			item.setProperty(key, value);
		}
		String productId = inItemElem.attributeValue("product_id");
		Product product = inStore.getProduct(productId);
		String isku = inItemElem.attributeValue("sku");
		if (product != null) {
			item.setProduct(product);
			InventoryItem inventory = product.getInventoryItemBySku(isku);
			// Find the inventor item for this cartitem product.
			if (inventory != null) {
				item.setInventoryItem(inventory);
			} else {
				inventory = new InventoryItem(isku);
				item.setInventoryItem(inventory);
				item.setProduct(product);
			}
		} else {
			InventoryItem inventory = new InventoryItem(isku);
			Product stub = new Product();
			stub.setAvailable(false);
			stub.setName("deleted product");
			stub.setId(productId);

			item.setProduct(stub);
			item.setInventoryItem(inventory);
		}
		
		//append refundstate
		Element refundentry = inItemElem.element("refundstate");
		if (refundentry != null)
		{
			String quantitystr = refundentry.attributeValue("quantity");
			if (quantitystr!=null && !quantitystr.isEmpty())
			{
				int quantity = Integer.parseInt(quantitystr);
				item.getRefundState().setQuantity(quantity);
			}
		}
		return item;
	}

	protected PaymentMethod makePaymentMethod(Store inStore,
			Element inPaymentMethodElem) throws StoreException {
		String poNumber = inPaymentMethodElem.attributeValue("po_number");
		String payment_type = inPaymentMethodElem.attributeValue("payment_type");
		CreditPaymentMethod paymentMethod;
		if (poNumber != null && poNumber.length() > 0) {
			paymentMethod = new PurchaseOrderMethod();
			((PurchaseOrderMethod) paymentMethod).setPoNumber(poNumber);
		} else {
			paymentMethod = new CreditPaymentMethod();
		}
		if("prepaid".equals(payment_type)){
			PaymentMethod realmethod = new PrepaidPaymentMethod();
			return realmethod;
			
			
		}
		String ccId = inPaymentMethodElem.attributeValue("card_type");
		if (ccId == null) {
			ccId = "";
		}
		for (Iterator it = inStore.getCreditCardTypes().iterator(); it
				.hasNext();) {
			CreditCardType ccType = (CreditCardType) it.next();
			if (ccType.getId().equals(ccId)) {
				paymentMethod.setCreditCardType(ccType);
				break;
			}
		}
		if (paymentMethod.getCreditCardType() == null) {
			CreditCardType ccType = new CreditCardType();
			ccType.setId(ccId);
			ccType.setName(ccId);
			paymentMethod.setCreditCardType(ccType);
		}
		paymentMethod.setCardNumber(decrypt(inPaymentMethodElem
				.attributeValue("card_number")));
		paymentMethod.setCardVerificationCode(decrypt(inPaymentMethodElem
				.attributeValue("card_verification_code")));
		String expDate = inPaymentMethodElem.attributeValue("expiration_date");
		if (expDate != null) {
			int slashPos = expDate.indexOf('/');
			if (slashPos >= 0) {
				String monthStr = expDate.substring(0, slashPos).trim();
				paymentMethod.setExpirationMonth(Integer.parseInt(monthStr));
				String yearStr = expDate.substring(slashPos + 1).trim();
				paymentMethod.setExpirationYear(Integer.parseInt(yearStr));
			}
		}
		paymentMethod.setCardHolderName(inPaymentMethodElem.attributeValue("cardholder_name") == null ? "" : inPaymentMethodElem.attributeValue("cardholder_name"));
		paymentMethod.setNote(inPaymentMethodElem.attributeValue("note") == null ? "" : inPaymentMethodElem.attributeValue("note"));
		String billmelater = inPaymentMethodElem
				.attributeValue("bill_me_later");
		paymentMethod.setBillMeLater("true".equals(billmelater));
		return paymentMethod;
	}

	protected ShippingMethod makeShippingMethod(Store inStore, String inMethodId) {
		if (inMethodId == null) {
			inMethodId = "";
		}
		for (Iterator it = inStore.getAllShippingMethods().iterator(); it
				.hasNext();) {
			ShippingMethod method = (ShippingMethod) it.next();
			if (method.getId().equals(inMethodId)) {
				return method;
			}
		}
		// in case it was deleted?
		return null;
	}

	protected Address makeAddress(Element inAddressElem) {
		Address address = new Address();
		address.setName(inAddressElem.attributeValue("name"));
		address.setAddress1(inAddressElem.attributeValue("address1"));
		address.setAddress2(inAddressElem.attributeValue("address2"));
		address.setCity(inAddressElem.attributeValue("city"));
		address.setState(inAddressElem.attributeValue("state"));
		address.setZipCode(inAddressElem.attributeValue("zip_code"));
		address.setCountry(inAddressElem.attributeValue("country"));
		String addressType = inAddressElem.attributeValue("type");
		address.setPrefix(addressType);
		return address;
	}

	protected String decrypt(String inCreditCard) throws StoreException {
		if (inCreditCard == null || inCreditCard.trim().length() == 0
				|| !inCreditCard.startsWith("DES:")) {
			return inCreditCard;
		}
		// long encryptionKey = 7939805759879765L; //TODO: Move this to a
		// properties file
		// encryptionKey++;
		try {
			// StringEncryption encrypter = new StringEncryption(
			// StringEncryption.DES_ENCRYPTION_SCHEME, encryptionKey + "42" +
			// encryptionKey );
			// String code = inCreditCard.substring(4,inCreditCard.length());
			// //take off the DES:
			String decryptedString = getStringEncryption()
					.decrypt(inCreditCard);
			return decryptedString;
		} catch (Exception ex) {
			throw new StoreException(ex);
		}
	}

	public String encrypt(String inCreditCard) throws StoreException {
		try {
			if (inCreditCard == null || inCreditCard.trim().length() == 0) {
				return inCreditCard;
			}
			// long encryptionKey = 7939805759879765L; encryptionKey++;
			// StringEncryption encrypter = new StringEncryption(
			// StringEncryption.DES_ENCRYPTION_SCHEME, encryptionKey + "42" +
			// encryptionKey );
			String val = getStringEncryption().encrypt(inCreditCard);
			return val;
		} catch (OpenEditException ex) {
			throw new StoreException(ex);
		}
	}

	public StringEncryption getStringEncryption() {
		return fieldStringEncryption;
	}

	public void setStringEncryption(StringEncryption inStringEncryption) {
		fieldStringEncryption = inStringEncryption;
	}

	public XmlUtil getXmlUtil() {
		if (fieldXmlUtil == null) {
			fieldXmlUtil = new XmlUtil();
		}
		return fieldXmlUtil;
	}

	public void setXmlUtil(XmlUtil inXmlUtil) {
		fieldXmlUtil = inXmlUtil;
	}

	public PropertyDetailsArchive getFieldArchive() {
		return fieldFieldArchive;
	}

	public void setFieldArchive(PropertyDetailsArchive inFieldArchive) {
		fieldFieldArchive = inFieldArchive;
	}

	public UserManager getUserManager() {
		return fieldUserManager;
	}

	public void setUserManager(UserManager inUserManager) {
		fieldUserManager = inUserManager;
	}
	protected File getOrdersDirectory( Store inStore )
	{
		Page target = inStore.getPageManager().getPage("/WEB-INF/data/" + inStore.getCatalogId() + "/storeorders/");
		return new File(target.getContentItem().getAbsolutePath() );
	}
}