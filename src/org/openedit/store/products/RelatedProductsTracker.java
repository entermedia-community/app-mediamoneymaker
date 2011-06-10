package org.openedit.store.products;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.openedit.hittracker.ListHitTracker;

public class RelatedProductsTracker extends ListHitTracker{

	public List getRelatedByType(String inType){
		ArrayList list = new ArrayList();
		for (Iterator iterator = iterator(); iterator.hasNext();) {
			RelatedProduct product = (RelatedProduct) iterator.next();
			if(inType.equals(product.getType())){
				list.add(product);
			}
			
		}
		return list;
	}

}
