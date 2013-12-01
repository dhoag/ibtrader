package com.davehoag.ib.dataTypes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

public class BarCacheTest {
	BarCache sampleSet;
	@Before
	public void setUp() {
		int idx = 1;
		BarCache qr = new BarCache();
		Bar b = newBar(12, 15, 11.5, 15.8, 14.25);
		b.volume = 2012;
		b.originalTime = idx++;
		qr.push(b);
		b = newBar(15.4, 15.6, 15 ,17,15.7);
		b.volume = 2012;
		b.originalTime = idx++;
		qr.push(b);
		b = newBar(15, 17, 14.7,17.1,16.9 );
		b.volume = 2012;
		b.originalTime = idx++;
		qr.push(b);
		b = newBar(17, 19, 16.5,19.6,18.75);
		b.volume = 2012;
		b.originalTime = idx++;
		qr.push(b);
		b = newBar(19, 19.6, 18.5,20,18.9);
		b.volume = 2012;
		b.originalTime = idx++;
		qr.push(b);
		b = newBar(19.6, 18.6, 18.4,20,19.5 );
		b.volume = 2012;
		b.originalTime = idx++;
		qr.push(b);
		b = newBar(18.6, 22.2, 17,23,21 );
		b.volume = 2012;
		b.originalTime = idx++;
		qr.push(b);
		sampleSet = qr;
	}
	@Test 
	public void testADL(){
		Bar aBar = sampleSet.get(0);
		double expectedAdl = (((aBar.close - aBar.low) - (aBar.high - aBar.close)) / (aBar.high - aBar.low))*aBar.volume;
		double adl = sampleSet.getADL(0, 1, false);
		assertEquals(expectedAdl, adl, .001);
		aBar = sampleSet.get(1);
		expectedAdl = (((aBar.wap - aBar.low) - (aBar.high - aBar.wap)) / (aBar.high - aBar.low))*aBar.volume;
		adl = sampleSet.getADL(1, 1, true);
		assertEquals(expectedAdl, adl, .001);

		aBar = sampleSet.get(0);
		expectedAdl += (((aBar.wap - aBar.low) - (aBar.high - aBar.wap)) / (aBar.high - aBar.low))*aBar.volume;
		adl = sampleSet.getADL(0, 2, true);
		assertEquals(expectedAdl, adl, .001);
	}
	@Test
	public void testRSI(){
		int idx = 1;
		BarCache qr = new BarCache();
		Bar b = newBar(12, 15, 0, 0, 0);
		Bar orig = b;
		b.originalTime = idx++;
		qr.push(b);
		b = newBar(12, 15, 0,0,0);
		b.originalTime = idx++;
		qr.push(b);
		b = newBar(15, 17, 0,0,0 );
		b.originalTime = idx++;
		qr.push(b);
		b = newBar(17, 19, 0,0,0);
		b.originalTime = idx++;
		qr.push(b);
		b = newBar(19, 19.6, 0,0,0 );
		b.originalTime = idx++;
		qr.push(b);
		b = newBar(19.6, 18.6, 0,0,0 );
		b.originalTime = idx++;
		qr.push(b);
		double rsi = qr.getRSI(0, 3);
		b = newBar(18.6, 22.2, 0,0,0 );
		b.originalTime = idx++;
		qr.push(b);
		System.out.println(qr.getRSI(0, 2));
		System.out.println(qr.getRSI(0, 3));
		
		assertEquals(rsi, qr.getRSI(1, 3), .02);
		//TODO validate RSI calculation
	}
	@Test
	public void testFutureWeight(){
		int idx = 1;
		BarCache qr = new BarCache();
		Bar b = newBar(12, 15, 12, 16, 14.5 );
		Bar orig = b;
		b.originalTime = idx++;
		qr.push(b);
		b = newBar(12, 15, 12, 16, 14.5 );
		b.originalTime = idx++;
		qr.push(b);
		b = newBar(15, 17, 14.5, 17.1, 15.5 );
		b.originalTime = idx++;
		qr.push(b);
		b = newBar(17, 19, 16, 20, 18 );
		b.originalTime = idx++;
		qr.push(b);
		b = newBar(19, 19.6, 18.7, 20, 19.5 );
		b.originalTime = idx++;
		qr.push(b);
		int future = qr.getFutureTrend(orig, 4);
		assert(future > 0);
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
		qr.push(newBar(10, 12));
		qr.push(newBar(12, 13));
		qr.push(newBar(12, 15));
		qr.push(newBar(14, 17));
		qr.push(newBar(15, 16));
		qr.push(newBar(13, 16));
		//17 - 10 * 1
		double res = qr.getFibonacciRetracement(6, 1);
		assertEquals(10.0, res, .01);
		res = qr.getFibonacciRetracement(6, 0);
		assertEquals(17.0, res, .01);
		qr.push(newBar(12, 15));
		qr.push(newBar(12, 14));
		qr.push(newBar(11, 13));
		res = qr.getFibonacciRetracement(6, 1);
		//Down trend - no low for the high - would need a low below 11
		assertEquals(0, res, .01);
		qr.push(newBar(12, 13));
		qr.push(newBar(13, 14));
		qr.push(newBar(13, 15));
		res = qr.getFibonacciRetracement(6, 1);
		assertEquals(11.0, res, .01);
		res = qr.getFibonacciRetracement(9, 1);
		//should still be 4 as the 16 doesn't have a low behind it
		assertEquals(11.0, res, .01);
		//should reset to the original wider band
		res = qr.getFibonacciRetracement(12, 1);
		assertEquals(10.0, res, .01);
	}
	Bar newBar(double low, double high){
		Bar b = new Bar();
		b.high = high; b.low = low;
		return b;
	}
	Bar newBar(double open, double close, double low, double high, double wap){
		assert( open >= low && open <= high);
		assert( close >= low && close <= high);
		assert( wap >= low && wap <= high);
		Bar b = new Bar();
		b.high = high; b.low = low;
		b.open = open; b.close = close;
		b.wap = wap;
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
		qr.push(b);
		
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
		double first = pSar[0];
		qr.push(newBar(20,120));//ignore this bar in test
		pSar = qr.getParabolicSar(1, 5, .02);
		assertEquals(first, pSar[0], .02);
		//TODO validate PSAR calculation
	}
	@Test
	public void testMA(){
		BarCache qr = new BarCache();
		send5bars(qr);
		
		double closeMa = qr.getMA(5, 'c');
		assertEquals((10+20+30+40+50)/5, closeMa, .002);
		
		closeMa = qr.getMA(1,4, 'c');
		assertEquals((10+20+30+40)/4, closeMa, .002);
	}
	@Test
	public void testStdDev(){
		BarCache qr = new BarCache();
		send5bars(qr);
		
		double closeMa = qr.getStdDev(0, 5, 'c');
		assertEquals(14.142, closeMa, .002);
		
		closeMa = qr.getStdDev(1,4, 'c');
		assertEquals(11.18, closeMa, .002);
	}
	@Test
	public void testCorrelation(){
		BarCache qr = new BarCache();
		send5bars(qr);
		double correlation = qr.correlation(qr, 0, 5, 'c');
		assertEquals(1, correlation, .001);
	}
	@Test
	public void testIndexOf(){
		BarCache bc = new BarCache(5);
		Bar b = newBar(12,15) ;
		b.originalTime = 10;
		bc.push(b);
		int idx=bc.indexOf(10);
		assertEquals(0, idx); 
		b = newBar(12,15) ;
		b.originalTime = 15;
		bc.push(b);
		idx=bc.indexOf(10);
		assertEquals(1, idx); 
		idx=bc.indexOf(12);
		assertEquals(1, idx); 
		idx=bc.indexOf(15);
		assertEquals(0, idx);
		for(int i = 0; i < 5; i++){
			b = newBar(12,15) ;
			b.originalTime = 15 + (i*2);
			bc.push(b);
		}
		idx=bc.indexOf(15+(4*2));
		assertEquals(0, idx);
		idx=bc.indexOf(15+(3*2));
		assertEquals(1, idx);
		idx=bc.indexOf(15+(3*2)+1);
		assertEquals(1, idx);
		idx=bc.indexOf(15+(2*2));
		assertEquals(2, idx);
		idx=bc.indexOf(15+(1*2));
		assertEquals(3, idx);
		idx=bc.indexOf(15);
		assertEquals(4, idx);
		b = newBar(12,15);
		b.originalTime = 150;
		bc.push(b);
		b = newBar(12,15);
		b.originalTime = 155;
		bc.push(b);
		idx = bc.indexOf(150);
		assertEquals(1, idx);
		idx = bc.indexOf(21);
		assertEquals(3, idx);
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

			qr.push(b);
		}
	}
}
