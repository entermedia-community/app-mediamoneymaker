function validateCreditCard( creditCardForm )
{
        if ( creditCardForm.cardNumber.value.length < 10 )
        {
                alert( "Card number is required and must be at least 10 digits" );
                creditCardForm.cardNumber.focus();
                return false;
        }
        if ( creditCardForm.expirationYear.selectedIndex == 0 )
        {
		if( creditCardForm.expirationMonth.selectedIndex < 5 )
		{ 
        	        alert( "Is your credit card already expired?" );
        	        creditCardForm.expirationYear.focus();
        	        return false;
		}
        }
		var ccExp = creditCardForm.expirationMonth + "/" + creditCardForm.expirationYear;
		var value = validCCForm(creditCardForm.cardType,creditCardForm.cardNumber,ccExp);
		if ( value == false )
		{
			//value = confirm("Credit card is not valid. Place order with invalid card?");
		}
		return value;

}

function validateCustomer( customerForm )
{
	if ( customerForm.firstName.value.length == 0 )
	{
		alert( "First name is required" );
		customerForm.firstName.focus();
		return false;
	}
	if ( customerForm.lastName.value.length == 0 )
	{
		alert( "Last name is required" );
		customerForm.lastName.focus();
		return false;
	}
	if ( customerForm.email.value.length == 0 )
	{
		alert( "Email is required" );
		customerForm.email.focus();
		return false;
	}
	if ( customerForm.phone1.value.length == 0 )
	{
		alert( "Phone is required" );
		field.focus();
		return false;
	}
	
	var	field = document.getElementById("shipping.address1.value");
	if ( field.value.length == 0 )
	{
		alert( "Address is required" );
		field.focus();
		return false;
	}
	field = document.getElementById("shipping.city.value");
	if ( field.value.length == 0 )
	{
		alert( "City is required" );
		field.focus();
		return false;
	}
	
//	field = document.getElementById("shipping.state.value");
//	if ( field.value.length == 0 )
//	{
//		alert( "State or province is required" );
//		field.focus();
//		return false;
//	}
	
	field = document.getElementById("shipping.zipCode.value");
	if ( field.value.length == 0 )
	{
		alert( "ZIP or postal code is required" );
		field.focus();
		return false;
	}
	return true;
}

function validateCustomerAndSubmit( form )
{
	if ( !validateCustomer( form ) )
	{
		return;
	}
	form.submit();
}

function validateBilling( orderForm )
{
	var field = document.getElementById("billing.address1.value");
	if ( field.value.length == 0 )
	{
		alert( "Billing address is required." );
		field.focus();
		return false;
	}
	field = document.getElementById("billing.city.value");
	if ( field.value.length == 0 )
	{
		alert( "Billing city is required." );
		field.focus();
		return false;
	}
//	field = document.getElementById("billing.state.value");
//	if ( field.value.length == 0 )
//	{
//		alert( "Billing state or province is required." );
//		field.focus();
//		return false;
//	}
	field = document.getElementById("billing.zipCode.value");
	if ( field.value.length == 0 )
	{
		alert( "Billing ZIP or postal code is required." );
		field.focus();
		return false;
	}
//	field = document.getElementById("billing.country.value");
//	if ( field.value.length == 0 )
//	{
//		alert( "Billing country is required." );
//		field.focus();
//		return false;
//	}
	return true;
}

function validateCreditCardAndSubmit( form )
{
	//check for PO number then return if its set
	var po = document.getElementById("purchaseorder");
	var billmelater = document.getElementById("billmelater");
	if (billmelater == null || !billmelater.checked)
	{
		if ( po != null && po.value.length > 0 )
		{
			//then only check credit card if number is provided
			if( form.cardNumber.value && form.cardNumber.value.length > 0)
			{
				if ( !validateCreditCard( form ) )
				{
					return;
				}		
			}		
		}
		else
		{
			if ( !validateCreditCard( form ) )
			{
				return;
			}	
		}
	}
	else if (billmelater != null && billmelater.checked)
	{
		if (form.dispTermsConditions.value == "true")
		{
			if(form.acceptterms != null && !form.acceptterms.checked)
			{
				alert("To continue you must agree to the 'Terms & Conditions.'")
				return;
			}
		}
	}
	//validate billing
	if (!validateBilling(form))
	{
		return;
	}
	
	form.submit();
}

function validateAndSubmit( form )
{
	if ( !validateCustomer( form ) )
	{
		return;
	}
	if ( !validateCreditCard( form ) )
	{
		return;
	}
	form.submit();
}

