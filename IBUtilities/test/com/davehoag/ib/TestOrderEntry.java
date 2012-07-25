package com.davehoag.ib;

import com.davehoag.ib.dataTypes.LimitOrder;
import com.ib.client.EClientSocket;

public class TestOrderEntry extends ResponseHandlerDelegate {

	public TestOrderEntry(IBClientRequestExecutor ibInterface) {
		super(ibInterface);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ResponseHandler rh = new ResponseHandler();
		EClientSocket  m_client = new EClientSocket( rh );
		IBClientRequestExecutor clientInterface = new IBClientRequestExecutor(m_client, rh);
		clientInterface.connect();
		LimitOrder order = new LimitOrder("QQQ", 100, 64.68, true);
		clientInterface.executeOrder( order, new TestOrderEntry(clientInterface));
	}

}
