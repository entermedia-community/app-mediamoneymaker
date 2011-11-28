/*
 * Created on Oct 19, 2004
 */
package org.openedit.store.customer;

import java.io.File;
import java.io.FilenameFilter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexWriter;
import org.openedit.data.lucene.BaseLuceneSearcher;
import org.openedit.data.lucene.RecordLookUpAnalyzer;
import org.openedit.store.CustomerArchive;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.users.User;
import com.openedit.util.PathUtilities;

/**
 * @author cburkey
 *
 */
public class CustomerSearcher extends BaseLuceneSearcher
{
	private static final Log log = LogFactory.getLog(CustomerSearcher.class);
	protected CustomerArchive fieldCustomerArchive;
	
	/* (non-javadoc)
	 * @see com.openedit.modules.search.BaseLuceneSearch#reIndexAll() 
	 */
	public void reIndexAll(IndexWriter inWriter) throws OpenEditException
	{
		log.info("Reindex of customer users directory");
		//http://today.java.net/pub/a/today/2003/07/30/LuceneIntro.html?page=last&x-maxdepth=0
		
		//Analyzer analyzer = new LuceneWordsAndNumbersAnalyzer();//org.apache.lucene.analysis.SimpleAnalyzer();//WhitespaceAnalyzer();//SimpleAnalyzer();//StopAnalyzer();
		// FIXME: Move this to XmlCustomerArchive, e.g. getAllUserNames()
		File[] usersxml = getRootDirectory().listFiles(new FilenameFilter()
		{
			public boolean accept(File inDir, String inName)
			{
				if (inName.endsWith(".xml") )
				{
					return true;
				}
				return false;
			}
		});
		
		for (int i = 0; i < usersxml.length; i++)
		{
			Document doc = new Document();
			File xconf = usersxml[i];
			String username = PathUtilities.extractPageName(xconf.getPath());
			doc.add( new Field( User.USERNAME_PROPERTY, username, Store.YES, Index.ANALYZED ) );
			Customer customer = getCustomerArchive().getCustomer(username);
			//make sure its loaded
			customer.getUser().getPassword();
			
			String phone = customer.cleanPhoneNumber();
			if( phone != null)
			{
				doc.add( new Field( "Phone1",phone , Store.YES, Index.ANALYZED) ); //If tokenized then we use our lowercase analyser
			}
			
			String last = customer.getUser().getLastName();
			if( last != null)
			{
				//TODO: We should not have to lower case this since we have a lower casing analyser
				doc.add( new Field( "lastName",last, Store.YES, Index.ANALYZED) );
			}

//			
//			for (Iterator iter = customer.getUser().getProperties().keySet().iterator(); iter.hasNext();)
//			{
//				String key = (String) iter.next();
//				String value = (String)customer.getUser().get(key);
//				if ( value != null)
//				{
//					if ( key.equals( Customer.PHONE1 ) )
//					{
//						doc.add( new Field( key, customer.cleanPhoneNumber(), Store.YES, Index.NO_NORMS) );
//					}
//					else
//					{
//						doc.add( new Field( key, value, Store.YES, Index.NO_NORMS ) );
//					}
//				}
//			}
		

		}
	}

	public Analyzer getAnalyzer()
	{
		if (fieldAnalyzer == null) {
			fieldAnalyzer = new RecordLookUpAnalyzer();
		}
		return fieldAnalyzer;
	}

	public CustomerArchive getCustomerArchive()
	{
		return fieldCustomerArchive;
	}
	
	public void setCustomerArchive(CustomerArchive inCustomerArchive)
	{
		fieldCustomerArchive = inCustomerArchive;
	}

	public HitTracker getAllHits(WebPageRequest inReq)
	{
		return null;
		//return new ListHitTracker().setList(getCustomerArchive().)
	}
}
