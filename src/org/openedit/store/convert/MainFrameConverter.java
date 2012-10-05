package org.openedit.store.convert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.store.CartItem;
import org.openedit.store.CatalogConverter;
import org.openedit.store.Category;
import org.openedit.store.InventoryItem;
import org.openedit.store.Product;
import org.openedit.store.Store;
import org.openedit.store.customer.Customer;
import org.openedit.store.orders.OrderId;
import org.openedit.store.orders.OrderState;
import org.openedit.store.orders.SubmittedOrder;

import com.openedit.util.FileUtils;

public class MainFrameConverter extends CatalogConverter
{
	private static final Log log = LogFactory.getLog(MainFrameConverter.class);
	
	public void convert(Store inStore, ConvertStatus inErrorLog) throws Exception
	{
		Map products = new Hashtable();
		
		convertProducts(inStore, products, inErrorLog );
		convertInventory(inStore,products, inErrorLog);
		saveOutput(inStore,  new ArrayList(products.values()));
		convertOrderHistory(inStore, inErrorLog);
		convertOrders(inStore,inErrorLog);
		inStore.clearProducts();
		File inProducts = new File( inStore.getStoreDirectory() , "upload/a-PRODUCTS.txt");
		if( inProducts.exists())
		{
			inErrorLog.setReindex(true);
		}
	}
	
	public void convertProducts(Store inStore,Map products, ConvertStatus inErrorLog) throws Exception
	{
		//read in Inventory file
		File inProducts = new File( inStore.getStoreDirectory() , "upload/a-PRODUCTS.txt");
		File inSpecs = new File( inStore.getStoreDirectory() , "upload/b-SPEC.txt");
		//List productIds = new ArrayList();
		if( inProducts.exists() && inSpecs.exists() )
		{
			Reader productReader = new FileReader(inProducts);
			try
			{
				BufferedReader in = new BufferedReader(productReader);
				String line = null;
				int rowNum = 0;
				Product product = null;
				
				while( (line = in.readLine() ) != null)
				{
					rowNum++;
					if( line.length() == 0)
					{
						break;
					}
					String[] parts = line.split("\\|");
					String[] partNums = parts[0].split("@");
					String id = partNums[0].trim();
					product = createProduct(inStore,id);
					product.setName(id);
					product.setAvailable(true);					
					String customerPartNum="";
					if ( partNums.length > 1)
					{
						//second
						customerPartNum=partNums[1].trim();
					}
					
					product.setProperty("customerPartNum", customerPartNum);					
					product.setShortDescription(parts[3].trim()); //This is important. Used in the results 
					
					String family = parts[1].trim();
					String catid = extractId(family, true);
					Category familycat = inStore.getCategory(catid);
					if( familycat == null)
					{
						familycat = inStore.getCategoryArchive().addChild(new Category(catid,family));
					}
				
					Category subcategory = null;
						
					String subFamily = parts[2].trim();
					if( !subFamily.equals(""))
					{
						String subcatid = extractId(catid + "_" + subFamily, true);
						subcategory = inStore.getCategory(subcatid);
						if( subcategory == null)
						{
							subcategory = new Category(subcatid,subFamily);
							familycat.addChild(subcategory);
							inStore.getCategoryArchive().cacheCategory(subcategory);
						}
					}	
					
					if( subcategory == null)
					{
						product.addCatalog(familycat);
						product.setDefaultCatalog(familycat);
					}
					else
					{
						product.addCatalog(subcategory);
						product.setDefaultCatalog(subcategory);
					}
					
					product.setInventoryItems(null);
					products.put(product.getId(), product);
				}
				inErrorLog.add("Processed: " + products.size() + " products");
				inStore.getCategoryArchive().saveAll();
				
			}
			finally
			{
				FileUtils.safeClose(productReader);
			}
			
			Reader specReader=new FileReader(inSpecs);
			try 
			{
				String line;
				BufferedReader in=new BufferedReader(specReader);
				for (line=in.readLine(); line!=null && line.length()>0; line=in.readLine() )
				{
					String id = line.substring(0, 9).trim();
					if (products.get(id) != null)
					{
						Product product = (Product) products.get(id);
						String desc2 = line.substring(60, 90).trim();
						product.setProperty("description2", desc2); //This is not used much. Mostly numbers
						String width = line.substring(90,96).trim();
						product.setProperty("width", width);
						String length = line.substring(96, 102).trim();
						product.setProperty("length", length);					
						String die1 = line.substring(102, 111).trim();
						product.setProperty("die1", die1);
						String die2 = line.substring(111, 120).trim();
						product.setProperty("die2", die2);
						String die3 = line.substring(120, 129).trim();
						product.setProperty("die3", die3);
						String die4 = line.substring(129, 138).trim();
						product.setProperty("die4", die4);
						String unwind = line.substring(138, 139).trim();
						product.setProperty("unwind", unwind);
						String labelsPerRow = line.substring(139, 144).trim();
						product.setProperty("labelsPerRow", labelsPerRow);
						String labelsPerSheet = line.substring(144, 147).trim();
						product.setProperty("labelsPerSheet", labelsPerSheet);
						String UL = line.substring(147, 153).trim();
						product.setProperty("UL", UL);
						
						int index = 153;
						StringBuffer codes = new StringBuffer();
						String temp = line.substring(index, index+30).trim();
						product.clearKeywords();
						while(!temp.equals(""))
						{
							index+=30;
							codes.append(temp);
							codes.append(" ");
							temp = line.substring(index, index+30).trim(); 
							product.addKeyword(temp);
						}
					}
				}
			} finally
			{
				FileUtils.safeClose(specReader);
			}
		}
	}
		
	public void convertInventory(Store inStore, Map products, ConvertStatus inErrorLog) throws Exception
	{
			//read in Inventory file
			File input = new File( inStore.getStoreDirectory() , "upload/b-INVENTORY.txt");
			if( input.exists() )
			{
				Reader reader = new FileReader(input);
				try
				{
					BufferedReader in = new BufferedReader(reader);
					String line = null;
					int rowNum = 0;
					Product product = null;
					while( (line = in.readLine() ) != null)
					{
						rowNum++;
						if( line.length() == 0)
						{
							break;
						}
						String skuCell = line.substring(0,10).trim();
						if (!products.containsKey(skuCell)) {
							inErrorLog.add("Found inventory items for '"+skuCell+"', but no product to add it to.");
							continue;
						}
						
						product = (Product) products.get(skuCell);
						String description = line.substring(10,37).trim();
						//product.setName(skuCell);
						product.setDescription(description);
												
						InventoryItem inventoryItem = product.getInventoryItemBySku(product.getId());
						if (inventoryItem == null)
						{
							inventoryItem = new InventoryItem();
							inventoryItem.setSku(skuCell);
							product.addInventoryItem(inventoryItem);
						}
						String count = line.substring(47,57).trim();
						inventoryItem.setQuantityInStock(Integer.parseInt(count));
					}
					inErrorLog.add("Processed: " + products.size() + " products");
				}
				finally
				{
					FileUtils.safeClose(reader);
				}
			}


	}

	protected Product createProduct(Store inStore, String inId) throws Exception
	{		
		Product product = inStore.getProduct(inId);
		if ( product == null)
		{
			product = new Product();
			product.setId(inId);
			InventoryItem item = new InventoryItem();
			item.setSku(product.getId());
			item.setQuantityInStock(0);
			product.addInventoryItem(item);
			
		}
		return product;
	}

	public void convertOrderHistory(Store inStore, ConvertStatus inErrorLog) throws Exception
	{
		Set closedorders = new HashSet();
		SimpleDateFormat format = new SimpleDateFormat("MM/dd/yy");
		File input = new File(inStore.getStoreDirectory(), "upload/b-HISTORY.txt");
		if(input.exists())
		{
			Reader reader = new FileReader(input);
			try
			{
				BufferedReader in = new BufferedReader(reader);
				String line= null;
				int rowNum = 0;
				while((line=in.readLine())!= null)
				{
					rowNum++;
					if(line.length() == 0)
					{
						break;
					}
					String customercode = line.substring(0,6).trim();
					Customer customer = inStore.getCustomerArchive().getCustomer(customercode);
					if(customer == null)
					{
						customer = inStore.getCustomerArchive().createNewCustomer(customercode, null);
					}
					String prodid = line.substring(6, 16).trim();
					Product product = inStore.getProduct(prodid);
					String orderid = line.substring(36, 44).trim();
					String orderdate = line.substring(44, 52).trim();
					SubmittedOrder order= inStore.getOrderArchive().loadSubmittedOrder(inStore, customercode, orderid);
					if(order == null)
					{
						order = new SubmittedOrder();
						order.setCustomer(customer);
						order.setId(orderid);
					}
					if(!closedorders.contains(orderid))
					{
						if(order.getItems() != null)
						{
							order.getItems().clear();
						}
						closedorders.add(orderid);
					}
					order.setDate(format.parse(orderdate));
					String orderquantity = line.substring(52, 60).trim();
					String shipquantity = line.substring(60, 68).trim();
					String duedate = line.substring(68, 76).trim();
					String shipto = line.substring(76).trim();
					
					CartItem item = new CartItem();
					if( product == null )
					{
						product = new Product(prodid);
						product.setId(prodid);
						product.setAvailable(false);
					}
					item.setProduct(product);
					if( product.getInventoryItemCount() == 0)
					{	
						InventoryItem inventoryItem = new InventoryItem();
						inventoryItem.setSku(prodid);
						inventoryItem.setProduct(product);
						item.setInventoryItem(inventoryItem);
					}
						//This will be loaded dynamically
						//inStore.getProductArchive().saveProduct(product);
						
						
						//log.info("Order " + orderid + " has missing product " + prodid);
						//inErrorLog.add("Order " + orderid + " has missing product " + prodid + " skipping order");
						//continue;
					item.setProperty("duedate", duedate);
					item.setProperty("shiptoname", shipto);
					item.setShippingPrefix(shipto);
					item.setProperty("shipquantity", shipquantity);
					item.setQuantity(Integer.parseInt(orderquantity));
					item.setProperty("status", "closed");
					
					order.addItem(item);
					String status = "closed";
					OrderState orderStatus = new OrderState();
					orderStatus.setDescription("Closed");
					orderStatus.setId(status);
					order.setOrderState(orderStatus);
					inStore.getOrderArchive().saveOrder(inStore, order);
				}
			}
			finally
			{
				FileUtils.safeClose(reader);
			}
		}
	}
	public void convertOrders(Store inStore, ConvertStatus inErrorLog) throws Exception
	{
		Set openorders = new HashSet();
		SimpleDateFormat format = new SimpleDateFormat("MM/dd/yy");
		//read in Inventory file
		File input = new File( inStore.getStoreDirectory() , "upload/b-OPEN.txt");
		if( input.exists())
		{
			Reader reader = new FileReader(input);
			try
			{
				BufferedReader in = new BufferedReader(reader);
				String line = null;
				int rowNum = 0;
				while( (line = in.readLine() ) != null)
				{
					rowNum++;
					if( line.length() == 0)
					{
						break;
					}
					String customercode = line.substring(0,6).trim();
					Customer customer = inStore.getCustomerArchive().getCustomer(customercode);
					if( customer == null)
					{
						customer = inStore.getCustomerArchive().createNewCustomer(customercode, null);
					}
					String prodid = line.substring(6, 16).trim(); //Part#
					Product product = inStore.getProduct(prodid);


					String orderid = line.substring(36, 44).trim();
					SubmittedOrder order = inStore.getOrderArchive().loadSubmittedOrder(inStore,customercode ,orderid);
					if( order == null)
					{
						order = new SubmittedOrder();//inStore.getOrderArchive().
						order.setCustomer(customer);
						order.setId(orderid);
					}
					String orderdate = line.substring(44, 52).trim();
					if( !openorders.contains(orderid))
					{
						if( order.getItems() != null )
						{
							order.getItems().clear();
						}
						openorders.add(orderid);
						order.setDate(format.parse(orderdate)); //first one wins
					}
					CartItem item = new CartItem();
					item.setProperty("orderdate", orderdate);

					String customerpartnum = line.substring(16, 36).trim(); //Customer Part#
					item.setProperty("customerpartnum", customerpartnum);

					String quantity = line.substring(52,62).trim();
					
					if( product == null )
					{
						product = new Product(prodid);
						product.setId(prodid);
						product.setAvailable(false);
					}
					item.setProduct(product);
					if( product.getInventoryItemCount() == 0)
					{	
						InventoryItem inventoryItem = new InventoryItem();
						inventoryItem.setSku(prodid);
						inventoryItem.setProduct(product);
						item.setInventoryItem(inventoryItem);
					}
					
					item.setQuantity(Integer.parseInt(quantity));
					
					String status = line.substring(62, 71).trim();
					item.setProperty("status", status);
					String duedate = line.substring(96, 104).trim();
					item.setProperty("duedate", duedate);
					String shiptoname = line.substring(104).trim();
					item.setProperty("shiptoname", shiptoname);
					item.setShippingPrefix(shiptoname);
					
					order.addItem(item);
	
					OrderState orderStatus = new OrderState();
					orderStatus.setDescription("Open");
					orderStatus.setId("open");
					order.setOrderState(orderStatus);
					inStore.getOrderArchive().saveOrder(inStore, order);
				}
				//inErrorLog.add("Processed: " + products.size() + " products");
				//inStore.getCatalogArchive().saveCatalogs();
				//inStore.clearProducts();
				//input.delete();
			}
			finally
			{
				FileUtils.safeClose(reader);
			}
		}
		OrderState  closed = new OrderState();
		closed.setId("closed");
		closed.setDescription("Closed");
		List orderids = inStore.getOrderArchive().listAllOrderIds(inStore);
		log.info("Close out remaining orders");
		for (Iterator iterator = orderids.iterator(); iterator.hasNext();)
		{
			OrderId id = (OrderId) iterator.next();
			if( !openorders.contains(id.getOrderId()) && !id.getOrderId().startsWith("WEB") )
			{
				SubmittedOrder order = inStore.getOrderArchive().loadSubmittedOrder(inStore, id.getUsername(), id.getOrderId());
				if( order != null)
				{
					OrderState status = order.getOrderStatus();
					if( status == null || !status.getId().equals("closed"))
					{
						order.setOrderState(closed);
						inStore.getOrderArchive().saveOrder(inStore, order);
						inStore.getOrderSearcher().updateIndex(order);
					}
				}
			}
		}
		
	}

	
	
}
