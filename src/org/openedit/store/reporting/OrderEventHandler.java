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
import org.openedit.store.orders.Order;

import com.openedit.ModuleManager;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;

public class OrderEventHandler extends ScriptedDataSetEventAdapter implements IReportEventHandler
{
	private static final Log log = LogFactory.getLog(OrderEventHandler.class);

	protected SearcherManager searcherManager;
	protected ModuleManager moduleManager;
	protected WebPageRequest inReq;
	protected HitTracker fieldHitTracker;
	protected Iterator<?> fieldIterator;
	protected int count = 0;
	protected SearchQuery searchQuery;
	protected IReportContext context;
	
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
		count++;

		String catalogid = inReq.findValue("catalogid");
//		String name= dataSet.getName();
		
		Searcher searcher = getSearcherManager().getSearcher(catalogid, "storeOrder"); 
		double percent = ((double) count / (double) fieldHitTracker.size());
		log.info("Beginning Fetch: "+ count + " of " + fieldHitTracker.size() + " ("+String.format("%.2f", percent)+" % complete)") ;
		Iterator<?> iterator = fieldIterator;
		PropertyDetails details = searcher.getPropertyDetails();
		
		if (iterator.hasNext()) {
			Data target = (Data) iterator.next();
			for (Iterator<?> iterator2 = details.iterator(); iterator2.hasNext();) {
				PropertyDetail detail = (PropertyDetail) iterator2.next();
				String key = detail.getId();
				String val = target.get(key);
				if (key!=null && key.equals("shippingstatus")){
					if (val == null || val.isEmpty()){
						val = "Not Shipped";
					} else {
						Searcher shippingsearcher = getSearcherManager().getSearcher(catalogid, "shippingstatus");
						Data shipping = (Data) shippingsearcher.searchById(val);
						val = shipping.getName();
					}
				} else if (key!=null && key.equals("distributor")){
					if (val != null && !val.isEmpty()) {
						Searcher distributorsearcher = getSearcherManager().getSearcher(catalogid, "distributor");
						Data distributor = (Data) distributorsearcher.searchById(val.trim());
						val = distributor!=null ? distributor.get("fullname") : val;
					}
				}
				if(key != null && val!= null ){
					try{
						if(detail.isDate()){
							Date date = fieldHitTracker.getDateValue(target, key);
							row.setColumnValue(key, date);
						} else{
							row.setColumnValue(key, val);
						}
					} catch (ScriptException e){
						if (e.getMessage().contains("Invalid field name:")){
							log.info("Missing "+e.getMessage().replace("Invalid field name:", "\t").replace(".", "").trim());
						} else {
							log.info(e.getMessage());
						}
					} catch (Exception e){
						log.info(e.getMessage());
					}
				}
			}
			
			saveTallies(target, row, catalogid);

			return true;
		}
		return false;
	}
	
	protected void saveTallies(Data target, IUpdatableDataSetRow row, String catalogId) throws ScriptException{
		//get store and shipping info from order
		Searcher orderSearcher = getSearcherManager().getSearcher(catalogId, "storeOrder");
		Order order = (Order) orderSearcher.searchById(target.getId());
		String[][] money = {
				{"subtotal",order.getSubTotal().toShortString()},	
				{"totaltax",order.getTax().toShortString()},	
				{"totalshipping", order.getTotalShipping().toShortString()},	
				{"totalprice",order.getTotalPrice().toShortString()}
		};
		for (String [] pair:money){
			if(pair[1] != null ){
				try{
					row.setColumnValue(pair[0],new Double(pair[1]));
				} catch (ScriptException e){
					if (e.getMessage().contains("Invalid field name:")){
						log.info("Missing "+e.getMessage().replace("Invalid field name:", "\t").replace(".", "").trim());
					} else {
						log.info(e.getMessage());
					}
				} catch (Exception e){
					log.info(e.getMessage());
				}
			}
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
		
		fieldHitTracker = searcher.getAllHits(inReq);//prefilter - not all hits
		fieldHitTracker.setHitsPerPage(1000000);
		fieldIterator = fieldHitTracker.iterator();
		
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
		
		
	}

	@Override
	public void onPrepare(IReportContext inArg0) throws ScriptException
	{
		// TODO Auto-generated method stub
		
	}
}
