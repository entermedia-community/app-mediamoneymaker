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
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
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

	public StoreArchive getStoreArchive() {
		return fieldStoreArchive;
	}

	public void setStoreArchive(StoreArchive inStoreArchive) {
		fieldStoreArchive = inStoreArchive;
	}

	

	

	private void buildIndex(IndexWriter inWriter,
			TaxonomyWriter inTaxonomyWriter, List inList) throws Exception {
		PropertyDetails details = getPropertyDetailsArchive()
				.getPropertyDetails("storeOrder");

		for (Iterator iterator = inList.iterator(); iterator.hasNext();) {
			OrderId id = (OrderId) iterator.next();
			SubmittedOrder order = getOrderArchive().loadSubmittedOrder(
					getStore(), id.getUsername(), id.getOrderId());
			if (order != null) {
				// Document doc = new Document();
				// updateIndex(order, doc, details);
				updateIndex(inWriter, inTaxonomyWriter, order);
				// updateIndex(order);
				// inWriter.addDocument(doc);
			} else {
				log.error("Could not load " + id.getUsername() + " "
						+ id.getOrderId());
			}
		}

	}

	public OrderArchive getOrderArchive() {
		return (OrderArchive) getModuleManager().getBean(getCatalogId(),
				"orderArchive");
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
		HitTracker allorders = fieldSearch("username", inUser.getUserName(),
				"date");
		return allorders;
	}

	protected File getOrdersDirectory(Store inStore) {
		return new File(inStore.getStoreDirectory(), "orders");
	}

//	public void updateIndex(Order inOrder) throws StoreException {
//		try {
//			Document doc = new Document();
//			PropertyDetails details = getPropertyDetailsArchive()
//					.getPropertyDetails("storeOrder");
//			updateIndex(inOrder, doc, details);
//			populateProductDetails(inOrder, doc);
//			Term term = new Term("id", inOrder.getId());
//			getIndexWriter().updateDocument(term, doc, getAnalyzer());
//			flush();
//			clearIndex();
//		} catch (Exception ex) {
//			throw new StoreException(ex);
//		}
//
//	}

	protected void populateProductDetails(Order inOrder, Document doc) {

		StringBuffer products = new StringBuffer();
		for (Iterator iterator2 = inOrder.getItems().iterator(); iterator2
				.hasNext();) {
			CartItem item = (CartItem) iterator2.next();
			if (item.getProduct() != null) {
				products.append(item.getProduct().getId());
				products.append(" ");
			}
		}
		String value = products.toString();

		doc.add(new Field("products", value, Field.Store.YES,
				Field.Index.NOT_ANALYZED));

	}

	public Store getStore() {
		if (fieldStore == null) {
			fieldStore = getStoreArchive().getStore(getCatalogId());
		}
		return fieldStore;
	}

	public void setStore(Store inStore) {
		fieldStore = inStore;
	}

	// public HitTracker getAllHits(WebPageRequest inReq) {
	// List list = getOrderArchive().listAllOrderIds(getStore());
	// return new ListHitTracker(list);
	// }

	@Override
	protected void reIndexAll(IndexWriter inWriter,
			TaxonomyWriter inTaxonomyWriter) {
		List list = getOrderArchive().listAllOrderIds(getStore());
		try {
			buildIndex(inWriter, inTaxonomyWriter, list);
		} catch (Exception e) {
			throw new OpenEditException(e);
		}
	}

	@Override
	public Object searchById(String inId) {
		Data orderinfo = (Data) super.searchById(inId);
		if (orderinfo != null) {
			return getOrderArchive().loadSubmittedOrder(getStore(),
					orderinfo.get("customer"), inId);
		} else {
			return null;
		}
		// return orderinfo;

	}

	@Override
	public void saveData(Data inData, User inUser) {
		if (inData instanceof Order) {
			Order order = (Order) inData;
			getOrderArchive().saveOrder(getStore(), order);
		}
		updateIndex(inData);
	}

	@Override
	public String nextId() {
		return getStore().getOrderGenerator().nextOrderNumber(getStore());
	}

}
