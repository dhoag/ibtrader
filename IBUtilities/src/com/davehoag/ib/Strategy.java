package com.davehoag.ib;
import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.Portfolio;


public interface Strategy {

	/**
	 * Called by every 5 second real-time bar. The "Execution Engine" will be used to
	 * execute buy & sell orders.
	 * The holdings portfolio will be updated by the strategy or consulted by the strategy
	 * to make decisions about trading.
	 * 
	 * @param bar
	 * @param holdings
	 * @param executionEngine
	 */
	public void newBar(Bar bar, Portfolio holdings, QuoteRouter executionEngine);

	public String getBarSize();

	public void init(String parms);
	
}
