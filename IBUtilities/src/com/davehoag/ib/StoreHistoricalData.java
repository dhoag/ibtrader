package com.davehoag.ib;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;

import org.apache.logging.log4j.LogManager;

import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.BarCache;
import com.davehoag.ib.util.HistoricalDateManipulation;

/**
 * Write the historical data to Cassandra
 * @author dhoag
 *
 */
public class StoreHistoricalData extends ResponseHandlerDelegate {
	final String sym; 
	String barSize =  "bar5sec";
	long currentOpen = 0;
	boolean skip = false;
	BarCache barCache = null;

	public StoreHistoricalData(final String symbol, IBClientRequestExecutor ibInterface){
		super(ibInterface);
		sym = symbol;
	}
	public boolean isSkippingDate(final String dateStr){
		long newOpen;
		try {
			newOpen = HistoricalDateManipulation.getOpen(HistoricalDateManipulation.getTime(dateStr));

		} catch (ParseException e) {
			e.printStackTrace();
			return false;
		}
		if(currentOpen != newOpen){
			skip = false;
			currentOpen = newOpen;
		}
		return skip;
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
	 * Remove all dates for which we have an opening bar
	 * @param dates
	 */
	public void filterExistingDates(ArrayList<String> dates){
		if( barSize.equals("bar1day") ) return;
		ArrayList<String> purgeList = new ArrayList<String>();
		for(String dateStr: dates)
		try{
			long today = HistoricalDateManipulation.getTime(dateStr);
			Bar openBar = CassandraDao.getInstance().getOpen(sym, today);
			if(openBar != null){
				purgeList.add(dateStr);
			}
		}//Should never throw a parse exception
		catch(Exception ex){ ex.printStackTrace(); };
		dates.removeAll(purgeList);
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
		filterExistingDates(dates);
		return dates;
	}
	/**
	 * Provide some context to the log information.
	 */
	@Override
	public void info(  final String msg) {
		LogManager.getLogger("HistoricalData" ).info( msg + " " + sym);
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
		if(barCache == null){
			CassandraDao.getInstance().insertHistoricalData(barSize, sym, actualDateStr, open, high, low, close, volume, count, WAP, hasGaps);
		}
		else {
			Bar aBar = new Bar();
			aBar.symbol = sym; aBar.close = close; aBar.hasGaps = hasGaps; aBar.high = high;
			aBar.low = low; aBar.close =close;
			aBar.volume = volume; aBar.tradeCount = count; aBar.wap = WAP;
			
			aBar.originalTime = Long.valueOf(actualDateStr);

			barCache.pushLatest(aBar);
		}
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
		try{ 
			//dates are not in seconds, since I don't want queries to worry about bar size convert to seconds.
			long dateInSeconds  = HistoricalDateManipulation.getTime(dateStr + " 08:30:00");
			actualDateStr = "" + dateInSeconds;
		}catch(ParseException ex){
			throw new IllegalStateException("Should not get parse errors with 1 day bars");
		}
		return actualDateStr;
	}
	@Override
	public void error(int id, int errorCode, String errorMsg) {
		LogManager.getLogger("HistoricalData" ).error( "Realtime bar failed: " + id+ " " + errorCode + " "+ errorMsg);
		switch(errorCode){
		case 162:
		case 321: //skip that day?
			LogManager.getLogger("HistoricalData").warn(
					"Skiping current day " + HistoricalDateManipulation.getDateAsStr(currentOpen));
			skip = true;
		}
	}
	public void setCacheOnly(int i) {
		barCache = new BarCache(i); 
	}
	public BarCache getCache() {
		return barCache;
	}
}
