package com.davehoag.ib.strategies;

import java.text.NumberFormat;

import com.davehoag.ib.QuoteRouter;
import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.Portfolio;

public class OutputQuotesStrategy extends AbstractStrategy {
	NumberFormat nf = NumberFormat.getCurrencyInstance();
	@Override
	public void newBar(Bar bar, Portfolio holdings, QuoteRouter executionEngine) {
		// TODO Auto-generated method stub

	}

	@Override
	public void tickPrice(String symbol, int field, double price,
			Portfolio holdings, QuoteRouter executionEngine) {
		System.out.println("Sym:" + symbol + " " + field + " " + nf.format(price));

	}

}
