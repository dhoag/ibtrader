package com.davehoag.ib;

import org.slf4j.LoggerFactory;

import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.LimitOrder;
import com.davehoag.ib.dataTypes.Portfolio;
import com.davehoag.ib.dataTypes.SimpleMovingAvg;
import com.davehoag.ib.util.HistoricalDateManipulation;
/**
 * Current design only supports 1 strategy per symbol. Need to route market data not directly to a strategy
 * but through a StrategyManager.
 * @author dhoag
 *
 */
public class MACDStrategy implements Strategy {
	SimpleMovingAvg sma;
	SimpleMovingAvg smaTrades;
	int qty = 100;
	int fastMovingAvg = 18;
	int slowMovingAvg = 21;
	boolean useEma = true;
	boolean requireTradeConfirmation = false;

	public MACDStrategy(){	
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
	public LimitOrder newBar(final Bar bar ,final Portfolio port){		
		smaTrades.newTick(bar.tradeCount);
		final boolean crossOverEvent = sma.newTick(bar.wap) ;
		final int holdings = port.getShares(bar.symbol);

		LimitOrder order = null;
		if( inTradeWindow(bar.originalTime) ) {
			//only trade if the # of trades is rising with the cross over
			if(crossOverEvent){
				if( (!requireTradeConfirmation || smaTrades.isTrendingUp()) && sma.isTrendingUp()){
					LoggerFactory.getLogger("MACD").debug(port.getTime() + "Open position " + bar.symbol + " " + qty);
					order = new LimitOrder(qty, bar.close + .05, true);
				}
				else if(holdings > 0 && !sma.isTrendingUp()){
					LoggerFactory.getLogger("MACD").debug(port.getTime() + "Close position " + bar.symbol + " " + qty);
					order = new LimitOrder(qty, bar.close -.05, false);
				}
			}
		}
		else{
			if( holdings > 0) {
				 //end of the day, liquidate
				LoggerFactory.getLogger("MACD").debug("Outside trading hours - Liquidate open position " + bar.symbol + " " + holdings);
				order = new LimitOrder(holdings, bar.close - .05, false);
			}
			else{
				sma.reset();
			}	
		}
		return order;
	}
}
