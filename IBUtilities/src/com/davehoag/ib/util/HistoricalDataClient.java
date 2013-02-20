package com.davehoag.ib.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;

import com.davehoag.ib.CassandraDao;
import com.davehoag.ib.ResponseHandler;
import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.BarIterator;
import com.davehoag.ib.dataTypes.LimitOrder;
import com.ib.client.Contract;
import com.ib.client.EClientSocket;
import com.ib.client.Execution;
import com.ib.client.Order;

/**
 * A client that connects to our persistent store and simulates IB. There is one
 * and only one per simulated instance.
 * 
 * @author David Hoag
 */
public class HistoricalDataClient extends EClientSocket {
	protected boolean connected;
	protected ResponseHandler rh;
	protected HashMap<String, HistoricalDataSender> mktDataFeed = new HashMap<String, HistoricalDataSender>();

	public ExecutorService service = Executors.newFixedThreadPool(10);

	@Override
	public void cancelOrder(final int id) {
		LimitOrder lmtOrder = rh.getPortfolio().getOrder(id);
		if (lmtOrder == null) throw new IllegalStateException("[" + id + "] Canceling order we don't have:");
		final HistoricalDataSender sender = mktDataFeed.get(lmtOrder.getSymbol());

		sender.cancelOrder(lmtOrder);
		String status = "Cancelled";
		int filled = 0;
		int remaining = 0;
		double avgFillPrice = 0;
		int permId = id;
		int parentId = 0;
		double lastFillPrice = 0;
		int clientId = 0;
		String whyHeld = null;

		rh.orderStatus(id, status, filled, remaining, avgFillPrice, permId, parentId, lastFillPrice,
				clientId, whyHeld);

	}
	/**
	 * 
	 * @param anyWrapper
	 */
	public HistoricalDataClient(ResponseHandler anyWrapper) {
		super(anyWrapper);
		rh = anyWrapper;
	}
	@Override
    public void eConnect( String host, int port, int clientId) {
		connected = true;
    }
	@Override
    public boolean isConnected() { return connected; }
	/**
	 * Send historical data from the Cassandra store as "realtime" data. For now just sending 
	 * 5 second bars as that is RealTimeBar interval supported by IB
	 */
	@Override
	public void reqRealTimeBars(final int reqId, final Contract stock, final int barSize, final String barType, final boolean rthOnly){
		if(barSize != 5 ) throw new IllegalArgumentException("Only 5 second bars are supproted");

		HistoricalDataSender result;
		if (historicalDataEnd != 0) {
			result = HistoricalDataSender.initDataSender(reqId, stock, rh, this, historicalDataStart,
					historicalDataEnd);
		}
		else {
			result = HistoricalDataSender.initDataSender(reqId, stock, rh, this);
		}
		mktDataFeed.put(stock.m_symbol, result);
	}

	/**
	 * Send all of the data found in the market data feeds
	 */
	public void sendData() {
		Runnable r = new RealtimeBarSender();
		service.execute(r);
	}
	/**
	 * Send the stored historical data as realtime 5 second bars
	 * @author David Hoag
	 *
	 */
	class RealtimeBarSender implements Runnable{
		@Override
		public void run() {
			LogManager.getLogger("HistoricalData").info("Starting to send all data for client");
			long currentDay = 0;
			while(true){
				final boolean hasNext = allMarketDataFeedsHaveNext();
				if (hasNext) {
					final long time = sendRealtimeBar();
					currentDay = processPotentialNewDay(currentDay, time);
				} else {
					LogManager.getLogger("HistoricalData").info("Completed sending all data for client");
					break;
				}
			}
		}

		/**
		 * @param currentDay
		 * @param time
		 * @return
		 */
		protected long processPotentialNewDay(long currentDay, final long time) {
			if(currentDay == 0) currentDay = HistoricalDateManipulation.getOpenTime(time); 
			else {
				final long newDay = HistoricalDateManipulation.getOpenTime(time);
				if(currentDay != newDay ){
					//reset as if the trading process was reset
					rh.reset();
					currentDay = newDay;
				}
			}
			return currentDay;
		}

		/**
		 * @return
		 */
		protected boolean allMarketDataFeedsHaveNext() {
			boolean hasNext = true;
			for(HistoricalDataSender sender: mktDataFeed.values()){
				hasNext = hasNext & sender.hasNext();
			}
			return hasNext;
		}

		/**
		 * @param time
		 * @return
		 */
		protected long sendRealtimeBar() {
			long time = 0;
			try {
			for (HistoricalDataSender sender : mktDataFeed.values()) {
				long barTime = sender.sendBar();
				if(time == 0) time = barTime;
				else
					if(time != barTime){
						throw new IllegalStateException("Market data feeds are not synchronized! " + HistoricalDateManipulation.getDateAsStr(time) + " != " + HistoricalDateManipulation.getDateAsStr(barTime));
					}
			}
			} catch (Exception ex) {
				ex.printStackTrace(System.err);
				System.exit(-1);
			}
			return time;
		}	
	}
	/**
	 * Crippled implementation. Only/always sends the past 365 records
	 */
	@Override
    public void reqHistoricalData( final int tickerId, final Contract contract,
            final String endDateTime, final String durationStr,
            final String barSizeSetting, final String whatToShow,
            final int useRTH, final int formatDate) {
		
		Runnable r = new Runnable() { @Override
		public void run() {
			try { 
				final int daysToGoBack = 365;
				final long endTime = HistoricalDateManipulation.getTime(endDateTime);
				final BarIterator bars = CassandraDao.getInstance().getData(contract.m_symbol, endTime - daysToGoBack*24*60*60, endTime, "bar1day");
				LogManager.getLogger("HistoricalData").info("Starting to send historical data to client " + contract.m_symbol);
				SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
				//for bar1day it is not sent as seconds, I store it as seconds so need to convert
				while(bars.hasNext()){
					final Bar aBar = bars.next();
					String date = df.format(aBar.getTime());
					
					rh.historicalData(tickerId, 
							date, aBar.open, 
							aBar.high, aBar.low, 
							aBar.close, (int)aBar.volume, 
							aBar.tradeCount, aBar.wap, 
							aBar.hasGaps);
				}
				//send marker indicating data is complete
				rh.historicalData(tickerId, "", -1, -1, 0, 0, 0, 0, 0, false);
				LogManager.getLogger("HistoricalData").info("Completed historical data request " + contract.m_symbol);
			}
			catch(ParseException pe){ pe.printStackTrace(); }
		}
		};
		service.execute(r);
    }
	@Override
	public void eDisconnect() {

		try {
			service.shutdown();
			while (!service.awaitTermination(20, TimeUnit.SECONDS))
				System.out.println("Waiting termination");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		connected = false;
	}
	@Override
	public String TwsConnectionTime(){ return "MockClientTime " + System.currentTimeMillis(); }
	@Override
	public int serverVersion(){ return 100; }
	@Override
	public void placeOrder(final int id, final Contract contract, final Order order){
		final HistoricalDataSender sender = mktDataFeed.get(contract.m_symbol);
		if( sender!=null ){
			if( sender.fillOrBookOrder(id, contract, order) ){
				fillOrder(id, contract, order);
			}
		}
		else{ //Just fill it - shouldn't ever be this case
			fillOrder(id, contract, order);
		}
	}
	/**
	 * Noop for now, send realtime data later
	 */
	@Override
	public void reqMktData(int tickReqId, Contract stock, String genericTypes, boolean snapshot){}
	/**
	 * Create an execution and send it 
	 * @param id
	 * @param contract
	 * @param order
	 */
	public void fillOrder(final int id, final Contract contract, final Order order) {
		rh.execDetails(id, contract, getExecution(id,contract,order));
	}
	/**
	 * @param id
	 * @param contract
	 * @param order
	 */
	protected Execution getExecution(final int id, final Contract contract, final Order order) {
		final Execution execution = new Execution();
		execution.m_side = order.m_action;
		execution.m_orderId = id;
		execution.m_price = order.m_lmtPrice;
		execution.m_shares = order.m_totalQuantity;
		return execution;
	}
	@Override
	public void reqAccountUpdates(final boolean keepGetting, final String accountName){
		
	}

	long historicalDataStart;
	long historicalDataEnd;

	public void setSimulationRange(long startTime, long endTime) {
		historicalDataEnd = endTime;
		historicalDataStart = startTime;

	}
}
