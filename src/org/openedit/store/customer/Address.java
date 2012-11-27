/*
 * Created on Nov 19, 2004
 */
package org.openedit.store.customer;

import org.openedit.xml.ElementData;

import com.openedit.users.PropertyContainer;

/**
 * @author dbrown
 *
 */
public class Address extends ElementData
{
	//protected PropertyContainer fieldPropertyContainer;
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
		return get(ADDRESS1);
	}
	public void setAddress1(String inAddress1)
	{
		setProperty(ADDRESS1, inAddress1);
	}
	public String getAddress2()
	{
		return get(ADDRESS2);
	}
	public void setAddress2(String inAddress2)
	{
		setProperty(ADDRESS2, inAddress2);
	}
	public String getCity()
	{
		return get(CITY);
	}
	public void setCity(String inCity)
	{
		setProperty(CITY, inCity);
	}
	public String getCountry()
	{
		return get(COUNTRY);
	}
	public void setCountry(String inCountry)
	{
		setProperty(COUNTRY, inCountry);
	}
	public String getState()
	{
		return get(STATE);
	}
	public void setState(String inState)
	{
		setProperty(STATE, inState);
	}
	public String getZipCode()
	{
		return get(ZIP);
	}
	public String get5DigitZipCode()
	{
		String zip = get(ZIP);
		if (getCountry().equals("USA") && zip.length() > 5)
		{
			zip = zip.substring(0,5);
		}
		return zip;
	}

	public void setZipCode(String inZipCode)
	{
		setProperty(ZIP, inZipCode);
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
		return get(DESCRIPTION);
	}
	public void setDescription(String inDescription)
	{
		setProperty(DESCRIPTION, inDescription);
	}
	
	public String toString()
	{
		return super.toString();
	}
}
