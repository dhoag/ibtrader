package com.davehoag.ib.tools;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import com.davehoag.ib.CassandraDao;
import com.davehoag.ib.IBClientRequestExecutor;
import com.davehoag.ib.StoreHistoricalData;

public class VerifyData {
	static String [] bars = { "bar1day", "bar5sec" };
	static String [] symbols = { "QQQ", "SPY", "TIP", "AGG" };
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final IBClientRequestExecutor clientInterface = IBClientRequestExecutor.connectToAPI();
		final DateFormat df = new SimpleDateFormat("yyyyMMdd");
		
		for( String barSize: bars){
			for(String symbol: symbols )
			try{
				System.out.print("Checking " + barSize + " for " + symbol);
				long date = CassandraDao.getInstance().findMostRecentDate(symbol, barSize);
				if(date > 1000){
					System.out.println(" " + new Date(date*1000));
					String dateStr = df.format(new Date(date*1000));
					PullStockData.pullData(dateStr, barSize, clientInterface,0, symbol );
					clientInterface.waitForCompletion();
				}
				else { //No prior data
					Calendar c = Calendar.getInstance();
					c.add(-10, Calendar.MONTH);
					String dateStr = df.format(c);
					PullStockData.pullData(dateStr, barSize, clientInterface,0, symbol );
					clientInterface.waitForCompletion();
				}
			}
			catch(Exception ex){
				System.err.println("Problem with " + symbol + " " + ex);
			}
		}
		
		clientInterface.close();
	}

}
