package com.davehoag.ib.dataTypes;

import com.ib.client.Contract;
import com.ib.client.Order;

/**
 * 
 * @author dhoag
 * 
 */
public class LimitOrder implements Cloneable {
	
	Contract contract;
	int shares = 0;
	double fillPrice = 0;
	double orderPrice = 0;
	boolean buyOrder;
	LimitOrder stopLoss;
	LimitOrder profitTaker;
	int id = 0;
	boolean trail = false;
	boolean stop = false;
	boolean mkt = false;
	boolean confirmed = false;
	long timeConfirmed;
	Order ibOrder;
	LimitOrder onset;
	boolean closeMultiple = false;
	long portfolioTime;

	@Override
	public String toString() {
		return "[" + id + "] " + getOrderType() + " " + (buyOrder ? "BUY" : "SELL") + " " + getContract() + " " + shares + "@" + getPrice();
	}

	public String getOrderType(){
		if(isTrail()) return "TRAIL";
		if(isStop()) return "STPLMT";
		if(isMkt()) return "MKT";
		return "LMT";
	}
	public boolean isMkt() {
		return mkt;
	}

	/**
	 * When the order is actually submitted set the id for reference
	 * 
	 * @param orderId
	 */
	public void setId(final int orderId) {
		id = orderId;
	}

	public int getId() {
		return id;
	}

	public double getProfit() {
		final LimitOrder open = getOnset();
		if (open == null)
			throw new IllegalStateException("Should be a closing trade associated with an onset trade");
		final int shares = closeMultiple ? open.getShares() : getShares();
		return getPrice() * shares - (open.getPrice() * open.getShares());
	}

	/**
	 * Fill message was received and the price was updated to the passed in
	 * value. TODO support partial fills
	 */
	public void confirm() {
		confirmed = true;
		timeConfirmed = System.currentTimeMillis();
	}

	/**
	 * Check if order has been confirmed
	 * 
	 * @return True if this order has been completely filled
	 */
	public boolean isConfirmed() {
		return confirmed;
	}

	public void markAsTrailingOrder() {
		trail = true;
		stop = true;
	}

	public void markAsStop() {
		stop = true;
	}
	public void markAsCloseMultiple() {
		closeMultiple = true;
	}

	public void setOnset(final LimitOrder or) {
		onset = or;
	}

	public LimitOrder getOnset() {
		return onset;
	}

	public boolean isTrail() {
		return trail;
	}

	public void setIbOrder(final Order or) {
		ibOrder = or;
	}
	public LimitOrder(){}
	public LimitOrder(final String symbol, final int qty, final double price, final boolean buy) {
		this(new StockContract(symbol), qty, price, buy);
	}
	/**
	 * Useful to enable Orders to be created without having the contract yet. 
	 * @param qty
	 * @param price
	 * @param buy
	 */
	public LimitOrder(final int qty, final double price, final boolean buy) {
		this();
		shares = qty;
		orderPrice = price;
		buyOrder = buy;
	}

	public LimitOrder(final Contract aContract, final int qty, final double price, final boolean buy) {
		contract = aContract;
		shares = qty;
		orderPrice = price;
		buyOrder = buy;
	}
	
	public void setFillPrice(final double d) {
		fillPrice = d;
	}

	public double getPrice() {
		double result = fillPrice == 0 ? orderPrice : fillPrice;
		return round2(result);
	}

	double round2(double num) {
		double result = num * 100;
		result = Math.round(result);
		result = result / 100;
		return result;
	}
	public boolean isBuy() {
		return buyOrder;
	}

	public int getShares() {
		return shares;
	}

	public LimitOrder getStopLoss() {
		return stopLoss;
	}

	public void setStopLoss(final LimitOrder order) {
		stopLoss = order;
		if(order == null) return;
		order.setOnset(this);
		order.markAsStop();
	}
	public LimitOrder getProfitTaker() {
		return profitTaker;
	}

	public void setProfitTaker(final LimitOrder order) {
		profitTaker = order;
		if(order == null) return;
		profitTaker.setOnset(this);
	}

	public boolean isStop() {
		return stop;
	}
	@Override
	public LimitOrder clone() {
		try {
			LimitOrder result = (LimitOrder) super.clone();
			return result;
		} catch (CloneNotSupportedException ex) {}
		return null;
	}

	public void setPortfolioTime(long time) {
		portfolioTime = time;
	}

	/**
	 * Helpful when backtesting - use the prior date as the timestamp for this
	 * trade
	 * 
	 * @return
	 */
	public long getPortfolioTime() {
		return portfolioTime;
	}

	public void setContract(Contract aContract) {
		contract = aContract;
	}

	public Contract getContract() {
		return contract;
	}
	public void setOrderPrice(double price){
		orderPrice = price;
	}

	public void setMkt(boolean mkt2) {
		mkt = mkt2;
	}
}
