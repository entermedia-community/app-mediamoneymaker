package org.openedit.store.orders;

import java.util.ArrayList;
import java.util.Iterator;

import org.openedit.data.BaseData;

public class Shipment extends BaseData{

	protected ArrayList<ShipmentEntry> fieldShipmentEntries;
	
	public ArrayList<ShipmentEntry> getShipmentEntries() {
		if (fieldShipmentEntries == null) {
			fieldShipmentEntries = new ArrayList<ShipmentEntry>();
		}
		return fieldShipmentEntries;
	}

	public void setShipmentEntries(ArrayList<ShipmentEntry> inShipmentEntry) {
		fieldShipmentEntries = inShipmentEntry;
	}

	
	
	public void addEntry(ShipmentEntry inEntry) {
		getShipmentEntries().add(inEntry);
	}
	
	public ShipmentEntry getEntryForSku(String inSku){
		for (Iterator iterator = getShipmentEntries().iterator(); iterator.hasNext();) {
			ShipmentEntry entry = (ShipmentEntry) iterator.next();
			if(inSku.equals(entry.getItem().getSku())){
				return entry;
				
			}
		}
		return null;
	}
}
