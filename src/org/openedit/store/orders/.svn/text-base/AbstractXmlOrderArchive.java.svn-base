/*
 * Created on Jan 17, 2005
 */
package org.openedit.store.orders;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.openedit.store.Store;
import org.openedit.store.StoreException;

import com.openedit.util.FileUtils;

/**
 * @author dbrown
 *
 */
public abstract class AbstractXmlOrderArchive extends BaseOrderArchive 
{

	protected void writeXmlFile( Element inRoot, File inFile ) throws StoreException
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
		catch( IOException e )
		{
			throw new StoreException( e );
		}
		finally
		{
			FileUtils.safeClose( fos );
		}

		inFile.delete();
		if ( !tempFile.renameTo(inFile) )
		{
			throw new StoreException( "Unable to rename " + tempFile + " to " + inFile );
		}
	}

	protected Element getRootElement( File inFile, String inElementName ) throws StoreException
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
			catch( Exception e )
			{
				throw new StoreException( e );
			}
			finally
			{
				if ( stream != null )
				{
					try
					{
						stream.close();
					}
					catch ( IOException e )
					{
						throw new StoreException( e );
					}
				}
			}
		}
		else
		{
			return DocumentHelper.createElement( inElementName );
		}
	}

	protected File getOrdersDirectory( Store inStore )
	{
		return new File( inStore.getStoreDirectory(), "/data/orders/" );
	}


}
