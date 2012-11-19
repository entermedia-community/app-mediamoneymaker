package edi;

public class OutputUtilities {
	
	public String createTable( String section, String value, String status) {
		String out = "<table border=0 cellpadding=5 cellspacing=3>\n";
		out += "<th><tr>\n";
		out += " <td align=center>\n";
		out += "  <table border=1 cellpadding=3 cellspacing=3 width=100%>\n";
		out += "   <tr>\n";
		out += "     <td class=header><strong>" + section + "</strong></td>";
		out += "   </tr>\n";
		out += "  </table>";
		out += " </td>";
		out += " <td align=center>\n";
		out += "  <table border=1 cellpadding=3 cellspacing=3 width=100%>\n";
		out += "   <tr>\n";
		out += "     <td align=center><strong>" + value + "</strong></td>";
		out += "   </tr>\n";
		out += "  </table>";
		out += " </td>";
		out += " <td align=center>\n";
		out += "  <table border=1 cellpadding=3 cellspacing=3 width=100%>\n";
		out += "   <tr>\n";
		out += "     <td align=center><strong>" + status + "</strong></td>";
		out += "   </tr>\n";
		out += "  </table>";
		out += " </td>";
		out += "</tr></th>\n";
		out += "<tbody>\n";
		return out;
	}
	public String finishTable() {
		String out = "</tbody>\n";
		out += "</table>\n";
		return out;
	}

	public String appendOutMessage( String inHeading, String inValue, String inStatus ) 
	{
		String out = "<tr>";
		out += "  <td><strong>" + inHeading + "</strong></td>\n";
		out += "  <td>" + inValue + "</td>\n";
		out += "  <td>" + inStatus + "</td>\n";
		out += "</tr>\n";
		return out;
	}
	public String appendOutMessage( String inValue )
	{
		String out = "<tr><td colspan=3>\n";
		out += "  <table border=1 cellpadding=3 cellspacing=3 width=100%>\n";
		out += "   <tr>\n";
		out += "     <td align=center><strong>" + inValue + "</strong></td>";
		out += "   </tr>\n";
		out += "  </table>";
		out += "</td></tr>\n";
		return out;
	}
	public String appendList( String inValue ) {
		return "<li>" + inValue + "</li>\n";
	}

}
