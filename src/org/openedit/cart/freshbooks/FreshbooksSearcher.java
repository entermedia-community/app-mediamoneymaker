package org.openedit.cart.freshbooks;

import java.util.Collection;

import org.openedit.Data;
import org.openedit.data.BaseSearcher;

import com.openedit.OpenEditException;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.users.User;

public class FreshbooksSearcher extends BaseSearcher{

	@Override
	public void reIndexAll() throws OpenEditException {
		// TODO Auto-generated method stub
		
	}

	
	public SearchQuery createSearchQuery() {
		return new SearchQuery();
	}

	@Override
	public HitTracker search(SearchQuery inQuery) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getIndexId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void clearIndex() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteAll(User inUser) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete(Data inData, User inUser) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void saveAllData(Collection<Data> inAll, User inUser) {
		// TODO Auto-generated method stub
		
	}

	
	
	
}
