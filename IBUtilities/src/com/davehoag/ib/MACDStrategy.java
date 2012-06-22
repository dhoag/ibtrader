package com.davehoag.ib;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Calendar;
import java.util.Date;

import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.LimitOrder;
import com.davehoag.ib.dataTypes.Portfolio;
import com.davehoag.ib.dataTypes.SimpleMovingAvg;
import com.davehoag.ib.dataTypes.StockContract;
import com.ib.client.Contract;
import com.ib.client.Execution;

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
	int fastMovingAvg = 12;
	int slowMovingAvg = 25;
	boolean useEma = false;
	boolean requireTradeConfirmation = true;

	public MACDStrategy(){	}
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
		final int hour = getHour(time);
		//don't trade the open or close
		return hour > 8 & hour < 14;
	}
	/**
	 * @param time
	 * @return
	 */
	protected int getHour(final long time) {
		final Date d = new Date(time*1000);
		final Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		final int hour = cal.get(cal.HOUR_OF_DAY);
		return hour;
	}
	public LimitOrder newBar(final Bar bar ,final Portfolio port){		
		smaTrades.newTick(bar.tradeCount);
		final boolean crossOverEvent = sma.newTick(bar.wap) ;
		final int holdings = port.getShares(bar.symbol);

		LimitOrder order = null;
		if( inTradeWindow(bar.originalTime) ) {
			//only trade if the # of trades is rising with the cross over
			if(crossOverEvent){
				if( smaTrades.isTrendingUp() && sma.isTrendingUp()){
					order = new LimitOrder(qty, bar.close + .05, true);
				}
				else if(!sma.isTrendingUp()){
					order = new LimitOrder(qty, bar.close -.05, false);
				}
			}
		}
		else{
			if( holdings > 0) {
				 order = new LimitOrder(holdings, bar.close - .05, false);
			}
			else{
				sma.reset();
			}	
		}
		return order;
	}
}
