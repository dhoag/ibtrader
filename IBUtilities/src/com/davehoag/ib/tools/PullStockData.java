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
 * @author dhoag
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

		ResponseHandler rh = new ResponseHandler();
		EClientSocket  m_client = new EClientSocket( rh );
		IBClientRequestExecutor clientInterface = new IBClientRequestExecutor(m_client, rh);
		clientInterface.connect();
		try {
			for(; i < args.length;i++){
				final String symbol = args[i];
				StoreHistoricalData sh = new StoreHistoricalData(symbol, clientInterface);
				if( ! sh.isValidSize(barSize) ) throw new IllegalArgumentException("Bar size unknown " + barSize );
				sh.setBarSize( barSize );
				
				final String optimalStartDate = getOptimalStartDate(startDateStr, barSize, symbol);

				clientInterface.reqHistoricalData(symbol, optimalStartDate, sh);
			}
			clientInterface.waitForCompletion();
		} catch (ParseException e) {
			LoggerFactory.getLogger("PullStockData").error( "Parse Exception!! " + startDateStr, e);
		}
		finally { 
			clientInterface.close();
		}
        System.exit(0);
	}

	/**
	 * @param startDateStr
	 * @param barSize
	 * @param symbol
	 */
	protected static String getOptimalStartDate(String startDateStr, String barSize, final String symbol) {
		String optimalStartDate = startDateStr;
		if(System.getProperty("forceUpdate") == null){
			long firstRecord = CassandraDao.getInstance().findMostRecentDate(symbol, barSize);
			DateFormat df = new SimpleDateFormat("yyyyMMdd");
			//The optimal start date is the first day for which we don't have data
			if(firstRecord != 0)
				optimalStartDate = df.format(new Date((firstRecord+(24*60*60))*1000));
		}
		return optimalStartDate;
	}

}
