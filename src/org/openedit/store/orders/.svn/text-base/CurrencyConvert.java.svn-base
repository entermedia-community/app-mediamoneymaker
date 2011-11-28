/*
 * Created on Mar 7, 2005
 */
package org.openedit.store.orders;

import org.openedit.money.Fraction;

/**
 * @author cburkey
 *
 */
public class CurrencyConvert
{
	public Fraction getMultiplier(String inSourceCountry, String inDesctination) throws Exception
	{
		  String UrlString = "http://services.xmethods.net:9090/soap";
		  String nameSpaceUri = "urn:xmethods-CurrencyExchange";
          
          // create reference ot service
		  /*
          ExchangeRate exchange = (ExchangeRate)
              soaprmi.soaprpc.SoapServices.getDefault().createStartpoint(
              UrlString,  // service location
              new Class[]{ExchangeRate.class}, // remote service interface
              nameSpaceUri, // endpoint name
              soaprmi.soap.SoapStyle.SOAP11,
              "" // SOAPAction
          );
          
          float change = exchange.getRate(inSourceCountry,inDesctination);
          return new Fraction(change);
          */
		  return null;
	}
	
	public interface ExchangeRate
	{

		float getRate(String inSource, String inDest);
	}
	
}
