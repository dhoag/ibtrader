package com.davehoag.ib;


import com.davehoag.ib.dataTypes.Portfolio;
import com.davehoag.ib.util.HistoricalDataClient;
import com.ib.client.EClientSocket;


public class LaunchTrading {
static boolean simulateTrading = true;
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		final String symbol = "SPY";
		ResponseHandler rh = new ResponseHandler();
		
		EClientSocket  m_client;
		if( simulateTrading ) m_client = new HistoricalDataClient(rh);
		else  m_client = new EClientSocket( rh );
		
		IBClientRequestExecutor clientInterface = new IBClientRequestExecutor(m_client, rh);
		clientInterface.connect();
		clientInterface.initializePortfolio( );
		try{
			MACDStrategy macd = new MACDStrategy();
			TradingStrategy strat = new TradingStrategy(symbol, macd, clientInterface, rh.getPortfolio() );
			clientInterface.reqRealTimeBars(symbol, strat);
			clientInterface.waitForCompletion();
		}
		catch(Throwable t){
			t.printStackTrace();
			System.exit(1);
		}
		finally { 
			clientInterface.close();
		}
        System.exit(0);

	}

}
