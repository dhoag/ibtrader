package com.davehoag.ib.dataTypes;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SimpleMovingAvgTest {
	SimpleMovingAvg sma; 
	@Before
	public void setUp() throws Exception {
		//seeds are a time series of values with oldest at position 0
		double [] seeds = { 1.0, 2.0, 3.0, 4.0, 5.0 };
		sma = new SimpleMovingAvg(3, 5, seeds);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetAvg() {
		double fast = sma.getFastAvg();
		double slow = sma.getSlowAvg();
		assertEquals(4.0, fast,.0001);
		assertEquals(3.0, slow, .0001);
	}
	@Test
	public void testIsTrendingUp(){
		assert(sma.isTrendingUp());
	}
	@Test
	public void testInitTrend(){
		assert(!sma.initTrend(true));
		assert( sma.initTrend(false));
		assert(!sma.initTrend(false));
		assert(sma.initTrend(true));
	}
	@Test
	public void testNewTick(){
		sma.newTick(5.0);
		assert(sma.isTrendingUp());
		assertEquals((5.0+5.0+4.0) / 3.0, sma.getFastAvg() ,.0001);
		//the 1 should drop off
		assertEquals((5.0+5.0+4.0+3+2) / 5.0, sma.getSlowAvg() ,.0001);
		//lets wrap around each
		sma.newTick(4.0);
		sma.newTick(3.0);
		sma.newTick(2.0);
		sma.newTick(1.0);
		assert(!sma.isTrendingUp());
		assertEquals(3.0, sma.getSlowAvg(), .0001);
		assertEquals(2.0, sma.getFastAvg(), .0001);
	}
}
