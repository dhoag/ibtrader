package com.davehoag.ib.dataTypes;

import java.util.ArrayList;
import java.util.HashMap;

public class Portfolio {
	HashMap<String, Integer> portfolio = new HashMap<String, Integer>();
	ArrayList<String> history = new ArrayList<String>();
	double cash = 0;
	
	public synchronized void bought(final int orderId, final String symbol, final int qty, final double price){
		final Integer originalQty = portfolio.get(symbol);
		final Integer newQty = originalQty != null ? (originalQty.intValue() + qty) : qty;
		portfolio.put(symbol, newQty);
		history.add("Buy " + qty + " of " + symbol + " @ " + price);
		cash -= qty * price;
	}
	public synchronized void sold(final int orderId, final String symbol, final int qty, final double price){

		final Integer originalQty = portfolio.get(symbol);
		final Integer newQty = originalQty != null ? (originalQty.intValue() - qty) : -qty;
		portfolio.put(symbol, newQty);
		history.add("Sell " + qty + " of " + symbol + " @ " + price);
		cash += qty * price;
	}
	public double getCash(){
		return cash;
	}
	public double getValue(String symbol, final double price){
		final Integer currentQty = portfolio.get(symbol);
		if(currentQty == null) return 0;
		return currentQty.intValue() * price;
	}
}
