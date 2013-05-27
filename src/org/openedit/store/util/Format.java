package org.openedit.store.util;

import java.text.DecimalFormat;

public class Format {
	
	public Format() {
		//Empty Constructor
	}
	
	public String formatString( String inValue ) {
		String val = "";
		if (inValue != null) {
			double price = Double.parseDouble(inValue);
			val = priceWithDecimal(price);
//		    if (val.indexOf(".") > 0) {
//		        val = priceWithDecimal(price);
//		    } else {
//		        val = priceWithoutDecimal(price);
//		    }
		}
		return val;
	}
	
	private static String priceWithDecimal (Double price) {
	    DecimalFormat formatter = new DecimalFormat("###,###,###.00");
	    return formatter.format(price);
	}

//	private static String priceWithoutDecimal (Double price) {
//	    DecimalFormat formatter = new DecimalFormat("###,###,###.##");
//	    return formatter.format(price);
//	}
}
