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

import com.davehoag.ib.dataTypes.StockContract;
import com.davehoag.ib.util.HistoricalDateManipulation;
import com.ib.client.EClientSocket;
import com.ib.client.EWrapper;

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
	final HashMap<Integer, EWrapper> map = new HashMap<Integer, EWrapper>();
	/**
	 * Only want one thread sending the requests.
	 */
	IBClientRequestExecutor(final EClientSocket socket, final ResponseHandler rh) {
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
	 * Find a unique request id. They are reused and I can only have 31
	 * concurrent
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
		
		Logger.getLogger("RequestManager").log(Level.INFO, "Ending request " + reqId);
		int mask = 0xFFFFFFFF;
		mask = mask ^ reqId;
		requests = requests & mask;
		if (requests == 0) {
			Logger.getLogger("RequestManager").log(Level.INFO, "All submitted requests are complete");
			this.notifyAll();
		}
		map.remove(Integer.valueOf(reqId));
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
	public synchronized void execute(final Runnable r, final int seconds) {
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
	 *            "YYYYMMDD HH:MM:SS"
	 */
	public void reqHistoricalData(final String symbol, final String date, final EWrapper rh) throws ParseException {
		final StockContract stock = new StockContract(symbol);
		Logger.getLogger("HistoricalData").log(Level.INFO, "History data request(s) starting " + date + " " + symbol);
		reqHisData(date, stock, rh );
	}

	/**
	 * Run the scheduled tasks every 10 seconds. Ensure no more than 60 requests
	 * in a 10 minute period (a limit set by IB).
	 * @param date
	 * @param stock
	 * @throws ParseException
	 */
	protected void reqHisData(final String startingDate, final StockContract stock, final EWrapper rh)
			throws ParseException {

		// Get dates one week apart that will retrieve the historical data
		ArrayList<String> dates = HistoricalDateManipulation.getDates(startingDate);
		final int markerRequestId = pushRequest();
		boolean first = true;
		for (final String date : dates) {
			final Runnable r = new Runnable() {
				public void run() {
					final int reqId = pushRequest();
					pushResponseHandler(reqId, rh);
					Logger.getLogger("HistoricalData").log(Level.INFO,
							"Submitting request for historical data " + reqId + " " + date + " " + stock.m_symbol);

					client.reqHistoricalData(reqId, stock, date, IBConstants.dur1week, IBConstants.bar15min,
							IBConstants.showTrades, IBConstants.rthOnly, IBConstants.datesAsNumbers);

				}
			};
			if(first)
				execute(r,0);
			else //wait 10 seconds for the next request.
				execute(r, 10);
		}
		scheduleClosingRequest(markerRequestId);
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
	protected synchronized void pushResponseHandler(final int reqId, final EWrapper rh){
		map.put(Integer.valueOf(reqId), rh);
	}
	/**
	 * 
	 * @param reqId
	 * @return
	 */
	public EWrapper getResponseHandler(int reqId){
		return map.get(Integer.valueOf(reqId));
	}
}