/*
 * Created on Oct 1, 2004
 */
package org.openedit.store;

import org.openedit.store.convert.ConvertStatus;

/**
 * @author cburkey
 *
 */
public abstract class Converter
{
	public abstract void convert(Store inStore, ConvertStatus inErrorLog) throws Exception;

}
