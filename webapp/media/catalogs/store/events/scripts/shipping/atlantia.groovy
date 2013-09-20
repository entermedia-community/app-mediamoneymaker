package shipping;

import org.dom4j.Element
import org.openedit.Data
import org.openedit.data.BaseData
import org.openedit.money.Money
import org.openedit.store.Cart
import org.openedit.store.CartItem
import org.openedit.store.customer.Address
import org.openedit.store.shipping.BaseShippingMethod

public class atlantia extends BaseShippingMethod {

	private static final double threshold = 350;
	private static final String distributorID = "atlantia";
	private static final int shippingCost = 10;
	
	public Money getCost(Cart inCart) {
		// TODO Auto-generated method stub
		Money totalforatlantia = getDistributorTotal(inCart);
		
		if(totalforatlantia.doubleValue() > 0 && totalforatlantia.doubleValue() < this.threshold) {
			Address shippingAddress = inCart.getShippingAddress();
			if ((shippingAddress.getState().equals("BC")) || 
				(shippingAddress.getState().equals("AB")) || 
				(shippingAddress.getState().equals("SK"))) {
				return new Money(10);
			} else if ((shippingAddress.getState().equals("MB")) || 
				(shippingAddress.getState().equals("ON")) || 
				(shippingAddress.getState().equals("QC"))) {
				return new Money(15);
			} else {
				return new Money(17);
			} 
		} else{
			return new Money();
		}
	}

	private Money getDistributorTotal(Cart inCart) {
		Money totalforatlantia = new Money();
		for (Iterator iterator = inCart.getItems().iterator(); iterator.hasNext();) {
			CartItem item = (CartItem) iterator.next();
			if(item.getProduct().get('distributor').equals(this.distributorID)){
				Money itemprice = item.getYourPrice();
				Money totalcost = itemprice.multiply(item.getQuantity());
				totalforatlantia = totalforatlantia.add(totalcost);
				//totalformicrocel = totalformicrocel.add(item.getYourPrice());
			}
		}
		return totalforatlantia;
	}


	public boolean applies(Cart inCart) {
		for (Iterator iterator = inCart.getItems().iterator(); iterator.hasNext();) {
			CartItem item = (CartItem) iterator.next();
			if(item.getProduct().get('distributor').equals(this.distributorID)){
				return true;
			}
		}
		return false;
	}

	public void configure(Element inElement) {
		// TODO Auto-generated method stub
	}

	public Collection getHints(Cart inCart){
		ArrayList hints = new ArrayList();
		if(getCost(inCart).doubleValue() > 0){
			Data hint = new BaseData();
			hint.setProperty("distributor", this.distributorID);
			Money shortamount = new Money(this.threshold).subtract(getDistributorTotal(inCart));
			hint.setProperty("needed", shortamount.toShortString());
			hint.setProperty("savings",getCost(inCart).toShortString());
			hints.add(hint);
		}
		return hints;
	}
}