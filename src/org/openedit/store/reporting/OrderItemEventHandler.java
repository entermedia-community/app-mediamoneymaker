package org.openedit.store.reporting;

import java.util.Date;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.birt.report.engine.api.script.IReportContext;
import org.eclipse.birt.report.engine.api.script.IUpdatableDataSetRow;
import org.eclipse.birt.report.engine.api.script.ScriptException;
import org.eclipse.birt.report.engine.api.script.element.IReportDesign;
import org.eclipse.birt.report.engine.api.script.eventadapter.ScriptedDataSetEventAdapter;
import org.eclipse.birt.report.engine.api.script.eventhandler.IReportEventHandler;
import org.eclipse.birt.report.engine.api.script.instance.IDataSetInstance;
import org.eclipse.birt.report.engine.api.script.instance.IPageInstance;
import org.openedit.Data;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.money.Money;
import org.openedit.store.CartItem;
import org.openedit.store.customer.Address;
import org.openedit.store.orders.Order;
import org.openedit.store.orders.RefundItem;

import com.openedit.ModuleManager;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;

public class OrderItemEventHandler extends ScriptedDataSetEventAdapter implements IReportEventHandler
{
	private static final Log log = LogFactory.getLog(OrderItemEventHandler.class);

	protected SearcherManager searcherManager;
	protected ModuleManager moduleManager;
	protected WebPageRequest inReq;
	protected HitTracker fieldHitTracker;
	protected Iterator<?> fieldOrderIterator;
	protected Iterator<?> fieldItemIterator;
	protected Order fieldCurrentOrder;
	
	protected Data fieldTarget;
	protected PropertyDetails fieldDetails;
	
	protected int count = 0;
	protected int itemcount = 0;
	
	protected SearchQuery searchQuery;
	protected IReportContext context;
	
	protected boolean requiresCartItems()
	{
		return true;
	}
	
	public Order getCurrentOrder()
	{
		return fieldCurrentOrder;
	}
	
	public SearcherManager getSearcherManager() {
		return searcherManager;
	}

	public void setSearcherManager(SearcherManager fieldSearcherManager) {
		this.searcherManager = fieldSearcherManager;
	}

	public ModuleManager getModuleManager() {
		return moduleManager;
	}

	public void setModuleManager(ModuleManager fieldModuleManager) {
		this.moduleManager = fieldModuleManager;
	}

	public WebPageRequest getWebPageRequest() {
		return inReq;
	}

	public void setFieldWebPageRequest(WebPageRequest fieldWebPageRequest) {
		this.inReq = fieldWebPageRequest;
	}

	public boolean fetch(IDataSetInstance dataSet, IUpdatableDataSetRow row) throws ScriptException {
		//local vars
		boolean success = false;
		//make sure iterator has been set
		if (fieldOrderIterator != null){
			//initial case or when no items
			if (fieldItemIterator == null || !fieldItemIterator.hasNext()){
				if (fieldOrderIterator.hasNext()){
					count++;
					itemcount=0;
					fieldTarget = (Data) fieldOrderIterator.next();
					String catalogid = inReq.findValue("catalogid");
					Searcher searcher = getSearcherManager().getSearcher(catalogid, "storeOrder");
					fieldDetails = searcher.getPropertyDetails();
					fieldCurrentOrder = (Order) searcher.searchById(fieldTarget.getId());
					fieldItemIterator = fieldCurrentOrder.getItems().iterator();
				} else {
					fieldItemIterator = null;
					fieldOrderIterator = null;
				}
			}
			if (requiresCartItems()){
				if (fieldItemIterator!=null && fieldItemIterator.hasNext()){
					itemcount++;
					log.info("Fetching "+count+" of "+fieldHitTracker.size()+", item no. "+itemcount+" of "+fieldCurrentOrder.getItems().size());

					String catalogid = inReq.findValue("catalogid");
					CartItem item = (CartItem) fieldItemIterator.next();
					addOrderDetailsToRow(fieldDetails,fieldTarget,row,catalogid);
					addItemDetailsToRow(item,row,catalogid);
					success = true;
				}
			} else {
				if (fieldOrderIterator!=null && fieldDetails!=null && fieldTarget!=null){
					log.info("Fetching "+count+" of "+fieldHitTracker.size()+", omitting cart items");
					
					String catalogid = inReq.findValue("catalogid");
					addOrderDetailsToRow(fieldDetails,fieldTarget,row,catalogid);
					fieldItemIterator = null;
					success = true;
				}
			}
		}
		return success;
	}
	
	protected void addOrderDetailsToRow(PropertyDetails details, Data target, IUpdatableDataSetRow row, String catalogId) throws ScriptException{
		Iterator<?> iterator = details.iterator();
		while (iterator.hasNext()) {
			PropertyDetail detail = (PropertyDetail) iterator.next();
			String key = detail.getId();
			String val = target.get(key);
			if (key!=null && key.equals("shippingstatus")){
				if (val == null || val.isEmpty()){
					val = "Not Shipped";
				} else {
					Searcher searcher = getSearcherManager().getSearcher(catalogId, "shippingstatus");
					Data shipping = (Data) searcher.searchById(val);
					if (shipping!=null)
					{
						val = shipping.getName();
					}
				}
			} else if (key!=null && key.equals("distributor")){
				if (val != null && !val.isEmpty()) {
					Searcher distributorsearcher = getSearcherManager().getSearcher(catalogId, "distributor");
					Data distributor = (Data) distributorsearcher.searchById(val.trim());//bug: needs to be trimmed bc. val includes whitespace
					if (distributor != null)
					{
						val = distributor.get("fullname");
					}
				}
			}
			if(key != null && val!= null){
				if(detail.isDate()){
					Date date = fieldHitTracker.getDateValue(target, key);
					addKeyValuePairToRow(row,key,date);
				} else {
					addKeyValuePairToRow(row,key,val);
				}
			}
		}
		
		//get store and shipping info from order
		Searcher orderSearcher = getSearcherManager().getSearcher(catalogId, "storeOrder");
		Order order = (Order) orderSearcher.searchById(target.getId());
		String[][] money = {
				{"subtotal",order.getSubTotal().toShortString()},	
				{"totaltax",order.getTax().toShortString()},	
				{"totalshipping", order.getTotalShipping().toShortString()},	
				{"totalprice",order.getTotalPrice().toShortString()},
				//include refund info
				{"refundsubtotal",order.calculateRefundSubtotal().toShortString()},
				{"refundshipping",order.calculateRefundShipping().toShortString()},
				{"refundtax",order.calculateRefundTax().toShortString()},
				{"refundtotal",order.calculateRefundTotal().toShortString()},
				{"totalafterrefunds",order.calculateTotalAfterRefunds().toShortString()},
		};
		for (String [] pair:money){
			addKeyValuePairToRow(row,pair[0],pair[1]);
		}
	}
	
	protected void addKeyValuePairToRow(IUpdatableDataSetRow row, String key, Object value) throws ScriptException{
		if (key==null || value == null) return;
		try{
			row.setColumnValue(key, value);
		} catch (ScriptException e){
			if (e.getMessage().contains("Invalid field name:")){
//				log.info("Missing "+e.getMessage().replace("Invalid field name:", "\t").replace(".", "").trim());
			} else {
				log.info(e.getMessage());
			}
		} catch (Exception e){
			log.info(e.getMessage());
		}
	}
	
	protected void addItemDetailsToRow(CartItem item, IUpdatableDataSetRow row, String catalogId) throws ScriptException{
		//include refund information for the cart item
//		Money refund = Order.calculateRefundTotalForCartItem(getCurrentOrder(), item);
		RefundItem refundItem = Order.calculateRefundInfoForCartItem(getCurrentOrder(), item);
		Money refund = refundItem.getTotalPrice();//total amount refunded for this product on this order
		int quantity = refundItem.getQuantity();//total quantity refunded for this product on this order
		//display designation, in reference to brands
		String brand = "";
		String brandid = item.getProduct().get("displaydesignationid");
		if (brandid!=null && !brandid.isEmpty()){
			Searcher displaysearcher = getSearcherManager().getSearcher(catalogId, "displaydesignationid");
			Data data = (Data) displaysearcher.searchById(brandid);
			brand = data.getName();
		}
		String [][] pairs = {
				{"sku", item.getSku()},
				{"productid", item.getProduct().getId()},
				{"productname", item.getProduct().getName()},
				{"quantity", String.valueOf(item.getQuantity())},
				{"price",  item.getTotalPrice().toShortString()},
				{"refundamount",  refund.toShortString()},
				{"refundquantity",  String.valueOf(quantity)},
				{"brand", brand}
		};
		for (String [] pair:pairs){
			addKeyValuePairToRow(row,pair[0],pair[1]);
		}
	}

	public void open(IDataSetInstance dataSet) throws ScriptException {
		log.info("Starting BIRT - OrderEventHandler opening");
		
		String catalogid = inReq.getRequestParameter("catalogid");
		if(catalogid == null){
			 catalogid = inReq.findValue("catalogid");
		}
//		String name = dataSet.getName();
		Searcher searcher = getSearcherManager().getSearcher(catalogid,"storeOrder");
		//filter results
		SearchQuery query = searcher.addStandardSearchTerms(inReq);
		query.addSortBy("orderdate");
		fieldHitTracker = searcher.cachedSearch(inReq, query);
		if(fieldHitTracker == null){
			fieldHitTracker = searcher.getAllHits(inReq);//all hits if problem
		}
		fieldHitTracker.setHitsPerPage(1000000);
		fieldOrderIterator = fieldHitTracker.iterator();
		
		super.open(dataSet);
	}

	public void beforeOpen(IDataSetInstance dataSet,IReportContext reportContext) throws ScriptException {
		log.info("Starting BIRT - OrderEventHandler prepping");
		
		searcherManager = (SearcherManager) reportContext.getParameterValue("searcherManager");
		inReq = (WebPageRequest) reportContext.getParameterValue("context");
		context = reportContext;
		
//		Map params = dataSet.getInputParameters();
//		String name = dataSet.getName();
		
		super.beforeOpen(dataSet, reportContext);
	}

	@Override
	public void afterFactory(IReportContext inArg0) throws ScriptException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterRender(IReportContext inArg0) throws ScriptException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeFactory(IReportDesign inArg0, IReportContext inArg1) throws ScriptException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeRender(IReportContext inArg0) throws ScriptException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void initialize(IReportContext inArg0) throws ScriptException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPageEnd(IPageInstance inArg0, IReportContext inArg1) throws ScriptException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPageStart(IPageInstance inArg0, IReportContext inArg1) throws ScriptException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPrepare(IReportContext inArg0) throws ScriptException
	{
		// TODO Auto-generated method stub
		
	}
}
