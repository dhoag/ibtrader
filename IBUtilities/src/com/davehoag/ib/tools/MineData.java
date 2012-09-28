package com.davehoag.ib.tools;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.davehoag.ib.CassandraDao;
import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.BarIterator;
import com.davehoag.ib.dataTypes.LimitOrder;
import com.davehoag.ib.dataTypes.Portfolio;
import com.davehoag.ib.dataTypes.SimpleReturn;

public class MineData {
	public static void main(String [] args){
		DateFormat df = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
		NumberFormat nf = NumberFormat.getNumberInstance();
		int i = 0;
		final String barSize = args[i++];
		final String startTime = args[i++];
		final String endTime = args[i++];

		try 
		{
			long start = df.parse(startTime).getTime()/1000;
			long finish = df.parse(endTime).getTime()/1000;
			simpleMonthMomentum(start, finish, barSize, args, i);
			
		}//end try
		catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static void simpleMonthMomentum(long start, long finish, String barSize, String [] args, int idx){
		String oneStock = args[idx++];
		String twoStock = args[idx++]; 
		BarIterator data = CassandraDao.getInstance().getData(oneStock, start, finish, barSize);
		BarIterator data2 = CassandraDao.getInstance().getData(twoStock, start, finish, barSize);
		double [] results = new double[31];
		for(int i = 1; i <= 31; i++ ){
			results[i-1] = analyzeDateOptions(data, data2, i);
			data.reset();
			data2.reset();
		}
		for(int i =1; i < 31; i++){
			System.out.println( i + " "+ results[i-1]);
		}
	}
	/**
	 * @param data
	 * @param data2
	 */
	protected static double analyzeDateOptions(BarIterator data, BarIterator data2, final int dayOfMonth) {
		SimpleReturn srOne = new SimpleReturn(5*5*2);
		SimpleReturn srTwo = new SimpleReturn(5*5*2);
		int count = 0;
		int currentMonth=0;
		Portfolio port = new Portfolio();
		port.setCash(100000);
		while(true){
			if(data.hasNext() && data2.hasNext()){
				if(count < 4){//seed with 2 full months of data
					Bar b = data.next();
					Calendar c = Calendar.getInstance();
					c.setTime(new Date(b.originalTime*1000));
					if(c.get(Calendar.HOUR) == 4 || c.get(Calendar.HOUR) == 16) {
						b = data.next();
					}
					Bar b2 = data2.next();
					c = Calendar.getInstance();
					c.setTime(new Date(b2.originalTime*1000));
					if(c.get(Calendar.HOUR) == 4 || c.get(Calendar.HOUR) == 16) {
						b2 = data2.next();
					}
					//ignore my bad day data
					if(b2.originalTime != b.originalTime){
						throw new IllegalStateException("Analysis invalid " + b + " " + b2);
					}
					srOne.newBar(b);
					srTwo.newBar(b2);
					int barMonth = c.get(Calendar.MONTH);
					if(currentMonth != barMonth){
						currentMonth = barMonth;
						count++;
					}
				}
				else{
					//only add one more month of data
					count = 3;
					double oneReturn = srOne.getDayOfMonthReturn(dayOfMonth);
					double twoReturn = srTwo.getDayOfMonthReturn(dayOfMonth);
					
					final SimpleReturn best = oneReturn > twoReturn ? srOne : srTwo;
					final SimpleReturn worse = oneReturn < twoReturn ? srOne : srTwo;
					int qty = port.getShares(best.getSymbol());
					if(qty == 0){
						//time to rotate to the better trend
						sellExistingPosition(port, worse);
						openNewLongPosition(port, best);
					}
				}
			}
			else
				break;
		}
		port.updatePrice(srOne.getSymbol(), srOne.getMostRecent().close);
		port.updatePrice(srTwo.getSymbol(), srTwo.getMostRecent().close);
		port.displayTradeStats("Day " + dayOfMonth);
		port.displayValue();
		return port.getNetValue();
	}
	/**
	 * @param port
	 * @param best
	 */
	protected static void openNewLongPosition(final Portfolio port, final SimpleReturn best) {
		final Bar bestBar = best.getMostRecent();
		double money = port.getCash();
		double qtyD = Math.floor(money / (bestBar.wap*100));
		
		int buyQty = (int)(qtyD * 100);
		final LimitOrder order = new LimitOrder(best.getSymbol(), buyQty, bestBar.wap, true);
		int orderId = (int) bestBar.originalTime;
		order.setId(orderId);
		port.setTime(bestBar.originalTime);
		port.placedOrder(order);
		port.confirm(order.getId(), order.getSymbol(), order.getPrice(), order.getShares());
	}
	/**
	 * @param port
	 * @param worse
	 */
	protected static void sellExistingPosition(Portfolio port, final SimpleReturn worse) {
		Bar [] bars = worse.getDayOfMonthBars(31);

		int priorQty = port.getShares(worse.getSymbol());
		if(priorQty != 0){ //sell the losing shares at yesterday's close
			port.setTime(bars[1].originalTime);
			int orderId = 100;
			final LimitOrder order = new LimitOrder(worse.getSymbol(), priorQty, bars[1].close, false);
			order.setId(orderId);
			port.placedOrder(order);
			port.confirm(order.getId(), order.getSymbol(), order.getPrice(), order.getShares());
		}
	}
}