package com.davehoag.ib.util;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.LoggerFactory;

import com.davehoag.ib.ResponseHandler;
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
		Runnable r = new Runnable() {
			@Override
			public void run() {
				LoggerFactory.getLogger("HistoricalData").info("Starting to send all data for client");
				while(true){
					boolean hasNext = true;
					for(HistoricalDataSender sender: mktDataFeed.values()){
						hasNext = hasNext & sender.hasNext();
					}
					if (hasNext) {
						try {
						for (HistoricalDataSender sender : mktDataFeed.values()) {
							sender.sendBar();
						}
						} catch (Exception ex) {
							ex.printStackTrace(System.err);
							System.exit(-1);
						}
					} else {
						LoggerFactory.getLogger("HistoricalData").info("Completed sending all data for client");
						break;
					}
				}
			}
		};
		service.execute(r);
	}
	@Override
    public void reqHistoricalData( final int tickerId, final Contract contract,
            final String endDateTime, final String durationStr,
            final String barSizeSetting, final String whatToShow,
            final int useRTH, final int formatDate) {
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
		//Do nothing
	}

	long historicalDataStart;
	long historicalDataEnd;

	public void setSimulationRange(long startTime, long endTime) {
		historicalDataEnd = endTime;
		historicalDataStart = startTime;

	}
}
