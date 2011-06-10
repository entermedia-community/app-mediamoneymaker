package org.openedit.store.search;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.openedit.data.PropertyDetails;
import org.openedit.repository.ContentItem;
import org.openedit.store.Product;
import org.openedit.store.ProductArchive;

import com.openedit.OpenEditException;
import com.openedit.page.PageSettings;
import com.openedit.users.User;

public class ProductLuceneIndexAll extends ProductProcessor
{
	static final Log log = LogFactory.getLog(ProductLuceneIndexAll.class);
	protected IndexWriter fieldWriter;

	protected ProductArchive fieldProductArchive;
	protected ProductLuceneIndexer fieldIndexer;
	protected Boolean fieldIndexFolders;
	
	private boolean doesIndexFolders()
	{
		if (fieldIndexFolders == null)
		{
			PageSettings settings = getPageManager().getPageSettingsManager().getPageSettings("/" + getIndexer().getStore().getCatalogId() + "/products/");
			String val = settings.getPropertyValue("indexfolders", null);
			if (val != null && Boolean.valueOf(val).booleanValue())
			{
				fieldIndexFolders = Boolean.TRUE;
			}
			else
			{
				fieldIndexFolders = Boolean.FALSE;
			}
		}
		return fieldIndexFolders.booleanValue();
	}

	public ProductLuceneIndexer getIndexer()
	{
		return fieldIndexer;
	}

	public void setIndexer(ProductLuceneIndexer inIndexer)
	{
		fieldIndexer = inIndexer;
	}

	public ProductArchive getProductArchive()
	{
		return fieldProductArchive;
	}

	public void setProductArchive(ProductArchive inProductArchive)
	{
		fieldProductArchive = inProductArchive;
	}

	public PropertyDetails getDetails()
	{
		return getProductArchive().getPropertyDetails();
	}

	public Analyzer getAnalyzer()
	{
		return getIndexer().getAnalyzer();
	}

	public IndexWriter getWriter()
	{
		return fieldWriter;
	}

	public void setWriter(IndexWriter inWriter)
	{
		fieldWriter = inWriter;
	}
	
	public void processSourcePath(String inSourcePath)
	{
		IndexWriter writer = getWriter();
		long ramSizeInBytes = writer.ramSizeInBytes();
		if (ramSizeInBytes > 1024 * 350) // flush every
		// 35 megs
		{
			log.info("Flush writer in reindex mem: " + ramSizeInBytes
					+ " finished " + getExecCount() + " records ");
			try
			{
				writer.flush();
			} catch (Exception e)
			{
				throw new OpenEditException(e);
			}
			getProductArchive().clearProducts();
		}

		Product product = getProductArchive().getProductBySourcePath(inSourcePath);
		
//		if ( product == null && doesIndexFolders() && inSourcePath.endsWith("/"))
//		{
//			//We may want to index a folder even if it has no product already
//			product = getProductArchive().getStore().createProduct(inSourcePath);
//			product.setProperty("datatype", "folder");
////			}
////			else
////			{
////				doc.add(new Field("datatype", "product", Field.Store.YES, Field.Index.NO_NORMS));
////			}
////			
//
//			
//		}
		
		if (product != null && product.isAvailable())
		{

			Document doc = getIndexer().createProductDoc(product, getDetails());
			String id = product.getId().toLowerCase();
			getIndexer().writeDoc(writer, id, doc, true);
			// remove it from mem
			
			incrementCount();
			
			log.info("count: " + getExecCount());
		}
		else
		{
			log.info("Error loading product: " + inSourcePath);
		}
	}

	public void processDir(ContentItem inContent)
	{
		String path = makeSourcePath(inContent) + "/";
		processSourcePath(path);
	}

	public void processFile(ContentItem inContent, User inUser)
	{
		String path = makeSourcePath(inContent);
		processSourcePath(path);
	}



}
