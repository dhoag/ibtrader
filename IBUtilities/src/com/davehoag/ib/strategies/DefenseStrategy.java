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
	}
	@Override
	public void newBar(Bar bar, Portfolio holdings, QuoteRouter executionEngine) {
		// TODO Auto-generated method stub
	}

	@Override
	public void tickPrice(String symbol, int field, double price,
			Portfolio holdings, QuoteRouter executionEngine) {
		boolean bidOrAsk = ( TickType.ASK == field || TickType.BID == field);
		if(cancelAll){
			cancelAll= false;
			executionEngine.cancelOpenOrders();
			return;
		}
		synchronized(this) {
			if( on & bidOrAsk ) { 
				createPosition(field, price, executionEngine);
			}
			if(playDefense) {
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
	}
	protected void createPosition(int field, double price,
			QuoteRouter executionEngine) {
		//got a decent price
		if(TickType.BID == field && upBias) {				
			LimitOrder buyOrder = createBuyOrder(price);
			buyOrder.setProfitTaker(null);
			if(closePosition){
				buyOrder.setStopLoss(null);
				buyOrder.setPrice(price + .25);
				closePosition = false;
			}
			positionTrade = buyOrder;
			executionEngine.executeOrder(buyOrder);
			//for now turn off after one order
			on= false;
		}
		else
			if(TickType.ASK == field & !upBias){
				
				LimitOrder sellOrder = createSellOrder(price);
				sellOrder.setProfitTaker(null);
				if(closePosition){
					sellOrder.setStopLoss(null);
					sellOrder.setPrice(price - .25);
					executionEngine.cancelOrder(positionTrade.getStopLoss());
					sellOrder.setOnset(positionTrade);
					closePosition = false;
				}
				positionTrade = sellOrder;
				executionEngine.executeOrder(sellOrder);
				//for now turn off after one order
				on= false;
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

	public void execDetails(Execution execution, Portfolio portfolio,
			QuoteRouter quoteRouter) {
		System.out.println("Executed " + execution.m_orderId);
		
		//Update the limit price to a scratch value
		if(buySide != null && buySide.getId() == execution.m_orderId){
			LimitOrder stopOrder = buySide.getStopLoss(); 
			stopOrder.setPrice(buySide.getPrice());
			quoteRouter.executeOrder(stopOrder);
		}
		if(sellSide != null && sellSide.getId() == execution.m_orderId){
			LimitOrder stopOrder = sellSide.getStopLoss(); 
			stopOrder.setPrice(sellSide.getPrice());
			quoteRouter.executeOrder(stopOrder);
		}
	}

}
