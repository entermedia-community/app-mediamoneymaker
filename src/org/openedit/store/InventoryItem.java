/*
 * Created on Nov 4, 2004
 */
package org.openedit.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.money.Money;


/**
 * This is a specific item that is held in stock
 *
 */
public class InventoryItem
{
	protected int fieldQuantityInStock;
	protected Map fieldProperties;
	protected Set fieldOptions;
	protected String fieldSku;
	protected Product fieldProduct;
	protected PriceSupport fieldPriceSupport;
	protected String fieldDescription;
	protected double fieldWeight;
	protected boolean fieldRefundable;
	
	public double getHeight() {
		return fieldHeight;
	}
	public void setHeight(double inHeight) {
		fieldHeight = inHeight;
	}
	public double getWidth() {
		return fieldWidth;
	}
	public void setWidth(double inWidth) {
		fieldWidth = inWidth;
	}
	public double getLength() {
		return fieldLength;
	}
	public void setLength(double inLength) {
		fieldLength = inLength;
	}

	protected double fieldHeight;
	protected double fieldWidth;
	protected double fieldLength;
	
	protected boolean fieldBackOrdered;
	private static final Log log = LogFactory.getLog(InventoryItem.class);
	
	public InventoryItem()
	{
		super();
	}
	public InventoryItem(String inSku)
	{
		setSku(inSku);
	}
	public Money getYourPrice()
	{
		return getYourPriceByQuantity(1);
	}
	public Money getYourPriceByQuantity(int i)
	{
		PriceSupport sup =  getPriceSupport();
		if ( sup == null )
		{
			sup = getProduct().getPriceSupport();
		}
		if ( sup == null)
		{
			return null;
		}
		return sup.getYourPriceByQuantity( i );
	}
	
	public int getQuantityInStock()
	{
		return fieldQuantityInStock;
	}

	public void setQuantityInStock(int inQuantityInStock)
	{
		fieldQuantityInStock = inQuantityInStock;
	}
	
	public boolean isInStock()
	{
		return fieldQuantityInStock == -1 || fieldQuantityInStock > 0;
	}
	
	public void increaseQuantityInStock(int inIncrease)
	{
		fieldQuantityInStock = fieldQuantityInStock + inIncrease;
	}
	
	public void decreaseQuantityInStock(int inDecrease)
	{
		if ( fieldQuantityInStock == -1 || fieldQuantityInStock == 0)
		{
			return;
		}
		fieldQuantityInStock = fieldQuantityInStock - inDecrease;
	}
	
	public Option getColor()
	{
		Option color = getOption("color");
		return color;
	}

	public Option getSize()
	{
		Option size = getOption("size");
		return size;
	}

	public void setSize(String inString)
	{
		Option opt = getLocalOption("size");
		if( opt == null)
		{
			opt = new Option();
			opt.setId("size");
			opt.setName("Size");
			Option parent = getOption("size");
			if( parent != null)
			{
				opt.setRequired(parent.isRequired());
				opt.setName(parent.getName());
				opt.setPriceSupport(parent.getPriceSupport());
			}
			
			addOption(opt);
		}
		opt.setValue(inString);
	}
	public void setColor(String inString)
	{
		Option opt = getLocalOption("color");
		if( opt == null)
		{
			opt = new Option();
			opt.setId("color");
			opt.setName("Color");
			Option parent = getOption("color");
			if( parent != null)
			{
				opt.setRequired(parent.isRequired());
				opt.setName(parent.getName());
				opt.setPriceSupport(parent.getPriceSupport());
			}
			addOption(opt);
		}
		opt.setValue(inString);
	}

	
	public void addOption(Option inOpt)
	{
		getOptions().add(inOpt);
	}

	public String getSku()
	{
		return fieldSku;
	}

	public void setSku( String sku )
	{
		fieldSku = sku;
	}

	public boolean hasSize()
	{
		Option size = getSize();
		if( size == null || size.getValue() == null )
		{
			return false;
		}
		return true;
	}

	public boolean hasColor()
	{
		Option option = getColor();
		if( option == null || option.getValue() == null )
		{
			return false;
		}
		return true;
	}

	public String toString()
	{
		return "[Item: " + getSku() + " " + getSize() + " " + getColor() + "]";
	}

	/**
	 * @param inCartItem
	 * @return
	 */
	public boolean isExactMatch( Collection inSomeOptions )
	{
		//all options must match
		for (Iterator iter = inSomeOptions.iterator(); iter.hasNext();)
		{
			Option option = (Option) iter.next();
			Option found = getLocalOption(option.getId());

			if( found == null)
			{
				//log.info("No option was found locally with id: " + option.getId());
				continue; //it is not specified
			}
			//log.info("Does " + option.getId() + ":" + option.getValue()
				//	+ " match " + found.getId() + ":" + option.getValue() + "?");
			
			if( option.equals(found) )
			{
				//log.info("Yes.");
				continue;
			}
			//log.info("No. Not a match");
			return false;
		}
		//log.info("We found a match.");
		return true;
	}
	
		
	public boolean isCloseMatch( Collection inOptions )
	{
		//look over all the required options and see if they all match
		for (Iterator iter = inOptions.iterator(); iter.hasNext();)
		{
			Option inCheck = (Option) iter.next();
			if( inCheck.isRequired() || inCheck.getId().equals("color") || inCheck.getId().equals("size"))
			{
				Option has = getLocalOption(inCheck.getId());
				if( has == null)
				{
					return false;
				}
				if( has.equals(inCheck))
				{
					continue;
				}
			}
		}
		return true;
	}	
	public boolean isAnyMatch(Collection inOptions)
	{
		for (Iterator iter = inOptions.iterator(); iter.hasNext();)
		{
			Option inCheck = (Option) iter.next();
			if( inCheck.getId().equals("color") || inCheck.getId().equals("size"))
			{
				Option has = getLocalOption(inCheck.getId());
				if( has != null && has.equals(inCheck))
				{
					return true;
				}
			}
		}
		for (Iterator iter = inOptions.iterator(); iter.hasNext();)
		{
			Option inCheck = (Option) iter.next();
			Option has = getLocalOption(inCheck.getId());
			if( has != null && has.equals(inCheck))
			{
				return true;
			}
		}
		return false;
	}

	protected boolean equals( Option inOption, Option inOther)
	{
		if( inOption == inOther)
		{
			return true;
		}
		if( inOption == null || inOther == null)
		{
			return false;
		}
		if( inOption.getValue() == inOther.getValue() )
		{
			return true;
		}
		if( inOption.getValue() == null || inOther.getValue() == null)
		{
			return false;
		}
		return inOption.getValue().equals(inOther.getValue());
		
	}
	protected Option findOption( String inId, Collection inOptions)
	{
		for (Iterator iterator = inOptions.iterator(); iterator.hasNext();)
		{
			Option found = (Option) iterator.next();
			if( found.getId().equals(inId))
			{
				return found;
			}
		}
		return null;

	}
	
	public Money getRetailPrice()
	{
		//return getPriceSupport().getPrice();
		if (getPriceSupport() != null)
		{
			return getPriceSupport().getRetailPrice();
		}
		else
		{
			return Money.ZERO;
		}
		
	}

	public boolean isOnSale()
	{
		if( getPriceSupport() == null)
		{
			return false;
		}
		return getPriceSupport().isOnSale();
	}


	public void put( String inKey, String inValue )
	{
		if( inKey.equals("size"))
		{
			setSize(inValue);
		}
		else
		{
			getProperties().put( inKey, inValue );
		}
	}

	public String get( String inkey )
	{
		String value = (String) getProperties().get( inkey );
		if( value == null && getProduct() != null)
		{
			value = getProduct().get(inkey); //Check the product and catalogs
		}
		return value;
	}
	public String getProperty(String inKey)
	{
		return (String)getProperties().get(inKey);
	}
	public Map getProperties()
	{
		if (fieldProperties == null)
		{
			fieldProperties = new HashMap();
		}

		return fieldProperties;
	}
	
	public void addProperty(String inKey, String inValue)
	{
		if ( inValue == null || inValue.length() == 0)
		{
			getProperties().remove(inKey);
		}
		else
		{
			getProperties().put( inKey , inValue);
		}
	}

	public void setProperties( Map inAttributes )
	{
		fieldProperties = inAttributes;
	}

	public String getName()
	{
		return getProduct().getName();
	}

	public Product getProduct()
	{
		return fieldProduct;
	}

	public void setProduct( Product product )
	{
		fieldProduct = product;
	}

	public PriceSupport getPriceSupport()
	{
		return fieldPriceSupport;
	}

	public void setPriceSupport( PriceSupport priceSupport )
	{
		fieldPriceSupport = priceSupport;
	}
	/**
	 * @return
	 */
	public List getTiers()
	{
		return getPriceSupport().getTiers();
	}
	/**
	 * @param inQuantity
	 * @param money
	 */
	public void addTierPrice( int inQuantity, Price inPrice  )
	{
		if( fieldPriceSupport == null)
		{
			fieldPriceSupport = new PriceSupport();
		}
		getPriceSupport().addTierPrice( inQuantity, inPrice );
	}

	/**
	 * @return
	 */
	public boolean hasOwnPrice()
	{
		return fieldPriceSupport != null && fieldPriceSupport.getTiers().size()> 0;
	}

	/**
	 * @param inI
	 * @return
	 */
	public PriceTier getTier(int inI)
	{
		if ( fieldPriceSupport == null || getPriceSupport().getTiers() == null || getPriceSupport().getTiers().size() <= inI)
		{
			return null;
		}
		
		return (PriceTier)getPriceSupport().getTiers().get(inI);
	}

	public String getDescription()
	{
		return fieldDescription;
	}
	public void setDescription(String inDescription)
	{
		fieldDescription = inDescription;
	}
	public List getPropertiesStartingWith(String inKey)
	{
		List keys = new ArrayList();
		for (Iterator iter = getProperties().keySet().iterator(); iter.hasNext();)
		{
			String key = (String) iter.next();
			if ( key.startsWith(inKey))
			{
				keys.add(key);
			}
		}
		Collections.sort(keys);
		List values = new ArrayList();
		for (Iterator iter = keys.iterator(); iter.hasNext();)
		{
			String key = (String) iter.next();
			values.add(getProperty(key));
		}
		return values;
	}

	public double getWeight()
	{
		return fieldWeight;
	}

	public void setWeight(double inWeight)
	{
		fieldWeight = inWeight;
	}

	public boolean isBackOrdered()
	{
		return fieldBackOrdered;
	}

	public void setBackOrdered(boolean inBackOrdered)
	{
		fieldBackOrdered = inBackOrdered;
	}
	
	public void clearOptions()
	{
		getOptions().clear();
	}
	
	public Set getOptions()
	{
		if (fieldOptions == null)
		{
			fieldOptions = new TreeSet( new Comparator()
			{
				public int compare(Object arg0, Object arg1)
				{
					Option opt0 = (Option)arg0;
					Option opt1 = (Option)arg1;
					return opt0.getId().compareTo( opt1.getId() );
				}
			}
			);
		}
		return fieldOptions;
	}
	
	public List getAllOptions()
	{
		Map optionsMap = new HashMap();
		List productOptions = getProduct().getAllOptions();

		for (Iterator iter = productOptions.iterator(); iter.hasNext();)
		{
			Option option = (Option) iter.next();
			optionsMap.put(option.getId(), option);
		}
		
		for (Iterator iter = getOptions().iterator(); iter.hasNext();)
		{
			Option option = (Option) iter.next();
			optionsMap.put(option.getId(), option);
		}
		
		List allOptions = new ArrayList();
		allOptions.addAll(new ArrayList(optionsMap.values()));
		Collections.sort(allOptions, new Comparator() 
		{
			public int compare(Object inO1, Object inO2)
			{
				Option one = (Option)inO1;
				Option two = (Option)inO2;
				return one.getName().compareTo(two.getName());
			}
		});
		
		return allOptions;
	}

	public void setOptions(Set inOptions)
	{
		fieldOptions = inOptions;
	}
	public Option getLocalOption( String inId)
	{
		for (Iterator iter = getOptions().iterator(); iter.hasNext();)
		{
			Option option = (Option ) iter.next();
			if( option.getId().equals(inId))
			{
				return option;
			}
		}
		return null;
	}
	
	public Option getOption(String inId)
	{
		Option option = getLocalOption(inId);
		if( option == null)
		{
			if( fieldProduct == null)
			{
				return null;
			}
			return getProduct().getOption(inId);
		}
		return option;
	}
	
	public boolean isRefundable()
	{
		return fieldRefundable;
	}
	
	public void setRefundable(boolean inRefundable)
	{
		this.fieldRefundable = inRefundable;
	}

	public InventoryItem copy() {
		InventoryItem item = new InventoryItem();
		item.fieldBackOrdered=fieldBackOrdered;
		item.fieldDescription=fieldDescription;
		item.fieldOptions=null;
		Iterator i = getOptions().iterator();
		while (i.hasNext())
		{
			item.addOption( ((Option) i.next()).copy() );
		}
		
		item.fieldPriceSupport=getPriceSupport();
		item.fieldProduct=fieldProduct;
		item.fieldProperties=null;
		i = getProperties().keySet().iterator();
		while (i.hasNext())
		{
			String key = (String) i.next();
			item.addProperty(key, getProperty(key));
		}

		item.fieldQuantityInStock=fieldQuantityInStock;
		item.fieldSku=fieldSku;
		item.fieldWeight=fieldWeight;
		return item;
	}
	

}
