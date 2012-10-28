package com.davehoag.ib;

import java.text.NumberFormat;
import java.util.ArrayList;

import org.slf4j.*;
import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.LimitOrder;
import com.davehoag.ib.dataTypes.Portfolio;
import com.ib.client.Contract;
import com.ib.client.Execution;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.TickType;

public class TradingStrategy extends ResponseHandlerDelegate {
	final NumberFormat nf = NumberFormat.getCurrencyInstance();
	
	boolean positionOnTheBooks = false;
	Strategy strategy;
	final String symbol;
	Portfolio portfolio;
	long initialTimeStamp;

	/**
	 * One strategy, one symbol, one IBClient, and one portfolio per "TradingStrategy" instance.
	 * If you want to have multiple strategies on a given symbol then either build that in the 
	 * "strategy" that is passed in or overhaul this class.
	 * 
	 * @param sym
	 * @param strat
	 * @param exec
	 * @param port
	 */
	public TradingStrategy(final String sym, final Strategy strat, final IBClientRequestExecutor exec, final Portfolio port){
		super(exec);
		symbol = sym;
		portfolio =port;
		strategy = strat;
	}
	public void displayTradeStats(){ portfolio.displayTradeStats(strategy.getClass().getSimpleName() + ":" + symbol); }
	public void setPortfolio(Portfolio p){
		portfolio = p;
	}
	public void setStrategy(final Strategy strat){
		strategy = strat;
	}
	@Override
	public void execDetails(final int reqId, final Contract contract, final Execution execution) {
		if(contract.m_symbol.equals(symbol)){
			
			requester.endRequest(reqId);
			portfolio.confirm(execution.m_orderId, contract.m_symbol ,execution.m_price, execution.m_shares);
			LoggerFactory.getLogger("Trading").info( "[" + reqId + "] " + execution.m_side +  " execution report. Filled " + contract.m_symbol + " " + execution.m_shares + " @ " + nf.format(execution.m_price ));
		}
		else{
			LoggerFactory.getLogger("Trading").error( "Execution report for an unexpected symbol : " + contract.m_symbol + " expecting: " + symbol);
		}		
	}
	/**
	 * This method is called once all executions have been sent to a client in response to reqExecutions().
	 * Shouldn't be called in this case.
	 */
	@Override
	public void execDetailsEnd(int id) {
		LoggerFactory.getLogger("Trading").error( "Didn't expect execDetailsEnd " + reqId + " my id is " + reqId + " passed Id is " + id);
	}

	@Override
	public void realtimeBar(final int reqId, final long time, final double open, final double high,
			final double low, final double close, final long volume, final double wap, final int count) {

		final Bar bar = getBar(time, open, high, low, close, volume, wap, count);
		bar.barSize = strategy.getBarSize();
		updatePortfolioTime(time);
		portfolio.updatePrice(symbol, close);
		if( time % (60*30) == 0) { 
			LoggerFactory.getLogger("MarketData").info( "Realtime bar : " + reqId + " " + bar);
			portfolio.displayValue(symbol);
		}
	
		strategy.newBar(bar, portfolio, this);

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
		if(order.getSymbol() == null ) order.setSymbol(symbol);
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
	final protected void updatePortfolioTime(final long time) {
		portfolio.setTime(time);
		//if time is 10 hours beyond the last check, update "yesterday"
		if(time - initialTimeStamp > (10*60*60)){
			try { 
				initialTimeStamp = time;
				final Bar yest = CassandraDao.getInstance().getYesterday(symbol, time);
				portfolio.setYesterday( yest );
			} catch(Exception ex){
				LoggerFactory.getLogger("MarketData").warn( "Can't getting yesterday's bar", ex);
			}
			
		}
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
//		System.out.println("Symbol " + symbol + " tickPrice " + tickerId + " " + priceType + " " + price + " " + canAutoExecute );
		
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
			LoggerFactory.getLogger("MarketData").error( "Realtime bar failed: " + id+ " " + errorCode + " "+ errorMsg);
			
		}
		else{
			LoggerFactory.getLogger("Trading").error( "Order failed failed: " + id+ " " + errorCode + " "+ errorMsg);
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

}
