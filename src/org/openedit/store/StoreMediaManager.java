package org.openedit.store;

import org.openedit.entermedia.MediaArchive;

import com.openedit.ModuleManager;
import com.openedit.page.manage.PageManager;

public class StoreMediaManager {

	
	protected MediaArchive fieldMediaArchive;
	
	protected PageManager fieldPageManager;
	protected ModuleManager fieldModuleManager;
	protected Store fieldStore;
	
	
	public ModuleManager getModuleManager() {
		return fieldModuleManager;
	}
	public void setModuleManager(ModuleManager inModuleManager) {
		fieldModuleManager = inModuleManager;
	}
	public MediaArchive getMediaArchive() {
		if (fieldMediaArchive == null) {
			String mediacatid = getStore().getCatalogId();
			fieldMediaArchive = (MediaArchive) getModuleManager().getBean(mediacatid, "mediaArchive");
		}
		return fieldMediaArchive;
	}

	
	public void setMediaArchive(MediaArchive inMediaArchive) {
		fieldMediaArchive = inMediaArchive;
	}
	public PageManager getPageManager() {
		return fieldPageManager;
	}
	public void setPageManager(PageManager inPageManager) {
		fieldPageManager = inPageManager;
	}
	public Store getStore() {
		return fieldStore;
	}
	public void setStore(Store inStore) {
		fieldStore = inStore;
	}
	
	
	
}
