package org.openedit.store.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

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
	    DecimalFormat formatter = new DecimalFormat("###,###,##0.00", new DecimalFormatSymbols(Locale.ENGLISH));
	    String output = formatter.format(price);
	    return output;
	}

//	private static String priceWithoutDecimal (Double price) {
//	    DecimalFormat formatter = new DecimalFormat("###,###,###.##");
//	    return formatter.format(price);
//	}
}
