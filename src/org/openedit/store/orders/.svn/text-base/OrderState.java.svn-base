/*
 * Created on Nov 2, 2004
 */
package org.openedit.store.orders;

/**
 * @author dbrown
 *
 */
public class OrderState
{
	protected boolean fieldOk;
	protected String fieldId;
	
	protected String fieldDescription;
	public String getDescription()
	{
		if ( fieldDescription == null)
		{
			return "No Order Details Available";
		}
		return fieldDescription;
	}
	public void setDescription(String inDescription)
	{
		fieldDescription = inDescription;
	}
	public boolean isOk()
	{
		return fieldOk;
	}
	public void setOk(boolean inOk)
	{
		fieldOk = inOk;
	}

	public String toString()
	{
		return getDescription();
	}
	public String getId()
	{
		return fieldId;
	}
	public void setId(String inId)
	{
		fieldId = inId;
	}
	public OrderState copy()
	{
		OrderState state = new OrderState();
		state.setId( getId());
		state.setOk(isOk());
		state.setDescription(getDescription());
		return state;
	}
}
