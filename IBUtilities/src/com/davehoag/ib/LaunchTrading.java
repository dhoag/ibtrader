package com.davehoag.ib;

import com.davehoag.ib.dataTypes.Portfolio;
import com.davehoag.ib.util.HistoricalDataClient;
import com.ib.client.EClientSocket;

public class LaunchTrading {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String symbol = "SPY";
		ResponseHandler rh = new ResponseHandler();
		//EClientSocket  m_client = new EClientSocket( rh );
		//simulate trading
		HistoricalDataClient m_client = new HistoricalDataClient(rh);
		IBClientRequestExecutor clientInterface = new IBClientRequestExecutor(m_client, rh);
		clientInterface.connect();
		Portfolio port = new Portfolio();
		clientInterface.initializePortfolio( port );
		try{
			TradingStrategy strat = new TradingStrategy(symbol, clientInterface);
			MACDStrategy macd = new MACDStrategy();
			strat.setEntryStrategy(macd);
			strat.setExitStrategy(macd);
			strat.setPortfolio( port );
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
