package org.openedit.store.orders;

import org.openedit.data.BaseData;
import org.openedit.store.CartItem;

public class ShipmentEntry extends BaseData{

	protected int quantity;
	protected CartItem cartItem;
	
	
	public int getQuantity() {
		return quantity;
	}
	public void setQuantity(int inQuantity) {
		quantity = inQuantity;
	}
	
	public CartItem getItem() {
		if (cartItem == null) {
			cartItem = new CartItem();
		}
		return cartItem;
	}
	public void setCartItem(CartItem inCartItem) {
		cartItem = inCartItem;
	}
	
	public String getWaybill() {
		return get("waybill");
	}
	public void setWaybill(String inWaybill) {
		setProperty("waybill", inWaybill);
	}
	
	public String getPerson() {
	return get("person");
	}
	public void setPerson(String inPerson) {
		setProperty("person", inPerson);
	}
}
