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
import org.openedit.store.InventoryItem;
import org.openedit.store.PaymentMethod
import org.openedit.store.Product
import org.openedit.store.PurchaseOrderMethod
import org.openedit.store.ShippingMethod;
import org.openedit.store.Store
import org.openedit.store.customer.Address
import org.openedit.store.customer.Customer
import org.openedit.store.modules.ProcessOrderModule;
import org.openedit.store.orders.Order
import org.openedit.store.orders.OrderSet;
import org.openedit.store.orders.OrderState
import org.openedit.util.DateStorageUtil;

import com.openedit.OpenEditException
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.page.Page
import com.openedit.users.BaseUser
import com.openedit.users.User
import com.openedit.util.FileUtils
import com.sun.org.apache.bcel.internal.generic.GETSTATIC;

public class ImportRogersOrder extends EnterMediaObject {

	private int columnStore = 4;
	private String colHeadSTORE = "ISTORE";
	private int columnStoreRank = 5;
	private String colHeadRANK = "RANK"
	private int columnStoreName = 6;
	private String colHeadSTORENAME = "STORE"
	private int columnManfactID = 7;
	private String colHeadMANUFACTID = "IMFGR"
	private int columnManfactName = 8;
	private String colHeadMANUFACTNAME = "MFC"
	private int columnAS400id = 9;
	private String colHeadAS400ID = "INUMBR";
	private int columnAvailable = 19;
	private String colHeadAVAILABLE = "AVAILABLE";
	private int columnSuggest = 20;
	private String colHeadSUGGEST = "Sugg";
	private int columnQuantity = 21;
	private String colHeadQUANTITY = "To Order";
	private int columnRogersID = 33;
	private String colHeadROGERSID = "IVNDP#";
	private int columnRogersOtherID = 34;
	private String colHeadROGERSOTHERID = "IRLSDT";
	int totalRows;
	Map<String, Order> orderMap;
	
	Map<String, Integer> badProductMap;
	List<String> goodOrderList;
	List<String> badProductList;
	List<String> badStoreList;
	List<String> badColumnList;
	boolean processOrder;
	
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
		if(badProductMap == null) {
			badProductMap = new HashMap<String, Integer>()
		}
		List<String> badList = new ArrayList<String>();
		if (badProductMap.size() > 0) {
			Iterator it = badProductMap.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry entry = (Map.Entry) it.next();
				String key = entry.getKey();
				badList.add(key);
			}
		}
		return badList;
	}
	public void addToBadProductList(String inItem) {
		if(badProductMap == null) {
			badProductMap = new HashMap<String, Integer>()
		}
		if (badProductMap.size() == 0) {
			badProductMap.put(inItem, 1);
		} else {
			Integer value = 1;
			Iterator it = badProductMap.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry entry = (Map.Entry) it.next();
				String key = entry.getKey();
				if (key.equalsIgnoreCase(inItem)) {
					value = entry.getValue();
					value++;
				}	
			}
			badProductMap.put(inItem, value);
		}
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
	
	public void addToBadColumnList(String inItem) {
		if (badColumnList == null) {
			badColumnList = new ArrayList<String>();
		}
		badColumnList.add(inItem);
	}
	public List<String> getBadColumnList() {
		if (badColumnList == null) {
			badColumnList = new ArrayList<String>();
		}
		return badColumnList;
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
	public void setProcessOrder() {
		processOrder = true;
	}
	public boolean getProcessOrder() {
		if (processOrder == null) {
			processOrder = false;
		}
		return processOrder;
	}
	public List<Order> getOrders() {
		List<Order> orders = new ArrayList<Order>();
		if (orderMap == null) {
			orderMap = new HashMap<String, Order>()
		}
		if (orderMap.size() > 0) {
			Iterator iter = orderMap.entrySet().iterator();
			while(iter.hasNext()) {
				Map.Entry entry = (Map.Entry) iter.next();
				Order order = (Order) entry.getValue();
				orders.add(order);
			}
		}
		return orders;
	}

	public void orderImport(){
		//Create Store, MediaArchive Object

		log.info("PROCESS: START Orders.RogersImport");
		
		MediaArchive archive = context.getPageValue("mediaarchive");
		String catalogid = getMediaArchive().getCatalogId();
		boolean production = Boolean.parseBoolean(context.findValue('productionmode'));
		
		String choice = context.getRequestParameter("choice");
		if (choice != null) {
			if (choice.equals("exit")) {
				context.redirect("/ecommerce/rogers/orders/rogersorder.html");
				return;
			} else {
				setProcessOrder(); 
			}
		}
		
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
		//PropertyDetail detail = itemsearcher.getDetail("quantity");
		//detail.get("column");
		
		Searcher as400searcher = manager.getSearcher(archive.getCatalogId(), "as400");
		Data as400Record = as400searcher.createNewData();
		as400Record.setId(as400searcher.nextId());
		as400Record.setName("Batch " + as400Record.getId());
		String batchID = UUID.randomUUID().toString();
		as400Record.setProperty("batchid", batchID);
		as400Record.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
		as400Record.setProperty("exportstatus", "open");
		as400Record.setProperty("user", context.getUser().getName());
		as400searcher.saveData(as400Record, context.getUser());
		as400searcher.reIndexAll();

		Page upload = archive.getPageManager().getPage("/${catalogid}/temp/upload/rogers_order.csv");
		Reader reader = upload.getReader();
		try
		{
			def SEARCH_FIELD = "rogerssku";
			boolean done = false;

			//Create CSV reader
			CSVReader read = new CSVReader(reader, ',', '\"');
			
			//Read 1 line of header
			String[] headers = read.readNext();
			boolean errorFields = checkColumns(headers);
			
			if (errorFields == true) {
				
				//String errorOut = "<p>The following fields in the input file are invalid:<ul>" + errorOut + "</ul></p>";
				context.putPageValue("errorfields", getBadColumnList());
				context.putPageValue("errorcontext", "fields");

			} else {

				int productCount = 0;

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
		
				Reader checkProducts = upload.getReader();
				String[] orderLine;
				while ((orderLine = read.readNext()) != null)
				{
					def storeNum = orderLine[columnStore].trim();

					if (orderMap == null) {
						orderMap = new HashMap<String, Order>()
					}

					Order order = orderMap.get(storeNum);
					if (order == null) {
						order = new Order();
						order.setId("Rogers-"+ordersearcher.nextId());
						order.setSourcePath(order.getId());
						order.setProperty("batchid", batchID);
						orderMap.put(storeNum, order);
					}
					
					//Create as400list Searcher
					Searcher storeList = manager.getSearcher(archive.getCatalogId(), "rogersstore");
					Data targetStore = storeList.searchById(storeNum);
					if(targetStore == null){
						addToBadStoreList(storeNum);
						break;
					}

					//Get Product Info
					//Read the oraclesku from the as400 table
					String rogerssku = orderLine[columnRogersID].trim();

					//Search the product for the oracle sku(rogerssku)
					Data targetProduct = productsearcher.searchByField(SEARCH_FIELD, rogerssku);
					if (targetProduct != null) {
						
						Product product = productsearcher.searchById(targetProduct.getId());
						if (product != null) {

							//productsearcher.saveData(real, context.getUser());
							log.info("ProductID Found: " + product.getId());
	
							int qty = Integer.parseInt(orderLine[columnQuantity]);
							
							//Everything is good... Create the cart item!
							CartItem orderitem = new CartItem();
							
							orderitem.setProduct(product);
							orderitem.setQuantity(Integer.parseInt(orderLine[columnQuantity]));
							
							String as400id = product.get("as400id");
							if (as400id == null) {
								product.setProperty("as400id", orderLine[columnAS400id]);
								productsearcher.saveData(product, context.getUser());
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
								Data billingAddress = addresssearcher.searchByField("storenumber", storeNum);
								billing = new Address();
								billing.setId(billingAddress.get("id"));
								billing.setName(billingAddress.get("name"));
								billing.setAddress1(billingAddress.get("address1"));
								billing.setAddress2("");
								billing.setCity(billingAddress.get("city"));
								billing.setState(billingAddress.get("state"));
								billing.setZipCode(billingAddress.get("zip"));
								billing.setCountry(billingAddress.get("country"));
								billing.setDescription(billingAddress.get("description"));
								billing.setProperty("phone", billingAddress.get("phone1"));
								order.setBillingAddress(billing);
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
							
							//Lets Save the ORDER!
							log.info("Order (" + order.getId() + ") saved to OrderMap");
						} else {
							order.addMissingItem(rogerssku);
						}
					} else {
						order.addMissingItem(rogerssku);
					}
				}
				OrderSet orderSet = new OrderSet();
				for (Iterator iterator = orderMap.keySet().iterator(); iterator.hasNext();) {
					String key = (String) iterator.next();
					Order order = orderMap.get(key);
					
					if (order.getNumItems() > 0) {
					
						orderSet.recalculateOrder(order, store);
						
						OrderState orderState = new OrderState();
						orderState.setId("pending");
						orderState.setDescription("pending");
						orderState.setOk(true);
						order.setOrderState(orderState);
						
						//Set other properties
						Date now = new Date();
						String newDate = DateStorageUtil.getStorageUtil().formatForStorage(now);
						order.setProperty("orderdate", newDate);
						
						order.setProperty("orderstatus", "pending");
						order.setProperty("order_status", "pending");
						order.setProperty("id", order.getId());
						order.setProperty("customer", order.getCustomer().getId());
						order.setProperty("batchid", batchID);
				
	//					store.saveOrder(order);
	//					store.getOrderSearcher().updateIndex(order);
						orderSet.recalculateOrder(order, store);
						orderSet.addOrder(order);
					} else {
						log.info("Order (" + order.getId() + ") skipped. No Cart Items");
					}
				}
				context.putSessionValue("orderset", orderSet);
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
	
	private boolean checkColumns( String[] headers) {
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
				addToBadColumnList(headers[columnNumbers.get(index)].toString());
			}
		}
		return (getBadColumnList().size() > 0);
	}
	
	private boolean checkOrder( Reader csvReader, ArrayList columnNumbers, 
		MediaArchive archive, String catalogId, Searcher productsearcher ) {
		boolean result = false;

		int columnQuantity = 21;
		int columnRogersID = 33;
		String SEARCH_FIELD = "rogerssku";
		
		CSVReader readLine = new CSVReader(csvReader, ',', '\"');
		//Read Header Line
		readLine.readNext();
		String[] orderLine;
		while ((orderLine = readLine.readNext()) != null) {
			def storeNum = orderLine[columnStore].trim();
			String rogerssku = orderLine[columnRogersID].trim();
			//Search the product for the oracle sku(rogerssku)
			Data targetProduct = productsearcher.searchByField(SEARCH_FIELD, rogerssku);
			if (targetProduct != null) {
				Product product = productsearcher.searchById(targetProduct.getId());
				if (product == null) {
					addToBadProductList(rogerssku);				
				} else {
					int qty = Integer.parseInt(orderLine[columnQuantity]);
					if ( qty < 1 ) {
						log.info("ERROR: Invalid quantity value");
						log.info(" - Quantity: " + orderLine[columnQuantity]);
						log.info(" - Store: " + storeNum);
						log.info(" - ProductID: " + targetProduct.getId());
						addToBadProductList(rogerssku);
					}
				}
			} else {
				addToBadProductList(rogerssku);
			}
		}
		if (getBadProductList().size() == 0) {
			result = true;
		}
		return result;
	}
	private boolean checkInventory( Reader csvReader, ArrayList columnNumbers, 
		MediaArchive archive, String catalogId, Searcher productsearcher, boolean skipBadProducts ) {
		boolean result = false;

		int columnQuantity = 21;
		int columnRogersID = 33;
		String SEARCH_FIELD = "rogerssku";
		
		CSVReader readLine = new CSVReader(csvReader, ',', '\"');
		//Read Header Line
		readLine.readNext();
		String[] orderLine;
		while ((orderLine = readLine.readNext()) != null) {
			String rogerssku = orderLine[columnRogersID].trim();
			//Search the product for the oracle sku(rogerssku)
			Data targetProduct = productsearcher.searchByField(SEARCH_FIELD, rogerssku);
			if (targetProduct != null) {
				Product product = productsearcher.searchById(targetProduct.getId());
				if (product != null) {
					int qty = Integer.parseInt(orderLine[columnQuantity]);
					InventoryItem item = product.getInventoryItem(0);
					int qtyInStock = item.getQuantityInStock();
					if (qty > qtyInStock) {
						addToBadProductList(rogerssku);
					}
				}	
			}
		}
		if (getBadProductList().size() == 0) {
			result = true;
		}
		return result;
	}
}

log = new ScriptLogger();
log.startCapture();

try {

	ImportRogersOrder importOrder = new ImportRogersOrder();
	importOrder.setLog(log);
	importOrder.setContext(context);
	importOrder.setModuleManager(moduleManager);
	importOrder.setPageManager(pageManager);

	//Read the production value
	boolean production = Boolean.parseBoolean(context.findValue('productionmode'));

	log.info("PROCESS: START Orders.RogersImport");
	importOrder.orderImport();
	log.info("PROCESS: END Orders.RogersImport");
}
finally {
	log.stopCapture();
}
