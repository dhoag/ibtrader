package com.davehoag.ib;

import java.text.ParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.davehoag.ib.dataTypes.Portfolio;
import com.davehoag.ib.dataTypes.StockContract;
import com.davehoag.ib.util.HistoricalDateManipulation;
import com.ib.client.EClientSocket;
import com.ib.client.EWrapper;
import com.ib.client.Order;

/**
 * Control all IB client requests
 * 
 * @author dhoag
 * 
 */
public class IBClientRequestExecutor {

	final EClientSocket client;
	final Queue<Runnable> tasks = new ArrayDeque<Runnable>();
	final ExecutorService executor;
	Runnable active;
	int requests = 0;
	final HashMap<Integer, ResponseHandlerDelegate> map = new HashMap<Integer, ResponseHandlerDelegate>();
	/**
	 * Only want one thread sending the requests.
	 */
	public IBClientRequestExecutor(final EClientSocket socket, final ResponseHandler rh) {
		client = socket;
		this.executor = Executors.newSingleThreadExecutor();
		rh.setRequestor(this);
	}

	/**
	 * Got a disconnect from the TWS and I'm responding as best I can
	 */
	protected void forcedClose() {
		executor.shutdownNow();
		Logger.getLogger("RequestManager").log(Level.SEVERE, "Forced Exit");
		client.eDisconnect();
		requests = 0;
	}

	/**
	 * Gracefully exit
	 */
	public void close() {
		Logger.getLogger("RequestManager").log(Level.INFO, "Shutting down");
		executor.shutdown();
		client.eDisconnect();
	}

	/**
	 * Connect to the default host/ports through the TWS client. It doesn't
	 * actually validate the clientId until the first request is made
	 */
	public void connect() {
		client.eConnect(IBConstants.host, IBConstants.port, IBConstants.clientId);
		if (client.isConnected()) {
			System.out.println("Connected to Tws server version " + client.serverVersion() + " at "
					+ client.TwsConnectionTime());

		} else {
			Logger.getLogger("RequestManager").log(Level.SEVERE,
					"Failed to connect " + IBConstants.host + " " + IBConstants.port);
			System.exit(1);
		}
	}
	/**
	 * 
	 * @param contract
	 * @param qty
	 * @param price
	 * @param rh
	 */
	public int executeBuyOrder(final String symbol, final int qty, final double price, final ResponseHandlerDelegate rh){
		final int id = pushRequest();
		pushResponseHandler(id, rh);
		Order order = new Order();
		order.m_clientId = IBConstants.clientId;
		order.m_orderType = "LMT";
		order.m_action = "BUY";
		order.m_lmtPrice = price;
		order.m_totalQuantity = qty;

		order.m_clientId = IBConstants.clientId;
		order.m_orderId = id;
		order.m_permId = (int)(System.currentTimeMillis() / 1000);
		//TODO  sure if IOC and allOrNone are not compatible
		order.m_orderType = "IOC";
		order.m_allOrNone = true;
		order.m_transmit = true;
		final StockContract contract = new StockContract(symbol);
		client.placeOrder(id, contract, order);
		return order.m_orderId;
	}
	/**
	 * Can only be used to sell a long position, I think I need to use 
	 * action as SSHORT for a short position
	 * @param contract
	 * @param qty
	 * @param price
	 * @param rh
	 * @return
	 */
	public int executeSellOrder(final String symbol, final int qty, final double price, final ResponseHandlerDelegate rh){
		final int id = pushRequest();
		pushResponseHandler(id, rh);
		Order order = new Order();
		order.m_clientId = id;
		order.m_orderType = "LMT";
		order.m_action = "SELL";
		order.m_lmtPrice = price;
		order.m_totalQuantity = qty;

		order.m_clientId = IBConstants.clientId;
		order.m_orderId = id;
		order.m_permId = (int)(System.currentTimeMillis() / 1000);
		//TODO  sure if IOC and allOrNone are not compatible
		order.m_orderType = "IOC";
		order.m_transmit = true;
		
		final StockContract contract = new StockContract(symbol);
		client.placeOrder(id, contract, order);
		return order.m_orderId;
	}
	/**
	 * Populate the portfolio with existing positions
	 * @param port
	 */
	final void initializePortfolio( final Portfolio port ){
		//TODO actually pull in portfolio data
	}
	/**
	 * Find a unique request id. They are reused and I can only have 31
	 * concurrent. I reuse the IDs in this strategy, but I'm unsure if this
	 * works for executions. TODO test it with test connection
	 * 
	 * @return int An id that should be used to mark a request has been
	 *         fulfilled
	 */
	final synchronized int pushRequest() {
		int shifts = 0;

		int val = 1;
		// find the first bit that is currently a zero
		for (; ((val & requests) != 0 && shifts < 31); shifts++) {
			val = val << 1;
		}
		requests = val | requests;
		return val;
	}

	/**
	 * Mark the request id as available for reuse
	 * 
	 * @param reqId
	 *            The ID of the request that has completed
	 */
	final synchronized void endRequest(final int reqId) {
		final Integer id = Integer.valueOf(reqId);
		final ResponseHandlerDelegate rd = map.get(id);
		if(rd != null) {
			rd.log(Level.INFO, "[" + reqId + "] ending executionTime: " + (System.currentTimeMillis() - rd.getStartTime()));
		}
		else
			Logger.getLogger("RequestManager").log(Level.INFO, "[" + reqId + "] Ending request " );
		
		int mask = 0xFFFFFFFF;
		mask = mask ^ reqId;
		requests = requests & mask;
		
		if (requests == 0) {
			if(rd != null) {
				rd.log(Level.INFO, "All submitted requests are complete");
			}
			else
				Logger.getLogger("RequestManager").log(Level.INFO, "All submitted requests are complete");
			this.notifyAll();
		}
		map.remove(id);
	}

	/**
	 * Wait until all requests have been completed.
	 */
	public synchronized void waitForCompletion() {
		while (requests != 0 || !tasks.isEmpty() || active != null)
			try {
				Logger.getLogger("RequestManager").log(Level.INFO, "Waiting " + requests + " " + tasks.isEmpty());
				wait();
			} catch (InterruptedException e) {
				Logger.getLogger("RequestManager").log(Level.SEVERE, "Interrupted!!", e);
			}
	}
	/**
	 * Wait before - no need to penalize the request after the historical data request.
	 * @param seconds
	 *            The amount of time to wait *before* the runnable is executed
	 *            
	 */
	protected synchronized void execute(final Runnable r, final int seconds) {
		Logger.getLogger("RequestManager").log(Level.FINEST, "Enqueing request");
		
		boolean result = tasks.offer(new Runnable() {
			public void run() {
				try {
					if (seconds > 0) {
						// Send the request then wait for 10 seconds
						synchronized (this) {
							try {
								wait(1000 * seconds);
							} catch (InterruptedException e) {
								Logger.getLogger("RequestManager").log(Level.SEVERE, "Interrupted!!", e);
							}
						}
					}
					r.run();
				} finally {
					scheduleNext();
				}
			}
		});
		if (! result){
			throw new RuntimeException("Never expected my runnable to not enque!");
		}
		// Active is null on the first request so automatically kick it off
		if (active == null) {
			scheduleNext();
		}
	}

	/**
	 * Check the queue for work and schedule it
	 */
	protected synchronized void scheduleNext() {
		if ((active = tasks.poll()) != null) {
			//tell the thread pool to actually run the active item
			executor.execute(active);
		}
		else {
			Logger.getLogger("RequestManager").log(Level.INFO, "All queued requests are complete");
			this.notifyAll();
		}
	}

	/**
	 * Find historical data for the provided symbol
	 * 
	 * @param symbol
	 *            Simple stock symbol
	 * @param date
	 *            First day for which we want historical data Format like
	 *            "YYYYMMDD"
	 */
	public void reqHistoricalData(final String symbol, final String date, final ResponseHandlerDelegate rh) throws ParseException {
		final StockContract stock = new StockContract(symbol);
		reqHisData(date, stock, rh );
		rh.log(Level.INFO, "History data request(s) starting " + date + " " + symbol);
	}
	/**
	 * Run the scheduled tasks every 10 seconds. Ensure no more than 60 requests
	 * in a 10 minute period (a limit set by IB).
	 * @param date
	 * @param stock
	 * @throws ParseException
	 */
	protected void reqHisData(final String startingDate, final StockContract stock, final ResponseHandlerDelegate rh)
			throws ParseException {

		// Get dates one hour apart that will retrieve the historical data
		ArrayList<String> dates = HistoricalDateManipulation.getDatesBrokenIntoHours(startingDate);
		final int markerRequestId = pushRequest();
		boolean first = true;
		for (final String date : dates) {
			final Runnable r = new Runnable() {
				public void run() {
					final int reqId = pushRequest();
					pushResponseHandler(reqId, rh);
					rh.log(Level.INFO,
							"Submitting request for historical data " + reqId + " " + date + " " + stock.m_symbol);

					client.reqHistoricalData(reqId, stock, date, IBConstants.dur1hour, IBConstants.bar5sec,
							IBConstants.showTrades, IBConstants.rthOnly, IBConstants.datesAsNumbers);

				}
			};
			if(first) {
				execute(r,0);
				first = false;
			}
			else //wait 10 seconds for the next request.
				execute(r, 11);
		}
		scheduleClosingRequest(markerRequestId);
	}
	/**
	 * Get 5 second bars and route to the request.
	 * @param symbol
	 * @param rh
	 */
	public void reqRealTimeBars(final String symbol, final ResponseHandlerDelegate rh){
		Logger.getLogger("MarketData").log(Level.INFO, "Requesting realtime bars for " + symbol);
		final Runnable r = new Runnable() {
			public void run() {
				StockContract stock = new StockContract(symbol);
				final int reqId = pushRequest();
				rh.setReqId(reqId);
				pushResponseHandler(reqId, rh);
				Logger.getLogger("MarketData").log(Level.INFO,
						"Submitting request for market data " + reqId + " " + stock.m_symbol);
				//true means RTH only
				//5 is the only legal value for realTimeBars - resulting in 5 second bars
				client.reqRealTimeBars(reqId, stock, 5, IBConstants.showTrades, true);

			}
		};
		execute(r, 0);
	}
	/**
	 * Since the "Push of a request occurs within a thread asynchronously started this means
	 * that this current thread could see zero work in the "tasks" queue and no request
	 * thus causing the "waitForCompletion" method to erroneously not block. So, this nows 
	 * requires any request to push a "request" on the queue in the current thread and then schedule
	 * a runnable to release that request after all of the other work has completed.
	 * 
	 * @param reqId
	 */
	protected void scheduleClosingRequest(final int reqId){
		final Runnable r = new Runnable() {
			public void run() {
				endRequest(reqId);
			};	
		};
		execute(r,0);
	}
	/**
	 * Associate an implementation of AbstractResponseHandler with the request ID
	 * @param reqId
	 * @param rh
	 */
	public synchronized void pushResponseHandler(final int reqId, final ResponseHandlerDelegate rh){
		rh.setStartTime(System.currentTimeMillis());
		
		map.put(Integer.valueOf(reqId), rh);
	}
	/**
	 * 
	 * @param reqId
	 * @return
	 */
	public ResponseHandlerDelegate getResponseHandler(int reqId){
		return map.get(Integer.valueOf(reqId));
	}
}