/*
 * Created on Oct 5, 2004
 */
package org.openedit.store.orders;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermedia.email.PostMail;
import org.entermedia.email.Recipient;
import org.entermedia.email.TemplateWebEmail;
import org.openedit.store.Store;
import org.openedit.store.StoreException;

import com.openedit.WebPageRequest;
import com.openedit.page.Page;
import com.openedit.page.PageProperty;
import com.openedit.page.manage.PageManager;

/**
 * @author cburkey
 *
 */
public class EmailOrderProcessor extends BaseOrderProcessor implements OrderProcessor
{
	private static final Log log = LogFactory.getLog( EmailOrderProcessor.class );
	protected PageManager fieldPageManager;
	private PostMail postMail;

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
		try
		{
			//notifyStoreOwners(inStore, inOrder);
			//TODO: PathUtilities.buildRelative(store.getThanksLayout(),
			Page clerkLayout= getPageManager().getPage(inStore.getOrderLayout() );
			if (!clerkLayout.exists()) {
				throw new StoreException("Clerklayout" + clerkLayout + "does not exist or is invalid");
			}
			Page customerLayout = getPageManager().getPage(inStore.getEmailLayout() );
			if (!customerLayout.exists()) {
				throw new StoreException("Customerlayout" + customerLayout + "does not exist or is invalid");
			}

			TemplateWebEmail mailer = postMail.getTemplateWebEmail();
			mailer.setFrom( inStore.getFromAddress() );
			mailer.setWebPageContext(inContext);
			// Email clerk
			PageProperty prop = new PageProperty("customeremail");
			prop.setPath(inContext.getPath());
			prop.setValue(inOrder.getCustomer().getEmail());
			inContext.getPage().getPageSettings().getProperties().put("customeremail", prop);
			String subject = inContext.findValue("emailsubject");
			if( subject == null)
			{
				subject =  inStore.getName() + " New Order: " + inOrder.getCustomer().getEmail();
			}
			mailer.setSubject(subject);
			
			if ( inStore.getToAddresses().size() > 0)
			{
				mailer.setRecipientsFromStrings(inStore.getToAddresses());
				mailer.setMailTemplatePage(clerkLayout);				
				mailer.send();
			}
			// Email customer
			mailer.setSubject( subject );
			String email = inOrder.getCustomer().getEmail();
			if(email == null){
				email = inContext.getUser().getEmail();
			}
			Recipient recipient = new Recipient();
			recipient.setEmailAddress(email);
			recipient.setLastName(inOrder.getCustomer().getLastName());
			recipient.setFirstName(inOrder.getCustomer().getFirstName());
			mailer.setRecipient(recipient);
			mailer.setMailTemplatePage(customerLayout);
			mailer.send();
			
			//inOrder.getOrderState().setDescription("Order accepted");
			inOrder.getOrderState().setOk(true);
			
		}
		catch ( Exception e )
		{
			log.error( "Could not email this order request:\n"
				+ inOrder.getCustomer(), e );
			inOrder.getOrderState().setDescription("Order could not be sent " + e.getMessage());
			inOrder.getOrderState().setOk(false);
			throw new StoreException( e );
		}
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
	public void sendReceipt( WebPageRequest inContext, Store inStore,
			Order inOrder ) throws StoreException
		{
			try
			{
				//notifyStoreOwners(inStore, inOrder);
				//TODO: PathUtilities.buildRelative(store.getThanksLayout(),
				Page statusLayout= getPageManager().getPage(inStore.getStatusEmailLayout());
				if (statusLayout == null || !statusLayout.exists()) {
					throw new StoreException("StatusEmailLayout" + statusLayout + "does not exist or is invalid");
				}
				TemplateWebEmail mailer = postMail.getTemplateWebEmail();
				mailer.setFrom( inStore.getFromAddress() );
				
				// Email customer
				String email = inOrder.getCustomer().getEmail();
				if(email == null){
					email = inContext.getUser().getEmail();
				}
				Recipient recipient = new Recipient();
				recipient.setEmailAddress(email);
				recipient.setLastName(inOrder.getCustomer().getLastName());
				recipient.setFirstName(inOrder.getCustomer().getFirstName());
				mailer.setRecipient(recipient);
				
				/* Try to get the mail subject from inContext.findValue("subject")
				 * "$customeremail" can be used there to get the customer's email address
				 * If it is not there then use a hard-coded one.
				 */
				PageProperty prop = new PageProperty("customeremail");
				prop.setPath(inContext.getPath());
				prop.setValue(inOrder.getCustomer().getEmail());
				inContext.getPage().getPageSettings().getProperties().put("customeremail", prop);
				mailer.loadSettings(inContext);
				if (mailer.getSubject() == null)
				{
					String subject =  inStore.getName() + " Update to Order: " + inOrder.getCustomer().getEmail();
					mailer.setSubject(subject);
				}
				mailer.setMailTemplatePage(statusLayout);
				mailer.send();
				
				//inOrder.getOrderState().setDescription("Order accepted");
				inOrder.getOrderStatus().setOk(true);
				
			}
			catch ( Exception e )
			{
				log.error( "Could not email this order request:\n"
					+ inOrder.getCustomer(), e );
				inOrder.getOrderState().setDescription("Order could not be sent " + e.getMessage());
				inOrder.getOrderState().setOk(false);
				throw new StoreException( e );
			}
		}

	/**
	 * @param inStore
	 * @param inOrder
	 */
//	protected void notifyStoreOwners(Store inStore, Order inOrder) throws Exception
//	{
//		String[] recipients = (String[])inStore.getNotifyAddresses().toArray( new String[inStore.getNotifyAddresses().size()]);
//		log.info("Sending notification to " + recipients.length + " people");
//		if ( recipients.length > 0)
//		{
//			
//			String content = "A new order has been received\n ";
//			if (inStore.getHostName()  != null)
//			{
//				content += "http://" + inStore.getHostName() + "/admin/orders/vieworders.html \n";
//				content += "Secure Link: https://" + inStore.getHostName() + "/admin/orders/vieworders.html";
//			}
//			postMail.postMail(recipients,"Order Notification",content, null,inStore.getFromAddress()
//				);
//		}
//
//	}

}