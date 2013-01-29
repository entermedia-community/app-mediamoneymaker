/*
 * Created on Oct 20, 2003
 */
package org.openedit.store.xmldb;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.openedit.money.Fraction;
import org.openedit.money.Money;
import org.openedit.store.CreditCardType;
import org.openedit.store.HandlingCharge;
import org.openedit.store.ProductPathFinder;
import org.openedit.store.ShippingMethod;
import org.openedit.store.Store;
import org.openedit.store.StoreArchive;
import org.openedit.store.StoreException;
import org.openedit.store.TaxRate;
import org.openedit.store.products.SegmentedProductPathFinder;
import org.openedit.store.shipping.BaseShippingMethod;
import org.openedit.store.shipping.CompositeShippingMethod;
import org.openedit.store.shipping.PriceBasedShippingMethod;
import org.openedit.store.shipping.WeightBasedShippingMethod;

import com.openedit.ModuleManager;
import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.config.XMLConfiguration;
import com.openedit.entermedia.scripts.GroovyScriptRunner;
import com.openedit.entermedia.scripts.Script;
import com.openedit.entermedia.scripts.ScriptManager;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.util.XmlUtil;

/**
 * Creates and sets up our store object.
 * This class is responsible for configuring all the other objects we need
 * @author cburkey
 *
 */
public class XmlStoreArchive implements StoreArchive
{
	private static final Log log = LogFactory.getLog(XmlStoreArchive.class);
	protected ScriptManager fieldScriptManager;
	protected ModuleManager fieldModuleManager;
	protected PageManager fieldPageManager;
	protected Map fieldStores;
	
	/* (non-Javadoc)
	 * @see org.openedit.store.xmldb.StoreArchive#hasChanged(org.openedit.store.Store)
	 */
	public boolean hasChanged(Store inStore) throws OpenEditException
	{
		Page config = getPageManager().getPage(inStore.getStoreHome() + "/configuration/store.xml");
		if (config.getLastModified().getTime() == inStore.getLastModified() )
		{
			return false;
		}
		else
		{
			return true;
		}
	}
	/* (non-Javadoc)
	 * @see org.openedit.store.xmldb.StoreArchive#loadStore(com.openedit.WebPageRequest)
	 */
	public Store loadStore(WebPageRequest inReq) throws StoreException
	{
		Store store = (Store)inReq.getPageValue("store");
		if( store == null)
		{
			store = getStore(inReq.getPage());
			//inReq.putSessionValue("store", store); //Should not use session values if not needed
			inReq.putPageValue("store", store);
		}
		inReq.putPageValue("catalogid", store.getCatalogId());
		return store;
	}
	/* (non-Javadoc)
	 * @see org.openedit.store.xmldb.StoreArchive#getStore(com.openedit.page.Page)
	 */
	public Store getStore(Page inPage) throws StoreException
	{
		String name = inPage.get("catalogid");
		if( name == null)
		{
			name = "store";
			log.error("Should define catalogid property. Defaulting to /store/");
		}
		return getStore( name);
	}
	
	/* (non-Javadoc)
	 * @see org.openedit.store.xmldb.StoreArchive#getStore(java.lang.String)
	 */
	public Store getStore(String inCatalogId) throws StoreException 
	{
		try
		{
			Store store = getStoreForCatalog(inCatalogId);
			if( hasChanged(store) )
			{
				store.clear();
				configureStore(store, inCatalogId);
			}
			return store;
		}
		catch (Exception ex)
		{
			throw new StoreException(ex);
		}
	}
	/**
	 * @param storeFile
	 * @throws StoreException
	 * @throws FileNotFoundException
	 * @throws DocumentException
	 * @throws Exception
	 */
	protected void configureStore(Store inStore, String inCatalogId) throws StoreException, FileNotFoundException, DocumentException, Exception
	{
		log.info("Configuring new store:" + inCatalogId);
		Page config = getPageManager().getPage("/" + inCatalogId + "/configuration/store.xml");
		
		
		inStore.setLastModified( config.getLastModified().getTime() );		
		
		inStore.getProductArchive().clearProducts();
		
		SegmentedProductPathFinder pathFinder = new SegmentedProductPathFinder();
		inStore.setProductPathFinder( pathFinder );

		if( !config.exists())
		{
			return;
		}
		Element rootElement = new XmlUtil().getXml(config.getReader(),config.getCharacterEncoding());
		
		inStore.setConfiguration(new XMLConfiguration(rootElement));
		//configureOrderArchive(rootElement);
		String hostName = rootElement.elementTextTrim("hostName");
		if ( hostName == null)
		{
			//legacy support
			hostName = rootElement.elementTextTrim("hostname");
		}
		inStore.setHostName(hostName);
		inStore.setName(rootElement.elementTextTrim("name"));
		Element coup = rootElement.element("coupons");
		inStore.setCouponsAccepted(coup != null && "true".equals( coup.attributeValue("enabled")));
		
		Element ponum = rootElement.element("ponumber");
		inStore.setCouponsAccepted(coup != null && "true".equals( coup.attributeValue("enabled")));
		
		inStore.setSmtpServer(rootElement.elementTextTrim("smtp-server"));
		inStore.getToAddresses().clear();
		for ( Iterator it = rootElement.elementIterator("to-address"); it.hasNext();)
		{
			Element toAddressElement = (Element)it.next();
			inStore.addToAddress( toAddressElement.getTextTrim());
		}
		inStore.getNotifyAddresses().clear();
		for ( Iterator it = rootElement.elementIterator("notify-address"); it.hasNext();)
		{
			Element toAddressElement = (Element)it.next();
			inStore.addNotifyAddress( toAddressElement.getTextTrim());
		}
		inStore.setFromAddress(rootElement.elementTextTrim("from-address"));
		inStore.setEmailLayout(rootElement.elementTextTrim("email-layout"));
		inStore.setOrderLayout(rootElement.elementTextTrim("order-layout"));
		inStore.setStatusEmailLayout(rootElement.elementTextTrim("status-email-layout"));
		inStore.setUsesPoNumbers(Boolean.parseBoolean(rootElement.elementTextTrim("uses-po-numbers")));
		inStore.setDisplayTermsConditions(Boolean.parseBoolean(rootElement.elementTextTrim("display-terms-conditions")));
		inStore.setUsesBillMeLater(Boolean.parseBoolean(rootElement.elementTextTrim("uses-bill-me-later")));
		inStore.setAllowDuplicateAccounts(Boolean.parseBoolean(rootElement.elementTextTrim("allow-duplicate-accounts")));
		inStore.setAllowSpecialRequest(Boolean.parseBoolean(rootElement.elementTextTrim("allow-special-request")));
		inStore.setAutoCapture(Boolean.parseBoolean(rootElement.elementTextTrim("auto-capture-credit-cards")));
		
		configureCreditCards(inStore, rootElement);
		configureTax(inStore, rootElement);
		//Load up the search capability

		configureShipping(inStore, rootElement);
		configureProductPathFinder(pathFinder, rootElement);
		//inStore.getFieldArchive().setConfigurationPath("/" + inStore.getCatalogId() + "/data/");
		//inStore.getOrderSearch().setFieldArchive(inStore.getFieldArchive());
		//Get Order, Product, Customer Searchers
	}

	/**
	 * @param document
	 * @throws StoreException
	 */
	private void configureShipping(Store inStore, Element rootElement) throws StoreException
	{
		//this is if the user has not selected one and there is only one choice
		//this api can probably be deleted?
		Element assignShippingMethodElem = rootElement.element("assign-shipping-method");
		if ( assignShippingMethodElem != null )
		{
			inStore.setAssignShippingMethod( Boolean.valueOf(assignShippingMethodElem.getTextTrim()).booleanValue() );
		}
		else
		{
			inStore.setAssignShippingMethod( false );
		}

		inStore.getAllShippingMethods().clear();
		
		Iterator methods = rootElement.elementIterator("price-shipping-method");
		if ( !methods.hasNext() )
		{
			//try the old name
			methods = rootElement.elementIterator("shipping-method");
		}
		for (; methods.hasNext();)
		{
			Element method = (Element) methods.next();
			PriceBasedShippingMethod shippingmethod = (PriceBasedShippingMethod)getModuleManager().getBean("priceBasedShipping");
			configureShippingMethod(shippingmethod, method);

			appendHandlingCharge(shippingmethod, method);

			inStore.getAllShippingMethods().add(shippingmethod);
		}
		
		for (Iterator iter = rootElement.elementIterator("weight-shipping-method"); iter.hasNext();)
		{
			Element element = (Element) iter.next();
			WeightBasedShippingMethod shippingmethod = (WeightBasedShippingMethod)getModuleManager().getBean("weightBasedShipping");
			configureShippingMethod(shippingmethod, element);

			appendHandlingCharge(shippingmethod, element);

			inStore.getAllShippingMethods().add(shippingmethod);
		}
		
		

		for (Iterator iter = rootElement.elementIterator("canada-post-shipping-method"); iter.hasNext();)
		{
			Element element = (Element) iter.next();
			
			ShippingMethod shippingmethod = (ShippingMethod)getModuleManager().getBean("canadaPostShipping");
			shippingmethod.configure(element);

			appendHandlingCharge(shippingmethod, element);

			inStore.getAllShippingMethods().add(shippingmethod);
		}

		
		for (Iterator iter = rootElement.elementIterator("scripted-shipping-method"); iter.hasNext();)
		{
			Element element = (Element) iter.next();
			String pathtoscript = element.attributeValue("path");
					
			Script script = getScriptManager().loadScript(pathtoscript);
			GroovyScriptRunner runner = (GroovyScriptRunner)getModuleManager().getBean("groovyScriptRunner");
			ShippingMethod method  = (ShippingMethod)runner.newInstance(script);
		
			
		

			inStore.getAllShippingMethods().add(method);
		}
		
		
		for (Iterator iter = rootElement.elementIterator("composite-shipping-method"); iter.hasNext();)
			
		{
			Element element = (Element) iter.next();
			CompositeShippingMethod method = new CompositeShippingMethod();
			
			for (Iterator iterator = element.elementIterator("scripted-shipping-method"); iterator.hasNext();) {
			Element subelement = (Element) iterator.next();
				
		
			
			String pathtoscript = subelement.attributeValue("path");
					
			Script script = getScriptManager().loadScript(pathtoscript);
			GroovyScriptRunner runner = (GroovyScriptRunner)getModuleManager().getBean("groovyScriptRunner");
			ShippingMethod nextmethod  = (ShippingMethod)runner.newInstance(script);
			method.addShippingMethod(nextmethod);
			}
		

			inStore.getAllShippingMethods().add(method);
		}
		
		
	}

	public ScriptManager getScriptManager() {
		return fieldScriptManager;
	}
	public void setScriptManager(ScriptManager fieldScriptManager) {
		this.fieldScriptManager = fieldScriptManager;
	}
	
	private void configureShippingMethod(BaseShippingMethod shippingmethod, Element method)
	{
		shippingmethod.setDescription(method.attributeValue("description"));
		shippingmethod.setId(method.attributeValue("id"));
		String costStr = method.attributeValue("costs");
		if (costStr != null)
		{
			Money cost = new Money(costStr);
			shippingmethod.setCost(cost);
		}
		String percentageCostStr = method.attributeValue("percentageCosts");
		if ( percentageCostStr != null )
		{
			shippingmethod.setPercentageCost(Double.parseDouble(percentageCostStr));
		}
		String lowerThresholdStr = method.attributeValue("lowerThreshold");
		if (lowerThresholdStr != null)
		{
			shippingmethod.setLowerThreshold(new Money(lowerThresholdStr));
		}
		String upperThresholdStr = method.attributeValue("upperThreshold");
		if (upperThresholdStr != null)
		{
			shippingmethod.setUpperThreshold(new Money(upperThresholdStr));
		}
		String hidden = method.attributeValue("hidden");
		shippingmethod.setHidden(Boolean.parseBoolean(hidden));
	}

	private void appendHandlingCharge(ShippingMethod shippingmethod, Element method)
	{
		for ( Iterator handlingCharges = method.elementIterator("handling-charge");
			handlingCharges.hasNext(); )
		{
			Element handlingChargeElem = (Element) handlingCharges.next();
			HandlingCharge handlingCharge = new HandlingCharge();
			handlingCharge.setLevel(handlingChargeElem.attributeValue("level"));
			String cost = handlingChargeElem.attributeValue("costs");
			if ( cost != null)
			{
				handlingCharge.setCost(new Money(cost));
			}
			else
			{
				handlingCharge.setCost(Money.ZERO);
			}
			String additionalCosts = handlingChargeElem.attributeValue("additionalCosts");
			if ( additionalCosts != null )
			{
				handlingCharge.setAdditionalCosts( additionalCosts.equalsIgnoreCase( "true" ) );
			}
			shippingmethod.addHandlingCharge(handlingCharge);
		}
	}

	/**
	 * @throws StoreException

	private void configureSearch() throws StoreException
	{
		getProductSearch().setSearchDirectory(new File(getStore().getStoreDirectory(), PRODUCTS_DIR));
		getStore().setStoreSearcher(getProductSearch());
		getStore().getCustomerArchive().setCustomersDirectory(new File( getStore().getStoreDirectory().getParentFile(),"WEB-INF/users/"));
	}
	*/
	
	/**
	 * @param inElement
	 */
	private void configureTax(Store inStore, Element inElement)
	{
		for (Iterator iter = inElement.elementIterator("tax"); iter.hasNext();)
		{
			Element element = (Element) iter.next();
			String state = element.attributeValue("statecode");
			String applytoshipping = element.attributeValue("applytoshipping");
			String name = element.attributeValue("name");
			
			Fraction rate = new Fraction(element.attributeValue("rate"));
			TaxRate tax = new TaxRate();
			tax.setFraction(rate);
			tax.setName(name);
			tax.setState(state);
			tax.setApplyToShipping(Boolean.parseBoolean(applytoshipping));
			inStore.putTaxRate(tax);
		}
	}


	/**
	 * @param inElement
	 */
	private void configureCreditCards(Store inStore, Element inElement)
	{
		for (Iterator iter = inElement.elementIterator("credit-card-type"); iter.hasNext();)
		{
			Element element = (Element) iter.next();

			CreditCardType type = new CreditCardType(element.attributeValue("name"));
			type.setId(element.attributeValue("id"));

			inStore.addCreditCardType(type);
		}
	}
	
	/**
	 * Sets the {@link ProductPathFinder} on the store from the
	 * <tt>&lt;default-product-paths&gt;</tt> or the
	 * <tt>&lt;segmented-product-paths&gt;</tt> elements within the given root
	 * element.
	 * 
	 * @param inRootElement  The store root element
	 */
	private void configureProductPathFinder( SegmentedProductPathFinder pathFinder, Element inRootElement )
		throws StoreException
	{
		Element element = inRootElement.element( "segmented-product-paths" );
		if ( element != null )
		{
			Element segmentLengthElem = element.element( "segment-length" );
			if ( segmentLengthElem != null )
			{
				pathFinder.setSegmentLength(
					Integer.parseInt( segmentLengthElem.getTextTrim() ) );
			}
			Element reverse = element.element( "reverse" );
			if ( reverse != null )
			{
				pathFinder.setReverse(Boolean.parseBoolean( reverse.getTextTrim() ) );
			}
			Element group = element.element( "groupincategory" );
			if ( group != null )
			{
				pathFinder.setGroupInTopCategory(Boolean.parseBoolean( group.getTextTrim() ) );
			}
		}
	}
	
	
	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}


	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}
	public Map getStores()
	{
		if (fieldStores == null)
		{
			fieldStores = new HashMap();
		}
		return fieldStores;
	}
	/* (non-Javadoc)
	 * @see org.openedit.store.xmldb.StoreArchive#getStoreForCatalog(java.lang.String)
	 */
	public Store getStoreForCatalog( String inCatalogId) throws Exception
	{
		Store store = (Store)getStores().get( inCatalogId );
		if( store == null)
		{
			store = (Store)getModuleManager().getBean("store"); //singleton=false
			store.setCatalogId(inCatalogId);
			store.getCategoryArchive().setCatalogId(inCatalogId);
			store.getProductArchive().setCatalogId(inCatalogId);

			configureStore(store, inCatalogId);

			getStores().put( inCatalogId, store);
		}
		return store;
		
	}
	
}