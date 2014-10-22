package com.davehoag.ib;

import org.apache.logging.log4j.LogManager;

import com.ib.client.CommissionReport;
import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.EWrapper;
import com.ib.client.Execution;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.TickType;
import com.ib.client.UnderComp;

abstract class ResponseHandlerDelegate implements EWrapper {

	IBClientRequestExecutor requester;
	int reqId;
	private int tickerRequestId;
	long startTime;	
	int countOfRecords;
	
	/* (non-Javadoc)
	 * @see com.ib.client.EWrapper#position(java.lang.String, com.ib.client.Contract, int, double)
	 */
	@Override
	public void position(String account, Contract contract, int pos,
			double avgCost) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.ib.client.EWrapper#verifyMessageAPI(java.lang.String)
	 */
	@Override
	public void verifyMessageAPI(String apiData) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.ib.client.EWrapper#verifyCompleted(boolean, java.lang.String)
	 */
	@Override
	public void verifyCompleted(boolean isSuccessful, String errorText) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.ib.client.EWrapper#displayGroupList(int, java.lang.String)
	 */
	@Override
	public void displayGroupList(int reqId, String groups) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.ib.client.EWrapper#displayGroupUpdated(int, java.lang.String)
	 */
	@Override
	public void displayGroupUpdated(int reqId, String contractInfo) {
		// TODO Auto-generated method stub
		
	}

	public void resetRecordCount(){ countOfRecords = 0; }

	/**
	 * Determine how many times records were stored by this object.
	 * @return
	 */
	public int getCountOfRecords(){
		return countOfRecords;
	}
	/**
	 * Force one to be set
	 * @param ibInterface
	 */
	public ResponseHandlerDelegate(IBClientRequestExecutor ibInterface){
		requester = ibInterface;
		
	}

	public IBClientRequestExecutor getRequester() {
		return requester;
	}
	/**
	 * 
	 * @param millis
	 */
	public void setStartTime(long millis){
		startTime = millis;
	}
	public long getStartTime(){
		return startTime;
	}
	/**
	 * Could be useful for the subclasses.
	 * @param val
	 */
	public void setReqId(int val){
		reqId = val;
	}
	/**
	 * Expect this to be overridden to provide a meaningful logger context.
	 * @param logLevel
	 * @param message
	 */
	public void info( final String message) {
		LogManager.getLogger("Delegate").info(message);
	}
	public void setRequestor(final IBClientRequestExecutor req) {
		requester = req;
	}
	@Override
	public void error(Exception e) {
		LogManager.getLogger("Delegate").warn("RH Delegate error: " + e);
		e.printStackTrace();
	}

	@Override
	public void error(String str) {
		LogManager.getLogger("Delegate").warn( "RH Delegate error: " + str );
	}

	@Override
	public void error(int id, int errorCode, String errorMsg) {
		LogManager.getLogger("Delegate").error( "Order failed or realtime bar failed: " + id+ " " + errorCode + " "+ errorMsg);
	}

	@Override
	public void connectionClosed() {
		// TODO Auto-generated method stub

	}

	@Override
	public void tickPrice(int tickerId, int field, double price, int canAutoExecute) {
		System.out.println("TickPrice [" + tickerId +"] "+ TickType.getField(field) + price);
	}

	@Override
	public void tickSize(int tickerId, int field, int size) {
		System.out.println("TickSize [" + tickerId +"] "  + TickType.getField(field) + size);
	}

	@Override
	public void tickOptionComputation(int tickerId, int field, double impliedVol, double delta,
			double optPrice, double pvDividend, double gamma, double vega, double theta, double undPrice) {

		System.out.println("TickOption [" + tickerId +"] " + TickType.getField(field));

	}

	@Override
	public void tickGeneric(int tickerId, int tickType, double value) {
		if(tickType == TickType.HALTED && value != 0.0){
			error("[" + tickerId + "] Got a market halted generic tick. 1 general halt, 2 Volatility halt: " + value);
		}
		else 
			System.out.println("TickGeneric [" + tickerId +"] " + TickType.getField(tickType) + " " + value);
	}

	@Override
	public void tickString(int tickerId, int tickType, String value) {	}

	@Override
	public void tickEFP(int tickerId, int tickType, double basisPoints, String formattedBasisPoints,
			double impliedFuture, int holdDays, String futureExpiry, double dividendImpact,
			double dividendsToExpiry) {
		System.out.println("TickEFP [" + tickerId +"] " + TickType.getField(tickType));

	}

	@Override
	public void orderStatus(int orderId, String status, int filled, int remaining, double avgFillPrice,
			int permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
		// TODO Auto-generated method stub

	}

	@Override
	public void openOrder(int orderId, Contract contract, Order order, OrderState orderState) {
		// TODO Auto-generated method stub

	}

	@Override
	public void openOrderEnd() {
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
	public void updateAccountValue(String key, String value, String currency, String accountName) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updatePortfolio(Contract contract, int position, double marketPrice, double marketValue,
			double averageCost, double unrealizedPNL, double realizedPNL, String accountName) {
		// TODO Auto-generated method stub

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
	public void execDetails(int reqId, Contract contract, Execution execution) {
		// TODO Auto-generated method stub

	}

	@Override
	public void execDetailsEnd(int reqId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateMktDepth(int tickerId, int position, int operation, int side, double price, int size) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateMktDepthL2(int tickerId, int position, String marketMaker, int operation, int side,
			double price, int size) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateNewsBulletin(int msgId, int msgType, String message, String origExchange) {
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

	@Override
	public void historicalData(int reqId, String date, double open, double high, double low, double close,
			int volume, int count, double WAP, boolean hasGaps) {
		// TODO Auto-generated method stub

	}

	@Override
	public void scannerParameters(String xml) {
		// TODO Auto-generated method stub

	}

	@Override
	public void scannerData(int reqId, int rank, ContractDetails contractDetails, String distance,
			String benchmark, String projection, String legsStr) {
		// TODO Auto-generated method stub

	}

	@Override
	public void scannerDataEnd(int reqId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void realtimeBar(int reqId, long time, double open, double high, double low, double close,
			long volume, double wap, int count) {
		// TODO Auto-generated method stub

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
		System.out.println("TickSnapshotEnd[" + reqId +"] "  );

	}

	@Override
	public void marketDataType(int reqId, int marketDataType) {
		// TODO Auto-generated method stub

	}

	@Override
	public void commissionReport(CommissionReport commissionReport) {
		// TODO Auto-generated method stub

	}

	int getTickerRequestId() {
		return tickerRequestId;
	}

	void setTickerRequestId(int tickerRequestId) {
		this.tickerRequestId = tickerRequestId;
	}

}
