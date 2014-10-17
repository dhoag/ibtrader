package com.davehoag.ib.strategies;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;

import com.davehoag.ib.QuoteRouter;
import com.davehoag.ib.Strategy;
import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.LimitOrder;
import com.davehoag.ib.dataTypes.Portfolio;
import com.ib.client.Execution;
import com.ib.client.TickType;

public abstract class AbstractStrategy implements Strategy {
	boolean maxQty;
	int qty = 100;
	Portfolio port;

	String convertTickType(int field){
		String priceType;
		switch(field){
		case TickType.ASK: priceType = "Ask"; break;
		case TickType.BID: priceType = "Bid"; break;
		case TickType.LAST: priceType = "Last"; break;
		case TickType.OPEN: priceType = "Open"; break;
		case TickType.HIGH: priceType = "High"; break;
		case TickType.LOW: priceType = "Low"; break;
		case TickType.CLOSE: priceType = "Close"; break;
		default: priceType = "Other?";
		}
		return priceType;
	}
	@Override
	public Portfolio getPortfolio() {
		return port;
	}

	@Override
	public void setPortfolio(Portfolio p) {
		port = p;
	}

	public double[] getStrategyData(Bar aBar) {
		return new double[0];
	}
	public void maximizeQty() {
		maxQty = true;
	}

	public void setQty(final int q) {
		qty = q;
	}
	public AbstractStrategy() {
		super();
	}

	@Override
	public String getBarSize() {
		return "bar5sec";
	}

	@Override
	public void init(final String parms) {
	}

	protected ArrayList<String> getParms(final String ori) {
		ArrayList<String> result = new ArrayList<String>();
		int idx = ori.indexOf(',');
		String remaining = ori;
		while (idx > 0) {
			String val = remaining.substring(0, idx);
			result.add(val);
			remaining = remaining.substring(idx + 1);
			idx = remaining.indexOf(',');
		}
		result.add(remaining);
		return result;
	}
	/**
	 * @param port
	 * @param best
	 */
	protected void openNewLongPosition(final String symbol, final double price, final Portfolio port,
			final QuoteRouter executionEngine) {
		int priorQty = port.getShares(symbol);

		if (priorQty != 0)
			return; // already have long position

		final LimitOrder order = getBuyLimitOrder(symbol, price, port);
		QuoteRouter exe = executionEngine.getRequester().getQuoteRouter(symbol);
		exe.executeOrder(order);
	}

	/**
	 * @param symbol
	 * @param price
	 * @param port
	 * @return
	 */
	protected LimitOrder getBuyLimitOrder(final String symbol, final double price, final Portfolio port) {
		int buyQty = qty;
		if (maxQty) {
			final double money = port.getCash();
			final double qtyD = Math.floor(money / (price * 100));

			buyQty = (int) (qtyD * 100);
		}
		final LimitOrder order = new LimitOrder(symbol, buyQty, price, true);
		return order;
	}

	/**
	 * Will block until order is confirmed
	 * 
	 * @param port
	 * @param worse
	 */
	protected void sellExistingPosition(final String symbol, final double price, final Portfolio port,
			final QuoteRouter executionEngine) {
		int priorQty = port.getShares(symbol);

		if (priorQty != 0) {
			LimitOrder original = getOnsetTrade(symbol, port);
			final LimitOrder order = new LimitOrder(symbol, priorQty, price, false);
			final QuoteRouter exe = executionEngine.getRequester().getQuoteRouter(symbol);
			exe.executeOrder(order);
			// TODO spin loop less than ideal
			while (!order.isConfirmed())
				try {
					Thread.sleep(500);
					System.out.println("Waiting on position liquidation!! Shouldn't have to wait!");

				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			final LimitOrder stopLoss = original.getStopLoss();
			if (stopLoss != null) executionEngine.cancelOrder(stopLoss);
		}

	}

	/**
	 * @param symbol
	 * @param port
	 */
	protected LimitOrder getOnsetTrade(final String symbol, final Portfolio port) {
		LimitOrder original = null;
		Iterator<LimitOrder> onePosition = port.getPositionsToUnwind(symbol);
		if (onePosition.hasNext()) {
			original = onePosition.next();
			if (onePosition.hasNext())
				LogManager.getLogger("Strategy").error(
						"There should be only one open trade to offset when selling existing positions");
		}
		else {
			LogManager.getLogger("Strategy").error(
					"There should be an open trade to offset when selling existing positions");
		}
		return original;
	}
	public void execDetails(Execution execution, Portfolio portfolio,
			QuoteRouter quoteRouter) {
		// do nothing
	}
}