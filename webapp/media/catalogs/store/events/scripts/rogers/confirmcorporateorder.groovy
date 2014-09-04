import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.store.Cart
import org.openedit.store.CartItem
import org.openedit.store.Product
import org.openedit.store.PurchaseOrderMethod
import org.openedit.store.Store
import org.openedit.store.customer.Address
import org.openedit.store.customer.Customer
import org.openedit.store.orders.Order
import org.openedit.store.orders.OrderSet
import org.openedit.store.orders.OrderState
import org.openedit.util.DateStorageUtil

import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.users.BaseUser
import com.openedit.users.User


public void init(){
	log.info("Confirm Corporate Orders");
	
	WebPageRequest req = context;
	MediaArchive archive = req.getPageValue("mediaarchive");
	Store store = req.getPageValue("store");
	
	String uuid = req.getRequestParameter("uuid");
	if (uuid == null || uuid.trim().isEmpty()){
		log.info("UUID not defined, aborting");
		return;
	}
	Searcher searcher = archive.getSearcher("corporateorder");
	Searcher productsearcher = archive.getSearcher("product");
	Searcher storesearcher = archive.getSearcher("rogersstore");
	Searcher ordersearcher = archive.getSearcher("storeOrder");
	Searcher usersearcher = archive.getSearcherManager().getSearcher("system", "user");
	
	OrderSet orderSet = new OrderSet();
	SearchQuery query = searcher.createSearchQuery();
	query.addMatches("uuid",uuid);
	query.addMatches("status","ok");
	query.addSortBy("idUp");
	HitTracker hits = searcher.search(query);
	hits.each{
		Data data = searcher.searchById(it.id);
		int quantity = toInt(data.get("quantity"),0);
		if (quantity <= 0){//fail-safe
			return;
		}
		String storeNum = data.get("store");
		Order order = orderSet.getOrderByCustomerId("rogers-"+storeNum);
		if (order == null) {
			Data targetstore = storesearcher.searchById(storeNum);
			
			order = new Order();
			order.setId("Rogers-"+ordersearcher.nextId());
			order.setSourcePath(order.getId());
			order.setProperty("orderdate", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
			order.setProperty("batchid", uuid);
			order.setCart(new Cart(store));
			
			OrderState orderState = new OrderState();
			orderState.setId("pending");
			orderState.setDescription("pending");
			orderState.setOk(true);
			order.setOrderState(orderState);
			
			Address shipping = new Address();
			shipping.setId(targetstore.get("id"));
			shipping.setName(targetstore.get("name"));
			shipping.setAddress1(targetstore.get("address1"));
			shipping.setAddress2("");
			shipping.setCity(targetstore.get("city"));
			shipping.setState(targetstore.get("province"));
			shipping.setZipCode(targetstore.get("zip"));
			shipping.setCountry(targetstore.get("country"));
			shipping.setDescription(targetstore.get("description"));
			shipping.setProperty("phone", targetstore.get("phone1"));
			order.setShippingAddress(shipping);
			
			Address billing = new Address();
			billing.setId(targetstore.get("id"));
			billing.setName(targetstore.get("name"));
			billing.setAddress1(targetstore.get("address1"));
			billing.setAddress2("");
			billing.setCity(targetstore.get("city"));
			billing.setState(targetstore.get("province"));
			billing.setZipCode(targetstore.get("zip"));
			billing.setCountry(targetstore.get("country"));
			billing.setDescription(targetstore.get("description"));
			billing.setProperty("phone", targetstore.get("phone1"));
			order.setBillingAddress(billing);
			
			User user = usersearcher.searchById("rogers-"+storeNum);
			if (user == null) {
				user = new BaseUser();
				user.setId("rogers-"+storeNum);
				user.setPassword("rogers");
				user.setFirstName(targetstore.getName());
				usersearcher.saveData(user,null);
			}
			Customer customer = new Customer();
			customer.setId(targetstore.get("id"));
			customer.setName(targetstore.get("name"));
			customer.setPhone1(targetstore.get("phone"));
			customer.setCompany("Rogers");
			customer.setShippingAddress(shipping);
			customer.setBillingAddress(billing);
			customer.setUser(user);
			order.setCustomer(customer);
			
			PurchaseOrderMethod poMethod = new PurchaseOrderMethod();
			poMethod.setPoNumber(order.getId());
			poMethod.setBillMeLater(true);
			order.setPaymentMethod(poMethod);
			
			orderSet.addOrder(order);
		}
		String as400id = data.get("product");
		//search for rogersas400id or fidoas400id
		Data targetProduct = productsearcher.searchByField("rogersas400id", as400id);
		if (targetProduct == null){
			targetProduct = productsearcher.searchByField("fidoas400id", as400id);
		}
		if (targetProduct){
			Product product = productsearcher.searchById(targetProduct.getId());
			CartItem orderitem = new CartItem();
			
			orderitem.setProduct(product);
			orderitem.setQuantity(quantity);
			
			order.getCart().addItem(orderitem);
			order.addItem(orderitem);
		}
	}
	orderSet.recalculateAll(store);
	req.putSessionValue("orderset", orderSet);
	
	//delete all: should move this to processcorporateorder.groovy
//	query = searcher.createSearchQuery();
//	query.addMatches("uuid",uuid);
//	hits = searcher.search(query);
//	hits.each{
//		Data data = searcher.searchById(it.id);
//		searcher.delete(data, null);
//	}
}

public int toInt(String inVal, int inDefault){
	int out = inDefault;
	try{
		out = Integer.parseInt(inVal);
	}catch(Exception e){}
	return out;
}

init();