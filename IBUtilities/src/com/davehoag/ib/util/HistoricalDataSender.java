package com.davehoag.ib.util;

import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.LoggerFactory;

import com.davehoag.ib.CassandraDao;
import com.davehoag.ib.ResponseHandler;
import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.BarIterator;
import com.davehoag.ib.dataTypes.OrderOnBook;
import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.TickType;

/**
 * Connect to Cassandra and simulate the realtime bars with the historical data.
 * One instance per symbol. Coordinate timing of market data by enumerating
 * through each sender (symbol) for a particular strategy. This is also where
 * the order book for that symbol is maintained. Thus need one for each
 * simulated trading environment.
 * 
 * @author David Hoag
 * 
 */
public class HistoricalDataSender {
	final static String defaultHistoricalDataBarSize = "bar5sec";

	// will be changed somewhere, I realize this forces all back testing to go
	// back the same # of days
	public static int daysToBackTest = 3;

	int reqId;
	final Contract contract;
	ResponseHandler handler;
	HistoricalDataClient client;
	ArrayList<OrderOnBook> restingOrders;
	Bar lastBar;
	BarIterator data;
	double lastPrice;
	// Keep a cache around to enable reuse between strategy testing
	protected static HashMap<String, HistoricalDataSender> cache = new HashMap<String, HistoricalDataSender>();

	/**
	 * Initialize the HistoricalDataSender (create it or reset it).
	 * 
	 * @param id
	 * @param stock
	 * @param rh
	 * @param sock
	 * @return
	 */
	public static HistoricalDataSender initDataSender(final int id, final Contract stock,
			final ResponseHandler rh, final HistoricalDataClient sock) {
		HistoricalDataSender result = cache.get(stock.m_symbol);
		if (result == null) {
			result = new HistoricalDataSender(id, stock, rh, sock);
			result.init();
			cache.put(stock.m_symbol, result);
		} else {
			// resuse the sender - has the opportunity to eliminate a call to
			// Cassandra
			result.initCriticalValues(id, rh, sock);
		}
		return result;
	}
	public HistoricalDataSender(final Contract stock) {
		contract = stock;
	}

	/**
	 * If this is called the code is reusing the sender (outside of the time
	 * called in the constructor).
	 * 
	 * @param id
	 * @param rh
	 * @param sock
	 * @param result
	 */
	protected void initCriticalValues(final int id, final ResponseHandler rh, final HistoricalDataClient sock) {
		reqId = id;
		client = sock;
		handler = rh;
		restingOrders = new ArrayList<OrderOnBook>();
		if (data != null)
			data.reset();
	}

	public HistoricalDataSender(final int id, final Contract stock, final ResponseHandler rh,
			HistoricalDataClient sock) {
		this(stock);
		initCriticalValues(id, rh, sock);
	}

	/**
	 * Execute the Cassandra query to get the data.
	 */
	public void init() {
		data = CassandraDao.getInstance().getData(contract.m_symbol, daysToBackTest, 0,
				defaultHistoricalDataBarSize);
	}

	public boolean hasNext() {
		return data.hasNext();
	}

	/**
	 * Each bar represents a forward looking 5 second period - thus the first
	 * time is 8:30 and last is 2:55:55
	 */
	public void sendBar() {
		final Bar bar = data.next();
		checkRestingOrders(bar);
		lastBar = bar;
		lastPrice = bar.open;
		handler.tickPrice(reqId, TickType.LAST, bar.open, 0);
		lastPrice = bar.high;
		handler.tickPrice(reqId, TickType.LAST, bar.high, 0);
		lastPrice = bar.low;
		handler.tickPrice(reqId, TickType.LAST, bar.low, 0);
		lastPrice = bar.close;
		handler.tickPrice(reqId, TickType.LAST, bar.close, 0);
		handler.realtimeBar(reqId, bar.originalTime, bar.open, bar.high, bar.low, bar.close, bar.volume,
				bar.wap, bar.tradeCount);
	}

	/**
	 * 
	 * @param bar
	 */
	protected synchronized void checkRestingOrders(final Bar bar) {
		final ArrayList<OrderOnBook> executed = new ArrayList<OrderOnBook>();
		for (OrderOnBook order : restingOrders) {
			if (order.isTriggered(bar.high, bar.low)) {
				executed.add(order);
				LoggerFactory.getLogger("Trading").debug("Filling a booked limit order ");
				client.fillOrder(order.orderId, order.lmtContract, order.lmtOrder);
			} else // If its a trailing order we need update the new lmt price
			if (order.isTrail()) {
				order.updateTrailingLmtValue(bar.high, bar.low);
				// check if the swing could have caused an exit
				if (order.isTriggered(bar.high, bar.low)) {
					executed.add(order);
					LoggerFactory.getLogger("Trading").debug("Filling a booked trailing limit order ");
					client.fillOrder(order.orderId, order.lmtContract, order.lmtOrder);
				}
			}
		}
		restingOrders.removeAll(executed);
	}

	/**
	 * 
	 * @param id
	 * @param contract
	 * @param order
	 * @return
	 */
	public boolean fillOrBookOrder(final int id, final Contract contract, final Order order) {
		if (order.m_orderType.equals("LMT")) {
			if (isExecutable(order.m_lmtPrice, order.m_action.equals("BUY"))) {
				return true;
			} else {
				addLimitOrder(id, contract, order);
			}
		}
		else
			if (order.m_orderType.equals("TRAIL")) {
			// Always book trail orders - they should be filled filled by check
			// resting orders
			addLimitOrder(id, order);
		}
			else
				if ("STPLMT".equals(order.m_orderType)) {
					addLimitOrder(id, order);
				}
		return false;
	}

	/**
	 * Allow fake clients to be plugged in
	 * 
	 * @param socket
	 */
	public void setClient(final HistoricalDataClient socket) {
		client = socket;
	}

	public boolean isExecutable(final double price, final boolean isBuy) {
		return isExecutable(price, isBuy, lastPrice);
	}

	public boolean isExecutable(final double price, final boolean isBuy, final double mktPrice) {
		if (price >= mktPrice && isBuy)
			return true;
		if (price <= mktPrice && !isBuy)
			return true;
		return false;
	}

	public void addLimitOrder(final int id, final Order order) {
		addLimitOrder(id, contract, order);
	}

	public synchronized void addLimitOrder(final int id, final Contract lmtContract, final Order order) {
		LoggerFactory.getLogger("Trading").info("booking order!");
		restingOrders.add(getOrderOnBook(id, lmtContract, order));
	}

	OrderOnBook getOrderOnBook(final int id, final Contract lmtContract, final Order order) {
		return new OrderOnBook(id, lmtContract, order, lastBar.close);
	}

}
