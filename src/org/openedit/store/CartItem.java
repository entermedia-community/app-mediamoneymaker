/*
 * Created on Nov 4, 2004
 */
package org.openedit.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.collections.map.ListOrderedMap;
import org.openedit.data.BaseData;
import org.openedit.money.Fraction;
import org.openedit.money.Money;
import org.openedit.store.orders.RefundState;
import org.openedit.store.orders.Shipment;


/**
 * @author dbrown
 *
 */
public class CartItem extends BaseData
{	
	protected int fieldQuantity = 1;
	protected Money fieldYourPrice;
	protected InventoryItem fieldInventoryItem;
	protected Boolean fieldBackOrdered;
	protected Map fieldOptions;
	protected Map fieldProperties;
	protected Product fieldProduct;
	protected String fieldShippingPrefix = "shipping";
	protected String fieldStatus;
	protected List fieldShipments;
	protected RefundState fieldRefundState;
	
	public CartItem()
	{
		super();
	}

	public void addOption( Option inOption )
	{
		getOptionsMap().put( inOption.getId(), inOption );
	}
	public void setOptions( Set inOptions )
	{
		getOptionsMap().clear();
		for (Iterator iter = inOptions.iterator(); iter.hasNext();)
		{
			Option obj = (Option) iter.next();
			addOption(obj);
		}
	}
	public void removeOption( Option inOption )
	{
		getOptionsMap().remove( inOption.getId() );
	}

	public Money getTotalPrice()
	{
		return getYourPrice().multiply( getQuantity() );
	}

	public Money getYourPrice()
	{
		if ( fieldYourPrice != null)
		{
			return fieldYourPrice;
		}
		if( getInventoryItem() == null)
		{
			return null;
		}
		int quantity = getQuantity();
		Money price = Money.ZERO;
		Money priceByQuantity = getInventoryItem().getYourPriceByQuantity(quantity);
		if (priceByQuantity != null)
		{
			price = price.add(priceByQuantity);
		}
		for ( Iterator it = getOptions().iterator(); it.hasNext();)
		{
			Option option = (Option)it.next();
			PriceSupport prices = option.getPriceSupport();
			if(  prices != null)
			{
				price = prices.getYourPriceByQuantity(quantity).add(price);
			}
		}
		return price;
	}
	public void setYourPrice(Money inMoney)
	{
		fieldYourPrice = inMoney;
	}
	public int getQuantity()
	{
		return fieldQuantity;
	}

	public void setQuantity(int quantity) throws StoreException
	{
		fieldQuantity = quantity;
		
		/* Set the cart's backorder quantity.  This is for
		informational purposes only.  No inventory quantities
		are being adjusted (that is done at checkout).  A cart
		item's backorder quantity indicates the portion of the 
		cart item's quantity that is unavailable (not in stock),
		at the time	the cart item's quantity was last changed.*/
		/*
		InventoryItem inventoryItem = getInventoryItem();
		if ( inventoryItem != null )
		{
			int quantityInStock = inventoryItem.getQuantityInStock();
			int remainingStock = quantityInStock - quantity;
			if (remainingStock < 0)
			{
				setBackOrderQuantity(-remainingStock);
			}
			else
			{
				setBackOrderQuantity(0);
			}
		}
		*/
	}
	public Map getOptionsMap()
	{
		if ( fieldOptions == null )
		{
			//	Options are always sorted so that SKUs can be
			//	easily uniquely generated from a set of options.
			fieldOptions =  ListOrderedMap.decorate(new HashMap());
		}
		return fieldOptions;
	}

	public Collection getOptions()
	{
		return getOptionsMap().values();
	}

	public Set getOtherOptions()
	{
		//everything but size and color
		Set rest = new TreeSet( new Comparator()
		{
			public int compare(Object arg0, Object arg1)
			{
				Option opt0 = (Option)arg0;
				Option opt1 = (Option)arg1;
				return opt0.getId().compareTo( opt1.getId() );
			}
		}
		);
		
		for (Iterator iter = getOptions().iterator(); iter.hasNext();)
		{
			Option option = (Option) iter.next();
			if( !option.getId().equals("size") && !option.getId().equals("color") )
			{
				rest.add(option);
			}
		}
		return rest;
	}
	
	/**
	 * @param inRate
	 * @return
	 */
	public Money calculateTax(Fraction inRate)
	{
		if ( inRate == null)
		{
			return Money.ZERO;
		}
		return getYourPrice().multiply(inRate);
	}
	
	public InventoryItem getInventoryItem() 
	{
		if( fieldInventoryItem == null && fieldProduct != null && getProduct().getInventoryItemCount() > 0)
		{
			return getProduct().getInventoryItem(0);
		}
		return fieldInventoryItem;
	}
	
	public boolean isBackOrdered()
	{
		if ( fieldBackOrdered != null)
		{
			return getBackOrdered().booleanValue();
		}
		if( getInventoryItem().isBackOrdered() )
		{
			return true;
		}
		return getQuantity() > getInventoryItem().getQuantityInStock();
	}

	public Option getOption(String inId)
	{
		Option option = (Option)getOptionsMap().get(inId);
		if( option == null)
		{
			option = getInventoryItem().getOption(inId);
			if( option != null)
			{
				//make a copy?
			}
		}
		return option;
	}
	
	public boolean hasOption(String inId)
	{
		Option option = (Option)getOptionsMap().get(inId);
		if (option == null)
		{
			return false;
		}
		else
		{
			return true;
		}
	}
	
	public boolean hasOption(Option inOption)
	{
		Option option =  (Option)getOptionsMap().get(inOption.getId());
		if (option == null)
		{
			return false;
		}
		else
		{
			return option.equals(inOption);
		}
	}

	public void setInventoryItem(InventoryItem inInventoryItem)
	{
		fieldInventoryItem = inInventoryItem;
	}
	public String getSku()
	{
		String sku = getInventoryItem().getSku();
		//add options?
		
		return sku;
	}
	public boolean isSpecialRequest()
	{
		if( hasOption("specialrequest") )
		{
			return true;
		}
		return false;
	}

	public boolean isSize(String inSize)
	{
		Option option = getSize();
		if( option != null)
		{
			if ( inSize != null && inSize.equals(option.getValue()))
			{
				return true;
			}
		}
		if( option == null && inSize == null)
		{
			return true;
		}
		return false;
	}
	public boolean isColor(String inColor)
	{
		Option option = getColor();
		if( option != null)
		{
			if ( inColor != null && inColor.equals(option.getValue()))
			{
				return true;
			}
		}
		if( option == null && inColor == null)
		{
			return true;
		}
		return false;
	}


	public Option getSize()
	{
		return getOption("size");
	}
	public Option getColor()
	{
		return getOption("color");
	}

	public Product getProduct()
	{
		if( fieldProduct != null) //This is here in case someone wants to use product without inventory ie Archive
		{
			return fieldProduct;
		}
		if( getInventoryItem() != null)
		{
			return getInventoryItem().getProduct();
		}
		return null;
	}

	public void setProduct(Product inProduct)
	{
		fieldProduct = inProduct;
	}
	public Boolean getBackOrdered()
	{
		return fieldBackOrdered;
	}

	public void setBackOrdered(boolean inBackOrdered)
	{
		fieldBackOrdered = new Boolean(inBackOrdered);
	}

	


	

	public double getWeight()
	{
		return getInventoryItem().getWeight();
	}
	public String getName()
	{
		if( fieldInventoryItem == null || getInventoryItem().getProduct()==null)
		{
			return getProduct().getName();
		}
		return getInventoryItem().getName();
	}
	public String get(String inString)
	{
		if( getProperties() == null)
		{
			return getInventoryItem().get(inString);
		}
		String val = (String)getProperties().get(inString);
		if( val == null)
		{
			val = getInventoryItem().get(inString);
		}
		return val;
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

	public String getShippingPrefix()
	{
		return fieldShippingPrefix;
	}

	public void setShippingPrefix(String inShippingPrefix)
	{
		fieldShippingPrefix = inShippingPrefix;
	}

	public String getStatus()
	{
		return fieldStatus;
	}

	public void setStatus(String inStatus)
	{
		fieldStatus = inStatus;
	}

	public double getLenth() {
		return getInventoryItem().getLength();
	}

	public double getWidth() {
		return getInventoryItem().getWidth();
		
	}

	public double getHeight() {
		return getInventoryItem().getHeight();
		
	}
	public List<Shipment> getShipments() {
		if (fieldShipments == null) {
			fieldShipments = new ArrayList<Shipment>();
		}
		return fieldShipments;
	}

	public void setShipments(List<Shipment> inShipments) {
		fieldShipments = inShipments;
	}

	public RefundState getRefundState()
	{
		if (fieldRefundState == null)
		{
			fieldRefundState = new RefundState();
		}
		return fieldRefundState;
	}

	public void setRefundState(RefundState inRefundState)
	{
		fieldRefundState = inRefundState;
	}
}
