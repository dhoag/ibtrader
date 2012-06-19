package com.davehoag.ib.dataTypes;

import com.ib.client.Contract;
import com.ib.client.Order;

public class LimitOrder{
		final public int orderId;
		final public Contract lmtContract;
		final public Order lmtOrder;
 
		public LimitOrder(final int id, final Contract contract, final Order order)
		{
			orderId = id;
			lmtContract = contract;
			lmtOrder = order;
		}
		public double getPrice(){
			return lmtOrder.m_lmtPrice;
		}
		public String getSymbol(){ return lmtContract.m_symbol; }
		public boolean isBuy(){ return lmtOrder.m_action.equals("BUY"); }
}
