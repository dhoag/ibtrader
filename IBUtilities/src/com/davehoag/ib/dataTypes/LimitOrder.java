package com.davehoag.ib.dataTypes;


public class LimitOrder{
		String sym;
		int shares;
		double orderPrice;
		boolean buyOrder;
		LimitOrder stopLoss;
 
		public LimitOrder( final int qty, final double price, final boolean buy ){
			this(null, qty, price, buy);
		}
		public LimitOrder( final String symbol, final int qty, final double price, final boolean buy ){
			sym = symbol;
			shares = qty;
			orderPrice = price;
			buyOrder = buy;
		}
		public void setSymbol(final String symbol){
			sym = symbol;
		}
		public double getPrice(){
			return orderPrice;
		}
		public String getSymbol(){ return sym; }
		public boolean isBuy(){ return buyOrder; }
		public int getShares(){ return shares; }
		public LimitOrder getStopLoss(){ return stopLoss; }
		public void setStopLoss( final LimitOrder order ){ stopLoss = order; }
}
