package com.davehoag.ib.tools;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

import com.davehoag.ib.IBClientRequestExecutor;
import com.davehoag.ib.ResponseHandler;
import com.davehoag.ib.Strategy;
import com.davehoag.ib.TradingStrategy;
import com.davehoag.ib.util.HistoricalDataClient;
import com.davehoag.ib.util.HistoricalDataSender;
import com.davehoag.ib.util.ImmediateExecutor;

/**
 * Run multiple strategies concurrently. Creates an instance of this class for
 * each strategy to be run
 * 
 * @author David Hoag
 * 
 */
public class SimulateTrading extends RecursiveTask<TradingStrategy> {
	private static final long serialVersionUID = 4997373692668656258L;
	static ForkJoinPool forkJoinPool = new ForkJoinPool();
	static IBClientRequestExecutor clientInterface ;
	static ResponseHandler rh; 

	String symb;
	String stratName;

	TradingStrategy strat;

	/**
	 * Share the execution environment as if there is only one marketdata source for all strategies
	 */
	static void initExecutionEnv(){
		rh = new ResponseHandler();
		HistoricalDataClient m_client = new HistoricalDataClient(rh);
		rh.setExecutorService(new ImmediateExecutor());
		
		clientInterface = new IBClientRequestExecutor(m_client, rh);
		clientInterface.setExcutor(new ImmediateExecutor());
		clientInterface.connect();
		clientInterface.initializePortfolio( );
		rh.getPortfolio().setCash(100000.0);
	}
	/**
	 * Take args : Simple:IBM MACD:QQQ
	 * @param args
	 */
	public static void main(String[] args) {
		LaunchTrading.simulateTrading = true;
		int i = 0;
		HistoricalDataSender.daysToBackTest = Integer.parseInt(args[i++]);
		initExecutionEnv();
		simpleApproach(args, i);
		System.exit(0);
	}
	/**
	 * @param args
	 * @param i
	 * @return
	 */
	protected static void simpleApproach(String[] args, int i) {
		for (; i < args.length; i++)
			try {
			int idx = args[i].indexOf(":");

			final String strategyName = args[i].substring(0, idx);
			final String symbol = args[i].substring(idx + 1);
				Strategy strategy = (Strategy) Class.forName(
						"com.davehoag.ib.strategies." + strategyName + "Strategy").newInstance();
				TradingStrategy strat = TradingStrategy.getQuoteRouter(symbol, clientInterface, rh);
				strat.addStrategy(strategy);

			} catch (Throwable t) {
				t.printStackTrace();
				System.exit(-1);
		}
		TradingStrategy.requestData();
		clientInterface.close();
	}
	/**
	 * @param args
	 * @param i
	 */
	protected static void forkJoinApproach(String[] args, int i) {
		ArrayList<ForkJoinTask<TradingStrategy>> simulations = new ArrayList<ForkJoinTask<TradingStrategy>>();
		for(; i < args.length; i++){
			int idx = args[i].indexOf(":");
			
			final String strategyName = args[i].substring(0, idx);
			final String symbol  = args[i].substring(idx+1);
			SimulateTrading runnable = new SimulateTrading();
			runnable.setTestParameters(symbol, strategyName);
			ForkJoinTask<TradingStrategy> task = forkJoinPool.submit(runnable);
			simulations.add(task);
		}
		
		for(ForkJoinTask<TradingStrategy> t : simulations){
			t.join();
		}
		for(ForkJoinTask<TradingStrategy> t : simulations){
			try {
				t.get().displayTradeStats();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}
	}
	public void setTestParameters(String sym, String strat){
		symb = sym; stratName = strat;
	}
	
	@Override
	public TradingStrategy compute(){
		testStrategy(symb, stratName);
		return strat;
	}
	/**
	 * @param symbol
	 * @param strategyName
	 */
	protected  void testStrategy(final String symbol, final String strategyName) {
		 
		try{
			Strategy strategy = (Strategy)Class.forName("com.davehoag.ib.strategies." + strategyName + "Strategy").newInstance();
			strat = new TradingStrategy(symbol, clientInterface, rh.getPortfolio());
			strat.addStrategy(strategy);
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
	}

}
