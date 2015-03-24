/*
 * Created on Mar 2, 2004
 */
package org.openedit.store.modules;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.entermedia.MediaArchive;
import org.openedit.event.WebEvent;
import org.openedit.links.Link;
import org.openedit.profile.UserProfile;
import org.openedit.store.Cart;
import org.openedit.store.CartItem;
import org.openedit.store.Category;
import org.openedit.store.Coupon;
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
import org.openedit.store.orders.Order;
import org.openedit.store.orders.OrderArchive;
import org.openedit.store.orders.OrderState;
import org.openedit.store.orders.SubmittedOrder;
import org.openedit.users.UserSearcher;
import org.openedit.util.DateStorageUtil;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.page.Page;
import com.openedit.users.User;
import com.openedit.util.PathUtilities;

/**
 * @author cburkey
 * 
 */
public class CartModule extends BaseStoreModule {
	protected List fieldListConverters;

	private static final Log log = LogFactory.getLog(CartModule.class);

	public CartModule() {

	}

	public void loadCatalog(WebPageRequest inPageRequest)
			throws OpenEditException {
		String id = inPageRequest.getRequestParameter(CATEGORYID);
		if (id == null) {
			id = inPageRequest.getCurrentAction().getChildValue("catalogid");
		}
		if (id == null) {
			Page page = inPageRequest.getPage();
			id = page.get(CATEGORYID);
		}
		if (id == null) {
			Page page = inPageRequest.getContentPage();
			id = page.get(CATEGORYID);
		}

		if (id == null) {
			// get it from the path name
			String path = inPageRequest.getPath();
			id = PathUtilities.extractPageName(path);
		}
		Store store = getStore(inPageRequest);
		Category cat = store.getCategory(id);
		if (cat == null) {
			log.error("No Such catalog: " + id);
			inPageRequest.putPageValue("catalog", "No such catalog " + id);
		} else {
			// Crumb crumb = store.buildCrumb(cat);
			Link link = store.buildLink(cat,
					inPageRequest.findValue("url-prefix"));
			inPageRequest.putPageValue("crumb", link);
			inPageRequest.putPageValue("catalog", cat); // @deprecated
			inPageRequest.putPageValue("category", cat);
			getCart(inPageRequest).setLastVisitedCatalog(cat);
		}
	}

	public void loadProductDetails(WebPageRequest inPageRequest)
			throws OpenEditException {
		/*
		 * Page page = inPageRequest.getContentPage(); if (page.isImage()) //
		 * this action is called for every image. Yuck. { return; } Store store
		 * = getStore(inPageRequest); String sourcePath =
		 * inPageRequest.getRequestParameter("sourcepath"); String id =
		 * inPageRequest.getRequestParameter("productid");
		 * 
		 * Product item = null;
		 * 
		 * if (sourcePath != null) { item =
		 * store.getProductBySourcePath(sourcePath); }
		 * 
		 * if( id != null) { if( id.startsWith("multiedit:") ) { Data data =
		 * (Data)inPageRequest.getSessionValue(id);
		 * inPageRequest.putPageValue("product", item);
		 * inPageRequest.putPageValue("data", item); return; } } else if (item
		 * == null ) { item = store.getProductBySourcePath(page); }
		 * 
		 * if ( item == null ) { if ( id == null ) { id =
		 * page.getPageSettings().getUserDefined("product", "id"); } item =
		 * store.getProduct(id); // get it from the path? // String path =
		 * page.getPath(); // id = PathUtilities.extractPageName(path); }
		 */
		Product item = getProduct(inPageRequest);
		if (item == null) {
			log.debug("Product: is null, cannot put page value");
			return;
		}
		Store store = getStore(inPageRequest);

		inPageRequest.putPageValue("product", item);
		inPageRequest.putPageValue("data", item);
		Category cat = item.getDefaultCategory();
		if (cat != null) {
			Link crumb = store.buildLink(cat,
					inPageRequest.findValue("url-prefix"));
			inPageRequest.putPageValue("crumb", crumb);
		}
	}

	public void clearCart(WebPageRequest inPageRequest) throws Exception {
		Cart cart = getCart(inPageRequest);
		if (cart != null) {
			cart.removeAllItems();
		}
	}

	/**
	 * @deprecated used on aniara TODO: Replace with cart properties
	 * @param inReq
	 * @throws Exception
	 */
	public void setRegion(WebPageRequest inReq) throws Exception {
		Cart cart = getCart(inReq);
		String region = inReq.getRequestParameter("region");
		cart.setRegion(region);
	}

	public void updateCart(WebPageRequest inPageRequest) throws Exception {
		Cart cart = getCart(inPageRequest);
		getProductAdder().updateCart(inPageRequest, cart);

	}

	/**
	 * @param inPageRequest
	 */

	// Is this used anywhere?
	public void removeProduct(WebPageRequest inPageRequest) throws Exception {
		String productId = inPageRequest.getRequestParameter("productid");
		if (productId != null) {
			Store store = getStore(inPageRequest);
			Product product = store.getProduct(productId);
			Cart cart = getCart(inPageRequest);
			cart.removeProduct(product);
		}
	}
	
	public void removeItemById(WebPageRequest inPageRequest) throws Exception {
		String id = inPageRequest.getRequestParameter("itemid");
		if (id != null) {
			Cart cart = getCart(inPageRequest);
			CartItem cartitem  = null;
			//find cartitem first
			for (Iterator<?> iterator = cart.getItemIterator(); iterator.hasNext();)
			{
				CartItem item = (CartItem) iterator.next();
				if (id.equals(item.getId()))
				{
					cartitem = item;
					break;
				}
			}
			cart.removeById(id);
			Coupon.recalculateAdjustments(cart);
		}
	}
	
	public void toggleProduct(WebPageRequest inPageRequest) throws Exception {
		String productId = inPageRequest.getRequestParameter("productid");
		if (productId != null) {
			Store store = getStore(inPageRequest);
			Product product = store.getProduct(productId);
			Cart cart = getCart(inPageRequest);
			if(cart.containsProduct(productId)){
				cart.removeProduct(product);	
			} else{
				updateCart(inPageRequest);
			}
			
		}
	}

	protected ProductAdder getProductAdder() {
		ProductAdder adder = (ProductAdder) getBeanFactory().getBean(
				"ProductAdder");
		return adder;
	}

	/*
	 * public void addItem(WebPageRequest inPageRequest) throws Exception { Cart
	 * cart = getCart(inPageRequest); getProductAdder().addItem(inPageRequest,
	 * cart); }
	 */

	public void addCoupon(WebPageRequest inReq) throws Exception {
		Cart cart = getCart(inReq);
		getProductAdder().addCoupon(inReq, cart);
	}

	public Cart getCart(WebPageRequest inPageRequest) throws OpenEditException {
		Store store = getStore(inPageRequest);
		Cart cart = (Cart) inPageRequest.getSessionValue(store.getCatalogId()
				+ "cart");
		if (cart == null) {
			cart = new Cart();

			inPageRequest.putSessionValue(store.getCatalogId() + "cart", cart);
		}
		cart.setStore(store);
		inPageRequest.putPageValue("cart", cart);
		return cart;
	}

	public void createCustomer(WebPageRequest inReq) throws Exception {
		Store store = getStore(inReq);
		Cart cart = getCart(inReq);
		
		//page could have username, email, password + first/last names
		String username = inReq.getRequestParameter("userName");
		String email = inReq.getRequestParameter("email");
		String password = inReq.getRequestParameter("password");
		String firstname = inReq.getRequestParameter("firstName");
		String lastname = inReq.getRequestParameter("lastName");
		
		if (username!=null && username.isEmpty()) {
			username = null;
		}
		if (email!=null && email.isEmpty()) {
			email = null;
		} else if (email != null){
			email = email.toLowerCase().trim();
		}
		if (password!=null && password.isEmpty()) {
			password = null;
		}
		if (firstname!=null && firstname.isEmpty()) {
			firstname = null;
		}
		if (lastname!=null && lastname.isEmpty()) {
			lastname = null;
		}
		
		if (cart.getCustomer() != null) {
			if (username == null && email == null) {
				return;
			}
			if ((username != null || email != null) && password != null) {
				User user = null;
				if(username != null){
					user = getUserManager(inReq).getUser(username);
				}
				if(user == null && email != null){
					user = getUserManager(inReq).getUserByEmail(email);
				}
				if (user != null && cart.getCustomer().getUserName().equals(user.getUserName())) {
					log.info("Already created " + username);
					return;
				}
			}
		}

		Customer customer = null;
		if ((username != null || email != null) && password != null) {
			User user = null;
			if(username != null){
				user = getUserManager(inReq).getUser(username);
			}
			if(user == null && email != null){
				user = getUserManager(inReq).getUserByEmail(email);
			}
			
			if(user != null){
				if (getUserManager(inReq).authenticate(user, password)) {
					customer = store.getCustomerArchive().getCustomer(username);
					inReq.putSessionValue("user", user);
				} else{
					inReq.putPageValue("authenticationstatus", "failed" );
					String redirecturl = inReq.getPageProperty("registerpage");
					cart.setCustomer(null);
					inReq.setCancelActions(true);
					if (redirecturl!= null) {
						inReq.forward(redirecturl);
					}
					return;
				}
			}
		}

		if (customer == null) {
			//Brand new user
			if (password == null) {
				customer = store.getCustomerArchive().createNewCustomer(null,
						null);
			} else {
				customer = store.getCustomerArchive().createNewCustomer(null,
						password);
			}
			log.info("Created new Customer");
			customer.getUser().setEnabled(true);
			if(email != null){
				customer.setEmail(email);
			}
			inReq.putSessionValue("user", customer.getUser());
		}
		
		//final sanity check 
		if (firstname!=null) {
			customer.setFirstName(firstname);
		}
		if (lastname!=null) {
			customer.setLastName(lastname);
		}
		customer.setPaymentMethod(null);//make sure payment method has been reset
		
		cart.setCustomer(customer);
		inReq.putPageValue("user", customer.getUser());
		inReq.putPageValue("customer", customer);
	}

	public void updateCustomer(WebPageRequest inReq) throws Exception {
		Cart cart = getCart(inReq);
		Customer customer = cart.getCustomer();
		Store store = getStore(inReq);
		// Page one stuff
		String email = inReq.getRequestParameter("email");
		if (email != null) {
			customer.setEmail(email);
			String firstName = inReq.getRequestParameter("firstName");
			customer.setFirstName(firstName);
			String lastName = inReq.getRequestParameter("lastName");
			customer.setLastName(lastName);
			String company = inReq.getRequestParameter("company");
			if (company != null) {
				customer.setCompany(company);
			} else {
				customer.setCompany(customer.getFirstName() + " "
						+ customer.getLastName());
			}
			customer.setAllowEmail(Boolean.valueOf(
					inReq.getRequestParameter("allowEmail"))
					.booleanValue());
			customer.setPhone1(inReq.getRequestParameter("phone1"));
			customer.setFax(inReq.getRequestParameter("fax"));
			// TODO: Remove these, replace with list that the usermanager uses
			// to display data
			customer.setUserField1(inReq
					.getRequestParameter("userfield1"));
			customer.setUserField2(inReq
					.getRequestParameter("userfield2"));
			if (customer.getReferenceNumber() == null) {
				customer.setReferenceNumber(inReq
						.getRequestParameter("referenceNumber"));
			}
		}
		if (inReq.getRequestParameter("billing.address1.value") != null && 
				!inReq.getRequestParameter("billing.address1.value").isEmpty()) {
			populateCustomerAddress(inReq, customer.getBillingAddress());
			
			log.info("billing address " +customer.getBillingAddress()); 
			
			cart.setBillingAddress(customer.getBillingAddress());
		}
		if (inReq.getRequestParameter("shipping.address1.value") != null && 
				!inReq.getRequestParameter("shipping.address1.value").isEmpty()) {
			log.info("Shipping address found, saving it");
			populateCustomerAddress(inReq,
					customer.getShippingAddress());
			log.info("Shipping address " +customer.getShippingAddress()); 
			
			cart.setShippingAddress(customer.getShippingAddress());
		}
		
		Address taxRateAddress = null;
		String taxrate = null;
		if (customer.getShippingAddress()!=null && 
				customer.getShippingAddress().getState()!=null &&
				!customer.getShippingAddress().getState().isEmpty()){
			taxRateAddress = customer.getShippingAddress();
			taxrate = taxRateAddress.getState();
			
		} else if (customer.getBillingAddress()!=null && 
				customer.getBillingAddress().getState()!=null &&
				!customer.getBillingAddress().getState().isEmpty()){
			taxRateAddress = customer.getBillingAddress();
			taxrate = taxRateAddress.getState();
		}
		UserProfile profile = (UserProfile) cart.getStore().getSearcherManager().getSearcher(store.getCatalogId(), "userprofile").searchById(customer.getUser().getId());
		if(profile != null){
			if(profile.get("state_province") != null){
				taxrate = profile.get("state_province");
			}
		}
				
		if (taxrate != null){
			List<?> taxrates = cart.getStore().getTaxRatesFor(taxrate);
			customer.setTaxRates(taxrates);
		}

		if (inReq.getRequestParameter("taxExemptId") != null && 
				!inReq.getRequestParameter("taxExemptId").isEmpty()) {
			customer.setTaxExemptId(inReq
					.getRequestParameter("taxExemptId"));
		}
		
		UserSearcher searcher = (UserSearcher)store.getSearcherManager().getSearcher("system", "user");
		String[] fields = inReq.getRequestParameters("field");
		searcher.updateData(inReq, fields, customer);

		log.debug("Setting cart customer to " + customer);
		cart.setCustomer(customer);
		cart.getStore().getCustomerArchive().saveCustomer(customer);
//		cart.setShippingAddress(customer.getShippingAddress());
//		cart.setBillingAddress(customer.getBillingAddress());
		customer.getUser().setEnabled(true);

		inReq.putPageValue("customer", customer);
	}

	protected void populateCustomerAddress(WebPageRequest inPageRequest,
			Address inAddress) {

		String inPrefix = inAddress.getPrefix();
		String addressid = getAddressValue(inPageRequest, inPrefix, "id");
		if (addressid != null) {
			inAddress.setId(addressid);
		}
		String storeName = getAddressValue(inPageRequest, inPrefix, "name");
		if (storeName != null) {
			inAddress.setName(storeName);
		}
		inAddress.setAddress1(getAddressValue(inPageRequest, inPrefix,
				"address1"));
		inAddress.setAddress2(getAddressValue(inPageRequest, inPrefix,
				"address2"));
		inAddress.setCity(getAddressValue(inPageRequest, inPrefix, "city"));
		String state = getAddressValue(inPageRequest, inPrefix, "state");
		inAddress.setState(state);
		inAddress
				.setCountry(getAddressValue(inPageRequest, inPrefix, "country"));
		inAddress
				.setZipCode(getAddressValue(inPageRequest, inPrefix, "zipCode"));
	}

	private String getAddressValue(WebPageRequest inPageRequest,
			String inPrefix, String key) {
		String value = inPageRequest.getRequestParameter(inPrefix + "." + key
				+ ".value");
		if (value == null) {
			value = inPageRequest.getRequestParameter(key + ".value");

		}
		return value;
	}

	public Customer loadCustomer(WebPageRequest inPageRequest) throws Exception {
		Customer customer = null;
		Cart cart = getCart(inPageRequest);
		String customerId = inPageRequest.getRequestParameter("customerId");
		if (customerId == null) {
			User user = inPageRequest.getUser();
			if (user != null) {
				customerId = user.getUserName();
			}
		}

		if (customerId != null) {
			Store store = getStore(inPageRequest);

			customer = store.getCustomerArchive().getCustomer(customerId);
			if (customer == null) {
				// set error
				inPageRequest.putPageValue("errorMessage", "No such customer");
			} else {
				cart.setCustomer(customer);
			}
		}
		if (cart.getCustomer() != null) {
			populateAddressList(cart.getCustomer());

			inPageRequest.putPageValue("customer", cart.getCustomer());
		} else {
			
		}

		return customer;
	}

	private void populateAddressList(Customer customer) {
		User user = customer.getUser();
		String alist = (String) user.getProperty("addresslist");
		customer.getAddressList().clear();

		if (alist != null && !alist.equals("")) {
			String[] current = alist.split(",");
			for (int i = 0; i < current.length; i++) {
				String prefix = current[i];
				Address address = new Address();
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
			throws OpenEditException {

		String prefix = inPageRequest.getRequestParameter("prefix");

		if (prefix == null) {
			return;
		}
		Cart cart = getCart(inPageRequest);
		Customer customer = cart.getCustomer();
		Address address = new Address();
		address.setPrefix(prefix);
		populateCustomerAddress(inPageRequest, address);
		customer.addAddress(address);
		updateAddresses(customer);
		getStore(inPageRequest).saveCustomer(customer);
	}

	public void removeAddress(WebPageRequest inPageRequest)
			throws OpenEditException {
		String prefix = getAddressPrefix(inPageRequest);

		Cart cart = getCart(inPageRequest);
		Customer customer = cart.getCustomer();

		Address toRemove = customer.getAddress(prefix);
		if (toRemove != null) {
			log.debug("Removing address with prefix: " + toRemove.getPrefix());
			customer.removeAddress(toRemove);
			String[] fields = new String[] { "Address1", "Address2", "City",
					"State", "ZipCode", "Country" };
			for (int i = 0; i < fields.length; i++) {
				customer.getUser().remove(prefix + fields[i]);
			}
			updateAddresses(customer);
			getStore(inPageRequest).saveCustomer(customer);
		}

	}

	protected String getAddressPrefix(WebPageRequest inPageRequest) {
		String prefix = inPageRequest.getRequestParameter("prefix");
		if (prefix == null) {
			prefix = "";
		}
		return prefix;
	}

	private void updateAddresses(Customer customer) {
		List addresslist = customer.getAddressList();
		StringBuffer buff = new StringBuffer();
		for (Iterator iterator = addresslist.iterator(); iterator.hasNext();) {
			Address add = (Address) iterator.next();
			buff.append(add.getPrefix());
			if (iterator.hasNext()) {
				buff.append(",");
			}
		}
		String list = buff.toString();
		if (list.length() == 0) {
			customer.getUser().remove("addresslist");
		} else {
			customer.getUser().put("addresslist", buff.toString());
		}
	}
	
	public void updatePaymentAdjustments(WebPageRequest inReq) 
			throws OpenEditException
	{
		Cart cart = getCart(inReq);
		String[] products = inReq.getRequestParameters("products");
		if (products!=null && products.length!=0){
			for(String product:products){
				String frequency = inReq.getRequestParameter(product+".frequency");
				String occurrences = inReq.getRequestParameter(product+".occurrences");
				if (frequency!=null && occurrences!=null){
					cart.updatePartialPayment(product, frequency, occurrences);
				}
			}
		}
		
	}

	public void saveCreditPaymentMethodData(WebPageRequest inReq)
			throws OpenEditException

	{
		Cart cart = getCart(inReq);

		// check for billing info
		if (inReq.getRequestParameter("billing.address1.value") != null) {
			populateCustomerAddress(inReq, cart.getCustomer()
					.getBillingAddress());
		}

		CreditPaymentMethod method = null;
		// PO
		String purchaseorder = inReq
				.getRequestParameter("purchaseorder");
		if (purchaseorder != null && purchaseorder.trim().length() > 0) {
			PurchaseOrderMethod po = new PurchaseOrderMethod();
			po.setPoNumber(purchaseorder);
			method = po;
		} else {
			method = new CreditPaymentMethod();
		}
		// Card type
		String cardType = inReq.getRequestParameter("cardType");
		if (cardType != null && !cardType.isEmpty()) {
			method.setCreditCardType(getStore(inReq).getCreditCardType(
					cardType));
		}
		// Card number
		String cardNumber = inReq.getRequestParameter("cardNumber");
		if (!CreditPaymentMethod.isMasked(cardNumber)){
			method.setCardNumber(cardNumber);
		}
		String cardVerificationCode = inReq.getRequestParameter("cardVerificationCode");
		if (!CreditPaymentMethod.isMasked(cardVerificationCode)){
			method.setCardVerificationCode(cardVerificationCode);
		}
		// Expiration month
		String expirationMonth = inReq
				.getRequestParameter("expirationMonth");
		if (expirationMonth != null && !expirationMonth.trim().equals("")) {
			method.setExpirationMonth(Integer.valueOf(expirationMonth)
					.intValue());
		}
		// Expiration year
		String expirationYear = inReq
				.getRequestParameter("expirationYear");
		if (expirationYear != null && !expirationYear.trim().equals("")) {
			method.setExpirationYear(Integer.valueOf(expirationYear).intValue());
		}
		//cardholder name
		String cardholdername = inReq.getRequestParameter("cardholdername");
		method.setCardHolderName(cardholdername == null || cardholdername.isEmpty() ? "" : cardholdername);
		//note
		String note = inReq.getRequestParameter("ordernote");
		if (note != null) {
			method.setNote(note);
		}
		String bill = inReq.getRequestParameter("billmelater");
		boolean billMeLater = (bill != null && bill.equalsIgnoreCase("true"));
		method.setBillMeLater(billMeLater);

		// Save payment method
		cart.getCustomer().setPaymentMethod(method);
	}

	public void autoSelectShipping(WebPageRequest inPageRequest)
			throws OpenEditException {
		Cart cart = getCart(inPageRequest);
		if (!cart.hasZeroSubTotal()) {
			List availableMethods = cart.getAvailableShippingMethods();
			Store store = getStore(inPageRequest);
			if (availableMethods.size() == 1) {
				cart.setShippingMethod((ShippingMethod) availableMethods.get(0));
				inPageRequest.redirect(cart.getStore().getStoreHome()
						+ "/checkout2.html");
			} else if (availableMethods.size() > 1
					&& store.isAssignShippingMethod()) {
				cart.setShippingMethod((ShippingMethod) availableMethods.get(0));
				inPageRequest.redirect(cart.getStore().getStoreHome()
						+ "/checkout2.html");
			}
		}
	}

	public void saveShippingMethod(WebPageRequest inPageRequest)
			throws OpenEditException {
		String method = inPageRequest.getRequestParameter("shippingmethod");
		Cart cart = getCart(inPageRequest);
		Store store = getStore(inPageRequest);
		if (method != null) {
			ShippingMethod smethod = store.findShippingMethod(method);
			cart.setShippingMethod(smethod);
		} else if (cart.getShippingMethod() == null
				&& store.isAssignShippingMethod()) {
			List availableMethods = cart.getAvailableShippingMethods();
			if (availableMethods.size() > 0) {
				cart.setShippingMethod((ShippingMethod) availableMethods.get(0));
			}
		}
	}

	public synchronized Order processOrder(WebPageRequest inPageRequest) throws Exception {
		Store store = getStore(inPageRequest);
		Cart cart = getCart(inPageRequest);
		if(cart.isEmpty()){
			inPageRequest.putPageValue("orderState", "Your cart is empty");
			return null;
		}
		
		Order order = store.getOrderGenerator().createNewOrder(store, cart);
		OrderState orderState = order.getOrderStatus();
		cart.setCurrentOrder(order);

		if (cart.isEmpty()) {
			orderState.setOk(false);
			orderState.setDescription("Error: Cart is empty.<br><br>");
			return order;
		}
		
		//update gateway if needed
		if (cart.get("gateway")!=null){
			order.setProperty("gateway",cart.get("gateway"));
			cart.setProperty("gateway",null);
		}
		//check forcetestmode
		if (cart.get("forcetestmode") !=null){
			order.setProperty("forcetestmode",cart.get("forcetestmode"));
		}

		String applicationid = inPageRequest.findValue("applicationid");
		order.setProperty("applicationid", applicationid);
		
		// Assign default shipping method if one has not been assigned
		if (cart.getShippingMethod() == null) {
			List shippingMethods = cart.getAvailableShippingMethods();
			if (shippingMethods.size() > 0) {
				cart.setShippingMethod((ShippingMethod) shippingMethods.get(0));
			}
		}
		order.copyAdjustments(cart);
		order.setShippingAddress(cart.getShippingAddress());
		order.setBillingAddress(cart.getBillingAddress());
		
//		log.info("#### ADJUSTMENTS 1: "+order.getAdjustments());
		
		// Export order to XML
		store.saveOrder(order);
		//append this to order history, opened
		inPageRequest.putPageValue("orderhistorystate", "opened");
		appendToOrderHistory(inPageRequest);
		
		if (cart.getShippingAddress() != null
				&& cart.getShippingAddress().getId() == null) {
			Searcher addressSearcher = getSearcherManager().getSearcher(
					store.getCatalogId(), "address");
			Address target = cart.getShippingAddress();
			target.setId(addressSearcher.nextId());
			if (inPageRequest.getUserProfile() != null){
				target.setProperty("userprofile", inPageRequest.getUserProfile().getId());
				if(inPageRequest.getUserProfile().get("dealer") != null){
					target.setProperty("dealer", inPageRequest.getUserProfileValue("dealer"));
				}
			}
			addressSearcher.saveData(target, inPageRequest.getUser());

		} else {

			Searcher addressSearcher = getSearcherManager().getSearcher(
					store.getCatalogId(), "address");
			Address target = cart.getShippingAddress();
			if (target != null) {
				if (target.getName() == null) {
					String inPrefix = target.getPrefix();
					String storeName = getAddressValue(inPageRequest, inPrefix,
							"name");
					if (storeName != null) {
						target.setName(storeName);
					}
				}
				target.setProperty("userprofile", inPageRequest
						.getUserProfile().getId());
				addressSearcher.saveData(target, inPageRequest.getUser());
			}
		}

		// process the order with the varous processors
		inPageRequest.putPageValue("order", order);
		store.processOrder(inPageRequest, order);
		store.saveOrder(order);

		if (order.getOrderStatus().isOk()) {
			// Remove items from stock
			for (Iterator iter = cart.getItemIterator(); iter.hasNext();) {
				CartItem cartItem = (CartItem) iter.next();
				InventoryItem inventoryItem = cartItem.getInventoryItem();
				if (inventoryItem != null) {
					inventoryItem.decreaseQuantityInStock(cartItem
							.getQuantity());
				}
			}

			// Save updated product inventory values to disk
			for (Iterator iter = cart.getItemIterator(); iter.hasNext();) {
				CartItem cartItem = (CartItem) iter.next();
				Product product = cartItem.getProduct();
				store.getProductArchive().saveProduct(product);
			}
			// Order succeeded - remove cart
			cart.removeAllItems();
			cart.getAdjustments().clear();
			store.saveOrder(order);

		}
		//export order again after state change
//		log.info("#### ADJUSTMENTS 2: "+order.getAdjustments());
		//append this to order history, opened
		inPageRequest.putPageValue("orderhistorystate", "orderplaced");
		appendToOrderHistory(inPageRequest);
		
		//trigger webevent after the order has been saved
		if (order.getOrderStatus().isOk()){
			WebEvent event = new WebEvent();
			event.setSearchType("order");
			event.setCatalogId(getMediaArchive(inPageRequest).getCatalogId());
			event.setOperation("orders/orderprocessed");
			event.setUser(inPageRequest.getUser());
			event.setProperty("orderid", order.getId());
			getMediaArchive(inPageRequest).getMediaEventHandler().eventFired(event);
		}
		return order;
	}

	public void archiveOrders(WebPageRequest inPageRequest) throws Exception {
		Store store = getStore(inPageRequest);
		OrderArchive orderArchive = store.getOrderArchive();
		orderArchive.archiveOrderData(store);
	}

	public void convertData(WebPageRequest inPageRequest) throws Exception {

		// loop over all the converters and let them convert what they find
		// clear out the reader so its all reloaded
		Store store = getStore(inPageRequest);

		boolean forced = Boolean.parseBoolean(inPageRequest
				.getRequestParameter("forced"));

		ConvertStatus errorlog = store.convertCatalog(inPageRequest.getUser(),
				forced);
		if (inPageRequest != null) {
			inPageRequest.removeSessionValue("store");
			inPageRequest.putPageValue("exception-report", errorlog.getLog());
			inPageRequest.putPageValue("status", errorlog);
		}
	}

	/**
	 * 
	 */
	public void reIndexStore(WebPageRequest inPageRequest) throws Exception {
		Store store = getStore(inPageRequest);

		store.reindexAll();
		store.getCategoryArchive().reloadCategories();
	}

	public void loadCustomerAddressList(WebPageRequest inReq) {
		if (inReq.getUser() == null) {
			return;
		}
		Store store = getStore(inReq);
		if (inReq.getUser() == null || inReq.getUserProfile().getId() == null) {
			return;
		}
		Searcher addressSearcher = store.getSearcherManager().getSearcher(
				store.getCatalogId(), "address");

		HitTracker addresslist;
		String inDealer = inReq.getUserProfile().get("dealer");
		if (inDealer != null) {
			addresslist = addressSearcher.fieldSearch("dealer", inDealer);
		} else {
			addresslist = addressSearcher.fieldSearch("userprofile", inReq.getUserProfile().getId());
		}
		if (addresslist != null && addresslist.size() > 0) {
			inReq.putPageValue("addresslist", addresslist);
		} else {
			Data userAddress = (Data) addressSearcher.searchById(inReq.getUser().getId());
			if (userAddress != null) {
				ArrayList<Data> addr = new ArrayList<Data>();
				addr.add(userAddress);
				inReq.putPageValue("addresslist", addr);					
			} else {
				inReq.putPageValue("addresslist", null);
			}
		}
	}

	public void selectShippingAddress(WebPageRequest inReq) {

		Store store = getStore(inReq);
		Searcher addressSearcher = store.getSearcherManager().getSearcher(
				store.getCatalogId(), "address");
		String addressid = inReq.getRequestParameter("address.value");
		Address address = (Address) addressSearcher.searchById(addressid);
		getCart(inReq).setShippingAddress(address);

	}
	
	
	public void createCartFromOrder(WebPageRequest inReq) throws Exception{
		clearCart(inReq);
		String orderid = inReq.getRequestParameter("orderid");
		Store store = getStore(inReq);
		Order order = (SubmittedOrder)store.getOrderSearcher().searchById(orderid);
		Cart cart = order.getCart();
		inReq.putSessionValue(store.getCatalogId() + "cart", cart);
		cart.setStore(store);
		inReq.putPageValue("cart", cart);		
		
	}
	
	public void saveWishList(WebPageRequest inReq) throws Exception{
		Cart cart = getCart(inReq);
		Store store = getStore(inReq);
		Searcher wishlistsearcher = store.getSearcherManager().getSearcher(store.getCatalogId(), "wishlist");
		Searcher wishitems = store.getSearcherManager().getSearcher(store.getCatalogId(), "wishlistitems");
		UserProfile profile = inReq.getUserProfile();
		String parentid = profile.get("parentprofile");
		User user = inReq.getUser();
		Data wishlist = wishlistsearcher.createNewData();
		wishlist.setProperty("userid", user.getId());
		wishlist.setProperty("creationdate",DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
		wishlist.setProperty("profileid", profile.getId());
		wishlist.setProperty("dealer", profile.get("dealer"));
		wishlist.setProperty("address", profile.get("address"));

		wishlist.setProperty("store", profile.get("store"));
		wishlist.setId(wishlistsearcher.nextId());
		wishlist.setProperty("name", "WISHLIST" + wishlist.getId());
		String[] fields = inReq.getRequestParameters("field");
		wishlistsearcher.updateData(inReq, fields, wishlist);
		wishlist.setProperty("wishstatus", "pending");
		wishlistsearcher.saveData(wishlist, user);
		
		for (Iterator iterator = cart.getItems().iterator(); iterator.hasNext();) {
			CartItem item = (CartItem) iterator.next();
			Data listitem = wishitems.createNewData();
			listitem.setId(wishitems.nextId());
			listitem.setProperty("product", item.getProduct().getId());
			listitem.setProperty("quantity", String.valueOf(item.getQuantity()));
			listitem.setProperty("wishlist", wishlist.getId());
			wishitems.saveData(listitem, inReq.getUser());
		}
		
		
	}
	
	
	public Cart loadWishList(WebPageRequest inReq) throws Exception{
		Store store = getStore(inReq);
		clearCart(inReq);
		Cart cart = getCart(inReq);
		String [] listids = inReq.getRequestParameters("listid");
		Searcher wishlistsearcher = store.getSearcherManager().getSearcher(store.getCatalogId(), "wishlist");
		Searcher wishitems = store.getSearcherManager().getSearcher(store.getCatalogId(), "wishlistitems");
			
		for (int i = 0; i < listids.length; i++) {
			String listid = listids[i];
			cart.setProperty("wishlist", listid);
			HitTracker items = wishitems.fieldSearch("wishlist", listid);
		
			for (Iterator iterator = items.iterator(); iterator.hasNext();) {
				Data item = (Data) iterator.next();
				Product product = store.getProduct(item.get("product"));
				if(product != null){
					CartItem cartitem = new CartItem();
					cartitem.setProduct(product);
					cartitem.setQuantity(Integer.parseInt(item.get("quantity")));
					cart.addItem(cartitem);
				}
			}
		}
		
		return cart;
		
	}
	
	
	public void addItemBySku(WebPageRequest inReq) throws Exception{
		Store store = getStore(inReq);
		clearCart(inReq);
		Cart cart = getCart(inReq);
		String sku = inReq.getRequestParameter("sku");
		
		Product p = getProduct(inReq);
		if(sku != null){
			InventoryItem i = p.getInventoryItemBySku(sku);
			CartItem item = new CartItem();
			item.setInventoryItem(i);
			item.setQuantity(1);
			cart.addItem(item);
		}
		
	}
	
	protected void appendToOrderHistory(WebPageRequest inReq)
	{
		Cart cart = getCart(inReq);
		Order order = cart == null ? null : cart.getCurrentOrder();
		if (order == null)
		{
			log.info("Error! Unable to load order, cannot append order history, skipping");
			return;
		}
		if (inReq.getPageValue("orderhistorystate") == null)
		{
			log.info("Error! Unable to load orderhistorystate from webpagecontext, cannot append order history, skipping");
			return;
		}
		String state = inReq.getPageValue("orderhistorystate").toString();
		MediaArchive archive = (MediaArchive) inReq.getPageValue("mediaarchive");
		if (archive==null)
		{
			log.info("Error! Unable to load mediaarchive from webpagecontext, cannot append order history, skipping");
			return;
		}
		WebEvent evt = new WebEvent();
		evt.setSearchType("detailedorderhistory");//productupdates
		evt.setCatalogId(archive.getCatalogId());
		evt.setProperty("applicationid", inReq.findValue("applicationid"));
		evt.setOperation("orderhistory/appendorderhistory");//products/
		evt.setProperty("orderid", order.getId());
		evt.setProperty("type","automatic");
		evt.setProperty("state",state);
		archive.getMediaEventHandler().eventFired(evt);
	}
	
	public void changeMode(WebPageRequest inReq) throws Exception{
		Cart cart = getCart(inReq);
		if (!"administrator".equals(inReq.getUserProfileValue("settingsgroup"))){
			//if not administrator, ignore command but also make sure forcetestmode is false
			if (cart.getBoolean("forcetestmode")){
				cart.setProperty("forcetestmode", "false");
			}
			log.info("ignoring request to change mode of order, user is not an administrator");
			return;
		}
		String mode = inReq.getRequestParameter("mode");
		if (mode!=null){
			cart.setProperty("forcetestmode", Boolean.parseBoolean(mode) ? "true" : "false");
			log.info("changed forcetestmode to "+Boolean.parseBoolean(mode));
		} else {
			log.info("did not modify forcetestmode, mode is null");
		}
	}
	
	public void changeGateway(WebPageRequest inReq) throws Exception{
		Cart cart = getCart(inReq);
		if (!"administrator".equals(inReq.getUserProfileValue("settingsgroup"))){
			//if not administrator, ignore command but also make sure forcetestmode is false
			log.info("ignoring request to change gateway of order, user is not an administrator");
			return;
		}
		String gateway = inReq.getRequestParameter("gateway");
		if (gateway!=null){
			cart.setProperty("gateway", gateway);
			log.info("changed gateway of cart to "+gateway);
		} else {
			log.info("did not modify gateway, request parameter is null");
		}
	}

}