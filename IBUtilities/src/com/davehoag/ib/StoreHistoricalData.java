package com.davehoag.ib;

import java.text.DecimalFormat;


public class StoreHistoricalData extends AbstractResponseHandler {
	final String sym; 
	public StoreHistoricalData(final String symbol){
		sym = symbol;
		
	}
	/**
	 * Delegated response from the ResponseHandler that is actually getting the call
	 */
	@Override
	public void historicalData(final int reqId, final String dateStr,
			final double open, final double high, final double low,
			final double close, final int volume, final int count,
			final double WAP, final boolean hasGaps) {

		
		DecimalFormat df = new DecimalFormat("#.##");
		System.out.println("His Data for " + sym + " - Req: " + reqId + " " + dateStr + " O:"
				+ df.format(open) + " C:" + df.format(close) + " H:"
				+ df.format(high) + " W:" + df.format(WAP) + " L:"
				+ df.format(low) + " V:" + volume + " #:" + count + " gaps?"
				+ hasGaps);
	}
}
