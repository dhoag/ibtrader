package com.davehoag.ib.util;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.davehoag.ib.CassandraDao;
import com.davehoag.ib.ResponseHandler;
import com.davehoag.ib.dataTypes.Bar;
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
	public void sendData() {
		CassandraDao dao = new CassandraDao();
		try { 
			Iterator<Bar> data = dao.getData(contract.m_symbol);
			while(data.hasNext()){
				Bar bar = data.next();
				handler.realtimeBar(reqId, bar.originalTime, bar.open, bar.high, bar.low, bar.close, bar.volume, bar.wap, bar.tradeCount);
			}
		}
		catch(Throwable t){
			Logger.getLogger("Backtesting").log(Level.SEVERE, "Failure running data for " + contract.m_symbol);
			t.printStackTrace();
		}
	}
}
