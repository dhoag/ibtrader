package com.davehoag.ib.strategies;

import org.slf4j.LoggerFactory;

import com.davehoag.ib.Strategy;
import com.davehoag.ib.TradingStrategy;
import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.LimitOrder;
import com.davehoag.ib.dataTypes.Portfolio;
import com.davehoag.ib.dataTypes.SimpleMovingAvg;
import com.davehoag.ib.tools.LaunchTrading;
import com.davehoag.ib.util.HistoricalDateManipulation;
/**
 * @author dhoag
 */
public class TrailingExitsStrategy implements Strategy {
	SimpleMovingAvg sma;
	SimpleMovingAvg smaTrades;
	int qty = 100;
	int fastMovingAvg = 18;
	int slowMovingAvg = 21;
	boolean useEma = true;
	boolean requireTradeConfirmation = false;

	public TrailingExitsStrategy(){	
		init(null);
	}
	/**
	 * 
	 * @param seeds
	 */
	public void init( final double [] seeds){
		sma = new SimpleMovingAvg(fastMovingAvg, slowMovingAvg, seeds);
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
	public void newBar(final Bar bar ,final Portfolio port, TradingStrategy executionEngine){		
		smaTrades.newTick(bar.tradeCount);
		final boolean crossOverEvent = sma.newTick(bar.wap) ;
		final int holdings = port.getShares(bar.symbol);

		LimitOrder order = null;
		if( inTradeWindow(bar.originalTime) ) {
			//only trade if the # of trades is rising with the cross over
			if(crossOverEvent){
				if( (!requireTradeConfirmation || smaTrades.isTrendingUp()) && sma.isTrendingUp() && sma.recentJumpExceedsAverage()){
					LoggerFactory.getLogger("TrailingExits").debug(port.getTime() + " Open position " + bar.symbol + " " + qty);
					order = new LimitOrder(qty, bar.close + .02, true);

					LimitOrder stopLoss = new LimitOrder(qty, sma.getVolatilityPercent(), false);
					stopLoss.markAsTrailingOrder();
					order.setStopLoss(stopLoss);
					executionEngine.executeOrder(order);
				}
				else
				if( (!requireTradeConfirmation || smaTrades.isTrendingUp()) && ! sma.isTrendingUp() && sma.recentJumpExceedsAverage()){
					LoggerFactory.getLogger("TrailingExits").debug(port.getTime() + " Open short position " + bar.symbol + " " + qty);
					order = new LimitOrder(qty, bar.close + .02, false);

					LimitOrder stopLoss = new LimitOrder(qty, sma.getVolatilityPercent(), true);
					stopLoss.markAsTrailingOrder();
					order.setStopLoss(stopLoss);
					executionEngine.executeOrder(order);
				}

			}
		}
		else{
			if( holdings > 0) {
				executionEngine.cancelOpenOrders();
				 //end of the day, liquidate
				LoggerFactory.getLogger("TrailingExits").debug("Outside trading hours - Liquidate open position " + bar.symbol + " " + holdings);
				order = new LimitOrder(holdings, bar.close - .05, holdings  < 0);
			}
			else{
				sma.reset();
			}	
		}
	}
}
