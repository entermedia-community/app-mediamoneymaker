package inventory;

import org.openedit.Data
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.util.ssh.SftpUtil
import org.openedit.store.InventoryItem
import org.openedit.store.Product
import org.openedit.store.search.ProductSearcher

import com.jcraft.jsch.ChannelSftp
import com.openedit.page.Page
import com.openedit.util.OutputFiller







public void sync(){
	MediaArchive archive = context.getPageValue("mediaarchive");
	SftpUtil util = new SftpUtil();
	util.setKeyFile("/home/ian/git/testbench/etc/discover/id_rsa");
	util.setUsername("AreaComm");
	util.setHost("sftp2.rogersdirect.ca");
	List childnames = util.getChildNames("/Area Communication/Inventory/");


	childnames.each{
		ChannelSftp.LsEntry entry = it;
		String name = entry.getFilename();
		Page page = archive.getPageManager().getPage("/WEB-INF/${archive.getCatalogId()}/incoming/inventory/discover/${name}");
		if(!page.exists()){
			InputStream input  = 	util.getFileFromRemote("/Area Communication/Inventory/${name}");
			OutputFiller filler = new OutputFiller();
			File file = new File(page.getContentItem().getAbsolutePath());
			filler.fill(input, file);
		}
		processPage(page);
	}
}


public void processPage(Page input){
	MediaArchive archive = context.getPageValue("mediaarchive");
	ProductSearcher s = archive.getSearcher("product");
	def inventory = new XmlSlurper().parse(input.getReader());
	inventory.Accessory.each{
		String sku = it.RogersSKU.text();
		String inventorylevel = it.Quantity.text();
		int level = Integer.parseInt(inventorylevel);
		Data  hit =  s.searchByField("rogerssku", sku);
		if(hit != null){
			Product p = s.searchById(hit.getId());
			InventoryItem i =  p.getInventoryItem(0);
			if(i == null){
				i = new InventoryItem();
				i.setSku(sku);
			}
			i.setQuantityInStock(level);
			s.saveData(p, null);
		}
	}
}


sync();