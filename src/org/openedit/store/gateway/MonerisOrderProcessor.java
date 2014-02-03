package org.openedit.store.gateway;

import java.net.URLDecoder;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.openedit.data.SearcherManager;
import org.openedit.store.CreditPaymentMethod;
import org.openedit.store.Store;
import org.openedit.store.StoreException;
import org.openedit.store.orders.BaseOrderProcessor;
import org.openedit.store.orders.Order;
import org.openedit.store.orders.OrderState;
import org.openedit.store.orders.Refund;

import JavaAPI.AvsInfo;
import JavaAPI.CvdInfo;
import JavaAPI.HttpsPostRequest;
import JavaAPI.Purchase;
import JavaAPI.Receipt;

import com.openedit.WebPageRequest;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.users.User;
import com.openedit.users.UserManager;
import com.openedit.util.XmlUtil;

public class MonerisOrderProcessor extends BaseOrderProcessor
{

	private static final Log log = LogFactory.getLog(MonerisOrderProcessor.class);
	protected PageManager fieldPageManager;
	protected XmlUtil fieldXmlUtil;

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager fieldSearcherManager)
	{
		this.fieldSearcherManager = fieldSearcherManager;
	}

	protected UserManager fieldUserManager;
	protected SearcherManager fieldSearcherManager;

	public XmlUtil getXmlUtil()
	{
		if (fieldXmlUtil == null)
		{
			fieldXmlUtil = new XmlUtil();
		}
		return fieldXmlUtil;
	}

	public void setXmlUtil(XmlUtil inXmlUtil)
	{
		fieldXmlUtil = inXmlUtil;
	}

	public UserManager getUserManager()
	{
		return fieldUserManager;
	}

	public void setUserManager(UserManager inUserManager)
	{
		fieldUserManager = inUserManager;
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	protected boolean requiresValidation(Store inStore, Order inOrder)
	{

		if (inStore.get("gateway") != null && inStore.get("gateway").equals("moneris"))
		{
			return true;
		}

		return false;
	}

	public void processNewOrder(WebPageRequest inContext, Store inStore, Order inOrder) throws StoreException
	{
		if (!requiresValidation(inStore, inOrder))
		{
			return;
		}
		process(inStore, inOrder, "AUTH_CAPTURE");

	}

	protected void process(Store inStore, Order inOrder, String inType) throws StoreException
	{
//		String host = "esqa.moneris.com";
//		String store_id = "store5";
//		String api_token = "yesguy";
		
		String host = inStore.get("moneris_host");
		String store_id = inStore.get("moneris_store_id");
		String api_token = inStore.get("moneris_api_token");
		
		String order_id = inOrder.getId();
		String amount = inOrder.getTotalPrice().toShortString();
		CreditPaymentMethod cc = (CreditPaymentMethod) inOrder.getPaymentMethod();
		String pan = cc.getCardNumber();
		String expdate = cc.getExpirationDateString().replace("/", "");
		String crypt = "7";

		// AvsInfo avs = new AvsInfo ("123", "Edgar Street", "M1M1M1");
		CvdInfo cvd = new CvdInfo("1", cc.getCardVerificationCode());

		Purchase p = new Purchase(order_id, amount, pan, expdate, crypt);

		// p.setAvsInfo (avs);
		p.setCvdInfo(cvd);

		try
		{
			HttpsPostRequest mpgReq = new HttpsPostRequest(host, store_id, api_token, p, false);
			Receipt receipt = mpgReq.getReceipt();
			StringBuilder buf = new StringBuilder();
			buf.append("Receipt Details:\n\tCardType = " + receipt.getCardType()).append("\n\t");
			buf.append("TransAmount = " + receipt.getTransAmount()).append("\n\t");
			buf.append("TxnNumber = " + receipt.getTxnNumber()).append("\n\t");
			buf.append("ReceiptId = " + receipt.getReceiptId()).append("\n\t");
			buf.append("TransType = " + receipt.getTransType()).append("\n\t");
			buf.append("ReferenceNum = " + receipt.getReferenceNum()).append("\n\t");
			buf.append("ResponseCode = " + receipt.getResponseCode()).append("\n\t");
			buf.append("ISO = " + receipt.getISO()).append("\n\t");
			buf.append("BankTotals = " + receipt.getBankTotals()).append("\n\t");
			buf.append("Message = " + receipt.getMessage()).append("\n\t");
			buf.append("AuthCode = " + receipt.getAuthCode()).append("\n\t");
			buf.append("Complete = " + receipt.getComplete()).append("\n\t");
			buf.append("TransDate = " + receipt.getTransDate()).append("\n\t");
			buf.append("TransTime = " + receipt.getTransTime()).append("\n\t");
			buf.append("Ticket = " + receipt.getTicket()).append("\n\t");
			buf.append("TimedOut = " + receipt.getTimedOut()).append("\n\t");
			buf.append("StatusCode = " + receipt.getStatusCode()).append("\n\t");
			buf.append("StatusMessage = " + receipt.getStatusMessage()).append("\n\t");
			buf.append("IsVisaDebit = " + receipt.getIsVisaDebit());
			log.info(buf.toString());
			
			/*
			 * CardType = V TransAmount = 25.99 TxnNumber = 89175-0_9 ReceiptId
			 * = WEB0000003 TransType = 00 ReferenceNum = 660109290015501270
			 * ResponseCode = 027 ISO = 01 BankTotals = null Message = APPROVED
			 * * = AuthCode = 208650 Complete = true TransDate = 2014-01-02
			 * TransTime = 12:37:18 Ticket = null TimedOut = false StatusCode =
			 * null StatusMessage = null IsVisaDebit = false
			 */

			if (receipt.getMessage() != null && receipt.getMessage().contains("APPROVED"))
			{
				OrderState orderState = inStore.getOrderState(Order.AUTHORIZED);
				orderState.setDescription("Your transaction has been authorized.");
				orderState.setOk(true);
				inOrder.setOrderState(orderState);
				inOrder.setProperty("txn_number", receipt.getTxnNumber());

			}
			else
			{
				OrderState orderState = inStore.getOrderState(Order.REJECTED);
				orderState.setDescription(receipt.getMessage());
				orderState.setOk(false);
				inOrder.setOrderState(orderState);
			}

		}

		catch (Exception e)
		{
			OrderState orderState = inStore.getOrderState(Order.REJECTED);
			e.printStackTrace();
			orderState.setDescription("An error occurred while processing your transaction.");
			orderState.setOk(false);
			inOrder.setOrderState(orderState);
		}

	}

	public void captureOrder(WebPageRequest inContext, Store inStore, Order inOrder) throws StoreException
	{
		if (!requiresValidation(inStore, inOrder))
		{
			return;
		}
		process(inStore, inOrder, "");
	}

	@Override
	public void refundOrder(WebPageRequest inContext, Store inStore, Order inOrder, Refund inRefund) throws StoreException
	{
		String host = inStore.get("moneris_host");
		String store_id = inStore.get("moneris_store_id");
		String api_token = inStore.get("moneris_api_token");
		
		String order_id = inOrder.getId();
		String amount = inRefund.getTotalAmount().toShortString();
		
		String crypt = "7";
		String txn_number = inOrder.get("txn_number");
		
		JavaAPI.Refund refund = new JavaAPI.Refund(order_id, amount, txn_number, crypt);
		HttpsPostRequest mpgReq = new HttpsPostRequest(host, store_id, api_token, refund);
		try
		{
			Receipt receipt = mpgReq.getReceipt();
			StringBuilder buf = new StringBuilder();
			buf.append("Receipt Details:\n\tCardType = " + receipt.getCardType()).append("\n\t");
			buf.append("TransAmount = " + receipt.getTransAmount()).append("\n\t");
			buf.append("TxnNumber = " + receipt.getTxnNumber()).append("\n\t");
			buf.append("ReceiptId = " + receipt.getReceiptId()).append("\n\t");
			buf.append("TransType = " + receipt.getTransType()).append("\n\t");
			buf.append("ReferenceNum = " + receipt.getReferenceNum()).append("\n\t");
			buf.append("ResponseCode = " + receipt.getResponseCode()).append("\n\t");
			buf.append("ISO = " + receipt.getISO()).append("\n\t");
			buf.append("BankTotals = " + receipt.getBankTotals()).append("\n\t");
			buf.append("Message = " + receipt.getMessage()).append("\n\t");
			buf.append("AuthCode = " + receipt.getAuthCode()).append("\n\t");
			buf.append("Complete = " + receipt.getComplete()).append("\n\t");
			buf.append("TransDate = " + receipt.getTransDate()).append("\n\t");
			buf.append("TransTime = " + receipt.getTransTime()).append("\n\t");
			buf.append("Ticket = " + receipt.getTicket()).append("\n\t");
			buf.append("TimedOut = " + receipt.getTimedOut());
			log.info(buf.toString());
			
			if(receipt.getMessage() != null && receipt.getMessage().contains("APPROVED")){
				inRefund.setSuccess(true);
				inRefund.setAuthorizationCode(receipt.getAuthCode());
				inRefund.setTransactionId(receipt.getTxnNumber());
				inRefund.setDate(new Date());
			} else{
				inRefund.setSuccess(false);
				String message = receipt.getMessage();
				message = message!=null ? URLDecoder.decode(message,"UTF-8").replaceAll("\\<.*?\\>", "") : "Unknown declined response";
				inRefund.setMessage(message);
				inRefund.setDate(new Date());//or parse trnDate
				
			}
		}
		catch (Exception e)
		{
			inRefund.setSuccess(false);
			inRefund.setMessage("An error occurred while processing your transaction.");
			e.printStackTrace();
			throw new StoreException(e);
		}

	}
}
