package com.davehoag.ib;

import java.text.ParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import org.apache.logging.log4j.LogManager;

import com.davehoag.ib.dataTypes.FutureContract;
import com.davehoag.ib.dataTypes.LimitOrder;
import com.davehoag.ib.dataTypes.Portfolio;
import com.davehoag.ib.dataTypes.StockContract;
import com.davehoag.ib.util.HistoricalDataClient;
import com.davehoag.ib.util.ImmediateExecutor;
import com.ib.client.Contract;
import com.ib.client.EClientSocket;
import com.ib.client.Order;
import com.ib.client.TagValue;
import com.ib.contracts.FutContract;

/**
 * Control all IB client requests
 * 
 * @author David Hoag
 */
public class IBClientRequestExecutor {

	final EClientSocket client;
	final Queue<Runnable> tasks = new ArrayDeque<Runnable>();
	Executor executor;
	Runnable active;
	int requests = 0;
	final HashMap<Integer, ResponseHandlerDelegate> map = new HashMap<Integer, ResponseHandlerDelegate>();
	final ResponseHandler responseHandler;
	int orderIdCounter;
	boolean connectionConfirmed = false;
	/**
	 * Helper method to bootstrap conenction to the client.
	 * @return
	 */
	public static IBClientRequestExecutor connectToAPI(){
		ResponseHandler rh = new ResponseHandler();
		EClientSocket  m_client = new EClientSocket( rh );
		IBClientRequestExecutor clientInterface = new IBClientRequestExecutor(m_client, rh);
		clientInterface.connect();
		return clientInterface;
	}
	/**
	 * Called when we get an "error" message from the client that we have market data
	 */
	public synchronized void confirmConnection(){ 
		connectionConfirmed = true;
		notify();
	}
	synchronized void waitOnConfirmation(){
		while(!connectionConfirmed)
			try {
				System.out.println("Waiting until successfully connected!");
				wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	/**
	 * Create a simulated client.
	 * @return
	 */
	public static IBClientRequestExecutor initSimulatedClient() {
		final ResponseHandler rh = new ResponseHandler();

		HistoricalDataClient m_client = new HistoricalDataClient(rh);
		return newClientInterface(rh, m_client);
	}

	/**
	 * Similar to the initSimulatedClient but it reuses the ResponseHandler & client
	 * 
	 * @param rh
	 * @param m_client
	 * @return
	 */
	public static IBClientRequestExecutor newClientInterface(final ResponseHandler rh,
			HistoricalDataClient m_client) {
		rh.setExecutorService(new ImmediateExecutor());

		IBClientRequestExecutor clientInterface = new IBClientRequestExecutor(m_client, rh);
		clientInterface.setExecutor(new ImmediateExecutor());
		clientInterface.connect();
		clientInterface.initializePortfolio();
		rh.getPortfolio().setCash(100000.0);
		return clientInterface;
	}
	/**
	 * Only want one thread sending the requests.
	 */
	public IBClientRequestExecutor(final EClientSocket socket, final ResponseHandler rh) {
		client = socket;
		this.executor = Executors.newSingleThreadExecutor();
		rh.setRequestor(this);
		responseHandler = rh;
	}
	/**
	 * Allow override - typically for testing
	 * @param ex
	 */
	public void setExecutor(final Executor ex){
		this.executor = ex;
	}
	/**
	 * Got a disconnect from the TWS and I'm responding as best I can
	 */
	protected synchronized void forcedClose() {
		LogManager.getLogger("RequestManager").error("Forced Exit");
		client.eDisconnect();
		if(executor instanceof ExecutorService)
			((ExecutorService)executor).shutdownNow();
		requests = 0;
		notifyAll();
	}

	/**
	 * Gracefully exit
	 */
	public void close() {
		LogManager.getLogger("RequestManager").info( "Shutting down");
		Thread.currentThread().dumpStack();
		client.eDisconnect();
		if(executor instanceof ExecutorService)
			((ExecutorService)executor).shutdown();
	}

	/**
	 * Connect to the default host/ports through the TWS client. It doesn't
	 * actually validate the clientId until the first request is made
	 */
	public void connect(){
		connect(true);
	}
	public void connect(boolean exitOnFailure){
		System.out.println(IBConstants.blah);
		client.eConnect(IBConstants.host, IBConstants.port, IBConstants.clientId);
		if (client.isConnected()) {
			System.out.println("Connected to Tws server version " + client.serverVersion() + " at "
					+ client.TwsConnectionTime());

		} else {
			LogManager.getLogger("RequestManager").error(
					"Failed to connect " + IBConstants.host + " " + IBConstants.port);
			if(exitOnFailure) System.exit(1);
		}
	}
	public boolean reconnect() {
		try {
			Thread.currentThread().sleep(1000);
			connectionConfirmed = false;
			connect(false);
			waitOnConfirmation();
			return true;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	/**
	 * Pass right through, no delay on this one
	 * @param id
	 */
	public void cancelOrder(int id){
		client.cancelOrder(id);
	}
	/**
	 * 
	 * @param contract
	 * @param qty
	 * @param price
	 * @param rh
	 * @return 
	 */
	public void executeOrder(final LimitOrder lmtOrder, final ResponseHandlerDelegate rh){
		final boolean buy = lmtOrder.isBuy();
		
		final int qty = lmtOrder.getShares();
		final double price = lmtOrder.getPrice();
		final boolean openStopLoss = lmtOrder.getStopLoss() != null;
		final boolean openProfitTaker = lmtOrder.getProfitTaker() != null;
		final Runnable r = new Runnable() {
			@Override
			public void run() {
				final Order order = createPrimaryOrder(buy, qty, price, rh);
				order.m_orderType =  lmtOrder.getOrderType();
				final Contract contract = lmtOrder.getContract();
				//If the id already exists then we need to actually modify the order, not create a new one
				if(lmtOrder.getId() == 0)
					lmtOrder.setId(order.m_orderId);
				else
					order.m_orderId = lmtOrder.getId();
				//TODO cleanup the response handler delegate stuff if I override the newly generated ID
				
				//Log to portfolio because we are assuming a fill		
				responseHandler.getPortfolio().placedOrder( lmtOrder );
				order.m_transmit = ! openStopLoss && ! openProfitTaker;
				LogManager.getLogger("RequestManager").warn("Placing order " + lmtOrder );
				client.placeOrder(order.m_orderId, contract, order);
				if( openStopLoss ) {
					final Order stop = createStopOrder( lmtOrder.getStopLoss(), order.m_orderId, rh);
					lmtOrder.getStopLoss().setId(stop.m_orderId);
					stop.m_transmit = ! openProfitTaker;
					client.placeOrder(stop.m_orderId, contract, stop);

					LogManager.getLogger("RequestManager").warn("Placing stop order " + lmtOrder.getStopLoss() );
					responseHandler.getPortfolio().stopOrder(lmtOrder.getStopLoss());
				}
				if(openProfitTaker){
					final Order profit = createProfitTaker( lmtOrder.getProfitTaker(), order.m_orderId, rh);
					lmtOrder.getProfitTaker().setId(profit.m_orderId);
					client.placeOrder(profit.m_orderId, contract, profit);

					LogManager.getLogger("RequestManager").warn("Placing profit taker order " + lmtOrder.getProfitTaker() );
					responseHandler.getPortfolio().profitTakerOrder(lmtOrder.getProfitTaker());
				}
			}
		};
		execute(r, 0);
	}

	public Portfolio getPortfolio() {
		return responseHandler.getPortfolio();
	}
	/**
	 * @param buy
	 * @param m_contract
	 * @param qty
	 * @param price
	 * @param rh
	 * @return
	 */
	protected Order createPrimaryOrder(final boolean buy, final int qty,
			final double price, final ResponseHandlerDelegate rh) {
		Order order = new Order();
		order.m_permId = (((int) (System.currentTimeMillis() / 1000)) << 4);
		order.m_permId += (orderIdCounter++ % 16);
		order.m_orderId = order.m_permId;
		
		pushResponseHandler(order.m_orderId, rh);

		order.m_action = buy ? "BUY" : "SELL";
		order.m_lmtPrice = price;
		order.m_totalQuantity = qty;

		order.m_clientId = IBConstants.clientId;
		order.m_allOrNone = false;
		order.m_transmit = false;
		return order;
	}
	/**
	 * 
	 * @param stopOrder
	 * @param primaryOrderId
	 * @param rh
	 * @return
	 */
	protected Order createStopOrder(final LimitOrder stopOrder, final int primaryOrderId, final ResponseHandlerDelegate rh){
		Order order = new Order();
		order.m_permId = primaryOrderId + (orderIdCounter++ % 16);
		order.m_orderId = order.m_permId;
		pushResponseHandler(order.m_orderId, rh);

		order.m_parentId = primaryOrderId;
		order.m_action = stopOrder.isBuy() ? "BUY" : "SELL";
		if( stopOrder.isTrail() ){
			order.m_orderType = "TRAIL";
			order.m_trailingPercent = stopOrder.getPrice();
		}
		else{
			order.m_orderType = "STPLMT";
			order.m_auxPrice = stopOrder.getPrice();
			order.m_lmtPrice = order.m_auxPrice;
		}
		order.m_totalQuantity = stopOrder.getShares();

		order.m_clientId = IBConstants.clientId;
		order.m_transmit = true;
		return order;
		
	}
	/**
	 * A limit order that will secure the profit!
	 * @param profitTaker
	 * @param primaryOrderId
	 * @param rh
	 * @return
	 */
	protected Order createProfitTaker(final LimitOrder profitTaker, final int primaryOrderId, final ResponseHandlerDelegate rh){
		Order order = new Order();
		order.m_permId = primaryOrderId + (orderIdCounter++ % 16);
		order.m_orderId = order.m_permId;
		pushResponseHandler(order.m_orderId, rh);

		order.m_parentId = primaryOrderId;
		order.m_action = profitTaker.isBuy() ? "BUY" : "SELL";
		if( profitTaker.isTrail() ){
			order.m_orderType = "TRAIL";
			order.m_trailingPercent = profitTaker.getPrice();
		}
		else{
			order.m_orderType = "LMT";
			order.m_lmtPrice = profitTaker.getPrice();
			
		}
		order.m_totalQuantity = profitTaker.getShares();

		order.m_clientId = IBConstants.clientId;
		order.m_transmit = true;
		return order;
		
	}
	/**
	 * Populate the portfolio with existing positions
	 * @param port
	 */
	final public void initializePortfolio( ){
		//@TODO hard coded account name
		String testAccountName = "DU132661";
		client.reqAccountUpdates(true, testAccountName);
		System.err.println("In client initializePortfolio - hard coded to the test account name " + testAccountName + "\nthis will need to change when going live");
	}
	private int requestId = 100;
	/**
	 * Find a unique request id.
	 * 
	 * @return int An id that should be used to mark a request has been
	 *         fulfilled
	 */
	final synchronized int pushRequest() {
		return requestId++;
	}
	//Keeping dead code for the moment
	synchronized int originalPushRequest(){
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
			LogManager.getLogger("PerfMetrics").info( "[" + reqId + "] ending executionTime: " + (System.currentTimeMillis() - rd.getStartTime()));
		}
	
		LogManager.getLogger("RequestManager").info( "[" + reqId + "] Ending request " );
		
		//A tracked request
		if(reqId < 1066544174) { 
			int mask = 0xFFFFFFFF;
			mask = mask ^ reqId;
			requests = requests & mask;
			
			if (requests == 0) {
				if(rd != null) {
					rd.info( "All submitted requests are complete");
				}
				else
					LogManager.getLogger("RequestManager").info( "All submitted requests are complete");
				this.notifyAll();
			}
		}
		map.remove(id);
	}

	/**
	 * Wait until all requests have been completed.
	 */
	public synchronized void waitForCompletion() {
		while (requests != 0 || !tasks.isEmpty() || active != null)
			try {
				LogManager.getLogger("RequestManager").info( "Waiting " + requests + " " + tasks.isEmpty());
				wait();
			} catch (InterruptedException e) {
				LogManager.getLogger("RequestManager").error( "Interrupted!!", e);
			}
		LogManager.getLogger("RequestManager").debug( "Waiting complete" );
	}
	/**
	 * Wait before - no need to penalize the request after the historical data request.
	 * @param seconds
	 *            The amount of time to wait *before* the runnable is executed
	 *            
	 */
	protected synchronized void execute(final Runnable r, final int seconds) {
		execute(r,seconds,null);
	}
	/**
	 * Wait before - no need to penalize the request after the historical data request.
	 * @param seconds
	 *            The amount of time to wait *before* the runnable is executed
	 *            
	 */
	protected synchronized void execute(final Runnable r, final int seconds, final StoreHistoricalData histData) {
		LogManager.getLogger("RequestManager").debug( "Enqueing request");
		
		boolean result = tasks.offer(new Runnable() {
			@Override
			public void run() {
				try {
					final boolean skip = histData != null ? histData.isSkippingDate(((HistoricalDataRequest)r).date) : false;
					if (seconds > 0 && ! skip && (!(executor instanceof ImmediateExecutor))) {
						
						// Send the request then wait for 10 seconds
						synchronized (this) {
							try {
								wait(1000 * seconds);
							} catch (InterruptedException e) {
								LogManager.getLogger("RequestManager").error( "Interrupted!!", e);
							}
						}
					}
					if(!skip) r.run();
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
			LogManager.getLogger("RequestManager").debug( "All queued requests are complete");
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
	 * @param rh For now, force to be the ResponseHandlerDelegate that writes the data out
	 */
	public void reqHistoricalData(final String symbol, final String date, final StoreHistoricalData rh) throws ParseException {
		Contract c = null;
		int idx = symbol.indexOf('_');
		if(idx > 0){
			c = new FutureContract(symbol.substring(0, idx), symbol.substring(idx));
		}
		else {
			c = new StockContract(symbol);
		}
		reqHisData(date, c, rh );
		rh.info( "History data request(s) starting " + date + " " + symbol);
	}
	
	/**
	 * Used when fixing historical data that didn't fully get loaded into the
	 * database. Only works for 5 second bar data.
	 * 
	 * @param missingData
	 * @param symbol
	 */
	public void reqHistoricalData(final ArrayList<String> missingData, final Contract symbol) {
		final StoreHistoricalData sh = new StoreHistoricalData(symbol, this);
		sh.setBarSize("bar5sec");
		requestHistoricalData(missingData, symbol, sh);
	}
	/**
	 * Enable an ending date request
	 * @param date
	 * @param m_contract
	 * @param rh
	 */
	public void requestHistDataEnding(final Contract stock,final String date,  final StoreHistoricalData rh){
		final HistoricalDataRequest r = new HistoricalDataRequest(date, rh, stock);
		execute(r, 11, rh);
	}
	/**
	 * Assumes the date list is separated into intervals that comply with the
	 * historical data request limits imposed by IB API.
	 * 
	 * @param dates
	 * @param symbol
	 * @param rh
	 */
	protected void requestHistoricalData(final ArrayList<String> dates, final Contract symbol,
			final StoreHistoricalData rh) {
		waitOnConfirmation();;
		final int markerRequestId = pushRequest();
		LogManager.getLogger("RequestManager").info( "[" + markerRequestId + "] " + 
				"Submitting HistoricalData marker" );
		boolean first = true;
		for (final String date : dates) {
			final HistoricalDataRequest r = new HistoricalDataRequest(date, rh, symbol);
			if (first) {
				execute(r, 0);
				first = false;
			} else {// wait 11 seconds for the next request.
				execute(r, 11, rh);
			}
		}
		scheduleClosingRequest(markerRequestId);
	}
	/**
	 * Run the scheduled tasks every 10 seconds. Ensure no more than 60 requests
	 * in a 10 minute period (a limit set by IB).
	 * @param date
	 * @param stock
	 * @throws ParseException
	 */
	protected void reqHisData(final String startingDate, final Contract stock, final StoreHistoricalData rh)
			throws ParseException {
		
		// Get dates one hour apart that will retrieve the historical data
		ArrayList<String> dates = rh.getDates(startingDate);
		if(dates.size() == 0){
			rh.info( "No data to request for starting date " + startingDate);
			return;
		}
		requestHistoricalData(dates, stock, rh);
	}
	class HistoricalDataRequest implements Runnable{
		String date; StoreHistoricalData rh; Contract stock;
		HistoricalDataRequest(String dateStr, StoreHistoricalData store, Contract st){
			date = dateStr; rh = store; stock = st;
		}
		boolean skip(){
			return  rh.isSkippingDate(date);
		}
		@Override
		public void run() {
			if(!skip()) { 
				final int reqId = pushRequest();
				pushResponseHandler(reqId, rh);
				rh.info(
						"["+ reqId + "] Submitting request for historical data " + date);
				rh.resetRecordCount();
				List<TagValue> notUsed = new Vector<TagValue>();
				System.out.println("" + reqId+ " "+ stock+ " "+ date+ " "+ rh.getDuration()+ " "+ rh.getBar()+ " "+
						IBConstants.showTrades+ " "+ IBConstants.rthOnly+ " "+ IBConstants.datesAsNumbers);
				client.reqHistoricalData(reqId, stock, date, rh.getDuration(), rh.getBar(),
						IBConstants.showTrades, IBConstants.rthOnly, IBConstants.datesAsNumbers, notUsed);
			}
		}
	}
	/**
	 * Get 5 second bars and route to the request.
	 * Legal ones for (STK) are: 100(Option Volume),101(Option Open Interest),
	 * 104(Historical Volatility),105(Average Opt Volume),106(Option Implied Volatility),
	 * 107(Close Implied Volatility),125(Bond analytic data),
	 * 165(Misc. Stats),166(CScreen),225(Auction),232/221(Mark Price),
	 * 233(RTVolume),236(inventory),258/47(Fundamentals),
	 * 291(Close Implied Volatility),293(TradeCount),
	 * 294(TradeRate),295(VolumeRate),318(LastRTHTrade),370(ParticipationMonitor),
	 * 370(ParticipationMonitor),377(CttTickTag),377(CttTickTag),
	 * 381(IB Rate),384(RfqTickRespTag),384(RfqTickRespTag),387(DMM),
	 * 388(Issuer Fundamentals),391(IBWarrantImpVolCompeteTick),
	 * 407(FuturesMargins),411(Real-Time Historical Volatility),428(Monetary Close Price)
	 * @param m_contract
	 * @param rh
	 */
	public void reqRealTimeBars(final Contract stock, final ResponseHandlerDelegate rh){
		final Runnable r = new Runnable() {
			@Override
			public void run() {
				final int reqId = pushRequest();
				rh.setReqId(reqId);
				
				pushResponseHandler(reqId, rh);
				LogManager.getLogger("RequestManager").info(
						"Submitting request for real time bars [" + reqId + "] " + stock);
				Vector<TagValue> notUsed = null;
				//true means RTH only
				//5 is the only legal value for realTimeBars - resulting in 5 second bars
				client.reqRealTimeBars(reqId, stock, 5, IBConstants.showTrades, false, notUsed);
				final boolean snapshot = false;
				final int tickReqId = pushRequest();
				rh.setTickerRequestId(tickReqId);
				pushResponseHandler(tickReqId, rh);
				LogManager.getLogger("RequestManager").info(
						"Submitting request for tick data [" + tickReqId + "] " + stock);
				client.reqMktData(tickReqId, stock, "100,101,106,233", snapshot, notUsed);

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
			@Override
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
	protected ResponseHandlerDelegate getResponseHandler(int reqId){
		return map.get(Integer.valueOf(reqId));
	}

	HashMap<Contract, QuoteRouter> quoteRouters = new HashMap<Contract, QuoteRouter>();

	/**
	 * Get (and maybe create) a quote router for the provided symbol.
	 * 
	 * @param symbol
	 * @return
	 */
	public synchronized QuoteRouter getQuoteRouter(final String symbol) {
		StockContract contract = new StockContract(symbol);
		return getQuoteRouter(contract);
	}
	public void cancelMktData(){
		for (QuoteRouter strat : quoteRouters.values()) {
			Logger.getLogger("MarketData").info("Cancelling ticker market data " + strat.getTickerRequestId());
			client.cancelMktData(strat.getTickerRequestId());
			Logger.getLogger("MarketData").info("Cancelling realtime bar market data " + strat.getBarRequestId());
			client.cancelMktData(strat.getBarRequestId());
		}
		
	}
	/**
	 * For every quote router I have get the quotes
	 */
	public void requestQuotes() {

		for (QuoteRouter strat : quoteRouters.values()) {
			reqRealTimeBars(strat.getContract(), strat);
		}
		if (client instanceof HistoricalDataClient) {
			((HistoricalDataClient) client).sendData();
		}
	}
	/**
	 * Event originates in HistoricalDataSender - a new day is being sent through. This will
	 * reset internal caches to ensure processing is somewhat consistent with actual processing.
	 * Things it doesn't do - result in a new request for quotes (current request is being fulfilled)
	 * Doesn't reset the portfolio - don't want to keep a simulated server version of the portfolio 
	 */
	public void reset() {
		for (QuoteRouter strat : quoteRouters.values()) {
			strat.initialize(getPortfolio());
		}
	}
	/**
	 * Similar to the other getQuoteRouter but needs an expiration date to select a specific futures contract
	 * 
	 * @param symbol
	 * @param date
	 * @return
	 */
	public QuoteRouter getQuoteRouter(String symbol, String expiry) {
		FutureContract contract = new FutureContract(symbol, expiry);
		return getQuoteRouter(contract);
	}
	/**
	 * @param contract
	 * @return
	 */
	public QuoteRouter getQuoteRouter(Contract contract) {
		QuoteRouter strat = quoteRouters.get(contract);
		if (strat == null) {
			strat = new QuoteRouter(contract, this, responseHandler.getPortfolio());
			quoteRouters.put(contract, strat);
		}
		else {
			strat.setPortfolio(responseHandler.getPortfolio());
		}
		return strat;
	}
}