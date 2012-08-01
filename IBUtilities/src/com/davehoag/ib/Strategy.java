package com.davehoag.ib;
import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.Portfolio;


public interface Strategy {

	public void newBar(Bar bar, Portfolio holdings, TradingStrategy executionEngine);
	
}
