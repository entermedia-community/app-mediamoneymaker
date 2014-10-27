package inventory

import java.util.regex.Matcher
import java.util.regex.Pattern

import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.ftp.FTPReply
import org.entermedia.email.PostMail
import org.entermedia.email.TemplateWebEmail
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive
import org.openedit.store.InventoryItem
import org.openedit.store.Product
import org.openedit.store.Store
import org.openedit.util.DateStorageUtil

import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.modules.update.Downloader
import com.openedit.page.Page
import com.openedit.users.User
import com.openedit.users.UserManager


public void init(){
	log.info("---- Starting Import Atlantia Inventory -----");
	
	MediaArchive archive = context.getPageValue("mediaarchive");
	Store store = context.getPageValue("store");
	
	RemotePathProcessor processor = new RemotePathProcessor();
	processor.setContext(context);
	processor.setContentPage(content);
	processor.setLogger(log);
	processor.setArchive(archive);
	processor.setStore(store);
	processor.setIncomingDirectory("/WEB-INF/data/${archive.catalogId}/incoming/inventory/atlantia/");
	processor.setProcessedDirectory("/WEB-INF/data/${archive.catalogId}/processed/inventory/imports/atlantia/");
	processor.setUrl("http://matrixgroup.ca/downloads/Atlantia_Rogers/");//should make this configurable
	processor.addContains("a","href",".csv");//tag attribute contains some value (case insensitive)
	processor.addEmailNotification("erin@atlantia.ca");
	processor.addEmailNotification("megan@atlantia.ca");
	processor.addEmailNotification("dsf@area.ca");
	processor.addEmailNotification("kk@area.ca");
//	processor.addEmailNotification("shawn@ijsolutions.ca");
	try{
		processor.process();
	}catch (Exception e){
		log.error(e);
	}
	log.info("---- Finished Import Atlantia Inventory -----");
	
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
	List<String> emails;
	WebPageRequest req;
	Page contentpage;
	int rowsProcessed = 0;
	
	public void resetRowsProcessed(){
		rowsProcessed = 0;
	}
	
	public void incrementRowsProcessed(){
		rowsProcessed ++;
	}
	
	public int getRowsProcessed(){
		return rowsProcessed;
	}
	
	public void setContext(WebPageRequest context){
		req = context;
	}
	
	public void setContentPage(Page inContent){
		contentpage = inContent;
	}
	
	public List<String> getEmails(){
		if (emails == null){
			emails = new ArrayList<String>();
		}
		return emails;
	}
	
	public void addEmailNotification(String email){
		getEmails().add(email);
	}
	
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
//							log.info("Unable to retrieve file(${file.getName()}). Error code: ${reply}");
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
			}catch (Exception e){
				log.error("Exception caught in finally clause, ${e.getMessage()}");
			}
			setLastInventoryUpdate();
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
		ArrayList<String> badRows = new ArrayList<String>();
		ArrayList<String> badProducts = new ArrayList<String>();
		File input = new File(inPage.getContentItem().getAbsolutePath());
		//CSVReader does not work for these files
//		Reader reader = new FileReader(input);
//		CSVReader csvreader = new CSVReader(new java.io.InputStreamReader(inPage.getContentItem().getInputStream()));
//		try{
//			List<?> lines = csvreader.readAll();
//			Iterator<?> itr = lines.iterator();
//			resetRowsProcessed();
//			while(itr.hasNext()){
//				String[] entries = itr.next();
//				incrementRowsProcessed();
//				if (entries.length != 6){
//					badRows.add("Number of entries is not 6 (actual=${entries.length}), values=${entries}");
//					log.error("Error processing ${inPage.getName()}: CSV entries does not equal 6 (${entries.length}), abandoning parse");
//					continue;
//				}
//				String manufactSku = entries[0];
//				String rogersSku = entries[1];
//				String upc = entries[2];
//				String quantity = entries[3];
//				Product product = getUpdatedProduct(manufactSku,rogersSku,upc,quantity);
//				if (product!=null){
//					productsToUpdate.add(product);
//				} else {
//					badRows.add("Cannot find product, manufacturer sku: $manufactSku, rogers sku: $rogersSku, UPC: $upc");
//				}
//			}
		//parse file directly instead
		BufferedReader bufferedreader = null;
		try{
			bufferedreader = new BufferedReader(new InputStreamReader(new FileInputStream(input)));
			resetRowsProcessed();
			while (bufferedreader.ready()){
				String line= bufferedreader.readLine();
				if (line.contains(",")){
					incrementRowsProcessed();
					String [] entries = line.split(",");
					if (entries.length != 6){
						badRows.add("Number of entries is not 6 (actual=${entries.length}), values=${entries}");
						log.error("Error processing ${inPage.getName()}: CSV entries does not equal 6 (${entries.length}), skipping");
						continue;
					}
					String manufactSku = entries[0].trim();
					String rogersSku = entries[1].trim();
					String upc = entries[2].trim();
					String quantity = entries[3].trim();
					
					StringBuilder output = new StringBuilder();
					Product product = getUpdatedProduct(manufactSku,rogersSku,upc,quantity,output);
					if (product!=null){
						productsToUpdate.add(product);
					} else {
						//badProducts.add("manufacturer sku: $manufactSku, rogers sku: $rogersSku, UPC: $upc, Quantity: $quantity");
						badProducts.add(output.toString());
					}
				}
			}
		}catch (Exception e){
			log.error("Exception caught processing page ${inPage}, ${e.getMessage()}",e);
		}
		finally{
			try{
				bufferedreader.close();
			}catch(Exception e){}
		}
		if (!productsToUpdate.isEmpty()){
			getStore().saveProducts(productsToUpdate);
			dispatchEmailNotifications(productsToUpdate,badRows,badProducts);
			setProductsUpdated(true);
		}
		Page toPage = getArchive().getPageManager().getPage(inProcessedPath);
		getArchive().getPageManager().movePage(inPage, toPage);
	}
	
	public Product getUpdatedProduct(String inManufacturerSku, String inRogersSku, String inUpc, String inQuantity, StringBuilder inBuf){
		MediaArchive archive = getArchive();
		Searcher searcher = archive.getSearcherManager().getSearcher(archive.getCatalogId(), "product");
		
		//copy search terms
		String manufactSku = inManufacturerSku;
		String rogersSku = inRogersSku;
		String upc = inUpc;
		//clean if required
		if (manufactSku.contains("/")) manufactSku = manufactSku.replace("/", "\\/");
		if (rogersSku.contains("/")) rogersSku = rogersSku.replace("/", "\\/");
		if (upc.contains("/")) upc = upc.replace("/", "\\/");
		
		Data data = null;
		//search each term
		if (manufactSku!="") data = searcher.searchByField("manufacturersku", manufactSku);
		if (data == null && rogersSku!="") data = searcher.searchByField("rogerssku",rogersSku);
		if (data == null && upc!="") data = searcher.searchByField("upc",upc);
		if (data != null){
			Product product = getStore().getProduct(data.id);
			if (canUpdateInventory(product) == false){
				inBuf.append("<span style='color:red'>Unable to update inventory:</span> NHL product updates are disabled for $product (${product.id})");
				return null;
			}
			InventoryItem item = product.getInventoryItemBySku(inManufacturerSku);
			if (item == null){	
				item = product.getInventoryItemBySku(inRogersSku);// seems to be a problem with some of the data, check this
			}
			if (item == null){
				//can't find by sku but should only ever be one item in the list inventory items
				if (product.getInventoryItem(0) != null){
					item = product.getInventoryItem(0);
				}
			}
			//at this point, call it a day
			if (item == null){
				inBuf.append("<span style='color:red'>Unable to find any inventory items:</span> looking for Manufacturer Sku = ${inManufacturerSku} or Rogers Sku = ${inRogersSku}");
			}
			else 
			{
//				int currentQuantity = item.getQuantityInStock();
//				try{
//					String decimal = "0.0";
//					if (inQuantity.contains(".")){
//						decimal = inQuantity.substring(inQuantity.indexOf("."));
//						inQuantity = inQuantity.substring(0,inQuantity.indexOf("."));
//					}
//					int quantity = Integer.parseInt(inQuantity);
//					double decimalValue = Double.parseDouble(decimal);
//					if (decimalValue != 0){//LOG this condition
//						log.info("Warning: inventory item ${inRogersSku} of product ${product.getId()} has a DECIMAL quantity, ${inQuantity}; updating Quantity In Stock to ${quantity}");
//					}
//					if (quantity < 0){
//						item.setQuantityInStock(0);
//					} else {
//						item.setQuantityInStock(quantity);
//					}
//					product.setProperty("inventoryupdated", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));//also update inventory updated field of product
//					return product;
//				}catch (Exception e){
//					log.error("Exception caught parsing inventory quantity update [${inQuantity}] ${e.getMessage()}");
//					return null;
//				}
				int newquantity = toInt(inQuantity,0,0);
				item.setQuantityInStock(newquantity);
				product.setProperty("inventoryupdated", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
				return product;
			}
		} else {
			inBuf.append("<span style='color:red'>Unable to find product:</span> looking for Manufacturer Sku = ${inManufacturerSku}, Rogers Sku = ${inRogersSku}, UPC = ${inUpc}");
		}
		return null;
	}
	
	public boolean canUpdateInventory(Product inProduct){
		//check if NHL products should be updated
		if(contentpage && contentpage.isPropertyTrue("disablenhlupdates")){
			boolean isNHL = Boolean.parseBoolean(inProduct.get("nhl"));
			if (isNHL){
				log.info("### cannot update inventory for $inProduct (${inProduct.getId()}), disabled for NHL Products");
				return false;
			}
		}
		return true;
	}
	
	public int toInt(String inValue, int inDefault, int inMinimum){
		int value = inDefault;
		if (inValue.contains(".")){
			try{
				double d = java.lang.Double.parseDouble(inValue);
				value = new Double(d).toInteger().intValue();
			}catch (Exception e){};
		} else {
			try{
				value = java.lang.Integer.parseInt(inValue);
			}catch(Exception e){}
		}
		if (value < inMinimum){
			value = inMinimum;
		}
		return value;
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
	
	public void dispatchEmailNotifications(List updatedProducts, List badRows, List badProducts){
		if (getEmails().isEmpty() || updatedProducts.isEmpty()){
			return;
		}
		//add info to request
		req.putPageValue("distributor", "Atlantia");
		req.putPageValue("totalrows", getRowsProcessed()+"");
		req.putPageValue("badrows", badRows);
		req.putPageValue("goodproductlist", updatedProducts);
		req.putPageValue("badproductlist", badProducts);
		//Distributor: $distributor
		//Total Rows Processed: $totalrows
		//Total Products Properly Processed: $goodproductlist.size()
		String templatePage = "/ecommerce/views/modules/product/workflow/inventory-notification.html";
		sendEmail(getEmails(), templatePage);
	}
	
	protected void sendEmail(List emaillist, String templatePage){
		StringBuilder buf = new StringBuilder();
		Iterator itr = emaillist.iterator();
		while(itr.hasNext()){
			buf.append(itr.next());
			if (itr.hasNext()) buf.append(",");
		}
		Page template = getArchive().getPageManager().getPage(templatePage);
		WebPageRequest newcontext = req.copy(template);
		TemplateWebEmail mailer = getMail();
		mailer.setFrom("info@wirelessarea.ca");
		mailer.loadSettings(newcontext);
		mailer.setMailTemplatePath(templatePage);
		mailer.setRecipientsFromCommas(buf.toString());
		mailer.setSubject("Support Ticket Update");
		mailer.send();
		log.info("Emails sent to ${buf.toString()}");
	}
	
	protected TemplateWebEmail getMail() {
		PostMail mail = (PostMail)getArchive().getModuleManager().getBean( "postMail");
		return mail.getTemplateWebEmail();
	}
}

init();