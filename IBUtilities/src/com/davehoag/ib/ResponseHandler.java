package com.davehoag.ib;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;

import com.davehoag.ib.dataTypes.Portfolio;
import com.davehoag.ib.dataTypes.StockContract;
import com.davehoag.ib.util.HistoricalDateManipulation;
import com.ib.client.CommissionReport;
import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.EWrapper;
import com.ib.client.Execution;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.TickType;
import com.ib.client.UnderComp;
/**
 * The one and only response handler registered with the client. Requests will 
 * register their own "response handler" (EWrapper interface) to which calls will
 * delegate.
 * 
 * @author dhoag
 *
 */
public class ResponseHandler implements EWrapper {

	IBClientRequestExecutor requester;
	Executor executorService = Executors.newFixedThreadPool(10);
	//There is only one per account
	Portfolio portfolio = new Portfolio();

	public Portfolio getPortfolio(){
		return portfolio;
	}
	public void setRequestor(final IBClientRequestExecutor req) {
		requester = req;
	}
	/**
	 * Allow the default 10 thread pool to be overridden
	 * @param exe
	 */
	public void setExecutorService(final Executor exe){
		executorService = exe;
	}

	@Override
	public void error(Exception e) {
		LogManager.getLogger("ResponseHandler").warn( "unknown source", e);
	}

	@Override
	public void error(String str) {
		LogManager.getLogger("ResponseHandler").warn( str);
	}

	@Override
	public void error(final int id, final int errorCode, final String errorMsg) {
		
		int [] informationalCodes = { 2104 ,2106 };
		boolean inf = false;
		for(int code: informationalCodes){
			if(code == errorCode){
				inf = true;
				break;
			}
		}
		if(inf)
			LogManager.getLogger("ResponseHandler").info( "[" + id + "]  "+ errorCode + " '" + errorMsg + "'");
		else {
			LogManager.getLogger("ResponseHandler").error( "[" + id + "]  ERROR:" + errorCode + " '" + errorMsg + "'");
			switch(errorCode ){
			case 326: //Unable to connect
			case 504: //Not connected
			case 2105: //Historical Market Data Service is stopped.
				requester.close();
				System.exit(errorCode);
			}
		}
		
		if(id > 0){//TODO figure out if I should move the "endRequest" to the delegate. 
			final ResponseHandlerDelegate ew = requester.getResponseHandler(id);
			if(ew != null) ew.error(id, errorCode, errorMsg);
			requester.endRequest(id);
		}
	}
	/**
	 * Told by client that we are closed
	 */
	@Override
	public void connectionClosed() {
		portfolio.displayValue();
		requester.forcedClose();
	}

	@Override
	public void tickPrice(final int tickerId, final int field, final double price,
			final int canAutoExecute) {
		final Runnable r = new Runnable(){
			@Override
			public void run(){
				EWrapper ew = requester.getResponseHandler(tickerId);
				
				if(ew != null) ew.tickPrice(tickerId, field, price, canAutoExecute);
				else LogManager.getLogger("Trading").warn( "[" + tickerId + "] Received tickPrice " + TickType.getField(field)+ " @" +  price + " but no delegate registered");
			}
		};
		executorService.execute(r);
	}

	@Override
	public void tickSize(final int tickerId, final int field, final int size) {
		final Runnable r = new Runnable(){
			@Override
			public void run(){
				EWrapper ew = requester.getResponseHandler(tickerId);
				if(ew != null) ew.tickSize(tickerId, field, size);
				else LogManager.getLogger("Trading").warn( "[" + tickerId + "] Received tickSize " + TickType.getField(field)+ " " +  size + " but no delegate registered");
			}
		};
		executorService.execute(r);
	}
	@Override
	public void tickOptionComputation(final int tickerId, final int field,
			final double impliedVol, final double delta, final double optPrice,
			final double pvDividend, final double gamma, final double vega, final double theta,
			final double undPrice) {
		final Runnable r = new Runnable(){
			@Override
			public void run(){
				EWrapper ew = requester.getResponseHandler(tickerId);
				
				if(ew != null) ew.tickOptionComputation(tickerId, field, impliedVol, delta, optPrice, pvDividend, gamma, vega, theta, undPrice);
				else LogManager.getLogger("Trading").warn( "[" + tickerId + "] Received tickOptionComp " + TickType.getField(field) + " but no delegate registered");
			}
		};
		executorService.execute(r);
	}
	@Override
	public void tickGeneric(final int tickerId, final int tickType, final double value) {
		final Runnable r = new Runnable(){
			@Override
			public void run(){
				EWrapper ew = requester.getResponseHandler(tickerId);
				if(ew != null) ew.tickGeneric(tickerId, tickType, value);
				else LogManager.getLogger("Trading").warn( "[" + tickerId + "] Received tickGeneric " + TickType.getField(tickType)+ " " +  value+ " but no delegate registered");
			}
		};
		executorService.execute(r);
	}

	@Override
	public void tickString(final int tickerId, final int tickType, final String value) {
		final Runnable r = new Runnable(){
			@Override
			public void run(){
				
				EWrapper ew = requester.getResponseHandler(tickerId);
				
				if(ew != null) ew.tickString(tickerId, tickType, value);
				else LogManager.getLogger("Trading").warn( "[" + tickerId + "] Received tickString " + TickType.getField(tickType)+ " " +  value+ " but no delegate registered");
			}
		};
		executorService.execute(r);
	}

	@Override
	public void tickEFP(int tickerId, int tickType, double basisPoints,
			String formattedBasisPoints, double impliedFuture, int holdDays,
			String futureExpiry, double dividendImpact, double dividendsToExpiry) {
		// TODO Auto-generated method stub

	}

	@Override
	public void orderStatus(int orderId, String status, int filled,
			int remaining, double avgFillPrice, int permId, int parentId,
			double lastFillPrice, int clientId, String whyHeld) {

		getPortfolio().orderStatus(orderId, status, filled, remaining, avgFillPrice, permId, parentId,
				lastFillPrice, clientId, whyHeld);

	}
	@Override
	public void position(String account, Contract contract, int pos) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void positionEnd() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void accountSummary(int reqId, String account, String tag, String value, String currency) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void accountSummaryEnd(int reqId) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void openOrder(final int orderId, final Contract contract, final Order order,
			final OrderState orderState) {
		Runnable r = new Runnable(){
			@Override
			public void run(){ 
				//delegate to the registered handler
				//TODO figure out the response handler based on orderid
				final ResponseHandlerDelegate ew = requester.getResponseHandler(orderId);
				if(ew != null) ew.openOrder(orderId, contract, order, orderState);
				else LogManager.getLogger("RealTimeBar").warn( "[" + orderId + "] Reveived openOrder but no delegate registered " );
			};
		};
		executorService.execute(r);

	}

	@Override
	public void openOrderEnd() {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateAccountValue(String key, String value, String currency,
			String accountName) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updatePortfolio(Contract contract, int position,
			double marketPrice, double marketValue, double averageCost,
			double unrealizedPNL, double realizedPNL, String accountName) {
		if(contract.m_secType.equals(StockContract.TYPE) )
			portfolio.update(contract.m_symbol, position);
	}

	/**
	 * Comes across as "10:05"
	 */
	@Override
	public void updateAccountTime(String timeStamp) {
		LogManager.getLogger("Misc").info( "Update time to " + timeStamp);
		portfolio.setTime( System.currentTimeMillis() );
	}

	@Override
	public void accountDownloadEnd(String accountName) {
		// TODO Auto-generated method stub

	}

	@Override
	public void nextValidId(int orderId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void contractDetails(int reqId, ContractDetails contractDetails) {
		// TODO Auto-generated method stub

	}

	@Override
	public void bondContractDetails(int reqId, ContractDetails contractDetails) {
		// TODO Auto-generated method stub

	}

	@Override
	public void contractDetailsEnd(int reqId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void execDetails(final int reqId, final Contract contract, final Execution execution) {
		Runnable r = new Runnable(){
			@Override
			public void run(){
				EWrapper ew = requester.getResponseHandler(reqId);
				
				if(ew == null && execution != null){
					ew = requester.getResponseHandler(execution.m_orderId);
				}
				if(ew != null) ew.execDetails(execution.m_orderId, contract, execution);
				else LogManager.getLogger("Trading").warn( "[" + reqId + "] Received execDetails " + contract.m_symbol + " " + execution.m_shares + "@" + execution.m_price + " but no delegate registered");
			};
		};
		executorService.execute(r);
	}

	@Override
	public void execDetailsEnd(final int reqId) {
		final EWrapper ew = requester.getResponseHandler(reqId);
		if(ew != null) ew.execDetailsEnd(reqId);
		else LogManager.getLogger("Trading").warn( "[" + reqId + "] Received execEnd but no delegate registered");
	}

	@Override
	public void updateMktDepth(int tickerId, int position, int operation,
			int side, double price, int size) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateMktDepthL2(int tickerId, int position,
			String marketMaker, int operation, int side, double price, int size) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateNewsBulletin(int msgId, int msgType, String message,
			String origExchange) {
		// TODO Auto-generated method stub

	}

	@Override
	public void managedAccounts(String accountsList) {
		// TODO Auto-generated method stub

	}

	@Override
	public void receiveFA(int faDataType, String xml) {
		// TODO Auto-generated method stub

	}

	/**
	 * Called from a thread started by the EClientSocket to handle replies
	 */
	@Override
	public void historicalData(final int reqId, final String dateStr,
			final double open, final double high, final double low,
			final double close, final int volume, final int count,
			final double WAP, final boolean hasGaps) {
		//delegate to the registered handler
		final ResponseHandlerDelegate ew = requester.getResponseHandler(reqId);
		// end of data
		if (open < 0 && high < 0) {
			requester.endRequest(reqId);
			//delegate so specific delegate can add additional details to the message 
			ew.info( "[" + reqId + "] Completed historical data request having written: " + ew.getCountOfRecords());
			return;
		}

		ew.historicalData(reqId, dateStr, open, high, low, close, volume, count, WAP, hasGaps);
	}

	@Override
	public void scannerParameters(String xml) {
		// TODO Auto-generated method stub

	}

	@Override
	public void scannerData(int reqId, int rank,
			ContractDetails contractDetails, String distance, String benchmark,
			String projection, String legsStr) {
		// TODO Auto-generated method stub

	}

	@Override
	public void scannerDataEnd(int reqId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void realtimeBar(final int reqId, final long time, final double open, final double high,
			final double low, final double close, final long volume, final double wap, final int count) {
		Runnable r = new Runnable(){
			@Override
			public void run(){ 
				//delegate to the registered handler
				final ResponseHandlerDelegate ew = requester.getResponseHandler(reqId);
				if(ew != null) ew.realtimeBar(reqId, time, open, high, low, close, volume, wap, count);
				else LogManager.getLogger("RealTimeBar").warn( "[" + reqId + "] Reveived realtime bar " + HistoricalDateManipulation.getDateAsStr(time) + " but no delegate registered " );
			};
		};
		executorService.execute(r);
	}

	@Override
	public void currentTime(long time) {
		// TODO Auto-generated method stub

	}

	@Override
	public void fundamentalData(int reqId, String data) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deltaNeutralValidation(int reqId, UnderComp underComp) {
		// TODO Auto-generated method stub

	}

	@Override
	public void tickSnapshotEnd(int reqId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void marketDataType(int reqId, int marketDataType) {
		// TODO Auto-generated method stub

	}

	@Override
	public void commissionReport(CommissionReport commissionReport) {
		// TODO Auto-generated method stub

	}
	public void endRequest(int reqId){
		requester.endRequest(reqId);
	}
	public void reset() {
		requester.reset();
		
	}

}
