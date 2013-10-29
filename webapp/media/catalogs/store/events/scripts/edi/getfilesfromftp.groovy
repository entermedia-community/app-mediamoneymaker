package edi;

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.ftp.FTPReply
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.PublishResult
import org.openedit.store.util.MediaUtilities;

import com.openedit.BaseWebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.GroovyScriptRunner
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.page.Page
import com.openedit.page.manage.PageManager
import com.openedit.users.User
import com.openedit.users.UserManager

public class GetFilesFromFTP extends EnterMediaObject {
	
	private String username;
	private String password;
	private String host;
	private String workingDirectory;
	private String localUploadFolder;
	private String localDownloadFolder;
	private String fileExtension;
	private Boolean overwrite;
	
	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setWorkingDirectory(String workingDirectory) {
		this.workingDirectory = workingDirectory;
	}
	
	public void setLocalUploadFolder(String localuploadfolder) {
		this.localUploadFolder = localuploadfolder;
	}

	public void setLocalDownloadFolder(String inLocalDownloadfolder) {
		this.localDownloadFolder = inLocalDownloadfolder;
	}
	
	public void setFileExtension(String fileExtension) {
		this.fileExtension = fileExtension;
	}

	public void setOverwrite(Boolean overwrite) {
		this.overwrite = overwrite;
	}
	
	public boolean setInfo( Data ftpInfo ) {
		setUsername(ftpInfo.username);
		setPassword(ftpInfo.password);
		setHost(ftpInfo.host_address);
		setWorkingDirectory(ftpInfo.download_folder);
		setLocalUploadFolder(ftpInfo.localuploadfolder);
		setLocalDownloadFolder(ftpInfo.localdownloadfolder);
		setFileExtension("xml");
		
	}

	public PublishResult getFiles() 
	{
		Log log = LogFactory.getLog(GroovyScriptRunner.class);
		MediaArchive archive = context.getPageValue("mediaarchive");
		String catalogid = getMediaArchive().getCatalogId();
		boolean production = Boolean.parseBoolean(context.findValue("productionmode"));
		
		UserManager userManager = archive.getModuleManager().getBean("userManager");
		PageManager pageManager = archive.getPageManager();

		PublishResult result = new PublishResult();
		result.setComplete(false);

		//Create the FTP Client	
		FTPClient ftp = new FTPClient();
		
		//Get Server Info
		String serverName = this.host;
	
		//Connect to the FTP Client
		ftp.connect(serverName);
	
		//check to see if connected
		int reply = ftp.getReplyCode();
		if(FTPReply.isPositiveCompletion(reply)) {
			log.info("FTP: Connected to ${serverName}");
		}
		else {
			result.setErrorMessage("Unable to connect to ${serverName}, error code: ${reply}")
			ftp.disconnect();
			return result;
		}
	
		String username = this.username;
		User user = userManager.getUser(username);
		if(user == null) {
			result.setErrorMessage("Unknown user, ${username}");
			ftp.disconnect();
			return result;
		}
		log.info("FTP: Attempting to connect as user: ${username}");
		
		String ftpPassword = userManager.decryptPassword(user);
		ftp.login(username, ftpPassword);
		reply = ftp.getReplyCode();
		if(FTPReply.isPositiveCompletion(reply)) {
			log.info("FTP: User(${username}) successfully logged in.");
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

		String url = this.workingDirectory;
		log.info("Working directory: " + url);
	
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
		
		String replyString = "";
		String strMsg = "<p>The following files have been uploaded.</p>";\
		String downloadFolder = "/WEB-INF/data/media/catalogs/store/" + this.localUploadFolder + "/";
		
		FTPFile[] files = ftp.listFiles();
		log.info("length: " + files.length);
		int ctr = 0;
		if (files.length > 0) {
			strMsg += "<ul>\n";
			for (int i = 0; i < files.length; i++) {
				//@todo: fix this - no file info is ever printed to strMsg
				FTPFile file = files[i];
				if (file.getName().endsWith(this.fileExtension) || 
					file.getName().endsWith(this.fileExtension.toUpperCase()))
				{
					log.info("found file: " + file.getName());
					Page downloadFile = pageManager.getPage(downloadFolder + file.getName());
					if (!downloadFile.exists()){
						ftp.retrieveFile(file.getName(), downloadFile.getContentItem().getOutputStream());
						reply = ftp.getReplyCode();
						if(FTPReply.isPositiveCompletion(reply)) {
							replyString = ftp.getReplyString();
							log.info("Reply: " + replyString);
						} else {
							log.info("Unable to retrieve file(${file.getName()}). Error code: ${reply}");
						}
					} else {
						//should we skip this?
						log.info("FTP: Download skipped: " + downloadFile.getName());
					}
				}
			}
			strMsg += "</ul>\n";
			if (ctr == 0) {
				log.info("There are no files to upload at this time.");
				log.info("Logging out.");
				ftp.disconnect();
				result.setCompleteMessage("<p>There are no files to upload at this time.</p>");
				result.setComplete(true);
				return result;
			}
		} else {
			log.info("There are no files to upload at this time.");
			log.info("Logging out.");
			ftp.disconnect();
			result.setCompleteMessage("<p>There are no files to upload at this time.</p>");
			result.setComplete(true);
			return result;
		}
		log.info("Logging out.");
		ftp.disconnect();
		
		result.setCompleteMessage(strMsg);
		result.setComplete(true);
		return result;
	}
	
	private Data getFtpInfo(catalogid, String ftpID) {
		BaseWebPageRequest inReq = context;
		MediaArchive archive = inReq.getPageValue("mediaarchive");
		SearcherManager manager = archive.getSearcherManager();
		Searcher ftpsearcher = manager.getSearcher(catalogid, "ftpinfo");
		Data ftpInfo = ftpsearcher.searchById(ftpID)
		return ftpInfo
	}

}
PublishResult result = new PublishResult();
result.setComplete(false);

logs = new ScriptLogger();
logs.startCapture();

try {
	
	MediaUtilities media = new MediaUtilities();
	media.setContext(context);
	
	GetFilesFromFTP ftpInvoice = new GetFilesFromFTP();
	ftpInvoice.setLog(logs);
	ftpInvoice.setContext(media.getContext());
	ftpInvoice.setModuleManager(moduleManager);
	ftpInvoice.setPageManager(pageManager);
	
	//Read the production value
	boolean production = Boolean.parseBoolean(context.findValue('productionmode'));
	
	
	String ftpID = "";
	String ftpIDProd = media.getContext().findValue('ftpidprod');
	String ftpIDTest = media.getContext().findValue('ftpidtest');
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
	
	//Get the FTP Info
	Data ftpInfo = ftpInvoice.getFtpInfo(media.getCatalogid(), ftpID);
	ftpInvoice.setInfo(ftpInfo);
	ftpInvoice.setFileExtension("xml");

	result = ftpInvoice.getFiles();
	if (result.isComplete()) {
		//print output of files
		String out = result.getCompleteMessage();
		log.info(out);
	} else {
		//ERROR: Throw exception
		log.info("ERROR: " + result.getErrorMessage());
	}
}
finally {
	logs.stopCapture();
}
