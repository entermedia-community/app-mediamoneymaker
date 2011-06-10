/*
 * Created on Nov 19, 2004
 */
package org.openedit.store.customer;

import com.openedit.users.PropertyContainer;

/**
 * @author dbrown
 *
 */
public class Address
{
	protected PropertyContainer fieldPropertyContainer;
	protected String fieldPrefix;
	public static final String ADDRESS1 = "Address1";
	public static final String ADDRESS2 = "Address2";	
	public static final String CITY = "City";
	public static final String STATE = "State";
	public static final String COUNTRY = "Country";
	public static final String ZIP = "ZipCode";
	public static final String DESCRIPTION = "Description";
	

	public Address()
	{
	}

	public Address(PropertyContainer inPropertyContainer)
	{
		setPropertyContainer(inPropertyContainer);
	}

	protected String getProperty(String inPropertyName)
	{
		return (String)getPropertyContainer().get(getPrefix() + inPropertyName);
	}

	protected void putProperty(String inPropertyName, String inValue)
	{
		getPropertyContainer().safePut(getPrefix() + inPropertyName, inValue);
	}

	public String getPrefix()
	{
		if ( fieldPrefix == null )
		{
			fieldPrefix = "";
		}
		return fieldPrefix;
	}
	public void setPrefix(String inPrefix)
	{
		fieldPrefix = inPrefix;
	}

	public String getAddress1()
	{
		return getProperty(ADDRESS1);
	}
	public void setAddress1(String inAddress1)
	{
		putProperty(ADDRESS1, inAddress1);
	}
	public String getAddress2()
	{
		return getProperty(ADDRESS2);
	}
	public void setAddress2(String inAddress2)
	{
		putProperty(ADDRESS2, inAddress2);
	}
	public String getCity()
	{
		return getProperty(CITY);
	}
	public void setCity(String inCity)
	{
		putProperty(CITY, inCity);
	}
	public String getCountry()
	{
		return getProperty(COUNTRY);
	}
	public void setCountry(String inCountry)
	{
		putProperty(COUNTRY, inCountry);
	}
	public String getState()
	{
		return getProperty(STATE);
	}
	public void setState(String inState)
	{
		putProperty(STATE, inState);
	}
	public String getZipCode()
	{
		return getProperty(ZIP);
	}
	public String get5DigitZipCode()
	{
		String zip = getProperty(ZIP);
		if (getCountry().equals("USA") && zip.length() > 5)
		{
			zip = zip.substring(0,5);
		}
		return zip;
	}

	public void setZipCode(String inZipCode)
	{
		putProperty(ZIP, inZipCode);
	}

	public PropertyContainer getPropertyContainer()
	{
		return fieldPropertyContainer;
	}
	public void setPropertyContainer(PropertyContainer inPropertyContainer)
	{
		fieldPropertyContainer = inPropertyContainer;
	}

	public void setCityState( String inCityState )
	{
		if ( inCityState != null && inCityState.indexOf(",") > -1)
		{
			String[] both = inCityState.split(",");
			setCity(both[0]);
			setState(both[1]);
		}
	}
	public String getCityState()
	{
		return getCity() + ", " + getState();
	}
	public String getDescription()
	{
		return getProperty(DESCRIPTION);
	}
	public void setDescription(String inDescription)
	{
		putProperty(DESCRIPTION, inDescription);
	}
	
	public String toString()
	{
		return super.toString() + " prefix: " + getPrefix();
	}
}
