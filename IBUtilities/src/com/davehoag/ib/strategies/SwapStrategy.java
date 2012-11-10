package com.davehoag.ib.strategies;

import com.davehoag.ib.QuoteRouter;
import com.davehoag.ib.Strategy;
import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.Portfolio;

public class SwapStrategy implements Strategy {

	@Override
	public void newBar(Bar bar, Portfolio holdings, QuoteRouter executionEngine) {
		// System.out.println(bar);

	}

	@Override
	public String getBarSize() {
		// TODO Auto-generated method stub
		return null;
	}

}
