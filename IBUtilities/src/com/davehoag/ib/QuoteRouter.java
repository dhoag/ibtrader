package com.davehoag.ib;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.logging.log4j.LogManager;

import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.IntegerCache;
import com.davehoag.ib.dataTypes.LimitOrder;
import com.davehoag.ib.dataTypes.Portfolio;
import com.davehoag.ib.dataTypes.StockContract;
import com.davehoag.ib.util.HistoricalDateManipulation;
import com.ib.client.Contract;
import com.ib.client.Execution;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.TickType;

/**
 * Route quotes to the various interested parties. A QuoteRouter is tied to a
 * single symbol because a realtime bar doesn't pass the symbol from IB. So, the
 * request ID associated with the request for a realtime bar maps to the
 * QuoteRouter and thus the symbol for the data.
 * 
 * @author David Hoag
 * 
 */
public class QuoteRouter extends ResponseHandlerDelegate {
	final NumberFormat nf = NumberFormat.getCurrencyInstance();
	
	boolean positionOnTheBooks = false;
	ArrayList<Strategy> strategies = new ArrayList<Strategy>();
	final Contract m_contract;
	String date;
	Portfolio portfolio;
	boolean requestedHistoricalData = false;
	IntegerCache lastSize = new IntegerCache();
	IntegerCache bidSize = new IntegerCache();
	IntegerCache askSize = new IntegerCache();
	IntegerCache volume = new IntegerCache();
	
	public IntegerCache getLastSize(){ return lastSize; }
	public IntegerCache getBidSize(){ return bidSize; }
	public IntegerCache getAskSize(){ return askSize; }
	public IntegerCache getVolume(){ return volume; }
	public Portfolio getPortfolio(){
		return portfolio;
	}
	/**
	 * Don't go out and get historical data when the first realtime bar arrives
	 */
	public void dontGetHistoricalData(){
		requestedHistoricalData = true;
	}
	/**
	 * Reset some data
	 */
	public void initialize(final Portfolio port) {
		portfolio = port;
	}
	/**
	 * One strategy, one symbol, one IBClient, and one portfolio per
	 * "TradingStrategy" instance. If you want to have multiple strategies on a
	 * given symbol then either build that in the "strategy" that is passed in
	 * or overhaul this class.
	 * 
	 * @param sym
	 * @param strat
	 * @param exec
	 * @param port
	 */
	public QuoteRouter(final Contract sym, final IBClientRequestExecutor exec, final Portfolio port) {
		super(exec);
		m_contract = sym;
		initialize(port);
	}
	public QuoteRouter(Contract symbol2, String dt,
			IBClientRequestExecutor ibClientRequestExecutor,
			Portfolio portfolio2) {
		this(symbol2, ibClientRequestExecutor, portfolio2);
		date = dt;
	}
	/**
	 * Get 1 day bars for the past year - we only keep the last N based on the size of the 
	 * cache
	 */
	public void requestHistorical1dayBars(final long seconds) {
		final String date = HistoricalDateManipulation.getDateAsStr(new Date(seconds * 1000));
		final StoreHistoricalData histStore = new StoreHistoricalData(m_contract, getRequester());
		histStore.setBarSize("bar1day");
		histStore.setCacheOnly(200);
		getRequester().requestHistDataEnding(m_contract, date, histStore);
		portfolio.getQuoteData().putDayBarCache(m_contract, histStore.getCache());
	}
	/**
	 * 
	 * @param strat
	 */
	public void addStrategy(final Strategy strat) {
		strategies.add(strat);
	}
	public void displayTradeStats(){ portfolio.displayTradeStats(strategies.getClass().getSimpleName() + ":" + m_contract); }
	public void setPortfolio(Portfolio p){
		portfolio = p;
	}
	@Override
	public void execDetails(final int reqId, final Contract contract, final Execution execution) {
		if(m_contract.equals(contract)){
			portfolio.confirm(execution.m_orderId, m_contract ,execution.m_price, execution.m_shares);

			LogManager.getLogger("Trading").info( "[" + reqId + "] " + execution.m_side +  " execution report. Filled " + m_contract.m_symbol + " " + execution.m_shares + " @ " + nf.format(execution.m_price ));

			for (Strategy strat : strategies) {
				strat.execDetails(execution, portfolio, this);
			}
		}
		else{
			LogManager.getLogger("Trading").error( "Execution report for an unexpected symbol : " + contract.m_symbol + " " + contract.m_expiry + " expecting: " + m_contract);
		}		
	}
	/**
	 * This method is called once all executions have been sent to a client in response to reqExecutions().
	 * Shouldn't be called in this case.
	 */
	@Override
	public void execDetailsEnd(int id) {
		LogManager.getLogger("Trading").error( "Didn't expect execDetailsEnd " + reqId + " my id is " + reqId + " passed Id is " + id);
	}

	@Override
	public void realtimeBar(final int reqId, final long time, final double open, final double high,
			final double low, final double close, final long volume, final double wap, final int count) {

		final Bar bar = getBar(time, open, high, low, close, volume, wap, count);
		bar.barSize = "bar5sec";
		if(!requestedHistoricalData){
			requestedHistoricalData = true;
			requestHistorical1dayBars(time);
		}
		portfolio.getQuoteData().push5SecBar(m_contract, bar);
		portfolio.updatePrice(m_contract, close);
		updatePortfolioTime(time);
		if( time % (60*30) == 0) { 
			LogManager.getLogger("MarketData").info( "Realtime bar : " + reqId + " " + bar);
			portfolio.displayValue(m_contract);
		}
		for (Strategy strat : strategies) {
			strat.newBar(bar, portfolio, this);
		}

	}

	/**
	 * Cancel all of the orders for which we didn't get a confirm message related to the symbol
	 * of this particular trading strategy
	 */
	public void cancelOpenOrders(){
		LogManager.getLogger("Trading").warn("Cancelling all orders ");
		ArrayList<Integer> orderIds = portfolio.getOpenOrderIds(getContract());
		for(int i : orderIds){
			requester.cancelOrder(i);
			LogManager.getLogger("Trading").warn("Cancelling order id" + i);
		}
		LogManager.getLogger("Trading").warn("Completed cancelling all orders ");
	}
	/**
	 * Place an order, buy/sell ...whatever
	 * @param order
	 */
	public void executeOrder(final LimitOrder order){
		if (order.getContract() == null) order.setContract(getContract());
		else
			if (!order.getContract().equals(getContract())) {
				throw new IllegalStateException("Limit order " + order + " send through wrong router ["
						+ m_contract + ']');
			}
		
		if(order.getStopLoss() != null){
			order.getStopLoss().setContract(getContract());
		}
		if(order.getProfitTaker() != null) order.getProfitTaker().setContract(getContract());
		requester.executeOrder(order, this);
	}
	/**
	 * Called only when a request for all open orders is made or if there are resting orders
	 * known to IB
	 */
	@Override
	public void openOrder(int orderId, Contract contract, Order order, OrderState orderState) {

	}
	/**
	 * @param time
	 */
	protected void updatePortfolioTime(final long time) {
		portfolio.setTime(time);
	}

	@Override
	public void tickString(int tickerId, int tickType, String value) {
		for (Strategy strat : strategies) {
			strat.tickString(m_contract, tickType, value, portfolio, this);
		}
		
		//System.out.println("TickString: " + getContract() + " [" + tickerId + "] " + TickType.getField(tickType) + " " + value);
	}
	@Override
	public void tickPrice(int tickerId, int field, double price, int canAutoExecute) {
		for (Strategy strat : strategies) {
			strat.tickPrice(m_contract.m_symbol, field, price, portfolio, this);
		}
		
		if(field == TickType.LAST){
			portfolio.updatePrice(m_contract, price );
		}
	}
	@Override
	public void tickSize(int tickerId, int field, int size) {
		if(TickType.ASK_SIZE == field) askSize.pushPrice(size);
		if(TickType.BID_SIZE == field) bidSize.pushPrice(size);
		if(TickType.VOLUME == field) volume.pushPrice(size);
		if(TickType.LAST_SIZE == field) lastSize.pushPrice(size);
		for (Strategy strat : strategies) {
			strat.tickSize(m_contract, field, size, portfolio, this);
		}
		//System.out.println("TickSize " + getContract() + " [" + tickerId + "] " + TickType.getField(field) + " " + size );
	}
	@Override
	public void error(final int id, final int errorCode, final String errorMsg) {
		// TODO need to figure out how to map to my buy or sell so that I can undo it.
		if( id == reqId) {
			LogManager.getLogger("MarketData").error( "Realtime bar failed: " + id+ " " + errorCode + " "+ errorMsg);
			
		}
		else{
			LogManager.getLogger("Trading").error( "Order failed failed: " + id+ " " + errorCode + " "+ errorMsg);

			for (Strategy strat : strategies) {
				strat.cancelOrder(id, errorCode, portfolio, this);
			}
			portfolio.canceledOrder( id );
		}
	}
	/**
	 * @param open
	 * @param high
	 * @param low
	 * @param close
	 * @param volume
	 * @param wap
	 * @param count
	 * @return
	 */
	final protected Bar getBar(final long time, final double open, final double high, final double low, final double close, final long volume,
			final double wap, final int count) {
		final Bar bar = new Bar(); 
		bar.originalTime = time;
		bar.symbol = m_contract.m_symbol;
		bar.close = close;
		bar.open = open;
		bar.high = high;
		bar.low = low;
		bar.volume = volume;
		bar.wap = wap;
		bar.tradeCount = count;
		return bar;
	}
	/**
	 * Doesn't really try to figure out what is or isn't canceled so just brute force cancel everything
	 * associated with the original order.
	 * @param limitOrder
	 */
	public void cancelOrder(final LimitOrder limitOrder) {
		requester.cancelOrder(limitOrder.getId());
		if(limitOrder.getStopLoss() != null) cancelOrder(limitOrder.getStopLoss());
		if(limitOrder.getProfitTaker() != null) cancelOrder(limitOrder.getProfitTaker());
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//IBClientRequestExecutor clientInterface = IBClientRequestExecutor.connectToAPI();
		IBClientRequestExecutor clientInterface = IBClientRequestExecutor.initSimulatedClient();
		
		try {
			StockContract ct = new StockContract("QQQ");
			QuoteRouter qr = new QuoteRouter(ct, clientInterface, clientInterface.getPortfolio());
			long time = HistoricalDateManipulation.getTime("20130214 07:44:30");
			qr.requestHistorical1dayBars(time );
			clientInterface.waitForCompletion();
			System.out.println(qr.portfolio.getQuoteData().getDayBarCache(ct).get(0));
		} catch (Exception e) {
			LogManager.getLogger("QuoteRouter").error( "Exception!! " , e);
		}
		finally { 
			clientInterface.close();
		}
        System.exit(0);
	}
	public Contract getContract() {
		return m_contract;
	}
	/**
	 * For QuoteRouters this is the reqid
	 * @return
	 */
	public int getBarRequestId() {
		
		return reqId;
	}
}
