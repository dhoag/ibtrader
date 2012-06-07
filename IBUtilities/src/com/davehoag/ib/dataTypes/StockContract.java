package com.davehoag.ib.dataTypes;

import com.ib.client.Contract;
/**
 * Simplified form of "Contract" 
 * @author dhoag
 *
 */
public class StockContract extends Contract {
	   public StockContract(final String symbol) {
	      m_symbol = symbol;
	      m_secType = "STK";
	      m_exchange = "SMART";
	      m_currency = "USD";
	   }
	   @Override
	   public int hashCode(){
		   return m_symbol.hashCode();
	   }
	   /**
	    * Eventually may eliminate this override, the IB Contract is extensive.
	    * Odd, they didn't override hashCode.
	    */
	   @Override
	   public boolean equals(Object obj){
		   return m_symbol.equals(obj);
	   }
	   public String getIdentifier(){
		   return m_secType + '.' + m_symbol;
	   }
}