/*
 * Created on Oct 5, 2004
 */
package org.openedit.cart.paypal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermedia.email.PostMail;
import org.openedit.store.CreditPaymentMethod;
import org.openedit.store.Store;
import org.openedit.store.StoreException;
import org.openedit.store.orders.BaseOrderProcessor;
import org.openedit.store.orders.Order;
import org.openedit.store.orders.OrderProcessor;
import org.openedit.store.orders.Refund;

import com.openedit.ModuleManager;
import com.openedit.WebPageRequest;
import com.openedit.page.manage.PageManager;

/**
 * @author Ian Miller, ian@ijsolutions.ca
 *
 */
public class PaypalOrderProcessor extends BaseOrderProcessor implements OrderProcessor 
{
	private static final Log log = LogFactory.getLog( PaypalOrderProcessor.class );
	protected PageManager fieldPageManager;
	private PostMail postMail;
	protected PaypalUtil fieldPaypalUtil;
	protected ModuleManager fieldModuleManager;
	
	public ModuleManager getModuleManager() {
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager moduleManager) {
		this.fieldModuleManager = moduleManager;
	}

	public PostMail getPostMail() {
		return postMail;
	}

	public void setPostMail(PostMail postMail) {
		this.postMail = postMail;		
	}

	/* (non-javadoc)
	 * @see com.openedit.store.OrderArchive#exportNewOrder(com.openedit.store.Cart)
	 */
	public void processNewOrder( WebPageRequest inContext, Store inStore,
		Order inOrder ) throws StoreException
	{
		String usepaypal = inContext.getPageProperty("usepaypal");
		if(Boolean.parseBoolean(usepaypal)){
			try {
				String result = getPaypalUtil().handlePDT(inContext);
				inContext.putPageValue("paymentResult", result);
				if("SUCCESS".equals(result)){
				
					CreditPaymentMethod creditCard = (CreditPaymentMethod) inOrder.getPaymentMethod();
					creditCard.setNote(creditCard.getNote()  + "(This was a paypal credit card payment)");
					inOrder.getOrderState().setOk(true);
					
				}
				else {
					log.info("paypal validation false");
					inOrder.getOrderState().setOk(false);
				}
			} catch (Exception e) {
				return;
			}
			
	
			//inOrder.getOrderState().setDescription("Order accepted");
		}
		return;
			
	
	}

	/**
	 * @param inOrder
	 * @return
	 */
	protected boolean requiresValidation(Order inOrder)
	{
		
		return inOrder.getPaymentMethod().requiresValidation();
	}
	protected PageManager getPageManager()
	{
		return fieldPageManager;
	}
	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}
	public PaypalUtil getPaypalUtil() {
		if (fieldPaypalUtil == null) {
			fieldPaypalUtil = new PaypalUtil();
			
		}

		return fieldPaypalUtil;
	}

	public void setPaypalUtil(PaypalUtil paypalUtil) {
		this.fieldPaypalUtil = paypalUtil;
	}

	@Override
	public void refundOrder(WebPageRequest inContext, Store inStore,
			Refund inRefund) throws StoreException {
		// TODO Auto-generated method stub
		
	}

}