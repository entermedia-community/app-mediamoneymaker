/*
 * Created on Jul 13, 2006
 */
package org.openedit.store.products;

import org.openedit.data.PropertyDetails;
import org.openedit.store.Product;
import org.openedit.store.ProductArchive;
import org.openedit.store.ProductPathFinder;
import org.openedit.store.Store;
import org.openedit.store.StoreException;
import org.openedit.users.User;

public class MirrorProductArchive implements ProductArchive
{
	protected ProductArchive fieldBaseArchive;
	protected ProductArchive fieldMirrorArchive;
	
	public ProductArchive getBaseArchive()
	{
		return fieldBaseArchive;
	}

	public void setBaseArchive(ProductArchive inBaseArchive)
	{
		fieldBaseArchive = inBaseArchive;
	}

	public ProductArchive getMirrorArchive()
	{
		return fieldMirrorArchive;
	}

	public void setMirrorArchive(ProductArchive inMirrorArchive)
	{
		fieldMirrorArchive = inMirrorArchive;
	}

	public void clearProduct(Product inProduct)
	{
		fieldBaseArchive.clearProduct(inProduct);
	}

	public void clearProducts()
	{
		fieldBaseArchive.clearProducts();
	}

	public void deleteProduct(Product inItem) throws StoreException
	{
		fieldBaseArchive.deleteProduct(inItem);
	}

	public Product getProduct(String inId) throws StoreException
	{
		return fieldBaseArchive.getProduct(inId);
	}

	public ProductPathFinder getProductPathFinder()
	{
		return fieldBaseArchive.getProductPathFinder();
	}

	public PropertyDetails getPropertyDetails() 
	{
		return fieldBaseArchive.getPropertyDetails();
	}

	public Store getStore()
	{
		return fieldBaseArchive.getStore();
	}


	public String loadDescription(Product inProduct) throws StoreException
	{
		return fieldBaseArchive.loadDescription(inProduct);
	}

	public String nextProductNumber() throws StoreException
	{
		return fieldBaseArchive.nextProductNumber();
	}

	public void saveBlankProductDescription(Product inProduct) throws StoreException
	{
		fieldBaseArchive.saveBlankProductDescription(inProduct);
	}
	public void saveProduct(Product inProduct) throws StoreException
	{
		saveProduct(inProduct, (User) null);
	}

	public void saveProduct(Product inInItem, User inUser) throws StoreException
	{
		fieldBaseArchive.saveProduct(inInItem);
		if (fieldMirrorArchive != null)
		{
			fieldMirrorArchive.saveProduct(inInItem, inUser);
		}
	}

	public void saveProductDescription(Product inProduct, String inDescription) throws StoreException
	{
		fieldBaseArchive.saveProductDescription(inProduct, inDescription);
	}

	public void setStore(Store inDir)
	{
		fieldBaseArchive.setStore(inDir);
	}

	public String getCatalogId()
	{
		// TODO Auto-generated method stub
		return fieldBaseArchive.getCatalogId();
	}

	public void setCatalogId(String inId)
	{
		fieldBaseArchive.setCatalogId(inId);
		
	}

	public Product getProductBySourcePath(String inSourcePath) throws StoreException
	{
		return fieldBaseArchive.getProductBySourcePath(inSourcePath);
	}

	public String buildXconfPath(Product inProduct)
	{
		return fieldBaseArchive.buildXconfPath(inProduct);
	}
}
