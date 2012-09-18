
/*
 * Created on Aug 24, 2005
 */

import org.apache.commons.httpclient.util.URIUtil
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openedit.Data
import org.openedit.data.PropertyDetail
import org.openedit.data.PropertyDetails
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.util.CSVReader
import org.openedit.money.Money
import org.openedit.store.Category
import org.openedit.store.InventoryItem
import org.openedit.store.Price
import org.openedit.store.PriceSupport
import org.openedit.store.Product
import org.openedit.store.Store
import org.openedit.store.StoreException
import org.openedit.store.convert.ConvertStatus
import org.openedit.store.search.ProductSearcher

import com.openedit.modules.update.Downloader
import com.openedit.page.Page
import com.openedit.page.manage.PageManager
import com.openedit.util.FileUtils
import com.openedit.util.XmlUtil



Store store = context.getPageValue("store");
ConvertStatus result = new ConvertStatus();
convert(store, result);
Log log = LogFactory.getLog(RogersConverter.class);



	public void setProperties(HashMap inProperties) {
		fieldProperties = inProperties;
	}

	 Category fieldLastCatalog;

	
	public void convert(Store inStore, ConvertStatus inErrorLog)
			throws Exception {
				short SKU_CELL = (short) 0;
				short PRODUCT_ID_CELL = (short) 0;
				short PRODUCT_NAME_CELL = (short) 1;
				short PRODUCT_DESCRIPTION_CELL = (short) 7;
				short CATALOG_ID1 = (short) 11;
				short CATALOG_ID2 = (short) 4;
				
				// protected static final short SUB_CATALOG_ID1 = (short) 13;
				// protected static final short CATALOG_DESC = (short) 6;
				short PRICE_CELL = (short) 13;
				short COST_CELL = (short) 8;
				short WEIGHT_CELL = (short) 14;
				short QUANTITY_CELL = (short) 29;
				PageManager fieldPageManager;
				HashMap fieldProperties;
		// boolean success = downloadCSV(inStore);
		boolean success = true;

		if (success) {
			Page page = inStore.getPageManager().getPage(
					"/" + inStore.getCatalogId()
							+ "/configuration/credentials.html");

			File input = new File(inStore.getStoreDirectory(),
					"upload/import.csv");
			Reader reader = new FileReader(input);
			try {
				boolean done = false;
				CSVReader read = new CSVReader(reader, (char)'|', (char)'\"');

				String[] headers = read.readNext();
				String line = null;
				int rowNum = 0;
				Product product = null;
				List products = new ArrayList();
				Map finalproductlist = new HashMap();
				String[] tabs;
				while ((tabs = read.readNext()) != null) {

					rowNum++;

					String skuCell = tabs[SKU_CELL];
					if (skuCell == null || skuCell.length() == 0) {
						done = true;
					} else {
						InventoryItem inventoryItem = createInventoryItem(1.0f,
								tabs, headers);
						if (inventoryItem == null) {
							continue;
						}
						String idCell = tabs[PRODUCT_ID_CELL];
						if (idCell.equals("1265")) {
							continue;
						}
						// This means we have moved on to a new product
						if (idCell != null) {
							if (product == null
									|| !product.getId().equals(idCell)) {
								product = createProduct(inStore, headers, tabs,
										idCell);

								products.add(product);
								// finalproductlist.put(product.getId(),
								// product);
							}
						}

						if (product == null) {
							inErrorLog.add("No product at or above row "
									+ rowNum);
						} else {
							product.addInventoryItem(inventoryItem);
						}
					}

//					log.info(product);
					// inStore.getProductSearcher().saveData(target, null);
					downloadImage(inStore, product);
					product.setAvailable(true);
					
					
					if("1".equals(product.get("displaydesignationid"))){
						product.setProperty("group", "rogers");
					}
					if("2".equals(product.get("displaydesignationid"))){
						product.setProperty("group", "fido");
					}
					if("3".equals(product.get("displaydesignationid"))){
						product.setProperty("group", null);
					}
					
					
					
					try {
						inStore.getProductArchive().saveProduct(product);
					} catch (Exception e) {
						log.info("could not save: " + product);
					}

					// inStore.getProductSearcher().saveAllData(products, null);

					// saveOutput(inStore, products);
					products.clear();
					// inStore.clearProducts();

				}
				//log.info(rowNum);

				inStore.getCategoryArchive().saveAll();

				inErrorLog.add("Total products processed: " + rowNum);
				inStore.clearProducts();
				//input.delete();

				inStore.getProductSearcher().reIndexAll();
				// removeInvalidProducts(inStore, finalproductlist);
			}

			finally {
				FileUtils.safeClose(reader);
			}
		}
	}

	private void removeInvalidProducts(Store inStore, Map inFinalproductlist) {
		Collection productids = inStore.listAllKnownProductIds();
		for (Iterator iterator = inFinalproductlist.keySet().iterator(); iterator
				.hasNext();) {
			String id = (String) iterator.next();
			productids.remove(id);

		}
		for (Iterator iterator = productids.iterator(); iterator.hasNext();) {

			Data hit = (Data) iterator.next();
			String productid = hit.getId();
			Product p = inStore.getProduct(productid);
			if (p != null) {
				if ("isn".equals(p.getProperty("productsource"))) {
					log.info("deleting old product that is no longer available: "
							+ p.getId());
					inStore.getProductArchive().deleteProduct(p);

				}
			}
		}

	}

	/**
	 * @param inTabs
	 * @param idCell
	 * @return
	 */
	protected Product createProduct(Store inStore, String[] inHeaders,
			String[] inTabs, String inId) throws Exception {
			def SKU_CELL = (short) 0;
			short PRODUCT_ID_CELL = (short) 0;
			short PRODUCT_NAME_CELL = (short) 1;
			short PRODUCT_DESCRIPTION_CELL = (short) 7;
			short CATALOG_ID1 = (short) 11;
			short CATALOG_ID2 = (short) 4;
			
			// protected static final short SUB_CATALOG_ID1 = (short) 13;
			// protected static final short CATALOG_DESC = (short) 6;
			short PRICE_CELL = (short) 13;
			short COST_CELL = (short) 8;
			short WEIGHT_CELL = (short) 14;
			short QUANTITY_CELL = (short) 29;
			PageManager fieldPageManager;
			HashMap fieldProperties;
		Product product = inStore.getProduct(inId);
		if (product == null) {
			product = new Product();
			product.setId(inId);

		} else {
			log.info("updating pre-existing product");
			product.clearItems();
		}
		// addCatalog(inStore, inTabs, product);

		String nameCell = inTabs[PRODUCT_NAME_CELL];
		product.setName(nameCell);
		String des = inTabs[PRODUCT_DESCRIPTION_CELL];
		if (des != null) {
			des = des.replaceAll("\r\n|\r|\n|\n\r", "<br>");
			product.setDescription(des);
			inStore.getProductArchive().saveProductDescription(product, des);
		}

		addProperties(inStore, inHeaders, inTabs, product);
		addCatalog(inStore, inTabs, product);
		return product;
	}

	protected void createSourcePath(Product inProduct) {

		StringBuffer buffer = new StringBuffer();
		buffer.append("/tools");
		Category cat1 = inProduct.getDefaultCategory();
		Category cat2 = cat1.getParentCatalog();
		if (cat2.getParentCatalog() != null) {
			buffer.append("/");
			buffer.append(cat2.getId());

		}
		buffer.append("/");
		buffer.append(cat1.getId());
		buffer.append("/");
		buffer.append(inProduct.getId().replaceAll("/", "_"));
		inProduct.setSourcePath(buffer.toString());
		// log.info(inProduct.getSourcePath());

	}

	protected void downloadImage(Store inStore, Product inProduct) {
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
			MediaArchive archive = inStore.getStoreMediaManager()
					.getMediaArchive();
			Asset asset = archive.getAssetBySourcePath(sourcepath);
			if (asset == null) {
				asset = archive.createAsset(sourcepath);
			}
			// archive.getAssetImporter().getAssetUtilities().populateCategory(asset,
			// archive, "/WEB-INF/data/media/catalogs/public/originals",
			// path, null);
			asset.setPrimaryFile(image.getName());
			//archive.removeGeneratedImages(asset);
			archive.saveAsset(asset, null);
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

	protected void addProperties(Store inStore, String[] inHeaders,
			String[] inInTabs, Product inProduct) {
		ProductSearcher searcher = inStore.getProductSearcher();
		for (int i = 0; i < inInTabs.length; i++) {
			String header = inHeaders[i];
			header = header.toLowerCase().replace(" ", "");
			header.trim();
			PropertyDetail detail = searcher.getDetail(header);
			if (detail == null) {
				detail = new PropertyDetail();
				detail.setId(header);
				PropertyDetails details = searcher.getPropertyDetails();
				details.addDetail(detail);
				detail.setText(inHeaders[i]);
				detail.setIndex(true);
				detail.setStored(true);
				searcher.getPropertyDetailsArchive().savePropertyDetails(
						details, "product", null);

			}
			String value = inInTabs[i];
			if (value != null && !"NULL".equals(value)) {
				value = new XmlUtil().xmlEscape(value);
				if(value.equals("True")){
					value = "true";
				}
				if(value.equals("False")){
					value = "false";
				}
				
				inProduct.setProperty(header, value);
			}
		}

	}

	protected void addCatalog(Store inStore, String[] inInTabs, Product product)
			throws StoreException {
		// product.addCatalog(inStore.getCategoryArchive().getRootCategory());

				def SKU_CELL = (short) 0;
				short PRODUCT_ID_CELL = (short) 0;
				short PRODUCT_NAME_CELL = (short) 1;
				short PRODUCT_DESCRIPTION_CELL = (short) 7;
				short CATALOG_ID1 = (short) 11;
				short CATALOG_ID2 = (short) 4;
				
				// protected static final short SUB_CATALOG_ID1 = (short) 13;
				// protected static final short CATALOG_DESC = (short) 6;
				short PRICE_CELL = (short) 13;
				short COST_CELL = (short) 8;
				short WEIGHT_CELL = (short) 14;
				short QUANTITY_CELL = (short) 29;
				PageManager fieldPageManager;
				HashMap fieldProperties;
						String id = inInTabs[CATALOG_ID1];
		if (id == null || id.length() == 0) {
			id = "general";
		}
		Category cat1 = null;
		Category cat2 = null;

		Category root = inStore.getCatalogArchive().getCategory("accessories");

		if (root == null) {
			root = new Category();
			root.setId("accessories");
			root.setName("Accessories");

			inStore.getCategoryArchive().getRootCategory().addChild(root);
			root.setParentCatalog(inStore.getCatalogArchive().getRootCategory());
			inStore.getCategoryArchive().cacheCategory(root);
		}

		Category manu = inStore.getCatalogArchive()
				.getCategory("manufacturers");

		if (manu == null) {
			manu = new Category();
			manu.setId("manufacturers");
			manu.setName("Manufacturers");
			inStore.getCategoryArchive().getRootCategory().addChild(manu);
			manu.setParentCatalog(inStore.getCatalogArchive().getRootCategory());
			inStore.getCategoryArchive().cacheCategory(manu);
		}

		if (id != null && id.length() > 0) {
			cat1 = inStore.getCategory(id);
			if (cat1 == null) {
				// String desc = inInTabs[CATALOG_DESC];
				cat1 = inStore.getCategoryArchive().cacheCategory(
						new Category(id, id.replace("_", " ")));
				cat1.setParentCatalog(root);
				root.addChild(cat1);
			}

		}

		String id2 = inInTabs[CATALOG_ID2];
		id2 = "manu" + id2;
		if (id2 != null && id2.length() > 0) {
			cat2 = inStore.getCategory(id2);
			if (cat2 == null) {
				cat2 = inStore.getCategoryArchive().cacheCategory(
						new Category(id2, id2.replace("_", " ")));
				cat2.setParentCatalog(manu);
				manu.addChild(cat2);
			}
		}

		if (cat2 != null) {
			product.addCatalog(cat2);
			product.setDefaultCategory(cat2);
		}
		product.addCatalog(cat1);
		product.setDefaultCategory(cat1);

	}

	private Category getRootCategory(Store inStore) {
		Category tools = inStore.getCategory("tools");
		if (tools == null) {
			tools = new Category();
			tools.setId("tools");

		}
		tools.setParentCatalog(inStore.getCatalogArchive().getRootCategory());
		inStore.getCatalogArchive().getRootCategory().addChild(tools);
		return tools;
	}

	/**
	 * @param inTabs
	 * @param skuCell
	 * @return
	 */
	protected InventoryItem createInventoryItem(float markup, String[] inTabs,
			String[] inHeaders) {
			def SKU_CELL = (short) 0;
			short PRODUCT_ID_CELL = (short) 0;
			short PRODUCT_NAME_CELL = (short) 1;
			short PRODUCT_DESCRIPTION_CELL = (short) 7;
			short CATALOG_ID1 = (short) 11;
			short CATALOG_ID2 = (short) 4;
			
			// protected static final short SUB_CATALOG_ID1 = (short) 13;
			// protected static final short CATALOG_DESC = (short) 6;
			short PRICE_CELL = (short) 13;
			short COST_CELL = (short) 8;
			short WEIGHT_CELL = (short) 14;
			short QUANTITY_CELL = (short) 29;
			PageManager fieldPageManager;
			HashMap fieldProperties;
			InventoryItem inventoryItem = new InventoryItem();
		String skuCell = inTabs[SKU_CELL];
		inventoryItem.setSku(skuCell);
		// String sizeCell = inTabs[SIZE_CELL];
		// inventoryItem.setSize( sizeCell );
		// String colorCell = inTabs[COLOR_CELL];
		// inventoryItem.setColor(colorCell);
		//
		String cost = inTabs[COST_CELL];
		if (cost == null || cost.equals("")) {
			// log.info("product without cost found");
			return null;
		}
		Price price = new Price();

		String retail = inTabs[PRICE_CELL];

		if (retail != null && retail.trim().length() > 0) {
			try {
				Money money = new Money(retail);

				price.setRetailPrice(money);

				PriceSupport pricedata = new PriceSupport();
				pricedata.addTierPrice(1, price);
				inventoryItem.setPriceSupport(pricedata);
			} catch (NumberFormatException e) {
				log.info("error on " + retail);

			}
		}
		// log.info(yourPrice);

		// String weightCell = inTabs[WEIGHT_CELL];
		// if (weightCell != null && weightCell.length() > 0)
		// {
		// inventoryItem.setWeight(Double.parseDouble(weightCell));
		// }

		// String quantity = inTabs[QUANTITY_CELL];
		String quantity = "1000";
		if (quantity != null && quantity.length() > 0) {
			int q = Integer.parseInt(quantity);
			if (q < 0) {
				q = 0;
			}
			inventoryItem.setQuantityInStock(q);

		} else {
			inventoryItem.setQuantityInStock(1000);
		}

		// //Now loop over everything else
		// for (int i = QUANTITY_CELL + 1; i < inHeaders.length; i++)
		// {
		// String col = inHeaders[i];
		// if( inTabs.length < i)
		// {
		// String extCell = inTabs[(short)i];
		// if ( extCell == null || extCell.length() > 0)
		// {
		// inventoryItem.addProperty(col, extCell);
		// }
		// }
		// }

		return inventoryItem;
	}

	protected Price calculatePrice(float markup, InventoryItem inItem,
			String inCost) {

		float cost = Float.parseFloat(inCost);

		return calculatePrice(inItem, cost, markup);

	}

	protected Price calculatePrice(InventoryItem inventoryItem, float inCost,
			float inConversion) {
		Price price = new Price();
		float totalprice = inCost * inConversion;
		Money money = new Money(totalprice);

		price.setRetailPrice(money);

		PriceSupport pricedata = new PriceSupport();
		pricedata.addTierPrice(1, price);
		inventoryItem.setPriceSupport(pricedata);
		return price;
	}



			
		