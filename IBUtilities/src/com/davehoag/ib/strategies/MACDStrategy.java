package com.davehoag.ib.strategies;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;

import com.davehoag.ib.QuoteRouter;
import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.LimitOrder;
import com.davehoag.ib.dataTypes.Portfolio;
import com.davehoag.ib.dataTypes.SimpleMovingAvg;
import com.davehoag.ib.util.HistoricalDateManipulation;

/**
 * Current design only supports 1 strategy per symbol. Need to route market data
 * not directly to a strategy but through a StrategyManager.
 * 
 * @author David Hoag
 * 
 */
public class MACDStrategy extends AbstractStrategy {
	SimpleMovingAvg sma;
	SimpleMovingAvg smaTrades;
	int qty = 100;
	int fastMovingAvg = 18;
	int slowMovingAvg = 21;
	boolean useEma = true;
	boolean requireTradeConfirmation = false;

	public MACDStrategy(){	
		init();
	}
	/**
	 * 
	 * @param seeds
	 */
	public void init() {
		sma = new SimpleMovingAvg(fastMovingAvg, slowMovingAvg, null);
		sma.setUseEmaForCrossOvers(useEma);
		smaTrades = new SimpleMovingAvg(fastMovingAvg, slowMovingAvg);
		smaTrades.setUseEmaForCrossOvers(true);
	}
	/**
	 * determine if this strategy likes this time of day for trading
	 * @return
	 */
	protected boolean inTradeWindow(final long time){
		final int hour = HistoricalDateManipulation.getHour(time);

		//don't trade the open or close
		return !HistoricalDateManipulation.isEndOfDay(time) && (hour > 9 & hour <= 14);
	}
	@Override
	public void newBar(final Bar bar ,final Portfolio port, QuoteRouter executionEngine){		
		smaTrades.newTick(bar.tradeCount);

		final boolean crossOverEvent = sma.newTick(bar.wap);
		final int holdings = port.getShares(executionEngine.getContract());

		if( inTradeWindow(bar.originalTime) ) {
			//only trade if the # of trades is rising with the cross over
			if (isTradeCondition(crossOverEvent)) {
					LogManager.getLogger("MACD").debug(port.getTime() + " Open position " + bar.symbol + " " + qty);
					LimitOrder buyOrder = new LimitOrder(qty, bar.close + .02, true);

					// Put a safety net out
					LimitOrder stopLoss = new LimitOrder(qty, bar.close - .08, false);
					buyOrder.setStopLoss(stopLoss);

					executionEngine.executeOrder(buyOrder);
				}
				else if(holdings > 0 && !sma.isTrendingUp()){
					executionEngine.cancelOpenOrders();
					LogManager.getLogger("MACD").debug(port.getTime() + " Close position " + bar.symbol + " " + qty);
					LimitOrder sellOrder = new LimitOrder(qty, bar.close -.02, false);
					executionEngine.executeOrder(sellOrder);
				}
		}
		else{
			if( holdings > 0) {
				executionEngine.cancelOpenOrders();
				 //end of the day, liquidate
				LogManager.getLogger("MACD").debug("Outside trading hours - Liquidate open position " + bar.symbol + " " + holdings);
				LimitOrder sellAll = new LimitOrder(holdings, bar.close - .05, false);
				executionEngine.executeOrder(sellAll);
			}
			else{
				sma.reset();
			}	
		}
	}
	/**
	 * @param crossOverEvent
	 * @return
	 */
	protected boolean isTradeCondition(final boolean crossOverEvent) {
		return crossOverEvent 
				&& ((!requireTradeConfirmation || smaTrades.isTrendingUp()) 
				&& sma.isTrendingUp()
				&& sma.isRecentlyUp());
	}
	@Override
	public String getBarSize() {
		return "bar5sec";
	}

	@Override
	public void init(String parms) {
		ArrayList<String> vals = super.getParms(parms);
		if (vals.size() == 2) {
			slowMovingAvg = Integer.parseInt(vals.get(0));
			fastMovingAvg = Integer.parseInt(vals.get(1));
		}
		init();
	}
	@Override
	public void tickPrice(String symbol, int field, double price, Portfolio holdings, QuoteRouter executionEngine) {
		// TODO Auto-generated method stub
	}
	@Override
	public double[] getStrategyData(final Bar aBar) {
		double[] data = new double[2];
		sma.newTick(aBar.wap);
		if (sma.isInitialized()) {
			data[0] = sma.getFastAvg();
			data[1] = sma.getSlowAvg();
		}
		else {
			data[0] = aBar.wap;
			data[1] = aBar.wap;
		}
		return data;
	}

}
