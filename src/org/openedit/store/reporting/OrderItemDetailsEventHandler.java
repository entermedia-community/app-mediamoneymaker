package org.openedit.store.reporting;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.birt.report.engine.api.script.IUpdatableDataSetRow;
import org.eclipse.birt.report.engine.api.script.ScriptException;
import org.openedit.data.Searcher;
import org.openedit.store.CartItem;
import org.openedit.store.Product;
import org.openedit.Data;

public class OrderItemDetailsEventHandler extends OrderItemEventHandler
{
	private static final Log log = LogFactory.getLog(OrderItemDetailsEventHandler.class);
	
	protected void addItemDetailsToRow(CartItem item, IUpdatableDataSetRow row, String catalogId) throws ScriptException{
		super.addItemDetailsToRow(item, row, catalogId);//adds sku, productid, productname, quantity, price,refundamount
		
		Searcher manufacturerSearcher = getSearcherManager().getSearcher(catalogId, "manufacturer");
		Searcher categorySearcher = getSearcherManager().getSearcher(catalogId, "categoryid");
		Product product = item.getProduct();
		//manufacturer, category
		String manufacturerId = product.get("manufacturerid");
		Data manufacturer = (Data) manufacturerSearcher.searchById(manufacturerId);
		String manufacturerName = manufacturer.getName();
		String categoryId = product.get("categoryid");
		Data category = (Data) categorySearcher.searchById(categoryId);
		String categoryName = category.getName();
		String [][] pairs = {
				{"manufacturerid", manufacturerId},
				{"manufacturername", manufacturerName},
				{"categoryid", categoryId},
				{"categoryname",  categoryName}
		};
		for (String [] pair:pairs){
			addKeyValuePairToRow(row,pair[0],pair[1]);
		}
	}

}
