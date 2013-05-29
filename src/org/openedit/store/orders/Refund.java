package org.openedit.store.orders;

import org.openedit.data.BaseData;
import org.openedit.money.Money;

public class Refund extends BaseData{

	
	protected Order fieldOrder;
	protected Money fieldAmound;
	protected boolean fieldSuccess;
	
	
	
	public Order getOrder() {
		return fieldOrder;
	}
	public void setOrder(Order inOrder) {
		fieldOrder = inOrder;
	}
	public boolean isSuccess() {
		return fieldSuccess;
	}
	public void setSuccess(boolean inSuccess) {
		fieldSuccess = inSuccess;
	}

	
	
	public Money getAmound() {
		return fieldAmound;
	}
	public void setAmound(Money inAmound) {
		fieldAmound = inAmound;
	}
	
	
	
}
