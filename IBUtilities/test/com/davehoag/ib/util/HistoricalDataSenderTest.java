package com.davehoag.ib.util;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.davehoag.ib.CaptureHistoricalDataMock;
import com.davehoag.ib.IBClientRequestExecutor;
import com.davehoag.ib.IBClientRequestExecutorTest;
import com.davehoag.ib.ResponseHandler;
import com.davehoag.ib.TestClientMock;
import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.StockContract;
import com.ib.client.Order;

public class HistoricalDataSenderTest {
	HistoricalDataSender sender;
	int reqId = 4;
	CaptureHistoricalDataMock rh = new CaptureHistoricalDataMock();
	
	@Before
	public void setUp() throws Exception {
		ResponseHandler hl = new ResponseHandler();
		TestClientMock mock = new TestClientMock(hl);
		IBClientRequestExecutor req = new IBClientRequestExecutor(mock, hl);


		req.pushResponseHandler(reqId, rh);
		sender = new HistoricalDataSender(reqId, null, null, mock);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testCheckRestingOrders() {
		testAddLimitOrderIntContractOrder();
		sender.lastBar = new Bar();
		sender.lastBar.low = 1.5;
		sender.checkRestingOrders(sender.lastBar);
		assertNotNull(rh.exec);
	}

	@Test
	public void testIsExecutable() {
		boolean test = sender.isExecutable(2.0, "BUY", 1.9);
		assertTrue( "Should have been executable!", test);
		test = sender.isExecutable(1.8, "BUY", 2.0);
		assertFalse( "Should not have been executable!", test);
	}

	@Test
	public void testAddLimitOrderIntContractOrder() {
		StockContract lmtContract = new StockContract("IBM");
		Order order = new Order();
		order.m_action = "BUY";
		order.m_lmtPrice = 2.0;
		sender.addLimitOrder(reqId, lmtContract, order);
		assertEquals(sender.restingOrders.size(), 1);
	}

}
