package com.davehoag.ib.dataTypes;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PortfolioTest {
	Portfolio p;
	@Before
	public void setUp() throws Exception {
		p = new Portfolio();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testBought() {
		LimitOrder order = new LimitOrder("IBM", 100, 12.2, true);

		p.placedOrder(order);
		assertEquals(-1220.0, p.cash, .001);
		LimitOrder order2 = new LimitOrder("IBM", 100, 10.0, true);
		p.placedOrder(order2);
		assertEquals(-2220.0, p.cash, .001);
	}

	@Test
	public void testSold() {
		LimitOrder order = new LimitOrder("IBM", 100, 12.2, false);
		p.placedOrder(order);
		assertEquals(1220.0, p.cash, .001);
		order.setPrice(10.0);
		p.placedOrder(order);
		assertEquals(2220.0, p.cash, .001);
	}


	@Test
	public void testValue() {
		LimitOrder order = new LimitOrder("IBM", 100, 12.2, true);
		p.placedOrder(order);
		assertEquals(1000.0, p.getValue("IBM", 10), .0001);
	}
	
	@Test
	public void testOpenCloseAccounting(){
		int newQty = 100;
		int originalQty = 0;
		LimitOrder lmtOrder = new LimitOrder("IBM", 100, 190.0, true);
		p.openCloseAccounting(newQty, originalQty, lmtOrder);
		LimitOrder sellOrder = new LimitOrder("IBM", 100, 200.0, false);
		newQty = 0;
		originalQty = 100;
		p.openCloseAccounting(newQty, originalQty, sellOrder);
		assertEquals(100*10, p.getProfit(), .01);
		
		lmtOrder = new LimitOrder("IBM", 100, 190.0, true);
		p.openCloseAccounting(100, 0, lmtOrder);
		lmtOrder = new LimitOrder("IBM", 100, 190.0, true);
		p.openCloseAccounting(200, 100, lmtOrder);
		//sell all 200 at once
		sellOrder = new LimitOrder("IBM", 200, 200.0, false);
		p.openCloseAccounting(0, 200, sellOrder);
		assertEquals(100*10*3, p.getProfit(), .01);
	}

}
