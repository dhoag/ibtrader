package com.davehoag.ib;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.logging.log4j.LogManager;

import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.LimitOrder;
import com.davehoag.ib.dataTypes.Portfolio;
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
	final String symbol;
	Portfolio portfolio;
	boolean requestedHistoricalData = false;
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
	public QuoteRouter(final String sym, final IBClientRequestExecutor exec, final Portfolio port) {
		super(exec);
		symbol = sym;
		initialize(port);
	}
	/**
	 * Get 1 day bars for the past year - we only keep the last N based on the size of the 
	 * cache
	 */
	public void requestHistorical1dayBars(final long seconds) {
		String date = HistoricalDateManipulation.getDateAsStr(new Date(seconds * 1000));
		final StoreHistoricalData histStore = new StoreHistoricalData(symbol, getRequester());
		histStore.setBarSize("bar1day");
		histStore.setCacheOnly(200);
		requester.requestHistDataEnding(symbol, date, histStore);
		portfolio.getQuoteData().putDayBarCache(symbol, histStore.getCache());
	}
	/**
	 * 
	 * @param strat
	 */
	public void addStrategy(final Strategy strat) {
		strategies.add(strat);
	}
	public void displayTradeStats(){ portfolio.displayTradeStats(strategies.getClass().getSimpleName() + ":" + symbol); }
	public void setPortfolio(Portfolio p){
		portfolio = p;
	}
	@Override
	public void execDetails(final int reqId, final Contract contract, final Execution execution) {
		if(contract.m_symbol.equals(symbol)){
			portfolio.confirm(execution.m_orderId, contract.m_symbol ,execution.m_price, execution.m_shares);
			LogManager.getLogger("Trading").info( "[" + reqId + "] " + execution.m_side +  " execution report. Filled " + contract.m_symbol + " " + execution.m_shares + " @ " + nf.format(execution.m_price ));
		}
		else{
			LogManager.getLogger("Trading").error( "Execution report for an unexpected symbol : " + contract.m_symbol + " expecting: " + symbol);
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
		if(!requestedHistoricalData){
			requestedHistoricalData = true;
			requestHistorical1dayBars(time);
		}
		portfolio.getQuoteData().push5SecBar(symbol, bar);
		portfolio.updatePrice(symbol, close);
		updatePortfolioTime(time);
		if( time % (60*30) == 0) { 
			LogManager.getLogger("MarketData").info( "Realtime bar : " + reqId + " " + bar);
			portfolio.displayValue(symbol);
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
		ArrayList<Integer> orderIds = portfolio.getOpenOrderIds(symbol);
		for(int i : orderIds){
			requester.cancelOrder(i);
		}
	}
	/**
	 * Place an order, buy/sell ...whatever
	 * @param order
	 */
	public void executeOrder(final LimitOrder order){
		if (order.getSymbol() == null) {
			order.setSymbol(symbol);
		} else {
			if (!order.getSymbol().equals(symbol)) {
				throw new IllegalStateException("Limit order " + order + " send through wrong router ["
						+ symbol + ']');
			}
		}
		if(order.getStopLoss() != null) order.getStopLoss().setSymbol(symbol);
		requester.executeOrder(order, this);
	}
	/**
	 * Called only when a request for all open orders is made??
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
		//ignore for now
		//System.out.println("Symbol " + symbol + "tickString " + tickerId + " " + tickType + " " + value);
	}
	@Override
	public void tickPrice(int tickerId, int field, double price, int canAutoExecute) {
		String priceType;
		switch(field){
		case TickType.ASK: priceType = "Ask"; break;
		case TickType.BID: priceType = "Bid"; break;
		case TickType.LAST: priceType = "Last"; portfolio.updatePrice(symbol, price ); break;
		default: priceType = "High,Low,Close";
		}
		for (Strategy strat : strategies) {
			strat.tickPrice(symbol, field, price, portfolio, this);
		}
		
	}
	@Override
	public void tickSize(int tickerId, int field, int size) {
		//ignore for now
		//System.out.println("Symbol " + symbol + " tickSize " + tickerId + " " + field + " " + size );
	}
	@Override
	public void error(final int id, final int errorCode, final String errorMsg) {
		// TODO need to figure out how to map to my buy or sell so that I can undo it.
		if( id == reqId) {
			LogManager.getLogger("MarketData").error( "Realtime bar failed: " + id+ " " + errorCode + " "+ errorMsg);
			
		}
		else{
			LogManager.getLogger("Trading").error( "Order failed failed: " + id+ " " + errorCode + " "+ errorMsg);
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
		bar.symbol = symbol;
		bar.close = close;
		bar.open = open;
		bar.high = high;
		bar.low = low;
		bar.volume = volume;
		bar.wap = wap;
		bar.tradeCount = count;
		return bar;
	}

	public void cancelOrder(final LimitOrder stopLoss) {
		requester.cancelOrder(stopLoss.getId());
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//IBClientRequestExecutor clientInterface = IBClientRequestExecutor.connectToAPI();
		IBClientRequestExecutor clientInterface = IBClientRequestExecutor.initSimulatedClient();
		
		try {
			QuoteRouter qr = new QuoteRouter("QQQ", clientInterface, clientInterface.getPortfolio());
			long time = HistoricalDateManipulation.getTime("20130214 07:44:30");
			qr.requestHistorical1dayBars(time );
			clientInterface.waitForCompletion();
			System.out.println(qr.portfolio.getQuoteData().getDayBarCache("QQQ").get(0));
		} catch (Exception e) {
			LogManager.getLogger("QuoteRouter").error( "Exception!! " , e);
		}
		finally { 
			clientInterface.close();
		}
        System.exit(0);
	}
}
