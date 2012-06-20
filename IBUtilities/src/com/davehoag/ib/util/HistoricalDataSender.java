package com.davehoag.ib.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.davehoag.ib.CassandraDao;
import com.davehoag.ib.ResponseHandler;
import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.LimitOrder;
import com.ib.client.Contract;
import com.ib.client.EClientSocket;
import com.ib.client.Order;
/**
 * Connect to Cassandra and simulate the realtime bars wit the historical data
 * @author dhoag
 *
 */
public class HistoricalDataSender {
	final int reqId;
	final Contract contract;
	final ResponseHandler handler;
	HistoricalDataClient client;
	ArrayList<LimitOrder> restingOrders = new ArrayList<LimitOrder>();
	Bar lastBar;
	
	public HistoricalDataSender(final int id, final Contract stock, final ResponseHandler rh, HistoricalDataClient sock){
		reqId = id;
		contract = stock;
		handler = rh;
		client = sock;
	}
	public void sendData() {
		CassandraDao dao = new CassandraDao();
		try { 
			Iterator<Bar> data = dao.getData(contract.m_symbol);
			while(data.hasNext()){
				final Bar bar = data.next();
				checkRestingOrders(bar);
				lastBar = bar;
				handler.realtimeBar(reqId, bar.originalTime, bar.open, bar.high, bar.low, bar.close, bar.volume, bar.wap, bar.tradeCount);
			}
		}
		catch(Throwable t){
			Logger.getLogger("Backtesting").log(Level.SEVERE, "Failure running data for " + contract.m_symbol);
			t.printStackTrace();
		}
	}
	protected void checkRestingOrders(final Bar bar){
		ArrayList<LimitOrder> executed = new ArrayList<LimitOrder>();
		for(LimitOrder order: restingOrders){
			double mktPrice = order.lmtOrder.m_action.equals("BUY") ? bar.low : bar.high;
			if(isExecutable(order.getPrice(), order.lmtOrder.m_action, mktPrice)){
				executed.add(order);
				client.fillOrder(order.orderId, order.lmtContract, order.lmtOrder);
			}
		}
		restingOrders.removeAll(executed);
	}
	/**
	 * Allow fake clients to be plugged in
	 * @param socket
	 */
	public void setClient(HistoricalDataClient socket){
		client = socket;
	}
	public boolean isExecutable(final double price, final String action){
		return isExecutable(price, action, lastBar.close);
	}
	public boolean isExecutable(final double price, final String action, final double mktPrice){
		if(price >= mktPrice && action.equals("BUY")) return true;
		if(price <= mktPrice && action.equals("SELL")) return true;
		return false;
	}
	public void addLimitOrder(final int id, final Order order){
		addLimitOrder(id, contract, order);
	}
	public void addLimitOrder(final int id, final Contract lmtContract, final Order order){
		LimitOrder lmtOrder = new LimitOrder(id, lmtContract, order);
		restingOrders.add(lmtOrder);
	}
	
}
