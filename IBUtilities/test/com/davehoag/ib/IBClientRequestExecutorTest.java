package com.davehoag.ib;

import static org.junit.Assert.*;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class IBClientRequestExecutorTest  {
	IBClientRequestExecutor executor;
	@Before
	public void setUp() throws Exception {
		ResponseHandler rh = new ResponseHandler();
		TestClient client = new TestClient(rh);
		executor = new IBClientRequestExecutor(client, rh);
		rh.setRequestor( executor );
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
		
		Runnable r = new Runnable() { 
			public void run() { final int reqId = executor.pushRequest(); executor.endRequest(reqId);  };
		};
		executor.execute(r, 2);
		executor.execute(r, 2);
		executor.waitForCompletion();
		long elapsed = System.currentTimeMillis();
		assertTrue( elapsed - current > 1800);
	}
	/**
	 * 
	 * @throws ParseException
	 */
	@Test
	public void testReqHistoricalData() throws ParseException {
		DateFormat df = new SimpleDateFormat( "yyyyMMdd HH:mm:ss");
		String date = df.format(Calendar.getInstance().getTime() );
		executor.reqHistoricalData("IBM", date);
		executor.waitForCompletion();
	}

}
