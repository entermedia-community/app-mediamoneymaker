package org.openedit.store.reporting;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.birt.report.engine.api.script.IUpdatableDataSetRow;
import org.eclipse.birt.report.engine.api.script.ScriptException;
import org.openedit.Data;
import org.openedit.data.PropertyDetails;
import org.openedit.data.Searcher;
import org.openedit.store.orders.Order;
import org.openedit.store.customer.Address;

public class FinancesEventHandler extends OrderItemEventHandler
{
	private static final Log log = LogFactory.getLog(FinancesEventHandler.class);
	
	protected void addOrderDetailsToRow(PropertyDetails details, Data target, IUpdatableDataSetRow row, String catalogId) throws ScriptException{
		super.addOrderDetailsToRow(details, target, row, catalogId);//adds all order info to row
		//get store and shipping info from order
//		Searcher orderSearcher = getSearcherManager().getSearcher(catalogId, "storeOrder");
//		Order order = (Order) orderSearcher.searchById(target.getId());
		Order order = getCurrentOrder();//only need current order, don't need to search
		
		//Shipping address
		StringBuilder buf = new StringBuilder();
		Address address = order.getShippingAddress();
		
		addKeyValuePairToRow(row,"shippingaddressname",address.getName());
//		addKeyValuePairToRow(row,"shippingaddressdescription",address.getDescription());
		
		buf.append(address.getAddress1())
			.append(address.getAddress2()==null || address.getAddress2().isEmpty() ? "" : address.getAddress2())
			.append(", ")
			.append(address.getCity()).append(", ")
			.append(address.getCityState()).append(", ")
			.append(address.getZipCode() == null || address.getZipCode().isEmpty() ? "" : address.getZipCode()+", ")
			.append(address.getCountry());
		
		addKeyValuePairToRow(row,"shippingaddress",buf.toString().trim());
		
		String storeId = address.getId();//refers to the address table
		if (storeId !=null && !storeId.isEmpty())
		{
			Searcher addressSearcher = getSearcherManager().getSearcher(catalogId, "address");
			Data data = (Data) addressSearcher.searchById(storeId);
			String storenumber = data.get("storenumber");
			
			addKeyValuePairToRow(row, "storenumber",storenumber);
		}
		
	}
}
