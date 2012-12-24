package com.davehoag.ib.strategies;

import java.util.Iterator;

import org.slf4j.LoggerFactory;

import com.davehoag.ib.QuoteRouter;
import com.davehoag.ib.Strategy;
import com.davehoag.ib.dataTypes.LimitOrder;
import com.davehoag.ib.dataTypes.Portfolio;

public abstract class AbstractStrategy implements Strategy {
	boolean maxQty;
	int qty = 100;

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

		if (priorQty != 0) { // sell the losing shares
			LimitOrder original = getOnsetTrade(symbol, port);
			final LimitOrder order = new LimitOrder(symbol, priorQty, price, false);
			QuoteRouter exe = executionEngine.getRequester().getQuoteRouter(symbol);
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
				LoggerFactory.getLogger("Strategy").error(
						"There should be only one open trade to offset when selling existing positions");
		}
		else {
			LoggerFactory.getLogger("Strategy").error(
					"There should be an open trade to offset when selling existing positions");
		}
		return original;
	}

}