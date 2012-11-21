/*
 * Created on Mar 2, 2004
 */
package org.openedit.store.modules;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.links.Link;
import org.openedit.repository.ContentItem;
import org.openedit.store.Cart;
import org.openedit.store.CartItem;
import org.openedit.store.Category;
import org.openedit.store.CreditPaymentMethod;
import org.openedit.store.InventoryItem;
import org.openedit.store.Product;
import org.openedit.store.ProductAdder;
import org.openedit.store.PurchaseOrderMethod;
import org.openedit.store.ShippingMethod;
import org.openedit.store.Store;
import org.openedit.store.convert.ConvertStatus;
import org.openedit.store.customer.Address;
import org.openedit.store.customer.Customer;
import org.openedit.store.edit.ProductSourcePathCreator;
import org.openedit.store.orders.Order;
import org.openedit.store.orders.OrderArchive;
import org.openedit.store.orders.OrderState;
import org.openedit.store.search.ProductProcessor;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.page.Page;
import com.openedit.users.User;
import com.openedit.util.PathProcessor;
import com.openedit.util.PathUtilities;

/**
 * @author cburkey
 * 
 */
public class CartModule extends BaseStoreModule
{
	protected List fieldListConverters;

	private static final Log log = LogFactory.getLog(CartModule.class);

	public CartModule()
	{

	}

	public void loadCatalog(WebPageRequest inPageRequest)
			throws OpenEditException
	{
		String id = inPageRequest.getRequestParameter(CATEGORYID);
		if (id == null)
		{
			id = inPageRequest.getCurrentAction().getChildValue("catalogid");
		}
		if (id == null)
		{
			Page page = inPageRequest.getPage();
			id = page.get(CATEGORYID);
		}
		if (id == null)
		{
			Page page = inPageRequest.getContentPage();
			id = page.get(CATEGORYID);
		}

		if (id == null)
		{
			// get it from the path name
			String path = inPageRequest.getPath();
			id = PathUtilities.extractPageName(path);
		}
		Store store = getStore(inPageRequest);
		Category cat = store.getCategory(id);
		if (cat == null)
		{
			log.error("No Such catalog: " + id);
			inPageRequest.putPageValue("catalog", "No such catalog " + id);
		}
		else
		{
			// Crumb crumb = store.buildCrumb(cat);
			Link link = store.buildLink(cat, inPageRequest
					.findValue("url-prefix"));
			inPageRequest.putPageValue("crumb", link);
			inPageRequest.putPageValue("catalog", cat); // @deprecated
			inPageRequest.putPageValue("category", cat);
			getCart(inPageRequest).setLastVisitedCatalog(cat);
		}
	}

	public void loadProductDetails(WebPageRequest inPageRequest)
			throws OpenEditException
	{
/*		Page page = inPageRequest.getContentPage();
		if (page.isImage()) // this action is called for every image. Yuck.
		{
			return;
		}
		Store store = getStore(inPageRequest);
		String sourcePath = inPageRequest.getRequestParameter("sourcepath");
		String id = inPageRequest.getRequestParameter("productid");

		Product item = null;
		
		if (sourcePath != null)
		{
			item = store.getProductBySourcePath(sourcePath);
		}

		if( id != null)
		{
			if( id.startsWith("multiedit:") )
			{
				Data data = (Data)inPageRequest.getSessionValue(id);
				inPageRequest.putPageValue("product", item);
				inPageRequest.putPageValue("data", item);
				return;
			}
		}
		else if (item == null )
		{
			item = store.getProductBySourcePath(page);
		}

		if ( item == null )
		{
			if ( id == null )
			{
				id = page.getPageSettings().getUserDefined("product", "id");
			}
			item = store.getProduct(id);
			// get it from the path?
			// String path = page.getPath();
			// id = PathUtilities.extractPageName(path);
		}
*/		
		Product item = getProduct(inPageRequest);
		if (item == null)
		{
			log.debug("Product: is null, cannot put page value");
			return;
		}
		Store store = getStore(inPageRequest);

		inPageRequest.putPageValue("product", item);
		inPageRequest.putPageValue("data", item);
		Category cat = item.getDefaultCategory();
		if (cat != null)
		{
			Link crumb = store.buildLink(cat, inPageRequest
					.findValue("url-prefix"));
			inPageRequest.putPageValue("crumb", crumb);
		}
	}

	public void clearCart(WebPageRequest inPageRequest) throws Exception
	{
		Cart cart = getCart(inPageRequest);
		if (cart != null)
		{
			cart.removeAllItems();
		}
	}

	/**
	 * @deprecated used on aniara TODO: Replace with cart properties
	 * @param inReq
	 * @throws Exception
	 */
	public void setRegion(WebPageRequest inReq) throws Exception
	{
		Cart cart = getCart(inReq);
		String region = inReq.getRequestParameter("region");
		cart.setRegion(region);
	}

	public void updateCart(WebPageRequest inPageRequest) throws Exception
	{
		Cart cart = getCart(inPageRequest);
		getProductAdder().updateCart(inPageRequest, cart);
	}

	/**
	 * @param inPageRequest
	 */

	// Is this used anywhere?
	public void removeProduct(WebPageRequest inPageRequest) throws Exception
	{
		String productId = inPageRequest.getRequestParameter("productid");
		if (productId != null)
		{
			Store store = getStore(inPageRequest);
			Product product = store.getProduct(productId);
			Cart cart = getCart(inPageRequest);
			cart.removeProduct(product);
		}
	}

	protected ProductAdder getProductAdder()
	{
		ProductAdder adder = (ProductAdder) getBeanFactory().getBean(
				"ProductAdder");
		return adder;
	}

	/*
	 * public void addItem(WebPageRequest inPageRequest) throws Exception { Cart
	 * cart = getCart(inPageRequest); getProductAdder().addItem(inPageRequest,
	 * cart); }
	 */

	public void addCoupon(WebPageRequest inReq) throws Exception
	{
		Cart cart = getCart(inReq);
		getProductAdder().addCoupon(inReq, cart);
	}

	public Cart getCart(WebPageRequest inPageRequest) throws OpenEditException
	{
		Store store = getStore(inPageRequest);
		Cart cart = (Cart) inPageRequest.getSessionValue(store.getCatalogId()
				+ "cart");
		if (cart == null)
		{
			cart = new Cart();
			
			inPageRequest.putSessionValue(store.getCatalogId() + "cart", cart);
		}
		cart.setStore(store);
		inPageRequest.putPageValue("cart", cart);
		return cart;
	}

	public void createCustomer(WebPageRequest inPageRequest) throws Exception
	{
		Store store = getStore(inPageRequest);
		Cart cart = getCart(inPageRequest);
		String userName = inPageRequest.getRequestParameter("userName");
		String passWord = inPageRequest.getRequestParameter("password");
		if (cart.getCustomer() != null)
		{
			if (userName == null)
			{
				return;
			}
			if (cart.getCustomer().getUserName().equals(userName))
			{
				log.info("Already created " + userName);
				return;
			}
		}

		Customer customer = null;
		if (userName != null && passWord != null)
		{
			User user = getUserManager().getUser(userName);
			if (getUserManager().authenticate(user, passWord))
			{
				customer = store.getCustomerArchive().getCustomer(userName);
			}
		}
		
		
		
		
		
		if (customer == null)
		{
			if (!store.getAllowDuplicateAccounts())
			{
				// create a new customer
				String email = inPageRequest.getRequestParameter("email");
				email = email.toLowerCase().trim();
				User user = getUserManager().getUserByEmail(email);
				if (user != null)
				{
					// TODO: Replace with a stream API
					inPageRequest.forward(store.getStoreHome()
							+ "/customers/duplicate.html");
					return;
				}
			}
			if(passWord == null){
			customer = store.getCustomerArchive().createNewCustomer(null, null);
			} else{
				customer = store.getCustomerArchive().createNewCustomer(null, passWord);
			}
			log.info("Created new Customer");
		}
		cart.setCustomer(customer);
		inPageRequest.putPageValue("user", customer.getUser());
		inPageRequest.putPageValue("customer", customer);
	}

	public void updateCustomer(WebPageRequest inPageRequest) throws Exception
	{
		Cart cart = getCart(inPageRequest);
		Customer customer = cart.getCustomer();

		// Page one stuff
		String email = inPageRequest.getRequestParameter("email");
		if (email != null)
		{
			customer.setEmail(email);
			String firstName = inPageRequest.getRequestParameter("firstName");
			customer.setFirstName(firstName);
			String lastName = inPageRequest.getRequestParameter("lastName");
			customer.setLastName(lastName);
			String company = inPageRequest.getRequestParameter("company");
			if (company != null)
			{
				customer.setCompany(company);
			}
			else
			{
				customer.setCompany(customer.getFirstName() + " "
						+ customer.getLastName());
			}
			customer.setAllowEmail(Boolean.valueOf(
					inPageRequest.getRequestParameter("allowEmail"))
					.booleanValue());
			customer.setPhone1(inPageRequest.getRequestParameter("phone1"));
			customer.setFax(inPageRequest.getRequestParameter("fax"));
			// TODO: Remove these, replace with list that the usermanager uses
			// to display data
			customer.setUserField1(inPageRequest
					.getRequestParameter("userfield1"));
			customer.setUserField2(inPageRequest
					.getRequestParameter("userfield2"));
			if (customer.getReferenceNumber() == null)
			{
				customer.setReferenceNumber(inPageRequest
						.getRequestParameter("referenceNumber"));
			}
		}

		if (inPageRequest.getRequestParameter("billing.address1.value") != null)
		{
			populateCustomerAddress(inPageRequest, customer.getBillingAddress());
		}
		if (inPageRequest.getRequestParameter("shipping.address1.value") != null)
		{
			populateCustomerAddress(inPageRequest, customer
					.getShippingAddress());
		}

		List taxrates = cart.getStore().getTaxRatesFor(
				customer.getShippingAddress().getState());
		if(customer.getShippingAddress().getState() == null){
			taxrates = cart.getStore().getTaxRatesFor(
					customer.getBillingAddress().getState());
		}
		customer.setTaxRates(taxrates);	
		
		if (inPageRequest.getRequestParameter("taxExemptId") != null)
		{
			customer.setTaxExemptId(inPageRequest
					.getRequestParameter("taxExemptId"));
		}

		log.debug("Setting cart customer to " + customer);
		cart.setCustomer(customer);
		cart.getStore().getCustomerArchive().saveCustomer(customer);
		inPageRequest.putPageValue("customer", customer);
	}

	protected void populateCustomerAddress(WebPageRequest inPageRequest,
			Address inAddress)
	{

		String inPrefix = inAddress.getPrefix();
		inAddress.setAddress1(getAddressValue(inPageRequest, inPrefix,
				"address1"));
		inAddress.setAddress2(getAddressValue(inPageRequest, inPrefix,
				"address2"));
		inAddress.setCity(getAddressValue(inPageRequest, inPrefix, "city"));
		String state = getAddressValue(inPageRequest, inPrefix, "state");
		if (state != null)
		{
			state = state.toUpperCase();
		}
		inAddress.setState(state);
		inAddress
				.setCountry(getAddressValue(inPageRequest, inPrefix, "country"));
		inAddress
				.setZipCode(getAddressValue(inPageRequest, inPrefix, "zipCode"));
	}

	private String getAddressValue(WebPageRequest inPageRequest,
			String inPrefix, String key)
	{
		String value = inPageRequest.getRequestParameter(inPrefix + "." + key
				+ ".value");
		if (value == null)
		{
			value = inPageRequest.getRequestParameter(key + ".value");

		}
		return value;
	}

	public Customer loadCustomer(WebPageRequest inPageRequest) throws Exception
	{
		Customer customer = null;
		Cart cart = getCart(inPageRequest);
		String customerId = inPageRequest.getRequestParameter("customerId");
		if (customerId == null)
		{
			User user = inPageRequest.getUser();
			if (user != null)
			{
				customerId = user.getUserName();
			}
		}

		if (customerId != null)
		{
			Store store = getStore(inPageRequest);

			customer = store.getCustomerArchive().getCustomer(customerId);
			if (customer == null)
			{
				// set error
				inPageRequest.putPageValue("errorMessage", "No such customer");
			}
			else
			{
				cart.setCustomer(customer);
			}
		}
		if (cart.getCustomer() != null)
		{
			populateAddressList(cart.getCustomer());

			inPageRequest.putPageValue("customer", cart.getCustomer());
		}

		return customer;
	}

	private void populateAddressList(Customer customer)
	{
		User user = customer.getUser();
		String alist = (String) user.getProperty("addresslist");
		customer.getAddressList().clear();

		if (alist != null && !alist.equals(""))
		{
			String[] current = alist.split(",");
			for (int i = 0; i < current.length; i++)
			{
				String prefix = current[i];
				Address address = new Address();
				address.setPropertyContainer(user);
				address.setPrefix(prefix);
				address.setAddress1((String) user.getProperty(prefix
						+ "Address1"));
				address.setAddress2((String) user.getProperty(prefix
						+ "Address2"));
				address.setCity((String) user.getProperty(prefix + "City"));
				address.setCountry((String) user
						.getProperty(prefix + "Country"));
				address.setState((String) user.getProperty(prefix + "State"));
				address.setZipCode((String) user
						.getProperty(prefix + "ZipCode"));
				customer.addAddress(address);
			}
		}
	}

	public void saveAddress(WebPageRequest inPageRequest)
			throws OpenEditException
	{

		String prefix = inPageRequest.getRequestParameter("prefix");

		if (prefix == null)
		{
			return;
		}
		Cart cart = getCart(inPageRequest);
		Customer customer = cart.getCustomer();
		Address address = new Address();
		address.setPropertyContainer(customer.getUser());
		address.setPrefix(prefix);
		populateCustomerAddress(inPageRequest, address);
		customer.addAddress(address);
		updateAddresses(customer);
		getStore(inPageRequest).saveCustomer(customer);
	}

	public void removeAddress(WebPageRequest inPageRequest)
			throws OpenEditException
	{
		String prefix = getAddressPrefix(inPageRequest);

		Cart cart = getCart(inPageRequest);
		Customer customer = cart.getCustomer();

		Address toRemove = customer.getAddress(prefix);
		if (toRemove != null)
		{
			log.debug("Removing address with prefix: " + toRemove.getPrefix());
			customer.removeAddress(toRemove);
			String[] fields = new String[] { "Address1", "Address2", "City",
					"State", "ZipCode", "Country" };
			for (int i = 0; i < fields.length; i++)
			{
				customer.getUser().remove(prefix + fields[i]);
			}
			updateAddresses(customer);
			getStore(inPageRequest).saveCustomer(customer);
		}

	}

	protected String getAddressPrefix(WebPageRequest inPageRequest)
	{
		String prefix = inPageRequest.getRequestParameter("prefix");
		if (prefix == null)
		{
			prefix = "";
		}
		return prefix;
	}

	private void updateAddresses(Customer customer)
	{
		List addresslist = customer.getAddressList();
		StringBuffer buff = new StringBuffer();
		for (Iterator iterator = addresslist.iterator(); iterator.hasNext();)
		{
			Address add = (Address) iterator.next();
			buff.append(add.getPrefix());
			if (iterator.hasNext())
			{
				buff.append(",");
			}
		}
		String list = buff.toString();
		if (list.length() == 0)
		{
			customer.getUser().remove("addresslist");
		}
		else
		{
			customer.getUser().put("addresslist", buff.toString());
		}
	}

	public void saveCreditPaymentMethodData(WebPageRequest inPageRequest)
			throws OpenEditException

	{
		Cart cart = getCart(inPageRequest);

		// check for billing info
		if (inPageRequest.getRequestParameter("billing.address1.value") != null)
		{
			populateCustomerAddress(inPageRequest, cart.getCustomer()
					.getBillingAddress());
		}

		CreditPaymentMethod method = null;
		// PO
		String purchaseorder = inPageRequest
				.getRequestParameter("purchaseorder");
		if (purchaseorder != null && purchaseorder.trim().length() > 0)
		{
			PurchaseOrderMethod po = new PurchaseOrderMethod();
			po.setPoNumber(purchaseorder);
			method = po;
		}
		else
		{
			method = new CreditPaymentMethod();
		}
		// Card type
		String cardType = inPageRequest.getRequestParameter("cardType");
		if (cardType != null)
		{
			method.setCreditCardType(getStore(inPageRequest).getCreditCardType(
					cardType));
		}
		// Card number
		String cardNumber = inPageRequest.getRequestParameter("cardNumber");
		method.setCardNumber(cardNumber);
		String cardVerificationCode = inPageRequest.getRequestParameter("cardVerificationCode");
		method.setCardVerificationCode(cardVerificationCode);
		// Expiration month
		String expirationMonth = inPageRequest
				.getRequestParameter("expirationMonth");
		if (expirationMonth != null && !expirationMonth.trim().equals(""))
		{
			method.setExpirationMonth(Integer.valueOf(expirationMonth)
					.intValue());
		}
		// Experation year
		String expirationYear = inPageRequest
				.getRequestParameter("expirationYear");
		if (expirationYear != null && !expirationYear.trim().equals(""))
		{
			method
					.setExpirationYear(Integer.valueOf(expirationYear)
							.intValue());
		}

		String note = inPageRequest.getRequestParameter("ordernote");
		if (note != null)
		{
			method.setNote(note);
		}

		String bill = inPageRequest.getRequestParameter("billmelater");
		boolean billMeLater = (bill != null && bill.equalsIgnoreCase("true"));
		method.setBillMeLater(billMeLater);

		// Save payment method
		cart.getCustomer().setPaymentMethod(method);
	}

	public void autoSelectShipping(WebPageRequest inPageRequest)
			throws OpenEditException
	{
		Cart cart = getCart(inPageRequest);
		if (!cart.hasZeroSubTotal())
		{
			List availableMethods = cart.getAvailableShippingMethods();
			Store store = getStore(inPageRequest);
			if (availableMethods.size() == 1)
			{
				cart
						.setShippingMethod((ShippingMethod) availableMethods
								.get(0));
				inPageRequest.redirect(cart.getStore().getStoreHome()
						+ "/checkout2.html");
			}
			else if (availableMethods.size() > 1
					&& store.isAssignShippingMethod())
			{
				cart
						.setShippingMethod((ShippingMethod) availableMethods
								.get(0));
				inPageRequest.redirect(cart.getStore().getStoreHome()
						+ "/checkout2.html");
			}
		}
	}

	public void saveShippingMethod(WebPageRequest inPageRequest)
			throws OpenEditException
	{
		String method = inPageRequest.getRequestParameter("shippingmethod");
		Cart cart = getCart(inPageRequest);
		Store store = getStore(inPageRequest);
		if (method != null)
		{
			ShippingMethod smethod = store.findShippingMethod(method);
			cart.setShippingMethod(smethod);
		}
		else if (cart.getShippingMethod() == null
				&& store.isAssignShippingMethod())
		{
			List availableMethods = cart.getAvailableShippingMethods();
			if (availableMethods.size() > 0)
			{
				cart
						.setShippingMethod((ShippingMethod) availableMethods
								.get(0));
			}
		}
	}

	public Order processOrder(WebPageRequest inPageRequest) throws Exception
	{
		Store store = getStore(inPageRequest);
		Cart cart = getCart(inPageRequest);
		Order order = store.getOrderGenerator().createNewOrder(store, cart);
		OrderState orderState = order.getOrderStatus();
		cart.setCurrentOrder(order);

		if (cart.isEmpty())
		{
			orderState.setOk(false);
			orderState.setDescription("Error: Cart is empty.<br><br>");
			return order;
		}

		// Assign default shipping method if one has not been assigned
		if (cart.getShippingMethod() == null)
		{
			List shippingMethods = cart.getAvailableShippingMethods();
			if (shippingMethods.size() > 0)
			{
				cart.setShippingMethod((ShippingMethod) shippingMethods.get(0));
			}
		}
		if(cart.getAdjustments() != null){
			order.setAdjustments(cart.getAdjustments());
		}
		order.setShippingAddress(cart.getShippingAddress());
		// Export order to XML
		store.saveOrder(order);
		// process the order with the varous processors
		inPageRequest.putPageValue("order", order);
		store.processOrder(inPageRequest, order);
		
		if (order.getOrderStatus().isOk())
		{
			// Remove items from stock
			for (Iterator iter = cart.getItemIterator(); iter.hasNext();)
			{
				CartItem cartItem = (CartItem) iter.next();
				InventoryItem inventoryItem = cartItem.getInventoryItem();
				if (inventoryItem != null)
				{
					inventoryItem.decreaseQuantityInStock(cartItem
							.getQuantity());
				}
			}

			// Save updated product inventory values to disk
			for (Iterator iter = cart.getItemIterator(); iter.hasNext();)
			{
				CartItem cartItem = (CartItem) iter.next();
				Product product = cartItem.getProduct();
				store.getProductArchive().saveProduct(product);
			}
			// Order succeeded - remove cart
			cart.removeAllItems();
			cart.getAdjustments().clear();
			
		}
		store.saveOrder(order);

		return order;
	}

	public void archiveOrders(WebPageRequest inPageRequest) throws Exception
	{
		Store store = getStore(inPageRequest);
		OrderArchive orderArchive = store.getOrderArchive();
		orderArchive.archiveOrderData(store);
	}

	public void convertData(WebPageRequest inPageRequest) throws Exception
	{

		// loop over all the converters and let them convert what they find
		// clear out the reader so its all reloaded
		Store store = getStore(inPageRequest);

		boolean forced = Boolean.parseBoolean(inPageRequest
				.getRequestParameter("forced"));

		ConvertStatus errorlog = store.convertCatalog(inPageRequest.getUser(),forced);
		if (inPageRequest != null)
		{
			inPageRequest.removeSessionValue("store");
			inPageRequest.putPageValue("exception-report", errorlog.getLog());
			inPageRequest.putPageValue("status", errorlog);
		}
	}

	/**
	 * 
	 */
	public void reIndexStore(WebPageRequest inPageRequest) throws Exception
	{
		Store store = getStore(inPageRequest);

		store.reindexAll();
		store.getCategoryArchive().reloadCategories();
	}

	
}