/*
 * Created on Mar 4, 2004
 *
 */
package org.openedit.store.customer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openedit.Data;
import org.openedit.money.Fraction;
import org.openedit.store.PaymentMethod;
import org.openedit.store.TaxRate;

import com.openedit.users.User;
import com.openedit.users.filesystem.FileSystemUser;

/**
 * A customer with nice getters and setters on top of the standard user object.
 * 
 * @author dbrown
 */
public class Customer implements Data {
	public static final String NAME = "Name";
	public static final String PHONE1 = "Phone1";
	public static final String FAX = "Fax";
	public static final String TITLE = "Title";
	public static final String ALLOW_EMAIL = "AllowEmail";
	public static final String REFERENCE = "ReferenceNumber";
	public static final String TAX_RATE = "TaxRate";
	public static final String TAX_EXEMPT_ID = "TaxExemptId";
	public static final String COMPANY = "Company";
	public static final String USERFIELD1 = "userfield1";
	public static final String USERFIELD2 = "userfield2";

	protected User fieldUser; // used to store information in Open Edit
	protected PaymentMethod fieldPaymentMethod;
	// protected Address fieldBillingAddress;
	// protected Address fieldShippingAddress;
	protected String fieldUserName;
	protected List addressList;
	protected List fieldTaxRates;
	
	/**
	 * Creates a customer that uses the given user to store its information.
	 * 
	 * @param inUser
	 */
	public Customer(User inUser) {
		fieldUser = inUser;

	}

	public Customer() 
	{
		fieldUser = new FileSystemUser();
	}
	public void setUserName(String inUsername)
	{
		getUser().setUserName(inUsername);
	}
	
	public User getUser() {
		return fieldUser;
	}
	public void setUser(User inUser) {
		fieldUser = inUser;
	}


	public String getUserName() {
		return getUser().getUserName();
	}

	public String getFirstName() {
		return getUser().getFirstName();
	}

	public void setFirstName(String inFirstName) {
		getUser().setFirstName(inFirstName);
	}

	public String getLastName() {
		return getUser().getLastName();
	}

	public void setLastName(String inLastName) {
		getUser().setLastName(inLastName);
	}

	public String getEmail() {
		return getUser().getEmail();
	}

	public void setEmail(String inEmail) {
		getUser().setEmail(inEmail);
	}

	public String getPassword() {
		return getUser().getPassword();
	}

	public boolean isAllowEmail() {
		return getUser().getBoolean(ALLOW_EMAIL);
	}

	public void setAllowEmail(boolean inValue) {
		getUser().safePut(ALLOW_EMAIL, String.valueOf(inValue));
	}

	public String getTaxExemptId() {
		return getUser().getString(TAX_EXEMPT_ID);
	}

	public void setTaxExemptId(String inString) {
		getUser().safePut(TAX_EXEMPT_ID, inString);
	}

	public String getName() {
		return getFirstName() + " " + getLastName();
	}
	public void setName(String inName)
	{
		getUser().safePut(NAME, inName);
	}
	public PaymentMethod getPaymentMethod() {
		return fieldPaymentMethod;
	}

	public void setPaymentMethod(PaymentMethod inPaymentMethod) {
		fieldPaymentMethod = inPaymentMethod;
	}

	public String getPhone1() {
		return getUser().getString(PHONE1);
	}

	public void setPhone1(String inPhone1) {
		getUser().safePut(PHONE1, inPhone1);
	}

	public String getCompany() {
		return getUser().getString(COMPANY);
	}

	public void setCompany(String inCompany) {
		getUser().safePut(COMPANY, inCompany);
	}

	public List getTaxRates() {
	
		if(fieldTaxRates == null){
		
			String taxRateString = Fraction.ZERO.toString();
			TaxRate rate = new TaxRate();
			rate.setFraction(new Fraction(taxRateString));
			fieldTaxRates = new ArrayList();
			fieldTaxRates.add(rate);
		
		
		}
		 return fieldTaxRates;
	}

	public void setTaxRates(List inTaxRates) {
		
		fieldTaxRates = inTaxRates;
	}

	public String getReferenceNumber() {
		return getUser().getString(REFERENCE);
	}

	public void setReferenceNumber(String inReferenceNumber) {
		getUser().safePut(REFERENCE, inReferenceNumber);
	}

	public String getTitle() {
		return getUser().getString(TITLE);
	}

	public void setTitle(String inTitle) {
		getUser().safePut(TITLE, inTitle);
	}

	// See UserManagerModule.clean
	public String cleanPhoneNumber() {
		String phoneNumber = getPhone1();
		if (phoneNumber != null) {
			StringBuffer out = new StringBuffer();
			for (int i = 0; i < phoneNumber.length(); i++) {
				if (Character.isDigit(phoneNumber.charAt(i))) {
					out.append(phoneNumber.charAt(i));
				}
			}
			phoneNumber = out.toString();
		}
		return phoneNumber;
	}

	public Address getBillingAddress(boolean create) {
		Address billing = getAddress("billing");
		if (billing == null && create) {
			billing = new Address();
			billing.setPrefix("billing"); // only used to store the data
		}
		if(billing != null){
		addAddress(billing);
		}
		return billing;
	}
	
	public Address getBillingAddress() {
		return getBillingAddress(true);
	}

	public void setBillingAddress(Address inAddress) {
		inAddress.setPrefix("billing");
		Address billing = getAddress("billing");
		if (getAddress("billing") != null) {
			getAddressList().remove("billing");
		}
		getAddressList().add(inAddress);

	}

	public Address getAddress(String inString) {
		for (Iterator iterator = getAddressList().iterator(); iterator
				.hasNext();) {
			Address address = (Address) iterator.next();
			if (inString.equals(address.getPrefix())) {
				return address;
			}
		}
		return null;
	}

	public Address getShippingAddress(boolean create)
	{
		Address shipping = getAddress("shipping");
		if (shipping == null && create) {
			shipping = new Address();
			shipping.setPrefix("shipping");
			addAddress(shipping);
		}
		addAddress(shipping);

		return shipping;
	}
	
	public Address getShippingAddress() 
	{
		return getShippingAddress(true);
	}

	public void setShippingAddress(Address inShippingAddress) {
		inShippingAddress.setPrefix("shipping");
		Address shipping = getAddress("shipping");
		if (shipping != null) {
			getAddressList().remove("shipping");
		}
		getAddressList().add(inShippingAddress);
	}

	public String getFax() {
		return getUser().getString(FAX);
	}

	public void setFax(String inFax) {
		getUser().safePut(FAX, inFax);
	}

	public String getUserField1() {
		return getUser().getString(USERFIELD1);
	}

	public void setUserField1(String inUserField1) {
		getUser().safePut(USERFIELD1, inUserField1);
	}

	public String getUserField2() {
		return getUser().getString(USERFIELD2);
	}

	public void setUserField2(String inUserField2) {
		getUser().safePut(USERFIELD2, inUserField2);
	}

	public void setPassword(String inPassword) {
		getUser().setPassword(inPassword);
	}

	public List getAddressList() {
		if (addressList == null) {
			addressList = new ArrayList();
		}
		
		return addressList;
	}

	public void setAddressList(List addressList) {
		this.addressList = addressList;
	}

	public void addAddress(Address inAddress) {
		removeAddress(inAddress);
		getAddressList().add(inAddress);
	}

	public void removeAddress(Address inAddress) {
		Address toRemove = getAddress(inAddress.getPrefix());
		if (toRemove != null) {
			getAddressList().remove(toRemove);
		}

	}

	public String get( String inId )
	{
		return (String)getUser().get(inId);
	}

	public String getId()
	{
		// TODO Auto-generated method stub
		return getUserName();
	}

	public void setId( String inNewid )
	{
		getUser().setUserName( inNewid );
	}

	public void setProperty( String inId, String inValue )
	{
		getUser().put( inId, inValue);
		
	}

	public String getSourcePath()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public void setSourcePath(String inSourcepath)
	{
		// TODO Auto-generated method stub
		
	}

	public Map getProperties() {
		return getUser().getProperties();
	}

	@Override
	public void setProperties(Map<String, String> inProperties) {
		// TODO Auto-generated method stub
		
	}

}
