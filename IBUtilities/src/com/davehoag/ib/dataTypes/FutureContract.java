package com.davehoag.ib.dataTypes;

import com.ib.client.Contract;

/**
 * Simplified form of "Contract"
 * 
 * @author dhoag
 * 
 */
public class FutureContract extends Contract {
	public final static String TYPE = "FUT";

	public FutureContract(final String symbol, String date) {
		m_symbol = symbol;
		m_secType = TYPE;
		m_exchange = "GLOBEX";
		m_currency = "USD";
		m_expiry = date;
	}

	@Override
	public int hashCode() {
		return m_symbol.hashCode();
	}

	/**
	 * Eventually may eliminate this override, the IB Contract is extensive.
	 * Odd, they didn't override hashCode.
	 */
	@Override
	public boolean equals(Object obj) {
		return m_symbol.equals(obj);
	}

	public String getIdentifier() {
		return m_secType + '.' + m_symbol + '.' + m_expiry;
	}

	@Override
	public String toString() {
		return getIdentifier();
	}
}