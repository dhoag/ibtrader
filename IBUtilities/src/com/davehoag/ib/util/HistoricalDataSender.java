package com.davehoag.ib.util;

import com.davehoag.ib.ResponseHandler;
import com.ib.client.Contract;
/**
 * Connect to Cassandra and simulate the realtime bars wit the historical data
 * @author dhoag
 *
 */
public class HistoricalDataSender {
	final int reqId;
	final Contract contract;
	final ResponseHandler handler;
	public HistoricalDataSender(final int id, final Contract stock, final ResponseHandler rh){
		reqId = id;
		contract = stock;
		handler = rh;
	}
	public void sendData(){
		
	}
}
