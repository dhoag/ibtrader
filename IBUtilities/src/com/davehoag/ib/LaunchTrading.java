package com.davehoag.ib;

import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ib.client.EClientSocket;

public class LaunchTrading {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String symbol = "SPY";
		ResponseHandler rh = new ResponseHandler();
		EClientSocket  m_client = new EClientSocket( rh );
		IBClientRequestExecutor clientInterface = new IBClientRequestExecutor(m_client, rh);
		clientInterface.connect();
		try{
			MACDStrategy strat = new MACDStrategy(symbol, null);
			clientInterface.reqRealTimeBars(symbol, strat);
			clientInterface.waitForCompletion();
		}
		catch(Throwable t){
			t.printStackTrace();
			System.exit(1);
		}
		finally { 
			clientInterface.close();
		}
        System.exit(0);

	}

}
