package com.davehoag.ib.dataTypes;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.BarCache;

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
