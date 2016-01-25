/*
 * Created on Nov 16, 2004
 */
package org.openedit.store.modules;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Attribute;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.upload.FileUpload;
import org.entermediadb.asset.upload.FileUploadItem;
import org.entermediadb.asset.upload.UploadRequest;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.FilteredTracker;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.Searcher;
import org.openedit.event.WebEvent;
import org.openedit.event.WebEventListener;
import org.openedit.hittracker.HitTracker;
import org.openedit.money.Money;
import org.openedit.page.Page;
import org.openedit.repository.RepositoryException;
import org.openedit.store.Category;
import org.openedit.store.Image;
import org.openedit.store.InventoryItem;
import org.openedit.store.Option;
import org.openedit.store.Price;
import org.openedit.store.PriceSupport;
import org.openedit.store.Product;
import org.openedit.store.Store;
import org.openedit.store.StoreArchive;
import org.openedit.store.StoreException;
import org.openedit.store.edit.StoreEditor;
import org.openedit.util.FileUtils;
import org.openedit.util.PathUtilities;
import org.openedit.xml.ElementData;

/**
 * @author cburkey
 * 
 */
public class CatalogEditModule extends BaseStoreModule {
	protected WebEventListener fieldWebEventListener;
	private static final String LINKTREE = "linktree";

	private static final String ERROR = "editerror";
	private static final Log log = LogFactory.getLog(CatalogEditModule.class);

	public void addCatalog(WebPageRequest inContext) throws OpenEditException {
		StoreEditor storeEditor = getStoreEditor(inContext);
		Category newCatalog = storeEditor.addNewCatalog(new Date().getTime()
				+ "", "New Category");
		storeEditor.setCurrentCategory(newCatalog);
		storeEditor.saveCatalog(newCatalog);
		inContext.putPageValue("category", newCatalog);
	}

	public void moveCatalog(WebPageRequest inContext) throws OpenEditException {
		StoreEditor storeEditor = getStoreEditor(inContext);
		String catalog2Id = inContext.getRequestParameter("categoryid2");
		Category catalog1 = storeEditor.getCurrentCategory();
		Category catalog2 = storeEditor.getCatalog(catalog2Id);

		if (catalog1 != null && catalog2 != null) {
			// don't move if same catalog or catalog2 is already the parent
			if (catalog1 != catalog2 && catalog1.getParentCatalog() != catalog2
					&& !catalog1.isAncestorOf(catalog2)) {
				catalog1.getParentCatalog().removeChild(catalog1);

				catalog1.setParentCatalog(catalog2);
				catalog2.addChild(catalog1);

				storeEditor.saveCatalog(catalog1);
				storeEditor.saveCatalog(catalog2);
			}
		}
	}

	public void moveCatalogHere(WebPageRequest inContext)
			throws OpenEditException {
		StoreEditor storeEditor = getStoreEditor(inContext);
		String catalog2Id = inContext.getRequestParameter("categoryid2");
		Category catalog1 = storeEditor.getCurrentCategory();
		Category catalog2 = storeEditor.getCatalog(catalog2Id);

		storeEditor.moveCatalogBefore(catalog1, catalog2);
	}

	public void sortCatalog(WebPageRequest inContext) throws OpenEditException {
		getStoreEditor(inContext).sortCatalog(
				getStoreEditor(inContext).getCurrentCategory());
	}

	public void moveCatalogUp(WebPageRequest inContext)
			throws OpenEditException {
		StoreEditor storeEditor = getStoreEditor(inContext);
		String catalogId = inContext.getRequestParameter(CATEGORYID);
		Category catalog = storeEditor.getCatalog(catalogId);

		// don't move if doesn't have a parent
		if (catalog.getParentCatalog() != null) {
			storeEditor.moveCatalogUp(catalog);
		}
	}

	public void moveCatalogDown(WebPageRequest inContext)
			throws OpenEditException {
		StoreEditor storeEditor = getStoreEditor(inContext);
		String catalogId = inContext.getRequestParameter(CATEGORYID);
		Category catalog = storeEditor.getCatalog(catalogId);

		// don't move if doesn't have a parent
		if (catalog.getParentCatalog() != null) {
			storeEditor.moveCatalogDown(catalog);
		}
	}

	public void deleteCatalog(WebPageRequest inContext)
			throws OpenEditException {
		String catalogId = inContext.getRequestParameter(CATEGORYID);
		StoreEditor storeEditor = getStoreEditor(inContext);
		Category catalog = storeEditor.getCatalog(catalogId);
		if (catalog != null) {
			Category parent = catalog.getParentCatalog();
			storeEditor.deleteCatalog(catalog);
			if (parent != null) {
				storeEditor.setCurrentCategory(parent);
			}
			// reloadCatalogs(inContext);
		}
	}


	

	public void uploadItemImages(WebPageRequest inContext)
			throws OpenEditException {
		FileUpload command = new FileUpload();
		command.setPageManager(getPageManager());
		UploadRequest properties = command.parseArguments(inContext);
		if (properties == null) {
			return;
		}
		String id = inContext.getRequestParameter("imageid");
		// handle the upload
		// figure out path
		if (id != null && id.length() > 0) {
			StoreEditor editor = getStoreEditor(inContext);
			Image image = editor.getImage(id);

			InventoryItem item = editor.getCurrentItem();
			String pid = item.getSku();
			String path = editor.getStore().getStoreHome()
					+ "/products/images/items/" + image.getType() + "/" + pid
					+ image.getPostfix() + ".jpg";
			properties.saveFirstFileAs(path, inContext.getUser());

			// command.saveFile(properties,path,inContext);
		}
	}

	public void uploadProductImages(WebPageRequest inContext)
			throws OpenEditException {
		Store store = getStore(inContext);
		StoreEditor editor = getStoreEditor(inContext);
		Product prod = editor.getCurrentProduct();
	
		FileUpload command = new FileUpload();
		command.setPageManager(getPageManager());
		UploadRequest properties = command.parseArguments(inContext);
		if (properties == null) {
			return;
		}

		String sourcepath = "/productimages/" + prod.getSourcePath();
		String path = "/WEB-INF/data/" + store.getCatalogId()
				+ "/originals/" + sourcepath + "/original.jpg";

		properties.saveFirstFileAs(path, inContext.getUser());
		File image = new File(store.getRootDirectory(), path);

		MediaArchive archive = store.getStoreMediaManager().getMediaArchive();

		Asset asset = archive.getAssetBySourcePath(sourcepath);
		if (asset == null) {
			asset = archive.createAsset(sourcepath);
		}
		// archive.getAssetImporter().getAssetUtilities().populateCategory(asset,
		// archive, "/WEB-INF/data/media/catalogs/public/originals", path,
		// null);
		asset.setPrimaryFile(image.getName());
		archive.removeGeneratedImages(asset);
		archive.saveAsset(asset, null);
		prod.setProperty("image", asset.getId());

	}

	public void uploadProductVideo(WebPageRequest inContext)
			throws OpenEditException {
		FileUpload command = new FileUpload();
		command.setPageManager(getPageManager());
		UploadRequest properties = command.parseArguments(inContext);
		if (properties == null) {
			return;
		}
		FileUploadItem item = properties.getFirstItem();
		if (item == null) {
			inContext.putPageValue("upload-error",
					"We could not upload your file - please try again.");
		}

		StoreEditor editor = getStoreEditor(inContext);
		Product prod = editor.getCurrentProduct();

		String pid = prod.getId();
		String path = inContext.getPageProperty("uploadvideofolder");
		if (path == null) {
			path = editor.getStore().getStoreHome() + "/products/videos/"
					+ editor.getStore().getProductPathFinder().idToPath(pid)
					+ ".flv";
		} else {
			if (!inContext.getUserName().equals(prod.get("user"))
					&& !inContext.getUser().isInGroup(
							getUserManager(inContext).getGroup("administrators"))) {
				return;
			}
			if (path.charAt(path.length() - 1) != '/') {
				path += "/";
			}
			String ext = properties.getFirstItem().getName();
			if (ext.lastIndexOf('.') != -1)
				ext = ext.substring(ext.lastIndexOf('.'));
			else
				ext = "";
			path += prod.getId() + ext;
			prod.setProperty("videopath", path);
			editor.saveProduct(prod);
		}
		properties.saveFirstFileAs(path, inContext.getUser());

	}

	// public void uploadCatalogImages(WebPageRequest inContext) throws
	// OpenEditException
	// {
	// FileUpload command = new FileUpload();
	// command.setPageManager(getPageManager());
	// UploadRequest properties = command.parseArguments(inContext);
	// if (properties == null)
	// {
	// return;
	// }
	//
	// StoreEditor editor = getStoreEditor(inContext);
	//
	// Category catalog = editor.getCurrentCategory();
	// String cid = catalog.getId();
	// String path = editor.getStore().getStoreHome() +
	// "/categories/images/original/" + cid + ".jpg";
	// String thumb = editor.getStore().getStoreHome() +
	// "/categories/images/thumb/" + cid + ".jpg";
	// properties.saveFirstFileAs(path, inContext.getUser());
	// File in = new File(getRoot(), path);
	// File out = new File(getRoot(), thumb);
	// ConvertInstructions inStructions = new ConvertInstructions();
	// inStructions.setMaxScaledSize(175, 175);
	// getImageConverter().resizeImage(in, out, inStructions);
	//
	// // command.saveFile(properties,path,inContext);
	//
	// }

	public void uploadCatalogFiles(WebPageRequest inContext)
			throws OpenEditException {
		FileUpload command = new FileUpload();
		command.setRoot(getRoot());
		command.setPageManager(getPageManager());
		UploadRequest properties = command.parseArguments(inContext);
		if (properties == null) {
			return;
		}
		String name = inContext.getRequestParameter("uploadedfilename");
		// prefix the name with the catalog id
		name = PathUtilities.extractFileName(name);

		// handle the upload
		// figure out path
		StoreEditor editor = getStoreEditor(inContext);
		String saveas = editor.getCurrentCategory().getId() + "-" + name;

		properties.saveFirstFileAs(saveas, inContext.getUser());

	}

	// public void resizeImage(WebPageRequest inContext) throws
	// OpenEditException
	// {
	// ConvertInstructions inStructions = new ConvertInstructions();
	// String path = inContext.getRequestParameter("imagepath");
	// ImageConverter resizer = getImageConverter();
	// File in = new File(getRoot(), path);
	// String width = inContext.findValue("width");
	// inStructions.setMaxScaledSize(new Dimension(Integer.parseInt(width),
	// Integer.MAX_VALUE));
	//
	// try
	// {
	// resizer.resizeImage(in, in, inStructions);
	// }
	// catch (Exception ex)
	// {
	// log.error(ex);
	// throw new StoreException(ex);
	// }
	// }

	public void deleteImage(WebPageRequest inContext) throws OpenEditException {
		StoreEditor editor = getStoreEditor(inContext);
		Category catalog = editor.getCurrentCategory();
		String type = inContext.getRequestParameter("imageid");

		Image image = catalog.getImage(type);
		if (image != null) {
			editor.deleteImage(image);
		}

	}

	// public void generateAllThumbnails( WebPageRequest inContext ) throws
	// OpenEditException
	// {
	// ImageResizer resizer = new ImageResizer();
	// ConvertInstructions inStructions = new ConvertInstructions();
	// File storeDir = getStoreEditor( inContext
	// ).getStore().getStoreDirectory();
	// try
	// {
	// //TODO: Do a recursive search
	// resizer.resizeImagesInDirectory( new File ( storeDir,
	// "/products/images/original" ),
	// new File( storeDir, "/products/images/medium" ), inStructions, true );
	//
	// resizer.resizeImagesInDirectory( new File ( storeDir, "images/medium" ),
	// new File( storeDir, "images/thumb" ), inStructions, true );
	// }
	// catch ( Exception ioe )
	// {
	// throw new OpenEditException( ioe );
	// }
	// }

	public void reloadCatalogs(WebPageRequest inRequest)
			throws OpenEditException {
		// StoreEditor editor = getStoreEditor(inRequest);
		// editor.reloadCatalogs();
		//
		// treeid = name + "_" + store.getCatalogId() + "_" +
		// inRequest.getUserName();

		// storeAdminCatalogTree
		Store store = getStore(inRequest);
		String treeid = inRequest.getRequestParameter("treeid");
		if (treeid == null) {
			treeid = "storeAdminCatalogTree_" + store.getCatalogId() + "_"
					+ inRequest.getUserName();
		}

		inRequest.removeSessionValue(treeid);

	}

	public void saveCatalog(WebPageRequest inContext) throws OpenEditException {
		String id = inContext.getRequestParameter("id");
		String name = inContext.getRequestParameter("name");

		StoreEditor editor = getStoreEditor(inContext);
		Category currentCatalog = editor.getCurrentCategory();

		String copy = inContext.getRequestParameter("saveasnew");
		if (currentCatalog != null && Boolean.parseBoolean(copy)) {
			currentCatalog = new Category(currentCatalog.getId() + "copy",
					currentCatalog.getName());
			editor.getCurrentCategory().getParentCatalog()
					.addChild(currentCatalog);
		} else if (!id.equals(currentCatalog.getId())) {
			editor.changeCatalogId(currentCatalog, id);
		}

		currentCatalog.setShortDescription(inContext
				.getRequestParameter("shortdescription"));
		currentCatalog.setName(name);

		String sortfield = inContext.getRequestParameter("sortfield");
		if (sortfield == null || sortfield.length() < 1) {
			currentCatalog.removeProperty("sortfield");
		} else {
			currentCatalog.setProperty("sortfield", sortfield);
		}
		// currentCatalog.setProperty( "quickship",
		// inContext.getRequestParameter( "quickship" ) != null );
		/*
		 * final String[] CATALOG_PROPERTIES = { "otherdownload" }; for (int n =
		 * 0; n < CATALOG_PROPERTIES.length; n++) { String propValue =
		 * inContext.getRequestParameter( CATALOG_PROPERTIES[n] ); if (
		 * propValue != null && propValue.length() == 0 ) { propValue = null; }
		 * currentCatalog.setProperty( CATALOG_PROPERTIES[n], propValue ); }
		 */
		editor.saveCatalog(currentCatalog);
	}

	public void selectProduct(WebPageRequest inContext)
			throws OpenEditException {
		StoreEditor editor = getStoreEditor(inContext);
		Object productPageValue = inContext.getPageValue("product");
		Product product = null;
		if (productPageValue != null && productPageValue instanceof Product)
		// it may be a CompositeProduct, which would throw an exception
		{
			product = (Product) productPageValue;
		}

		if (product == null) {
			String id = inContext.getRequestParameter("productid");
			if(id == null){
				id = inContext.getRequestParameter("id");
			}
			
			if (id == null) {
				id = PathUtilities.extractPageName(inContext.getContentPage()
						.getPath());
			}
			if (id != null) {
				product = editor.getProduct(id);
			}
		}
		if (product == null) {
			return;
		}
		if (product != editor.getCurrentProduct()) {
			editor.setCurrentItem(null);
		}
		editor.setCurrentProduct(product);
		inContext.putPageValue("product", product);

		String cid = inContext.getRequestParameter(CATEGORYID);
		if (cid != null) {
			Category cat = editor.getCatalog(cid);
			editor.setCurrentCatalog(cat);
		}

	}

	/**
	 * @deprecated Replaced by BSH script /store/admin/products/createnew.bsh
	 */
	public Product createProduct(WebPageRequest inContext)
			throws OpenEditException {
		StoreEditor editor = getStoreEditor(inContext);
		String existingproductid = inContext
				.getRequestParameter("existingproductid");
		Product product = null;
		if (existingproductid != null && existingproductid.length() > 0) {
			product = editor.getProduct(existingproductid);
		}
		if (product == null) {
			product = editor.createProductWithDefaults();
			if (existingproductid != null && existingproductid.length() > 0) {
				product.setId(existingproductid);
			}
		}
		if (editor.getCurrentCategory() != null) {
			product.addCatalog(editor.getCurrentCategory());
		}
		String sourcepath = inContext.findValue("sourcepath");
		if (sourcepath != null && sourcepath.endsWith("/")) {
			boolean createAsFolder = Boolean.parseBoolean(inContext
					.findValue("createasfolder"));
			if (createAsFolder) {
				sourcepath = sourcepath + product.getId() + "/";
				String folderpath = "/" + editor.getStore().getCatalogId()
						+ "/data/assets/" + sourcepath;
				Page folder = getPageManager().getPage(folderpath);
				getPageManager().putPage(folder);
			} else {
				sourcepath = sourcepath + product.getId() + ".data";
			}
		}

		product.setSourcePath(sourcepath);
		String xconfpath = editor.getStore().getProductArchive()
				.buildXconfPath(product);
		Page sourcePage = getPageManager().getPage(xconfpath);
		product.setSourcePage(sourcePage);

		if (product.getSourcePath() != null
				&& product.getSourcePath().startsWith("newproducts/")) {
			Category pcat = editor.getStore().getCategory("newproducts");
			if (pcat == null) {
				pcat = new Category("newproducts", "Pending");
				editor.getStore().getCategoryArchive().addChild(pcat);
			}
			Category cat = editor.getStore().getCategory(
					"newproducts_" + inContext.getUserName());
			if (cat == null) {
				cat = new Category();
				cat.setId("newproducts_" + inContext.getUserName());
				cat.setName(inContext.getUser().getShortDescription());
				pcat.addChild(cat);
				editor.getStore().getCategoryArchive().saveCategory(cat);
			}
			product.addCatalog(cat);
		}

		editor.saveProduct(product);
		editor.setCurrentProduct(product);
		inContext.putPageValue("product", product);
		inContext.setRequestParameter("productid", product.getId());
		inContext.setRequestParameter("sourcepath", product.getSourcePath());
		saveProductProperties(inContext);
		String sendto = inContext.findValue("sendtoeditor");

		if (Boolean.parseBoolean(sendto)) {
			inContext.redirect("/" + editor.getStore().getCatalogId()
					+ "/admin/products/editor/" + product.getId() + ".html");
		}

		String tosourcepath = inContext.findValue("redirecttosourcepath");

		if (Boolean.parseBoolean(tosourcepath)) {
			String path = "/" + editor.getStore().getCatalogId() + "/products/"
					+ product.getSourcePath();
			if (path.endsWith("/")) {
				path = path + "index.html";
			} else {
				path = path + ".html";
			}
			inContext.redirect(path);
		}

		return product;
	}

	

	public void removeProductsFromCatalog(WebPageRequest inContext)
			throws OpenEditException {
		StoreEditor editor = getStoreEditor(inContext);
		String catalogId = inContext.getRequestParameter("selectedcatalog");
		if (catalogId == null) {
			catalogId = editor.getCurrentCategory().getId();
		}
		String[] productIds = inContext.getRequestParameters("productid");
		Category cat = editor.getCatalog(catalogId);
		editor.removeProductFromCatalog(cat, productIds);
	}

	public void saveProductResultsEdits(WebPageRequest inRequest)
			throws OpenEditException {
		Store store = getStore(inRequest);
		String[] fields = inRequest.getRequestParameters("editfield");
		if (fields != null) {
			for (int i = 0; i < fields.length; i++) {
				String key = fields[i];
				String productid = key.substring(0, key.indexOf("."));
				String fieldid = key.substring(key.indexOf(".") + 1);
				String value = inRequest.getRequestParameter(key);
				Product product = store.getProduct(productid);
				if (product == null) {
					throw new OpenEditException("Product is not found " + key);
				}
				String oldvalue = product.getProperty(key);
				product.setProperty(fieldid, value);
				// null check
				if (value != null && !value.equals(oldvalue)) {
					store.getProductSearcher().saveData(product,
							inRequest.getUser());
				} else if (oldvalue != null && !oldvalue.equals(value)) {
					store.getProductSearcher().saveData(product,
							inRequest.getUser());
				}
			}
		}
	}

	public void copyProduct(WebPageRequest inContext) throws OpenEditException {
		Product product = getProduct(inContext);

		String originalsourcepath = product.getSourcePath();
		StoreEditor editor = getStoreEditor(inContext);
		Store store = editor.getStore();

		String targetName = inContext.getRequestParameter("name");
		String newSourcePath;
		String sourceDirectory = inContext.findValue("defaultsourcedirectory");
		if (sourceDirectory == null) {
			sourceDirectory = PathUtilities
					.extractDirectoryPath(originalsourcepath);
			if (originalsourcepath.endsWith("/")) {
				sourceDirectory = PathUtilities
						.extractDirectoryPath(sourceDirectory);
			}
		}
		if (sourceDirectory.endsWith("/")) {
			sourceDirectory = sourceDirectory.substring(0,
					sourceDirectory.length() - 2);
		}
		String newId = editor.getStore().getProductArchive()
				.nextProductNumber();

		if (targetName != null) // Is this really used? Seems wrong somehow...
		{
			newSourcePath = sourceDirectory + targetName + "/";
		} else {
			boolean createAsFolder = Boolean.parseBoolean(inContext
					.findValue("createasfolder"));
			if (createAsFolder) {
				newSourcePath = sourceDirectory + "/" + newId + "/";
			} else {
				newSourcePath = sourceDirectory + "/" + newId + ".data";
			}
		}
		if (newSourcePath.startsWith("/")) {
			newSourcePath = newSourcePath.substring(1);
		}

		if (newSourcePath.equals(originalsourcepath)) {
			return; // can't copy to itself
		}

		Product newproduct = editor.copyProduct(product, newId, newSourcePath);

		// Copy any images or folders using OE File Manager
		String newpath = "/" + editor.getStore().getCatalogId()
				+ "/data/assets/" + newSourcePath;
		String oldpath = "/" + editor.getStore().getCatalogId()
				+ "/data/assets/" + originalsourcepath;

		Page newpage = getPageManager().getPage(newpath);
		Page oldpage = getPageManager().getPage(oldpath);

		// Check for flag indicating that the image should not be copied
		boolean copyimage = Boolean.parseBoolean(inContext
				.findValue("copyimage"));
		if (!copyimage) {
			// Remove the image reference from the xconf
			newproduct.removeProperties(new String[] { "primaryimagename",
					"fileformat" });
			// create a blank directory
			getPageManager().putPage(newpage);
		} else {
			// copy the original assets directory (including the image)
			getPageManager().copyPage(oldpage, newpage);
		}

		newproduct.setName(targetName);

		Collection categories = product.getCategories();
		for (Iterator iter = categories.iterator(); iter.hasNext();) {
			Category element = (Category) iter.next();
			newproduct.addCategory(element);
		}

		Page oldPage = getPageManager().getPage(
				editor.getStore().getStoreHome() + "/products/"
						+ product.getSourcePath() + ".html");
		if (oldPage.exists()) {
			Page newPage = getPageManager().getPage(
					editor.getStore().getStoreHome() + "/products/"
							+ newproduct.getSourcePath() + ".html");
			try {
				getPageManager().copyPage(oldPage, newPage);
			} catch (RepositoryException re) {
				throw new OpenEditException(re);
			}
		}

		// Remove the PDF text
		newproduct.removeProperty("fulltext");
		editor.setCurrentProduct(newproduct);
		if (inContext.getRequestParameters("field") != null) {
			saveProductProperties(inContext);
		} else {
			editor.getStore().saveProduct(newproduct, inContext.getUser());
		}
		inContext.setRequestParameter("targetsourcepath",
				newproduct.getSourcePath());
		inContext.setRequestParameter("newproductid", newproduct.getId());
		copyJoinData(product, newproduct);
	}

	protected void copyJoinData(Product source, Product target) {
		PropertyDetails properties = getStore(source.getCatalogId())
				.getFieldArchive().getPropertyDetails("product");
		List lists = properties.getDetailsByProperty("type", "textjoin");
		lists.addAll(properties.getDetailsByProperty("type", "datejoin"));
		HashSet processed = new HashSet();
		for (Iterator iterator = lists.iterator(); iterator.hasNext();) {
			PropertyDetail detail = (PropertyDetail) iterator.next();
			String detailid = detail.getId();
			if (detailid.indexOf(".") > 0) {
				detailid = detailid.split("\\.")[0];
			}
			if (processed.contains(detailid)) {
				continue;
			} else {
				processed.add(detailid);
			}

			FilteredTracker tracker = new FilteredTracker();
			tracker.setSearcher(getSearcherManager().getSearcher(
					detail.getCatalogId(), detailid));
			tracker.filter("productid", source.getId());
			HitTracker hits = tracker.filtered();

			Searcher targetSearcher = getSearcherManager().getSearcher(
					target.getCatalogId(), detailid);
			if (targetSearcher != null && hits != null && hits.size() > 0) {
				List data = new ArrayList();
				for (Iterator iterator2 = hits.iterator(); iterator2.hasNext();) {
					ElementData item = (ElementData) iterator2.next();
					Data newItem = targetSearcher.createNewData();
					for (Iterator iterator3 = item.getElement().attributes()
							.iterator(); iterator3.hasNext();) {
						Attribute property = (Attribute) iterator3.next();
						if (property.getName().equals("productid")) {
							newItem.setProperty("productid", target.getId());
						} else if (!property.getName().equals("id")) {
							newItem.setProperty(property.getName(),
									property.getValue());
						}
					}
					data.add(newItem);
				}
				targetSearcher.saveAllData(data, null);
			}
		}
	}

	public void saveProduct(WebPageRequest inContext) throws OpenEditException {
		String saveAsNew = inContext.getRequestParameter("saveasnew");
		StoreEditor editor = getStoreEditor(inContext);
		Product product = editor.getCurrentProduct();

		String newId = inContext.getRequestParameter("newproductid");
		// was id changed?
		if (newId != null && !newId.equals(product.getId())) {
			Product newproduct = editor.copyProduct(product, newId);
			Collection catalogs = product.getCategories();
			for (Iterator iter = catalogs.iterator(); iter.hasNext();) {
				Category element = (Category) iter.next();
				newproduct.addCatalog(element);
			}
			if (saveAsNew == null || saveAsNew.equalsIgnoreCase("false")) {
				Page oldPage = getPageManager().getPage(
						editor.getStore().getStoreHome() + "/products/"
								+ product.getId() + ".html");
				if (oldPage.exists()) {
					Page newPage = getPageManager().getPage(
							editor.getStore().getStoreHome() + "/products/"
									+ newproduct.getId() + ".html");
					try {
						getPageManager().movePage(oldPage, newPage);
					} catch (RepositoryException re) {
						throw new OpenEditException(re);
					}
				}

				editor.deleteProduct(product); // changing product id, and erase
				// the old id
				// editor.getStore().reindexAll();
			} else {
				Page oldPage = getPageManager().getPage(
						editor.getStore().getStoreHome() + "/products/"
								+ product.getId() + ".html");
				if (oldPage.exists()) {
					Page newPage = getPageManager().getPage(
							editor.getStore().getStoreHome() + "/products/"
									+ newproduct.getId() + ".html");
					try {
						getPageManager().copyPage(oldPage, newPage);
					} catch (RepositoryException re) {
						throw new OpenEditException(re);
					}
				}
			}
			product = newproduct;
		}

		product.setName(inContext.getRequestParameter("name"));
		// product.setDescription( inContext.getRequestParameter( "description"
		// ) );

		editor.saveProduct(product);
		product = editor.getProduct(product.getId());
		editor.setCurrentProduct(product);

		inContext.putPageValue("product", product);
		inContext.setRequestParameter("productid", product.getId());
	}

	public void createProductFromTemplate(WebPageRequest inContext)
			throws OpenEditException {
		if (inContext.getUser() == null) {
			throw new OpenEditException(
					"User must login before creating product from template");
		}
		StoreEditor editor = getStoreEditor(inContext);
		Store store = getStore(inContext);
		String templateId = inContext.findValue("templateId");
		String newId = store.getProductArchive().nextProductNumber();
		Product template = store.getProduct(templateId);
		Product product = editor.copyProduct(template, newId);
		Collection catalogs = template.getCategories();
		for (Iterator iter = catalogs.iterator(); iter.hasNext();) {
			Category element = (Category) iter.next();
			product.addCatalog(element);
		}
		Page oldPage = getPageManager().getPage(
				editor.getStore().getStoreHome() + "/products/"
						+ template.getId() + ".html");
		if (oldPage.exists()) {
			Page newPage = getPageManager().getPage(
					editor.getStore().getStoreHome() + "/products/"
							+ product.getId() + ".html");
			try {
				getPageManager().movePage(oldPage, newPage);
			} catch (RepositoryException re) {
				throw new OpenEditException(re);
			}
		}
		List inventoryItems = product.getInventoryItems();
		int count = 1;
		for (Iterator iterator = inventoryItems.iterator(); iterator.hasNext();) {
			InventoryItem item = (InventoryItem) iterator.next();
			item.setSku(product.getId() + "-" + count);
		}

		product.setAvailable(template.isAvailable());
		product.setName(template.getName());
		product.setProperty("user", inContext.getUser().getUserName());
		product.setDescription(template.getDescription());
		product.setHandlingChargeLevel(template.getHandlingChargeLevel());
		String shipping = inContext.getRequestParameter("shippingmethod");
		product.setShippingMethodId(template.getShippingMethodId());
		PriceSupport ps = createPriceSupport(inContext);
		product.setPriceSupport(ps);
		editor.setCurrentProduct(product);
		editor.saveProduct(product);
		editor.getStore().reindexAll();
		inContext.putPageValue("product", product);
		inContext.setRequestParameter("productid", product.getId());
	}

	public void loadProperties(WebPageRequest inContext)
			throws OpenEditException {
		String type = inContext.findValue("type");
		StoreEditor editor = getStoreEditor(inContext);
		Map properties = null;
		if (type.equals("product")) {
			Product product = editor.getCurrentProduct();
			properties = ListOrderedMap.decorate(new HashMap());
			properties.putAll(product.getProperties());
			List details = editor.getStore().getProductArchive()
					.getPropertyDetails().getDetails();
			for (Iterator iterator = details.iterator(); iterator.hasNext();) {
				PropertyDetail detail = (PropertyDetail) iterator.next();
				if (!properties.containsKey(detail.getId())) {
					String val = editor.getCurrentProduct().getProperty(
							detail.getId());
					if (val == null) {
						val = "";
					}
					properties.put(detail.getId(), val);
				}
			}
		} else if (type.equals("catalog")) {
			Category catalog = editor.getCurrentCategory();
			properties = catalog.getProperties();
		} else if (type.equals("item")) {
			InventoryItem item = editor.getCurrentItem();
			properties = item.getProperties();
		}
		inContext.putPageValue("properties", properties);
		inContext.putPageValue("type", type);
	}

	public void loadOptions(WebPageRequest inContext) throws OpenEditException {
		String type = inContext.getRequestParameter("type");
		StoreEditor editor = getStoreEditor(inContext);
		List options = null;
		if (type.equals("product")) {
			Product product = editor.getCurrentProduct();
			options = product.getOptions();
		} else if (type.equals("catalog")) {
			Category catalog = editor.getCurrentCategory();
			options = catalog.getOptions();
		} else if (type.equals("item")) {
			InventoryItem item = editor.getCurrentItem();
			// TODO:Fix item options.
			// options = item.getOptions();
		}
		inContext.putPageValue("options", options);
		inContext.putPageValue("type", type);
	}

	public void addProductKeyword(WebPageRequest inReq)
			throws OpenEditException {
		StoreEditor editor = getStoreEditor(inReq);
		String key = inReq.getRequestParameter("keyword");
		if (key == null) {
			return;
		}
		Product product;
		String id = inReq.getRequestParameter("productid");
		if (id == null) {
			product = editor.getCurrentProduct();
		} else {
			product = getStore(inReq).getProduct(id);
		}
		product.addKeyword(key);
		editor.saveProduct(product);
	}

	public void removeProductKeyword(WebPageRequest inReq)
			throws OpenEditException {
		StoreEditor editor = getStoreEditor(inReq);
		String key = inReq.getRequestParameter("keyword");
		if (key == null) {
			return;
		}
		Product product;
		String id = inReq.getRequestParameter("productid");
		if (id == null) {
			product = editor.getCurrentProduct();
		
		} else {
			product = getStore(inReq).getProduct(id);
		}
		product.removeKeyword(key);
		editor.saveProduct(product);
	}

	public void saveProductProperties(WebPageRequest inReq)
			throws OpenEditException {
		StoreEditor editor = getStoreEditor(inReq);
		String[] fields = inReq.getRequestParameters("field");
		if (fields == null) {
			return;
		}
		String productid = inReq.getRequestParameter("productid");
		// <input type="hidden" name="productid" value="$product.id"/>

		Product product = editor.getCurrentProduct();
		StringBuffer changes = new StringBuffer();

		if (product == null) {
			product = editor.getProduct(productid);
		}
		for (int i = 0; i < fields.length; i++) {
			String field = fields[i];
			String value = inReq.getRequestParameter(field + ".value");
			String oldval = product.getProperty(field);

			if (value != null) {
				product.setProperty(field, value);
			} else {
				product.removeProperty(field);
			}

			if (value != null || oldval != null) {
				PropertyDetail detail = getStore(inReq).getProductArchive()
						.getPropertyDetails().getDetail(field);
				if (detail.isViewType("list")) {
					Searcher listSearcher = getStore(inReq)
							.getSearcherManager().getSearcher(
									getStore(inReq).getCatalogId(), field);
					Data data = (Data) listSearcher.searchById(oldval);
					if (data != null) {
						oldval = data.getName();
					}
					data = (Data) listSearcher.searchById(value);
					if (data != null) {
						value = data.getName();
					}

				} else if (detail.isDataType("date")) {
					// LuceneHitTracker ht = new LuceneHitTracker();
					// oldval = ht.toDate(oldval, detail.getDateFormatString());
					// value = ht.toDate(value, detail.getDateFormatString());
				}

				if (oldval == null) {
					oldval = "No Data";
				}
				if (value == null) {
					value = "No Data";
				}
				if (!oldval.equals(value)) {
					if (changes.length() > 0) {
						changes.append(", ");
					}
					changes.append(field + ": \"" + oldval + "\" -> \"" + value
							+ "\"");
				}

			}
		}
		editor.getStore().saveProduct(product, inReq.getUser());
		if (fieldWebEventListener != null && changes.length() > 0) {
			WebEvent event = new WebEvent();
			event.setCatalogId(editor.getStore().getCatalogId());

			event.setSearchType("productedit");
			event.setSource(this);
			event.addDetail("productid", product.getId());
			event.addDetail("productname", product.getName());
			event.addDetail("changes", changes.toString());
			event.setUsername(inReq.getUserName());
			getWebEventListener().eventFired(event);
		}
	}

	public void saveCategoryProperties(WebPageRequest inReq)
			throws OpenEditException {
		StoreEditor editor = getStoreEditor(inReq);
		String[] fields = inReq.getRequestParameters("field");
		String catid = inReq.getRequestParameter("categoryid");
		if (fields == null || catid == null) {
			return;
		}
		Category cat = editor.getStore().getCatalog(catid);
		for (int i = 0; i < fields.length; i++) {
			String field = fields[i];
			String value = inReq.getRequestParameter(field + ".value");
			if (value != null) {
				cat.setProperty(field, value);
			} else {
				cat.removeProperty(field);
			}
		}
		editor.getStore().getCategoryArchive().saveCategory(cat);
	}

	public void addProductsToCatalog(WebPageRequest inContext)
			throws OpenEditException {
		String prefix = inContext.getRequestParameter("prefix");
		String suffix = inContext.getRequestParameter("suffix");
		String catalogid = inContext.getRequestParameter("catalogs2");
		String[] productid = inContext.getRequestParameters("productid");

		StoreEditor editor = getStoreEditor(inContext);
		Category catalog = editor.getCatalog(catalogid);

		editor.addProductsToCatalog(productid, prefix, suffix, catalog);
	}

	public void moveProductsToCatalog(WebPageRequest inContext)
			throws OpenEditException {
		String catalog1id = inContext.getRequestParameter("catalogs1");
		String catalog2id = inContext.getRequestParameter("catalogs2");
		String[] productid = inContext.getRequestParameters("productid");
		StoreEditor editor = getStoreEditor(inContext);
		Category catalog1 = editor.getCatalog(catalog1id);
		Category catalog2 = editor.getCatalog(catalog2id);

		editor.moveProductsToCatalog(productid, catalog1, catalog2);
	}

	public void loadCategory(WebPageRequest inContext) throws OpenEditException {
		String catalogid = inContext.findValue(CATEGORYID);
		if (catalogid == null) {
			catalogid = PathUtilities.extractPageName(inContext.getPath());
		}
		StoreEditor editor = getStoreEditor(inContext);
		if (catalogid != null) {
			// load up catalog and products
			Category catalog = editor.getCatalog(catalogid);
			if (catalog != null) {
				editor.setCurrentCategory(catalog);
				inContext.putPageValue("category", catalog);
			}
		}
	}

	/**
	 * Use getCategory
	 * 
	 * @deprecated
	 * @param inCatalogId
	 * @return
	 * @throws StoreException
	 */
	public void loadCatalog(WebPageRequest inContext) throws OpenEditException {
		loadCategory(inContext);
	}

	/**
	 * Set the "picked catalog" in the store editor based on the
	 * <code>catalogid</code> request parameter that is passed in.
	 * 
	 * @param inContext
	 *            The web request
	 * 
	 * @throws OpenEditException
	 */
	public void pickCatalog(WebPageRequest inContext) throws OpenEditException {
		String catalogid = inContext.getRequestParameter(CATEGORYID);
		StoreEditor editor = getStoreEditor(inContext);
		if (catalogid != null) {
			Category catalog = editor.getCatalog(catalogid);
			if (catalog != null) {
				editor.setPickedCatalog(catalog);
			}
		}
	}

	public StoreEditor getStoreEditor(WebPageRequest inContext)
			throws OpenEditException {
		Store store = getStore(inContext);
		StoreEditor editor = (StoreEditor) inContext
				.getSessionValue("storeeditor" + store.getCatalogId());
		if (editor == null) {
			editor = new StoreEditor(); // TODO: Use Spring to create one
			editor.setPageManager(getPageManager());
			editor.setStore(store);
			inContext.putSessionValue("storeeditor" + store.getCatalogId(),
					editor);
		}
		// CatalogToHtml cataloghtml = new CatalogToHtml();
		// cataloghtml.setRootCatalog(editor.getRootCatalog());
		inContext.putPageValue("storeeditor", editor);

		return editor;
	}

	public void selectItem(WebPageRequest inContext) throws OpenEditException {
		StoreEditor storeEditor = getStoreEditor(inContext);
		Product product = storeEditor.getCurrentProduct();
		String sku = inContext.getRequestParameter("sku");
		if (product!=null && sku != null && sku.length() > 0) {
			StoreEditor editor = getStoreEditor(inContext);
			InventoryItem item = product.getInventoryItemBySku(sku);
			if (item == null) {
				storeEditor.getStore().getProductArchive().clearProducts();
				inContext.putPageValue(ERROR,
						"Could not find sku in product. Reloaded products "
								+ sku);
				return;
			}
			editor.setCurrentItem(item);
			inContext.putPageValue("item", editor.getCurrentItem());
		} else {
			inContext.putPageValue(ERROR, "Could not find sku in product. "
					+ sku);
		}
	}

	public void createItem(WebPageRequest inContext) throws OpenEditException {
		StoreEditor editor = getStoreEditor(inContext);
		Product product = editor.getCurrentProduct();
		String sku = inContext.getRequestParameter("newsku");
		if (product==null || sku == null || sku.length() == 0) {
			return;
		}
		InventoryItem old = product.getInventoryItemBySku(sku);
		if (old != null) {
			editor.setCurrentItem(old);
			inContext.putPageValue("item", old);
			return;
		}
		InventoryItem item = editor.createItem();
		item.setSku(sku);
		item.setQuantityInStock(1);
		log.info("Added item to product " + item + " to " + product);
		product.addInventoryItem(item);
		editor.saveProduct(product);
		editor.setCurrentItem(product.getInventoryItemBySku(sku));
	}

	public void deleteItem(WebPageRequest inContext) throws OpenEditException {
		StoreEditor editor = getStoreEditor(inContext);
		Product product = editor.getCurrentProduct();
		if (product == null) {
			return;
		}
		String sku = inContext.getRequestParameter("sku");
		InventoryItem item = product.getInventoryItemBySku(sku);
		if (item != null) {
			if (editor.getCurrentItem() != null
					&& item.getSku().equals(editor.getCurrentItem().getSku())) {
				editor.setCurrentItem(null);
			}
			editor.deleteItem(item);
		}
	}

	public void saveItem(WebPageRequest inContext) throws OpenEditException {
		StoreEditor editor = getStoreEditor(inContext);

		InventoryItem item = editor.getCurrentItem();
		if (item != null) {
			item.setDescription(inContext.getRequestParameter("description"));
			String val = String.valueOf(inContext
					.getRequestParameter("quickship"));
			item.addProperty("quickship", val);

			/*
			 * final String[] ITEM_PROPERTIES = { "width", "depth", "height",
			 * "seatcap", "basesreq", "list", "com", "col", "a", "b", "c", "d",
			 * "e", "f", "g", "l" };
			 */
			List itemProperties = editor.getItemProperties();
			for (Iterator iter = itemProperties.iterator(); iter.hasNext();) {
				PropertyDetail config = (PropertyDetail) iter.next();
				String propString = config.getId();
				String propValue = inContext.getRequestParameter(propString);
				if (propValue != null && propValue.length() == 0) {
					propValue = null;
				}
				item.addProperty(propString, propValue);

			}

			String qinstock = inContext.getRequestParameter("qinstock");
			if (qinstock != null) {
				int q = Integer.parseInt(qinstock);
				item.setQuantityInStock(q);
			}

			// TODO: Move to generic function
			PriceSupport ps = createPriceSupport(inContext);
			item.setPriceSupport(ps);

			// Legacy
			String color = inContext.getRequestParameter("color");
			if (color != null) {
				item.setColor(color);
			}
			String size = inContext.getRequestParameter("size");
			if (size != null) {
				item.setSize(size);
			}
			String weight = inContext.getRequestParameter("weight");
			if (weight != null) {
				item.setWeight(Double.parseDouble(weight));
			}
			item.getOptions().clear();
			for (Iterator iter = inContext.getParameterMap().keySet()
					.iterator(); iter.hasNext();) {
				String id = (String) iter.next();
				if (id.startsWith("optionid")) {
					String oid = id.substring("optionid".length());
					Option newOption = new Option();
					String value = inContext.getRequestParameter("optionvalue"
							+ oid);
					Option old = item.getOption(oid);
					if (old != null) {
						if (value == null || value.equals(old.getValue())) {
							continue; // Duplicate of product option
						}
						newOption.setPriceSupport(old.getPriceSupport());
						newOption.setDataType(old.getDataType());
						newOption.setRequired(old.isRequired());
						newOption.setName(old.getName());
						newOption.setValue(old.getValue());
					}
					newOption.setId(oid);
					newOption.setValue(value);
					item.addOption(newOption);
				}
			}
			editor.saveProduct(item.getProduct());
		}
	}

	public void saveProductPrices(WebPageRequest inContext)
			throws OpenEditException {
		PriceSupport ps = createPriceSupport(inContext);

		StoreEditor editor = getStoreEditor(inContext);
		Product product = editor.getCurrentProduct();
		product.setPriceSupport(ps);
		product.setHandlingChargeLevel(inContext
				.getRequestParameter("handlingchargelevel"));
		product.setAvailable(Boolean.parseBoolean(inContext
				.getRequestParameter("available")));
		String shipping = inContext.getRequestParameter("shippingmethod");
		product.setShippingMethodId(shipping);

		String sortnum = inContext.getRequestParameter("sortnum");
		if (sortnum != null) {
			product.setOrdering(Integer.parseInt(sortnum));
		}
		String taxexemptamount = inContext.getRequestParameter("taxexemptamount");
		if (taxexemptamount!=null){
			try{
				taxexemptamount = String.format("%.2f", new Double(Double.parseDouble(taxexemptamount)));
				product.setProperty("taxexemptamount", taxexemptamount);
			}catch (Exception e){
				log.info("error saving taxexemptamount, skipping update; val="+taxexemptamount+", message="+e.getMessage());
			}
		}
		editor.saveProduct(product);
	}

	private PriceSupport createPriceSupport(WebPageRequest inContext) {
		PriceSupport ps = null;
		String saleprice = inContext.getRequestParameter("saleprice");
		String retailprice = inContext.getRequestParameter("retailprice");
		String wholesaleprice = inContext.getRequestParameter("wholesaleprice");
		if (saleprice != null || retailprice != null || wholesaleprice != null) {
			ps = new PriceSupport();
			Price point = new Price();
			if (saleprice != null) {
				Money money = new Money(saleprice);
				point.setSalePrice(money);
			}
			if (retailprice != null) {
				Money money = new Money(retailprice);
				point.setRetailPrice(money);
			}
			if (wholesaleprice != null) {
				Money money = new Money(wholesaleprice);
				point.setWholesalePrice(money);
			}
			ps.addTierPrice(1, point);
		}
		return ps;
	}

	// public void importImages(WebPageRequest inContext) throws
	// OpenEditException
	// {
	// StoreEditor editor = getStoreEditor(inContext);
	// String catalogId = inContext.getRequestParameter(CATEGORYID);
	// Category catalog = editor.getCatalog(catalogId);
	// if (catalog == null)
	// {
	// return;
	// }
	// String directoryName = inContext.getRequestParameter("directory");
	// int maxWidth =
	// Integer.parseInt(inContext.getRequestParameter("maxWidth"));
	// ConvertInstructions inStructions = new ConvertInstructions();
	// ImageResizer resizer = new ImageResizer();
	// inStructions.setMaxScaledSize(new Dimension(maxWidth,
	// Integer.MAX_VALUE));
	// File inDirectory = new File(directoryName);
	// // TODO: set the output directory to whatever...
	// File outDirectory = new File(editor.getStore().getStoreDirectory(),
	// "images" + File.separatorChar + "thumb");
	// try
	// {
	// resizer.resizeImagesInDirectory(inDirectory, outDirectory, inStructions,
	// true);
	// }
	// catch (Exception ioe)
	// {
	// throw new OpenEditException(ioe);
	// }
	// }

	/*
	 * public void createCatalogLinks( WebPageRequest inContext ) throws
	 * OpenEditException { StoreEditor editor = getStoreEditor( inContext );
	 * LinkTree links = (LinkTree)inContext.getPageValue(LINKTREE); String id =
	 * inContext.getRequestParameter("linkid");
	 * 
	 * if ( id == null && links.getRootLink() != null) { id =
	 * links.getRootLink().getId(); } Link parentLink = links.getLink(id);
	 * editor.addCatalogAsLink(editor.getCurrentCatalog(),links, parentLink,
	 * false, null);
	 * getModuleManager().execute("LinkTree.saveAllLinks",inContext); } public
	 * void createCatalogLinksWithProducts( WebPageRequest inContext ) throws
	 * OpenEditException { StoreEditor editor = getStoreEditor( inContext );
	 * LinkTree links = (LinkTree)inContext.getPageValue(LINKTREE); String id =
	 * inContext.getRequestParameter("linkid");
	 * 
	 * if ( id == null && links.getRootLink() != null) { id =
	 * links.getRootLink().getId(); } Link parentLink = links.getLink(id);
	 * String[] args = inContext.getRequestParameters("productid");
	 * editor.addCatalogAsLink(editor.getCurrentCatalog(),links, parentLink,
	 * true, args);
	 * 
	 * getModuleManager().execute("LinkTree.saveAllLinks",inContext); }
	 */

	/*
	 * public void loadOptions(WebPageRequest inReq) throws OpenEditException {
	 * StoreEditor editor = getStoreEditor( inReq );
	 * inReq.putPageValue("options",editor.getAllOptions()); }
	 */
	/*
	 * public void saveOptionsToCatalog(WebPageRequest inReq) throws
	 * OpenEditException { StoreEditor editor = getStoreEditor( inReq );
	 * Category cat = editor.getCurrentCatalog(); cat.clearAvailableOptions();
	 * //this includes notes?
	 * 
	 * //loop over all known options List all = editor.getAllOptions(); for
	 * (Iterator iter = all.iterator(); iter.hasNext();) { Option element =
	 * (Option) iter.next(); String value =
	 * inReq.getRequestParameter(element.getId() ); if ( value != null &&
	 * value.length() > 0) { cat.addAvailableOption(element); } } //grab the
	 * notes
	 * 
	 * editor.saveCatalog(cat);
	 * 
	 * }
	 */
	// If you want to add a property, use saveCatalogProperties
	public void addOption(WebPageRequest inReq) throws OpenEditException {
		StoreEditor editor = getStoreEditor(inReq);
		String name = inReq.getRequestParameter("optionname");
		String id = inReq.getRequestParameter("optionid");
		String type = inReq.getRequestParameter("type");
		String defaultvalue = inReq.getRequestParameter("defaultvalue");
		String price = inReq.getRequestParameter("price");
		String required = inReq.getRequestParameter("required");
		String dataType = inReq.getRequestParameter("dataType");

		Option option = new Option();
		option.setId(id);
		option.setName(name);
		option.setDataType(dataType);
		option.setValue(defaultvalue);
		option.setRequired(Boolean.parseBoolean(required));
		if (price != null) {
			PriceSupport pricesupport = new PriceSupport();
			pricesupport.addTierPrice(1, new Price(new Money(price)));
			option.setPriceSupport(pricesupport);
		}
		if (name != null && name.length() > 0 && id != null && id.length() > 0) {

			if (type.equals("product")) {
				editor.getCurrentProduct().addOption(option);
				editor.saveProduct(editor.getCurrentProduct());
			} else if (type.equals("catalog")) {
				editor.getCurrentCategory().addOption(option);
				editor.saveCatalog(editor.getCurrentCategory());
			} else if (type.equals("item")) {
				// editor.saveItem(editor.getCurrentItem());
				// TODO: Finish Item properties.
			}

		}
	}

	public void saveObjectOptions(WebPageRequest inContext)
			throws OpenEditException {
		String type = inContext.getRequestParameter("type");
		StoreEditor editor = getStoreEditor(inContext);
		loadOptions(inContext);
		List options = (List) inContext.getPageValue("options");
		saveOptions(inContext, options);

		if (type.equals("product")) {
			editor.saveProduct(editor.getCurrentProduct());
		} else if (type.equals("catalog")) {
			editor.saveCatalog(editor.getCurrentCategory());
		} else if (type.equals("item")) {
			// editor.saveItem(editor.getCurrentItem());
			// TODO: Finish Item properties.
		}
	}

	public void saveOptions(WebPageRequest inContext, List inOptions)
			throws OpenEditException {
		String id[] = inContext.getRequestParameters("optionid");
		String name[] = inContext.getRequestParameters("optionname");
		String defaultvalue[] = inContext.getRequestParameters("defaultvalue");
		String price[] = inContext.getRequestParameters("price");
		String required[] = inContext.getRequestParameters("required");
		String type[] = inContext.getRequestParameters("dataType");
		String delete[] = inContext.getRequestParameters("delete");
		inOptions.clear();
		List deletelist = new ArrayList();
		if (delete != null) {
			deletelist = Arrays.asList(delete);
		}
		List requiredlist = new ArrayList();
		if (required != null) {
			requiredlist = Arrays.asList(required);
		}

		for (int i = 0; i < id.length; i++) {
			if (!deletelist.contains(id[i])) {
				if (id[i].length() > 0) {
					Option option = new Option();
					option.setId(id[i]);
					option.setDataType(type[i]);
					option.setName(name[i]);
					option.setValue(defaultvalue[i]);
					if (price[i] != null && price[i].length() > 0) {
						PriceSupport pricesupport = new PriceSupport();
						pricesupport.addTierPrice(1, new Price(new Money(
								price[i])));
						option.setPriceSupport(pricesupport);
					}

					if (requiredlist.contains(id[i])) {
						option.setRequired(true);
					}

					inOptions.add(option);
				}
			}
		}

	}

	/**
	 * @param inString
	 * @return
	 */
	private String makeId(String inString) {
		String id = inString.toLowerCase();
		id = id.replaceAll(" ", "");

		return id;
	}

	public void addResultsToCategory(WebPageRequest inRequest)
			throws OpenEditException {
		String hitsname = inRequest.findValue("hitsname");
		HitTracker hits = (HitTracker) inRequest.getPageValue(hitsname);

		String catalogid = inRequest.getRequestParameter("categoryid");
		Store store = getStore(inRequest);
		if (catalogid == null) {
			return;
		}
		Category cat = store.getCategory(catalogid);
		if (cat != null) {
			for (Iterator iter = hits.iterator(); iter.hasNext();) {
				String path = hits.getValue(iter.next(), "sourcepath");
				Product product = store.getProductArchive()
						.getProductBySourcePath(path);
				product.addCategory(cat);
				// Could put this in a list for speed
				store.getProductSearcher().saveData(product,
						inRequest.getUser());
			}
		}
	}

	public void removeCategoryFromResults(WebPageRequest inRequest)
			throws OpenEditException {
		String hitsname = inRequest.findValue("hitsname");
		HitTracker hits = (HitTracker) inRequest.getPageValue(hitsname);

		String catalogid = inRequest.getRequestParameter("categoryid");
		Store store = getStore(inRequest);
		if (catalogid == null) {
			return;
		}
		Category cat = store.getCategory(catalogid);
		if (cat != null) {
			for (Iterator iter = hits.iterator(); iter.hasNext();) {
				String path = hits.getValue(iter.next(), "sourcepath");
				Product product = store.getProductArchive()
						.getProductBySourcePath(path);
				product.removeCategory(cat);
				// Could put this in a list for speed
				store.getProductSearcher().saveData(product,
						inRequest.getUser());
			}
		}
	}

	public void removeCategoryFromProduct(WebPageRequest inPageRequest)
			throws Exception {
		String productid = inPageRequest.getRequestParameter("productid");
		String add = inPageRequest.getRequestParameter("categoryid");
		Store store = getStore(inPageRequest);
		Category c = store.getCategory(add);
		if (c == null) {
			return;
		}

		
			Product product = store.getProduct(productid);
			product.removeCategory(c);
			store.saveProduct(product, inPageRequest.getUser());
		
	}

	public void addCategoryToProduct(WebPageRequest inPageRequest)
			throws Exception {
		String productid = inPageRequest.getRequestParameter("productid");
		String add = inPageRequest.getRequestParameter("categoryid");
		Store store = getStore(inPageRequest);
		Category c = store.getCategory(add);
		if (c == null) {
			return;
		}

		
			Product product = store.getProduct(productid);
			product.addCatalog(c);
			store.saveProduct(product, inPageRequest.getUser());
	
	}

	/**
	 * Removes generated images (medium, thumbs, etc) for a product.
	 * 
	 * @param inRequest
	 *            The web request. Needs a <code>productid</code> or
	 *            <code>sourcePath</code> request parameter.
	 */
	public void removeProductImages(WebPageRequest inRequest) {
		Product product = getProduct(inRequest);
		Store store = getStore(inRequest);

		String catalogId = store.getCatalogId();

		String prefix = "/" + catalogId + "/products/images/generated/"
				+ product.getSourcePath();

		final File path = new File(getPageManager().getRepository()
				.getStub(prefix).getAbsolutePath());
		File[] todelete = null;
		if (path.exists() && path.isDirectory()) {
			todelete = path.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.endsWith(".jpg")
							|| new File(dir, name).isDirectory();
				}
			});
		} else {
			File parent = path.getParentFile();
			todelete = parent.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.startsWith(PathUtilities.extractFileName(path
							.getAbsolutePath()));
				}
			});
		}
		if (todelete != null) {
			for (int i = 0; i < todelete.length; i++) {
				new FileUtils().deleteAll(todelete[i]);
			}
		}

		/*
		 * String files[] = new String[] { "150x150.jpg", "300x400.jpg",
		 * "800x600.jpg" };
		 * 
		 * for (int i = 0; i < files.length; i++) { String path = prefix +
		 * files[i]; Page page = getPageManager().getPage(path);
		 * 
		 * if (page.exists()) { getPageManager().removePage(page); }else{ path =
		 * prefix + "index" + files[i]; page = getPageManager().getPage(path);
		 * if(page.exists()){ getPageManager().removePage(page); } }
		 * 
		 * 
		 * }
		 */
	}



	public WebEventListener getWebEventListener() {
		return fieldWebEventListener;
	}

	public void setWebEventListener(WebEventListener inWebEventListener) {
		fieldWebEventListener = inWebEventListener;
	}

	public void loadStore(WebPageRequest inReq) {
		String catalogid = inReq.getRequestParameter("catalogid");

		if (getPageManager().getRepository().doesExist("/" + catalogid)) {
			StoreArchive storeDataReader = (StoreArchive) getModuleManager()
					.getBean("storeArchive");
			Store store = storeDataReader.getStore(catalogid);
			inReq.putPageValue("store", store);
			inReq.putPageValue("catalogid", catalogid);
			inReq.putPageValue("cataloghome", "/" + catalogid);
		}
	}
}