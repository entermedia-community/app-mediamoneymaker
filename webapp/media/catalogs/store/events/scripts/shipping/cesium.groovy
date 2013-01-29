package shipping;

import org.dom4j.Element
import org.openedit.money.Money
import org.openedit.store.Cart
import org.openedit.store.CartItem
import org.openedit.store.ShippingMethod
import org.openedit.store.shipping.BaseShippingMethod

public class cesium extends BaseShippingMethod {

	public Money getCost(Cart inCart) {
		// TODO Auto-generated method stub
		return new Money(9);
	}

	public boolean applies(Cart inCart) {
		for (Iterator iterator = inCart.getItems().iterator(); iterator.hasNext();) {
			CartItem item = (CartItem) iterator.next();
			if(item.getProduct().get('distributor').equals("105")){
				return true;
			}
		}
		return false;
	}

	public void configure(Element inElement) {
		// TODO Auto-generated method stub

	}

	public Collection getHints(Cart inCart){
		return new ArrayList();
	}
	
	
	
	
}