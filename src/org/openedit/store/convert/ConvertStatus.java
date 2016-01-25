package org.openedit.store.convert;

import java.util.ArrayList;
import java.util.List;

import org.openedit.store.Product;
import org.openedit.users.User;


public class ConvertStatus
{
	protected boolean fieldReindex;
	protected boolean fieldForcedConvert;
	protected boolean fieldInputProcessed;
	
	protected List fieldLog;
	protected Exception fieldException;
	protected List fieldInputs;
	protected String fieldDestinationDirectory;
	protected List fieldConvertedProducts;
	protected User fieldUser;
	
	public User getUser()
	{
		return fieldUser;
	}
	public void setUser(User inUser)
	{
		fieldUser = inUser;
	}
	public boolean isReindex()
	{
		return fieldReindex;
	}
	public void setReindex(boolean inReindex)
	{
		fieldReindex = inReindex;
	}
	public List getLog()
	{
		if (fieldLog == null)
		{
			fieldLog = new ArrayList();
		}
		return fieldLog;
	}
	public void setLog(List inLog)
	{
		fieldLog = inLog;
	}
	public void add(String inString)
	{
		getLog().add(inString);
		
	}
	public Exception getException()
	{
		return fieldException;
	}
	public void setException(Exception inException)
	{
		fieldException = inException;
	}
	public boolean isForcedConvert()
	{
		return fieldForcedConvert;
	}
	public void setForcedConvert(boolean inForcedConvert)
	{
		fieldForcedConvert = inForcedConvert;
	}
	public List getInputs()
	{
		if (fieldInputs == null)
		{
			fieldInputs = new ArrayList();
		}

		return fieldInputs;
	}
	public void setInputs(List inInputs)
	{
		fieldInputs = inInputs;
	}
	public void addInput(String inInput)
	{
		getInputs().add(inInput);
	}
	public boolean isInputProcessed()
	{
		return fieldInputProcessed;
	}
	public void setInputProcessed(boolean inInputProcessed)
	{
		fieldInputProcessed = inInputProcessed;
	}

	public List getConvertedProducts()
	{
		if (fieldConvertedProducts == null)
		{
			fieldConvertedProducts = new ArrayList();
			
		}
		return fieldConvertedProducts;
	}
	
	public void addConvertedProducts( List inProducts )
	{
		getConvertedProducts().addAll(inProducts);
	}
	public void addConvertedProduct( Product inProduct )
	{
		getConvertedProducts().add(inProduct);
	}
	public String getDestinationDirectory()
	{
		return fieldDestinationDirectory;
	}
	public void setDestinationDirectory(String inDestinationDirectory)
	{
		fieldDestinationDirectory = inDestinationDirectory;
	}
	

}
