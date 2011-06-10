package org.openedit.store.links;

import org.openedit.store.Category;

import com.openedit.webui.tree.HtmlTreeRenderer;
import com.openedit.webui.tree.TreeRenderer;
import com.openedit.webui.tree.WebTree;

/**
 * A {@link TreeRenderer} that renders {@link WebTree}s whose nodes are
 * {@link Category}s.
 * 
 * @author Eric Galluzzo
 */
public class CatalogTreeRenderer extends HtmlTreeRenderer
{
	public CatalogTreeRenderer()
	{
		super();
	}

	public CatalogTreeRenderer( WebTree inWebTree )
	{
		super( inWebTree );
	}

	/**
	 * @deprecated use getModel().getId()
	 * @param inNode
	 * @return
	 
	public String toId( Object inNode )
	{
		return getWebTree().getModel().getId(inNode);
		//return ( (Category) inNode ).getId();
	}
	*/

	public String toName( Object inNode )
	{
		return ( (Category) inNode ).getName();
	}

	public String toUrl( Object inNode )
	{
		return getWebTree().getModel().getId(inNode);
	}
}
