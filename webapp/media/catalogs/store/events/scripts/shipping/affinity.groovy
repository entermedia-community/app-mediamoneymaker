package shipping;

import org.dom4j.Element
import org.openedit.Data
import org.openedit.data.BaseData
import org.openedit.money.Money
import org.openedit.store.Cart
import org.openedit.store.CartItem
import org.openedit.store.shipping.BaseShippingMethod

public class affinity extends BaseShippingMethod {
	protected String distributor = "102"

	public Money getCost(Cart inCart) {
		// TODO Auto-generated method stub
		if(inCart.getShippingAddress() == null){
			return new Money();
		}
		Money totalformicrocel = getDistributorTotal(inCart)
		if(totalformicrocel.doubleValue() == 0){
			return new Money();
		}
		if(totalformicrocel.doubleValue() < 100){
			if(inCart.getShippingAddress().getState() != null && inCart.getShippingAddress().getState().equals("ON")){
				return new Money(7);
			}else{
				return new Money(10);
			}
		}
	}

	private Money getDistributorTotal(Cart inCart) {
		Money totalformicrocel = new Money();
		for (Iterator iterator = inCart.getItems().iterator(); iterator.hasNext();) {
			CartItem item = (CartItem) iterator.next();
			if(item.getProduct().get('distributor').equals(distributor)){

				totalformicrocel = totalformicrocel.add(item.getYourPrice());
			}
		}
		return totalformicrocel;
	}


	public boolean applies(Cart inCart) {
		for (Iterator iterator = inCart.getItems().iterator(); iterator.hasNext();) {
			CartItem item = (CartItem) iterator.next();
			if(item.getProduct().get('distributor').equals(distributor)){
				return true;
			}
		}
		return false;
	}

	public void configure(Element inElement) {
		// TODO Auto-generated method stub

	}

	public List getHints(Cart inCart){
		if(!applies(inCart)){
			return new ArrayList();
		}
		ArrayList hints = new ArrayList();
		if(getCost(inCart).doubleValue() > 0){
			Data hint = new BaseData();
			hint.setProperty("distributor", distributor);
			Money shortamount = new Money(100).subtract(getDistributorTotal(inCart));
			hint.setProperty("needed", shortamount.toShortString());
			hint.setProperty("savings",getCost(inCart).toShortString());
			
			hints.add(hint);
		}
		return hints;
	}


}