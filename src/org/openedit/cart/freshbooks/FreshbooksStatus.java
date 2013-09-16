package org.openedit.cart.freshbooks;

import java.util.ArrayList;

public class FreshbooksStatus {
	//responses from freshooks
	protected String fieldInvoiceId;//invoice_id from freshbooks
	protected String fieldInvoiceStatus;//actual status from freshbooks: paid, auto-paid, retry, etc
	
	//error messages from invoice create only
	protected String fieldErrorMessage;
	protected String fieldErrorCode;
	
	//require a list of FreshbooksRecurringProfiles
	//if the recurring id has not been set, then the profile creation did not work
	//otherwise, the recurring profile is created remotely
	protected ArrayList<RecurringProfile> fieldRecurringProfiles;
	
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
	public void setRecurringProfiles(ArrayList<RecurringProfile> inList){
		fieldRecurringProfiles = inList;
	}
	public ArrayList<RecurringProfile> getRecurringProfiles(){
		if (fieldRecurringProfiles == null){
			setRecurringProfiles(new ArrayList<RecurringProfile>());
		}
		return fieldRecurringProfiles;
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
	public String toString(){
		StringBuilder buf = new StringBuilder();
		buf.append("Freshbooks Status: InvoiceId=").append(getInvoiceId()).append("\n")
			.append("\t").append("InvoiceStatus=").append(getInvoiceStatus()).append("\n")
			.append("\t").append("RecurringProfiles={").append(getRecurringProfiles()).append("}\n");
		if (getErrorMessage()!=null && !getErrorMessage().isEmpty()){
			buf.append("\t").append("ErrorMessage=").append(getErrorMessage()).append("\n")
				.append("\t").append("ErrorCode=").append(getErrorCode() == null ? "" : getErrorCode());
		}
		return buf.toString().trim();
	}
}
