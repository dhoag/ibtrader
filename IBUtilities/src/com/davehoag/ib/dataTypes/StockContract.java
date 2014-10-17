package com.davehoag.ib.dataTypes;

import com.ib.client.Contract;
/**
 * Simplified form of "Contract" 
 * @author dhoag
 *
 */
public class StockContract extends Contract {
	public final static String TYPE = "STK";
	   public StockContract(final String symbol) {
	      m_symbol = symbol;
	      m_secType = TYPE;
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
			if(obj != this) {
				if(obj.getClass() == Contract.class){
					final Contract aContract = (Contract)obj;
					return aContract.m_symbol.equals(m_symbol) & aContract.m_secType == TYPE;
				}
				return obj.toString().equals(toString());
			}
			return true;
	   }
	   public String getIdentifier(){
		   return m_secType + '.' + m_symbol;
	   }

	@Override
	public String toString() {
		return m_symbol;
	}
}