package com.davehoag.ib.dataTypes;

import java.util.ArrayList;
import java.util.HashMap;

public class Portfolio {
	HashMap<StockContract, Integer> portfolio = new HashMap<StockContract, Integer>();
	ArrayList<String> history = new ArrayList<String>();
	double cash = 0;
	
	public synchronized void bought(final int orderId, final String symbol, final int qty, final double price){
		StockContract contract = new StockContract(symbol);
		final Integer originalQty = portfolio.get(contract);
		final Integer newQty = (originalQty.intValue() + qty);
		portfolio.put(contract, newQty);
		history.add("Buy " + qty + " of " + contract.getIdentifier() + " @ " + price);
		cash -= qty * price;
	}
	public synchronized void sold(final int orderId, final String symbol, final int qty, final double price){
		StockContract contract = new StockContract(symbol);
		final Integer originalQty = portfolio.get(contract);
		final Integer newQty = (originalQty.intValue() - qty);
		portfolio.put(contract, newQty);
		history.add("Sell " + qty + " of " + contract.getIdentifier() + " @ " + price);
		cash += qty * price;
	}
	public double getCash(){
		return cash;
	}
	public double value(StockContract contract, final double price){
		final Integer currentQty = portfolio.get(contract);
		if(currentQty == null) return 0;
		return currentQty * price;
	}
}
