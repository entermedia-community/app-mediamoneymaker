package org.openedit.store.orders;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.FSDirectory;
import org.openedit.Data;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.lucene.BaseLuceneSearcher;
import org.openedit.data.lucene.LuceneSearchQuery;
import org.openedit.store.CartItem;
import org.openedit.store.Category;
import org.openedit.store.Option;
import org.openedit.store.Store;
import org.openedit.store.StoreArchive;
import org.openedit.store.StoreException;
import org.openedit.store.customer.Customer;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.ListHitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.users.User;

public class OrderSearcher extends BaseLuceneSearcher {
	private static final Log log = LogFactory.getLog(OrderSearcher.class);
	protected Store fieldStore;
	protected OrderArchive fieldOrderArchive;
	protected StoreArchive fieldStoreArchive;

	public StoreArchive getStoreArchive()
	{
		return fieldStoreArchive;
	}

	public void setStoreArchive(StoreArchive inStoreArchive)
	{
		fieldStoreArchive = inStoreArchive;
	}

	public HitTracker fieldSearch(WebPageRequest inReq)
			throws OpenEditException {
		SearchQuery search = addStandardSearchTerms(inReq);
		if (search == null) {
			search = new LuceneSearchQuery();
			search.addOrsGroup("orderstatus", "accepted authorized");
			// search.putInput("orderstatus", "accepted");
		}
		addActionFilters(inReq, search);
		return cachedSearch(inReq, search);
	}

	public HitTracker fieldSearchForUser(WebPageRequest inReq, User inUser)
			throws OpenEditException {
		SearchQuery search = addStandardSearchTerms(inReq);
		if (search == null) {
			search = new LuceneSearchQuery();
		}
		addActionFilters(inReq, search);
		search.addMatches("customer", inUser.getUserName());
		search.setSortBy(inReq.findValue("ordersort"));
		return cachedSearch(inReq, search);
	}

	private void buildIndex(IndexWriter inWriter, List inList) throws Exception {
		PropertyDetails details = getPropertyDetailsArchive()
				.getPropertyDetails("storeOrder");

		for (Iterator iterator = inList.iterator(); iterator.hasNext();) {
			OrderId id = (OrderId) iterator.next();
			SubmittedOrder order = getOrderArchive().loadSubmittedOrder(
					getStore(), id.getUsername(), id.getOrderId());
			if (order != null) {
				//Document doc = new Document();
				//updateIndex(order, doc, details);
				updateIndex(inWriter, order);
				//updateIndex(order);
				//inWriter.addDocument(doc);
			} else {
				log.error("Could not load " + id.getUsername() + " "
						+ id.getOrderId());
			}
		}

	}

	public OrderArchive getOrderArchive() {
		return (OrderArchive) getModuleManager().getBean(getCatalogId(), "orderArchive");
	}

	public void setOrderArchive(OrderArchive inOrderArchive) {
		fieldOrderArchive = inOrderArchive;
	}

	public List listOrders(Store inStore, WebPageRequest inContext)
			throws StoreException {
		File ordersFile = getOrdersDirectory(inStore);
		List allorders = new ArrayList();
		try {
			allorders = inStore.getOrderArchive().listAllOrderIds(inStore);
		} catch (Exception ex) {
			throw new StoreException(ex);
		}
		// Collections.sort( allorders);
		return allorders;
	}

	public HitTracker listOrdersForUser(Store inStore, User inUser)
			throws StoreException {
		File ordersFile = getOrdersDirectory(inStore);
		HitTracker allorders = fieldSearch("username" ,inUser.getUserName(),
				"date");
		return allorders;
	}

	protected File getOrdersDirectory(Store inStore) {
		return new File(inStore.getStoreDirectory(), "orders");
	}

	public void updateIndex(Order inOrder) throws StoreException {
		try {
			Document doc = new Document();
			PropertyDetails details = getPropertyDetailsArchive()
					.getPropertyDetails("order");
			updateIndex(inOrder, doc, details);
			Term term = new Term("id", inOrder.getId());
			getIndexWriter().updateDocument(term, doc, getAnalyzer());
			flush();
			clearIndex();
		} catch (Exception ex) {
			throw new StoreException(ex);
		}

	}

//	protected void updateIndex(Order inOrder, Document doc,
//			PropertyDetails inDetails) {
//		super.updateIndex(inOrder);
//		
//		if (inOrder.getCustomer() != null) {
//			doc.add(new Field("username", inOrder.getCustomer().getUserName(),
//					Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
//		}
//		
//		StringBuffer keywords = new StringBuffer();
//		
//		List details = inDetails.findIndexProperties();
//		for (Iterator iterator = details.iterator(); iterator.hasNext();) {
//			PropertyDetail detail = (PropertyDetail) iterator.next();
//			String prop = detail.getId();
//			if (prop.equals("id") || prop.equals("name")) {
//				continue;
//			}
//			String value = null;
//			if (prop.equals("orderstatus")) {
//				value = inOrder.getOrderStatus().getId();// inOrder.get(detail.getId());
//			} else if (prop.equals("customer")) {
//				Customer customer = inOrder.getCustomer();
//				String customerString = customer.getFirstName() + " "
//						+ customer.getLastName();
//				Map properties = customer.getUser().getProperties();
//				for (Iterator iterator2 = properties.keySet().iterator(); iterator2
//						.hasNext();) {
//					String key = (String) iterator2.next();
//					keywords.append(" ");
//					keywords.append(properties.get(key));
//				}
//				keywords.append(" ");
//				keywords.append(customerString);
//				value = customer.getUserName();
//
//			} else if (prop.equals("products")) {
//				StringBuffer products = new StringBuffer();
//				for (Iterator iterator2 = inOrder.getItems().iterator(); iterator2
//						.hasNext();) {
//					CartItem item = (CartItem) iterator2.next();
//					if (item.getProduct() != null) {
//						products.append(item.getProduct().getId());
//						products.append(" ");
//					}
//				}
//				value = products.toString();
//			} else if (prop.equals("categories")) {
//				StringBuffer categories = new StringBuffer();
//				for (Iterator iterator2 = inOrder.getItems().iterator(); iterator2
//						.hasNext();) {
//					CartItem item = (CartItem) iterator2.next();
//					if (item.getProduct() != null) {
//						Category cat = item.getProduct().getDefaultCategory();
//						if (cat != null) {
//							categories.append(cat.getId());
//							categories.append(" ");
//						}
//
//					}
//				}
//				value = categories.toString();
//			} else if (detail.isDate()) {
//				if(inOrder.getDate() != null){
//				value = DateTools.dateToString(inOrder.getDate(),
//						Resolution.MINUTE);
//				}
//			} else {
//				value = inOrder.get(prop);
//			}
//			if (value != null) {
//				if (detail.isStored()) {
//					if (detail.isDate()) {
//						doc.add(new Field(detail.getId(), value,
//								Field.Store.YES,
//								Field.Index.NOT_ANALYZED_NO_NORMS));
//					} else {
//						doc.add(new Field(detail.getId(), value,
//								Field.Store.YES, Field.Index.ANALYZED));
//					}
//				} else {
//					doc.add(new Field(detail.getId(), value, Field.Store.NO,
//							Field.Index.ANALYZED));
//				}
//				if (detail.isKeyword()) {
//					keywords.append(" ");
//					keywords.append(value);
//				}
//			}
//			List productkeywords = getPropertyDetailsArchive()
//					.getPropertyDetailsCached("product")
//					.findKeywordProperties();
//			for (Iterator iterator2 = inOrder.getItems().iterator(); iterator2
//					.hasNext();) {
//				CartItem item = (CartItem) iterator2.next();
//				for (Iterator iterator3 = productkeywords.iterator(); iterator3
//						.hasNext();) {
//					PropertyDetail itemdetail = (PropertyDetail) iterator3
//							.next();
//					String kvalue = item.get(itemdetail.getId());
//					if (kvalue == null) {
//						Option option = item.getOption(itemdetail.getId());
//						if (option != null) {
//							kvalue = option.getValue();
//						}
//					}
//
//					if (kvalue != null) {
//						keywords.append(" ");
//						keywords.append(kvalue);
//					}
//
//				}
//			}
//
//			doc.add(new Field("description", keywords.toString(),
//					Field.Store.YES, Field.Index.ANALYZED));
//		}
//	}

	public Store getStore()
	{
		if (fieldStore == null)
		{
			fieldStore = getStoreArchive().getStore(getCatalogId());
		}
		return fieldStore;
	}

	public void setStore(Store inStore) {
		fieldStore = inStore;
	}

//	public HitTracker getAllHits(WebPageRequest inReq) {
//		List list = getOrderArchive().listAllOrderIds(getStore());
//		return new ListHitTracker(list);
//	}

	

	@Override
	protected void reIndexAll(IndexWriter inWriter) {
		List list = getOrderArchive().listAllOrderIds(getStore());
		try {
			buildIndex(inWriter, list);
		} catch (Exception e) {
			throw new OpenEditException(e);
		}
	}

	@Override
	public Object searchById(String inId)
	{
		Data orderinfo = (Data) super.searchById(inId);
		if(orderinfo != null){
			return getOrderArchive().loadSubmittedOrder(getStore(), orderinfo.get("customer"), inId);	
		}
		else{
			return null;
		}
		//return orderinfo;
		
	}
	
	
	@Override
	public void saveData(Data inData, User inUser){
		if(inData instanceof Order){
			Order order = (Order) inData;
			getOrderArchive().saveOrder(getStore(), order);
		}
		super.updateIndex(inData);
	}
	
	@Override
	public String nextId(){
	return getStore().getOrderGenerator().nextOrderNumber(getStore());
	}
	
}
