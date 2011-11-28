/*
 * Created on Oct 4, 2004
 */
package org.openedit.store;

import java.io.File;

import org.openedit.store.customer.Customer;


/**
 * @author cburkey
 *
 */
public interface CustomerArchive
{
	public abstract Customer getCustomer(String inId) throws StoreException;

	public abstract void saveCustomer(Customer inCustomer) throws StoreException;

	public abstract void saveAndExportCustomer(Customer inC) throws StoreException;

	/**
	 * @return
	 */
	public abstract Customer createNewCustomer(String inUsername, String inPassword)throws StoreException;
	
	//public List findCustomer(String inField1, String inValue1, String inField2, String inValue2)throws StoreException;

	/**
	 * 
	 */
	public abstract void clearCustomers() throws StoreException;

	/**
	 * @param inCustomerDirectory
	 */
	public abstract void setCustomersDirectory(File inCustomerDirectory);

}