package com.davehoag.ib.tools;

import java.text.NumberFormat;

import org.apache.logging.log4j.LogManager;

import com.davehoag.ib.IBClientRequestExecutor;
import com.davehoag.ib.QuoteRouter;
import com.davehoag.ib.ResponseHandler;
import com.davehoag.ib.strategies.SwapStrategy;
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
public class AnalyzeOptions {
	private static final long serialVersionUID = 4997373692668656258L;

	/**
	 * Take args : Simple:IBM MACD:QQQ
	 * @param args
	 */
	public static void main(String[] args) {
		NumberFormat nf = NumberFormat.getCurrencyInstance();
		int i = 0;
		HistoricalDataSender.daysToBackTest = 25;
		String strategies = "";
		int[] rangs = new int[] { 25, 40, 50, 60, 70, 80, 90, 100, 125, 150, 175, 200, 250, 300, 350, 400 };
		for (int val : rangs) {
			final IBClientRequestExecutor clientInterface = initSimulatedClient();
			SwapStrategy swap = new SwapStrategy();
			swap.init(String.valueOf(val));
			final QuoteRouter quoteSource = clientInterface.getQuoteRouter("QQQ");
			quoteSource.addStrategy(swap);
			final QuoteRouter altSource = clientInterface.getQuoteRouter("TIP");
			altSource.addStrategy(swap);
			clientInterface.requestQuotes();
			clientInterface.close();
			LogManager.getLogger("Swap").info(
					"Portfolio " + nf.format(clientInterface.getPortfolio().getNetValue()));
			clientInterface.getPortfolio().displayTradeStats("swap " + val);
		}
		System.exit(0);
	}
	
	/**
	 * @return
	 */
	protected static IBClientRequestExecutor initSimulatedClient() {
		final ResponseHandler rh = new ResponseHandler();

		HistoricalDataClient m_client = new HistoricalDataClient(rh);
		rh.setExecutorService(new ImmediateExecutor());

		IBClientRequestExecutor clientInterface = new IBClientRequestExecutor(m_client, rh);
		clientInterface.setExecutor(new ImmediateExecutor());
		clientInterface.connect();
		clientInterface.initializePortfolio();
		rh.getPortfolio().setCash(100000.0);
		return clientInterface;
	}
}
