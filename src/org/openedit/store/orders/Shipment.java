package org.openedit.store.orders;

import java.util.ArrayList;

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
}
