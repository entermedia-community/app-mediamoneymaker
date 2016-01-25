/*
 * Created on Aug 24, 2006
 */
package org.openedit.store;

import java.io.Writer;

import org.openedit.OpenEditException;

public interface ProductExport
{
	public void exportAllProducts(Store inStore, Writer inOut) throws OpenEditException;
	public void exportCatalogsWithProducts(Store inStore, Writer inOut) throws OpenEditException;

}
