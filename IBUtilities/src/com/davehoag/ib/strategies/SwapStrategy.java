package com.davehoag.ib.strategies;

import java.util.HashMap;
import java.util.Map.Entry;

import com.davehoag.ib.QuoteRouter;
import com.davehoag.ib.Strategy;
import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.LimitOrder;
import com.davehoag.ib.dataTypes.Portfolio;
import com.davehoag.ib.dataTypes.SimpleMovingAvg;
import com.davehoag.ib.util.HistoricalDateManipulation;

public class SwapStrategy implements Strategy {
	int intervalSize = 12;
	HashMap<String, SimpleMovingAvg> trends = new HashMap<String, SimpleMovingAvg>();
	Double oneDelta;
	Bar oneBar;

	@Override
	public void newBar(final Bar bar, final Portfolio holdings, final QuoteRouter executionEngine) {
		// TODO the execution engine is tis the quot router for one symbol,
		// can't be for two
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
	protected void considerTrading(Bar bar, Portfolio holdings, QuoteRouter executionEngine) {

		for (Entry<String, SimpleMovingAvg> trend : trends.entrySet()) {
			if (oneDelta == null && trend.getKey().equals(bar.symbol)) {
				oneDelta = trend.getValue().getSlowChange();
				oneBar = bar;
				break;
			}
			// Ignore the entry that also happens to the one bar source
			if (!oneBar.symbol.equals(trend.getKey())) {
				consider(oneBar, bar, holdings, executionEngine,
						(oneDelta > trend.getValue().getSlowChange()));
				oneDelta = null;
				oneBar = null;
			}
		}
	}

	private void consider(Bar oneBar, Bar bar, Portfolio holdings, QuoteRouter executionEngine,
			boolean oneOverTwo) {
		Bar betterReturn = bar;
		Bar lowerReturn = oneBar;
		if (oneOverTwo) {
			betterReturn = oneBar;
			lowerReturn = bar;
		}

		int currentQty = holdings.getShares(betterReturn.symbol);
		// Need to swap if current holdings are zero
		if (currentQty == 0) {
			sellExistingPosition(lowerReturn, holdings, executionEngine);
			// need a way to wait for confirmation
			openNewLongPosition(betterReturn, holdings, executionEngine);
		}
	}

	/**
	 * @param port
	 * @param best
	 */
	protected void openNewLongPosition(final Bar latestBar, final Portfolio port,
			final QuoteRouter executionEngine) {
		final double money = port.getCash();
		final double qtyD = Math.floor(money / (latestBar.close * 100));

		final int buyQty = (int) (qtyD * 100);
		final LimitOrder order = new LimitOrder(latestBar.symbol, buyQty, latestBar.close, true);
		QuoteRouter exe = executionEngine.getRequester().getQuoteRouter(latestBar.symbol);
		exe.executeOrder(order);
	}

	/**
	 * Will block until order is confirmed
	 * 
	 * @param port
	 * @param worse
	 */
	protected void sellExistingPosition(final Bar latestBar, final Portfolio port,
			final QuoteRouter executionEngine) {
		int priorQty = port.getShares(latestBar.symbol);

		if (priorQty != 0) { // sell the losing shares
			final LimitOrder order = new LimitOrder(latestBar.symbol, priorQty, latestBar.close, false);
			QuoteRouter exe = executionEngine.getRequester().getQuoteRouter(latestBar.symbol);
			exe.executeOrder(order);
			// TODO spin loop less than ideal
			while (!order.isConfirmed())
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
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

}
