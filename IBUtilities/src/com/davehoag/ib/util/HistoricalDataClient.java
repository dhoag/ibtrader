package com.davehoag.ib.util;

import java.net.ConnectException;
import java.util.HashMap;
import org.slf4j.LoggerFactory;

import me.prettyprint.hector.api.exceptions.HectorException;

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
	protected HashMap<String, HistoricalDataSender> mktDataFeed = new HashMap<String, HistoricalDataSender>();
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
				HistoricalDataSender sender = new HistoricalDataSender(reqId, stock, rh, HistoricalDataClient.this);
				mktDataFeed.put(stock.m_symbol, sender);
				try{
					sender.sendData();
				}
				catch(Throwable t){
					LoggerFactory.getLogger("Backtesting").error( "Failure running data for " + stock.m_symbol);
					t.printStackTrace();
					System.exit(1);
				}
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
	public void placeOrder(final int id, final Contract contract, final Order order){
		final HistoricalDataSender sender = mktDataFeed.get(contract.m_symbol);
		if( sender!=null && order.m_orderType.equals("LMT")){
			if(sender.isExecutable(order.m_lmtPrice, order.m_action)){
				fillOrder(id, contract, order);
			}
			else{
				LoggerFactory.getLogger("Trading").info( "booking order!" );
				sender.addLimitOrder(id, contract, order);
			}
		}
		else
		if(sender != null && order.m_orderType.equals("TRAIL")){
			final double price = sender.getLimitPrice(order.m_action.equals("BUY"), order.m_percentOffset);
			if(sender.isExecutable(price, order.m_action)){
				fillOrder(id, contract, order);
			}
			else{
				order.m_lmtPrice = price;
				sender.addLimitOrder(id, order);
			}
		}
		else{
			fillOrder(id, contract, order);
		}
	}
	/**
	 * @param id
	 * @param contract
	 * @param order
	 */
	protected void fillOrder(int id, Contract contract, Order order) {
		Execution execution = new Execution();
		execution.m_side = order.m_action;
		execution.m_orderId = id;
		execution.m_price = order.m_lmtPrice;
		execution.m_shares = order.m_totalQuantity;
		rh.execDetails(id, contract, execution);
	}
	@Override
	public void reqAccountUpdates(final boolean keepGetting, final String accountName){
		//Do nothing
	}
}
