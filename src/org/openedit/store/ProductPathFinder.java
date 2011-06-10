package org.openedit.store;

/**
 * An interface that maps a product ID to a full path.
 * 
 * @author Eric Galluzzo
 */
public interface ProductPathFinder
{
	/**
	 * Converts the given ID to a relative path, not including any file
	 * extension.
	 * 
	 * @param inProductId  The product ID
	 * 
	 * @return  The full path
	 */
	String idToPath( String inProductId );
}
