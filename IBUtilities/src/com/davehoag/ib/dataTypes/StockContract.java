package com.davehoag.ib.dataTypes;

import com.ib.client.Contract;

public class StockContract extends Contract {
	   public StockContract(final String symbol) {
	      m_symbol = symbol;
	      m_secType = "STK";
	      m_exchange = "SMART";
	      m_currency = "USD";
	   }
}