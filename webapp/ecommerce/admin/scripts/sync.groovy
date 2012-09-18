import java.util.List;

import org.openedit.Data
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

import com.openedit.OpenEditRuntimeException
import com.openedit.page.Page
import com.openedit.util.FileUtils
import com.openedit.util.OutputFiller
import com.openedit.util.URLUtilities

/*
* Created on Aug 24, 2005
*/



protected void saveOutput(Store inStore, List inOutputAllProducts ) throws Exception
{
	for (int i = 0; i < inOutputAllProducts.size(); i++)
	{
		Product product = (Product) inOutputAllProducts.get(i);
		//product
		if ( product.getOrdering() == -1)
		{
			product.setOrdering(i);
		}
		product.setAvailable(true);
		inStore.getProductArchive().saveProduct( product );
		//inStore.getProductArchive().saveBlankProductDescription(product);
	}
}
  

  

  

   public HashMap getProperties()
   {
		   fieldProperties = new HashMap();
		   fieldProperties.put(2, "suppartnumber");
		   fieldProperties.put(4, "imageurl");
		   fieldProperties.put(5, "manufacturer");
		   fieldProperties.put(6, "shortdescription");
		   fieldProperties.put(8, "cost");
		   fieldProperties.put(10, "isnretail");
		   fieldProperties.put(11, "MAPP");
		   fieldProperties.put(15, "length");
		   fieldProperties.put(16, "width");
		   fieldProperties.put(17, "height");
		   fieldProperties.put(18, "stocking");
		   fieldProperties.put(19, "reorder");
		   fieldProperties.put(20, "UOM");
		   fieldProperties.put(21, "freight");
		   fieldProperties.put(22, "Hazmat");
		   fieldProperties.put(23, "Carb");
		   fieldProperties.put(24, "ORMB");
		   fieldProperties.put(25, "selfcontained");
		   fieldProperties.put(26, "warranty");
		   fieldProperties.put(27, "addeddate");
		   fieldProperties.put(28, "modifydate");
		   fieldProperties.put(29, "UPC");

	

	   return fieldProperties;
   }

   public void setProperties(HashMap inProperties)
   {
	   fieldProperties = inProperties;
   }

    Category fieldLastCatalog;

   

   public void convert(Store inStore, ConvertStatus inErrorLog) throws Exception
   {

	   
	   def short SKU_CELL = (short) 1;
	   def short PRODUCT_ID_CELL = (short) 1;
	   def short PRODUCT_NAME_CELL = (short) 3;
	   def short PRODUCT_DESCRIPTION_CELL = (short) 7;
	   def short CATALOG_ID1 = (short) 12;
	   def short SUB_CATALOG_ID1 = (short) 13;
	   def short CATALOG_DESC = (short) 6;
	   
	   def short COST_CELL = (short) 8;
	   def short WEIGHT_CELL = (short) 14;
	   def short QUANTITY_CELL = (short) 29;
	   
	   boolean success = downloadCSV(inStore);
	   //	boolean success = true;

	   if (success)
	   {
		   //Page page = store.getPageManager().getPage("/" + inStore.getCatalogId() + "/configuration/credentials.html");
		   String markups = "1.25";
		   if (markups == null)
		   {
			   throw new OpenEditRuntimeException("no markup specified!");
		   }
		   float markup = Float.parseFloat(markups);
		   File input = new File(inStore.getStoreDirectory(), "upload/inventory.csv");
		   Reader reader = new FileReader(input);
		   try
		   {
			   boolean done = false;
			   CSVReader read = new CSVReader(reader, (char)',', (char)'\"');

			   String[] headers = read.readNext();
			   String line = null;
			   int rowNum = 0;
			   Product product = null;
			   List products = new ArrayList();
			   Map finalproductlist = new HashMap();
			   String[] tabs;
			   while ((tabs = read.readNext()) != null)
			   {

				   rowNum++;

				   String skuCell = tabs[SKU_CELL];
				   if (skuCell == null || skuCell.length() == 0)
				   {
					   done = true;
				   }
				   else
				   {
					   InventoryItem inventoryItem = createInventoryItem(markup, tabs, headers);
					   if (inventoryItem == null)
					   {
						   continue;
					   }
					   String idCell = tabs[PRODUCT_ID_CELL];

					   //This means we have moved on to a new product
					   if (idCell != null)
					   {
						   if (product == null || !product.getId().equals(idCell))
						   {
							   product = createProduct(inStore, tabs, idCell);
							   products.add(product);
							   //finalproductlist.put(product.getId(), product);
						   }
					   }

					   if (product == null)
					   {
						   inErrorLog.add("No product at or above row " + rowNum);
					   }
					   else
					   {
						   product.addInventoryItem(inventoryItem);
					   }
				   }
				   if (products.size() == 20)
				   {
					   log.info("Processed: " + products.size() + " products" + rowNum);
					   inStore.getProductSearcher().saveAllData(products, null);
					   saveOutput(inStore, products);
					   products.clear();
					   inStore.clearProducts();

				   }
				   //log.info( rowNum);
			   }

			   inStore.getCategoryArchive().saveAll();
			   saveOutput(inStore, products); //save whatever is left
			   inStore.getProductSearcher().saveAllData(products, null);
			   inErrorLog.add("Total products processed: " + rowNum);
			   inStore.clearProducts();
			   input.delete();
//				for (Iterator iterator = products.iterator(); iterator.hasNext();)
//				{
//					Product target = (Product) iterator.next();
//					log.info(target);
//					//			inStore.getProductSearcher().saveData(target, null);
//					//downloadImage(inStore, target);
//
//				}
			   //inStore.getProductSearcher().reIndexAll();
			   //removeInvalidProducts(inStore, finalproductlist);
		   }

		   finally
		   {
			   FileUtils.safeClose(reader);
		   }
	   }
   }

   private void removeInvalidProducts(Store inStore, Map inFinalproductlist)
   {
	   Collection productids = inStore.listAllKnownProductIds();
	   for (Iterator iterator = inFinalproductlist.keySet().iterator(); iterator.hasNext();)
	   {
		   String id = (String) iterator.next();
		   productids.remove(id);

	   }
	   for (Iterator iterator = productids.iterator(); iterator.hasNext();)
	   {
		   
		   Data hit = (Data) iterator.next();
		   String productid = hit.getId();
		   Product p = inStore.getProduct(productid);
		   if(p != null){
			   if("isn".equals(p.getProperty("productsource"))){
				   log.info("deleting old product that is no longer available: " + p.getId());
				   inStore.getProductArchive().deleteProduct(p);
				   
			   }
		   }
	   }

   }

   protected boolean downloadCSV(Store inStore) throws IOException
   {
	  
	   String path = "ftp://FIX002:F1x3d072hop@web-ftp1.toolweb.com/ISNC/ISNC.csv";
	  
	   URL url = new URL(path);
	   URLConnection con = url.openConnection();
	   InputStream instream = con.getInputStream();

	   Page download = context.getPageValue("pageManager").getPage("/" + inStore.getCatalogId() + "/upload/inventory.csv");

	   OutputFiller filler = new OutputFiller();
	   filler.fill(instream, download.getContentItem().getOutputStream());
	   return true;

   }

   /**
	* @param inTabs
	* @param idCell
	* @return
	*/
   protected Product createProduct(Store inStore, String[] inTabs, String inId) throws Exception
   {
	   def short SKU_CELL = (short) 1;
	   def short PRODUCT_ID_CELL = (short) 1;
	   def short PRODUCT_NAME_CELL = (short) 3;
	   def short PRODUCT_DESCRIPTION_CELL = (short) 7;
	   def short CATALOG_ID1 = (short) 12;
	   def short SUB_CATALOG_ID1 = (short) 13;
	   def short CATALOG_DESC = (short) 6;
	   
	   def short COST_CELL = (short) 8;
	   def short WEIGHT_CELL = (short) 14;
	   def short QUANTITY_CELL = (short) 29;
	   
	   Product product = new Product();
	   product.setId(inId);
	   addCatalog(inStore, inTabs, product);
	   createSourcePath(product);
	   //log.info("processing product: " + inId);
//		Product product = inStore.getProduct(inId);
//		if (product == null)
//		{
//			product = new Product();
//			product.setId(inId);
//
//		}
//		else
//		{
//			log.info("updating pre-existing product");
//			product.clearItems();
//		}
//		addCatalog(inStore, inTabs, product);
	   
	   product.setProperty("productsource", "isn");
	   String nameCell = inTabs[PRODUCT_NAME_CELL];
	   product.setName(nameCell);
	   String des = inTabs[PRODUCT_DESCRIPTION_CELL];
	   if (des != null)
	   {
		   des = des.replaceAll("\r\n|\r|\n|\n\r", "<br>");
		   product.setDescription(des);
		   inStore.getProductArchive().saveProductDescription(product, des);
	   }

	   addProperties(inStore, inTabs, product);

	   return product;
   }

   
   protected void createSourcePath(Product inProduct)
   {

	   StringBuffer buffer = new StringBuffer();
	   buffer.append("/tools");
	   Category cat1 = inProduct.getDefaultCategory();
	   Category cat2 = cat1.getParentCatalog();
	   if (cat2.getParentCatalog() != null)
	   {
		   buffer.append("/");
		   buffer.append(cat2.getId());

	   }
	   buffer.append("/");
	   buffer.append(cat1.getId());
	   buffer.append("/");
	   buffer.append(inProduct.getId().replaceAll("/", "_"));
	   inProduct.setSourcePath(buffer.toString());
	   //log.info(inProduct.getSourcePath());

   }

//	protected void downloadImage(Store inStore, Product inProduct)
//	{
//		try
//		{
//			String path = inStore.getStoreHome() + "/data/assets/" + inProduct.getSourcePath() + ".jpg";
//			File image = new File(inStore.getRootDirectory(), path);
//
//			if (!image.exists())
//			{
//				Downloader dl = new Downloader();
//				String imageurl = inProduct.getProperty("imageurl");
//				if (imageurl != null && imageurl.length() > 0)
//				{
//					dl.download(imageurl, image);
//					inStore.getImageCreator().createMedium(inProduct.getSourcePath(), new ArrayList());
//
//					inStore.getImageCreator().createThumb(inProduct.getSourcePath(), new ArrayList());
//				}
//			}
//		}
//		catch (Exception e)
//		{
//			log.info("Unable to download image for product: " + inProduct.getId() + "url: " + inProduct.getProperty("imageurl"));
//		}
//
//	}

   protected void addProperties(Store inStore, String[] inInTabs, Product inProduct)
   {
	   for (Iterator iterator = getProperties().keySet().iterator(); iterator.hasNext();)
	   {
		   int field = (Integer) iterator.next();
		   String prop = (String) getProperties().get(field);
		   String val = inInTabs[(short) field];
		   val = URLUtilities.xmlEscape(val);
		   if (prop != null && val != null)
		   {
			   inProduct.putAttribute(prop, val);
		   }
	   }

   }

   protected void addCatalog(Store inStore, String[] inInTabs, Product product) throws StoreException
   {
	   def short SKU_CELL = (short) 1;
	   def short PRODUCT_ID_CELL = (short) 1;
	   def short PRODUCT_NAME_CELL = (short) 3;
	   def short PRODUCT_DESCRIPTION_CELL = (short) 7;
	   def short CATALOG_ID1 = (short) 12;
	   def short SUB_CATALOG_ID1 = (short) 13;
	   def short CATALOG_DESC = (short) 6;
	   
	   def short COST_CELL = (short) 8;
	   def short WEIGHT_CELL = (short) 14;
	   def short QUANTITY_CELL = (short) 29;
	   
	   	   String id = inInTabs[CATALOG_ID1];
	   if (id == null || id.length() == 0)
	   {
		   id = "tools";
	   }
	   Category cat1 = null;
	   Category cat2 = null;
	   Category root = inStore.getCategory("tools");
	   if(root == null){
		   root = new Category();
		   root.setId("tools");
		   root.setName("Tools and Equipment");
		   inStore.getCategoryArchive().getRootCategory().addChild(root);
		   root.setParentCatalog(inStore.getCatalogArchive().getRootCategory());
	   }

	   if (id != null && id.length() > 0)
	   {
		   cat1 = inStore.getCategory(id);
		   if (cat1 == null)
		   {
			   String desc = inInTabs[CATALOG_DESC];
			   cat1 = inStore.getCategoryArchive().cacheCategory(new Category(id, id.replace("_", " ")));
			   cat1.setParentCatalog(inStore.getCategoryArchive().getRootCategory());
			   root.addChild(cat1);
		   }
		   String id2 = inInTabs[SUB_CATALOG_ID1];

		   if (id2 != null && id2.length() > 0)
		   {
			   cat2 = inStore.getCategory(id2);
			   if (cat2 == null)
			   {
				   cat2 = inStore.getCategoryArchive().cacheCategory(new Category(id2, id2.replace("_", " ")));
				   cat2.setParentCatalog(cat1);
				   cat1.addChild(cat2);
			   }
		   }

		   if (cat2 != null)
		   {
			   product.addCatalog(cat2);
			   product.setDefaultCategory(cat2);
		   }
		   else
		   {
			   product.addCatalog(cat1);
			   product.setDefaultCategory(cat1);
		   }
	   }
   }

   private Category getRootCategory(Store inStore)
   {
	   Category tools = inStore.getCategory("tools");
	   if (tools == null)
	   {
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
   protected InventoryItem createInventoryItem(float markup, String[] inTabs, String[] inHeaders)
   {
	   def short SKU_CELL = (short) 1;
	   def short PRODUCT_ID_CELL = (short) 1;
	   def short PRODUCT_NAME_CELL = (short) 3;
	   def short PRODUCT_DESCRIPTION_CELL = (short) 7;
	   def short CATALOG_ID1 = (short) 12;
	   def short SUB_CATALOG_ID1 = (short) 13;
	   def short CATALOG_DESC = (short) 6;
	   
	   def short COST_CELL = (short) 8;
	   def short WEIGHT_CELL = (short) 14;
	   def short QUANTITY_CELL = (short) 29;
	   
	   InventoryItem inventoryItem = new InventoryItem();
	   String skuCell = inTabs[SKU_CELL];
	   inventoryItem.setSku(skuCell);
	   //String sizeCell = inTabs[SIZE_CELL];
	   //inventoryItem.setSize( sizeCell  );
	   //			String colorCell = inTabs[COLOR_CELL];
	   //			inventoryItem.setColor(colorCell);
	   //
	   String cost = inTabs[COST_CELL];
	   if (cost == null || cost.equals(""))
	   {
		   //log.info("product without cost found");
		   return null;
	   }
	   Price price = calculatePrice(markup, inventoryItem, cost);

	   //			String re = inTabs[YOURPRICE_CELL];

	   //				if(retail != null && retail.trim().length() > 0){
	   //					price.setRetailPrice(new Money(  retail.trim() ) );
	   //				}
	   //log.info(yourPrice);

	   String weightCell = inTabs[WEIGHT_CELL];
	   if (weightCell != null && weightCell.length() > 0)
	   {
		   inventoryItem.setWeight(Double.parseDouble(weightCell));
	   }

	   String quantity = inTabs[QUANTITY_CELL];
	   if (quantity != null && quantity.length() > 0)
	   {
		   int q = Integer.parseInt(quantity);
		   if (q < 0)
		   {
			   q = 0;
		   }
		   inventoryItem.setQuantityInStock(q);

	   }
	   else
	   {
		   inventoryItem.setQuantityInStock(1000);
	   }

	   //			//Now loop over everything else
	   //			for (int i = QUANTITY_CELL + 1; i < inHeaders.length; i++)
	   //			{
	   //				String col = inHeaders[i];
	   //				if( inTabs.length < i)
	   //				{
	   //					String extCell = inTabs[(short)i];
	   //					if ( extCell == null || extCell.length() > 0)
	   //					{
	   //						inventoryItem.addProperty(col, extCell);
	   //					}
	   //				}
	   //			}

	   return inventoryItem;
   }

   protected Price calculatePrice(float markup, InventoryItem inItem, String inCost)
   {

	   float cost = Float.parseFloat(inCost);

	   return calculatePrice(inItem, cost, markup);

   }

   private Price calculatePrice(InventoryItem inventoryItem, float inCost, float inConversion)
   {
	   Price price = new Price();
	   float totalprice = inCost * inConversion;
	   Money money = new Money(totalprice);

	   price.setRetailPrice(money);

	   PriceSupport pricedata = new PriceSupport();
	   pricedata.addTierPrice(1, price);
	   inventoryItem.setPriceSupport(pricedata);
	   return price;
   }



Store store = context.getPageValue("store");
ConvertStatus result = new ConvertStatus();
convert(store, result);
def short SKU_CELL = (short) 1;
def short PRODUCT_ID_CELL = (short) 1;
def short PRODUCT_NAME_CELL = (short) 3;
def short PRODUCT_DESCRIPTION_CELL = (short) 7;
def short CATALOG_ID1 = (short) 12;
def short SUB_CATALOG_ID1 = (short) 13;
def short CATALOG_DESC = (short) 6;

def short COST_CELL = (short) 8;
def short WEIGHT_CELL = (short) 14;
def short QUANTITY_CELL = (short) 29;
