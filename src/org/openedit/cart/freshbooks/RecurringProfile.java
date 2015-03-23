package org.openedit.cart.freshbooks;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.openedit.store.CartItem;

public class RecurringProfile {
	
	protected String recurringId;
	protected String frequency;
	protected String occurrence;
	protected Date startDate;
	ArrayList<CartItem> cartItems = new ArrayList<CartItem>();
	
	public String getRecurringId(){
		return recurringId;
	}
	public void setRecurringId(String recurringId){
		this.recurringId = recurringId;
	}
	public String getFrequency() {
		return frequency;
	}
	public void setFrequency(String frequency) {
		this.frequency = frequency;
	}
	public String getOccurrence() {
		return occurrence;
	}
	public void setOccurrence(String occurrence) {
		this.occurrence = occurrence;
	}
	public Date getStartDate(){
		return startDate;
	}
	public void setStartDate(Date date){
		startDate = date;
	}
	public void setStartDate(Date inOrderDate, String inDays, String inMonths, String inYears){
		long days = 0;
		if (!inDays.equals("0")){//days
			days = Long.parseLong(inDays);
		} else if (!inYears.equals("0")){ //years
			days = Integer.parseInt(inYears) * 365;
		} else { //months
			int months = Integer.parseInt(inMonths);
			Calendar cal = new GregorianCalendar();
			cal.setTime(inOrderDate);
			cal.add(Calendar.MONTH,months);
			days = (cal.getTimeInMillis() - inOrderDate.getTime())/(24*60*60*1000);
		}
		long futureTime = inOrderDate.getTime() + ((long)days) *24*60*60*1000;
		startDate = new Date();
		startDate.setTime(futureTime);
	}
	public ArrayList<CartItem> getCartItems() {
		return cartItems;
	}
	public void setCartItems(ArrayList<CartItem> cartItems) {
		this.cartItems = cartItems;
	}
	public String toString(){
		StringBuilder buf = new StringBuilder();
		buf.append("[RecurringId=").append(getRecurringId() == null ? "NULL" : getRecurringId()).append(",Frequency=").append(getFrequency())
			.append(",Occurrence=").append(getOccurrence()).append(",Start Date=").append(getStartDate())
			.append("CartItems=").append(getCartItems() == null ? "0" : getCartItems().size()).append("]");
		return buf.toString();
	}
}
