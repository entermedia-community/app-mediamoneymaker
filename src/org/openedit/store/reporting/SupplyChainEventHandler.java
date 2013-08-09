package org.openedit.store.reporting;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.birt.report.engine.api.script.IUpdatableDataSetRow;
import org.eclipse.birt.report.engine.api.script.ScriptException;
import org.openedit.store.CartItem;

public class SupplyChainEventHandler extends FinancesEventHandler
{
	private static final Log log = LogFactory.getLog(SupplyChainEventHandler.class);
	
	protected boolean requiresCartItems()
	{
		return false;
	}
}
