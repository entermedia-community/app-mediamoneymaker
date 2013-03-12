package rogers

/*
 * Created on October 4th, 2012
 * Created by Peter Floyd
 */

//Import List
import java.util.ArrayList;
import java.util.List;

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.util.CSVReader
import org.openedit.store.Cart
import org.openedit.store.CartItem
import org.openedit.store.PaymentMethod
import org.openedit.store.Product
import org.openedit.store.PurchaseOrderMethod
import org.openedit.store.Store
import org.openedit.store.customer.Address
import org.openedit.store.customer.Customer
import org.openedit.store.orders.Order
import org.openedit.store.orders.OrderState

import com.openedit.OpenEditException
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.page.Page
import com.openedit.users.BaseUser
import com.openedit.users.User
import com.openedit.util.FileUtils

public class ImportRogersOrder extends EnterMediaObject {

	List<String> goodOrderList;
	List<String> badProductList;
	List<String> badStoreList;
	int totalRows;
	Map<String, Order> orderMap;

	public List<String> getGoodOrderList() {
		if(goodOrderList == null) {
			goodOrderList = new ArrayList<String>();
		}
		return goodOrderList;
	}
	public void addToGoodOrderList(String inItem) {
		if(goodOrderList == null) {
			goodOrderList = new ArrayList<String>();
		}
		goodOrderList.add(inItem);
	}

	public List<String> getBadProductList() {
		if(badProductList == null) {
			badProductList = new ArrayList<String>();
		}
		return badProductList;
	}
	public void addToBadProductList(String inItem) {
		if(badProductList == null) {
			badProductList = new ArrayList<String>();
		}
		badProductList.add(inItem);
	}

	public List<String> getBadStoreList() {
		if(badStoreList == null) {
			badStoreList = new ArrayList<String>();
		}
		return badStoreList;
	}
	public void addToBadStoreList(String inItem) {
		if(badStoreList == null) {
			badStoreList = new ArrayList<String>();
		}
		badStoreList.add(inItem);
	}

	public int getTotalRows() {
		if (totalRows == null) {
			totalRows = 0;
		}
		return totalRows;
	}
	public void increaseTotalRows() {
		if (totalRows == null) {
			totalRows = 0;
		}
		this.totalRows++;
	}

	public void orderImport(){
		//Create Store, MediaArchive Object

		log.info("PROCESS: START Orders.RogersImport");
		
		MediaArchive archive = context.getPageValue("mediaarchive");
		String catalogid = getMediaArchive().getCatalogId();
		boolean production = Boolean.parseBoolean(context.findValue('productionmode'));
		
		String strMsg = "";
		Store store = null;
		try {
			store  = context.getPageValue("store");
			if (store != null) {
				log.info("Store loaded");
			} else {
				strMsg = "ERROR: Could not load store";
				throw new Exception(strMsg);
			}
		}
		catch (Exception e) {
			strMsg += "Exception thrown:\n";
			strMsg += "Local Message: " + e.getLocalizedMessage() + "\n";
			strMsg += "Stack Trace: " + e.getStackTrace().toString();;
			log.info(strMsg);
			throw new OpenEditException(strMsg);
		}
		
		// Create the Searcher Objects to read values!
		SearcherManager manager = archive.getSearcherManager();

		//Create Searcher Object
		Searcher productsearcher = manager.getSearcher(archive.getCatalogId(), "product");
		///Searcher itemsearcher = manager.getSearcher(archive.getCatalogId(), "rogers_order_item");
		Searcher storesearcher = manager.getSearcher(archive.getCatalogId(), "store");
		Searcher distributorsearcher = manager.getSearcher(archive.getCatalogId(), "distributor");
		Searcher addresssearcher = manager.getSearcher(archive.getCatalogId(), "address");
		Searcher ordersearcher = manager.getSearcher(archive.getCatalogId(), "storeOrder");
		Searcher usersearcher = manager.getSearcher("system", "user");

		//Define columns from spreadsheet
		def int columnStore = 4;
		def String colHeadSTORE = "ISTORE";
		def int columnStoreRank = 5;
		def String colHeadRANK = "RANK"
		def int columnStoreName = 6;
		def String colHeadSTORENAME = "STORE"
		def int columnManfactID = 7;
		def String colHeadMANUFACTID = "IMFGR"
		def int columnManfactName = 8;
		def String colHeadMANUFACTNAME = "MFC"
		def int columnAS400id = 9;
		def String colHeadAS400ID = "INUMBR";
		def int columnAvailable = 19;
		def String colHeadAVAILABLE = "AVAILABLE";
		def int columnSuggest = 20;
		def String colHeadSUGGEST = "Sugg";
		def int columnQuantity = 21;
		def String colHeadQUANTITY = "To Order";
		def int columnRogersID = 33;
		def String colHeadROGERSID = "IVNDP#";
		def int columnRogersOtherID = 34;
		def String colHeadROGERSOTHERID = "IRLSDT";

		//PropertyDetail detail = itemsearcher.getDetail("quantity");
		//detail.get("column");
		
		String batchID = UUID.randomUUID().toString();

		Page upload = archive.getPageManager().getPage("/${catalogid}/temp/upload/rogers_order.csv");
		Reader reader = upload.getReader();
		try
		{
			def SEARCH_FIELD = "rogerssku";
			boolean done = false;

			//Create CSV reader
			CSVReader read = new CSVReader(reader, ',', '\"');
			
			orderMap = new HashMap()

			//Read 1 line of header
			String[] headers = read.readNext();
			boolean errorFields = false;
			String errorOut = "";

			def List columnNumbers = new ArrayList();
			columnNumbers.add(columnStore);
			columnNumbers.add(columnStoreRank);
			columnNumbers.add(columnStoreName);
			columnNumbers.add(columnManfactID);
			columnNumbers.add(columnManfactName);
			columnNumbers.add(columnAS400id);
			columnNumbers.add(columnAvailable);
			columnNumbers.add(columnSuggest);
			columnNumbers.add(columnQuantity);
			columnNumbers.add(columnRogersID);
			columnNumbers.add(columnRogersOtherID);

			def List columnNames = new ArrayList();
			columnNames.add(colHeadSTORE);
			columnNames.add(colHeadRANK);
			columnNames.add(colHeadSTORENAME);
			columnNames.add(colHeadMANUFACTID);
			columnNames.add(colHeadMANUFACTNAME);
			columnNames.add(colHeadAS400ID);
			columnNames.add(colHeadAVAILABLE);
			columnNames.add(colHeadSUGGEST);
			columnNames.add(colHeadQUANTITY);
			columnNames.add(colHeadROGERSID);
			columnNames.add(colHeadROGERSOTHERID);

			for ( int index=0; index < columnNumbers.size(); index++ ) {
				if ( headers[columnNumbers.get(index)].toString().toUpperCase() != columnNames.get(index).toString().toUpperCase()) {
					errorOut += "<li>" + addQuotes(headers[columnNumbers.get(index)].toString()) + " at column " + columnNumbers.get(index).toString() + " is invalid.</li>";
					errorFields = true;
				}
			}
			if (errorFields == true) {

				errorOut = "<p>The following fields in the input file are invalid:<ul>" + errorOut + "</ul></p>";
				context.putPageValue("errorout", errorOut);

			} else {

				int productCount = 0;
				int badProductCount = 0;
				List badStoreList = new ArrayList();

				String[] orderLine;
				while ((orderLine = read.readNext()) != null)
				{
					def storeNum = orderLine[columnStore].trim();

					//Create as400list Searcher
					Searcher storeList = manager.getSearcher(archive.getCatalogId(), "rogersstore");
					Data targetStore = storeList.searchById(storeNum);
					if(targetStore == null){
						addToBadStoreList(storeNum);
						break;
					}

					//Get Product Info
					//Read the oraclesku from the as400 table
					String rogerssku = orderLine[columnRogersID];

					//Search the product for the oracle sku(rogerssku)
					Data targetProduct = productsearcher.searchByField(SEARCH_FIELD, rogerssku);
					if (targetProduct != null) {

						//productsearcher.saveData(real, context.getUser());
						log.info("ProductID Found: " + targetProduct.getId());

						int qty = Integer.parseInt(orderLine[columnQuantity]);
						if ( qty < 1 ) {
							log.info("ERROR: Invalid quantity value");
							log.info(" - Quantity: " + orderLine[columnQuantity]);
							log.info(" - Store: " + storeNum);
							log.info(" - ProductID: " + targetProduct.getId());
							break;
						}
						
						//Everything is good... Create the cart item!
						CartItem orderitem = new CartItem();
						Product product = productsearcher.searchById(targetProduct.getId());
						orderitem.setProduct(product);
						orderitem.setQuantity(Integer.parseInt(orderLine[columnQuantity]));
						
						String as400id = product.get("as400id");
						if (as400id == null) {
							product.setProperty("as400id", orderLine[columnAS400id]);
							productsearcher.saveData(product, context.getUser());
						}
						
						if (orderMap == null) {
							orderMap = new HashMap()
						}

						Order order = orderMap.get(storeNum);
						if (order == null) {
							order = new Order();
							order.setId("Rogers-"+ordersearcher.nextId());
							order.setSourcePath(order.getId());
							order.setProperty("batchid", batchID);
							orderMap.put(storeNum, order);
						}
						order.addItem(orderitem);
						
						Address shipping = order.getShippingAddress();
						if (shipping == null) {
							Data shippingAddress = addresssearcher.searchByField("storenumber", storeNum);
							shipping = new Address();
							shipping.setId(shippingAddress.get("id"));
							shipping.setName(shippingAddress.get("name"));
							shipping.setAddress1(shippingAddress.get("address1"));
							shipping.setAddress2("");
							shipping.setCity(shippingAddress.get("city"));
							shipping.setState(shippingAddress.get("state"));
							shipping.setZipCode(shippingAddress.get("zip"));
							shipping.setCountry(shippingAddress.get("country"));
							shipping.setDescription(shippingAddress.get("description"));
							shipping.setProperty("phone", shippingAddress.get("phone1"));
							order.setShippingAddress(shipping);
						}
						//store.saveOrder(order);
						
						Address billing = order.getBillingAddress();
						if (billing == null) {
							order.setBillingAddress(shipping);
							billing=shipping;
						}

						Customer customer = order.getCustomer();
						if (customer == null) {
							//ADD STORE AS USER
							
							User user = usersearcher.searchById("rogers-"+storeNum);
							if (user == null) {
								user = new BaseUser();
								user.setId("rogers-"+storeNum);
								user.setPassword("rogers");
								user.setFirstName(orderLine[columnStoreName]);
								usersearcher.saveData(user, context.getUser());
							}
							
							Searcher profileAddressSearcher = manager.getSearcher(archive.getCatalogId(), "address");
							Data customerInfo = profileAddressSearcher.searchByField("storenumber", storeNum);
							if (customerInfo == null) {
								throw new OpenEditException("Cannot find Store Information");
							}
							customer = new Customer()
							customer.setId(customerInfo.get("id"));
							customer.setName(customerInfo.get("name"));
							customer.setEmail(customerInfo.get("email"));
							customer.setPhone1(customerInfo.get("phone"));
							customer.setCompany("Rogers");
							customer.setShippingAddress(shipping);
							customer.setBillingAddress(billing);
							customer.setUser(user);
							order.setCustomer(customer);
						}
						
						String purchaseorder = order.getId();
						PurchaseOrderMethod poMethod = new PurchaseOrderMethod();
						poMethod.setPoNumber(purchaseorder);
						poMethod.setBillMeLater(true);
						PaymentMethod payment = poMethod;
						order.setPaymentMethod((PurchaseOrderMethod) payment);
						if (order.getPaymentMethod() == null) {
							throw new OpenEditException("Cannot set PaymentMethod()");
						}
						
						//////////////////////////////////////////
						/// THIS IS WRONG
						/// CHANGE SHIPPING METHOD
						//////////////////////////////////////////
						
						OrderState orderState = new OrderState();
						orderState.setId("authorized");
						orderState.setDescription("authorized");
						orderState.setOk(true);
										//Set other properties
						order.setOrderState(orderState);
						order.setProperty("orderstatus", "authorized");
						order.setProperty("id", order.getId());
						order.setProperty("customer", customer.id);
						
						//store.saveOrder(order);
						
					} else {

						//ID Does not exist!!! Add to badProductIDList
						badProductCount++;
						String errMsg = "BAD Product ID: " + rogerssku;
						addToBadProductList(errMsg);
						log.info(errMsg);

					}
				}
				for (Iterator iterator = orderMap.keySet().iterator(); iterator.hasNext();) {
/*
		ITEMS THAT NEED TO BE DONE! 
 		order.setShippingMethod(inCart.getShippingMethod());
		order.setAdjustments(inCart.getAdjustments());
					
 */
					String key = (String) iterator.next();
					Order order = orderMap.get(key);
					Cart cart = order.getCart();
					cart.setStore(store);
					cart.setItems(order.getItems());

					Customer customer = order.getCustomer()
					cart.setCustomer(customer);
					
					List taxrates = cart.getStore().getTaxRatesFor(
							customer.getShippingAddress().getState());
					if (customer.getShippingAddress().getState() == null) {
						taxrates = cart.getStore().getTaxRatesFor(
								customer.getBillingAddress().getState());
					}
					customer.setTaxRates(taxrates);
					
					order.setTaxes(cart.getTaxes());
					order.setSubTotal(cart.getSubTotal());
					
					order.setTotalShipping(cart.getTotalShipping());
					order.setTotalPrice(cart.getTotalPrice());
					
					order.setTotalTax(cart.getTotalTax());
			
					store.saveOrder(order);
					store.getOrderSearcher().updateIndex(order);
					addToGoodOrderList(order.getId());
				}
			}
		}
		finally
		{
			FileUtils.safeClose(reader);
		}
	}

	public String addQuotes( String s ) {
		return "\"" + s + "\"";
	}
}

logs = new ScriptLogger();
logs.startCapture();

try {

	ImportRogersOrder importOrder = new ImportRogersOrder();
	importOrder.setLog(logs);
	importOrder.setContext(context);
	importOrder.setModuleManager(moduleManager);
	importOrder.setPageManager(pageManager);

	//Read the production value
	boolean production = Boolean.parseBoolean(context.findValue('productionmode'));

	importOrder.orderImport();
	log.info("The following file(s) has been created. ");
	context.putPageValue("orderlist", importOrder.getGoodOrderList());
	log.info(importOrder.getGoodOrderList().toString());
	if (importOrder.getBadProductList().size()>0) {
		log.info("Bad Product List ");
		log.info(importOrder.getBadProductList().toString());
		context.putPageValue("badproductlist", importOrder.getBadProductList());
	}
	if (importOrder.getBadStoreList().size()>0) {
		log.info("Bad Store List ");
		log.info(importOrder.getBadStoreList().toString());
		context.putPageValue("badproductlist", importOrder.getBadStoreList());
	}
	log.info("PROCESS: END Orders.RogersImport");
}
finally {
	logs.stopCapture();
}
