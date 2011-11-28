package com.openedit.store;

import org.openedit.store.Product;
import org.openedit.store.Store;
import org.openedit.store.StoreTestCase;
import org.openedit.store.edit.StoreEditor;
import org.openedit.store.modules.VotingModule;

import com.openedit.WebPageRequest;
import com.openedit.users.User;

public class VotingTest extends StoreTestCase{
	
	
	public VotingTest(String arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	public void testVote() throws Exception
	{
		VotingModule mod = (VotingModule) getBean("VotingModule");
	  
		StoreEditor editor = getStoreEditor();
		Store store = getStore();
		WebPageRequest req = getFixture().createPageRequest("/testcatalog/admin/jobs/index.html");	
		User user = req.getUser();
		user.remove("votedfor");		
		//clear out old votes
		Product product = getStore().getProduct("2");
		product.removeProperty("votecount");
		editor.saveProduct(product);
		
	    product = getStore().getProduct("1");
		product.removeProperty("votecount");
		editor.saveProduct(product);
	
		//test voting here
		//vote for 1
		
		mod.processVote(store,user, "1", false);
		product = getStore().getProduct("1");
		assertEquals("1", product.getProperty("votecount"));
		assertEquals("1", user.getProperty("votedfor"));
		//now vote for 2 - vote changes not allowed		
		
		mod.processVote(store,user, "2", false);
		product = getStore().getProduct("1");
		assertEquals("1", product.getProperty("votecount"));
		assertEquals("1", user.getProperty("votedfor"));
		
		//Vote for 2, with vote changes allowed
		mod.processVote(store,user, "2", true);
		product = getStore().getProduct("1");
		assertEquals("0", product.getProperty("votecount"));
		product = getStore().getProduct("2");
		assertEquals("2", user.getProperty("votedfor"));
		
		// Vote for 2 again - make sure it doesn't get recounted
		mod.processVote(store,user, "2", true);
		product = getStore().getProduct("1");
		assertEquals("0", product.getProperty("votecount"));
		product = getStore().getProduct("2");
		assertEquals("2", user.getProperty("votedfor"));		
	}
	
	public StoreEditor getStoreEditor() throws Exception
	{
		if ( fieldStoreEditor == null)
		{
			fieldStoreEditor = new StoreEditor();
			fieldStoreEditor.setStore(getStore());
			fieldStoreEditor.setPageManager(getFixture().getPageManager());
		}
		return fieldStoreEditor;
	}
	protected StoreEditor fieldStoreEditor;
}
