package shipping;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element
import org.openedit.Data
import org.openedit.data.BaseData
import org.openedit.money.Money
import org.openedit.store.Cart
import org.openedit.store.CartItem
import org.openedit.store.modules.StoreSearchModule;
import org.openedit.store.shipping.BaseShippingMethod

public class avoca extends BaseShippingMethod {
	
	private static final Log log = LogFactory.getLog(avoca.class);
	
	protected String fieldDistributor = "106";
	
	public Money getCost(Cart inCart) {
		if(inCart.getShippingAddress() == null || inCart.getShippingAddress().getState() == null){
			return new Money();
		}
		String state = inCart.getShippingAddress().getState();
		if (state == null)
		{
			return new Money();
		}
		//maritimes 18
		if (state.equals("NL") || state.equals("NS") || state.equals("PE") || state.equals("NB"))
		{
			return new Money(18);
		}
		//QC ON 10
		if (state.equals("QC") || state.equals("ON"))
		{
			return new Money(10);
		}
		//MB SK 16
		if (state.equals("MB") || state.equals("SK"))
		{
			return new Money(16);
		}
		//AB BC 14
		if (state.equals("AB") || state.equals("BC"))
		{
			return new Money(14);
		}
		//YT NU 20
		if (state.equals("YT") || state.equals("NU"))
		{
			return new Money(20);
		}
		log.info("Warning: unknown province = $state, returning 0");
		return new Money();
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