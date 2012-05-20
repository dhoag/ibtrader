package com.davehoag.ib;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class IBClientRequestExecutorTest  {
	IBClientRequestExecutor executor = new IBClientRequestExecutor(null);
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testPushRequest() {
		int id1 = executor.pushRequest();
		int id2 = executor.pushRequest();
		Assert.assertTrue(id1 != id2);
		executor.endRequest(id1);
		int reusedId1 = executor.pushRequest();
		assertTrue(id1 == reusedId1);
	}

	/**
	 * Test the delay and the "wait" functionality
	 */
	@Test
	public void testExecute() {
		long current = System.currentTimeMillis();
		final int id = executor.pushRequest();
		Runnable r = new Runnable() { 
			public void run() { /*do nothing */ };
		};
		executor.execute(r, 2);
		//the first executed immediately as nothing in queue
		r = new Runnable() { 
			//End the request and release the wait lock
			public void run() { executor.endRequest(id); };
		};
		executor.execute(r, 2);
		executor.waitForCompletion();
		long elapsed = System.currentTimeMillis();
		assertTrue( elapsed - current > 1800);
	}

}
