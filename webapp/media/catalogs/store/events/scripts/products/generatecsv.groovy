package products
import org.openedit.Data
import org.openedit.data.*
import org.openedit.entermedia.util.CSVWriter
import org.openedit.store.InventoryItem
import org.openedit.store.Product

import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.HitTracker
	

public class GenerateCsv extends EnterMediaObject {
	
	public void generate() {
		
		HitTracker hits = (HitTracker) context.getPageValue("hits");	
		if(hits == null){
			String sessionid = context.getRequestParameter("hitssessionid");
			hits = context.getSessionValue(sessionid);
		}
		log.info("hits: " +hits);
		SearcherManager searcherManager = context.getPageValue("searcherManager");
		String searchtype = context.findValue("searchtype");
		String catalogid = context.findValue("catalogid");
		String view = context.findValue("view");
		Searcher searcher = searcherManager.getSearcher(catalogid, searchtype);
		boolean friendly = Boolean.parseBoolean(context.getRequestParameter("friendly"));
		String[] detaillist = context.getRequestParameters("detail");
		Collection details = null;
		
		if(detaillist != null){
			log.info("Detail List was used - customizing export");
			details = new ArrayList();
			for(int i = 0;i<detaillist.length;i++){
				String detailid = detaillist[i];
				PropertyDetail detail = searcher.getDetail(detailid);
				if(detail != null){
					details.add(detail);
				}
			}
		} 
		else{
			details = searcher.getDetailsForView(view, context.getUser());
		}
				
		StringWriter output  = new StringWriter();
		CSVWriter writer  = new CSVWriter(output);
		int count = 0;
		String[] headers = new String[details.size()+1];
		for (Iterator iterator = details.iterator(); iterator.hasNext();)
		{
			PropertyDetail detail = (PropertyDetail) iterator.next();
			headers[count] = detail.getText();		
			count++;
		}
		headers[details.size()] = "Qty In Stock";
		writer.writeNext(headers);
		
		log.info("about to start: " + hits);
		Iterator i = hits.iterator();
//		if(hits.size() == 0){
//			i = hits.iterator();
//		} else{
//			i = hits.getSelectedHits().iterator();
//		}
		if (hits.size()>0) {
			for (Data hit in hits) {
				//Data hit =  iterator.next();
				if (hit != null) {
					Product tracker = searcher.searchById(hit.getId());
					if (tracker != null) {
				
						String[] nextrow = new String[details.size()+1];//make an extra spot for c
						int fieldcount = 0;
						for (Iterator detailiter = details.iterator(); detailiter.hasNext();)
						{
							PropertyDetail detail = (PropertyDetail) detailiter.next();
							if (detail != null) {
								String value = tracker.get(detail.getId());
								if (value != null && value.length()>0 && value.contains("|") && friendly) {
									String[] split = value.split("\\|");
									String newValue = "";
									for (int ctr=0; ctr<split.length; ctr++) {
										//log.info("Value: '" + split[ctr] + "'");
										Searcher se = searcherManager.getSearcher(detail.getCatalogId(), detail.getListId())
										Data remote  = se.searchById(split[ctr].trim());
										if(remote != null){
											newValue += remote.getName();
											if (ctr<(split.length-1)) {
												newValue += "|";
											}
										}
									}
									if (newValue.length() > 0) {
										value = newValue;
									} else {
										value = "[BLANK]";
									}
								} else {
									//do special logic here
									if(detail.isList() && friendly){
										if (value != null && value.length()>0 && value.contains(" ") && friendly) {
											String[] split = value.split(" ");
											String newValue = "";
											for (int ctr=0; ctr < split.length; ctr++) {
												//log.info("ListID: " + detail.getListId());
												//log.info("Value: '" + split[ctr] + "'");
												Searcher se = searcherManager.getSearcher(detail.getCatalogId(), detail.getListId())
												Data remote  = se.searchById(split[ctr].trim());
												if(remote != null){
													newValue += remote.getName();
												} else {
													newValue += "INVALID DATA(" + split[ctr].trim() + ")";
												}
												if (newValue.length()>0 && ctr<(split.length-1)) {
													newValue += "|";
												}
											}
											if (newValue.length() > 0) {
												value = newValue;
											} else {
												value = "[BLANK]";
											}
										} else {
											Data remote  = searcherManager.getData( detail.getListCatalogId(),detail.getListId(), value);
											if(remote != null){
												value= remote.getName();
											} else {
												value = "[BLANK]";
											}
										}
									}
								}
								if (value == null) {
									value = "[BLANK]";
								} else {
									if (value.length() == 0) {
										value = "[BLANK]";
									}
								}
								log.info("value: " + value);
								nextrow[fieldcount] = value;
									
							} else {
								nextrow[fieldcount] = "INVALID DATA(" + hit.getId() + ")";
							}
							fieldcount++;
						}
						InventoryItem item = tracker.getInventoryItem(0);
						Integer qtyInStock = item.getQuantityInStock();
						nextrow[details.size()] = qtyInStock.toString();
						writer.writeNext(nextrow);
					}
				}
			}
		}
		writer.close();
		
		String finalout = output.toString();
		context.putPageValue("export", finalout);
	}		
}
log = new ScriptLogger();
log.startCapture();

try {

	log.info("START - GenerateCSV");
	GenerateCsv generateCSV = new GenerateCsv();
	generateCSV.setLog(log);
	generateCSV.setContext(context);
	generateCSV.setModuleManager(moduleManager);
	generateCSV.setPageManager(pageManager);
	generateCSV.generate();
	log.info("FINISH - GenerateCSV");
}
finally {
	log.stopCapture();
}
