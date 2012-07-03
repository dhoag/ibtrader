package com.davehoag.ib.dataTypes;

public interface RiskLimits {
	public boolean acceptTrade(boolean buy, int qty, final Portfolio port, double price);
	public boolean liquidateLong();
}
