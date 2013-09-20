/*
 * Created on Mar 4, 2004
 *
 */
package org.openedit.store;

import java.text.NumberFormat;

/**
 * @author dbrown
 *
 */ 
public class CreditPaymentMethod extends PaymentMethod
{
	public static final String MASK = "*";
	
	protected CreditCardType fieldCreditCardType;
	protected String fieldCardNumber = "";
	protected int fieldExpirationMonth = 0;
	protected int fieldExpirationYear = 0;
	protected String fieldNote;
	protected boolean fieldBillMeLater; //TODO: extract into a BillMeLaterPaymentMethod class
	protected String fieldCardVerificationCode = "";

	public CreditPaymentMethod()
	{
		super();
	}

	public String getCardNumber()
	{
		return fieldCardNumber;
	}

	public CreditCardType getCreditCardType()
	{
		if ( fieldCreditCardType == null )
		{
			fieldCreditCardType = new CreditCardType();
		}
		return fieldCreditCardType;
	}

	public String getExpirationDateString()
	{
		NumberFormat twoDigitIntFormat = NumberFormat.getIntegerInstance();
		twoDigitIntFormat.setMinimumIntegerDigits(2);
		return twoDigitIntFormat.format(getExpirationMonth()) + '/' +
			twoDigitIntFormat.format(getExpirationYear() % 100);
	}

	public String getExpirationMonthString()
	{
		String dateString = getExpirationDateString();
		String monthString = dateString.substring(0,2);
		return monthString;
	}
	
	public String getExpirationYearString()
	{
		String dateString = getExpirationDateString();
		String yearString = dateString.substring(3,5);
		return yearString;
	}
	
	public int getExpirationMonth()
	{
		return fieldExpirationMonth;
	}

	public int getExpirationYear()
	{
		return fieldExpirationYear;
	}

	public void setCardNumber(String inString)
	{
		fieldCardNumber = inString;
	}

	public void setCreditCardType(CreditCardType inType)
	{
		fieldCreditCardType = inType;
	}

	public void setExpirationMonth(int inI)
	{
		fieldExpirationMonth = inI;
	}

	public void setExpirationYear(int inI)
	{
		fieldExpirationYear = inI;
	}

	public String toString()
	{
		StringBuffer sb = new StringBuffer( "Credit Card: " );
		sb.append( fieldCreditCardType );
		sb.append( '\n' );
		sb.append( fieldCardNumber );
		sb.append("(" + fieldCardVerificationCode + ")\n");
		sb.append( " Expiration: " );
		sb.append( fieldExpirationMonth );
		sb.append( '/' );
		sb.append( fieldExpirationYear );
		sb.append( '\n' );
		return sb.toString();
	}

	/* (non-javadoc)
	 * @see com.openedit.store.PaymentMethod#requiresValidation()
	 */
	public boolean requiresValidation()
	{
		// This requires validation if "bill me later" is not turned on.
		return !getBillMeLater();
	}

	/* (non-javadoc)
	 * @see com.openedit.store.PaymentMethod#getType()
	 */
	public String getType()
	{
		return "creditcard";
	}
	public boolean getBillMeLater()
	{
		return fieldBillMeLater;
	}
	public void setBillMeLater(boolean inBillMeLater)
	{
		fieldBillMeLater = inBillMeLater;
	}
	
	public String getNote()
	{
		return fieldNote;
	}
	public void setNote( String inNote )
	{
		fieldNote = inNote;
	}
	public String getMaskedCardNumber(){
		if (getCardNumber()==null || getCardNumber().isEmpty())
			return getCardNumber();
		int length = getCardNumber().length();
		String mask = getCardNumber();
		if (length >= 4) {
			String sub = getCardNumber().substring(getCardNumber().length()-4);
			return pad("",MASK,length-4) + sub;
		}
		return mask;
	}
	
	protected String pad(String base, String str, int length){
		if (base.length() >= length) return base;
		return pad(base+str,str,length);
	}

	public String getCardVerificationCode() {
		return fieldCardVerificationCode;
	}

	public void setCardVerificationCode(String fieldCardVerificationCode) {
		this.fieldCardVerificationCode = fieldCardVerificationCode;
	}
	public String getMaskedVerificationCode(){
		if (getCardVerificationCode()==null || getCardVerificationCode().isEmpty())
			return getCardVerificationCode();
		int length = getCardVerificationCode().length();
		return pad("",MASK,length);
	}
	public static boolean isMasked(String inValue){
		if (inValue == null || inValue.isEmpty())
			return false;
		return (inValue.contains(MASK));
	}

}
