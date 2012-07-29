package com.davehoag.ib.dataTypes;

public class SimpleRiskLimits implements RiskLimits {
	boolean allowRebuys = true;
	
	@Override
	public boolean acceptTrade(boolean buy, int qty, Portfolio port, double price) {
		return true;
	}

	/**
	 * TODO eventually let the risk strategy set hard exit stops
	 */
	@Override
	public boolean liquidateLong() {
		// TODO Auto-generated method stub
		return false;
	}

}
