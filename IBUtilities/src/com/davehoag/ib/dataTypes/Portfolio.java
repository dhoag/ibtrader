package com.davehoag.ib.dataTypes;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;

import org.apache.logging.log4j.LogManager;

import com.davehoag.ib.util.HistoricalDateManipulation;

import flanagan.analysis.Stat;
import flanagan.math.Fmath;

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
	SimpleRiskLimits risk = new SimpleRiskLimits();
	HashMap<String, Stack<LimitOrder>> positionToUnwind = new HashMap<String, Stack<LimitOrder>>();
	TradingDay quoteData = new TradingDay();
	
	public TradingDay getQuoteData(){
		return quoteData;
	}
	/**
	 * 
	 * @param symbol
	 * @return
	 */
	public Iterator<LimitOrder> getPositionsToUnwind(String symbol){
		return positionToUnwind.get(symbol).iterator();
	}
	/**
	 * 
	 */
	public void displayValue(){
		for(String symbol: portfolio.keySet()){
			displayValue(symbol);
		}
		LogManager.getLogger("Portfolio").info("Overall value: " + getNetValue());
		dumpLog();
	}

	public void writeTradeDetails(final String strategyName) {
		NumberFormat nf = new DecimalFormat("#.##");
		FileWriter fw = null;
		try {
			fw = new FileWriter("trades-" + strategyName + ".csv");
			BufferedWriter bw = new BufferedWriter(fw);
			for (LimitOrder closingOrder : openCloseLog) {
				final double tradeProfit = closingOrder.getProfit();
				bw.write(closingOrder.getSymbol());
				bw.write(",");
				bw.write(String.valueOf(closingOrder.getOnset().getShares()));
				bw.write(",");
				bw.write(HistoricalDateManipulation.getDateAsStr(closingOrder.getOnset().getPortfolioTime()));
				bw.write(",");
				bw.write(nf.format(closingOrder.getOnset().getPrice()));
				bw.write(",");
				bw.write(HistoricalDateManipulation.getDateAsStr(closingOrder.getPortfolioTime()));
				bw.write(",");
				bw.write(nf.format(closingOrder.getPrice()));
				bw.write(",");
				bw.write(String.valueOf(nf.format(tradeProfit)));
				bw.write("\n");
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (fw != null) {
				try {
					fw.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	/**
	 */
	public void displayTradeStats(final String strategyName) {
		NumberFormat nf = NumberFormat.getCurrencyInstance();
		double profit = 0;
		int winningTrades = 0;
		double [] results = new double [openCloseLog.size()];
		int i = 0;

		for(LimitOrder closingOrder : openCloseLog){
			final double tradeProfit =closingOrder.getProfit();
			results[i++] = tradeProfit;
			profit += tradeProfit;
			if(tradeProfit > 0) winningTrades++;
		}

		LogManager.getLogger(strategyName).info(
				"Trades " + openCloseLog.size() + " Winning trades: " + winningTrades + " Profit: "
						+ nf.format(profit));
		if (results.length > 0) {
			double dmin = Fmath.minimum(results);
			double dmax = Fmath.maximum(results);
			double span = dmax - dmin;
			double[][] hist = Stat.histogramBins(results, span / 10);
			double[] cols = hist[0];
			double[] count = hist[1];
			for (int i1 = 0; i1 < cols.length; i1++) {
				LogManager.getLogger(strategyName).info(nf.format(cols[i1]) + " " + count[i1]);
			}
		}
		
		LogManager.getLogger(strategyName).info( "Drawdown " + maxDrawdown );
	}
	/**
	 * Walk through all open/closed trades and calculate the total profit
	 */
	public double getProfit(){
		double profit = 0;
		for(LimitOrder closingOrder : openCloseLog){
			double tradeProfit =closingOrder.getProfit(); 
			profit += tradeProfit;
		}
		return profit;
	}
	/**
	 * Determine the overall financial value of this portfolio
	 * @return
	 */
	public double getNetValue(){
		double value = 0;
		for(String symbol: lastPrice.keySet()){
			final double positionValue = getValue(symbol, lastPrice.get(symbol));
			value+=positionValue;
		}
		final double cashFromTrading = getCash();
		return value + cashFromTrading;
	}
	public void displayValue(final String symbol ){
		LogManager.getLogger("Portfolio").info( symbol + " Time: " + HistoricalDateManipulation.getDateAsStr(currentTime) + " C: " + nf.format( getCash())+ " " + lastPrice.get(symbol) + " * " + getShares(symbol) + " value " + nf.format( getValue(symbol, lastPrice.get(symbol))));
	}
	/**
	 * Called at start time when we are initializing the portfolio
	 * @param symbol
	 * @param qty
	 */
	public void update(final String symbol, final int qty){
		LogManager.getLogger("AccountManagement").info( "Updating account " + symbol + " " + qty);
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
	/**
	 * Look for open orders
	 * @param symbol Open orders for the given symbol
	 * @return
	 */
	public ArrayList<Integer> getOpenOrderIds(String symbol){
		ArrayList<Integer> result = new ArrayList<Integer>();
		for(LimitOrder order: orders.values() ){
			if(order.getSymbol().equals(symbol) && ! order.isConfirmed()){
				result.add(order.getId());
			}
		}
		return result;
	}

	public LimitOrder getOrder(int orderId) {
		return orders.get(orderId);
	}
	/**
	 * Confirm orders for the given symbol
	 * @param orderId
	 * @param symbol
	 * @param price
	 * @param qty
	 */
	public synchronized void confirm(final int orderId, final String symbol, final double price, final int qty){
		final LimitOrder order = orders.get(orderId);
		if(order != null){
			order.confirm();
			//set to the actual fill price, may be different than order price
			order.setPrice(price);
			order.setPortfolioTime(currentTime);
			//did this bypass the placedOrder method and thus the portfolio accounting?
			if(order.isStop()){
				placedOrder(order);
			}
		}
		else
		{
			LogManager.getLogger("Portfolio").error("Confirming an order [" + orderId + "] " + symbol + " " + price + " " + qty + " I don't know about!!");
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
	private synchronized void bought(final LimitOrder lmtOrder){
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
		if(positionToUnwind.get(lmtOrder.getSymbol()) == null) positionToUnwind.put(lmtOrder.getSymbol(), new Stack<LimitOrder>());
		if(Math.abs(newQty) > Math.abs(originalQty)){
			if(originalQty != 0 && ! risk.allowRebuys ) throw new IllegalStateException("Risk! Not allowing increases in existing positions");
			//Opening a long or short position
			positionToUnwind.get(lmtOrder.getSymbol()).push(lmtOrder);
		}
		else {
			int closedQty = 0;
			int sellQty= lmtOrder.getShares();
			LimitOrder closingOrder = lmtOrder;
			while(closedQty < sellQty){
				final LimitOrder unwound = positionToUnwind.get(lmtOrder.getSymbol()).pop();
				if(unwound.getShares() != sellQty && ! risk.allowRebuys) throw new IllegalStateException("Should only be selling the qty we bought");
				openCloseLog.add(closingOrder);
				closedQty += unwound.getShares();
				if(closedQty > sellQty) throw new IllegalStateException("Closed more open qty than was in the sell!!");
				LimitOrder placeholder = lmtOrder.clone();
				closingOrder.setOnset(unwound);
				if(closedQty < sellQty) {
					closingOrder.markAsCloseMultiple();
					placeholder.markAsCloseMultiple();
				}
				closingOrder = placeholder;
			}
		}
	}

	public void orderStatus(int orderId, String status, int filled, int remaining, double avgFillPrice,
			int permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
		if ("Cancelled".equals(status)) {
			LimitOrder ord = getOrder(orderId);
			if (ord != null) {
				ord.confirm();
			}
			canceledOrder(orderId);
		}

	}
	private synchronized void sold(final LimitOrder lmtOrder){
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
	public void setCash(double d){
		cash = d;
	}
	public double getValue(String symbol, final double price){
		final Integer currentQty = portfolio.get(symbol);
		if(currentQty == null) return 0;
		return currentQty.intValue() * price;
	}
	public synchronized void dumpLog(){
		LogManager.getLogger("TradingHistory").info( "#### Display trade history ####");
		for(String entry: history){
			LogManager.getLogger("TradingHistory").info( entry);
		}
	}
	/**
	 * Record that there is a pending order out there that may or may not ever get hit
	 * @param stopLoss
	 */
	public void stopOrder(final LimitOrder stopLoss) {
		orders.put(stopLoss.getId(), stopLoss);
	}
	/**
	 * Received a cancel from TWS
	 * @param id
	 */
	public void canceledOrder(int id) {
		orders.remove(id);
	}

	public ArrayList<LimitOrder> getTrades() {

		return openCloseLog;
	}
	public void profitTakerOrder(LimitOrder profitTaker) {
		orders.put(profitTaker.getId(), profitTaker);
	}
}
