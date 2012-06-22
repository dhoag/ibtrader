package com.davehoag.ib;

import java.lang.reflect.Array;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.davehoag.ib.dataTypes.Portfolio;
import com.ib.client.CommissionReport;
import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.EWrapper;
import com.ib.client.Execution;
import com.ib.client.Order;
import com.ib.client.OrderState;
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
	//There is only one per account
	Portfolio portfolio = new Portfolio();

	public Portfolio getPortfolio(){
		return portfolio;
	}
	public void setRequestor(final IBClientRequestExecutor req) {
		requester = req;
	}

	@Override
	public void error(Exception e) {
		Logger.getLogger("ResponseHandler").log(Level.WARNING, "unknown source", e);
	}

	@Override
	public void error(String str) {
		Logger.getLogger("ResponseHandler").log(Level.WARNING, str);
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
			Logger.getLogger("ResponseHandler").log(Level.INFO, "[" + id + "]  "+ errorCode + " '" + errorMsg + "'");
		else
			Logger.getLogger("ResponseHandler").log(Level.SEVERE, "[" + id + "]  ERROR:" + errorCode + " '" + errorMsg + "'");
		
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
		requester.forcedClose();
	}

	@Override
	public void tickPrice(int tickerId, int field, double price,
			int canAutoExecute) {
		// TODO Auto-generated method stub

	}

	@Override
	public void tickSize(int tickerId, int field, int size) {
		// TODO Auto-generated method stub

	}

	@Override
	public void tickOptionComputation(int tickerId, int field,
			double impliedVol, double delta, double optPrice,
			double pvDividend, double gamma, double vega, double theta,
			double undPrice) {
		// TODO Auto-generated method stub

	}

	@Override
	public void tickGeneric(int tickerId, int tickType, double value) {
		// TODO Auto-generated method stub

	}

	@Override
	public void tickString(int tickerId, int tickType, String value) {
		// TODO Auto-generated method stub

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
		// TODO Auto-generated method stub

	}

	@Override
	public void openOrder(int orderId, Contract contract, Order order,
			OrderState orderState) {
		// TODO Auto-generated method stub

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
		
		portfolio.update(contract.m_symbol, position);
	}

	@Override
	public void updateAccountTime(String timeStamp) {
		// TODO Auto-generated method stub

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
		EWrapper ew = requester.getResponseHandler(reqId);
		
		if(ew == null && execution != null){
			ew = requester.getResponseHandler(execution.m_orderId);
		}
		if(ew != null) ew.execDetails(execution.m_orderId, contract, execution);
		else Logger.getLogger("Trading").log(Level.WARNING, "[" + reqId + "] Received execDetails " + contract.m_symbol + " " + execution.m_shares + "@" + execution.m_price + " but no delegate registered");
	}

	@Override
	public void execDetailsEnd(final int reqId) {
		final EWrapper ew = requester.getResponseHandler(reqId);
		if(ew != null) ew.execDetailsEnd(reqId);
		else Logger.getLogger("Trading").log(Level.WARNING, "[" + reqId + "] Received execEnd but no delegate registered");
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
			Logger.getLogger("HistoricalData").log(Level.INFO, "[" + reqId + "] Completed historical data request having written: " + ew.getCountOfRecords());
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
	public void realtimeBar(int reqId, long time, double open, double high,
			double low, double close, long volume, double wap, int count) {
		//delegate to the registered handler
		final ResponseHandlerDelegate ew = requester.getResponseHandler(reqId);
		if(ew != null) ew.realtimeBar(reqId, time, open, high, low, close, volume, wap, count);
		else Logger.getLogger("RealTimeBar").log(Level.WARNING, "[" + reqId + "] Reveived realtime bar but no delegate registered " );
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

}
