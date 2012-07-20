package com.davehoag.ib.tools;
import com.davehoag.ib.IBClientRequestExecutor;
import com.davehoag.ib.MACDStrategy;
import com.davehoag.ib.ResponseHandler;
import com.davehoag.ib.TradingStrategy;
import com.davehoag.ib.util.HistoricalDataClient;
import com.davehoag.ib.util.HistoricalDataSender;
import com.davehoag.ib.util.ImmediateExecutor;
import com.ib.client.EClientSocket;


public class LaunchTrading {
	static boolean simulateTrading = true;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		final String symbol = args[0];
		
		ResponseHandler rh = new ResponseHandler();
		
		EClientSocket  m_client;
		if( simulateTrading ){
			HistoricalDataSender.daysToBackTest = Integer.parseInt(args[1]);
			m_client = new HistoricalDataClient(rh);
			rh.setExecutorService(new ImmediateExecutor());
		}
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
