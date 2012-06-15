package com.davehoag.ib;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.davehoag.ib.dataTypes.Portfolio;
import com.davehoag.ib.dataTypes.SimpleMovingAvg;
import com.davehoag.ib.dataTypes.StockContract;
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
	int qty = 100;
	
	public void log( final Level logLevel, final String message) {
		Logger.getLogger("Strategy").log(logLevel, message);
	}
	public MACDStrategy(final String sym, final double [] seeds){
		symbol = sym;
		init(seeds);
	}
	/**
	 * 
	 * @param seeds
	 */
	public void init( final double [] seeds){
		sma = new SimpleMovingAvg(12, 65, seeds);
	}
	/**
	 * Called when my execution is filled
	 */
	@Override
	public void execDetails(int reqId, Contract contract, Execution execution) {
		if(contract.m_symbol.equals(symbol)){
			
			//TODO need some logic to account for all orders not actually trading or trading at different prices
		//	portfolio.confirm(execution.m_execId, execution.m_price, execution.m_side);
		}
		else{
			log(Level.SEVERE, "Execution report for an unexpected symbol : " + contract.m_symbol + " expecting: " + symbol);
		}

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
	@Override
	public void realtimeBar(int reqId, long time, double open, double high,
			double low, double close, long volume, double wap, int count) {
		//Simple cross over strategy
		if(sma.newTick(close) )
		try{
			//TRADE!!
			if(sma.isTrendingUp()){
				final int orderId = requester.executeBuyOrder(symbol, qty, close + .05, this);
				portfolio.bought(orderId, symbol, qty, close + .05);
			}
			else {
				final int orderId = requester.executeSellOrder(symbol, qty, close + .05, this);
				portfolio.sold(orderId, symbol, reqId, close - .05);
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
