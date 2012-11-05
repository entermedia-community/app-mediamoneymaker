package orders;

import java.text.SimpleDateFormat

import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.PublishResult

import com.openedit.BaseWebPageRequest
import com.openedit.OpenEditException
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.Page
import com.openedit.page.manage.PageManager
import com.openedit.users.User
import com.openedit.users.UserManager

public void init() {

	BaseWebPageRequest inReq = context;

	MediaArchive archive = inReq.getPageValue("mediaarchive");
	SearcherManager manager = archive.getSearcherManager();
	boolean production = Boolean.parseBoolean(context.findValue('productionmode'));

	PageManager pageManager = archive.getPageManager();

	//Create Searcher Object
	Searcher productsearcher = manager.getSearcher(archive.getCatalogId(), "product");
	Searcher ordersearcher = manager.getSearcher(archive.getCatalogId(), "rogers_order");
	Searcher itemsearcher = manager.getSearcher(archive.getCatalogId(), "rogers_order_item");
	Searcher storesearcher = manager.getSearcher(archive.getCatalogId(), "store");
	Searcher distributorsearcher = manager.getSearcher(archive.getCatalogId(), "distributor");

	//Read orderid from the URL
	def String orderid = inReq.getRequestParameter("orderid");
	Data order = ordersearcher.searchById(orderid);

	//Get proper FTP info from Parameter
	String ftpID = "";
	String ftpIDProd = context.findValue('ftpidprod');
	String ftpIDTest = context.findValue('ftpidtest');
	if (production) {
		ftpID = ftpIDProd;
		if (ftpID == null) {
			ftpID = "104";
		} else if (ftpID.isEmpty()) {
			ftpID = "104";
		}
	} else {
		ftpID = ftpIDTest;
		if (ftpID == null) {
			ftpID = "103";
		} else if (ftpID.isEmpty()) {
			ftpID = "103";
		}
	}
	///////////////////////
	// FTPID OVERRIDE FOR TESTING
	///////////////////////
	ftpID = "104";
	///////////////////////
	
	HitTracker distributorList = distributorsearcher.getAllHits();
	List ftpTransferedFiles = new ArrayList();
	//Search through each distributor
	for (Iterator distribIterator = distributorList.iterator(); distribIterator.hasNext();)
	{

		//Get all of the hits and data for searching
		Data distributor = distribIterator.next();
		if (!Boolean.parseBoolean(distributor.useedi)) {
			continue;
		}

		SearchQuery distribQuery = itemsearcher.createSearchQuery();
		distribQuery.addExact("rogers_order",orderid);
		distribQuery.addExact("distributor", distributor.name);
		HitTracker numDistributors = itemsearcher.search(distribQuery);//Load all of the line items for store X

		if (numDistributors.size() > 0 )
		{
			// xml generation
			String fileName = "export-" + distributor.name.replace(" ", "-") + ".xml"
			Page page = pageManager.getPage("/WEB-INF/data/${catalogid}/orders/exports/${orderid}/${fileName}");

			String realpath = page.getContentItem().getAbsolutePath();
			File xmlFIle = new File(realpath);
			if (xmlFIle.exists()) {
				log.info("XML File exists: ${realpath}");
				//FTP the files to the server

				//Get the FTP Info
				Data ftpInfo = getFtpInfo(context, catalogid, ftpID);
				if (ftpInfo == null) {

					throw new OpenEditException("Cannot get FTP Info using ${ediID}");

				} else {

					PublishResult result = ftpFiles(manager, archive, page, ftpID);
					if (result.isComplete())
					{
						ftpTransferedFiles.add(fileName + " has been sent by FTP to " + ftpInfo.name);

					} else {

						log.info(result.getErrorMessage());

						throw new OpenEditException("FTP transfer did not complete. (${result.getErrorMessage()})");

					}
				}
			} else {

				throw new OpenEditException("CSV File does not exist: ${realpath}");

			}
		} else {

			log.info("Distributor (${distributor.name}) not found for this order (${orderid})");

		} // end if numDistributors
	} // end distribIterator LOOP
	context.putPageValue("filelist", ftpTransferedFiles);
	context.putPageValue("id", orderid);
}

private PublishResult ftpFiles(SearcherManager manager, MediaArchive archive, Page page, String ftpID) {

	UserManager userManager = archive.getModuleManager().getBean("userManager");
	PublishResult result = new PublishResult();

	//Get the FTP Info
	Data ftpInfo = getFtpInfo(context, catalogid, ftpID);

	//Get Server Info
	String serverName = ftpInfo.host_address;

	//Create the FTP Client
	FTPClient ftp = new FTPClient();

	//Connect to the FTP Client
	ftp.connect(serverName);

	//check to see if connected
	int reply = ftp.getReplyCode();
	if(FTPReply.isPositiveCompletion(reply))
	{
		log.info("FTP: Connected to ${serverName}");
	}
	else {
		result.setErrorMessage("Unable to connect to ${serverName}, error code: ${reply}")
		ftp.disconnect();
		return result;
	}

	String username = ftpInfo.username;
	User user = userManager.getUser(username);
	if(user == null)
	{
		result.setErrorMessage("Unknown user, ${username}");
		ftp.disconnect();
		return result;
	}
	log.info("FTP: Attempting to connect as user: ${username}");

	String ftpPassword = userManager.decryptPassword(user);
	log.info("FTP: ${ftpPassword}");

	ftp.login(username, ftpPassword);
	reply = ftp.getReplyCode();
	if(FTPReply.isPositiveCompletion(reply))
	{
		log.info("FTP: User(${username} successfully logged in.");
	}
	else {

		result.setErrorMessage("Unable to login to ${serverName}, error code: ${reply}");
		ftp.disconnect();
		return result;

	}

	ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
	//ftp.setFileTransferMode(FTPClient.BINARY_FILE_TYPE);
	reply = ftp.getReplyCode();
	if(FTPReply.isPositiveCompletion(reply)) {
		log.info("FTP: Filetype set to BINARY.");
	}
	else {
		result.setErrorMessage("FTP: Unable to set Filetype set to BINARY, error code: ${reply}");
		ftp.disconnect();
		return result;
	}

	ftp.enterLocalPassiveMode();
	reply = ftp.getReplyCode();
	if(FTPReply.isPositiveCompletion(reply)) {
		log.info("FTP: Local Passive Mode is now PASSIVE.");
	}
	else {
		result.setErrorMessage("FTP: Unable to set Local Passive Mode to PASSIVE, error code: ${reply}");
		ftp.disconnect();
		return result;
	}

	String url = ftpInfo.upload_folder;

	//change paths if necessary
	if(url != null && url.length() > 0) {
		ftp.changeWorkingDirectory(url);
		reply = ftp.getReplyCode();
		if(!FTPReply.isPositiveCompletion(reply)) {
			result.setErrorMessage("Unable to to cd to ${url}, error code: ${reply}");
			ftp.disconnect();
			return result;
		} else {
			String replyString = ftp.getReplyString();
			log.info("Reply: " + replyString);
		}
	}
	
	//Get the real path to the CSV file.
	String realpath = page.getContentItem().getAbsolutePath();
	FileInputStream fis = new FileInputStream(new File(realpath));

	String uploadFilename = page.getName();
	ftp.storeFile(uploadFilename, fis);

	reply = ftp.getReplyCode();
	if(FTPReply.isPositiveCompletion(reply))
	{
		log.info("FTP: Upload complete ${uploadFilename}.");
	}
	else {
		result.setErrorMessage("Unable to to send file, error code: ${reply}");
		ftp.disconnect();
		return result;
	}

	if(ftp.isConnected())
	{
		ftp.disconnect();
		log.info("FTP: Disconnected from ${serverName}.");
	}
	result.setComplete(true);
	log.info("Publishished ${uploadFilename} to FTP server ${serverName}");

	return result;
}

private Data getFtpInfo(context, catalogid, String ftpID) {
	BaseWebPageRequest inReq = context;
	MediaArchive archive = inReq.getPageValue("mediaarchive");
	SearcherManager manager = archive.getSearcherManager();
	Searcher ftpsearcher = manager.getSearcher(catalogid, "ftpinfo");
	Data ftpInfo = ftpsearcher.searchById(ftpID)
	return ftpInfo
}

private String generateEDIHeader ( boolean production, Data ftpInfo ){

	String output  = new String();
	output = ftpInfo.headericc;
	output += ftpInfo.headerfiletype;
	output += ftpInfo.headerdoctype.padRight(5);
	output += "ZZ:" + getSenderMailbox(production).padRight(18);
	output += "ZZ:" + getReceiverMailbox(ftpInfo, production).padRight(18);
	output += generateDate();
	output += generateTime();
	output += ftpInfo.headerversion;
	output += "".padRight(14);
	output += "\n";
	if (output.length() != 81 ) {
		throw new OpenEditException("EDI Header is not the correct length (${output.length().toString()})");
	}

	log.info("EDI Header: " + output + ":Length:" + output.length());

	return output;
}

private String getSenderMailbox( production ) {

	if(production) {
		return "AREACOMM";
	} else{
		return "AREACOMMT";
	}
}
private String getReceiverMailbox( Data ftpInfo, boolean production ) {

	if(production) {
		return ftpInfo.prodsendermailbox;
	} else{
		return ftpInfo.testsendermailbox;
	}
}

private String generateDate() {

	Date now = new Date();
	SimpleDateFormat tableFormat = new SimpleDateFormat("yyyyMMdd");
	String outDate = tableFormat.format(now);
	now = null;
	return outDate;

}
private String generateTime() {

	Date now = new Date();
	SimpleDateFormat tableFormat = new SimpleDateFormat("hhmmss");
	String outDate = tableFormat.format(now);
	now = null;
	return outDate;

}


init();
