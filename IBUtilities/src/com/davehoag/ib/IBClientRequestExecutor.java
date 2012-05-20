package com.davehoag.ib;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.davehoag.ib.dataTypes.StockContract;
import com.davehoag.ib.util.HistoricalDateManipulation;
import com.ib.client.EClientSocket;

/**
 * Control all IB client requests
 * 
 * @author dhoag
 * 
 */
public class IBClientRequestExecutor  {

	final EClientSocket client;
	final Queue<Runnable> tasks = new ArrayDeque<Runnable>();
	final ExecutorService executor;
	Runnable active;
	int requests = 0;

	/**
	 * Only want one thread sending the requests.
	 */
	IBClientRequestExecutor(final EClientSocket socket) {
		client = socket;
		this.executor = Executors.newSingleThreadExecutor();
	}
	/**
	 * Connect to the default host/ports through the TWS client
	 */
	public void connect() {
		client.eConnect(IBConstants.host, IBConstants.port,
				IBConstants.clientId);
		if (client.isConnected()) {
			System.out.println("Connected to Tws server version "
					+ client.serverVersion() + " at "
					+ client.TwsConnectionTime());

		} else {
			System.out.println("Failed to connect " + IBConstants.host + " "
					+ IBConstants.port);
			System.exit(1);
		}
	}

	/**
	 * Find a unique request id. They are reused and I can only have 31
	 * concurrent
	 * @return int An id that should be used to mark a request has been fulfilled
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
	 * @param reqId The ID of the request that has completed
	 */
	final synchronized void endRequest(final int reqId) {
		int mask = 0xFFFFFFFF;
		mask = mask ^ reqId;
		requests = requests & mask;
		if (requests == 0) {
			this.notifyAll();
		}

	}

	/**
	 * Wait until all requests have been completed
	 */
	public synchronized void waitForCompletion() {
		while(requests != 0)
			try{
				wait();
			}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * Run the scheduled tasks every 10 seconds. Ensure no more than 60 requests
	 * in a 10 minute period (a limit set by IB)
	 * @param seconds The amount of time to wait *after* the runnable is executed before moving on
	 */
	public synchronized void execute(final Runnable r, final int seconds) {
		tasks.offer(new Runnable() {
			public void run() {
				try {
					r.run();
					if (seconds > 0) {
						// Send the request then wait for 10 seconds
						synchronized (this) {
							try {
								wait(1000 * seconds);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				} finally {
					scheduleNext();
				}
			}
		});
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
			executor.execute(active);
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
	public void reqHistoricalData(final String symbol, final String date)
			throws ParseException {
		final StockContract stock = new StockContract(symbol);
		final int reqId = pushRequest();
		reqHisData(date, stock, reqId);
	}

	/**
	 * 
	 * @param date
	 * @param stock
	 * @param reqId
	 * @throws ParseException
	 */
	protected void reqHisData(final String startingDate,
			final StockContract stock, final int reqId) throws ParseException {

		// Get dates one week apart that will retrieve the historical data
		ArrayList<String> dates = HistoricalDateManipulation
				.getDates(startingDate);

		for (final String date : dates) {
			final Runnable r = new Runnable() {
				public void run() {
					System.out.println(date);
					/*
					 * client.reqHistoricalData( reqId , stock, date, IBConstants.dur1week,
					 * IBConstants.bar15min, IBConstants.showTrades, IBConstants.rthOnly,
					 * IBConstants.datesAsStrings );
					 */
				}
			};
			this.execute(r, 10);
		}
		

	}
}