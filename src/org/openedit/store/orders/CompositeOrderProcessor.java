/*
 * Created on Oct 3, 2005
 */
package org.openedit.store.orders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.ModuleManager;
import org.openedit.WebPageRequest;
import org.openedit.money.Money;
import org.openedit.store.Store;
import org.openedit.store.StoreException;

public class CompositeOrderProcessor implements OrderProcessor
{
	protected List fieldOrderProcessors;
	protected List fieldOrderProcessorObjects;
	protected ModuleManager fieldModuleManager;
	private static final Log log = LogFactory.getLog(CompositeOrderProcessor.class);
	
	public void archiveOrderData(Store inStore) throws StoreException
	{
		for (Iterator iter = getOrderProcessorObjects().iterator(); iter.hasNext();)
		{
			OrderArchive oa = (OrderArchive) iter.next();
			oa.archiveOrderData(inStore);
		}
	}

	public void processNewOrder(WebPageRequest inContext, Store inStore, Order inOrder)
		throws StoreException
	{
		if ("processing".equals(inOrder.get("processingstate")) )
		{
			throw new StoreException("Order is already being processed");
		}
		try{
			updateOrderProcessingState(inStore, inOrder, "processing");
			for (Iterator iter = getOrderProcessorObjects().iterator(); iter.hasNext();)
			{
				OrderProcessor op = (OrderProcessor) iter.next();
				op.processNewOrder(inContext, inStore, inOrder);
				if ( inOrder.getOrderStatus() != null &&  !inOrder.getOrderStatus().isOk() )
				{
					return;
				}
			}
			if(inOrder.getTotalPrice().isZero()){
				OrderState status = new OrderState();
				status.setOk(true);
				inOrder.setOrderState(status);
				//status.setId("accepted");
				return;
			}
			if( inOrder.getOrderState() == null)
			{
				throw new StoreException("Order state was not set with " + getOrderProcessorObjects().size() + " order archives." );			
			}
		}
		finally{
			
			updateOrderProcessingState(inStore, inOrder, "complete");
		}
	}
	
	protected void updateOrderProcessingState(Store inStore, Order inOrder, String inState){
		if (inOrder != null && inStore != null){
			inOrder.setProperty("processingstate",inState);
			inStore.saveOrder(inOrder);
		}
	}

	public void changeOrderStatus(OrderState inState, Store inStore, Order inOrder) throws StoreException
	{
		for (Iterator iter = getOrderProcessorObjects().iterator(); iter.hasNext();)
		{
			OrderArchive oa = (OrderArchive) iter.next();
			oa.changeOrderStatus(inState, inStore, inOrder);
		}
		
	}

	public List getOrderProcessors()
	{
		return fieldOrderProcessors;
	}

	public void setOrderProcessors(List inOrderArchives)
	{
		fieldOrderProcessors = inOrderArchives;
	}

	public void captureOrder(WebPageRequest inContext, Store inStore, Order inOrder) throws StoreException
	{
		for (Iterator iter = getOrderProcessorObjects().iterator(); iter.hasNext();)
		{
			OrderArchive oa = (OrderArchive) iter.next();
			oa.captureOrder(inContext, inStore, inOrder);
		}
	}

	public Map getOrderStates(Store inStore)
	{
		Map all = ListOrderedMap.decorate(new HashMap());
		for (Iterator iter = getOrderProcessorObjects().iterator(); iter.hasNext();)
		{
			OrderArchive oa = (OrderArchive) iter.next();
			Map list = oa.getOrderStates(inStore);
			if( list != null)
			{
//				log.info("Adding these " + list);
				all.putAll(list);
			}
		}
//		log.info("Finished this " + all);
		
		return all;
	}

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	public List getOrderProcessorObjects()
	{
		if (fieldOrderProcessorObjects == null) 
		{
			fieldOrderProcessorObjects = new ArrayList();
			for (Iterator iter = getOrderProcessors().iterator(); iter.hasNext();)
			{
				String element = (String) iter.next();
				try
				{
					OrderProcessor archive = (OrderProcessor)getModuleManager().getBean(element);
					fieldOrderProcessorObjects.add(archive);
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
					log.error("Skip: Could not load custom order archive " + element + " " + ex);
				}
			}
		}
		return fieldOrderProcessorObjects;
	}

	public void setOrderProcessorObjects(List inOrderArchiveObjects)
	{
		fieldOrderProcessorObjects = inOrderArchiveObjects;
	}

	public void refundOrder(WebPageRequest inContext, Store inStore, Order inOrder,
			Refund inRefund) throws StoreException {
		
		
		for (Iterator iter = getOrderProcessorObjects().iterator(); iter.hasNext();)
		{
			OrderProcessor op = (OrderProcessor) iter.next();
			op.refundOrder(inContext, inStore, inOrder, inRefund);
		}
		
		
	}

	
	
	
}
