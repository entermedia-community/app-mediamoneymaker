package org.openedit.store.modules;


import org.openedit.WebPageRequest;
import org.openedit.store.Product;
import org.openedit.store.Store;
import org.openedit.store.StoreException;
import org.openedit.users.User;

public class VotingModule extends BaseStoreModule  {

	public void voteForProduct(WebPageRequest inReq) throws Exception {
		Store store = getStore(inReq);
	
	    boolean changes = Boolean.parseBoolean(inReq.findValue("allowvotechange"));
		User user = inReq.getUser();
		String oldvote = (String) user.getProperty("votedfor");
		String id = inReq.getRequestParameter("productid");
		processVote(inReq, store, user, id, changes);
		
	
		}
	
	public void processVote(WebPageRequest inReq, Store store, User user, String id, boolean allowVoteChange) throws StoreException {
		
		String oldvote = (String)user.getProperty("votedfor");
		if(id.equals(oldvote)){
			return;
		}
		if(oldvote != null && !allowVoteChange){
			return;
		}
		if (id != null) {
			Product product = store.getProduct(id);
			String vote = product.getProperty("votecount");
			int val = 1;
			if (vote != null && vote.length() > 0) {
				val = Integer.parseInt(vote);
				val++;
			}
			product.putAttribute("votecount", String.valueOf(val));
			store.saveProduct(product, user);
			
			if (oldvote != null) {
				Product old = store.getProduct(oldvote);
				if (old != null) {
					String oldcount = old.getProperty("votecount");
					if (oldcount != null) {
						val = Integer.parseInt(oldcount);
						val--;
					}
					if(val == -1){
						val = 0;
					}
					old.putAttribute("votecount", String.valueOf(val));
					store.saveProduct(old);
				}
			}
			user.put("votedfor", id);
			getUserManager(inReq).saveUser(user);
		
	}}
}
