package com.davehoag.ib;
import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.Portfolio;
import com.ib.client.Contract;
import com.ib.client.Execution;


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

	public void tickPrice(String symbol, int field, double price, Portfolio holdings, QuoteRouter executionEngine);

	public String getBarSize();

	public void init(String parms);

	public Portfolio getPortfolio();

	public void setPortfolio(Portfolio p);

	/**
	 * Used for showing relevant data (if any) on a chart
	 * 
	 * @param aBar
	 * @return
	 */
	public double[] getStrategyData(Bar aBar);

	public void execDetails(Execution execution, Portfolio portfolio,
			QuoteRouter quoteRouter);

	public void cancelOrder(int id, int errorCode, Portfolio portfolio, QuoteRouter quoteRouter);

	public void tickString(Contract m_contract, int tickType, String value, Portfolio portfolio,
			QuoteRouter quoteRouter);

	public void tickSize(Contract m_contract, int field, int size, Portfolio portfolio,
			QuoteRouter quoteRouter);

}
