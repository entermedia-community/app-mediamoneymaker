importPackage( Packages.org.openedit.util );
importPackage( Packages.java.util );
importPackage( Packages.java.lang );
importPackage( Packages.java.io );
importPackage( Packages.org.entermediadb.modules.update );




var war = "http://dev.entermediasoftware.com/jenkins/job/em9_app-mediamoneymaker/lastSuccessfulBuild/artifact/deploy/ROOT.war";

var root = moduleManager.getBean("root").getAbsolutePath();
var web = root + "/WEB-INF";
var tmp = web + "/tmp";

log.info("1. GET THE LATEST WAR FILE");
var downloader = new Downloader();
downloader.download( war, tmp + "/ROOT.war");

log.info("2. UNZIP WAR FILE");
var unziper = new ZipUtil();
unziper.unzip(  tmp + "/ROOT.war",  tmp );

log.info("3. REPLACE LIBS");
var files = new FileUtils();
files.deleteMatch( web + "/lib/app-mediamoney*.jar");
files.deleteMatch( web + "/lib/poi*.jar");
files.deleteMatch( web + "/lib/xmlbeans*.jar");
files.deleteMatch( web + "/lib/jcsAnet*.jar");
files.deleteMatch( web + "/lib/openedit-money*.jar");
files.deleteMatch( web + "/lib/openecho*.jar");
files.deleteMatch( web + "/lib/money*.jar");
files.deleteMatch( web + "/lib/bsh*.jar");
files.deleteMatch( web + "/lib/gson*.jar");
files.deleteMatch( web + "/lib/stripe*.jar");
files.deleteMatch( web + "/lib/store-reports*.jar");
files.deleteMatch( web + "/lib/paypal*.jar");


files.copyFileByMatch( tmp + "/WEB-INF/lib/app-mediamoney*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/poi*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/xmlbeans*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/jcsAnet*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/openecho*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/openedit-money*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/bsh*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/gson*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/stripe*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/store-reports*.jar", web + "/lib/");
files.copyFileByMatch( tmp + "/WEB-INF/lib/paypal*.jar", web + "/lib/");



log.info("5. UPGRADE CART FILES");
files.deleteAll( root + "/WEB-INF/base/store");
files.copyFiles( tmp + "/WEB-INF/base/store", root + "/WEB-INF/base/store");


log.info("5. UPGRADE CART FILES");





files.deleteAll(tmp);

log.info("6. UGRADE COMPLETED");
importPackage( Packages.java.util );
importPackage( Packages.java.lang );
importPackage( Packages.com.openedit.modules.update );


