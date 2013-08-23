import org.openedit.store.Store;
import org.openedit.store.StoreException;
import org.openedit.store.orders.Order;
import org.openedit.store.orders.OrderProcessor
import org.openedit.store.orders.Refund;

import com.openedit.WebPageRequest;

public class customorderprocessor implements OrderProcessor{
	
	
	public void processNewOrder(WebPageRequest inReq, Store inStore, Order order) throws StoreException{
		println("processing custom order");
	}
	
	
	
	public void refundOrder(WebPageRequest inContext, Store inStore,
		Order inOrder, Refund inRefund) throws StoreException {
		println("refunding in  custom order");
		
		
	}
	
}