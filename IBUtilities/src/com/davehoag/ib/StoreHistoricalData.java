package com.davehoag.ib;

import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Write the historical data to Cassandra
 * @author dhoag
 *
 */
public class StoreHistoricalData extends ResponseHandlerDelegate {
	final String sym; 
	CassandraDao dao = new CassandraDao();
	public StoreHistoricalData(final String symbol, IBClientRequestExecutor ibInterface){
		super(ibInterface);
		sym = symbol;
		
	}
	/**
	 * Provide some context to the log information.
	 */
	public void log( final Level logLevel, final String msg) {
		Logger.getLogger("HistoricalData:" + sym).log(logLevel, msg);
	}
	/**
	 * Delegated response from the ResponseHandler that is actually getting the call
	 */
	@Override
	public void historicalData(final int reqId, final String dateStr,
			final double open, final double high, final double low,
			final double close, final int volume, final int count,
			final double WAP, final boolean hasGaps) {

		dao.insertHistoricalData(sym, dateStr, open, high, low, close, volume, count, WAP, hasGaps);
		countOfRecords++;
		/*DecimalFormat df = new DecimalFormat("#.##");
		System.out.println("His Data for " + sym + " - Req: " + reqId + " " + dateStr + " O:"
				+ df.format(open) + " C:" + df.format(close) + " H:"
				+ df.format(high) + " W:" + df.format(WAP) + " L:"
				+ df.format(low) + " V:" + volume + " #:" + count + " gaps?"
				+ hasGaps); */
	}
}
