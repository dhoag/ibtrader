package com.davehoag.ib.tools;
import java.util.ArrayList;

import com.davehoag.ib.IBClientRequestExecutor;
import com.davehoag.ib.QuoteRouter;
import com.davehoag.ib.ResponseHandler;
import com.davehoag.ib.Strategy;
import com.davehoag.ib.util.HistoricalDataClient;
import com.davehoag.ib.util.HistoricalDataSender;
import com.davehoag.ib.util.ImmediateExecutor;

/**
 * Run multiple strategies concurrently. Creates an instance of this class for
 * each strategy to be run.
 * 
 * @author David Hoag
 * 
 */
public class SimulateTrading {
	private static final long serialVersionUID = 4997373692668656258L;

	/**
	 * Take args : Simple:IBM MACD:QQQ
	 * @param args
	 */
	public static void main(String[] args) {
		int i = 0;
		HistoricalDataSender.daysToBackTest = Integer.parseInt(args[i++]);

		executeStrategies(args, i);
		System.exit(0);
	}
	
	/**
	 * StratName:QQQ,SPY Will create one instance of StratName and registered
	 * with the QQQ & SPY quote sources.
	 * 
	 * @param args
	 * @param i
	 * @return
	 */
	protected static void executeStrategies(String[] args, int i) {
		IBClientRequestExecutor clientInterface = initSimulatedClient();

		for (; i < args.length; i++)
			try {
				int idx = args[i].indexOf(":");

				final String strategyName = args[i].substring(0, idx);
				final String symbolList = args[i].substring(idx + 1);
				
				Strategy strategy = (Strategy) Class.forName(
						"com.davehoag.ib.strategies." + strategyName + "Strategy").newInstance();
				for (String symbol : getSymbols(symbolList)) {
					final QuoteRouter quoteSource = clientInterface.getQuoteRouter(symbol);
					quoteSource.addStrategy(strategy);
				}
				clientInterface.requestQuotes();
				clientInterface.close();

			} catch (Throwable t) {
				t.printStackTrace();
				System.exit(-1);
			}
	}
	/**
	 * @return
	 */
	protected static IBClientRequestExecutor initSimulatedClient() {
		final ResponseHandler rh = new ResponseHandler();

		HistoricalDataClient m_client = new HistoricalDataClient(rh);
		rh.setExecutorService(new ImmediateExecutor());

		IBClientRequestExecutor clientInterface = new IBClientRequestExecutor(m_client, rh);
		clientInterface.setExcutor(new ImmediateExecutor());
		clientInterface.connect();
		clientInterface.initializePortfolio();
		rh.getPortfolio().setCash(100000.0);
		return clientInterface;
	}

	/**
	 * Parse the string of symbols separated by ',' into a collection of Strings
	 * 
	 * @param symbolList
	 * @return
	 */
	static ArrayList<String> getSymbols(final String symbolList) {
		final ArrayList<String> result = new ArrayList<String>();
		int idx = symbolList.indexOf(',');
		String list = symbolList;
		while(idx > 0){
			String symbol = list.substring(0, idx);
			result.add(symbol);
			list = list.substring(idx + 1);
			idx = list.indexOf(',');
		}
		result.add(list);
		return result;
	}
}
