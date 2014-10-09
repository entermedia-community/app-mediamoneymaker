package org.openedit.store.adjustments;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.openedit.store.Cart;
import org.openedit.store.CartItem;
import org.openedit.store.Coupon;

public abstract class MultipleProductsAdjustment {
	
	protected List<String> fieldProducts;

	public List<String> getProducts() 
	{
		if (fieldProducts == null)
		{
			fieldProducts = new ArrayList<String>();
		}
		return fieldProducts;
	}

	public void setProducts(List<String> inProducts) 
	{
		fieldProducts = inProducts;
	}
	
	public void setProducts(String inProductList)
	{
		if (inProductList==null || inProductList.isEmpty())
		{
			return;
		}
		List<String> list = new ArrayList<String>();
		StringTokenizer tok = new StringTokenizer(inProductList,"|");
		while(tok.hasMoreTokens())
		{
			list.add(tok.nextToken().trim());
		}
		setProducts(list);
	}
	
	public boolean hasMultipleProducts()
	{
		return getProducts().size() > 1;
	}
	
	public String findAdjustedProductId(Cart inCart)
	{
		Iterator<?> itr = inCart.getItems().iterator();
		while (itr.hasNext())
		{
			CartItem item = (CartItem) itr.next();
			String id = findAdjustedProductId(item);
			if (id != null)
			{
				return id;
			}
		}
		return null;
	}
	
	public String findAdjustedProductId(CartItem item)
	{
		if (!Coupon.isCoupon(item) && item.getProduct()!=null)
		{
			Iterator<String> itr = getProducts().iterator();
			while(itr.hasNext())
			{
				String product = itr.next();
				if ( item.getProduct().getId().equals(product) )
				{
					return product;
				}
			}
		}
		return null;
	}
}
