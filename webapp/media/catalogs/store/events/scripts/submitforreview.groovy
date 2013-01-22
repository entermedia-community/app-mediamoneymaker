
/*
 * Created on Aug 24, 2005
 */

import org.entermedia.upload.FileUpload
import org.entermedia.upload.FileUploadItem
import org.entermedia.upload.UploadRequest
import org.openedit.data.Searcher
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.money.Money
import org.openedit.store.InventoryItem
import org.openedit.store.Price
import org.openedit.store.PriceSupport
import org.openedit.store.Product
import org.openedit.store.Store

import com.openedit.OpenEditException


public void handleSubmission(){
	
	Store store = context.getPageValue("store");
	MediaArchive archive = context.getPageValue("mediaarchive");
	 
	Searcher productsearcher = store.getProductSearcher();
	Searcher assetsearcher = archive.getAssetSearcher();
	
	Product product = null;
	
	FileUpload command = new FileUpload();
	command.setPageManager(archive.getPageManager());
	UploadRequest properties = command.parseArguments(context);
	
	String productid = context.getRequestParameter("productid");
	
	if (productid != null) {
		product = store.getProduct(productid);
	}
	
	FileUploadItem item = properties.getFirstItem();
	
	if (product == null && item == null) {
		return;
	}
	if (product == null) {
		product = productsearcher.createNewData();
		product.setId(productsearcher.nextId());
		product.setSourcePath(context.getUserProfile().get("distributor") + "/" + product.getId());
	}
	if (item != null && item.getName() != null && item.getName().length() > 0) {
		
		String sourcepath = "productimages/" + product.getSourcePath();
		String path = "/WEB-INF/data/" + archive.getCatalogId() + "/originals/" + sourcepath + "/";
		String filename =item.getName();
		path = path + filename;
		properties.saveFirstFileAs(path, context.getUser());
		Asset asset = archive.getAssetBySourcePath(sourcepath);
		if (asset == null) {
			asset = archive.createAsset(sourcepath);
			 root = archive.getCategoryArchive().createCategoryTree(sourcepath);
			asset.addCategory(root);
		}
		asset.setPrimaryFile(filename);
		asset.setProperty("product", product.getId());
	
		archive.removeGeneratedImages(asset);
		archive.saveAsset(asset, null);
		
		product.setProperty("image", asset.getId());
		productsearcher.saveData(product, context.getUser());
	}
	String [] fields = context.getRequestParameters("field");
	productsearcher.updateData(context, fields, product);
	product.setProperty("submittedby", context.getUserName());
	product.setProperty("distributor", context.getUserProfile().get("distributor"));
	product.setProperty("profileid", context.getUserProfile().getId());
	
	if (product.get("rogersprice") == null) {
		throw new OpenEditException("Cannot create product without pricing information.");
	}
	
	//Clear the items!
	product.clearItems();
	
	//Create the new item
	InventoryItem inventoryItem = new InventoryItem(product.get("manufacturersku"));
	Money money = new Money(product.get("rogersprice"));
	money = money.multiply(1.1);
	Price price = new Price(money);
	PriceSupport pricing = new PriceSupport();
	pricing.addTierPrice(1, price);
	inventoryItem.setPriceSupport(pricing);
	product.addInventoryItem(inventoryItem);
		
	productsearcher.saveData(product, context.getUser());
	context.putPageValue("product", product);
	
}

handleSubmission();



		