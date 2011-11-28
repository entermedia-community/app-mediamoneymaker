package com.openedit.store.convert;

import org.openedit.store.CartItem;
import org.openedit.store.InventoryItem;
import org.openedit.store.Product;
import org.openedit.store.Store;
import org.openedit.store.StoreTestCase;
import org.openedit.store.convert.ConvertStatus;
import org.openedit.store.convert.MainFrameConverter;
import org.openedit.store.orders.SubmittedOrder;

public class MainFrameConvertTest extends StoreTestCase
{

	public MainFrameConvertTest(String inArg0)
	{
		super(inArg0);
	}

	public void testOrderHistoryImport() throws Exception
	{
		MainFrameConverter mainframe = new MainFrameConverter();
		Store store = getStore();
		ConvertStatus status = new ConvertStatus();
		mainframe.convert(store, status);
		Product product = store.getProduct("57743");
		Product noInventoryProduct = store.getProduct("57740");
		
		/* Check properties and inventory */
		assertNotNull(product);
		//assertNotNull(product.getInventoryItemBySku(product.getId()));
		assertEquals(1,product.getCategories().size());
		//assertNotNull(store.getProductArchive().listAllProductIds());
		assertNotNull(product.getInventoryItem(0));
		
		assertEquals(11000, product.getInventoryItemBySku(product.getId()).getQuantityInStock());
		assertEquals("4x6_vantaggio", product.getDefaultCategory().getId());
		assertEquals("4.0000",product.get("width"));
		
		SubmittedOrder order = store.getOrderArchive().loadSubmittedOrder(store, "A01385", "61324");
		assertNotNull(order);
		assertNotNull(order.getOrderState());
		assertEquals("open", order.getOrderState().getId());
		
		
		//From order history
		order = store.getOrderArchive().loadSubmittedOrder(store, "A00200", "50995");
		assertNotNull(order);
		assertEquals(order.getItems().size(), 3);
		CartItem item = (CartItem) order.getItems().get(0);
		assertNotNull(item);
		assertNotNull(item.getProperties().get("duedate"));
		assertEquals("05/03/05", item.getProperties().get("duedate"));
		assertEquals("closed", item.getProperties().get("status"));
		assertEquals("ABBYLAND FOODS INC.", (String)item.getProperties().get("shiptoname"));
		
		/* Check reseting inventory for product*/
		assertNotNull(noInventoryProduct);
		assertEquals(0, noInventoryProduct.getInventoryItemCount());

		InventoryItem ii = new InventoryItem();
		ii.setSku("57740-1");
		ii.setProduct(noInventoryProduct);
		ii.setQuantityInStock(1000);
		noInventoryProduct.addInventoryItem(ii);
		store.getProductArchive().saveProduct(noInventoryProduct);
		
		noInventoryProduct = store.getProductArchive().getProduct("57740"); 
		assertNotNull(noInventoryProduct);
		/* One inventory item is created when importing orders */
		assertEquals(1, noInventoryProduct.getInventoryItemCount());
		assertEquals(noInventoryProduct.getInventoryItemBySku("57740-1").getQuantityInStock(), 1000);
		
		/* Reset*/
		mainframe.convert(store, status);
		noInventoryProduct = store.getProductArchive().getProduct("57740");
		
		assertNotNull(noInventoryProduct);
		assertEquals(0,noInventoryProduct.getInventoryItemCount());
		assertNull(product.getInventoryItemBySku("57740-1"));
		
	}
	
	public void testOpenOrders() throws Exception
	{
		Store store = getStore();
		SubmittedOrder order = store.getOrderArchive().loadSubmittedOrder(store, "A01385", "61324");
		assertEquals(11,order.getItems().size());
		CartItem item = (CartItem)order.getItems().get(0);
		assertEquals("WAREHOUSE",item.get("status"));
		assertEquals("06/16/06", item.get("duedate"));
		assertEquals("ALTO DAIRY", item.get("shiptoname"));
		assertEquals("ALTO DAIRY", item.getShippingPrefix());
		
		/* We need an order to test this */
		/* 
		SubmittedOrder openOrder = store.getOrderArchive().loadSubmittedOrder(store, "admin", "WEB0000004");
		assertNotNull(openOrder);
		assertEquals("accepted", openOrder.getOrderStatus().getId());
		*/
	}
	
}
