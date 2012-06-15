package com.davehoag.ib.util;

import com.davehoag.ib.IBConstants;
import com.davehoag.ib.ResponseHandler;
import com.ib.client.Contract;
import com.ib.client.EClientSocket;
import com.ib.client.Execution;
import com.ib.client.Order;

/**
 * A client that connects to our persistent store and simulates IB
 * @author dhoag
 *
 */
public class HistoricalDataClient extends EClientSocket {
	protected boolean connected;
	protected ResponseHandler rh;
	/**
	 * 
	 * @param anyWrapper
	 */
	public HistoricalDataClient(ResponseHandler anyWrapper) {
		super(anyWrapper);
		rh = anyWrapper;
	}
	@Override
    public void eConnect( String host, int port, int clientId) {
		connected = true;
    }
	@Override
    public boolean isConnected() { return connected; }
	/**
	 * Send historical data from the Cassandra store as "realtime" data. For now just sending 
	 * 5 second bars as that is RealTimeBar interval supported by IB
	 */
	@Override
	public void reqRealTimeBars(final int reqId, final Contract stock, final int barSize, final String barType, final boolean rthOnly){
		if(barSize != 5 ) throw new IllegalArgumentException("Only 5 second bars are supproted");
		final Runnable r = new Runnable() {
			public void run(){
				new HistoricalDataSender(reqId, stock, rh);
			}
		};
		new Thread(r).start();
	}
	@Override
    public void reqHistoricalData( final int tickerId, final Contract contract,
            final String endDateTime, final String durationStr,
            final String barSizeSetting, final String whatToShow,
            final int useRTH, final int formatDate) {
    }
	@Override
	public void eDisconnect(){ connected = false;}
	@Override
	public String TwsConnectionTime(){ return "MockClientTime " + System.currentTimeMillis(); }
	@Override
	public int serverVersion(){ return 100; }
	@Override
	public void placeOrder(int id, Contract contract, Order order){
		Execution execution = new Execution();
		execution.m_side = order.m_action;
		execution.m_orderId = id;
		execution.m_price = order.m_lmtPrice;
		execution.m_shares = order.m_totalQuantity;
		rh.execDetails(id, contract, execution);
	}
}
