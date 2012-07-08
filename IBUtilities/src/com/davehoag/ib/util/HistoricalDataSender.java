package com.davehoag.ib.util;

import java.util.ArrayList;
import java.util.Iterator;
import org.slf4j.*;

import com.davehoag.ib.CassandraDao;
import com.davehoag.ib.ResponseHandler;
import com.davehoag.ib.dataTypes.Bar;
import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.TickType;
/**
 * Connect to Cassandra and simulate the realtime bars wit the historical data
 * @author dhoag
 *
 */
public class HistoricalDataSender {
	final static String defaultHistoricalDataBarSize = "bar5sec";
	final static int daysToBackTest = 3;
	
	final int reqId;
	final Contract contract;
	final ResponseHandler handler;
	HistoricalDataClient client;
	ArrayList<OrderOnBook> restingOrders = new ArrayList<OrderOnBook>();
	Bar lastBar;
	
	public HistoricalDataSender(final int id, final Contract stock, final ResponseHandler rh, HistoricalDataClient sock){
		reqId = id;
		contract = stock;
		handler = rh;
		client = sock;
	}
	public void sendData() {
		CassandraDao dao = new CassandraDao();

    	LoggerFactory.getLogger("MarketData").info(  "Sending "  + contract.m_symbol +  " going back " + daysToBackTest);

		Iterator<Bar> data = dao.getData(contract.m_symbol, daysToBackTest, 0, defaultHistoricalDataBarSize);
		//Each bar represents a forward looking 5 second period - thus the first first time is 8:30 and last is 2:55:55
		//TODO To simulate realtime data need to make up some data points to send over tickXyz
		//prior to sending bar
		while(data.hasNext()){
			final Bar bar = data.next();
			checkRestingOrders(bar);
			lastBar = bar;
			handler.tickPrice(reqId, TickType.LAST, bar.open, 0);
			handler.tickPrice(reqId, TickType.LAST, bar.high, 0);
			handler.tickPrice(reqId, TickType.LAST, bar.low, 0);
			handler.tickPrice(reqId, TickType.LAST, bar.close, 0);
			handler.realtimeBar(reqId, bar.originalTime, bar.open, bar.high, bar.low, bar.close, bar.volume, bar.wap, bar.tradeCount);
		}
	}
	protected synchronized void checkRestingOrders(final Bar bar){
		final ArrayList<OrderOnBook> executed = new ArrayList<OrderOnBook>();
		for(OrderOnBook order: restingOrders){
			final boolean isBuy = order.lmtOrder.m_action.equals("BUY");
			final double mktPrice = isBuy ? bar.low : bar.high;
			if(isExecutable(order.lmtOrder.m_lmtPrice, order.lmtOrder.m_action, mktPrice)){
				executed.add(order);
				client.fillOrder(order.orderId, order.lmtContract, order.lmtOrder);
			}
			else
			if(order.lmtOrder.m_orderType.equals("TRAIL")){
				//the "lmtPrice" was set during placement of order
				//update it since it didn't fill in the prior if block
				order.lmtOrder.m_lmtPrice = getLimitPrice(isBuy, order.lmtOrder.m_percentOffset, mktPrice);
			}
		}
		restingOrders.removeAll(executed);
	}
	/**
	 * Allow fake clients to be plugged in
	 * @param socket
	 */
	public void setClient(final HistoricalDataClient socket){
		client = socket;
	}
	public double getLimitPrice(final boolean buy, final double percentageOffset){
		return getLimitPrice( buy, percentageOffset, lastBar.close);
	}
	public double getLimitPrice(final boolean buy, final double percentageOffset, final double referencePrice){
		return buy  ? referencePrice  * (1* + percentageOffset) : referencePrice * ( 1- percentageOffset);
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
	public synchronized void addLimitOrder(final int id, final Contract lmtContract, final Order order){
		final OrderOnBook lmtOrder = new OrderOnBook(id, lmtContract, order);
		restingOrders.add(lmtOrder);
	}
	class OrderOnBook{
		int orderId;
		Contract lmtContract;
		Order lmtOrder;
		OrderOnBook(int id, Contract c, Order o){
			orderId = id;
			lmtContract = c;
			lmtOrder = o;
		}
	}
	
}
