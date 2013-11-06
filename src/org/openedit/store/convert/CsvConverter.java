package org.openedit.store.convert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.openedit.money.Money;
import org.openedit.store.CatalogConverter;
import org.openedit.store.Category;
import org.openedit.store.InventoryItem;
import org.openedit.store.Price;
import org.openedit.store.PriceSupport;
import org.openedit.store.Product;
import org.openedit.store.Store;

import com.openedit.OpenEditException;
import com.openedit.util.FileUtils;

public class CsvConverter extends CatalogConverter
{

	protected static final short SKU_CELL = (short) 0;
	protected static final short PRODUCT_ID_CELL = (short) 1;
	protected static final short PRODUCT_NAME_CELL = (short) 2;
	protected static final short PRODUCT_DESCRIPTION_CELL = (short) 3;
	protected static final short KEYWORDS_CELL = (short) 4;
	protected static final short CATALOG_ID1 = (short) 5;
	protected static final short RETAILPRICE_CELL = (short) 6;
	protected static final short YOURPRICE_CELL = (short) 7;
	protected static final short SIZE_CELL = (short) 8;
	protected static final short COLOR_CELL = (short) 9;
	protected static final short WEIGHT_CELL = (short) 10;
	protected static final short QUANTITY_CELL = (short) 11;

	
	public void convert(Store inStore, ConvertStatus inErrorLog) throws Exception
	{
		File input = new File( inStore.getStoreDirectory() , "upload/inventory.csv");
		if( input.exists())
		{
			Reader reader = new FileReader(input);
			try
			{
				boolean done = false;

				BufferedReader in = new BufferedReader(reader);
				String[] headers = in.readLine().split(",");
				String line = null;
				int rowNum = 0;
				Product product = null;
				List products = new ArrayList();
				while( (line = in.readLine() ) != null)
				{
					rowNum++;
					String[] tabs = line.split(",");
					
					String skuCell = tabs[SKU_CELL];
					if (skuCell == null || skuCell.length() == 0)
					{
						done = true;
					}
					else
					{
						InventoryItem inventoryItem = createInventoryItem(tabs, headers);
						String idCell = tabs[PRODUCT_ID_CELL];

						//This means we have moved on to a new product
						if (idCell != null )
						{
							if ( product == null || !product.getId().equals(idCell))
							{
								product = createProduct(inStore, tabs,idCell);
								products.add(product);
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
				}
				inErrorLog.add("Processed: " + products.size() + " products");
				inStore.getCategoryArchive().saveAll();
				saveOutput(inStore, products);
				inStore.clearProducts();
				input.delete();
			}
			finally
			{
				FileUtils.safeClose(reader);
			}
		}
	}

	protected InventoryItem createInventoryItem(String[] inTabs, String[] inHeaders)
	{
			InventoryItem inventoryItem = new InventoryItem();
			String skuCell = inTabs[SKU_CELL];
			inventoryItem.setSku(skuCell );
			String sizeCell = inTabs[SIZE_CELL];
			inventoryItem.setSize( sizeCell  );			
			String colorCell = inTabs[COLOR_CELL];
			inventoryItem.setColor(colorCell);
			
			String retail = inTabs[RETAILPRICE_CELL];
			String yourPrice = inTabs[YOURPRICE_CELL];
			
			if ( yourPrice != null && yourPrice.trim().length() > 0)
			{
				Price price = new Price();
				if(retail != null && retail.trim().length() > 0){
					price.setRetailPrice(new Money(  retail.trim() ) );
				}
				price.setSalePrice(new Money(  yourPrice.trim() ) );
				
				PriceSupport pricedata = new PriceSupport();
				pricedata.addTierPrice(1, price);
				inventoryItem.setPriceSupport(pricedata);
			}
			
			String weightCell =  inTabs[WEIGHT_CELL];
			if ( weightCell != null && weightCell.length() > 0)
			{
				inventoryItem.setWeight(Double.parseDouble(weightCell));
			}
			
			String quantity =  inTabs[QUANTITY_CELL];
			if ( quantity != null && quantity.length() > 0)
			{
				inventoryItem.setQuantityInStock(Integer.parseInt(quantity));
			}
			else
			{
				inventoryItem.setQuantityInStock(1000);
			}

			//Now loop over everything else		
			for (int i = QUANTITY_CELL + 1; i < inHeaders.length; i++)
			{
				String col = inHeaders[i];
				if( inTabs.length < i)
				{
					String extCell = inTabs[(short)i];
					if ( extCell == null || extCell.length() > 0)
					{
						inventoryItem.addProperty(col, extCell);
					}
				}
			}
			
			return inventoryItem;
		}
	
	protected Product createProduct(Store inStore, String[] inTabs, String inId) throws Exception
	{		
		Product product = inStore.getProduct(inId);
		if ( product == null)
		{
			product = new Product();
			product.setId(inId);			
		}
		else
		{
			product.clearItems();
			inStore.getProductArchive().clearProduct(product);
		}
		String nameCell = inTabs[PRODUCT_NAME_CELL];
		product.setName(  nameCell );
		String descriptionCell = inTabs[PRODUCT_DESCRIPTION_CELL];
		if( descriptionCell != null)
		{
			String des = descriptionCell;
			if ( des != null)
			{
				des = des.replaceAll("\r\n|\r|\n|\n\r","<br>");
			}
			product.setDescription( des );
		}
		String keywords = inTabs[KEYWORDS_CELL];
		if ( keywords != null && keywords.length() > 0)
		{
			product.clearKeywords();
			product.addKeyword(keywords);
		}
		addCatalog(inStore, inTabs, product);
		return product;
	}

	protected void addCatalog(Store inStore, String[] inTabs, Product inProduct) throws OpenEditException
	{ 
		String cell = inTabs[CATALOG_ID1];
		if( cell != null && cell.length() > 0)
		{
			String[] ids = cell.split(",");
			if( ids.length > 0)
			{
				inProduct.clearCatalogs();
			}
			for (int i = 0; i < ids.length; i++)
			{
				String id = ids[i].trim();
				if( id.length() > 0)
				{
					Category cat1= inStore.getCatalog(id);
					if ( cat1 == null)
					{
						cat1 = new Category(id,id);
					}
					inProduct.addCatalog(cat1);
				}
			}
		}
	}

}
