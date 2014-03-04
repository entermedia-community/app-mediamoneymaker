/*
 * Created on May 26, 2004
 */
package org.openedit.store;

import java.util.Collection;
import java.util.Map;

import org.dom4j.Element;
import org.openedit.money.Money;


/**
 * @author cburkey
 *
 */
public interface ShippingMethod 
{

	public String getDescription() ;
	public void setDescription(String inDescription);
	public String getId() ;
	public Money getCost(Cart inCart);
	public Money getCost();

	public Map getHandlingCharges();

	public HandlingCharge getHandlingCharge(String inLevel);
	public boolean applies(Cart inCart);
	boolean isHidden();
	public void configure(Element inElement);
	public void addHandlingCharge(HandlingCharge inHandlingCharge);
	public Collection getHints(Cart inCart);
	
	public boolean isRefundable();
	
}
