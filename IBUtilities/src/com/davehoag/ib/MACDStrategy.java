package com.davehoag.ib;

import com.davehoag.ib.dataTypes.SimpleMovingAvg;

/**
 * Current design only supports 1 strategy per symbol. Need to route market data not directly to a strategy
 * but through a StrategyManager.
 * @author dhoag
 *
 */
public class MACDStrategy extends ResponseHandlerDelegate {
	final String symbol;
	SimpleMovingAvg sma;
	public MACDStrategy(final String sym, final double [] seeds){
		symbol = sym;
		init(seeds);
	}
	public void init( final double [] seeds){
		sma = new SimpleMovingAvg(12, 65, seeds);
	}
	@Override
	public void realtimeBar(int reqId, long time, double open, double high,
			double low, double close, long volume, double wap, int count) {
		if(sma.newTick(close) ){
			//TRADE!!
		}
	}
}
