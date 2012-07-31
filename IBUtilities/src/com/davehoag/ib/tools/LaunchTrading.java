package com.davehoag.ib.tools;
import com.davehoag.ib.IBClientRequestExecutor;
import com.davehoag.ib.ResponseHandler;
import com.davehoag.ib.Strategy;
import com.davehoag.ib.TradingStrategy;
import com.davehoag.ib.strategies.MACDStrategy;
import com.davehoag.ib.util.HistoricalDataClient;
import com.davehoag.ib.util.HistoricalDataSender;
import com.davehoag.ib.util.ImmediateExecutor;
import com.ib.client.EClientSocket;


public class LaunchTrading {
	public static boolean simulateTrading = false;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		final String symbol = args[0];
		final String strategyName = args[1];
		simulateTrading = args.length > 2;
		ResponseHandler rh = new ResponseHandler();
		
		EClientSocket  m_client;
		if( simulateTrading ){
			HistoricalDataSender.daysToBackTest = Integer.parseInt(args[2]);
			m_client = new HistoricalDataClient(rh);
			rh.setExecutorService(new ImmediateExecutor());
		}
		else  m_client = new EClientSocket( rh );
		
		IBClientRequestExecutor clientInterface = new IBClientRequestExecutor(m_client, rh);
		if( simulateTrading ){
			clientInterface.setExcutor(new ImmediateExecutor());
		}
		clientInterface.connect();
		clientInterface.initializePortfolio( );
		try{
			//repeat the next 3 lines for each symbol you want to trade
			Strategy macd = (Strategy)Class.forName("com.davehoag.ib.strategies." + strategyName + "Strategy").newInstance();
			TradingStrategy strat = new TradingStrategy(symbol, macd, clientInterface, rh.getPortfolio() );
			clientInterface.reqRealTimeBars(symbol, strat);
			
			clientInterface.waitForCompletion();
			//probably want to enumerate for each strat you set above
			strat.displayTradeStats();
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
