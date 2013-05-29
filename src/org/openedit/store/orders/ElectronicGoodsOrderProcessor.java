/*
 * Created on Apr 4, 2005
 */
package org.openedit.store.orders;

import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.store.CartItem;
import org.openedit.store.Product;
import org.openedit.store.Store;
import org.openedit.store.StoreException;

import com.openedit.ModuleManager;
import com.openedit.WebPageRequest;
import com.openedit.page.manage.PageManager;

/**
 * @author cburkey
 *
 */
public class ElectronicGoodsOrderProcessor extends BaseOrderProcessor
{
	protected PageManager fieldPageManager;
	protected ModuleManager fieldModuleManager;
	private static final Log log = LogFactory.getLog(ElectronicGoodsOrderProcessor.class);

	/* (non-javadoc)
	 * @see com.openedit.store.OrderArchive#exportNewOrder(com.openedit.WebPageRequest, com.openedit.store.Store, com.openedit.store.Order)
	 */
	public void processNewOrder(WebPageRequest inContext, Store inStore, Order inOrder)
		throws StoreException
	{
		log.info("Check for electronic delivery");
		//look for products that are electronic delivery
		for (Iterator iter = inOrder.getItems().iterator(); iter.hasNext();)
		{
			CartItem item = (CartItem) iter.next();
			Product product = item.getProduct();
			if ( product != null)
			{
				
				//TODO: Move to using a property not a keyword
				String keywords = item.getProduct().getShippingMethodId();
				
				if ( keywords != null )
				{
					log.info("Found keywords");
					inContext.putSessionValue("order",inOrder);
					inContext.putSessionValue("customer",inOrder.getCustomer());
					try
					{
						if ( keywords.indexOf("classified") >= 0 )
						{

							inContext.forward(inStore.getStoreHome() +"/electronic/classified.html");
							
							//inContext.forward(path, inReq)redirect( inStore.getStoreHome() +"/electronic/classified.html");
							
						}
						else if ( keywords.indexOf("electronic-delivery") >= 0 )
						{
							log.info("forward to electronic");
							String path = inContext.findValue("electronicdeliverypath");
							if( path == null)
							{
								path = inStore.getStoreHome() + "/electronic/electronic-delivery.html";
							}
							inContext.forward( path);
//							Page page = getPageManager().getPage( inStore.getStoreHome() + "/electronic/electronic-delivery.html");
//							getModuleManager().executePathActions(page, inContext);
//							getModuleManager().executePageActions(page, inContext);
						}
					} 
					catch ( Exception ex)
					{
						throw new StoreException(ex);
					}
				}
				//TODO: Move to new Options API with the value being the Path to the delivery
			}
		}
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	@Override
	public void refundOrder(WebPageRequest inContext, Store inStore,
			Refund inRefund) throws StoreException {
		// TODO Auto-generated method stub
		
	}
}
