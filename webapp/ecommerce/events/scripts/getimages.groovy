
/*
 * Created on Aug 24, 2005
 */

import org.apache.commons.httpclient.util.URIUtil
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.store.Product
import org.openedit.store.Store
import org.openedit.store.convert.ConvertStatus

import com.openedit.hittracker.HitTracker
import com.openedit.modules.update.Downloader



Store store = context.getPageValue("store");
MediaArchive archive = context.getPageValue("mediaarchive");

ConvertStatus result = new ConvertStatus();
convert(archive, store, result);
Log log = LogFactory.getLog(getimages.class);




public void convert(MediaArchive inArchive, Store inStore, ConvertStatus inErrorLog)
throws Exception {


	HitTracker all = inStore.getProductSearcher().getAllHits();
	all.each{
		Product product = inStore.getProduct(it.id);
		downloadImage(inArchive, inStore, product);
	}
	
}


protected void downloadImage(MediaArchive inArchive, Store inStore, Product inProduct) {
	try {
		String sourcepath = "/productimages/" + inProduct.getSourcePath();
		String path = "/WEB-INF/data/"	+ inStore.getCatalogId() + "/originals/" + sourcepath			+ "/original.jpg";
		log.info("1");
		File image = new File(inStore.getRootDirectory(), path);

		if (!image.exists() || image.length() == 0) {
			Downloader dl = new Downloader();
			String imagename = inProduct.getProperty("image");
			imagename = URIUtil.encodeQuery(imagename);
			String imageurl = "http://rogersfido.area.ca/productimages/"	+ imagename;

			log.info("URL : " + imageurl);
			dl.download(imageurl, image);
		}
		
		Asset asset = inArchive.getAssetBySourcePath(sourcepath);
		if (asset == null) {
			asset = inArchive.createAsset(sourcepath);
		}
		// archive.getAssetImporter().getAssetUtilities().populateCategory(asset,
		// archive, "/WEB-INF/data/media/catalogs/public/originals",
		// path, null);
		asset.setPrimaryFile(image.getName());
		//archive.removeGeneratedImages(asset);
		inArchive.saveAsset(asset, null);
		inProduct.setProperty("image", asset.getId());
		// }

		// inStore.getImageCreator().createMedium(inProduct.getSourcePath(),
		// new ArrayList());
		//
		// inStore.getImageCreator().createThumb(inProduct.getSourcePath(),
		// new ArrayList());

	} catch (Exception e) {
		log.info("Unable to download image for product: "
				+ inProduct.getId() + "url: "
				+ inProduct.getProperty("imageurl") );
		e.printStackTrace();
	}
}





