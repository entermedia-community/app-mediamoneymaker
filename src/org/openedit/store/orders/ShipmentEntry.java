package org.openedit.store.orders;

import org.openedit.data.BaseData;

public class ShipmentEntry extends BaseData{

	protected int quantity;
	protected String sku;
	
	
	public int getQuantity() {
		return quantity;
	}
	public void setQuantity(int inQuantity) {
		quantity = inQuantity;
	}
	
	public String getSku(){
		return sku;
	}
	public void setSku(String sku){
		this.sku = sku;
	}
	
	public String toString(){
		StringBuilder buf = new StringBuilder();
		buf.append(getSku()).append(":").append(getQuantity());
		return buf.toString();
	}
	
}
