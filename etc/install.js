importPackage( Packages.com.openedit.util );
importPackage( Packages.java.util );
importPackage( Packages.java.lang );
importPackage( Packages.java.io );
importPackage( Packages.com.openedit.modules.update );


var war = "http://dev.entermediasoftware.com/jenkins/job/app-mediamoneymaker/lastSuccessfulBuild/artifact/deploy/ROOT.war";

var root = moduleManager.getBean("root").getAbsolutePath();
var web = root + "/WEB-INF";
var tmp = web + "/tmp";

log.add("1. GET THE LATEST WAR FILE");
var downloader = new Downloader();
downloader.download( war, tmp + "/ROOT.war");

log.add("2. UNZIP WAR FILE");
var unziper = new ZipUtil();
unziper.unzip(  tmp + "/ROOT.war",  tmp );

log.add("3. REPLACE LIBS");
var files = new FileUtils();
files.deleteMatch( web + "/lib/app-mediamoney*.jar");
files.deleteMatch( web + "/lib/poi*.jar");
files.deleteMatch( web + "/lib/jcsAnet*.jar");
files.deleteMatch( web + "/lib/openedit-money*.jar");
files.deleteMatch( web + "/lib/openecho*.jar");
files.deleteMatch( web + "/lib/money*.jar");
files.deleteMatch( web + "/lib/bsh*.jar");

files.copyFileByMatch( tmp + "/WEB-INF/lib/app-mediamoney*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/poi*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/jcsAnet*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/openecho*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/openedit-money*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/bsh*.jar", web + "/lib/");

log.add("5. UPGRADE CART FILES");
files.deleteAll( root + "/WEB-INF/base/store");
files.copyFiles( tmp + "/WEB-INF/base/store", root + "/WEB-INF/base/store");


files.deleteAll(tmp);

log.add("6. UGRADE COMPLETED");openedit.util );
importPackage( Packages.java.util );
importPackage( Packages.java.lang );
importPackage( Packages.com.openedit.modules.update );


