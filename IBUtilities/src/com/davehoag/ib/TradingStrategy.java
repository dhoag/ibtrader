package com.davehoag.ib;

import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.LimitOrder;
import com.davehoag.ib.dataTypes.Portfolio;
import com.ib.client.Contract;
import com.ib.client.Execution;

public class TradingStrategy extends ResponseHandlerDelegate {
	
	boolean positionOnTheBooks = false;
	Strategy strategy;
	final String symbol;
	public int defaultQty = 100;
	Portfolio portfolio;
	long initialTimeStamp;

	public TradingStrategy(final String sym, final Strategy strat, final IBClientRequestExecutor exec, final Portfolio port){
		super(exec);
		symbol = sym;
		portfolio =port;
		strategy = strat;
	}
	
	public void setPortfolio(Portfolio p){
		portfolio = p;
	}
	public void setStrategy(final Strategy strat){
		strategy = strat;
	}
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
		portfolio.confirm(execution.m_orderId, contract.m_symbol ,execution.m_price, execution.m_shares);
		
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
	public void realtimeBar(final int reqId, final long time, final double open, final double high,
			final double low, final double close, final long volume, final double wap, final int count) {


		final Bar bar = getBar(time, open, high, low, close, volume, wap, count);
		portfolio.setTime(time);
		if(time - initialTimeStamp > (24*60*60)){
			try { 
			final Bar yest = CassandraDao.getInstance().getYesterday(symbol, time);
			portfolio.setYesterday( yest );
			} catch(Exception ex){
				Logger.getLogger("MarketData").log(Level.WARNING, "Error getting yesterday", ex);
			}
			initialTimeStamp = time;
		}
		if( time % 60 == 0) { 
			NumberFormat nf = NumberFormat.getCurrencyInstance();
			Logger.getLogger("MarketData").log(Level.INFO, "Realtime bar : " + reqId + " " + bar);
			Logger.getLogger("Portfolio").log(Level.INFO, "Time: " + time + " C: " + nf.format(portfolio.getCash()) + " value " + nf.format( portfolio.getValue(symbol, close)));
		}
	
		final LimitOrder order = strategy.newBar(bar, portfolio);
		if(order != null){
			if(order.getSymbol() == null ) order.setSymbol(symbol);
			portfolio.placedOrder(order.isBuy(), 0, order.getSymbol(), order.getShares(), order.getPrice() );
			requester.executeOrder(order.isBuy(), order.getSymbol(), order.getShares(), order.getPrice(), this);
		}

	}
	@Override
	public void error(final int id, final int errorCode, final String errorMsg) {
		// TODO need to figure out how to map to my buy or sell so that I can undo it.
		if( id == reqId) {
			Logger.getLogger("MarketData").log(Level.SEVERE, "Realtime bar failed: " + id+ " " + errorCode + " "+ errorMsg);
			
		}
		else{
			Logger.getLogger("Trading").log(Level.SEVERE, "Order failed failed: " + id+ " " + errorCode + " "+ errorMsg);
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
