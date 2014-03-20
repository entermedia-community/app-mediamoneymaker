package products;

import java.text.SimpleDateFormat
import org.entermedia.email.PostMail
import org.entermedia.email.TemplateWebEmail
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.util.CSVReader
import org.openedit.store.InventoryItem
import org.openedit.store.Product
import org.openedit.store.Store
import org.openedit.util.DateStorageUtil
import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker
import com.openedit.page.Page
import com.openedit.util.FileUtils

import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.Page
import com.openedit.users.UserManager

import org.entermedia.email.PostMail
import org.entermedia.email.TemplateWebEmail
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.search.AssetSearcher
import org.openedit.util.DateStorageUtil

import com.openedit.OpenEditException;

import java.util.*;

import org.openedit.Data
import org.openedit.data.Searcher

import com.openedit.users.Group
import com.openedit.users.User
import com.openedit.page.manage.*;

public void init(){
	MediaArchive archive = mediaarchive;
	Store store = context.getPageValue("store");
	SearcherManager manager = archive.getSearcherManager();
	Searcher productsearcher = store.getProductSearcher();
	UserManager usermanager = archive.getModuleManager().getBean("userManager");
	Group users = usermanager.getGroup("users");
	if (users!=null){
		HitTracker hits = productsearcher.getAllHits();
		hits.each{
			Data data = it;
			Product product = store.getProduct(data.id);
			if (product != null){
				String group = product.get("group");
				if (group == null || (group!=null && !group.contains("fido") && !group.contains("rogers") && !group.contains("users")) ){
					String groups = (group == null || group.isEmpty() ? "" : (group+" | "))  + "users";
					log.info("Adding $groups to $product");
					product.setProperty("group", groups);
					productsearcher.saveData(product, null);
				}
			}
		}
	}
}

init();