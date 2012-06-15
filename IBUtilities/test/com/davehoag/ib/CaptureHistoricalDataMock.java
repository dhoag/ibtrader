package com.davehoag.ib;

import com.ib.client.Contract;
import com.ib.client.Execution;

/**
 * Capture the historical data values - used to test the RequestHandler -> EWrapper delegation
 * @author dhoag
 *
 */
public class CaptureHistoricalDataMock extends ResponseHandlerDelegate {
	public String dateVal;
	public Execution exec;
	@Override
	public void historicalData(final int reqId, final String dateStr,
			final double open, final double high, final double low,
			final double close, final int volume, final int count,
			final double WAP, final boolean hasGaps) {
	
		dateVal = dateStr;
		
	}
	@Override
	public void execDetails(final int reqId, final Contract contract, final Execution execution) {
		//Filled!
		exec = execution;
	}
}
