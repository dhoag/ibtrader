package com.davehoag.ib.dataTypes;

import java.text.NumberFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import org.slf4j.LoggerFactory;

import com.davehoag.ib.util.HistoricalDateManipulation;

public class Portfolio {
	HashMap<String, Integer> portfolio = new HashMap<String, Integer>();
	HashMap<String, Double> lastPrice = new HashMap<String, Double>();
	HashMap<Integer, LimitOrder> orders = new HashMap<Integer, LimitOrder>();
	ArrayList<String> history = new ArrayList<String>();
	ArrayList<LimitOrder> openCloseLog = new ArrayList<LimitOrder>();
	double cash = 0;
	double maxDrawdown;
	long currentTime;
	NumberFormat nf = NumberFormat.getCurrencyInstance();
	Bar yesterday;
	RiskLimits risk = new SimpleRiskLimits();
	Stack<LimitOrder> positionToUnwind = new Stack<LimitOrder>();
	
	public void displayValue(){
		for(String symbol: portfolio.keySet()){
			displayValue(symbol);
		}
	}
	/**
	 *TODO need to get this working for porfolios holding multiple stocks
	 *right now assumes a single stock, unlike the rest of this class...:(
	 */
	public void displayTradeStats(){
		double profit = 0;
		int winningTrades = 0;
		for(LimitOrder closingOrder : openCloseLog){
			double tradeProfit =closingOrder.getProfit(); 
			profit += tradeProfit;
			if(tradeProfit > 0) winningTrades++;
		}
		LoggerFactory.getLogger("Portfolio").info( "Trades " + openCloseLog.size() + " Winning trades: " + winningTrades + " Profit: " + profit );
		LoggerFactory.getLogger("Portfolio").info( "Drawdown " + maxDrawdown );
	}
	public void displayValue(final String symbol ){
		LoggerFactory.getLogger("Portfolio").info( symbol + " Time: " + HistoricalDateManipulation.getDateAsStr(currentTime) + " C: " + nf.format( getCash()) + " value " + nf.format( getValue(symbol, lastPrice.get(symbol))));
	}
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
	public void updatePrice(final String symbol, final double price){
		lastPrice.put(symbol, price);
	}
	/**
	 * IB API approach with time in seconds 
	 * @param time
	 */
	public void setTime(final long time){
		currentTime = time;
		if(HistoricalDateManipulation.isEndOfDay(time)){
			//dumpLog();
		}
	}
	public String getTime(){
		return HistoricalDateManipulation.getDateAsStr(currentTime);
	}
	public synchronized void confirm(final int orderId, final String symbol, final double price, final int qty){
		final LimitOrder order = orders.get(orderId);
		if(order != null){
			order.confirm();
			//set to the actual fill price, may be different than order price
			order.setPrice(price);
		}
		history.add("[" + orderId + "] " + HistoricalDateManipulation.getDateAsStr(currentTime ) + " Confirm transaction of " + qty + " Cash: " +  nf.format(getCash()) + " Value:" + nf.format(getValue(symbol, price)));
	}
	public void placedOrder(final LimitOrder lmtOrder){
		orders.put(lmtOrder.getId(), lmtOrder);
		if(lmtOrder.isBuy()){
			bought(lmtOrder);
		}
		else {
			sold(lmtOrder);
		}
	}
	public synchronized void bought(final LimitOrder lmtOrder){
		final int orderId = lmtOrder.getId();
		final String symbol = lmtOrder.getSymbol();
		final int qty = lmtOrder.getShares();
		final double price = lmtOrder.getPrice();
		
		if( ! risk.acceptTrade(true, qty, this, price)){
			throw new IllegalStateException("Not willing to make  trade - too risky");
		}
		final Integer originalQty = portfolio.get(symbol);
		final int origQtyInt = originalQty != null ? (originalQty.intValue()) : 0;
		final int newQty = origQtyInt + qty;
		openCloseAccounting(newQty, origQtyInt, lmtOrder);
		portfolio.put(symbol, newQty);
		history.add("[" + orderId + "]" + HistoricalDateManipulation.getDateAsStr(currentTime) + " Buy " + qty + " of " + symbol + " @ " + nf.format(price));
		cash -= qty * price;
		if(cash < maxDrawdown){
			maxDrawdown = cash;
		}
	}
	/**
	 * If the order is unwinding an open position, record as such
	 * @param newQty
	 * @param originalQty
	 * @param lmtOrder
	 */
	protected void openCloseAccounting(final int newQty, final int originalQty, final LimitOrder lmtOrder){
		if(Math.abs(newQty) > Math.abs(originalQty)){
			//Opening a long or short position
			positionToUnwind.push(lmtOrder);
		}
		else {
			LimitOrder unwound = positionToUnwind.pop();
			if(unwound.getShares() != lmtOrder.getShares()) throw new IllegalStateException("Should only be selling the qty we bought");
			lmtOrder.setOnset(unwound);
			openCloseLog.add(lmtOrder);
		}
	}
	public synchronized void sold(final LimitOrder lmtOrder){
		final int orderId = lmtOrder.getId();
		final String symbol = lmtOrder.getSymbol();
		final int qty = lmtOrder.getShares();
		final double price = lmtOrder.getPrice();

		if( ! risk.acceptTrade(false, qty, this, price)){
			throw new IllegalStateException("Not willing to make  trade - too risky");
		}
		final Integer originalQty = portfolio.get(symbol);
		final int origQtyInt = originalQty != null ? (originalQty.intValue()) : 0;
		final int newQty =  origQtyInt - qty;
		openCloseAccounting(newQty, origQtyInt, lmtOrder);
		portfolio.put(symbol, newQty);
		history.add("[" + orderId + "]" + HistoricalDateManipulation.getDateAsStr(currentTime ) + " Sell " + qty + " of " + symbol + " @ " + nf.format(price));
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
		LoggerFactory.getLogger("TradingHistory").info( "#### Display trade history ####");
		for(String entry: history){
			LoggerFactory.getLogger("TradingHistory").info( entry);
		}
	}
}
