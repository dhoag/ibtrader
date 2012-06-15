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
		p.bought(1, "IBM", 100, 12.2);
		assertEquals(-1220.0, p.cash, .001);
		p.bought(1, "IBM", 100, 10.0);
		assertEquals(-2220.0, p.cash, .001);
	}

	@Test
	public void testSold() {
		p.sold(1, "IBM", 100, 12.2);
		assertEquals(1220.0, p.cash, .001);
		p.sold(1, "IBM", 100, 10.0);
		assertEquals(2220.0, p.cash, .001);
	}


	@Test
	public void testValue() {
		p.bought(1, "IBM", 100, 12.2);
		assertEquals(1000.0, p.getValue("IBM", 10), .0001);
	}

}
