package com.davehoag.ib.dataTypes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

public class BarCacheTest {

	@Before
	public void setUp() {

	}
	@Test
	public void testLocalCache() {
		BarCache qr = new BarCache();
		qr.localCache = new Bar[3];

		send5bars(qr);
		Bar[] cache = qr.getBars(2);
		assertEquals(2, cache.length);
		assertEquals(5, cache[0].tradeCount);
		assertEquals(4, cache[1].tradeCount);
	}

	@Test
	public void testPartialFilled() {
		BarCache qr = new BarCache();
		qr.localCache = new Bar[10];
		send5bars(qr);
		double[] vals = qr.getVwap(5);
		assertEquals(5, vals.length);

		try {
			qr.getBars(9);
			fail("did not generate exception");
		} catch (IllegalStateException ex) {
		}
	}
	@Test
	public void testLocalCacheWrap() {
		BarCache qr = new BarCache();
		qr.localCache = new Bar[3];

		send5bars(qr);
		double[] vals = qr.getVwap(3);
		assertEquals(3, vals.length);
		assertEquals(5 * 9, vals[0], .01);
		assertEquals(4 * 9, vals[1], .01);
		assertEquals(3 * 9, vals[2], .01);
	}

	@Test
	public void testLocalCacheVwap() {
		BarCache qr = new BarCache();
		qr.localCache = new Bar[3];

		send5bars(qr);
		double[] vals = qr.getVwap(2);
		assertEquals(2, vals.length);
		assertEquals(5 * 9, vals[0], .01);
		assertEquals(4 * 9, vals[1], .01);
	}

	@Test
	public void testWrapping() {
		BarCache qr = new BarCache();
		qr.localCache = new Bar[12];

		send5bars(qr);
		send5bars(qr);
		send5bars(qr);
		send5bars(qr);
		send5bars(qr);
		send5bars(qr);
		double[] vals = qr.getVwap(8);
		assertEquals(8, vals.length);
		assertEquals(5 * 9, vals[0], .01);
		assertEquals(4 * 9, vals[1], .01);
	}
	/**
	 * @param qr
	 */
	protected void send5bars(BarCache qr) {
		for (int i = 1; i < 6; i++) {
			Bar b = new Bar();
			b.close = 10 * i;
			b.open = 8 * i;
			b.wap = 9 * i;
			b.low = 7 * i;
			b.high = 11 * i;
			b.volume = 100 * i;
			b.tradeCount = i;
			b.originalTime = System.currentTimeMillis() / 1000;

			qr.pushLatest(b);
		}
	}

}
