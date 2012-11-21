package edi;

import java.util.ArrayList;

import org.openedit.Data;

import com.openedit.WebPageRequest;
import com.openedit.entermedia.scripts.EnterMediaObject;

public class ErrorProcessing extends EnterMediaObject {
	
	private ArrayList<String> errorList;
	private WebPageRequest context;
	private String errorType;
	private String processName;
	private String errorID;

	public ErrorProcessing() {
		errorList = new ArrayList<String>();
	}
	public void addToList(String inValue) {
		errorList.add(inValue);
	}
	public ArrayList<String> getErrorList() {
		return errorList;
	}
	public void setContext(WebPageRequest inContext) {
		context = inContext;
	}
	public void setErrorType(String inErrorType) {
		errorType = inErrorType;
	}
	public void setProcessName(String inProcessName) {
		processName = inProcessName;
	}
	public String getErrorID() {
		return errorID;
	}
	
	public boolean createNewMessage() {
		boolean complete = false;
		String fullMessage = "";
		
		if (errorList.size() > 0) {
			MediaUtilities searcherUtility = new MediaUtilities();
			searcherUtility.setContext(context);
			searcherUtility.setSearchers();
			
			Data errorMessage = searcherUtility.getErrorSearcher().createNewData();
			errorMessage.setId(searcherUtility.getErrorSearcher().nextId());
			for( int index=0; index < errorList.size(); index++) {
				fullMessage += "<li>" + errorList.get(index) + "</li>";
			}
			errorMessage.setProperty("errortype", errorType);
			errorMessage.setProperty("processname", processName);
			errorMessage.setProperty("errormessage", fullMessage);
			searcherUtility.getErrorSearcher().saveData(errorMessage, searcherUtility.getContext().getUser());
			complete = true;
			errorID = errorMessage.getId();
		}
		return complete;
	}
}
