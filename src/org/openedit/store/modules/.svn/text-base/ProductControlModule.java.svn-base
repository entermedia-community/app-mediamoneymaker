package org.openedit.store.modules;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.openedit.logger.LogSearchModule;
import org.openedit.logger.LuceneLogSearcher;
import org.openedit.store.CartItem;
import org.openedit.store.Product;
import org.openedit.store.Store;
import org.openedit.store.orders.SubmittedOrder;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.modules.BaseModule;
import com.openedit.page.Page;
import com.openedit.page.Permission;

public class ProductControlModule extends BaseModule 
{
	private static final Log log = LogFactory.getLog(ProductControlModule.class);
	
	public boolean checkOrderApproval(WebPageRequest inReq) throws Exception {
		Store store = getStore(inReq);
		String sourcepath = store.getSourcePathForPage(inReq);
		Product product = store.getProductArchive().getProductBySourcePath(sourcepath);
		if( product == null)
		{
			log.error("Invalid product loaded " + sourcepath);
			return false;
		}
		SearchQuery search = store.getOrderSearcher().createSearchQuery();
		search.addMatches("customer", inReq.getUser().getUserName());
		search.addMatches("products", product.getId());
		search.addMatches("orderstatus", "authorized");
		
		String days = inReq.findValue("days");
		if (days != null) {
			int after = Integer.parseInt(days);
			Calendar rightNow = Calendar.getInstance();
			rightNow.add(Calendar.DAY_OF_YEAR, (-1 * after));
			Date searchDate = rightNow.getTime();
			search.addAfter("orderdate", searchDate);
		}
		HitTracker results = store.getOrderSearcher().search(search);

		if (results.size() > 0) {
			return true;
		}
		return false;
	}

	public boolean checkItemApproval(WebPageRequest inReq) throws Exception {
		Store store = getStore(inReq);
		String sourcepath = store.getSourcePathForPage(inReq);
		//Product product = store.getProductArchive().getProductBySourcePath(sourcepath);
		Product product = findProduct(inReq);
		
		if( product == null)
		{
			log.error("Invalid product loaded " + sourcepath);
			return false;
		}

		if( inReq.getUser() == null)
		{
			return false;
		}
		SearchQuery search = store.getOrderSearcher().createSearchQuery();
		search.addMatches("customer", inReq.getUser().getUserName());
		search.addMatches("products", product.getId());
		
		//search.addMatches("orderstatus", "authorized", "Approved");
		search.setSortBy("orderdate");

		HitTracker results = store.getOrderSearcher().search(search);

		if (results.size() > 0) {
			return checkItemApproval(inReq, product.getId(), store, results);
		}
		return false;
	}

	private boolean checkItemApproval(WebPageRequest inReq, String productid, Store store, HitTracker results) {
		Calendar limitDate = null;
		String days = inReq.getCurrentAction().getChildValue("days");
		if (days != null)
		{
			limitDate = new GregorianCalendar();
			limitDate.add(Calendar.DATE, -1 * Integer.parseInt(days));
		}
		
		for (Iterator iterator = results.iterator(); iterator.hasNext();) {
			Document doc = (Document) iterator.next();
			String orderid = doc.get("id");
			SubmittedOrder order = store.getOrderArchive().loadSubmittedOrder(store, inReq.getUserName(), orderid);
			if (order != null && order.getItems() != null) {
				for (Iterator iterator2 = order.getItems().iterator(); iterator2.hasNext();) {
					CartItem item = (CartItem) iterator2.next();
					if (item.getProduct().getId().equals(productid) && "approved".equals(item.getStatus())) {
						if (limitDate == null || order.getDate().after(limitDate.getTime()))
							return true;
					}
				}
			}
		}
		return false;
	}

	public Boolean limitDownloads(WebPageRequest inReq) throws Exception {
		// check download limits
		String times = inReq.getCurrentAction().getChildValue("times");
		if (times != null) {
			Store store = getStore(inReq);
			//LuceneLogSearcher searcher = (LuceneLogSearcher) store.getSearcherManager().getSearcherWithDefault(store.getCatalogId(), "download", "logSearcher");
			LogSearchModule module = (LogSearchModule) getModuleManager().getModule("LogSearchModule");
			inReq.setRequestParameter("foldername",store.getCatalogId() + "_downloadLog");
			LuceneLogSearcher searcher = (LuceneLogSearcher) module.getLogSearcher(inReq);
			String userName = inReq.getUserName();
			SearchQuery search = searcher.createSearchQuery();
			search.addMatches("user", userName);
			
			Product product = findProduct(inReq);
			if (product == null)
				return Boolean.TRUE;
			String sourcePath = product.getSourcePath();
			if (sourcePath.startsWith("/"))
			{
				sourcePath = sourcePath.substring(1);
			}
			search.addMatches("filename", "\"" + sourcePath + "\"");
			search.addMatches("result", "success");
			HitTracker downloads = searcher.search(search);
			int timesDownloaded = downloads.getTotal();
			int max = Integer.parseInt(times);
			if (timesDownloaded >= max) {
				return Boolean.FALSE;
			}
		}
		return Boolean.TRUE;
	}

	protected Store getStore(WebPageRequest inContext) {
		CartModule cartm = (CartModule) getModuleManager().getModule("CartModule");
		return cartm.getStore(inContext);
	}

	protected Product findProduct(WebPageRequest inReq) throws Exception
	{
		Store store = getStore(inReq);
		Product product = null;
		
		/* Product Id */
		String productid = inReq.findValue("productid");
		if (productid != null)
		{
			product = store.getProduct(productid);
			return product;
		}
		
		String sourcepath = findSourcePath(inReq);
		if (sourcepath != null)
		{
			product = store.getProductBySourcePath(sourcepath);
			if (product != null)
			{
				return product;
			}	
		}
		return product;
	}
	
	/**
	 * This is a funny action that actually checks the permissions of the products directory
	 * @param inReq
	 * @return
	 * @throws Exception
	 */
	public void loadProductPermissions(WebPageRequest inReq) throws Exception
	{
		//look in the products xconf and check those permissions
		String sourcepath = findSourcePath(inReq);
		if( sourcepath == null)
		{
			return; //Dont know what to do here
		}

		Store store = getStore(inReq);
		//TODO: Make sure I load up the soucepath xconf files
		String path = null;
		if( sourcepath.endsWith("/") )
		{
			path = "/" + store.getCatalogId() + "/products/" + sourcepath + "index.html";
		}
		else
		{
			path = "/" + store.getCatalogId() + "/products/" + sourcepath + ".html";
		}
		
		List names = Arrays.asList(new String[]{"customdownload","download","forcewatermark","editproduct"});
		
		Page page = getPageManager().getPage(path,true);
		for (Iterator iterator = names.iterator(); iterator.hasNext();)
		{
			String pername = (String) iterator.next();
			Permission per = page.getPermission(pername);
			if (per != null)
			{
				boolean value = per.passes(inReq);
				inReq.putPageValue("can" + per.getName(), Boolean.valueOf(value) );
			}
		}
	}

	protected String findSourcePath(WebPageRequest inReq) throws Exception
	{
		Product product = (Product) inReq.getPageValue("product");
		
		if (product != null)
		{
			return product.getSourcePath();
		}
		Store store = getStore(inReq);
		String sourcePath = store.getSourcePathForPage(inReq);

		if( sourcePath == null)
		{
			String productid = inReq.getRequestParameter("productid");
			
			//look for 
			if (productid != null)
			{
				return store.getProductSourcePathFinder().idToPath(productid);
			}
			
		}
		return sourcePath;
		
	}
}
