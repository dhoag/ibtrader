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
	public void tetEma(){
		double val = sma.calcEma(22.15, 22.22, 10);
		assertEquals( 22.207,val, .001);
	}
	@Test
	public void testVolatility(){
		double [] seeds = { 10.0, 12.0, 12*1.2, 12*1.2*1.2 };
		SimpleMovingAvg sma = new SimpleMovingAvg(3, 4, seeds);
		assertEquals(20, sma.getVolatilityPercent(),.011);
		sma.newTick(12*1.2*1.2*1.4);
		assertTrue(sma.recentJumpExceedsAverage());
		sma.newTick(12*1.2*1.2*1.4);
		assertFalse(sma.recentJumpExceedsAverage());
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
		assert(!sma.updateTrend(true));
		assert( sma.updateTrend(false));
		assert(!sma.updateTrend(false));
		assert(sma.updateTrend(true));
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
	@Test
	public void testNoSeeds(){
		sma = new SimpleMovingAvg(3, 5);
		generateException();
		sma.newTick(1.0);
		generateException();
		sma.newTick(2.0);
		generateException();
		sma.newTick(3.0);
		generateException();
		sma.newTick(4.0);
		generateException();
		assertFalse(sma.newTick(5.0));
		assert(sma.isTrendingUp());
	}
	@Test
	public void testRest(){
		assert(sma.isTrendingUp());
		sma.reset();
		generateException();
		sma.newTick(1.0);
		generateException();
		sma.newTick(2.0);
		generateException();
		sma.newTick(3.0);
		generateException();
		sma.newTick(4.0);
		generateException();
		assertFalse(sma.newTick(5.0));
		assert(sma.isTrendingUp());		
	}

	/**
	 * 
	 */
	protected void generateException() {
		try{
			sma.isTrendingUp();
			assertTrue("Exception not generated despite not being initialized.", false);
		}
		catch(Throwable t){
			//should throw an exception!
		}
	}
}
