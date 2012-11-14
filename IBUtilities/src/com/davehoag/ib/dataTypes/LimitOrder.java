package com.davehoag.ib.dataTypes;

import com.ib.client.Order;

/**
 * 
 * @author dhoag
 *
 */
public class LimitOrder implements Cloneable{
		String sym;
		int shares = 0;
		double fillPrice = 0;
		double orderPrice = 0;
		boolean buyOrder;
		LimitOrder stopLoss;
		int id;
		boolean trail = false;
		boolean confirmed = false;
		long timeConfirmed;
		Order ibOrder;
		LimitOrder onset;
		boolean closeMultiple = false;

	@Override
	public String toString() {
		return "LMT " + (buyOrder ? "BUY" : "SELL") + " " + sym + " " + shares + "@" + orderPrice;
	}
		/**
		 * When the order is actually submitted set the id for reference
		 * @param orderId
		 */
		public void setId(final int orderId){ id = orderId; }
		public int getId(){ return id; }
		public double getProfit(){
			final LimitOrder open = getOnset();
			if( open == null) throw new IllegalStateException("Should be a closing trade associated with an onset trade");
			final int shares = closeMultiple ? open.getShares() : getShares();
			return getPrice() * shares - (open.getPrice() * open.getShares() );
		}
		/**
		 * Fill message was received and the price was updated to the passed in value.
		 * TODO support partial fills
		 */
		public void confirm(){ 
			confirmed = true;
			timeConfirmed = System.currentTimeMillis();
		}
		/**
		 * Check if order has been confirmed
		 * @return True if this order has been completely filled
		 */
		public boolean isConfirmed(){ return confirmed; }
		public void markAsTrailingOrder(){ trail = true; }
		public void markAsCloseMultiple(){ closeMultiple = true; }
		public void setOnset(final LimitOrder or){ onset = or; }
		public LimitOrder getOnset(){ return onset; }
		public boolean isTrail(){ return trail; }
		public void setIbOrder(final Order or){ ibOrder = or; }
		public LimitOrder( final int qty, final double price, final boolean buy ){
			this(null, qty, price, buy);
		}
		public LimitOrder( final String symbol, final int qty, final double price, final boolean buy ){
			sym = symbol;
			shares = qty;
			orderPrice = price;
			buyOrder = buy;
		}
		public void setPrice(final double d){
			fillPrice = d;
		}
		public void setSymbol(final String symbol){
			sym = symbol;
		}
		public double getPrice(){
			return fillPrice == 0 ? orderPrice : fillPrice;
		}
		public String getSymbol(){ return sym; }
		public boolean isBuy(){ return buyOrder; }
		public int getShares(){ return shares; }
		public LimitOrder getStopLoss(){ return stopLoss; }
		public void setStopLoss( final LimitOrder order ){ stopLoss = order; }
		@Override
		public LimitOrder clone() {
			try{
			LimitOrder result = (LimitOrder)super.clone();
			return result;
			}catch(CloneNotSupportedException ex){}
			return null;
		}
}
