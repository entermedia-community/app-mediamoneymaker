package org.openedit.store.xmldb;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Element;
import org.openedit.money.Money;
import org.openedit.store.BaseArchive;
import org.openedit.store.Option;
import org.openedit.store.Price;
import org.openedit.store.PriceSupport;
import org.openedit.store.PriceTier;

import com.openedit.config.Configuration;

public class BaseXmlArchive extends BaseArchive
{	
	protected void saveOptions(Collection inOptions, Element productelm)
	{
		deleteElements(productelm, "option");
		for (Iterator iter = inOptions.iterator(); iter.hasNext();)
		{
			Option option = (Option) iter.next();
			Element newOption = productelm.addElement("option");
			newOption.addAttribute("id", option.getId());
			newOption.addAttribute("type", option.getDataType());
			newOption.addAttribute("name", option.getName());
			if (option.isRequired())
			{
				newOption.addAttribute("required", "true");
			}
			if (option.getPriceSupport() != null)
			{
				addPrice(newOption, option.getPriceSupport());
			}
			if( option.getValue() != null && option.getValue().length() > 0)
			{
				newOption.addAttribute("value",option.getValue());
			}
		}
	}

	protected Option createOption(Configuration inOptionConfig)
	{
		Option option = new Option();
		option.setName(inOptionConfig.getAttribute("name"));
		option.setId(inOptionConfig.getAttribute("id"));
		option.setDataType(inOptionConfig.getAttribute("type"));
		option.setPriceSupport(createPriceSupport(inOptionConfig));
		String requiredStr = inOptionConfig.getAttribute("required");
		if (requiredStr != null && "true".equalsIgnoreCase(requiredStr))
		{
			option.setRequired(true);
		}
		option.setValue(inOptionConfig.getAttribute("value"));
		if( option.getValue() == null)
		{
			option.setValue(inOptionConfig.getValue());
		}
		return option;
	}

	
	/**
	 * @param elm
	 */
	protected void deleteElements(Element elm, String inName)
	{
		//remove old pricing
		for (Iterator iter = elm.elements(inName).iterator(); iter.hasNext();)
		{
			Element element = (Element) iter.next();
			elm.remove(element);
		}
	}
	
	/**
	 * @param ielement
	 * @param price
	 */
	protected void addPrice(Element inElement, PriceSupport inPriceSupport)
	{
		for (Iterator iter = inPriceSupport.getTiers().iterator(); iter.hasNext();)
		{
			PriceTier priceTier = (PriceTier) iter.next();

			Element tierElement = inElement.addElement("price");
			Price price = priceTier.getPrice();
//			if (price.getSalePrice() != null)
//			{
				Money money = price.getSalePrice();
				if (money != null)
				{
					Element salePriceElement = tierElement.addElement("sale");
					salePriceElement.setText(money.toShortString());
				}
				money = price.getRetailPrice();
				if (money != null)
				{
					Element retailPriceElement = tierElement.addElement("retail");
					retailPriceElement.setText(money.toShortString());
				}
				money = price.getWholesalePrice();
				if (money != null)
				{
					Element wholesalePriceElement = tierElement.addElement("wholesale");
					wholesalePriceElement.setText(money.toShortString());
				}
//			}
//			else
//			{
//				if (price.getRetailPrice() != null)
//				{
//					tierElement.setText(price.getRetailPrice().toShortString());
//				}
//				else
//				{
//					tierElement.setText("0.00");
//				}
//			}
			if ( price.getRegion() != null)
			{
				tierElement.addAttribute("region", price.getRegion() );
			}

			tierElement.addAttribute("quantity", String.valueOf(priceTier.getThresholdQuantity()));
		}
	}
	
	protected PriceSupport createPriceSupport(Configuration inConfig)
	{
		List prices = inConfig.getChildren("price");
		if (prices == null || prices.size() == 0)
		{
			return null;
		}
		PriceSupport priceSupport = null;
		for (Iterator iter = prices.iterator(); iter.hasNext();)
		{
			Configuration priceConfig = (Configuration) iter.next();
			int quantity = Integer.valueOf(priceConfig.getAttribute("quantity")).intValue();

			Price price = new Price();
			String inRegion = priceConfig.getAttribute("region"); //Not used anyplace
			if ( inRegion != null)
			{
				price.setRegion(inRegion);
			}
			else
			{
				
			}

			Configuration salePrice = priceConfig.getChild("sale");
			Configuration retailPrice = priceConfig.getChild("retail");
			Configuration wholesaleprice = priceConfig.getChild("wholesale");
			if (retailPrice != null)
			{
				price.setRetailPrice(new Money(retailPrice.getValue()));
			}
			if (salePrice != null)
			{
				price.setSalePrice(new Money(salePrice.getValue()));
			}
			if (wholesaleprice != null)
			{
				price.setWholesalePrice(new Money(wholesaleprice.getValue()));
			}
			if (retailPrice == null && salePrice == null && priceConfig.getValue() != null)
			{
				price.setRetailPrice(new Money(priceConfig.getValue()));
				System.err.println("&&&&&&& wholesale price not set in product ");
			}
			if( priceSupport == null)
			{
				priceSupport = new PriceSupport();
			}
			priceSupport.addTierPrice(quantity, price);
		}
		return priceSupport;
	}
}
