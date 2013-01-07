
/*
 * Created on Aug 24, 2005
 */

import org.entermedia.upload.FileUpload;
import org.entermedia.upload.FileUploadItem
import org.entermedia.upload.UploadRequest
import org.openedit.data.Searcher
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.store.Product
import org.openedit.store.Store


public void handleSubmission(){
Store store = context.getPageValue("store");
MediaArchive archive = context.getPageValue("mediaarchive");
 
Searcher productsearcher = store.getProductSearcher();
Searcher assetsearcher = archive.getAssetSearcher();

Product product = productsearcher.createNewData();
String [] fields = context.getRequestParameters("field");
productsearcher.updateData(context, fields, product)
productsearcher.saveData(product, context.getUser());

FileUpload command = new FileUpload();
command.setPageManager(archive.getPageManager());
UploadRequest properties = command.parseArguments(context);
if (properties == null) {
	return;
}
if (properties.getFirstItem() == null) {
	return;
}

FileUploadItem item = properties.getFirstItem();


if (item != null) {
				String sourcepath = "productimages/" + product.getSourcePath() ;
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
				context.putPageValue("product", product);
}




}

handleSubmission();



		