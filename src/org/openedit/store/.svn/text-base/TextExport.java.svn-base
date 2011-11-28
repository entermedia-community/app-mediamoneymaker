/*
 * Created on Feb 15, 2005
 */
package org.openedit.store;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.dom4j.DocumentException;
import org.dom4j.Element;

/**
 * @author Matthew Avery, mavery@einnovation.com
 */
public class TextExport
{
	protected String fieldDelimiter = "\t";
	protected Dom4JHelper fieldHelper;

	protected Dom4JHelper getHelper()
	{
		if( fieldHelper == null )
		{
			fieldHelper = new Dom4JHelper();
		}
		return fieldHelper;
	}
	
	protected Element getRootElement( File inFile, String inName ) throws IOException, DocumentException
	{
		return getHelper().getRootElement( inFile, inName );
	}
	
	public void exportOrders( File inOrderFile, File inOrderExportFile ) throws Exception
	{
		
		Element root = getRootElement( inOrderFile, "orders" );
		FileWriter writer = new FileWriter( inOrderExportFile );
		StringBuffer header = new StringBuffer();
		header.append( "order_number" );
		header.append( getDelimiter() );
		header.append( "shipping_method" );
		header.append( getDelimiter() );
		header.append( "subtotal" );
		header.append( getDelimiter() );
		header.append( "shipping_cost" );
		header.append( getDelimiter() );
		header.append( "customerid" );
		header.append( getDelimiter() );
		header.append( "payment_type" );
		header.append( getDelimiter() );
		header.append( "card_type" );
		header.append( getDelimiter() );
		header.append( "card_number" );
		header.append( getDelimiter() );
		header.append( "expiration_date" );
		writer.write( header.toString() + "\n");
		for ( Iterator iter = root.elementIterator("order"); iter.hasNext(); )
		{
			Element orderElement = (Element) iter.next();
			StringBuffer sb = new StringBuffer();
			sb.append( orderElement.attributeValue( "order_number" ) );
			sb.append( getDelimiter() );
			sb.append( orderElement.attributeValue( "shipping_method" ) );
			sb.append( getDelimiter() );
			sb.append( orderElement.attributeValue( "subtotal" ) );
			sb.append( getDelimiter() );
			sb.append( orderElement.attributeValue( "shipping_cost" ) );
			sb.append( getDelimiter() );
			Element customerElement = orderElement.element( "customer" );
			sb.append( customerElement.attributeValue("customerid"));
			sb.append( getDelimiter() );
		
			
			Element paymentElement = orderElement.element( "payment_method" );
		
			sb.append( paymentElement.attributeValue("payment_type"));
			sb.append( getDelimiter() );
			sb.append( paymentElement.attributeValue("po_number"));
			sb.append( getDelimiter() );
			sb.append( paymentElement.attributeValue("card_type"));
			sb.append( getDelimiter() );
			sb.append( paymentElement.attributeValue("card_number"));
			sb.append( getDelimiter() );
			sb.append( paymentElement.attributeValue("expiration_date"));
			sb.append( getDelimiter() );
			writer.write( sb.toString() + "\n");
		}
		
		writer.flush();
		writer.close();
	}
	
	public void exportItems( File inOrderFile, File inItemsFile ) throws Exception
	{
		
		Element root = getRootElement( inOrderFile, "orders" );
		FileWriter writer = new FileWriter( inItemsFile );
		StringBuffer header = new StringBuffer();
		header.append( "order_number" );
		header.append( getDelimiter() );
		header.append( "sku" );
		header.append( getDelimiter() );
		header.append( "product_id" );
		header.append( getDelimiter() );
		header.append( "quantity" );
		header.append( getDelimiter() );
		header.append( "price" );
		writer.write( header.toString() + "\n");
		for ( Iterator iter = root.elementIterator("order"); iter.hasNext(); )
		{
			Element orderElement = (Element) iter.next();
			StringBuffer sb = new StringBuffer();
			sb.append( orderElement.attributeValue( "order_number" ) );
			sb.append( getDelimiter() );
			for ( Iterator iterator = orderElement.elementIterator( "item"); iterator.hasNext(); )
			{
				Element itemElement = (Element) iterator.next();
				sb.append( itemElement.attributeValue("sku"));
				sb.append( getDelimiter() );
				sb.append( itemElement.attributeValue("product_id"));
				sb.append( getDelimiter() );
				sb.append( itemElement.attributeValue("quantity"));
				sb.append( getDelimiter() );
				sb.append( itemElement.attributeValue("price"));
			}
			writer.write( sb.toString() + "\n");
		}
		
		writer.flush();
		writer.close();
	}

	public void exportCustomers( File  inOrderFile, File inCustomerFile ) throws Exception
	{
		Element root = getRootElement( inOrderFile, "orders" );
		FileWriter writer = new FileWriter( inCustomerFile );
		StringBuffer header = new StringBuffer();
		header.append( "customerid" );
		header.append( getDelimiter() );
		header.append( "first_name" );
		header.append( getDelimiter() );
		header.append( "last_name" );
		header.append( getDelimiter() );
		header.append( "phone1" );
		header.append( getDelimiter() );
		header.append( "email" );
		String prefix = "bill_to_";
		header.append( prefix + "address1" );
		header.append( getDelimiter() );
		header.append( prefix + "address2" );
		header.append( getDelimiter() );
		header.append( prefix + "city" );
		header.append( getDelimiter() );
		header.append( prefix + "state" );
		header.append( getDelimiter() );
		header.append( prefix +  "zip_code"  );
		header.append( getDelimiter() );
		header.append( prefix + "country" );
		header.append( getDelimiter() );
		prefix = "ship_to_";
		header.append( prefix + "address1" );
		header.append( getDelimiter() );
		header.append( prefix + "address2" );
		header.append( getDelimiter() );
		header.append( prefix + "city" );
		header.append( getDelimiter() );
		header.append( prefix + "state" );
		header.append( getDelimiter() );
		header.append( prefix +  "zip_code"  );
		header.append( getDelimiter() );
		header.append( prefix + "country" );
		header.append( getDelimiter() );
		writer.write( header.toString() + "\n");
		for ( Iterator iter = root.elementIterator("order"); iter.hasNext(); )
		{
			Element orderElement = (Element) iter.next();
			StringBuffer sb = new StringBuffer();
			Element customerElement = orderElement.element( "customer" );
			sb.append( customerElement.attributeValue( "customerid" ) );
			sb.append( getDelimiter() );
			sb.append( customerElement.attributeValue( "first_name" ) );
			sb.append( getDelimiter() );
			sb.append( customerElement.attributeValue( "last_name" ) );
			sb.append( getDelimiter() );
			sb.append( customerElement.attributeValue( "phone1" ) );
			sb.append( getDelimiter() );
			sb.append( customerElement.attributeValue( "email" ) );
			sb.append( getDelimiter() );
			List addressElements = customerElement.elements("address");
			for ( Iterator iterator = addressElements.iterator(); iterator.hasNext(); )
			{
				// TODO:  need to ensure that "bill to" fields get written first.
				Element addressElement = (Element) iterator.next();
				prefix = "";
				if( addressElement.attributeValue("type").equals("shipping") )
				{
					//prefix = "ship_to_";
					prefix = "";
				}
				if( addressElement.attributeValue("type").equals("billing") )
				{
					//prefix = "bill_to_";
					prefix = "";
				}
				sb.append( prefix + addressElement.attributeValue( "address1" ) );
				sb.append( getDelimiter() );
				sb.append( prefix + addressElement.attributeValue( "address2" ) );
				sb.append( getDelimiter() );
				sb.append( prefix + addressElement.attributeValue( "city" ) );
				sb.append( getDelimiter() );
				sb.append( prefix + addressElement.attributeValue( "state" ) );
				sb.append( getDelimiter() );
				sb.append( prefix + addressElement.attributeValue( "zip_code" ) );
				sb.append( getDelimiter() );
				sb.append( prefix + addressElement.attributeValue( "country" ) );
				sb.append( getDelimiter() );
			}
			writer.write( sb.toString() + "\n");
		}
		writer.flush();
		writer.close();
	}


	public String getDelimiter()
	{
		return fieldDelimiter;
	}
	public void setDelimiter( String delimiter )
	{
		fieldDelimiter = delimiter;
	}
}
