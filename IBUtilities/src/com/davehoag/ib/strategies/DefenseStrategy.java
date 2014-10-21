package com.davehoag.ib.strategies;

import java.util.ArrayList;
import java.util.Stack;

import com.davehoag.ib.QuoteRouter;
import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.DoubleCache;
import com.davehoag.ib.dataTypes.LimitOrder;
import com.davehoag.ib.dataTypes.Portfolio;
import com.ib.client.Execution;
import com.ib.client.TickType;

public class DefenseStrategy extends AbstractStrategy {

	boolean upBias = true;
	final boolean buy = true;
	final boolean sell = false;
	boolean closePosition = false;
	boolean playDefense = false;
	boolean cancelAll = false;
	boolean mkt = false;
	int contractQty = 1;
	//Simple quote ladder with only two on each side
	LimitOrder buySide;
	LimitOrder sellSide;
	Stack<LimitOrder> positionTrade = new Stack<LimitOrder>();
	ArrayList<LimitOrder> defenseClosed = new ArrayList<LimitOrder>();
	ArrayList<LimitOrder> trendClosed = new ArrayList<LimitOrder>();
	QuoteRouter esRouter;
	double lastPrice;
	DoubleCache dc = new DoubleCache();
	/**
	 * Enter a long position with a stop 1 tick below entry price
	 */
	public void goLong(){
		upBias = true;
		createAndExecuteOrder();
	}
	/**
	 * Enter a short position with a stop 1 tick above entry price
	 */
	public void goShort(){
		upBias = false;
		createAndExecuteOrder();
	}
	public void sellClose(){
		upBias = false;
		closePosition = true;
		//if closePosition is true then it will seek to cancel
		//resting stop orders
		createAndExecuteOrder();
	}
	public void buyClose(){
		upBias = true;
		closePosition = true;
		createAndExecuteOrder();
	}
	public void playDefense(boolean toggle){
		playDefense = toggle;
		if(! playDefense){
			buySide = cancelOrder(buySide);
			sellSide = cancelOrder(sellSide);
		}
	}
	public LimitOrder cancelOrder(LimitOrder order){
		if(order == null) return null;
		//should be impossible if order isn't null
		if(esRouter == null) throw new IllegalStateException("Can't cancel as no quote router is established!");
		esRouter.cancelOrder(order);
		return null;
	}
	
	public void haltTrading(){
		cancelAll = true;
		playDefense = false;
		sellSide = null;
		buySide = null;
	}
	@Override
	public void newBar(Bar bar, Portfolio holdings, QuoteRouter executionEngine) {
		// TODO Auto-generated method stub
	}
	/**
	 * Defensive tick driving trading. 
	 * 1. enter distant limit
	 * ... limit hit, place stop up and down 1 tick from limit price (bracket)
	 */
	@Override
	public void tickPrice(String symbol, int field, double price,
			Portfolio holdings, QuoteRouter executionEngine) {
		if(esRouter == null) esRouter = executionEngine;
		
		if(cancelAll){
			cancelAll= false;
			executionEngine.cancelOpenOrders();
			return;
		}
		if(TickType.LAST == field) {
			lastPrice = price;
			dc.pushPrice(price);
			double r2 = dc.getRSquared();
			if(r2 > .5)
			System.out.println("Price " + price + "R^2 " + r2 + " Slope: " + dc.getSlope());
		}

		//TODO figure out better pricing for buy or sell
		if(playDefense){
			synchronized(this){
				if(buySide == null){
					buySide = createBuyOrder(price - .75);
					System.err.println(buySide);
					executionEngine.executeOrder(buySide);
				} else {
					if(defenseScratch(field, price, buySide)){
						updateStopOrderPrice(esRouter, buySide);
					}
				}
				if(sellSide == null){
					sellSide = createSellOrder(price + .75);
					System.err.println(sellSide);
					executionEngine.executeOrder(sellSide);			
				} else {
					if(defenseScratch(field, price, sellSide)){
						updateStopOrderPrice(esRouter, sellSide);
					}
				}
			}
		}
		
	}
	/**
	 * A place to try different scratch approaches.
	 * @param field
	 * @param price
	 * @param order
	 * @return
	 */
	private boolean defenseScratch(int field, double price,
			LimitOrder order) {
		//nothing to scratch
		if(! order.isConfirmed()) return false;
		return true;
	}
	/**
	 * Creates a limit order with an associated stop. No profit taker order as this is intended
	 * for directional trades.
	 * 
	 * @param field
	 * @param price
	 * @param executionEngine
	 */
	protected void createAndExecuteOrder() {
		//got a decent price
		LimitOrder order;
		if(upBias) {				
			order = createBuyOrder(lastPrice);
		}
		else{
			order = createSellOrder(lastPrice);
		}
		order.setProfitTaker(null);
		order.setMkt(mkt);
		executeOrder(esRouter, order);
	}
	/**
	 * Keep a stack of orders and try to correlate opens with closes. 
	 * Can be helpful with displaying stats.
	 * @param executionEngine
	 * @param limitOrder
	 */
	private synchronized void executeOrder(QuoteRouter executionEngine, LimitOrder limitOrder) {
		//special type of order - closing a previously opened directional trade
		//and thus need to remove the lingering stop order
		if(closePosition && !positionTrade.isEmpty()) {
			LimitOrder lastOpenPosition = positionTrade.pop();
			cancelStop(executionEngine, lastOpenPosition);
			limitOrder.setOnset(lastOpenPosition);
			trendClosed.add(limitOrder);
			limitOrder.setStopLoss(null);
		}
		else {
			positionTrade.push(limitOrder);
		}
		closePosition = false;
		executionEngine.executeOrder(limitOrder);
	}
	/**
	 * Cancel the stop on the original position order. On the new closingOrder set the onset to 
	 * be the original position order and be sure to clear out any stop that might be on the closing
	 * order (since we don't want a stop, this is the profit taker). 
	 * 
	 * @param executionEngine
	 * @param closingOrder
	 */
	private void cancelStop(QuoteRouter executionEngine, LimitOrder lastOpenPosition) {
		if(lastOpenPosition.getStopLoss() != null){
			executionEngine.cancelOrder(lastOpenPosition.getStopLoss());
			lastOpenPosition.setStopLoss(null);
		}
	}
	protected LimitOrder createSellOrder(double price) {
		LimitOrder sellOrder = new LimitOrder(contractQty, price , sell);
		// Put a safety net out
		LimitOrder stopLoss = new LimitOrder(contractQty, price + .5, buy);
		sellOrder.setStopLoss(stopLoss);

		LimitOrder profitTaker = new LimitOrder(contractQty, price - .25, buy);
		sellOrder.setProfitTaker(profitTaker);
		
		return sellOrder;
	}
	protected LimitOrder createBuyOrder(double price) {
		LimitOrder buyOrder = new LimitOrder(contractQty, price , buy);
		// Put a safety net out
		LimitOrder stopLoss = new LimitOrder(contractQty, price - .5, sell);
		buyOrder.setStopLoss(stopLoss);
		LimitOrder profitTaker = new LimitOrder(contractQty, price + .25, sell);
		buyOrder.setProfitTaker(profitTaker);
		
		return buyOrder;
	}
	synchronized void sleep(int time){
		 try {
			Thread.currentThread().sleep(time);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * Did the contingent orders execute
	 * @param order
	 * @param id
	 * @return
	 */
	final boolean theEnd(final LimitOrder order, final int id){
		if(order != null) {
			final LimitOrder stop = order.getStopLoss();
			final LimitOrder profit = order.getProfitTaker();
			if(stop != null && stop.getId() == id) {
				stop.setOnset(order);
				defenseClosed.add(stop);
				return true;
			}
			if(profit != null && profit.getId() == id) {
				profit.setOnset(order);
				defenseClosed.add(profit);
				return true;
			}
		}
		return false;
	}
	/**
	 * if playing defense and the contingent orders are complete empty out the order
	 */
	public void execDetails(Execution execution, Portfolio portfolio,
			QuoteRouter quoteRouter) {
		System.out.println("Executed " + execution.m_orderId + " " + execution.m_price);
		if(playDefense) {
			if(theEnd(buySide, execution.m_orderId)){
				buySide = null;
			}
			if(theEnd(sellSide, execution.m_orderId)){
				sellSide = null;
			}	
		}
	}
	/**
	 * @param order A filled order
	 * @param quoteRouter
	 */
	private void updateStopOrderPrice(QuoteRouter quoteRouter, LimitOrder order) {
		LimitOrder stopOrder = order.getStopLoss(); 
		stopOrder.setMkt(mkt);
		stopOrder.setOrderPrice(order.getPrice());
		//sleep(550);
		if(! stopOrder.isConfirmed())
			quoteRouter.executeOrder(stopOrder);
	}
	/**
	 * When submitting an order instead of using LMT use a mkt order
	 * @param selected
	 */
	public void setMkt(boolean selected) {
		mkt = selected;
		
	}
	public String getStats() {
		esRouter.getPortfolio().displayTradeStats("Defense", defenseClosed);
		esRouter.getPortfolio().displayTradeStats("Trend", trendClosed);
		return null;
	}

}
