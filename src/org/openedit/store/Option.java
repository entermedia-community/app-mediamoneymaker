/*
 * Created on Nov 4, 2004
 */
package org.openedit.store;

import org.openedit.money.Money;

/**
 * @author dbrown
 *
 */
public class Option implements Comparable
{
	protected String fieldName;
	protected String fieldId; //Must be Unique
	protected String fieldValue;
	protected String fieldDataType;
	
	protected PriceSupport fieldPriceSupport;
	protected boolean fieldRequired = false;

	public Option()
	{
	}
	public Option(String inId, String inValue)
	{
		setId(inId);
		setName(inId);
		setValue(inValue);
	}
	
	public Option copy()
	{
		Option o=new Option();
		o.setDataType(getDataType());
		o.setId(getId());
		o.setName(getName());
		o.setPriceSupport(getPriceSupport());
		//o.setPrice(getPriceSupport().getYourPriceByQuantity(1).toShortString());
		o.setRequired(isRequired());
		o.setValue(getValue());
		return o;
	}
	
	public String getId()
	{
		return fieldId;
	}
	
	public void setPrice (String inPrice)
	{
		fieldPriceSupport = null;
		Price price=new Price();
		price.setRetailPrice(new Money(inPrice));
		addTierPrice(1, price);
	}
	
	public void setId(String inId)
	{
		fieldId = inId;
	}
	public String getName()
	{
		if (fieldName == null)
		{
			fieldName = getId();
		}
		return fieldName;
	}
	public void setName(String inName)
	{
		fieldName = inName;
	}
	public PriceSupport getPriceSupport()
	{
		return fieldPriceSupport;
	}
	public void setPriceSupport(PriceSupport inPriceSupport)
	{
		fieldPriceSupport = inPriceSupport;
	}

	public void addTierPrice( int inQuantity, Price inPrice )
	{
		if (getPriceSupport() == null)
		{
			setPriceSupport(new PriceSupport());
		}
		getPriceSupport().addTierPrice( inQuantity, inPrice );
	}

	public boolean isRequired()
	{
		return fieldRequired;
	}
	public void setRequired(boolean inRequired)
	{
		fieldRequired = inRequired;
	}

	public String getValue()
	{
		return fieldValue;
	}

	public void setValue(String inValue)
	{
		fieldValue = inValue;
	}
	public String toString()
	{
		if( getValue() != null)
		{
			return getValue();
		}
		else
		{
			return "";
		}
	}
	public boolean equals( Object inObj)
	{
		if( inObj instanceof Option)
		{
			Option in = (Option)inObj;
			boolean ids =  in.getId().equals(getId());
			if( ids )
			{
				if( in.getValue() == getValue() )
				{
					return true;
				}
				if( in.getValue() != null && in.getValue().equalsIgnoreCase( getValue() ) ) 
				{
					return true;
				}
			}
		}
		return false;
	}
	public int compareTo(Object inO)
	{
		Option in = (Option)inO;
		return in.getName().compareTo(in.getName());
	}
	public String getDataType() {
		return fieldDataType;
	}
	public void setDataType(String inGroupId) {
		fieldDataType = inGroupId;
	}
}
