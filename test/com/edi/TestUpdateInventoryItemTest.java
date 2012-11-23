package com.edi;

import static org.junit.Assert.assertEquals;

import org.openedit.entermedia.publishing.PublishResult;

public class TestUpdateInventoryItemTest {

	public void testTestUpdateInventoryItem() {
		
		PublishResult result = new PublishResult();
		
		TestUpdateInventoryItem myTest = new TestUpdateInventoryItem();
		result = myTest.testUpdateInventoryItem("995", "2");
		assertEquals(true, result.isComplete());
	}

}
