/*
 * Created on Sep 23, 2004
 */
package org.openedit.store.excelconvert;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.openedit.store.CatalogConverter;
import org.openedit.store.Store;
import org.openedit.store.StoreException;
import org.openedit.store.convert.ConvertStatus;

import com.openedit.util.OutputFiller;

/**
 * @author Matthew Avery, mavery@einnovation.com
 */
public abstract class ExcelConverter extends CatalogConverter
{
	private static final Log log = LogFactory.getLog(ExcelConverter.class);
	protected File fieldSourceSpreadsheet;
	public ExcelConverter()
	{
		super();
	}

	public synchronized void convert(Store inStore, ConvertStatus inLog) throws StoreException
	{
		inLog.add("Starting converter with " + getClass().getName());
		//copy over the /upload/inventory.xls file
        File rootUploadDir = new File( inStore.getStoreDirectory(), "/upload/inventory.xls");

		if ( !rootUploadDir.exists() )
        {
        	log.error("No input found");
    		inLog.add("No input found");
        	return;
        }
        File workingfile = new File( inStore.getStoreDirectory(), "/tmp/inventory.xls");
        workingfile.getParentFile().mkdirs();
        try
		{
        	new OutputFiller().fill(rootUploadDir,workingfile);
		} catch ( Exception ex )
		{
			log.error( ex );
			return;
		}
		//TODO: Mark all items as 0 inventory
		inLog.add("Starting inventory import. Using this input: " + workingfile.getName());
		
		FileInputStream in = null;
		try
		{
			in = new FileInputStream(workingfile);
			//productDirectory().mkdirs();
			POIFSFileSystem fs = new POIFSFileSystem(in);
			HSSFWorkbook wb = new HSSFWorkbook(fs);
			processWorkbook(inStore, inLog, wb );
			inStore.clearProducts();
		}
		catch ( Exception ex)
		{
			throw new StoreException(ex);
		}
		finally
		{
			try
			{
				in.close();
			}catch ( Exception ex)
			{}
		}
		
		File completed = new File( rootUploadDir.getParentFile(),"completed");
		completed.mkdirs();
		String format = SimpleDateFormat.getDateTimeInstance().format(new Date());
		format = format.replace('/','-');
		format = format.replace(':','-');
		format = format.replace(' ','-');
		format = format.replace(',','-');
		
		File done = new File( completed,"inventory-" + format + ".xls" );
		done.delete(); //just in case
		rootUploadDir.renameTo(done);
		inLog.add("Completed conversion");
		inLog.setReindex(true);
	}
	protected String toString(HSSFCell inCell)
	{
		if ( inCell == null)
		{
			return null;
		}
		if ( inCell.getCellType() == HSSFCell.CELL_TYPE_NUMERIC)
		{
			double d = inCell.getNumericCellValue();
			double rem = d % 1;
			if ( rem == 0)
			{
				return String.valueOf((int)d);
			}
			return String.valueOf(d);
		}
		else if ( inCell.getCellType() == HSSFCell.CELL_TYPE_BLANK )
		{
			return null;
		}
		else 
		{
			String val = inCell.getStringCellValue();
			if ( val != null)
			{
				val = val.trim();
			}
			return val;
		}
	}
	protected abstract void processWorkbook(Store inStore, ConvertStatus inLog, HSSFWorkbook inWorkbook )
		throws Exception;
}
