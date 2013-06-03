package displaymedia

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.images.BannerModule;
import org.openedit.util.DateStorageUtil;

import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker

public class GetBannerInfo extends EnterMediaObject {

	private ArrayList<Data> bannerList;
	
	private addToBannerList(Data inData) {
		if (bannerList == null) {
			bannerList = new ArrayList<Data>();
		}
		bannerList.add(inData);
	}
	
	private ArrayList getBannerList() {
		if (bannerList == null) {
			bannerList = new ArrayList<Data>();
		}
		return bannerList;
	}
	
	public void getInfo() {
		
		WebPageRequest inReq = context;
		MediaArchive archive = context.getPageValue("mediaarchive");

		SearcherManager manager = archive.getSearcherManager();
		String catalogid = archive.getCatalogId();
		
		Searcher bannerSearcher = manager.getSearcher(catalogid, "banner");
		HitTracker bannerlist = bannerSearcher.fieldSearch("active", "true", "random");
		if (bannerlist != null && bannerlist.size() > 0) {
			for (Iterator bannerIterator = bannerlist.iterator(); bannerIterator.hasNext();) {
				Data currentBanner = bannerIterator.next();
				Date today = new Date();
				Date startDate = DateStorageUtil.getStorageUtil().parseFromStorage(currentBanner.get("startdate"));
				if (startDate != null) {
					Date endDate = DateStorageUtil.getStorageUtil().parseFromStorage(currentBanner.get("enddate"));
					if (endDate != null) {
						if ((today.compareTo(startDate) >= 0) && (today.compareTo(endDate) <= 0)) {
							addToBannerList(currentBanner);
						}
					}
				}
			}
		}
		inReq.putPageValue("bannerlist", getBannerList());
	}
}
logs = new ScriptLogger();
logs.startCapture();

try {
	GetBannerInfo getBannerInfo = new GetBannerInfo();
	getBannerInfo.setLog(logs);
	getBannerInfo.setContext(context);
	getBannerInfo.setModuleManager(moduleManager);
	getBannerInfo.setPageManager(pageManager);

	getBannerInfo.getInfo();
}
finally {
	logs.stopCapture();
}
