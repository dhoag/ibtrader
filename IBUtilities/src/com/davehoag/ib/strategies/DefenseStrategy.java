package com.davehoag.ib.strategies;

import com.davehoag.ib.QuoteRouter;
import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.LimitOrder;
import com.davehoag.ib.dataTypes.Portfolio;
import com.ib.client.Execution;
import com.ib.client.TickType;

public class DefenseStrategy extends AbstractStrategy {

	boolean on = false;
	boolean upBias = true;
	final boolean buy = true;
	final boolean sell = false;
	boolean closePosition = false;
	boolean playDefense = false;
	boolean cancelAll = false;
	int contractQty = 1;
	//Simple quote ladder with only two on each side
	LimitOrder buySide;
	LimitOrder sellSide;
	LimitOrder positionTrade;
	/**
	 * Enter a long position with a stop 1 tick below entry price
	 */
	public void goLong(){
		upBias = true;
		on = true;
	}
	/**
	 * Enter a short position with a stop 1 tick above entry price
	 */
	public void goShort(){
		upBias = false;
		on = true;
	}
	public void sellClose(){
		upBias = false;
		on = true;
		closePosition = true;
	}
	public void buyClose(){
		upBias = true;
		on = true;
		closePosition = true;
	}
	public void playDefense(){
		playDefense = true;
	}
	public void haltTrading(){
		playDefense = false;
		cancelAll = true;
		sellSide = null;
		buySide = null;
	}
	@Override
	public void newBar(Bar bar, Portfolio holdings, QuoteRouter executionEngine) {
		// TODO Auto-generated method stub
	}

	@Override
	public void tickPrice(String symbol, int field, double price,
			Portfolio holdings, QuoteRouter executionEngine) {
		boolean bidOrAsk = ( TickType.ASK == field || TickType.BID == field || TickType.LAST == field);
		if(cancelAll){
			cancelAll= false;
			executionEngine.cancelOpenOrders();
			return;
		}
		if( on & bidOrAsk ) { 
			createPosition(field, price, executionEngine);
		}
		if(playDefense) 
		synchronized(this){
			if(buySide == null){
				buySide = createBuyOrder(price - .75);
				System.err.println(buySide);
				executionEngine.executeOrder(buySide);
			}
			if(sellSide == null){
				sellSide = createSellOrder(price + .75);
				System.err.println(sellSide);
				executionEngine.executeOrder(sellSide);			
			}
		}
		
	}
	/**
	 * Creates a limit order with an associated stop. No profit taker order as this is intended
	 * for directional trades.
	 * 
	 * @param field
	 * @param price
	 * @param executionEngine
	 */
	protected void createPosition(int field, double price,	QuoteRouter executionEngine) {
		//got a decent price
		if( TickType.LAST == field){ 
			LimitOrder order;
			if(upBias) {				
				order = createBuyOrder(price);
			}
			else{
				order = createSellOrder(price);
			}
			order.setProfitTaker(null);
			executeOrder(executionEngine, order);
		}
	}
	/**
	 * @param executionEngine
	 * @param limitOrder
	 */
	private synchronized void executeOrder(QuoteRouter executionEngine, LimitOrder limitOrder) {
		if(on == false) return;
		//special type of order - closing a previously opened directional trade
		//and thus need to remove the lingering stop order
		if(closePosition & positionTrade != null && positionTrade.getStopLoss() != null){
			cancelStop(executionEngine, limitOrder);
		}
		positionTrade = limitOrder;
		executionEngine.executeOrder(limitOrder);
		//for now turn off after one order
		on= false;
	}
	/**
	 * Cancel the stop on the original position order. On the new closingOrder set the onset to 
	 * be the original position order and be sure to clear out any stop that might be on the closing
	 * order (since we don't want a stop, this is the profit taker). 
	 * 
	 * @param executionEngine
	 * @param closingOrder
	 */
	private void cancelStop(QuoteRouter executionEngine, LimitOrder closingOrder) {
		executionEngine.cancelOrder(positionTrade.getStopLoss());
		closingOrder.setStopLoss(null);
		closingOrder.setOnset(positionTrade);
		closePosition = false;
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
	public void execDetails(Execution execution, Portfolio portfolio,
			QuoteRouter quoteRouter) {
		System.out.println("Executed " + execution.m_orderId + " " + execution.m_price);
	}
	/**
	 * @param execution
	 * @param quoteRouter
	 */
	private void updateStopOrderPrice(QuoteRouter quoteRouter, LimitOrder order) {
		LimitOrder stopOrder = order.getStopLoss(); 
		stopOrder.setOrderPrice(order.getPrice());
		sleep(550);
		if(! stopOrder.isConfirmed())
			quoteRouter.executeOrder(stopOrder);
	}

}
