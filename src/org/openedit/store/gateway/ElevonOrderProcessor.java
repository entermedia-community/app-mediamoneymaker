package org.openedit.store.gateway;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.openedit.money.Money;
import org.openedit.store.CreditPaymentMethod;
import org.openedit.store.Store;
import org.openedit.store.StoreException;
import org.openedit.store.customer.Customer;
import org.openedit.store.orders.BaseOrderProcessor;
import org.openedit.store.orders.Order;
import org.openedit.store.orders.OrderState;

import com.openedit.WebPageRequest;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.util.XmlUtil;

public class ElevonOrderProcessor extends BaseOrderProcessor {

	private static final Log log = LogFactory
			.getLog(ElevonOrderProcessor.class);
	protected PageManager fieldPageManager;
	protected XmlUtil fieldXmlUtil;

	public XmlUtil getXmlUtil() {
		if (fieldXmlUtil == null) {
			fieldXmlUtil = new XmlUtil();
		}
		return fieldXmlUtil;
	}

	public void setXmlUtil(XmlUtil inXmlUtil) {
		fieldXmlUtil = inXmlUtil;
	}

	public PageManager getPageManager() {
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager) {
		fieldPageManager = inPageManager;
	}

	protected boolean requiresValidation(Store inStore, Order inOrder) {

		Page page = getPageManager().getPage(
				inStore.getStoreHome() + "/configuration/elevon.xml");
		if (page.exists()) {
			return inOrder.getPaymentMethod().requiresValidation();
		}
		return false;
	}

	public void processNewOrder(WebPageRequest inContext, Store inStore,
			Order inOrder) throws StoreException {
		if (!requiresValidation(inStore, inOrder)) {
			return;
		}
		// "AUTH_ONLY"); //AUTH_CAPTURE, AUTH_ONLY, CAPTURE_ONLY, CREDIT, VOID,
		// PRIOR_AUTH_CAPTURE.
		if (inStore.isAutoCapture()) {
			process(inStore, inOrder, "AUTH_CAPTURE");
		} else {
			process(inStore, inOrder, "AUTH_ONLY");
		}
	}

	protected void process(Store inStore, Order inOrder, String inType)
			throws StoreException {
		try {
			// See examples at http://www.jcommercesql.com/anet/
			// load properties (e.g. IP address, username, password) for
			// accessing authorize.net
			// load customer address info from order (in case needed for AVS)
			Page page = getPageManager().getPage(
					inStore.getStoreHome() + "/configuration/elevon.xml");
			Element conf = getXmlUtil().getXml(page.getReader(), "UTF-8");
			String merchant = conf.element("merchantid").getText();
			String user = conf.element("userid").getText();
			String pin = conf.element("pin").getText();

			Customer customer = inOrder.getCustomer();

			HttpClient client = new HttpClient();
			String url = "https://www.myvirtualmerchant.com/VirtualMerchant/processxml.do";
			PostMethod post = new PostMethod(url);

			CreditPaymentMethod creditCard = (CreditPaymentMethod) inOrder
					.getPaymentMethod();
			Money total = inOrder.getTotalPrice();

			Element root = DocumentHelper.createElement("txn");
			root.addElement("ssl_merchant_ID").setText(merchant);
			root.addElement("ssl_user_id").setText(user);
			root.addElement("ssl_pin").setText(pin);
			root.addElement("ssl_transaction_type").setText("ccsale");
			root.addElement("ssl_card_number").setText(
					creditCard.getCardNumber());
			root.addElement("ssl_exp_date").setText(
					(creditCard.getExpirationDateString()).replace("/", ""));
			Money rounded = total.round();
			root.addElement("ssl_amount").setText(
					String.valueOf(rounded.doubleValue()));
			root.addElement("ssl_salestax")
					.setText(inOrder.getTax().toString());
			root.addElement("ssl_cvv2cvc2_indicator").setText("1");
			root.addElement("ssl_cvv2cvc2").setText(
					creditCard.getCardVerificationCode());
			root.addElement("ssl_invoice_number").setText(
					inOrder.getOrderNumber());
			root.addElement("ssl_customer_code").setText(customer.getId());
			root.addElement("ssl_first_name").setText(customer.getFirstName());
			root.addElement("ssl_last_name").setText(customer.getLastName());
			root.addElement("ssl_avs_address").setText(
					customer.getBillingAddress().getAddress1());
			String address2 = customer.getBillingAddress().getAddress2();
			if (address2 != null) {
				root.addElement("ssl_address2").setText(address2);
			}
			root.addElement("ssl_city").setText(
					customer.getBillingAddress().getCity());
			root.addElement("ssl_state").setText(
					customer.getBillingAddress().getState());
			root.addElement("ssl_avs_zip").setText(
					customer.getBillingAddress().getZipCode());
			String phone1 = customer.getPhone1();
			if (phone1 != null) {
				root.addElement("ssl_phone").setText(phone1);
			}
			String email = customer.getEmail();
			if (email != null) {
				root.addElement("ssl_email").setText(email);
			}
			post.addParameter("xmldata", root.asXML());
			
			int result = client.executeMethod(post);
			OrderState orderState = null;

			String responsebody = post.getResponseBodyAsString();
			Document document = DocumentHelper.parseText(responsebody);
			Element root1 = document.getRootElement();
			String ssl_result, ssl_message;
			Element element = root1.element("ssl_result");
			if (element == null) {
				ssl_result = "1";// failed
				ssl_message = "Failed due to incorrect config.  Check elevon.xml"
						+ " error Code: "
						+ root1.element("errorCode").getText()
						+ " error Name: "
						+ root1.element("errorName").getText();

			} else {
				ssl_result = element.getText();
				ssl_message = root1.element("ssl_result_message").getText();
				String ssl_amount = root1.element("ssl_amount").getText();
			}
			if ("0".equals(ssl_result)) {
				// transaction approved
				// super.exportNewOrder(inContext, inStore, inOrder);

				orderState = inStore.getOrderState(Order.AUTHORIZED);
				orderState
						.setDescription("Your transaction has been authorized.");
				orderState.setOk(true);
			} else {
				// transaction declined
				log.warn("Transaction DECLINED for order #" + inOrder.getId());
				log.warn("Response code:" + ssl_result);
				log.warn("Response Message: " + ssl_message);

				String error = "Your transaction has been declined.  Please hit the back button on your browser to correct.<br>";
				error += " (Full Code:  " + ssl_result + "." + ssl_message
						+ ")";
				//error =error += "<br> " + root.asXML();
				orderState = inStore.getOrderState(Order.REJECTED);
				orderState.setDescription(error);
				orderState.setOk(false);
			}
			inOrder.setOrderState(orderState);
		} catch (Exception e) {
			OrderState orderState = new OrderState();
			orderState
					.setDescription("An error occurred while processing your transaction.");
			orderState.setOk(false);
			inOrder.setOrderState(orderState);
			e.printStackTrace();
			throw new StoreException(e);
		}
	}

	public void captureOrder(WebPageRequest inContext, Store inStore,
			Order inOrder) throws StoreException {
		if (!requiresValidation(inStore, inOrder)) {
			return;
		}
		process(inStore, inOrder, "");
	}
}
