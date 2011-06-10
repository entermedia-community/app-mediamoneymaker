/*
 * Created on Mar 4, 2004
 *
 */
package org.openedit.store;

/**
 * @author dbrown
 *
 */
public class CreditCardType implements Comparable
{
	protected String fieldName = "";
	protected String fieldId = "";
	
	public CreditCardType()
	{
	}

	public CreditCardType( String inName )
	{
		fieldName = inName;
	}

	public String getName()
	{
		if ( fieldName == null )
		{
			fieldName = "";
		}
		return fieldName;
	}

	public void setName(String inString)
	{
		fieldName = inString;
	}

	public String toString()
	{
		return fieldName;
	}

	public int compareTo(Object o)
	{
		if ( o instanceof CreditCardType )
		{
			return getName().compareTo( ((CreditCardType) o).getName() );
		}
		else
		{
			return -1;
		}
	}

	public boolean equals( Object o )
	{
		if ( o instanceof CreditCardType )
		{
			return getName().equals( ((CreditCardType) o).getName() );
		}
		else
		{
			return false;
		}
	}

	public int hashCode()
	{
		return getName().hashCode();
	}
	public String getId() {
		return fieldId;
	}
	public void setId(String inId) {
		fieldId = inId;
	}
}
