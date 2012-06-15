package com.davehoag.ib;

import com.davehoag.ib.util.HistoricalDataClient;
import com.ib.client.Contract;
import com.ib.client.EClientSocket;
import com.ib.client.Execution;
import com.ib.client.Order;
/**
 * 
 * @author Dave Hoag
 *
 */
public class TestClientMock extends HistoricalDataClient {
	public TestClientMock(ResponseHandler anyWrapper) {
		super(anyWrapper);
		rh = anyWrapper;
	}
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
