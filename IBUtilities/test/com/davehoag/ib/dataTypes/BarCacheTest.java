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
	public void testFib(){
		BarCache qr = new BarCache();
		qr.pushLatest(newBar(10, 12));
		qr.pushLatest(newBar(12, 13));
		qr.pushLatest(newBar(12, 15));
		qr.pushLatest(newBar(14, 17));
		qr.pushLatest(newBar(15, 16));
		qr.pushLatest(newBar(13, 16));
		//17 - 10 * 1
		double res = qr.getFibonacciRetracement(6, 1);
		assertEquals(7.0, res, .01);
		qr.pushLatest(newBar(12, 15));
		qr.pushLatest(newBar(12, 14));
		qr.pushLatest(newBar(11, 13));
		res = qr.getFibonacciRetracement(6, 1);
		//Down trend - no low for the high - would need a low below 11
		assertEquals(0, res, .01);
		qr.pushLatest(newBar(12, 13));
		qr.pushLatest(newBar(13, 14));
		qr.pushLatest(newBar(13, 15));
		res = qr.getFibonacciRetracement(6, 1);
		assertEquals(4.0, res, .01);
		res = qr.getFibonacciRetracement(9, 1);
		//should still be 4 as the 16 doesn't have a low behind it
		assertEquals(4.0, res, .01);
		//should reset to the original wider band
		res = qr.getFibonacciRetracement(12, 1);
		assertEquals(7.0, res, .01);
	}
	Bar newBar(double low, double high){
		Bar b = new Bar();
		b.high = high; b.low = low;
		return b;
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

	@Test
	public void testGet(){
		BarCache qr = new BarCache();
		qr.localCache = new Bar[4];

		Bar b = new Bar();
		b.close = 2;
		b.open = 2;
		b.wap = 2;
		b.low = 1;
		b.high = 3;
		b.volume = 100 ;
		b.tradeCount = 3;
		b.originalTime = System.currentTimeMillis() / 1000;
		qr.pushLatest(b);
		
		send5bars(qr);
		
		assertEquals(4, qr.size());
		Bar fifth = qr.get(0);
		assertEquals(50.0, fifth.close, .01);
		Bar fourth = qr.get(1);
		assertEquals(40.0, fourth.close, .01);
		Bar third = qr.get(2);
		assertEquals(30.0, third.close, .01);
		try{
			qr.get(5);
			fail("Should have thrown an error");
		}catch(Exception ex){}
	}
	@Test
	public void testPsar(){
		BarCache qr = new BarCache();
		send5bars(qr);
		double [] pSar = qr.getParabolicSar(5, .02);
		System.out.println(pSar);
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
