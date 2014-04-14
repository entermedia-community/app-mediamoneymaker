package shipping;

import org.dom4j.Element
import org.openedit.Data
import org.openedit.data.BaseData
import org.openedit.money.Money
import org.openedit.store.Cart
import org.openedit.store.CartItem
import org.openedit.store.shipping.BaseShippingMethod

public class libratel extends BaseShippingMethod {
	
	protected String distributor = "libratel";

	public Money getCost(Cart inCart) {
		
		if(inCart == null || inCart.getShippingAddress() == null)
		{
			return new Money();
		}
		Money total = getDistributorTotal(inCart)
		if(total.doubleValue() == 0)
		{
			return new Money();
		}
		if(total.doubleValue() < 200)
		{
			return new Money(10);
		}
		return new Money();
	}

	private Money getDistributorTotal(Cart inCart) {
		Money total = new Money();
		for (Iterator iterator = inCart.getItems().iterator(); iterator.hasNext();) {
			CartItem item = (CartItem) iterator.next();
			if(item.getProduct().get('distributor').equals(distributor)){
				Money itemprice = item.getYourPrice();
				Money totalcost = itemprice.multiply(item.getQuantity());
				total = total.add(totalcost);
			}
		}
		return total;
	}


	public boolean applies(Cart inCart) 
	{
		for (Iterator iterator = inCart.getItems().iterator(); iterator.hasNext();) 
		{
			CartItem item = (CartItem) iterator.next();
			if(item.getProduct().get('distributor').equals(distributor))
			{
				return true;
			}
		}
		return false;
	}

	public void configure(Element inElement) 
	{
	}

	public Collection getHints(Cart inCart){
		if(!applies(inCart)){
			return new ArrayList();
		}
		ArrayList hints = new ArrayList();
		Money cost = getCost(inCart);
		if(cost.doubleValue() > 0){
			Data hint = new BaseData();
			hint.setProperty("distributor", distributor);
			Money shortamount = new Money(200).subtract(getDistributorTotal(inCart));
			hint.setProperty("needed", shortamount.toShortString());
			hint.setProperty("savings",getCost(inCart).toShortString());
			
			hints.add(hint);
		}
		return hints;
	}


}