package com.davehoag.ib;
/**
 * Capture the historical data values - used to test the RequestHandler -> EWrapper delegation
 * @author dhoag
 *
 */
public class CaptureHistoricalDataMock extends AbstractResponseHandler {
	public String dateVal;
	@Override
	public void historicalData(final int reqId, final String dateStr,
			final double open, final double high, final double low,
			final double close, final int volume, final int count,
			final double WAP, final boolean hasGaps) {
	
		dateVal = dateStr;
		
	}
}
