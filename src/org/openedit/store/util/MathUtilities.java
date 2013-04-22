package org.openedit.store.util;

public class MathUtilities {
	
	public MathUtilities() {
		//Empty Constructor
	}
	
	public double multiply( String inValue1, String inValue2 ) {
		double val = 0d;
		val = Double.parseDouble(inValue1) * Double.parseDouble(inValue2);
		return val;
	}
	
	public double add( String inValue1, String inValue2 ) {
		double val = 0d;
		val = Double.parseDouble(inValue1) + Double.parseDouble(inValue2);
		return val;
	}
	
	public double addArray( String[] inStrings ) {
		double val = 0d;
		if (inStrings != null) {
			for (String value : inStrings ) {
				if (value != null) {
					val = add(String.valueOf(val), value);
				}
			}
		}
		return val;
	}
}
