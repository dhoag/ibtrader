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

import com.davehoag.ib.dataTypes.LimitOrder;
import com.davehoag.ib.util.ImmediateExecutor;

public class IBClientRequestExecutorTest  {
	IBClientRequestExecutor executor;
	@Before
	public void setUp() throws Exception {
		ResponseHandler rh = new ResponseHandler();
		rh.setExecutorService(new ImmediateExecutor());
		TestClientMock client = new TestClientMock(rh);
		executor = new IBClientRequestExecutor(client, rh);
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
		executor.setExcutor(new ImmediateExecutor());
		Calendar start = Calendar.getInstance();
		start.add(Calendar.DAY_OF_WEEK, -2);
		DateFormat df = new SimpleDateFormat( "yyyyMMdd HH:mm:ss");
		String startingDate = df.format(start.getTime() );
		CaptureHistoricalDataMock mock = new CaptureHistoricalDataMock();
		mock.barSize = "bar1day";
		executor.reqHistoricalData( "IBM", startingDate, mock);
		executor.waitForCompletion();
		assertNotNull(mock.dateVal);
	}
	@Test
	public void sumbitBuyOrder() {
		executor.setExcutor(new ImmediateExecutor());
		CaptureHistoricalDataMock mock = new CaptureHistoricalDataMock();
		LimitOrder order = new LimitOrder("IBM", 100, 203.83, true);
		executor.executeOrder(order, mock);
		assertNotNull(mock.exec);
	}

}
