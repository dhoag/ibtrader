package com.davehoag.ib.dataTypes;

import java.text.NumberFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.davehoag.ib.util.HistoricalDateManipulation;

public class Portfolio {
	HashMap<String, Integer> portfolio = new HashMap<String, Integer>();
	ArrayList<String> history = new ArrayList<String>();
	double cash = 0;
	long currentTime;
	NumberFormat nf = NumberFormat.getCurrencyInstance();
	/**
	 * Called at start time when we are initializing the portfolio
	 * @param symbol
	 * @param qty
	 */
	public void update(final String symbol, final int qty){
		Logger.getLogger("AccountManagement").log(Level.INFO, "Updating account " + symbol + " " + qty);
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
	public void confirm(final int orderId, final String symbol, final double price, final int qty){
		
		history.add("[" + orderId + "] " + new Date(currentTime) + " Confirm transaction of " + qty + " Cash: " +  nf.format(getCash()) + " Value:" + nf.format(getValue(symbol, price)));
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
		final Integer originalQty = portfolio.get(symbol);
		final Integer newQty = originalQty != null ? (originalQty.intValue() + qty) : qty;
		portfolio.put(symbol, newQty);
		history.add("[" + orderId + "]" + new Date(currentTime) + " Buy " + qty + " of " + symbol + " @ " + price);
		cash -= qty * price;
	}
	public synchronized void sold(final int orderId, final String symbol, final int qty, final double price){

		final Integer originalQty = portfolio.get(symbol);
		final Integer newQty = originalQty != null ? (originalQty.intValue() - qty) : -qty;
		portfolio.put(symbol, newQty);
		history.add("[" + orderId + "]" + new Date(currentTime) + " Sell " + qty + " of " + symbol + " @ " + price);
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
	public void dumpLog(){
		Logger.getLogger("Trading").log(Level.INFO, "#### Display trade history ####");
		for(String entry: history){
			Logger.getLogger("Trading").log(Level.INFO, entry);
		}
	}
}
