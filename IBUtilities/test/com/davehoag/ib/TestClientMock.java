package com.davehoag.ib;

import com.ib.client.Contract;
import com.ib.client.EClientSocket;
/**
 * 
 * @author Dave Hoag
 *
 */
public class TestClientMock extends EClientSocket {
	boolean connected;
	ResponseHandler rh;
	public TestClientMock(ResponseHandler anyWrapper) {
		super(anyWrapper);
		rh = anyWrapper;
	}
	@Override
    public void eConnect( String host, int port, int clientId) {
		connected = true;
    }
	@Override
    public boolean isConnected() { return connected; }
	@Override
    public void reqHistoricalData( int tickerId, Contract contract,
            String endDateTime, String durationStr,
            String barSizeSetting, String whatToShow,
            int useRTH, int formatDate) {
		//simulate to elements coming from the IB client
    	rh.historicalData(tickerId, endDateTime, 100, 105, 99, 101, 2020, 292, 102, false);
    	rh.historicalData(tickerId, endDateTime, -1, -1, -1, -1, -1, -1, -1, false);
    }
}
