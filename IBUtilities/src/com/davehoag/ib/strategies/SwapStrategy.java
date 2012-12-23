package com.davehoag.ib.strategies;

import java.util.HashMap;
import java.util.Map.Entry;

import com.davehoag.ib.QuoteRouter;
import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.Portfolio;
import com.davehoag.ib.dataTypes.SimpleMovingAvg;
import com.davehoag.ib.util.HistoricalDateManipulation;

/**
 * Look at two symbols performance over the past period of N 5 second bars.
 * Enter a position for which ever symbol is performing better.
 * 
 * @author David Hoag
 * 
 */
public class SwapStrategy extends AbstractStrategy {
	int intervalSize = 12;
	HashMap<String, SimpleMovingAvg> trends = new HashMap<String, SimpleMovingAvg>();
	Double oneDelta;
	Bar oneBar;

	@Override
	public void newBar(final Bar bar, final Portfolio holdings, final QuoteRouter executionEngine) {
		final SimpleMovingAvg avg = getTrends(bar.symbol);
		if (isInTradingWindow(bar) && avg.isInitialized()) {
			considerTrading(bar, holdings, executionEngine);
		}

		avg.newTick(bar.close);
	}

	/**
	 * 
	 * @param bar
	 * @param holdings
	 * @param executionEngine
	 */
	protected void considerTrading(final Bar bar, final Portfolio holdings, final QuoteRouter executionEngine) {
		if (HistoricalDateManipulation.isEndOfDay(bar.originalTime)) {
			if (holdings.getShares(bar.symbol) > 0) {
				System.out.println("Liquidate " + bar.symbol + " " + holdings.getShares(bar.symbol));
				sellExistingPosition(bar.symbol, bar.close, holdings, executionEngine);
			}

		} else
		for (Entry<String, SimpleMovingAvg> trend : trends.entrySet()) {
			if (oneDelta == null && trend.getKey().equals(bar.symbol)) {
				oneDelta = trend.getValue().getSlowChange();
				oneBar = bar;
				break;
			}
			// Ignore the entry that also happens to the one bar source
			if (!oneBar.symbol.equals(trend.getKey())) {
				double trendDelta = trend.getValue().getSlowChange();
				consider(oneBar, bar, holdings, executionEngine, (oneDelta > trendDelta));
				oneDelta = null;
				oneBar = null;
			}
		}
	}

	private void consider(final Bar oneBar, final Bar bar, final Portfolio holdings,
			final QuoteRouter executionEngine, final boolean oneOverTwo) {
		Bar betterReturn = bar;
		Bar lowerReturn = oneBar;
		if (oneOverTwo) {
			betterReturn = oneBar;
			lowerReturn = bar;
		}

		int currentQty = holdings.getShares(betterReturn.symbol);
		// Need to swap if current holdings are zero
		if (currentQty == 0) {
			sellExistingPosition(lowerReturn.symbol, lowerReturn.close, holdings, executionEngine);
			// need a way to wait for confirmation
			openNewLongPosition(betterReturn.symbol, betterReturn.close, holdings, executionEngine);
		}
	}

	/**
	 * if its an interval where we should trade
	 * 
	 * @param bar
	 * @return
	 */
	protected boolean isInTradingWindow(final Bar bar) {
		final long openTimeSecs = HistoricalDateManipulation.getOpen(bar.originalTime);
		if (bar.originalTime == openTimeSecs) {
			getTrends(bar.symbol).reset();
			return false;
		} else {
			final long diff = bar.originalTime - openTimeSecs;
			return (diff % intervalSize) == 0;
		}
	}
	/**
	 * 
	 * @param symbol
	 * @return
	 */
	public SimpleMovingAvg getTrends(final String symbol) {
		SimpleMovingAvg result = trends.get(symbol);
		if (result == null) {
			result = new SimpleMovingAvg(intervalSize, intervalSize + 1);
			trends.put(symbol, result);
		}
		return result;
	}

	@Override
	public String getBarSize() {
		return "bar5sec";
	}

	@Override
	public void init(String parms) {
		maximizeQty();
		try{
			intervalSize = Integer.parseInt(parms);
		} catch (NumberFormatException ex) {
			ex.printStackTrace();
		}
		
	}

	@Override
	public void tickPrice(String symbol, int field, double price, Portfolio holdings, QuoteRouter executionEngine) {
		// TODO Auto-generated method stub
		
	}
}
