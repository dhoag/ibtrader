package com.davehoag.ib;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Calendar;
import java.util.Date;

import com.davehoag.ib.dataTypes.Portfolio;
import com.davehoag.ib.dataTypes.SimpleMovingAvg;
import com.ib.client.Contract;
import com.ib.client.Execution;

/**
 * Current design only supports 1 strategy per symbol. Need to route market data not directly to a strategy
 * but through a StrategyManager.
 * @author dhoag
 *
 */
public class MACDStrategy extends ResponseHandlerDelegate {
	Portfolio portfolio;
	final String symbol;
	SimpleMovingAvg sma;
	SimpleMovingAvg smaTrades;
	int qty = 100;
	int fastMovingAvg = 12;
	int slowMovingAvg = 25;
	boolean useEma = false;
	boolean requireTradeConfirmation = true;

	public MACDStrategy(final String sym, final double [] seeds, IBClientRequestExecutor ibInterface){
		super(ibInterface);
		symbol = sym;
		init(seeds);
		log(Level.INFO, "Initializing MACD Strategy for "+ sym);
	}
	public void log( final Level logLevel, final String message) {
		Logger.getLogger("Strategy").log(logLevel, message);
	}
	/**
	 * 
	 * @param seeds
	 */
	public void init( final double [] seeds){
		sma = new SimpleMovingAvg(fastMovingAvg, slowMovingAvg, seeds);
		sma.setUseEmaForCrossOvers(useEma);
		smaTrades = new SimpleMovingAvg(fastMovingAvg, slowMovingAvg);
		smaTrades.setUseEmaForCrossOvers(true);
	}
	/**
	 * Called when my execution is filled
	 */
	@Override
	public void execDetails(final int reqId, final Contract contract, final Execution execution) {
		if(contract.m_symbol.equals(symbol)){
			
			//TODO need some logic to account for all orders not actually trading or trading at different prices
		//	portfolio.confirm(execution.m_execId, execution.m_price, execution.m_side);
			Logger.getLogger("Trading").log(Level.INFO, "[" + reqId + "] " + execution.m_side +  "Order " + execution.m_orderId + " filled " + execution.m_shares + " @ " + execution.m_price );
		}
		else{
			log(Level.SEVERE, "Execution report for an unexpected symbol : " + contract.m_symbol + " expecting: " + symbol);
		}
		//TODO for now assuming full fills, not partials
		requester.endRequest(reqId);
	}
	/**
	 * This method is called once all executions have been sent to a client in response to reqExecutions().
	 * Shouldn't be called in this case.
	 */
	@Override
	public void execDetailsEnd(int id) {
		log(Level.SEVERE, "Didn't expect execDetailsEnd " + reqId + " my id is " + reqId + " passed Id is " + id);
	}
	@Override
	public void error(int id, int errorCode, String errorMsg) {
		// TODO need to figure out how to map to my buy or sell so that I can undo it.
		if( id == reqId) {
			Logger.getLogger("MarketData").log(Level.SEVERE, "Realtime bar failed: " + id+ " " + errorCode + " "+ errorMsg);
			
		}
		else{
			Logger.getLogger("Trading").log(Level.SEVERE, "Order failed failed: " + id+ " " + errorCode + " "+ errorMsg);
		}
		
	}
	/**
	 * determine if this strategy likes this time of day for trading
	 * @return
	 */
	protected boolean inTradeWindow(final long time){
		final int hour = getHour(time);
		//don't trade the open or close
		return hour > 8 & hour < 14;
	}
	/**
	 * @param time
	 * @return
	 */
	protected int getHour(final long time) {
		final Date d = new Date(time*1000);
		final Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		final int hour = cal.get(cal.HOUR_OF_DAY);
		return hour;
	}
	@Override
	public void realtimeBar(final int reqId, final long time, final double open, double high,
			double low, double close, final long volume, final double wap, final int count) {
		
		if( ! inTradeWindow(time)) {
			final int holdings = portfolio.getShares(symbol);
			if(holdings > 0){
				final int orderId = requester.executeSellOrder(symbol, holdings, close + .05, this);
				portfolio.sold(orderId, symbol, holdings, close - .05);
				log(Level.INFO,"Cash " +  portfolio.getCash() + " Value " + portfolio.getValue(symbol, close));
			}
			sma.reset();

		}
		//Simple cross over strategy
		else
		try{
			smaTrades.newTick(count);
			//only trade if the # of trades is rising with the cross over
			if(sma.newTick(wap) && smaTrades.isTrendingUp() ){
				//TRADE!!
				if(sma.isTrendingUp()){
					final int orderId = requester.executeBuyOrder(symbol, qty, close + .05, this);
					portfolio.bought(orderId, symbol, qty, close + .05);
				}
				else {
					if(portfolio.getShares(symbol) >= qty) {
						final int orderId = requester.executeSellOrder(symbol, qty, close + .05, this);
						portfolio.sold(orderId, symbol, qty, close - .05);
					}
				}
				log(Level.INFO,"Cash " +  portfolio.getCash() + " Value " + portfolio.getValue(symbol, close));	
			}
		}
		catch(Exception ex){
			ex.printStackTrace();
		}
		
	}
	/**
	 * should only be set during initialization, no? 
	 * @param port
	 */
	public void setPortfolio(final Portfolio port){
		portfolio = port;
	}
}
