/*
 * Created on Feb 15, 2005
 */
package org.openedit.store;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.openedit.util.FileUtils;

/**
 * @author Matthew Avery, mavery@einnovation.com
 * @deprecated use XMLUtils
 */
public class Dom4JHelper
{

	public void writeXmlFile( Element inRoot, File inFile ) throws IOException
	{
		//	write to a temporary file first, in case there's an error
		File tempFile = new File( inFile.getParentFile(), inFile.getName() + ".temp" );

		inFile.getParentFile().mkdirs();
		FileOutputStream fos = null;
		try
		{
			fos = new FileOutputStream( tempFile );
			XMLWriter writer = new XMLWriter( fos, OutputFormat.createPrettyPrint() );
			writer.write( inRoot );
		}
		finally
		{
			FileUtils.safeClose( fos );
		}

		inFile.delete();
		if ( !tempFile.renameTo(inFile) )
		{
			throw new IOException( "Unable to rename " + tempFile + " to " + inFile );
		}
	}

	public Element getRootElement( File inFile, String inElementName ) throws IOException, DocumentException
	{
		if ( inFile.exists() )
		{
			SAXReader reader = new SAXReader();
			Document document;
			FileInputStream stream = null;
			try
			{
				stream = new FileInputStream(inFile);
				document = reader.read(stream);
				return document.getRootElement();
			}
			finally
			{
				if ( stream != null )
				{
					stream.close();	
				}
			}
		}
		else
		{
			return DocumentHelper.createElement( inElementName );
		}
	}

}
