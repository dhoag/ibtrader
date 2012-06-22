package com.davehoag.ib;
import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.LimitOrder;
import com.davehoag.ib.dataTypes.Portfolio;


public interface Strategy {

	public LimitOrder newBar(Bar bar, Portfolio holdings);
	
}
