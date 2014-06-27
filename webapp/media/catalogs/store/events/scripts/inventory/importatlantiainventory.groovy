package inventory



import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.modules.update.Downloader
import com.openedit.page.Page
import com.openedit.users.User
import com.openedit.util.PathProcessor

import java.io.File;
import java.util.regex.Matcher
import java.util.regex.Pattern

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.util.CSVReader
import org.openedit.repository.ContentItem
import org.openedit.store.InventoryItem
import org.openedit.store.Product
import org.openedit.store.Store
import org.openedit.util.DateStorageUtil;

import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.ftp.FTPReply

import com.openedit.users.UserManager


public void init(){
//	log.info("---- Starting Import Atlantia Inventory -----");
	
	MediaArchive archive = context.getPageValue("mediaarchive");
	Store store = context.getPageValue("store");
	
	RemotePathProcessor processor = new RemotePathProcessor();
	processor.setLogger(log);
	processor.setArchive(archive);
	processor.setStore(store);
	processor.setIncomingDirectory("/WEB-INF/data/${archive.catalogId}/incoming/inventory/atlantia/");
	processor.setProcessedDirectory("/WEB-INF/data/${archive.catalogId}/processed/inventory/imports/atlantia/");
	processor.setUrl("http://matrixgroup.ca/downloads/Atlantia_Rogers/");//should make this configurable
	processor.addContains("a","href",".csv");//tag attribute contains some value (case insensitive)
	try{
		processor.process();
	}catch (Exception e){
		log.error(e);
	}
//	log.info("---- Finished Import Atlantia Inventory -----");
	
}

class RemotePathProcessor {
	MediaArchive archive;
	Store store;
	ScriptLogger log;
	String url;
	String processedDirectory;
	String incomingDirectory;
	HashMap<String,HashMap<String,String>> map;
	Downloader downloader;
	boolean productsUpdated = false;
	
	public void setIncomingDirectory(String inIncomingDir){
		incomingDirectory = inIncomingDir;
	}
	
	public String getIncomingDirectory(){
		return incomingDirectory;
	}
	
	public void setProcessedDirectory(String inExportDir){
		processedDirectory = inExportDir;	
	}
	
	public String getProcessedDirectory(){
		return processedDirectory;
	}
	
	public void setArchive(MediaArchive inArchive){
		archive = inArchive;
	}
	
	public MediaArchive getArchive(){
		return archive;
	}
	
	public Store getStore(){
		return store;
	}
	
	public void setStore(Store inStore){
		store = inStore;
	}
	
	public void setLogger(ScriptLogger inlog){
		log = inlog;
	}
	
	public ScriptLogger getLogger(){
		return log;
	}
	
	public void setUrl(String inUrl){
		url = inUrl;
	}
	
	public String getUrl(){
		return url;
	}
	
	public void setProductsUpdated(boolean inUpdate){
		productsUpdated = inUpdate;
	}
	
	public boolean isProductsUpdated(){
		return productsUpdated;
	}
	
	public void addContains(String inTag, String inAttribute, String inValue){
		HashMap<String,String> attributeMap = null;
		if (getMap().containsKey(inTag.toLowerCase())){
			attributeMap = getMap().get(inTag.toLowerCase());	
		} else {
			attributeMap = new HashMap<String,String>();
			getMap().put(inTag.toLowerCase(), attributeMap);
		}
		attributeMap.put(inAttribute.toLowerCase(), inValue);
	}
	
	public HashMap<String,HashMap<String,String>> getMap(){
		if (map == null){
			map = new HashMap<String,HashMap<String,String>>();
		}
		return map;
	}
	
	public HashMap<String,String> getAttributeMap(String inTag){
		return getMap().get(inTag.toLowerCase());
	}
	
	public Downloader getDownloader(){
		if (downloader == null){
			downloader = new Downloader();
		}
		return downloader;
	}
	
	public void process() throws Exception{
		//OLD way
//		String html = getDownloader().downloadToString(getUrl())
//		try{
//			ArrayList<String> links = getParsedContent(html,"body");
//			for(String link:links){
//				downloadContent(link);
//			}
//		}catch (Exception e){
//			log.error("Exception caught processing ${getUrl()}, ${e.getMessage()}");
//		}
//		setLastInventoryUpdate();
		
		//NEW way
		FTPClient ftp = new FTPClient();
		try{
			String serverName = "ftp.atlantia.ca";
			ftp.connect(serverName);
			int reply = ftp.getReplyCode();
			if(FTPReply.isPositiveCompletion(reply)) {
				log.info("FTP: Connected to ${serverName}");
			}
			else {
				log.info("Unable to connect to ${serverName}, error code: ${reply}")
				return;
			}
			String username = "atlantia";
			UserManager userManager = getArchive().getModuleManager().getBean("userManager");
			User user = userManager.getUser(username);
			if(user == null) {
				log.info("Unknown user, ${username}");
				return;
			}
			log.info("FTP: Attempting to connect as user: ${username}");
			String ftpPassword = userManager.decryptPassword(user);
			ftp.login(username, ftpPassword);
			reply = ftp.getReplyCode();
			if(FTPReply.isPositiveCompletion(reply)) {
				log.info("FTP: User(${username}) successfully logged in.");
			}
			else {
				log.info("Unable to login to ${serverName}, error code: ${reply}");
				return;
			}
		
			ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
			reply = ftp.getReplyCode();
			if(FTPReply.isPositiveCompletion(reply)) {
				log.info("FTP: Filetype set to BINARY.");
			}
			else {
				log.info("FTP: Unable to set Filetype set to BINARY, error code: ${reply}");
				return;
			}
	
			ftp.enterLocalPassiveMode();
			reply = ftp.getReplyCode();
			if(FTPReply.isPositiveCompletion(reply)) {
				log.info("FTP: Local Passive Mode is now PASSIVE.");
			}
			else {
				log.info("FTP: Unable to set Local Passive Mode to PASSIVE, error code: ${reply}");
				return;
			}
			//build incoming directory
			String incomingPath = getIncomingDirectory();// where file is downloaded to
			String processedPath = getProcessedDirectory();// where file is moved to after processing
			
			String replyString;
			StringBuilder buf = new StringBuilder();
			buf.append("<p>The following files have been downloaded from the Atlantia server.</p>\n");
			buf.append("<ul>");
			FTPFile[] files = ftp.listFiles();
			for (FTPFile file: files){
				if (file.getName().toLowerCase().endsWith(".csv")){
//					log.info("found file: " + file.getName());
					buf.append("<li>").append(file.getName()).append(" - ");
	//				Page downloadFile = pageManager.getPage(downloadFolder + file.getName());
					String fileName = file.getName();
					//incoming path
					String incomingpath = "${incomingPath}${fileName}";
					Page incomingPage = getArchive().getPageManager().getPage(incomingpath);
					//processed path
					String processedpath = "${processedPath}${fileName}";
					Page processedPage = getArchive().getPageManager().getPage(processedpath);
					//check if processed page exists
					if (!processedPage.exists()){
						buf.append("New - ");
						ftp.retrieveFile(file.getName(), incomingPage.getContentItem().getOutputStream());
						reply = ftp.getReplyCode();
						if(FTPReply.isPositiveCompletion(reply)) {
							replyString = ftp.getReplyString();
//							log.info("Reply: " + replyString);
							buf.append("<span style='color:green'>Success</span>");
							processPage(incomingPage,processedpath);
						} else {
							log.info("Unable to retrieve file(${file.getName()}). Error code: ${reply}");
							buf.append("<span color='color:red'>Fail (").append(reply).append(")</span>");
						}
					} else {
						//should we skip this?
//						log.info("FTP: Download skipped: " + downloadFile.getName());
						buf.append("Duplicate - <span style='color:blue'>Skipped</span>");
					}
					buf.append("</li>\n");
				}
			}
			buf.append("</ul>\n");
			log.info(buf.toString());
		}
		finally{
			try{
				ftp.disconnect();
				setLastInventoryUpdate();
			}catch (Exception e){
				log.error("Exception caught in finally clause, ${e.getMessage()}");
			}
		}
	}
	
	protected void downloadContent(String link) throws Exception {
		StringBuilder buf = new StringBuilder();
		if (link.toLowerCase().startsWith("http://") || link.toLowerCase().startsWith("https://")){
			buf.append(link);
		} else {
			if (getUrl().endsWith("/")){
				buf.append(getUrl()).append(link);
			} else {
				buf.append(getUrl()).append("/").append(link);
			}
		}
		//build paths
		String fileName = null;
		String processedPath = getProcessedDirectory();// where file is moved to after processing
		if (processedPath.endsWith("/")){
			fileName = buf.toString().substring(buf.toString().lastIndexOf("/") + 1);
		} else {
			fileName = buf.toString().substring(buf.toString().lastIndexOf("/"));
		}
		processedPath = "${processedPath}${fileName}";
		Page page = getArchive().getPageManager().getPage(processedPath);
		if (page.exists()){
			return;
		}
		log.info("Downloading ${page.getName()} from ${buf.toString()}...");
		//build incoming directory
		String incomingPath = getIncomingDirectory();// where file is downloaded to
		if (incomingPath.endsWith("/")){
			fileName = buf.toString().substring(buf.toString().lastIndexOf("/") + 1);
		} else {
			fileName = buf.toString().substring(buf.toString().lastIndexOf("/"));
		}
		incomingPath = "${incomingPath}${fileName}";
		page = getArchive().getPageManager().getPage(incomingPath);
		
		File fileOut = new File(page.getContentItem().getAbsolutePath());
		getDownloader().download(buf.toString(),fileOut);
		processPage(page,processedPath);
	}
	
	protected ArrayList<String> getParsedContent(String inHTML, String inStartTag) throws Exception{
		ArrayList<String> content = new ArrayList<String>();
		Pattern pattern = Pattern.compile("<(\"[^\"]*\"|'[^']*'|[^'\">])*>");
		String input = inHTML.replace("\n", "").trim();
		Matcher matcher = pattern.matcher(input);
		boolean foundStartTag = false;
		while (matcher.find()){
			String tag = matcher.group();
			if (!foundStartTag){
				if (!tag.toLowerCase().contains(inStartTag.toLowerCase())){
					continue;
				}
				foundStartTag = true;
				continue;
			} else {
				if (tag.toLowerCase().contains(inStartTag.toLowerCase())){
					break;
				}
			}
			if (tag.endsWith("/>") || tag.startsWith("</")){
				continue;
			}
			String trimmed = tag.replace("<", "").replace(">","");
			String[] tokens = trimmed.split("\\s");
			if (tokens.length == 0){
				continue;
			}
			HashMap<String,String> nodeMap = getAttributeMap(tokens[0]);
			if (nodeMap == null){
				continue;
			}
			Iterator<String> itr = nodeMap.keySet().iterator();
			while (itr.hasNext()){
				String key = itr.next();
				for(String token:tokens){
					if (token.toLowerCase().contains(key)){
						String value = nodeMap.get(key);
						if (token.toLowerCase().contains(value.toLowerCase())){
							String lctok = token.toLowerCase();
							//find out where to start and end
							int startIndex = lctok.indexOf(key) + key.length();
							int lastIndex = lctok.lastIndexOf(value.toLowerCase())+value.length();
							//substring original content
							String parsedValue = token.substring(startIndex,lastIndex);//restores case
							parsedValue = parsedValue.trim().replace("=","").replace("\"","")//clean up
							if (!content.contains(parsedValue)){
								content.add(parsedValue);
							}
						}
					}
				}
			}
		}
		return content;
	}
	
	public void processPage(Page inPage, String inProcessedPath){
		ArrayList<Product> productsToUpdate = new ArrayList<Product>();
		File input = new File(inPage.getContentItem().getAbsolutePath());
		Reader reader = new FileReader(input);
		try{
			CSVReader csvreader = new CSVReader(reader, (char)',', (char)'\"');
			List<?> lines = csvreader.readAll();
			Iterator<?> itr = lines.iterator();
			while(itr.hasNext()){
				String[] entries = itr.next();
				if (entries.length != 6){
					log.error("Error processing ${inPage.getName()}: CSV entries does not equal 6 (${entries.length}), abandoning parse");
					break;
				}
				String manufactSku = entries[0];
				String rogersSku = entries[1];
				String upc = entries[2];
				String quantity = entries[3];
				Product product = getUpdatedProduct(manufactSku,rogersSku,upc,quantity);
				if (product!=null){
					productsToUpdate.add(product);
				}
			}
		}catch (Exception e){
			log.error("Exception caught processing page ${inPage}, ${e.getMessage()}");
		}
		if (!productsToUpdate.isEmpty()){
			getStore().saveProducts(productsToUpdate);
			setProductsUpdated(true);
		}
		Page toPage = getArchive().getPageManager().getPage(inProcessedPath);
		getArchive().getPageManager().movePage(inPage, toPage);//we don't want to move, we want to copy
//		getArchive().getPageManager().copyPage(inPage, toPage);
	}
	
	public Product getUpdatedProduct(String inManufacturerSku, String inRogersSku, String inUpc, String inQuantity){
		MediaArchive archive = getArchive();
		Searcher searcher = archive.getSearcherManager().getSearcher(archive.getCatalogId(), "product");
		Data data = (Data) searcher.searchByField("manufacturersku", inManufacturerSku);
		if (data == null) data = (Data) searcher.searchByField("rogerssku",inRogersSku);
		if (data == null) data = (Data) searcher.searchByField("upc",inUpc);
		if (data != null){
			Product product = getStore().getProduct(data.id);
			InventoryItem item = product.getInventoryItemBySku(inManufacturerSku);
			if (item == null) item = product.getInventoryItemBySku(inRogersSku);// seems to be a problem with some of the data, check this
			if (item != null){
				int currentQuantity = item.getQuantityInStock();
				try{
					String decimal = "0.0";
					if (inQuantity.contains(".")){
						decimal = inQuantity.substring(inQuantity.indexOf("."));
						inQuantity = inQuantity.substring(0,inQuantity.indexOf("."));
					}
					int quantity = Integer.parseInt(inQuantity);
					double decimalValue = Double.parseDouble(decimal);
					if (decimalValue != 0){//LOG this condition
						log.info("Warning: inventory item ${inRogersSku} of product ${product.getId()} has a DECIMAL quantity, ${inQuantity}; updating Quantity In Stock to ${quantity}");
					}
					if (quantity < 0){
						item.setQuantityInStock(0);
					} else {
						item.setQuantityInStock(quantity);
					}
					product.setProperty("inventoryupdated", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));//also update inventory updated field of product
					return product;
				}catch (Exception e){
					log.error("Exception caught parsing inventory quantity update, ${inQuantity}, ${e.getMessage()}");
				}
			} else {
				log.info("Warning, unable to find inventory item, Rogers Sku = ${inRogersSku}, Manufact Sku = ${inManufacturerSku}");
			}
		} else {
			log.info("Warning, unable to find product, UPC = ${inUpc}, Rogers Sku = ${inRogersSku}, Manufact Sku = ${inManufacturerSku}");
		}
		return null;
	}
	
	public void setLastInventoryUpdate(){
		if (!isProductsUpdated()){
			log.info("No products were updated so Last Inventory Update will not be changed for Atlantia");
			return;
		}
		log.info("Updating Last Inventory Update for Atlantia");
		Searcher searcher = getArchive().getSearcherManager().getSearcher(getArchive().getCatalogId(), "distributor");
		Data data = searcher.searchById("atlantia");
		if (data == null){
			data = searcher.searchByField("name", "Atlantia");
		}
		if (data == null){
			data = searcher.searchByField("fullname", "Atlantia");
		}
		if (data!=null){
			data.setProperty("lastinventoryupdate", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
			searcher.saveData(data, null);
		} else {
			log.info("Warning: Unable to find Atlantia in distributor table");
		}
		
	}
}

init();