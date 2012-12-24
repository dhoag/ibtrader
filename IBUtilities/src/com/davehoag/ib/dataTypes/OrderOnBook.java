package com.davehoag.ib.dataTypes;

import com.ib.client.Contract;
import com.ib.client.Order;

public class OrderOnBook{
	final public int orderId;
	final public Contract lmtContract;
	final public Order lmtOrder;

	@Override
	public String toString(){
		return "[" + orderId + "] " + getType() + " " + lmtContract.m_symbol + " " + lmtOrder.m_lmtPrice;
	}
	public OrderOnBook(final int id, final Contract c, final Order o, final double close){
		orderId = id;
		lmtContract = c;
		lmtOrder = o;
		if(isTrail()){
			//Calculate the trailing limit price
			final double price = getLimitPrice(isBuy(), getTrailPercent(), close);
			o.m_lmtPrice = price;
		}
	}
	public String getType(){ 
		if( lmtOrder.m_orderType == null ) return "MKT";
		return lmtOrder.m_orderType; 
	}
	public boolean isTrail(){ return "TRAIL".equals(getType()); }
	public boolean isLimit(){ return "LMT".equals(getType()); }

	public boolean isStpLimit() {
		return "STPLMT".equals(getType());
	}
	public boolean isBuy(){ return "BUY".equals( lmtOrder.m_action); }
	public double getLimitPrice(){ return lmtOrder.m_lmtPrice; }
	public double getTrailPercent(){ return lmtOrder.m_trailingPercent; }
	public void updateTrailingLmtValue(final double high, final double low ){
		final double newMktPrice = isBuy() ? low : high;
		lmtOrder.m_lmtPrice = getLimitPrice(isBuy(), lmtOrder.m_trailingPercent, newMktPrice);
	}

	public boolean isExecutable(final double price, final boolean isBuy, final double mktPrice){
		if(price >= mktPrice && isBuy) return true;
		if(price <= mktPrice && !isBuy) return true;
		return false;
	}
	public double getLimitPrice(final boolean buy, final double percentageOffset, final double referencePrice){
		return buy  ? referencePrice  * (1* + (percentageOffset / 100)) : referencePrice * ( 1- (percentageOffset/100));
	}
	public boolean isTriggered(final double high, final double low ){
		if( isLimit() ){
			final double mktPrice = isBuy() ? low : high;
			return isExecutable(getLimitPrice(), isBuy(), mktPrice);
		}
		else
			if (isTrail() || isStpLimit()) {
				final double thresholdPrice = isBuy() ? high : low;
				// check for trigger, pretend the market is the order price and
				// the order is the market
				if (isExecutable(thresholdPrice, isBuy(), getLimitPrice())) return true;
		}

		return false;
	}
}
