import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.store.InventoryItem
import org.openedit.store.Product
import org.openedit.util.DateStorageUtil

import com.openedit.hittracker.HitTracker


public void init()
{
	log.info("Checking for expired sales of inventory items");
	int count = 0;
	Date today = new Date();
	MediaArchive archive = context.getPageValue("mediaarchive");
	Searcher productsearcher = archive.getSearcher("product");
	HitTracker hits = productsearcher.getAllHits();
	hits.each{
		Data data = (Data) it;
		Product product = (Product) productsearcher.searchById(data.getId());
		Iterator<?> itr = product.getInventoryItems().iterator();
		while (itr.hasNext())
		{
			InventoryItem item = (InventoryItem) itr.next();
			String enddate = item.get("saleenddate");
			if (enddate!=null && !enddate.isEmpty())
			{
				Date saleDate = DateStorageUtil.getStorageUtil().parseFromStorage(enddate);
				if (saleDate!=null){
					Date dayAfterSale = saleDate.plus(1);
					if (today.after(dayAfterSale)){
						log.info("Sale expired for ${product}, sale end date ${saleDate}, removing sale price");
						item.getPriceSupport().removeSalePrice();
						item.getProperties().remove("saleenddate");
						productsearcher.saveData(data, null);
						count++;
					} else {
//						log.info("${dayAfterSale} is after or equal to ${today}");
					}
				} else {
					log.info("Error parsing sale end date for ${product}, ${enddate}");
				}
			}
		}
	}
	log.info("Finishing checking for expired sales of inventory items, updated ${count} items");
}

init();