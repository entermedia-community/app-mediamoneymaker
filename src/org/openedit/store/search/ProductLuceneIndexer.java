package org.openedit.store.search;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.money.Money;
import org.openedit.page.Page;
import org.openedit.store.Category;
import org.openedit.store.InventoryItem;
import org.openedit.store.Product;
import org.openedit.store.Store;
import org.openedit.store.StoreException;
import org.openedit.util.OutputFiller;
import org.openedit.util.PathUtilities;

public class ProductLuceneIndexer  {
//	static final Log log = LogFactory.getLog(ProductLuceneIndexer.class);
//	protected Analyzer fieldAnalyzer;
//	protected boolean usesSearchSecurity = false;
//	protected Store fieldStore;
//	protected File fieldRootDirectory;
//	protected SearcherManager fieldSearcherManager;
//
//	public File getRootDirectory() {
//		return fieldRootDirectory;
//	}
//
//	public void setRootDirectory(File inRootDirectory) {
//		fieldRootDirectory = inRootDirectory;
//	}
//
//
//
//	public Store getStore() {
//		return fieldStore;
//	}
//
//	public void setStore(Store inStore) {
//		fieldStore = inStore;
//	}
//
//	public boolean usesSearchSecurity() {
//		return usesSearchSecurity;
//	}
//
//	public void setUsesSearchSecurity(boolean inUsesSearchSecurity) {
//		usesSearchSecurity = inUsesSearchSecurity;
//	}
//
//	public Analyzer getAnalyzer() {
//		return fieldAnalyzer;
//	}
//
//	public void setAnalyzer(Analyzer inAnalyzer) {
//		fieldAnalyzer = inAnalyzer;
//	}
//
//	protected Set buildCatalogSet(Product inProduct) {
//		HashSet allCatalogs = new HashSet();
//		Collection catalogs = inProduct.getCategories();
//		allCatalogs.addAll(catalogs);
//		for (Iterator iter = catalogs.iterator(); iter.hasNext();) {
//			Category catalog = (Category) iter.next();
//			buildCatalogSet(catalog, allCatalogs);
//		}
//		return allCatalogs;
//	}
//
//	protected void buildCatalogSet(Category inCatalog, Set inCatalogSet) {
//		inCatalogSet.add(inCatalog);
//		Category parent = inCatalog.getParentCatalog();
//		if (parent != null) {
//			buildCatalogSet(parent, inCatalogSet);
//		}
//	}
//
//	
//	
//
//	public Document createProductDoc(Product product, PropertyDetails inDetails) {
//		
//
//
//
//		Field path = new Field("sourcepath", product.getSourcePath(),
//				Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS);
//		doc.add(path);
//
//		String primaryfile = product.getProperty("primaryimagename");
//		if (primaryfile != null) {
//			Field imagename = new Field("primaryimagename", primaryfile,
//					Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS);
//			doc.add(imagename);
//		}
//
//		if (product.getCatalogId() == null) {
//			product.setCatalogId(getStore().getCatalogId());
//		}
//		Field catalogid = new Field("catalogid", product.getCatalogId(),
//				Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS);
//		doc.add(catalogid);
//
//		Money price = product.getYourPrice();
//		if (price != null) {
//			doc.add(new Field("yourprice", price.toString(), Field.Store.YES,
//					Field.Index.NOT_ANALYZED_NO_NORMS));
//			String shortprice = price.toShortString();
//			shortprice = shortprice.replaceAll("\\.", "");
//
//			doc.add(new Field("priceOrdering", pad(shortprice), Field.Store.NO,
//					Field.Index.NOT_ANALYZED_NO_NORMS));
//
//			if (product.isOnSale()) {
//				Money regular = product.getRetailPrice();
//				if (regular != null) {
//					doc.add(new Field("regularprice", regular.toString(),
//							Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
//				}
//			}
//		}
//		// this may be invalid field of -1 but we still need to add it for
//		// the search to work
//		if (product.getOrdering() != -1) {
//			doc.add(new Field("ordering", Integer.toString(product
//					.getOrdering()), Field.Store.NO,
//					Field.Index.NOT_ANALYZED_NO_NORMS));
//		}
//
//		Set catalogs = buildCatalogSet(product);
//
//		//populateDescription(doc, product, inDetails, catalogs);
//		populateJoinData("category", doc, catalogs, "id", false);
//
//		// populateSecurity(doc, product, catalogs);
//		if (usesSearchSecurity()) {
//			populatePermission(doc, product, "viewproduct");
//		}
//		populateExactCategory(doc, product);
//		StringBuffer keywords = new StringBuffer();
//
//
//
//		
//
//		
//		
//		/*
//		 * StringBuffer sizes = new StringBuffer(); for (Iterator iters =
//		 * product.getSizes().iterator(); iters.hasNext();) { String size =
//		 * (String) iters.next(); sizes.append(size); sizes.append(" "); }
//		 * doc.add(Field.Text("sizes", sizes.toString().trim() ) );
//		 */
//		StringBuffer items = new StringBuffer();
//		for (Iterator iter = product.getInventoryItems().iterator(); iter
//				.hasNext();) {
//			InventoryItem item = (InventoryItem) iter.next();
//			items.append(item.getSku());
//			items.append(" ");
//		}
//		if (items.length() > 0) {
//			doc.add(new Field("items", items.toString().trim(), Field.Store.NO,
//					Field.Index.ANALYZED));
//		}
//
//		String folderpath = product.getSourcePath();
//		if (folderpath.endsWith("/")) {
//			folderpath = folderpath.substring(0, folderpath.length() - 1);
//		}
//		folderpath = PathUtilities.extractDirectoryPath(folderpath) + "/";
//		doc.add(new Field("foldersourcepath", folderpath, Field.Store.YES,
//				Field.Index.NOT_ANALYZED_NO_NORMS));
//
//		// for SF - Jobs
//		Page sourcePage = product.getSourcePage();
//		if (sourcePage != null) {
//			String clientview = sourcePage.getProperty("clientview");
//			if (("true").equals(clientview)) {
//				doc.add(new Field("clientview", "true", Field.Store.YES,
//						Field.Index.NOT_ANALYZED_NO_NORMS));
//			}
//		}
//
//		return doc;
//	}
//
//	protected Document populateProduct(IndexWriter writer, Product product,
//			boolean add, PropertyDetails inDetails) throws OpenEditException {
//		if (product.isAvailable()) {
//			Document doc = createProductDoc(product, inDetails);
//			writeDoc(writer, product.getId().toLowerCase(), doc, add);
//			return doc;
//		}
//		return null;
//	}
//
//	protected void populatePermission(Product p, Document inDoc,
//			List inAccessList, String inPermission) {
//
//		// permission is "viewasset" for earch security
//		boolean secure = false;
//		StringBuffer buffer = new StringBuffer();
//
//		for (Iterator iterator = inAccessList.iterator(); iterator.hasNext();) {
//			String allow = (String) iterator.next();
//			buffer.append(" ");
//			buffer.append(allow);
//		}
//		if (p.get("group") != null) {
//			
//			Collection<?> groups = p.getValues("group");
//			if (groups.size() > 0) {
//				secure = true;
//			}
//			Iterator<?> itr = groups.iterator();
//			while(itr.hasNext()) {
//				String group = (String) itr.next();
//				buffer.append(" ");
//				buffer.append("group_" + group);
//			}
//		}
//		
//		
//		
//		String distributorid = p.get("distributor");
//		if (distributorid != null) {
//			buffer.append(" distributor_" + distributorid);			
//		}
//		
//		if (p.get("user") != null) {
//			String[] users = p.get("user").split("\\s");
//
//			if (users.length > 0) {
//
//				secure = true;
//			}
//
//			for (int i = 0; i < users.length; i++) {
//				String user = users[i];
//				buffer.append(" ");
//				buffer.append("user_" + user);
//
//			}
//		}
//
//		if (inAccessList.size() == 0 && secure == false) {
//			buffer.append("blank");
//			buffer.append(" ");
//			buffer.append("true");
//
//		}
//
//		inDoc.add(new Field(inPermission, buffer.toString(), Field.Store.YES,
//				Field.Index.ANALYZED));
//	}
//
//	protected void populatePermission(Product inProduct, Document inDoc,
//			Page inPage, String inPermission) throws OpenEditException {
//		List add = getStore().getProductSecurityArchive().getAccessList(inPage);
//		populatePermission(inProduct, inDoc, add, inPermission);
//	}
//
//	protected void populatePermission(Document inDoc, Product inProduct,
//			String inPermission) throws OpenEditException {
//		List add = getStore().getProductSecurityArchive().getAccessList(
//				inProduct);
//		populatePermission(inProduct, inDoc, add, inPermission);
//	}
//
//
//
//	public void populateDateJoin(PropertyDetail inDetail, Document doc,
//			Collection allParentCategories, String inField, boolean inIsStored) {
//		StringBuffer buffer = new StringBuffer();
//		Date tosave = null;
//		boolean savebottom = "bottom".equals(inDetail.get("rangeposition"));
//
//		for (Iterator iter = allParentCategories.iterator(); iter.hasNext();) {
//
//			Object catalog = iter.next();
//
//			try {
//				String foo = null;
//				if (catalog instanceof Data) {
//					Data local = (Data) catalog;
//					foo = local.get(inField);
//				} else {
//					Document local = (Document) catalog;
//					foo = local.get(inField);
//				}
//
//				Date d1 = inDetail.getDateFormat().parse(foo);
//				if (tosave == null) {
//					tosave = d1;
//					continue;
//				}
//				if (savebottom) {
//					if (d1.before(tosave)) {
//						tosave = d1;
//					}
//				} else {
//					if (d1.after(tosave)) {
//						tosave = d1;
//					}
//				}
//			} catch (Exception ex) {
//				log.error(ex);
//			}
//		}
//		if (tosave != null) {
//			String val = DateTools.dateToString(tosave, Resolution.SECOND);
//			if (inIsStored) {
//				doc.add(new Field(inDetail.getId(), val, Field.Store.YES,
//						Field.Index.NOT_ANALYZED_NO_NORMS));
//			} else {
//				doc.add(new Field(inDetail.getId(), val, Field.Store.NO,
//						Field.Index.NOT_ANALYZED_NO_NORMS));
//			}
//		}
//	}
//
//	public void populateJoinData(PropertyDetail inDetail, Document doc,
//			Collection allParentCategories, String inField) {
//		populateJoinData(inDetail.getId(), doc, allParentCategories, inField,
//				inDetail.isStored());
//	}
//
//	public void populateJoinData(String inType, Document doc,
//			Collection inDataElements, String inField, boolean inIsStored) {
//		StringBuffer buffer = new StringBuffer();
//		for (Iterator iter = inDataElements.iterator(); iter.hasNext();) {
//			Object data = iter.next();
//			if (data instanceof Data) {
//				Data catalog = (Data) data;
//				buffer.append(catalog.get(inField));
//			} else {
//				Document catalog = (Document) data;
//				buffer.append(catalog.get(inField));
//			}
//			buffer.append(" ");
//		}
//		// Add in all the catalogs, price, gender, image on disk?, name+ full
//		// text
//		if (buffer.length() > 0) {
//			if (inIsStored) {
//				doc.add(new Field(inType, buffer.toString(), Field.Store.YES,
//						Field.Index.ANALYZED));
//			} else {
//				doc.add(new Field(inType, buffer.toString(), Field.Store.NO,
//						Field.Index.ANALYZED));
//			}
//		}
//		/*
//		 * Not used any more if ( item.getDepartment() != null) { doc.add( new
//		 * Field("department", item.getDepartment(), Field.Store.YES,
//		 * Field.Index.ANALYZED)); }
//		 */
//
//	}
//
//	protected void populateExactCategory(Document doc, Product item) {
//		// the idea here is to add a field that allows you to search for
//		// products in a category WITHOUT sub category products showing.
//		StringBuffer buffer = new StringBuffer();
//		for (Iterator iter = item.getCategories().iterator(); iter.hasNext();) {
//			Category catalog = (Category) iter.next();
//			buffer.append(catalog.getId());
//			buffer.append(" ");
//		}
//
//		if (buffer.length() > 0) {
//			doc.add(new Field("category-exact", buffer.toString(),
//					Field.Store.NO, Field.Index.ANALYZED));
//		}
//		/*
//		 * Not used any more if ( item.getDepartment() != null) { doc.add( new
//		 * Field("department", item.getDepartment(), Field.Store.YES,
//		 * Field.Index.ANALYZED)); }
//		 */
//	}
//
//	protected void populateDescription(Document doc, Product product,
//			PropertyDetails inDetails, Set inCategories) throws StoreException {
//		if (product.getName() != null) {
//			// This cannot be used in sorts since it is ANALYZED. For sorts use
//			doc.add(new Field("name", product.getName(), Field.Store.YES,
//					Field.Index.ANALYZED));
//			doc.add(new Field("name_sortable", product.getName().toLowerCase(),
//					Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
//		}
//		String htmlPath = product.getSourcePath() + ".html";
//		// Low level reading in of text
//		StringBuffer fullDesc = new StringBuffer();
//		fullDesc.append(product.getName());
//		fullDesc.append(' ');
//		fullDesc.append(product.getId());
//		populateKeywords(fullDesc, product, inDetails);
//		// add a bunch of stuff to the full text field
//		File descriptionFile = new File(getRootDirectory(), "/"
//				+ getStore().getCatalogId() + "/products/" + htmlPath);
//		if (descriptionFile.exists() || descriptionFile.length() > 0) {
//			FileReader descread = null;
//			try {
//				descread = new FileReader(descriptionFile);
//				StringWriter out = new StringWriter();
//				new OutputFiller().fill(descread, out);
//				fullDesc.append(out.toString());
//			} catch (Exception ex) {
//				throw new StoreException(ex);
//			} finally {
//				FileUtils.safeClose(descread);
//			}
//		}
//		fullDesc.append(' ');
//		for (Iterator iter = inCategories.iterator(); iter.hasNext();) {
//			Category cat = (Category) iter.next();
//			fullDesc.append(cat.getName());
//			fullDesc.append(' ');
//		}
//
//		String[] dirs = product.getSourcePath().split("/");
//
//		for (int i = 0; i < dirs.length; i++) {
//			fullDesc.append(dirs[i]);
//			fullDesc.append(' ');
//		}
//
//		try {
//			// String result = fixInvalidCharacters(fullDesc.toString());
//			doc.add(new Field("description", fullDesc.toString(),
//					Field.Store.NO, Field.Index.ANALYZED));
//		} catch (Exception ex) {
//			throw new StoreException(ex);
//		}
//	}
//
//	/**
//	 * This is here to help the stemmer handle weird cases of words For example:
//	 * century21 should contain both centuri and century21 in the search index
//	 * 
//	 * @param inString
//	 * @return
//	 * @throws IOException
//	 */
//	// protected String fixInvalidCharacters(String inString) throws IOException
//	// {
//	// StringBuffer fixed = new StringBuffer(inString.length() + 20);
//	// RecordLookUpAnalyzer analyser = new RecordLookUpAnalyzer();
//	// TokenStream stream = analyser.tokenStream("description", new
//	// StringReader(inString));
//	// Token token = stream.;
//	// while (token != null)
//	// {
//	// char[] text = token.termBuffer();
//	// if (text.length > 3 )
//	// {
//	// // loop over all the words until we find an invalid one
//	// for (int i = 0; i < text.length; i++)
//	// {
//	// fixed.append(text[i]);
//	// //Checking for Y in the middle of words: harleydavidson will now
//	// //index as harley davidson.
//	// if( text[i] == 'y')
//	// {
//	// break;
//	// }
//	// }
//	// fixed.append(' ');
//	// }
//	// // Always put the original back in there
//	// fixed.append(text);
//	// fixed.append(' ');
//	// token = stream.next();
//	// }
//	// return fixed.toString();
//	// }
//
//	protected void populateKeywords(StringBuffer inFullDesc, Product inProduct,
//			PropertyDetails inDetails) throws StoreException {
//		inFullDesc.append(' ');
//		if (inProduct.hasKeywords()) {
//			for (Iterator iter = inProduct.getKeywords().iterator(); iter
//					.hasNext();) {
//				String desc = (String) iter.next();
//				desc = desc.replace('/', ' '); // Is this needed?
//				desc = desc.replace('\\', ' ');
//				inFullDesc.append(desc);
//				inFullDesc.append(' ');
//			}
//		}
//		for (Iterator iter = inDetails.getDetails().iterator(); iter.hasNext();) {
//			PropertyDetail det = (PropertyDetail) iter.next();
//			if (det.isKeyword()) {
//				String prop = inProduct.getProperty(det.getId());
//				if (det.isList() && prop != null) {
//					Searcher searcher = getSearcherManager().getListSearcher(
//							det);
//					if (searcher != null) {
//						Data data = (Data) searcher.searchById(prop);
//						if (data != null) {
//							prop = data.getName();
//						}
//					}
//				}
//
//				if (prop != null) {
//					inFullDesc.append(prop);
//					inFullDesc.append(' ');
//				}
//			}
//		}
//	}
//
//	public String pad(String inValue) {
//
//		// return getDecimalFormatter().format(inShortprice);
//
//		String all = "0000000000000" + inValue;
//		String cut = all.substring(all.length() - 10); // 10 is the max width
//		// of integers
//		return cut;
//	}
//
//	protected void writeDoc(IndexWriter writer, String inId, Document doc,
//			boolean add) {
//		try {
//			if (add) {
//				writer.addDocument(doc, getAnalyzer());
//			} else {
//				Term term = new Term("id", inId);
//				writer.updateDocument(term, doc, getAnalyzer());
//			}
//		} catch (IOException ex) {
//			throw new StoreException(ex);
//		}
//	}
//
//	public SearcherManager getSearcherManager() {
//		return fieldSearcherManager;
//	}
//
//	public void setSearcherManager(SearcherManager inSearcherManager) {
//		fieldSearcherManager = inSearcherManager;
//	}
}
