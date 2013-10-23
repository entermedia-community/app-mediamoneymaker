package org.openedit.store.modules;


import org.openedit.cart.paypal.PaypalUtil;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.modules.MediaArchiveModule;

import com.openedit.WebPageRequest;

public class PaypalModule extends MediaArchiveModule{
	protected PaypalUtil fieldPaypalUtil;
	
	public void handleIPN(WebPageRequest inReq) throws Exception{
		String result = getPaypalUtil().handleIPN(inReq);
		inReq.putPageValue("paymentResult", result);
		System.out.print(result);
		
	}

	public void handlePDT(WebPageRequest inReq) throws Exception{
		String result = getPaypalUtil().handlePDT(inReq);
		inReq.putPageValue("paymentResult", result);
		 MediaArchive archive  = getMediaArchive(inReq);
		 
		//the result should be matched to an order....
		
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
}
