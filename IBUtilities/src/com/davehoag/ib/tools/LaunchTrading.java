package com.davehoag.ib.tools;
import com.davehoag.ib.IBClientRequestExecutor;
import com.davehoag.ib.QuoteRouter;
import com.davehoag.ib.ResponseHandler;
import com.davehoag.ib.Strategy;
import com.ib.client.EClientSocket;


public class LaunchTrading {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		final String symbol = args[0];
		final String strategyName = args[1];
		ResponseHandler rh = new ResponseHandler();
		
		EClientSocket m_client = new EClientSocket(rh);
		
		IBClientRequestExecutor clientInterface = new IBClientRequestExecutor(m_client, rh);

		clientInterface.connect();
		clientInterface.initializePortfolio( );
		try{
			//repeat the next 3 lines for each symbol you want to trade
			Strategy macd = (Strategy)Class.forName("com.davehoag.ib.strategies." + strategyName + "Strategy").newInstance();
			QuoteRouter strat = clientInterface.getQuoteRouter(symbol);
			strat.addStrategy(macd);
			clientInterface.requestQuotes();
			
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
