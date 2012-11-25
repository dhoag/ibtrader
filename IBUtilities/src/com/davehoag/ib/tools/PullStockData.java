package com.davehoag.ib.tools;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.LoggerFactory;

import com.davehoag.ib.CassandraDao;
import com.davehoag.ib.IBClientRequestExecutor;
import com.davehoag.ib.ResponseHandler;
import com.davehoag.ib.StoreHistoricalData;
import com.ib.client.EClientSocket;
/**
 * http://individuals.interactivebrokers.com/php/apiguide/interoperability/dde_excel/tabhistorical.htm
 * http://www.interactivebrokers.com/php/apiUsersGuide/apiguide/api/historical_data_limitations.htm
 * 
 * @author David Hoag
 *
 */
public class PullStockData {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int i = 0;
		String startDateStr = args[i++];
		String barSize = args[i++];

		IBClientRequestExecutor clientInterface = IBClientRequestExecutor.connectToAPI();
		
		try {
			pullData(startDateStr, barSize, clientInterface, i, args);
		} catch (ParseException e) {
			LoggerFactory.getLogger("PullStockData").error( "Parse Exception!! " + startDateStr, e);
		}
		finally { 
			clientInterface.close();
		}
        System.exit(0);
	}
	/**
	 * @param args
	 * @param i
	 * @param startDateStr
	 * @param barSize
	 * @param clientInterface
	 */
	public static void pullData(String startDateStr, String barSize,
			IBClientRequestExecutor clientInterface, int i, String... args) throws ParseException {
			for(; i < args.length;i++){
				final String symbol = args[i];
				final StoreHistoricalData sh = new StoreHistoricalData(symbol, clientInterface);
				if( ! sh.isValidSize(barSize) ) throw new IllegalArgumentException("Bar size unknown " + barSize );
				sh.setBarSize( barSize );
				
				final String optimalStartDate = getOptimalStartDate(startDateStr, barSize, symbol);

				clientInterface.reqHistoricalData(symbol, optimalStartDate, sh);
				clientInterface.waitForCompletion();
			}

	}
	/**
	 * Set a system property forceUpdate to avoid looking at current data and simply start updating
	 * from the given start date.
	 * 
	 * @param startDateStr
	 * @param barSize
	 * @param symbol
	 */
	protected static String getOptimalStartDate(String startDateStr, final String barSize, final String symbol) {
		String optimalStartDate = startDateStr;
		if(System.getProperty("forceUpdate") == null){
			final long firstRecord = CassandraDao.getInstance().findMostRecentDate(symbol, barSize);
			final DateFormat df = new SimpleDateFormat("yyyyMMdd");
			//The optimal start date is the first day for which we don't have data
			if(firstRecord != 0)
				optimalStartDate = df.format(new Date((firstRecord+(24*60*60))*1000));
		}
		return optimalStartDate;
	}

}
