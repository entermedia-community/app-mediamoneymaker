package org.openedit.store.shipping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Element;
import org.openedit.money.Money;
import org.openedit.store.Cart;
import org.openedit.store.ShippingMethod;

public class CompositeShippingMethod extends BaseShippingMethod implements
		ShippingMethod {

	protected List fieldShippingMethods;

	public List getShippingMethods() {
		if (fieldShippingMethods == null) {
			fieldShippingMethods = new ArrayList();

		}

		return fieldShippingMethods;
	}

	public void setShippingMethods(List inShippingMethods) {
		fieldShippingMethods = inShippingMethods;
	}

	public void addShippingMethod(ShippingMethod method) {
		getShippingMethods().add(method);
	}

	public Money getCost(Cart inCart) {
		Money total = new Money();
		for (Iterator iterator = getShippingMethods().iterator(); iterator
				.hasNext();) {
			ShippingMethod type = (ShippingMethod) iterator.next();
			if (type.applies(inCart)) {
				total = total.add(type.getCost(inCart));
			}
		}
		return total;
	}

	public boolean applies(Cart inCart) {
		for (Iterator iterator = getShippingMethods().iterator(); iterator
				.hasNext();) {
			ShippingMethod method = (ShippingMethod) iterator.next();
			if (method.applies(inCart)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void configure(Element inElement) {
		// TODO Auto-generated method stub

	}

	public Collection getHints(Cart inCart) {
		ArrayList hintlist = new ArrayList();
		for (Iterator iterator = getShippingMethods().iterator(); iterator
				.hasNext();) {
			ShippingMethod metho = (ShippingMethod) iterator.next();
			Collection hints = metho.getHints(inCart);
			if (hints != null) {
				hintlist.addAll(metho.getHints(inCart));
			}
		}
		return hintlist;
	}
}
