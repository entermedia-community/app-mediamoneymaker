/*
 * Created on Oct 3, 2004
 */
package org.openedit.store;

import java.util.List;

import org.openedit.OpenEditException;


/**
 * @author cburkey
 *
 */
public interface CategoryArchive
{
	Category getCategory(String inCatalog);
	
	Category getCategoryByName(String inCatalogName) throws StoreException;
	
	
	List listAllCategories()throws StoreException;
	
	public Category cacheCategory( Category inCatalog ) throws StoreException;

	public Category addChild( Category inCatalog ) throws StoreException;

	public void deleteCategory( Category inCatalog ) throws StoreException;


	void setRootCategory(Category inRoot) throws StoreException;

	Category getRootCategory() throws StoreException;

	/**
	 * Blows away all children
	 * @param inRoot
	 */
	public void clearCategories() throws StoreException;
	void reloadCategories() throws StoreException;
	//void saveCatalog( Category inCatalog) throws StoreException;
	void saveAll( ) throws StoreException;

	public void setCatalogId( String inCategoryId);

	void saveCategory(Category inCategory) throws StoreException;
	
	public Category createCategoryTree(String inPath) throws OpenEditException;
}
