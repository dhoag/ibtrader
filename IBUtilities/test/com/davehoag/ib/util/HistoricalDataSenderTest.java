package com.davehoag.ib.util;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.davehoag.ib.CaptureHistoricalDataMock;
import com.davehoag.ib.IBClientRequestExecutor;
import com.davehoag.ib.ResponseHandler;
import com.davehoag.ib.TestClientMock;
import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.StockContract;
import com.davehoag.ib.util.HistoricalDataSender;
import com.ib.client.Order;

public class HistoricalDataSenderTest {
	HistoricalDataSender sender;
	int reqId = 4;
	CaptureHistoricalDataMock rh = new CaptureHistoricalDataMock();
	
	@Before
	public void setUp() throws Exception {
		ResponseHandler hl = new ResponseHandler();
		hl.setExecutorService(new ImmediateExecutor());
		TestClientMock mock = new TestClientMock(hl);
		IBClientRequestExecutor req = new IBClientRequestExecutor(mock, hl);
		req.pushResponseHandler(reqId, rh);
		sender = new HistoricalDataSender(reqId, null, null, mock);
		sender.lastBar = new Bar();
		sender.lastBar.low = 1.5;
		sender.lastBar.close = 2.0;
		sender.lastBar.high = 2.5;
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetLimitPrice(){
		double offset = 10.0;
		double price = 100.0;
		boolean buy = false;
		Order order = new Order();
		order.m_action = "SELL";
		double limit = sender.getOrderOnBook( 1, null, order).getLimitPrice(buy, offset, price);
		assertEquals(90.0, limit, .001);
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
		boolean buy = true;
		boolean test = sender.isExecutable(2.0, buy, 1.9);
		assertTrue( "Should have been executable!", test);
		test = sender.isExecutable(1.8, buy, 2.0);
		assertFalse( "Should not have been executable!", test);
	}

	@Test
	public void testAddLimitOrderIntContractOrder() {
		StockContract lmtContract = new StockContract("IBM");
		Order order = new Order();
		order.m_action = "BUY";
		order.m_lmtPrice = 2.0;
		order.m_orderType = "LMT";
		sender.addLimitOrder(reqId, lmtContract, order);
		assertEquals(sender.restingOrders.size(), 1);
	}
	@Test
	public void testFillOrBook(){
		StockContract lmtContract = new StockContract("IBM");
		Order order = new Order();
		order.m_action = "BUY";
		order.m_lmtPrice = 2.0;
		order.m_orderType = "LMT";
		assert(sender.fillOrBookOrder(reqId, lmtContract, order));
		order.m_lmtPrice = 1.9;
		assertFalse(sender.fillOrBookOrder(reqId, lmtContract, order));
		order.m_orderType = "TRAIL";
		order.m_action = "SELL";
		order.m_trailingPercent = .01;
		assertFalse(sender.fillOrBookOrder(reqId, lmtContract, order));
		sender.checkRestingOrders(sender.lastBar);
		assertEquals(0, sender.restingOrders.size() );
		assertEquals(1.98, rh.exec.m_price , .001);
	}
	@Test
	public void testTrailingFills(){
		StockContract lmtContract = new StockContract("IBM");
		Order order = new Order();
		order.m_orderType = "TRAIL";
		order.m_action = "SELL";
		order.m_trailingPercent = .3;
		//test an order that should adjust and then fill
		//trail limit is 1.4, so it won't obviously fill
		//but, if the price when to 2.5 trail would up to 1.75
		//and the bar drops to 1.5 it would stop out at 1.75
		sender.fillOrBookOrder(reqId, lmtContract, order);
		sender.checkRestingOrders(sender.lastBar);
		assertEquals(1.75, rh.exec.m_price , .001);
		assertEquals("SELL", rh.exec.m_side);
	}

}
