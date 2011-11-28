package org.openedit.store.excelconvert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

public class ExcelExporter
{
	protected HSSFWorkbook fieldWorkbook;
	protected HSSFSheet fieldSheet;
	
	public ExcelExporter(String inName)
	{
		setWorkbook(new HSSFWorkbook());
		setSheet(getWorkbook().createSheet(inName));
	}
	
	protected HSSFWorkbook getWorkbook()
	{
		return fieldWorkbook;
	}

	protected void setWorkbook(HSSFWorkbook inWorkbook)
	{
		fieldWorkbook = inWorkbook;
	}

	public HSSFSheet getSheet()
	{
		return fieldSheet;
	}

	public void setSheet(HSSFSheet inSheet)
	{
		fieldSheet = inSheet;
	}
	
	public HSSFRow addRow(int inI)
	{
		return getSheet().createRow(inI);
	}

	public void export(File inOutFile) throws Exception
	{
		OutputStream os = new FileOutputStream(inOutFile);
	    getWorkbook().write(os);
	}
	
	public void export(OutputStream inOs) throws IOException
	{
		getWorkbook().write(inOs);
	}
}
