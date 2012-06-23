package com.davehoag.ib;

import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

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
		String symbol = args[0];
		String startDateStr = args[1];
		
		ResponseHandler rh = new ResponseHandler();
		EClientSocket  m_client = new EClientSocket( rh );
		IBClientRequestExecutor clientInterface = new IBClientRequestExecutor(m_client, rh);
		clientInterface.connect();
		try {
			StoreHistoricalData sh = new StoreHistoricalData(symbol, clientInterface);
			sh.setBarSize("bar1day");
			clientInterface.reqHistoricalData(symbol, startDateStr, sh);
			clientInterface.waitForCompletion();
		} catch (ParseException e) {
			Logger.getLogger("PullStockData").log(Level.SEVERE, "Parse Exception!! " + startDateStr, e);
		}
		finally { 
			clientInterface.close();
		}
        System.exit(0);
	}

}
