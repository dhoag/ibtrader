package com.davehoag.ib.strategies;

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

		int buyQty = qty;
		if (maxQty) {
			final double money = port.getCash();
			final double qtyD = Math.floor(money / (price * 100));

			buyQty = (int) (qtyD * 100);
		}
		final LimitOrder order = new LimitOrder(symbol, buyQty, price, true);
		QuoteRouter exe = executionEngine.getRequester().getQuoteRouter(symbol);
		exe.executeOrder(order);
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
			final LimitOrder order = new LimitOrder(symbol, priorQty, price, false);
			QuoteRouter exe = executionEngine.getRequester().getQuoteRouter(symbol);
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

}