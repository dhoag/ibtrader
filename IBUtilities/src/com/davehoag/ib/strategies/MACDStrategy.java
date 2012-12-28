package com.davehoag.ib.strategies;

import java.util.ArrayList;

import org.slf4j.LoggerFactory;

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
	boolean requireTradeConfirmation = true;

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
		final boolean crossOverEvent = sma.newTick(bar.wap) ;
		final int holdings = port.getShares(bar.symbol);

		if( inTradeWindow(bar.originalTime) ) {
			//only trade if the # of trades is rising with the cross over
			if(crossOverEvent){
				if( (!requireTradeConfirmation || smaTrades.isTrendingUp()) && sma.isTrendingUp()){
					LoggerFactory.getLogger("MACD").debug(port.getTime() + " Open position " + bar.symbol + " " + qty);
					LimitOrder buyOrder = new LimitOrder(qty, bar.close + .02, true);

					// Put a safety net out
					LimitOrder stopLoss = new LimitOrder(qty, sma.getVolatilityPercent() * 2, false);
					stopLoss.markAsTrailingOrder();
					buyOrder.setStopLoss(stopLoss);

					executionEngine.executeOrder(buyOrder);
				}
				else if(holdings > 0 && !sma.isTrendingUp()){
					executionEngine.cancelOpenOrders();
					LoggerFactory.getLogger("MACD").debug(port.getTime() + " Close position " + bar.symbol + " " + qty);
					LimitOrder sellOrder = new LimitOrder(qty, bar.close -.02, false);
					executionEngine.executeOrder(sellOrder);
				}
			}
		}
		else{
			if( holdings > 0) {
				executionEngine.cancelOpenOrders();
				 //end of the day, liquidate
				LoggerFactory.getLogger("MACD").debug("Outside trading hours - Liquidate open position " + bar.symbol + " " + holdings);
				LimitOrder sellAll = new LimitOrder(holdings, bar.close - .05, false);
				executionEngine.executeOrder(sellAll);
			}
			else{
				sma.reset();
			}	
		}
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

}
