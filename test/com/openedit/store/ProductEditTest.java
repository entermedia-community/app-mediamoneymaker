/*
 * Created on Apr 30, 2004
 */
package com.openedit.store;

import org.openedit.money.Money;
import org.openedit.store.InventoryItem;
import org.openedit.store.Option;
import org.openedit.store.Price;
import org.openedit.store.PriceSupport;
import org.openedit.store.Product;
import org.openedit.store.StoreTestCase;

/**
 * @author cburkey
 *
 */
public class ProductEditTest extends StoreTestCase
{

	/**
	 * Constructor for ItemEditTest.
	 * @param arg0
	 */
	public ProductEditTest(String arg0)
	{
		super(arg0);
	}

	
	public void testEditProductProperties() throws Exception
	{
		String originaltext = "Some weird & whacky product attribute's to \"insert\"";
		Product product = getStore().getProduct("1");
		product.addProperty("insertdata",originaltext);
		 
		getStore().saveProduct(product);
		getStore().clear();
		product = getStore().getProduct("1");
		String returntext = product.getProperty("insertdata");
		assertEquals(originaltext, returntext);
		
	}

	public void testEditItemProperties() throws Exception
	{
		String originaltext = "weird & whacky data insert'ed at \"item\" level";
		Product product = getStore().getProduct("1");
		InventoryItem item = product.getInventoryItem(0);
		assertNotNull(item);
		
		item.addProperty("insertdata",originaltext);
		 
		getStore().saveProduct(product);
		getStore().clearProducts();
		product = getStore().getProduct("1");
		item = product.getInventoryItem(0);
		String returntext = item.getProperty("insertdata");
		assertTrue(originaltext.equals( returntext) );
		
	}
	
	public void testAvailableFlag() throws Exception
	{
		Product product = getStore().getProduct("1");
		product.setAvailable(false);
		getStore().saveProduct(product);
		getStore().clearProducts();
		//Thread.sleep(500); //we need a pause here so the data is reloaded off the disk drive
		product = getStore().getProduct("1");
		assertFalse( product.isAvailable());
		
		product.setAvailable(true);
		getStore().saveProduct(product);
		getStore().clearProducts();
		product = getStore().getProduct("1");
		assertTrue( product.isAvailable());
	}

	public void xxtestOptions() throws Exception
	{
		Product product = getStore().getProduct("2");
		product.clearOptions();

		final String OPTION_NAME = "Mandatory Gold Plating";
		final String OPTION_ID = "gold";
		final Money OPTION_COST = new Money("750.00");

		Option option = new Option();
		option.setName(OPTION_NAME);
		option.setId(OPTION_ID);
		option.setRequired(true);
		option.addTierPrice(1, new Price(OPTION_COST));
		product.addOption(option);

		getStore().saveProduct(product);
		getStore().clearProducts();

		product = getStore().getProduct("2");
		assertTrue(product.getOptions().size() == 1);

		option = (Option)product.getOptions().get(0);
		assertTrue(option.isRequired());
		assertEquals(OPTION_NAME, option.getName());
		assertEquals(OPTION_ID, option.getId());
		PriceSupport ps = option.getPriceSupport();
		assertNotNull(ps);
		assertTrue(ps.getTiers().size() == 1);
		assertEquals(OPTION_COST, ps.getYourPriceByQuantity(1));

		product.clearOptions();
		getStore().saveProduct(product);
	}
	
	public void xxtestInventoryPrices() throws Exception{
		Product product = getStore().getProduct("2");
		product.clearItems();
		product.clearOptions();
		
		final Money retail = new Money("799.00");
		final Money wholesale = new Money("700.00");
		final Money sale = new Money("759.99");
		
		Price price = new Price();
		price.setRetailPrice(retail);
		price.setWholesalePrice(wholesale);
		price.setSalePrice(sale);
		
		InventoryItem item = new InventoryItem();
		item.addTierPrice(1, price);
		product.addInventoryItem(item);

		PriceSupport ps = item.getPriceSupport();
		assertNotNull(ps);
		assertTrue(ps.getTiers().size() == 1);
		assertEquals(sale, ps.getYourPriceByQuantity(1));
		assertEquals(wholesale, ps.getWholesalePriceByQuantity(1));

		
		
		getStore().saveProduct(product);

		 ps = item.getPriceSupport();
		assertNotNull(ps);
		assertTrue(ps.getTiers().size() == 1);
		assertEquals(sale, ps.getYourPriceByQuantity(1));
		assertEquals(wholesale, ps.getWholesalePriceByQuantity(1));

		
		
		
		getStore().clearProducts();
		
		
		
		product = getStore().getProduct("2");
		
		assertTrue(product.getInventoryItems().size() == 1);
		
		item = product.getInventoryItem(0);
		ps = item.getPriceSupport();
		assertNotNull(ps);
		assertTrue(ps.getTiers().size() == 1);
		assertEquals(sale, ps.getYourPriceByQuantity(1));
		assertEquals(wholesale, ps.getWholesalePriceByQuantity(1));

//		product.clearItems();
//		product.clearOptions();
//		getStore().saveProduct(product);
	}
	
	public void xxtestOptionPrices() throws Exception{
		Product product = getStore().getProduct("2");
		product.clearItems();
		product.clearOptions();
		
		final Money retail = new Money("799.00");
		final Money wholesale = new Money("700.00");
		final Money sale = new Money("759.99");
		
		Price price = new Price();
		price.setRetailPrice(retail);
		price.setWholesalePrice(wholesale);
//		price.setSalePrice(sale);
		
		final String OPTION_NAME = "Mandatory Gold Plating";
		final String OPTION_ID = "gold";

		Option option = new Option();
		option.setName(OPTION_NAME);
		option.setId(OPTION_ID);
		option.setRequired(true);
		option.addTierPrice(1, price);
		product.addOption(option);
		
		getStore().saveProduct(product);
		getStore().clearProducts();

		product = getStore().getProduct("2");
		assertTrue(product.getOptions().size() == 1);
		
		option = (Option)product.getOptions().get(0);
		PriceSupport ps = option.getPriceSupport();
		assertNotNull(ps);
		assertTrue(ps.getTiers().size() == 1);
		assertEquals(sale, ps.getYourPriceByQuantity(1));
		assertEquals(wholesale, ps.getWholesalePriceByQuantity(1));
		
//		product.clearItems();
//		product.clearOptions();
//		getStore().saveProduct(product);
		
	}


}
