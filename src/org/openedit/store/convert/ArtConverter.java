package org.openedit.store.convert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.store.CatalogConverter;
import org.openedit.store.Category;
import org.openedit.store.InventoryItem;
import org.openedit.store.Product;
import org.openedit.store.Store;
import org.openedit.store.StoreException;

import com.openedit.util.FileUtils;

/**
 * This class needs to be refactored, and even possibly eliminated.
 * It would be best to make this generic.
 * 
 * @author mavery
 *
 */
public class ArtConverter extends CatalogConverter
{
	private static final Log log = LogFactory.getLog(ArtConverter.class);
	
	public void convert(Store inStore, ConvertStatus inErrorLog) throws Exception
	{

		convertCategories(inStore, inErrorLog );
		convertArt( inStore, inErrorLog );

		inStore.clearProducts();
		File inProducts = new File( inStore.getStoreDirectory() , "upload/CATEGORY.txt");
		if( inProducts.exists() )
		{
			inErrorLog.setReindex(true);
		}
	}
	
	/**
	 *              CATEGORY.TXT - Categorias y Subcategorias
		Fiel name: position - length
		COD. CATEGORIA: 1 - 5
		NOMBRE CATEGORIA: 6 - 60
		COD. SUBCATEGORIA: 66 - 5
		NOMBRE SUBCATEGORIA: 71 - 100
	 * @param inStore
	 * @param inErrorLog
	 * @throws Exception
	 */
	public void convertCategories(Store inStore, ConvertStatus inErrorLog) throws Exception
	{
		//read in Inventory file
		File inCategories = new File( inStore.getStoreDirectory() , "upload/CATEGORY.TXT");
		
		//List productIds = new ArrayList();
		if( inCategories.exists() )
		{
			FileInputStream fileInput = new FileInputStream( inCategories );
			
			try
			{
				BufferedReader in = new BufferedReader(new InputStreamReader(fileInput, "ISO-8859-1"));
				String line = null;
				int rowNum = 0;
				
				while( (line = in.readLine() ) != null)
				{
					line = new String( line.getBytes(), "UTF8" );
					rowNum++;
					if( line.length() == 0)
					{
						break;
					}
					if ( line.length() < 70 )
					{
						inErrorLog.add( "Bad record in CATEGORY.TXT on line " + rowNum );
						log.warn( "Bad record in CATEGORY.TXT on line " + rowNum );
						continue;
					}
					
					
					String catid = extractCategoryId( line );
					String categoryName = new String( extractCategoryName( line ).getBytes(), "UTF8" );
					Category familycat = inStore.getCategory(catid);
					if( familycat == null)
					{
						familycat = inStore.getCategoryArchive().addChild(new Category(catid,categoryName));
					}
				
					Category subcategory = null;
						
					String subCategoryName = new String( extractSubCategoryName( line ).getBytes(), "UTF8" );
					
					if( !subCategoryName.equals(""))
					{
						String subCategoryId = extractSubCategoryId( line );
						subcategory = inStore.getCategory(subCategoryId);
						if( subcategory == null)
						{
							subcategory = new Category(subCategoryId,subCategoryName);
							familycat.addChild(subcategory);
							//inStore.getCategoryArchive().addChild(subcategory);
						}
					}	
					
				}
				inErrorLog.add( "Read " + rowNum + " category records." );
				inStore.getCategoryArchive().saveAll();
				
			}
			finally
			{
				FileUtils.safeClose(fileInput);
			}
		}
	}
	
	protected String extractCategoryId( String inLine )
	{
		return inLine.substring( 0, 5 ).trim();
	}
	
	protected String extractCategoryName( String inLine )
	{
		return inLine.substring( 5, 65 ).trim();
	}
	
	protected String extractSubCategoryId( String inLine )
	{
		return inLine.substring( 65, 70 ).trim();
	}
	
	protected String extractSubCategoryName( String inLine )
	{
		return inLine.substring( 70 ).trim();
	}
	
	protected String extractArtId( String inLine )
	{
		return inLine.substring(0,8).trim();
	}
	
	protected String extractArtName( String inLine )
	{
		return inLine.substring( 8, 38 ).trim();
	}
	
	protected String extractArtCataloguing (String inLine )
	{
		return inLine.substring( 38, 40 ).trim();
	}
	protected String extractArtWidth (String inLine )
	{
		return inLine.substring( 40, 45 ).trim();
	}
	
	protected String extractArtHeight ( String inLine )
	{
		return inLine.substring(45, 50 ).trim();
	}
	
	protected String extractArtCategory( String inLine )
	{
		return inLine.substring(50, 55 ).trim();
	}
	
	protected String extractArtSubCategory( String inLine )
	{
		return inLine.substring( 55, 60 ).trim();
	}
	
	protected String extractArtPath( String inLine )
	{
		return inLine.substring(60 + "$GRF_ARTSTORE".length()).trim();
	}

	public void convertArt(Store inStore, ConvertStatus inErrorLog) throws Exception
	{
		//read in Inventory file
		File artFile = new File( inStore.getStoreDirectory() , "upload/ART.TXT");
		
		//List productIds = new ArrayList();
		if( artFile.exists() )
		{
			FileInputStream fileInput = new FileInputStream(artFile);
			
			
			try
			{
				BufferedReader in = new BufferedReader(new InputStreamReader(fileInput, "ISO-8859-1"));
				String line = null;
				int rowNum = 0;
				Product product = null;
				
				while( (line = in.readLine() ) != null)
				{
					line = new String(line.getBytes(), "UTF8");
					rowNum++;
					if( line.length() == 0)
					{
						break;
					}
					if ( line.length() < 60 )
					{
						inErrorLog.add( "Bad record in ART.TXT on line " + rowNum );
						log.warn( "Bad record in ART.TXT on line " + rowNum );
						continue;
					}
					String id = extractArtId( line );
					product = createProduct(inStore,id);
					product.setName( new String(extractArtName( line ).getBytes(), "UTF8") );
					product.setAvailable(true);
					String cataloguing = extractArtCataloguing( line );
					product.setProperty("cataloguing", cataloguing );
					if("C".equalsIgnoreCase( cataloguing ) )
					{
						continue;
					}
					product.setProperty( "width", extractArtWidth( line ) );
					product.setProperty( "height", extractArtHeight( line ) );
					// Is this correct?
					product.setOriginalImagePath( extractArtPath( line ) );
					Category rootCatalog = inStore.getCategoryArchive().getRootCategory();
					product.addCatalog( rootCatalog );
					product.setDefaultCatalog( rootCatalog );
					
					String catalogId = extractArtCategory( line );
					setCatalog( inStore, product, catalogId );

					catalogId = extractArtSubCategory( line );
					setCatalog( inStore, product, catalogId );
					
	
					product.setInventoryItems(null);
					inStore.getProductArchive().saveProduct( product );
				}

				
			}
			finally
			{
				FileUtils.safeClose(fileInput);
			}
			

		}
	}

	protected void setCatalog( Store inStore, Product product, String catalogId )
			throws StoreException
	{
		if( catalogId != null && catalogId.length() > 0 )
		{
			Category catalog = inStore.getCategory(catalogId);
			if ( catalog != null )
			{
				log.debug( "Adding catalog " + catalog.getId() + " to product " + product.getId() );
				product.addCatalog( catalog );
				product.setDefaultCatalog( catalog );
			}
		}
	}
	

	protected Product createProduct(Store inStore, String inId) throws Exception
	{		
		Product product = inStore.getProduct(inId);
		if ( product == null)
		{
			product = new Product();
			product.setId(inId);
			InventoryItem item = new InventoryItem();
			item.setSku(product.getId());
			item.setQuantityInStock(0);
			product.addInventoryItem(item);
			
		}
		return product;
	}

	

	
	
}
