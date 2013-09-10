package org.openedit.cart.freshbooks;

public class FreshbookInstructions {

	
	
	protected String fieldFrequency;
	protected String fieldSendEmail;
	protected String fieldSendSnailMail;
	protected String fieldGateway;
	
	
	protected String fieldErrorMessage;
	protected String fieldErrorCode;
	
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
	public String getGateway() {
		return fieldGateway;
	}
	public void setGateway(String inGateway) {
		fieldGateway = inGateway;
	}
	
	
	
}
