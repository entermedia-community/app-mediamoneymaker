
/*
 * Created on Aug 24, 2005
 */

import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.store.Store
import org.openedit.util.DateStorageUtil

import com.openedit.WebPageRequest
import com.openedit.users.User
import com.openedit.users.UserManager


public void handleSubmission(){
	
	WebPageRequest inReq = context;
	MediaArchive archive = context.getPageValue("mediaarchive");
 	UserManager manager = archive.getModuleManager().getBean("userManager");
	User user = inReq.getUser();
	user.setProperty("acceptterms", "true");
	user.setProperty("accepttermsdate", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
	Searcher usersearcher = archive.getSearcherManager().getSearcher("system", "user");
	usersearcher.saveData(user, null);
//	inReq.forward("/index.html");
	
	
}

handleSubmission();



		