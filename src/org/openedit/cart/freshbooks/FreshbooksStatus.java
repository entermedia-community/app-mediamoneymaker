package org.openedit.cart.freshbooks;

import java.util.Date;

public class FreshbooksStatus {
	
	public static final int MAX_QUERY_REPEAT = 10;
	public static final long DELAY_BETWEEN_QUERIES = 220l;
	
	protected String fieldFrequency;
	protected String fieldOccurrences;
	protected String fieldSendEmail;
	protected String fieldSendSnailMail;
	protected Date fieldFirstRecurringInvoiceDate;
	
	protected String fieldInvoiceId;//invoice_id from freshbooks
	protected String fieldInvoiceStatus;//actual status from freshbooks: paid, auto-paid, retry, etc
	protected String fieldRecurringId;//recurring_id from freshbooks
	
	protected String fieldErrorMessage;
	protected String fieldErrorCode;
	
	//when processing an invoice, we can query synchronously (block) or asynchronously 
	//to check for a paid state
	protected int fieldMaximumQueryRepeat = -1;
	protected long fieldDelayBetweenQueries = -1l;
	protected boolean fieldIsBlocking = true;//blocking by default
	
	public String getInvoiceId() {
		return fieldInvoiceId;
	}
	public void setInvoiceId(String fieldInvoiceId) {
		this.fieldInvoiceId = fieldInvoiceId;
	}
	public String getInvoiceStatus(){
		return fieldInvoiceStatus;
	}
	public void setInvoiceStatus(String inStatus){
		fieldInvoiceStatus = inStatus;
	}
	public String getRecurringId() {
		return fieldRecurringId;
	}
	public void setRecurringId(String fieldRecurringId) {
		this.fieldRecurringId = fieldRecurringId;
	}
	public String getErrorMessage() {
		return fieldErrorMessage;
	}
	public void setErrorMessage(String fieldErrorMessage) {
		this.fieldErrorMessage = fieldErrorMessage;
	}
	public String getErrorCode() {
		return fieldErrorCode;
	}
	public void setErrorCode(String fieldErrorCode) {
		this.fieldErrorCode = fieldErrorCode;
	}
	public String getFrequency() {
		return fieldFrequency;
	}
	public void setFrequency(String inFrequency) {
		fieldFrequency = inFrequency;
	}
	public String getSendEmail() {
		return fieldSendEmail;
	}
	public void setSendEmail(String inSendEmail) {
		fieldSendEmail = inSendEmail;
	}
	public String getSendSnailMail() {
		return fieldSendSnailMail;
	}
	public void setSendSnailMail(String inSendSnailMail) {
		fieldSendSnailMail = inSendSnailMail;
	}
	public void setFirstRecurringInvoiceDate(Date inDate){
		fieldFirstRecurringInvoiceDate = inDate;
	}
	public Date getFirstRecurringInvoiceDate(){
		return fieldFirstRecurringInvoiceDate;
	}
	public void setMaximumQueryRepeat(int inMax){
		fieldMaximumQueryRepeat = inMax;
	}
	public int getMaximumQueryRepeat(){
		if (fieldMaximumQueryRepeat < 0){
			return MAX_QUERY_REPEAT;
		}
		return fieldMaximumQueryRepeat;
	}
	public void setDelayBetweenQueries(long inDelay){
		fieldDelayBetweenQueries = inDelay;
	}
	public long getDelayBetweenQueries(){
		if (fieldDelayBetweenQueries < 0){
			return DELAY_BETWEEN_QUERIES;
		}
		return fieldDelayBetweenQueries;
	}
	public void setBlocking(boolean isBlocking){
		fieldIsBlocking = isBlocking;
	}
	public boolean isBlocking(){
		return fieldIsBlocking;
	}
	public void setOccurrences(String inOccurrences){
		fieldOccurrences = inOccurrences;
	}
	public String getOccurrences(){
		return fieldOccurrences;
	}
}
