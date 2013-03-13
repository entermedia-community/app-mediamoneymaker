package shipping;

import org.dom4j.Element
import org.openedit.Data
import org.openedit.data.BaseData
import org.openedit.money.Money
import org.openedit.store.Cart
import org.openedit.store.CartItem
import org.openedit.store.shipping.BaseShippingMethod

public class avoca extends BaseShippingMethod {
	protected String fieldDistributor = "106";
	
	public Money getCost(Cart inCart) {
	
		if(inCart.getShippingAddress() == null || inCart.getShippingAddress().getState() == null){
			return new Money();
		}
		String state = inCart.getShippingAddress().getState();
		switch (state) {
		case "NL":
			return new Money(18);
			break;

			case "NS":
			return new Money(18);
			break;
			case "PE":
			return new Money(18);
			break;
			case "NB":
			return new Money(18);
			break;
			case "QC":
			return new Money(10);
			break;
			case "ON":
			return new Money(10);
			break;
			case "MB":
			return new Money(16);
			break;

			case "SK":
			return new Money(16);
			break;
			case "AB":
			return new Money(14);
			break;
			case "BC":
			return new Money(14);
			break;
			case "NT":
			return new Money(20);
			break;
			case "NT":
			return new Money(20);
			break;
			case "OTH":
			return new Money(20);
			break;
			
			
			
			
		default:
			return new Money();
			break;
		}

		
	}

	public boolean applies(Cart inCart) {
		for (Iterator iterator = inCart.getItems().iterator(); iterator.hasNext();) {
			CartItem item = (CartItem) iterator.next();
			if(item.getProduct().get('distributor').equals(fieldDistributor)){
				return true;
			}
		}
		return false;
	}

	public void configure(Element inElement) {
		// TODO Auto-generated method stub

	}
	private Money getDistributorTotal(Cart inCart) {
		Money totalformicrocel = new Money();
		for (Iterator iterator = inCart.getItems().iterator(); iterator.hasNext();) {
			CartItem item = (CartItem) iterator.next();
			if(item.getProduct().get('distributor').equals(fieldDistributor)){
				Money itemprice = item.getYourPrice();
				Money totalcost = itemprice.multiply(item.getQuantity());
				totalformicrocel = totalformicrocel.add(totalcost);
			}
		}
		return totalformicrocel;
	}
	public Collection getHints(Cart inCart){
	ArrayList hints = new ArrayList();
	
		return hints;
	}
	
	
	
	
}