package com.davehoag.ib.util;

import java.util.ArrayList;
import java.util.Iterator;
import org.slf4j.*;

import com.davehoag.ib.CassandraDao;
import com.davehoag.ib.ResponseHandler;
import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.OrderOnBook;
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
			if(order.isTriggered( bar.high, bar.low )){
				executed.add(order);
				client.fillOrder(order.orderId, order.lmtContract, order.lmtOrder);
			}
			else //If its a trailing order we need update the new lmt price
			if(order.isTrail()){
				order.updateTrailingLmtValue(bar.high, bar.low);
		
				//check if the swing could have caused an exit
				if( order.isTriggered(  bar.high, bar.low ) ){
					executed.add(order);
					client.fillOrder(order.orderId, order.lmtContract, order.lmtOrder);
				}
			}
		}
		restingOrders.removeAll(executed);
	}
	public boolean fillOrBookOrder(final int id, final Contract contract, final Order order){
		if(order.m_orderType.equals("LMT")){
			if( isExecutable(order.m_lmtPrice, order.m_action.equals("BUY"))){
				return true;
			}
			else{
				addLimitOrder(id, contract, order);
			}
		}
		else
		if( order.m_orderType.equals("TRAIL")){
			//Always book trail orders - they should be filled filled by check resting orders
			addLimitOrder(id, order);
		}
		return false;
	}
	/**
	 * Allow fake clients to be plugged in
	 * @param socket
	 */
	public void setClient(final HistoricalDataClient socket){
		client = socket;
	}
	public boolean isExecutable(final double price, final boolean isBuy){
		return isExecutable(price, isBuy, lastBar.close);
	}
	public boolean isExecutable(final double price, final boolean isBuy, final double mktPrice){
		if(price >= mktPrice && isBuy) return true;
		if(price <= mktPrice && !isBuy) return true;
		return false;
	}
	public void addLimitOrder(final int id, final Order order){
		addLimitOrder(id, contract, order);
	}
	public synchronized void addLimitOrder(final int id, final Contract lmtContract, final Order order){
		LoggerFactory.getLogger("Trading").info( "booking order!" );
		restingOrders.add( getOrderOnBook(id, lmtContract, order));
	}
	OrderOnBook getOrderOnBook(final int id, final Contract lmtContract, final Order order){
		return new OrderOnBook(id, lmtContract, order, lastBar.close);
	}
	
}
