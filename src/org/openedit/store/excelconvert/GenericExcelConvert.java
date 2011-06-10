/*
 * Created on Aug 24, 2005
 */
package org.openedit.store.excelconvert;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.openedit.money.Money;
import org.openedit.store.Category;
import org.openedit.store.InventoryItem;
import org.openedit.store.Price;
import org.openedit.store.PriceSupport;
import org.openedit.store.Product;
import org.openedit.store.Store;
import org.openedit.store.StoreException;
import org.openedit.store.convert.ConvertStatus;

public class GenericExcelConvert extends ExcelConverter
{

	private static final Log log = LogFactory.getLog(GenericExcelConvert.class);
	protected static final short SKU_CELL = (short) 0;
	protected static final short PRODUCT_ID_CELL = (short) 1;
	protected static final short PRODUCT_NAME_CELL = (short) 2;
	protected static final short PRODUCT_DESCRIPTION_CELL = (short) 3;
	protected static final short KEYWORDS_CELL = (short) 4;
	protected static final short CATALOG_ID1 = (short) 5;
	protected static final short CATALOG_DESC = (short) 6;
	protected static final short SIZE_CELL = (short) 7;
	protected static final short COLOR_CELL = (short) 8;
	protected static final short PRICE_CELL = (short) 9;
	protected static final short WEIGHT_CELL = (short) 10;
	protected static final short QUANTITY_CELL = (short) 11;
	
	protected Category fieldLastCatalog;
	
	public GenericExcelConvert()
	{
		super();
	}

	protected void processWorkbook(Store inStore,ConvertStatus inLog, HSSFWorkbook inWorkbook) throws Exception
	{
		HSSFSheet sheet = inWorkbook.getSheetAt(0);
		int rowNum = 0;
		boolean done = false;
		List products = new ArrayList();
		Header header = new Header();
		Product product = null;
		while (!done)
		{
			HSSFRow row = sheet.getRow(rowNum);
			rowNum++;
			if (row == null)
			{
				break;
			}
			if ( rowNum == 1 )
			{
				short col = 0;
				while( true )
				{
					HSSFCell cell = row.getCell(col);
					if ( cell != null && cell.getCellType() != HSSFCell.CELL_TYPE_BLANK)
					{
						header.addCol(col,toString( cell ) );
					}
					else
					{
						break;
					}
					col++;
				}
				continue;
			}
			HSSFCell skuCell = row.getCell(SKU_CELL);
			if (skuCell == null || skuCell.getCellType() == HSSFCell.CELL_TYPE_BLANK)
			{
				done = true;
			}
			else
			{
				InventoryItem inventoryItem = createInventoryItem(row, header);
				HSSFCell idCell = row.getCell(PRODUCT_ID_CELL);

				//This means we have moved on to a new product
				if (idCell != null && idCell.getCellType() != HSSFCell.CELL_TYPE_BLANK )
				{
					String id = toString( idCell  );
					if ( product == null || !product.getId().equals(id))
					{
						product = createProduct(inStore, row,id);
						products.add(product);
					}
				}

				if (product == null)
				{
					inLog.add("No product at or above row " + rowNum);
				}
				else
				{
					product.addInventoryItem(inventoryItem);
				}
			}
		}
		inLog.add("Processed: " + products.size() + " products");
		inStore.getCategoryArchive().saveAll();
		saveOutput(inStore, products);

	}

	/**
	 * @param inRow
	 * @param idCell
	 * @return
	 */
	protected Product createProduct(Store inStore, HSSFRow inRow, String inId) throws Exception
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
		}
		HSSFCell nameCell = inRow.getCell(PRODUCT_NAME_CELL);
		product.setName( toString( nameCell ) );
		HSSFCell descriptionCell = inRow.getCell(PRODUCT_DESCRIPTION_CELL);
		if( descriptionCell != null)
		{
			String des = toString( descriptionCell);
			if ( des != null)
			{
				des = des.replaceAll("\r\n|\r|\n|\n\r","<br>");
			}
			product.setDescription( des );
		}
		String keywords = toString( inRow.getCell(KEYWORDS_CELL) );
		if ( keywords != null && keywords.length() > 0)
		{
			product.addKeyword(keywords);
		}

		addCatalog(inStore, inRow, product);
		return product;
	}

	protected void addCatalog(Store inStore, HSSFRow inRow, Product product) throws StoreException
	{
		HSSFCell cell = inRow.getCell(CATALOG_ID1);
		String id = toString( cell );
		if( id != null && id.length() > 0)
		{
			Category cat1= inStore.getCatalog(id);
			if ( cat1 == null)
			{
				String desc = toString(inRow.getCell(CATALOG_DESC));
				cat1 = inStore.getCategoryArchive().cacheCategory(new Category(id,desc));
			}
			product.addCatalog(cat1);
		}
	}

	/**
	 * @param inRow
	 * @param skuCell
	 * @return
	 */
	protected InventoryItem createInventoryItem(HSSFRow inRow, Header inHeader)
	{
		InventoryItem inventoryItem = new InventoryItem();
		HSSFCell skuCell = inRow.getCell(SKU_CELL);
		inventoryItem.setSku(toString( skuCell ) );
		HSSFCell sizeCell = inRow.getCell(SIZE_CELL);
		inventoryItem.setSize(toString( sizeCell ) );			
		HSSFCell colorCell = inRow.getCell(COLOR_CELL);
		inventoryItem.setColor(toString(colorCell ));
		
		String val = toString(inRow.getCell(PRICE_CELL));
		if ( val != null)
		{
			PriceSupport price = new PriceSupport();
			price.addTierPrice(1,new Price(new Money(  val ) ) );
			inventoryItem.setPriceSupport(price);
		}
		
		String weightCell = toString( inRow.getCell(WEIGHT_CELL) );
		if ( weightCell != null && weightCell.length() > 0)
		{
			inventoryItem.setWeight(Double.parseDouble(weightCell));
		}
		
		String quantity = toString( inRow.getCell(QUANTITY_CELL) );
		if ( quantity != null && quantity.length() > 0)
		{
			inventoryItem.setQuantityInStock(Integer.parseInt(quantity));
		}
		else
		{
			inventoryItem.setQuantityInStock(1000);
		}

		//Now loop over everything else		
		for (int i = QUANTITY_CELL + 1; i < inHeader.getSize(); i++)
		{
			String col = inHeader.getColumn(i);
			HSSFCell extCell = inRow.getCell((short)i);
			if ( extCell == null || extCell.getCellType() != HSSFCell.CELL_TYPE_BLANK)
			{
				inventoryItem.addProperty(col, toString(extCell));
			}
		}
		
		return inventoryItem;
	}

}
