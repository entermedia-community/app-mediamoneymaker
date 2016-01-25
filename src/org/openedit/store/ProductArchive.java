/*
 * Created on Apr 23, 2004
 */
package org.openedit.store;

import org.openedit.data.PropertyDetails;
import org.openedit.users.User;

/**
 * @author cburkey
 *
 */
public interface ProductArchive
{

	public Product getProductBySourcePath(String inSourcePath) throws StoreException;

	
	Product getProduct(String inId) throws StoreException;

	/**
	 * 
	 */
	void clearProducts();

	public void clearProduct( Product inProduct );

	/**
	 * @param inInItem
	 */
	void saveProduct(Product inProduct, User inUser) throws StoreException;
	void saveProduct(Product inInItem) throws StoreException;

	void deleteProduct( Product inItem ) throws StoreException;

	/**
	 * @param inProduct
	 * @return
	 */
	String loadDescription(Product inProduct) throws StoreException;
	
	String nextProductNumber() throws StoreException;

	/**
	 * @param inProduct
	 */
	void saveProductDescription(Product inProduct, String inDescription) throws StoreException;
	
	/**
	 * Returns the product path finder.
	 * 
	 * @return  The product path finder
	 */
	ProductPathFinder getProductPathFinder();
		
	PropertyDetails getPropertyDetails();

	void saveBlankProductDescription(Product inProduct) throws StoreException;

	//List listAllProductIds();
	
	void setStore(Store inStore);
	Store getStore();
	
	void setCatalogId(String inId);
	String getCatalogId();
	
	public String buildXconfPath(Product inProduct);

}
