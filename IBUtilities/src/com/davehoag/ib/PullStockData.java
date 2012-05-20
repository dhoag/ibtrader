package com.davehoag.ib;

import com.davehoag.ib.dataTypes.StockContract;
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
		IBClientRequestExecutor clientInterface = new IBClientRequestExecutor(m_client);
		clientInterface.connect();

        
	}

}
