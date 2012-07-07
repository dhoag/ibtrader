package com.davehoag.ib;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.davehoag.ib.util.HistoricalDateManipulation;

/**
 * Write the historical data to Cassandra
 * @author dhoag
 *
 */
public class StoreHistoricalData extends ResponseHandlerDelegate {
	final String sym; 
	String barSize =  "bar5sec";
	
	public StoreHistoricalData(final String symbol, IBClientRequestExecutor ibInterface){
		super(ibInterface);
		sym = symbol;
		
	}
	public boolean isValidSize(final String size){
		switch(size){
			case "bar1day":
			case "bar15min" :
			case "bar5sec" : return true;
		}
		return false;
	}
	public String getBar(){ 
		if(barSize.equals("bar1day")) return IBConstants.bar1day;
		if (barSize.equals("bar15min")) return IBConstants.bar15min;
		if (barSize.equals("bar5sec")) return IBConstants.bar5sec;
		return IBConstants.bar1day;
	}
	public void setBarSize(final String aBarSize){
		barSize = aBarSize;
	}
	public String getDuration(){
		if(barSize.equals("bar1day")) return IBConstants.dur1year;
		if (barSize.equals("bar15min")) return IBConstants.dur1week;
		if (barSize.equals("bar5sec")) return IBConstants.dur1hour;
		return IBConstants.dur1hour;
	}
	/**
	 * Return the dates 
	 * @param startingDate
	 * @return
	 * @throws ParseException
	 */
	public ArrayList<String> getDates(String startingDate) throws ParseException{
		ArrayList<String> dates = new ArrayList<String>();
		
		if(barSize.equals("bar1day")) dates.add(HistoricalDateManipulation.getDateAsStr(Calendar.getInstance().getTime()));
		else if (barSize.equals("bar5sec")) dates = HistoricalDateManipulation.getDatesBrokenIntoHours(startingDate);
		else if (barSize.equals("bar15min")) dates = HistoricalDateManipulation.getDatesBrokenIntoWeeks(startingDate, Calendar.getInstance());
		
		return dates;
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

		final String actualDateStr = getModifiedDateString(dateStr);
		CassandraDao.getInstance().insertHistoricalData(barSize, sym, actualDateStr, open, high, low, close, volume, count, WAP, hasGaps);
		countOfRecords++;
		/* DecimalFormat df = new DecimalFormat("#.##");
		System.out.println("His Data for " + sym + " - Req: " + reqId + " " + dateStr + " O:"
				+ df.format(open) + " C:" + df.format(close) + " H:"
				+ df.format(high) + " W:" + df.format(WAP) + " L:"
				+ df.format(low) + " V:" + volume + " #:" + count + " gaps?"
				+ hasGaps);  */
	}
	/**
	 * @param dateStr
	 * @return
	 */
	protected String getModifiedDateString(final String dateStr) {
		String actualDateStr = dateStr; 
		if(barSize.equals("bar1day"))
		try{ //dates are not in seconds, since I don't want quries to worry about bar size convert to seconds
			long dateInSeconds  = HistoricalDateManipulation.getTime(dateStr + " 16:00:00");
			actualDateStr = "" + dateInSeconds;
		}catch(ParseException ex){
			throw new IllegalStateException("Should not get parse errors with 1 day bars");
		}
		return actualDateStr;
	}
}
