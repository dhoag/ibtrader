package com.davehoag.ib.dataTypes;

import java.text.NumberFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import org.slf4j.LoggerFactory;

import com.davehoag.ib.util.HistoricalDateManipulation;

public class Portfolio {
	HashMap<String, Integer> portfolio = new HashMap<String, Integer>();
	ArrayList<String> history = new ArrayList<String>();
	double cash = 0;
	long currentTime;
	NumberFormat nf = NumberFormat.getCurrencyInstance();
	Bar yesterday;
	RiskLimits risk = new SimpleRiskLimits();
	/**
	 * sometimes knowing yesterday's data is valuable. Could be null 
	 * @param aOneDayBar
	 */
	public void setYesterday(final Bar aOneDayBar){
		yesterday = aOneDayBar;
	}
	/**
	 * Called at start time when we are initializing the portfolio
	 * @param symbol
	 * @param qty
	 */
	public void update(final String symbol, final int qty){
		LoggerFactory.getLogger("AccountManagement").info( "Updating account " + symbol + " " + qty);
		portfolio.put(symbol, qty);
	}
	/**
	 * IB API approach with time in seconds 
	 * @param time
	 */
	public void setTime(final long time){
		currentTime = time;
		if(HistoricalDateManipulation.isEndOfDay(time)){
			dumpLog();
		}
	}
	public synchronized void confirm(final int orderId, final String symbol, final double price, final int qty){
		
		history.add("[" + orderId + "] " + new Date(currentTime*1000) + " Confirm transaction of " + qty + " Cash: " +  nf.format(getCash()) + " Value:" + nf.format(getValue(symbol, price)));
	}
	public void placedOrder(final boolean isBuy, final int orderId, final String symbol, final int qty, final double price){
		if(isBuy){
			bought(orderId, symbol, qty, price);
		}
		else {
			sold(orderId, symbol, qty, price);
		}
	}
	public synchronized void bought(final int orderId, final String symbol, final int qty, final double price){
		if( ! risk.acceptTrade(true, qty, this, price)){
			throw new IllegalStateException("Not willing to make  trade - too risky");
		}
		final Integer originalQty = portfolio.get(symbol);
		final Integer newQty = originalQty != null ? (originalQty.intValue() + qty) : qty;
		portfolio.put(symbol, newQty);
		history.add("[" + orderId + "]" + new Date(currentTime * 1000) + " Buy " + qty + " of " + symbol + " @ " + nf.format(price));
		cash -= qty * price;
	}
	public synchronized void sold(final int orderId, final String symbol, final int qty, final double price){
		if( ! risk.acceptTrade(false, qty, this, price)){
			throw new IllegalStateException("Not willing to make  trade - too risky");
		}
		final Integer originalQty = portfolio.get(symbol);
		final Integer newQty = originalQty != null ? (originalQty.intValue() - qty) : -qty;
		portfolio.put(symbol, newQty);
		history.add("[" + orderId + "]" + new Date(currentTime * 1000) + " Sell " + qty + " of " + symbol + " @ " + nf.format(price));
		cash += qty * price;
	}
	public int getShares(final String symbol){
		final Integer originalQty = portfolio.get(symbol);
		return originalQty != null ? originalQty.intValue() : 0;
	}
	public double getCash(){
		return cash;
	}
	public double getValue(String symbol, final double price){
		final Integer currentQty = portfolio.get(symbol);
		if(currentQty == null) return 0;
		return currentQty.intValue() * price;
	}
	public synchronized void dumpLog(){
		LoggerFactory.getLogger("Trading").info( "#### Display trade history ####");
		for(String entry: history){
			LoggerFactory.getLogger("Trading").info( entry);
		}
	}
}
