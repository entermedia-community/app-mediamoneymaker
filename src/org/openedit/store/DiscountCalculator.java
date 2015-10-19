package org.openedit.store;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.entermedia.MediaArchive;
import org.openedit.money.Money;

import com.openedit.WebPageRequest;
import com.openedit.users.User;

public class DiscountCalculator {
	
	private static final Log log = LogFactory.getLog(DiscountCalculator.class);
	
	/**
	 * 
	 * @param inReq
	 * @param inCartItem
	 * @return
	 */
	public Money getDiscount(WebPageRequest inReq, CartItem inCartItem){
		MediaArchive archive = (MediaArchive) inReq.getPageValue("mediaarchive");
		String applicationid = (String) inReq.getPageValue("applicationid");
		User user = inReq.getUser();
		Money discount = getDiscount(archive,applicationid,user,inCartItem.getProduct());
		return discount;
	}
	
	/**
	 * 
	 * @param inArchive
	 * @param inApplicationid
	 * @param inUser
	 * @param inProduct
	 * @return
	 */
	public Money getYourPrice(MediaArchive inArchive, String inApplicationid, User inUser, Data inProduct){
		Money yourprice = new Money(inProduct.get("yourprice"));
		Money discount = calculateDiscount(inArchive,inApplicationid,inUser,yourprice);
		return yourprice.subtract(discount);
	}
	
	/**
	 * 
	 * @param inArchive
	 * @param inApplicationid
	 * @param inUser
	 * @param inProduct
	 * @return
	 */
	public Money getDiscount(MediaArchive inArchive, String inApplicationid, User inUser, Product inProduct){
		Money price = calculateDiscount(inArchive,inApplicationid,inUser,inProduct.getYourPrice());
		return price;
	}
	
	/**
	 * 
	 * @param inArchive
	 * @param inApplicationid
	 * @param inUser
	 * @param inPrice
	 * @return
	 */
	public Money calculateYourPrice(MediaArchive inArchive, String inApplicationid, User inUser, Money inPrice){
		Money discount = calculateDiscount(inArchive,inApplicationid,inUser,inPrice);
		return inPrice.subtract(discount);
	}
	
	/**
	 * 
	 * @param inArchive
	 * @param inApplicationid
	 * @param inUser
	 * @param inPrice
	 * @return
	 */
	protected Money calculateDiscount(MediaArchive inArchive, String inApplicationid, User inUser, Money inPrice){
		Money price = new Money();
		if (inUser!=null){
			Data userprofile = (Data) inArchive.getSearcher("userprofile").searchById(inUser.getId());
			Data rolediscount = inArchive.getData("settingsgroupdiscount",userprofile.get("settingsgroup"));
			if (rolediscount!=null){
				Collection<String> webapps = ((MultiValued) rolediscount).getValues("webapp");
				if (webapps!=null && webapps.contains(inApplicationid)){
					double discount = toDouble(rolediscount.get("percentdiscount"),-1);
					if (discount > 0 && discount < 1){
						price = inPrice.multiply(discount);
					} else {
						discount = toDouble(rolediscount.get("dollardiscount"),-1);
						if (discount > 0){
							Money value = new Money(discount);
							Money diff = inPrice.subtract(value);
							if (!diff.isNegative() && !diff.isZero()){
								price = value;
							}
						}
					}
				}
			}
		}
		return price;
	}
	
	
	/**
	 * 
	 * @param inValue
	 * @param inDefault
	 * @return
	 */
	protected double toDouble(String inValue, double inDefault){
		if (inValue!=null && inValue.indexOf(".") == -1){
			inValue = inValue.trim() + ".0";
		}
		try{
			return Double.parseDouble(inValue);
		}catch(Exception e){}
		return inDefault;
	}

}
