/*
 * Created on Dec 9, 2004
 */
package org.openedit.store;

/**
 * @author cburkey
 *
 */
public class Image extends LinkedFile
{
	protected String fieldPostfix; //1 based
	protected String fieldId;
	protected String fieldType;
	public static final String TYPE_ORIGINAL = "original";
	public static final String TYPE_MEDIUM = "medium";
	public static final String TYPE_THUMBNAIL = "thumb";
	
	protected int fieldWidth;
	protected int fieldHeight;

	/**
	 * 
	 */
	public Image(String inDesc, int inWidth, String inPostfix)
	{
		setDescription(inDesc);
		setWidth(inWidth);
		setPostfix(inPostfix);
	}
	
	public Image(String inDesc, int inWidth, int inHeight, String inPostfix)
	{
		setDescription(inDesc);
		setWidth(inWidth);
		setHeight(inHeight);
		setPostfix(inPostfix);
	}
	
	public int getHeight() {
		return fieldHeight;
	}

	public void setHeight(int fieldHeight) {
		this.fieldHeight = fieldHeight;
	}

	/**
	 * 
	 */
	public Image()
	{
	}
	public String getPostfix()
	{
		if (fieldPostfix == null)
			fieldPostfix = "";
		
		return fieldPostfix;
	}
	public void setPostfix(String inPostfix)
	{
		fieldPostfix = inPostfix;
	}
	public int getWidth()
	{
		return fieldWidth;
	}
	public void setWidth(int inWidth)
	{
		fieldWidth = inWidth;
	}
	public boolean isOriginal()
	{
		return TYPE_ORIGINAL.equals( getType() );
	}

	public String getId()
	{
		return fieldId;
	}
	public void setId(String inId)
	{
		fieldId = inId;
	}
	public String getType()
	{
		return fieldType;
	}
	public void setType(String inType)
	{
		fieldType = inType;
	}
	public String buildLink(String inId)
	{
		return getType() + "/" + inId + "-" + getPostfix() + ".jpg";
	}

	
}
