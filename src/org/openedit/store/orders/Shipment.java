package org.openedit.store.orders;

import java.util.ArrayList;
import java.util.Iterator;

import org.openedit.data.BaseData;
import org.openedit.util.DateStorageUtil;

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
		for (Iterator<ShipmentEntry> iterator = getShipmentEntries().iterator(); iterator.hasNext();) {
			ShipmentEntry entry = (ShipmentEntry) iterator.next();
			if(inSku!=null && inSku.equals(entry.getSku())){
				return entry;
			}
		}
		return null;
	}
	
	public boolean containsEntryForSku(String inSku){
		return getEntryForSku(inSku) != null;
		
	}
	public String get(String inId) {
		return (String) getProperties().get(inId);
	}
}
