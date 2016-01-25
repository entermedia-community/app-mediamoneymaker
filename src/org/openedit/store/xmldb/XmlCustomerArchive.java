/*
 * Created on Oct 4, 2004
 */
package org.openedit.store.xmldb;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.store.CustomerArchive;
import org.openedit.store.StoreException;
import org.openedit.store.customer.Customer;
import org.openedit.users.User;
import org.openedit.users.UserManager;
import org.openedit.users.UserManagerException;
import org.openedit.users.UserSearcher;

/**
 * @author cburkey
 *
 */
public class XmlCustomerArchive implements CustomerArchive
{
	private static final Log log = LogFactory.getLog(CustomerArchive.class);
	
	protected Map fieldCustomers;
	protected UserManager fieldUserManager;
	
	protected UserSearcher fieldCustomerSearch;
	protected File fieldCustomerDirectory;
	
	public Customer getCustomer(String inId) throws StoreException
	{
		Customer cust = (Customer) getCustomers().get( inId );
		if ( cust == null )
		{
			try
			{
				User user = getUserManager().getUser( inId );
				if ( user != null )
				{
					cust = new Customer( user );
					getCustomers().put( inId, cust );
				}
			}
			catch ( UserManagerException ex )
			{
				throw new StoreException( ex );
			}
		}
		return cust;
	}
	
	public void saveCustomer(Customer inCustomer) throws StoreException
	{
		try
		{
			getUserManager().saveUser(inCustomer.getUser());
		}
		catch (UserManagerException ex)
		{
			log.error( ex );
			throw new StoreException( ex );
		}
	}
	
	public Map getCustomers()
	{
		if ( fieldCustomers == null)
		{
			fieldCustomers = new HashMap();
		}
		return fieldCustomers;
	}
	
	/* (non-javadoc)
	 * @see com.openedit.store.CustomerArchive#createNewCustomer()
	 */
	public Customer createNewCustomer( String inUsername, String inPassword ) throws StoreException
	{
		try
		{
			User user = null;
			if ( inUsername != null )
			{
				user = getUserManager().getUser(inUsername);
			}
			if ( inPassword == null )
			{
				inPassword = inUsername;
			}
			if ( user == null)
			{
				user = getUserManager().createUser( inUsername, inPassword );
			}
			Customer newCustomer = new Customer( user );
			getCustomers().put(newCustomer.getUserName(),newCustomer);
			return newCustomer;
		}
		catch ( Exception ex )
		{
			throw new StoreException( ex );
		}
	}
	
//	/* (non-javadoc)
//	 * @see com.openedit.store.CustomerArchive#findCustomer(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
//	 */
//	public List findCustomer(String inField1, String inValue1, String inField2, String inValue2) throws StoreException
//	{
//		//the search will handle the case problem
//		StringBuffer out = new StringBuffer();
//		out.append(inField1);
//		out.append(":");
//		out.append(inValue1);
//
//		out.append(" +");
////
////		//remove any - or () from phone number
//		out.append(inField2);
//		out.append(":");
//		try
//		{
//			String clean = clean(inValue2);
//			out.append(clean);
//			List customers = findUser( out.toString() );
//			return customers;
//		}
//		catch ( Exception ex )
//		{
//			throw new StoreException(ex);
//		}
//	}
	
	/* (non-javadoc)
	 * @see com.openedit.store.CustomerArchive#clearCustomers()
	 */
	public void clearCustomers() throws StoreException
	{
		getCustomers().clear();
	}

	/* (non-javadoc)
	 * @see com.openedit.store.CustomerArchive#setCustomersDirectory(java.io.File)
	 */
	public void setCustomersDirectory(File inCustomerDirectory)
	{
		fieldCustomerDirectory = inCustomerDirectory;
	}
	
	public File getCustomerDirectory()
	{
		return fieldCustomerDirectory;
	}
	
	
		
	/**
	 * @param inValue2
	 * @return
	 */
	public String clean(String inValue2)
	{
		if ( inValue2 == null)
		{
			return null;
		}
		String clean = inValue2.replaceAll("-","");
		clean = clean.replaceAll("\\(","");
		clean = clean.replaceAll("\\)","");
		clean = clean.replaceAll("\\ ","");

		return clean;
	}
	
	public UserManager getUserManager()
	{
		return fieldUserManager;
	}
	
	public void setUserManager( UserManager inUserManager )
	{
		fieldUserManager = inUserManager;
	}

	public void saveAndExportCustomer(Customer inC) throws StoreException
	{
		saveCustomer(inC);
		//no need to export anything else
	}
}
