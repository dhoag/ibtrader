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
 * Run multiple strategies concurrently.
 * Creates an instance of this class for each strategy to be run
 * @author dhoag
 *
 */
public class SimulateTrading extends RecursiveTask<TradingStrategy> {
	private static final long serialVersionUID = 4997373692668656258L;
	static ForkJoinPool forkJoinPool = new ForkJoinPool();
	String symb;
	String stratName;
	TradingStrategy strat;
	/**
	 * Take args : Simple:IBM MACD:QQQ
	 * @param args
	 */
	public static void main(String[] args) {
		LaunchTrading.simulateTrading = true;
		int i = 0;
		HistoricalDataSender.daysToBackTest = Integer.parseInt(args[i++]);
		initExecutionEnv();
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
		System.exit(0);
	}
	static IBClientRequestExecutor clientInterface ;
	static ResponseHandler rh; 
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
		//Safely building a whole stack for this simulation avoiding concurrency challenges
		//TODO test solutions ability to share response handler, m_client, portfolio and clientInterface
		 
		try{
			Strategy macd = (Strategy)Class.forName("com.davehoag.ib.strategies." + strategyName + "Strategy").newInstance();
			strat = new TradingStrategy(symbol, macd, clientInterface, rh.getPortfolio() );
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
