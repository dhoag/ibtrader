package com.davehoag.ib.tools;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import com.davehoag.ib.CassandraDao;
import com.davehoag.ib.IBClientRequestExecutor;
import com.davehoag.ib.util.HistoricalDateManipulation;

public class VerifyData {
	static String [] bars = { "bar1day", "bar5sec" };
	static String [] symbols = { "QQQ", "SPY", "TIP", "AGG" };
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length > 0) {
			symbols = args;
		}
		final IBClientRequestExecutor clientInterface = IBClientRequestExecutor.connectToAPI();
		pullLatestMarketData(clientInterface);
		cleanupMissingData(clientInterface);
		clientInterface.close();
	}

	/**
	 * @param clientInterface
	 */
	protected static void cleanupMissingData(final IBClientRequestExecutor clientInterface) {
		final Calendar today = Calendar.getInstance();
		today.add(Calendar.MONTH, -8);
		final String startingDateStr = HistoricalDateManipulation.getDateAsStr(today.getTime());
		for(String symbol: symbols )
		try{
				ArrayList<String> missingData = getDatesMissingData(startingDateStr, symbol);
				clientInterface.reqHistoricalData(missingData, symbol);
				clientInterface.waitForCompletion();
		}
		catch(Exception ex){
			ex.printStackTrace();
		}
	}

	/**
	 * 
	 */
	private static void pullLatestMarketData(final IBClientRequestExecutor clientInterface) {
		final DateFormat df = new SimpleDateFormat("yyyyMMdd");
		
		for( String barSize: bars){
			for(String symbol: symbols )
			try{
				System.out.println(Thread.currentThread() +"Checking " + barSize + " for " + symbol);
				long date = CassandraDao.getInstance().findMostRecentDate(symbol, barSize);
				if(date > 1000){
					System.out.println(" " + new Date(date*1000));
					String dateStr = df.format(new Date(date*1000));
					PullStockData.pullData(dateStr, barSize, clientInterface,0, symbol );
					clientInterface.waitForCompletion();
				}
				else { //No prior data
					Calendar c = Calendar.getInstance();
					c.add(Calendar.MONTH, -10);
					String dateStr = df.format(c.getTime());
					PullStockData.pullData(dateStr, barSize, clientInterface,0, symbol );
					clientInterface.waitForCompletion();
				}
			}
			catch(Exception ex){
				System.err.println("Skipping verification of " + symbol + " " + barSize + " due to " + ex);
				ex.printStackTrace(System.err);
			}
		}
	}

	/**
	 * Determine the dates for which we are missing 5 second bar data. Assumes
	 * we have 1 day bar data for all valid trading days.
	 * 
	 * @param startingDateStr
	 * @param symbol
	 * @return
	 * @throws ParseException
	 */
	static ArrayList<String> getDatesMissingData(final String startingDateStr, final String symbol) throws ParseException{
		final ArrayList<String> result = new ArrayList<String>();
		final Calendar today = Calendar.getInstance();
		final ArrayList<String> tradingDays = HistoricalDateManipulation.getWeekDays(startingDateStr, today);
		for(String dateStr : tradingDays ){
			final long day = HistoricalDateManipulation.getTime(dateStr);
			
			final int barDayCount = CassandraDao.getInstance().countRecordsForCurrentDay(symbol, "bar1day", day);
			final int bar5SecCount = CassandraDao.getInstance().countRecordsForCurrentDay(symbol, "bar5sec", day);
			if (barDayCount == 1 && (bar5SecCount != 4680 & bar5SecCount != 2520)) {
				String justDateValues = dateStr.substring(0, dateStr.indexOf(' '));
				DateFormat df = new SimpleDateFormat("yyyyMMdd");
				Date d = df.parse(justDateValues);
				Calendar sameDay = Calendar.getInstance();
				sameDay.setTime(d);
				ArrayList<String> hoursToGet = HistoricalDateManipulation.getDatesBrokenIntoHours(
						justDateValues, sameDay);
				result.addAll(hoursToGet);
				System.out.println(symbol + ' ' + dateStr+ ' ' + barDayCount + ' ' + bar5SecCount);
			}
		}
		return result;
	}
}
