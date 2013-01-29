package shipping;

import org.dom4j.Element
import org.openedit.Data
import org.openedit.data.BaseData
import org.openedit.money.Money
import org.openedit.store.Cart
import org.openedit.store.CartItem
import org.openedit.store.shipping.BaseShippingMethod

public class microcel extends BaseShippingMethod {


	public Money getCost(Cart inCart) {
		// TODO Auto-generated method stub
		Money totalformicrocel = getDistributorTotal(inCart);
		
		if(totalformicrocel .doubleValue() > 0 && totalformicrocel.doubleValue() < 100){
			return new Money(10);
		} else{
		return new Money();
		}
	}

	private Money getDistributorTotal(Cart inCart) {
		Money totalformicrocel = new Money();
		for (Iterator iterator = inCart.getItems().iterator(); iterator.hasNext();) {
			CartItem item = (CartItem) iterator.next();
			if(item.getProduct().get('distributor').equals("104")){
				Money itemprice = item.getYourPrice();
				Money totalcost = itemprice.multiply(item.getQuantity());
				totalformicrocel = totalformicrocel.add(totalcost);
				//totalformicrocel = totalformicrocel.add(item.getYourPrice());
			}
		}
		return totalformicrocel;
	}


	public boolean applies(Cart inCart) {
		for (Iterator iterator = inCart.getItems().iterator(); iterator.hasNext();) {
			CartItem item = (CartItem) iterator.next();
			if(item.getProduct().get('distributor').equals("104")){
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
			hint.setProperty("distributor", "104");
			Money shortamount = new Money(500).subtract(getDistributorTotal(inCart));
			hint.setProperty("needed", shortamount.toShortString());
			hint.setProperty("savings",getCost(inCart).toShortString());
			
			hints.add(hint);
		}
		return hints;
	}
	
	
}