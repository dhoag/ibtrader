package com.davehoag.ib.tools;

import java.text.ParseException;
import org.slf4j.LoggerFactory;

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
				clientInterface.reqHistoricalData(symbol, startDateStr, sh);
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

}
