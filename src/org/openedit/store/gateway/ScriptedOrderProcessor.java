package org.openedit.store.gateway;

import org.entermediadb.scripts.GroovyScriptRunner;
import org.entermediadb.scripts.Script;
import org.entermediadb.scripts.ScriptManager;
import org.openedit.ModuleManager;
import org.openedit.WebPageRequest;
import org.openedit.store.Store;
import org.openedit.store.StoreException;
import org.openedit.store.orders.Order;
import org.openedit.store.orders.OrderProcessor;
import org.openedit.store.orders.Refund;

public class ScriptedOrderProcessor implements OrderProcessor {

	
	protected OrderProcessor fieldOrderProcessor;
	protected ModuleManager fieldModuleManager;
	protected ScriptManager fieldScriptManager;
	
	
	public ModuleManager getModuleManager() {
		return fieldModuleManager;
	}


	public void setModuleManager(ModuleManager inModuleManager) {
		fieldModuleManager = inModuleManager;
	}


	public ScriptManager getScriptManager() {
		return fieldScriptManager;
	}


	public void setScriptManager(ScriptManager inScriptManager) {
		fieldScriptManager = inScriptManager;
	}



	public OrderProcessor getOrderProcessor(Store inStore) {
		
		if (fieldOrderProcessor == null) {
			Script script = getScriptManager().loadScript("/" + inStore.getCatalogId() + "/events/scripts/orders/customorderprocessor.groovy");
			GroovyScriptRunner runner = (GroovyScriptRunner)getModuleManager().getBean("groovyScriptRunner");
			OrderProcessor op  = (OrderProcessor)runner.newInstance(script);
			fieldOrderProcessor = op;
			
			
		}

		return fieldOrderProcessor;
		
		
		
		
	}


	public void setOrderProcessor(OrderProcessor inOrderProcessor) {
		fieldOrderProcessor = inOrderProcessor;
	}


	public void processNewOrder(WebPageRequest inContext, Store inStore,
			Order inOrder) throws StoreException {
	
		getOrderProcessor(inStore).processNewOrder(inContext, inStore, inOrder);
		
		
	
		
		
		
	}

	
	public void refundOrder(WebPageRequest inContext, Store inStore,
			Order inOrder, Refund inRefund) throws StoreException {
	getOrderProcessor(inStore).refundOrder(inContext, inStore, inOrder, inRefund);
		
	}

		
	
	
}
