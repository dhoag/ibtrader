package com.davehoag.ib.tools;
import java.util.ArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

import com.davehoag.ib.IBClientRequestExecutor;
import com.davehoag.ib.ResponseHandler;
import com.davehoag.ib.Strategy;
import com.davehoag.ib.TradingStrategy;
import com.davehoag.ib.util.HistoricalDataClient;
import com.davehoag.ib.util.HistoricalDataSender;
import com.davehoag.ib.util.ImmediateExecutor;

/**
 * Run multiple strategies concurrently.
 * Creates an instance of this class for each strategy to be run
 * @author dhoag
 *
 */
public class SimulateTrading implements Runnable {
	String symb;
	String stratName;
	/**
	 * Take args : Simple:IBM MACD:QQQ
	 * @param args
	 */
	public static void main(String[] args) {
		LaunchTrading.simulateTrading = true;
		int i = 0;
		HistoricalDataSender.daysToBackTest = Integer.parseInt(args[i++]);
		ForkJoinPool th = new ForkJoinPool();
		ArrayList<ForkJoinTask> simulations = new ArrayList<ForkJoinTask>();
		for(; i < args.length; i++){
			int idx = args[i].indexOf(":");
			
			final String strategyName = args[i].substring(0, idx);
			final String symbol  = args[i].substring(idx+1);
			SimulateTrading runnable = new SimulateTrading();
			runnable.setTestParameters(symbol, strategyName);
			ForkJoinTask<?> task = th.submit(runnable);
			simulations.add(task);
		}
		
		for(ForkJoinTask t : simulations){
			t.join();
		}
        System.exit(0);
	}

	public void setTestParameters(String sym, String strat){
		symb = sym; stratName = strat;
	}
	public void run(){
		testStrategy(symb, stratName);
	}
	/**
	 * @param symbol
	 * @param strategyName
	 */
	protected  void testStrategy(final String symbol, final String strategyName) {
		//Safely building a whole stack for this simulation avoiding concurrency challenges
		//TODO test solutions ability to share response handler, m_client, portfolio and clientInterface
		 
		ResponseHandler rh = new ResponseHandler();
		HistoricalDataClient m_client = new HistoricalDataClient(rh);
		rh.setExecutorService(new ImmediateExecutor());
		
		IBClientRequestExecutor clientInterface = new IBClientRequestExecutor(m_client, rh);
		clientInterface.setExcutor(new ImmediateExecutor());
		clientInterface.connect();
		clientInterface.initializePortfolio( );
		try{
			Strategy macd = (Strategy)Class.forName("com.davehoag.ib.strategies." + strategyName + "Strategy").newInstance();
			TradingStrategy strat = new TradingStrategy(symbol, macd, clientInterface, rh.getPortfolio() );
			clientInterface.reqRealTimeBars(symbol, strat);
			clientInterface.waitForCompletion();
			//TODO put strat in a "future" that can be used to evaluate each ones performance?
			strat.displayTradeStats();
		}
		catch(Throwable t){
			t.printStackTrace();
			System.exit(1);
		}
		finally { 
			clientInterface.close();
		}
	}

}
