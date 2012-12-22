package com.davehoag.ib;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.Portfolio;
import com.davehoag.ib.util.ImmediateExecutor;

public class QuoteRouterTest {
	IBClientRequestExecutor exec;
	Portfolio port;

	@Before
	public void setUp() {
		ResponseHandler rh = new ResponseHandler();
		rh.setExecutorService(new ImmediateExecutor());
		TestClientMock client = new TestClientMock(rh);
		exec = new IBClientRequestExecutor(client, rh);
		port = new Portfolio();
	}
	@Test
	public void testLocalCache() {
		QuoteRouter qr = new QuoteRouter("test", exec, port) {
			@Override
			protected void updatePortfolioTime(final long time) {
			}
		};
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
	protected void send5bars(QuoteRouter qr) {
		for (int i = 1; i < 6; i++) {
			double close = 10 * i;
			double open = 8 * i;
			double wap = 9 * i;
			double low = 7 * i;
			double high = 11 * i;
			int volume = 100 * i;
			int count = i;
			long time = System.currentTimeMillis() / 1000;
			int reqId = 0;
			qr.realtimeBar(reqId, time, open, high, low, close, volume, wap, count);
		}
	}

}
