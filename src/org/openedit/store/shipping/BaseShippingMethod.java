/*
 * Created on May 26, 2004
 */
package org.openedit.store.shipping;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.openedit.money.Money;
import org.openedit.store.Cart;
import org.openedit.store.CartItem;
import org.openedit.store.HandlingCharge;
import org.openedit.store.ShippingMethod;


/**
 * @author cburkey
 *
 */
public abstract class BaseShippingMethod implements ShippingMethod  
{
	public static final boolean DEFAULT_REFUNDABLE = true;
	
	protected String fieldId = "";
	protected String fieldDescription = "";
	protected Map fieldHandlingCharges;

	protected Money fieldCost = Money.ZERO;
	protected double fieldPercentageCost = 0.0;

	//	thresholds that indicate when this shipping method can be used
	protected Money fieldLowerThreshold = Money.ZERO;
	protected Money fieldUpperThreshold = null;
	
	protected boolean fieldHidden;
	
	// add refundable field
	protected boolean fieldRefundable = DEFAULT_REFUNDABLE;//initialize it with something
	
	public Money getCost()
	{
		return fieldCost;
	}
	
	public void setCost(Money inCost) {
		fieldCost = inCost;
	}
	public Money getLowerThreshold()
	{
		return fieldLowerThreshold;
	}
	public void setLowerThreshold(Money inLowerThreshold)
	{
		fieldLowerThreshold = inLowerThreshold;
	}
	public Money getUpperThreshold()
	{
		return fieldUpperThreshold;
	}
	public void setUpperThreshold(Money inUpperThreshold)
	{
		fieldUpperThreshold = inUpperThreshold;
	}

	public double getPercentageCost()
	{
		return fieldPercentageCost;
	}

	public void setPercentageCost(double inPercentageCost)
	{
		fieldPercentageCost = inPercentageCost;
	}

	
	
	public String getDescription() {
		return fieldDescription;
	}
	public void setDescription(String inDescription) {
		fieldDescription = inDescription;
	}
	public String getId() {
		return fieldId;
	}
	public void setId(String inId) {
		fieldId = inId;
	}
	public Map getHandlingCharges()
	{
		if ( fieldHandlingCharges == null )
		{
			fieldHandlingCharges = new HashMap();
		}
		return fieldHandlingCharges;
	}

	public void setHandlingCharges(Map inHandlingCharges)
	{
		fieldHandlingCharges = inHandlingCharges;
	}

	public HandlingCharge getHandlingCharge(String inLevel)
	{
		return (HandlingCharge)getHandlingCharges().get(inLevel);
	}

	public void addHandlingCharge(HandlingCharge inHandlingCharge)
	{
		getHandlingCharges().put(inHandlingCharge.getLevel(), inHandlingCharge);
	}
	public Money getHandlingCharge(Cart inCart)
	{
		Money totalPrice = Money.ZERO;
		if( !inCart.isEmpty() )
		{
			//	add in any per-item handling charges
			for ( Iterator it = inCart.getItemIterator(); it.hasNext(); )
			{
				CartItem item = (CartItem)it.next();
				String handlingChargeLevel = item.getProduct().getHandlingChargeLevel();
				if ( handlingChargeLevel != null )
				{
					HandlingCharge handlingCharge = getHandlingCharge( handlingChargeLevel );
					if ( handlingCharge != null )
					{
						Money handlingCost = handlingCharge.getCost();
						handlingCost = handlingCost.multiply( item.getQuantity() );
						totalPrice = totalPrice.add( handlingCost );
					}
				}
			}
		}
		return totalPrice;
	}

	public boolean isHidden()
	{
		return fieldHidden;
	}

	public void setHidden(boolean inHidden)
	{
		fieldHidden = inHidden;
	}

	public boolean isRefundable()
	{
		return fieldRefundable;
	}

	public void setRefundable(boolean inRefundable)
	{
		fieldRefundable = inRefundable;
	}

}
