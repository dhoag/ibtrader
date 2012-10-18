package com.davehoag.ib.strategies;

import java.util.Calendar;

import com.davehoag.ib.Strategy;
import com.davehoag.ib.TradingStrategy;
import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.LimitOrder;
import com.davehoag.ib.dataTypes.Portfolio;
import com.davehoag.ib.dataTypes.SimpleReturn;
import com.davehoag.ib.util.HistoricalDateManipulation;
/**
 * Need two instances (and only two) this strategy running at any given time. 
 * 
 * @author dhoag
 *
 */
public class SimpleMomentumStrategy implements Strategy {
	boolean optimistic = true;
	boolean monthly;
	int desiredDay = 23;
	boolean wereOne;
	Bar oldestBar = null;
	Bar latestBar = null;
	String symbol;

	static SimpleMomentumStrategy one;
	static SimpleMomentumStrategy two;
	/**
	 * populate the global static values necessary to compare the two against each other
	 */
	public SimpleMomentumStrategy(){
		synchronized(SimpleMomentumStrategy.class){
			if(one == null){
				one = this;
				wereOne = true;
			}
			else {
				wereOne = false;
				two = this;	
			}
		}
	}

	/**
	 * Figure out the day I think is the closest trading day
	 * @param aBar
	 * @param desiredDay
	 * @return
	 */
	public int getTargetDay(Bar aBar, int desiredDay){
		Calendar c = Calendar.getInstance();
		c.setTime(aBar.getTime());
		c.setLenient(false);
		int maxNumDays = c.getActualMaximum(Calendar.DAY_OF_MONTH);
		desiredDay = desiredDay > maxNumDays ? maxNumDays: desiredDay;
		c.set(Calendar.DAY_OF_MONTH, desiredDay);
		int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
		int result = c.get(Calendar.DAY_OF_MONTH);
		if(dayOfWeek == Calendar.SUNDAY)  result -=2;
		if(dayOfWeek == Calendar.SATURDAY) result -=1;
		if(result < 0) {
			result = c.get(Calendar.DAY_OF_MONTH);
			if(dayOfWeek == Calendar.SUNDAY)  result +=1;
			if(dayOfWeek == Calendar.SATURDAY) result +=2;
		}
		return result;
	}
	@Override
	public void newBar(Bar aBar, Portfolio holdings, TradingStrategy executionEngine) {
		final String timestamp = HistoricalDateManipulation.getDateAsStr( aBar.originalTime );
		//pick a high volatility window to trade
		if(timestamp.endsWith("08:35:00")){
			int target = getTargetDay(aBar, desiredDay);
			if(target == HistoricalDateManipulation.getDay(aBar.originalTime)){
				if(oldestBar == null ){//Nothing to compare against, just move on
					oldestBar = aBar;
				}
				else {
					latestBar = aBar;
					symbol = aBar.symbol;
					evaluateMomentum(holdings, executionEngine);
				}
			}
		}
	}
	/**
	 * Trading day! figure out if we should trade 
	 * @param aBar
	 * @param holdings
	 * @param executionEngine
	 */
	protected void evaluateMomentum(final Portfolio holdings, final TradingStrategy executionEngine) {
		synchronized(SimpleMomentumStrategy.class){

			final SimpleMomentumStrategy other = getOther();
			//is the other's latestBar timestamp equal or greater than my bar
			final boolean dataAvailable = other.isCurrent(latestBar.originalTime);
			if(dataAvailable){
				if( takePositions()){
					int currentQty = getCurrentPosition(holdings);
					if(currentQty == 0){
						other.sellExistingPosition(holdings, executionEngine);
						
						//need a way to wait for confirmation
						openNewLongPosition(holdings, executionEngine);
					}
				}
			}
		}
	}
	/**
	 * Get the stock I'm trading against
	 * @return
	 */
	public SimpleMomentumStrategy getOther(){
		if(wereOne)	return two;
		return one;
	}
	/**
	 * Is this return greater than the other return. 
	 * @return
	 */
	public boolean takePositions(){
		final SimpleMomentumStrategy other = getOther();
		final boolean take = getReturn() > other.getReturn();
		changeToLast();
		other.changeToLast();
		return take;
	}
	/**
	 * 
	 * @param port
	 * @return
	 */
	protected int getCurrentPosition(final Portfolio port){
		return port.getShares(symbol);
	}
	protected void changeToLast(){
		oldestBar = latestBar;
	}
	/**
	 * 
	 */
	public boolean isCurrent(long seconds){
		return latestBar.originalTime >= seconds;
	}
	/**
	 * Estimate return if we went long on the target date
	 * @return
	 */
	public double getReturn(){
		double purchase = optimistic ? ((oldestBar.high + oldestBar.low)/2) : oldestBar.high;
		double sale = optimistic ? ((latestBar.high + latestBar.low)/2) : latestBar.close;
		return (sale - purchase)  / purchase;
	}

	/**
	 * @param port
	 * @param best
	 */
	protected void openNewLongPosition(final Portfolio port, final TradingStrategy executionEngine) {
		final double money = port.getCash();
		final double qtyD = Math.floor(money / (latestBar.close*100));
		
		final int buyQty = (int)(qtyD * 100);
		final LimitOrder order = new LimitOrder(symbol, buyQty, latestBar.close, true);
		executionEngine.executeOrder(order);
	}
	/**
	 * Will block until order is confirmed
	 * @param port
	 * @param worse
	 */
	protected void sellExistingPosition(final Portfolio port, final TradingStrategy executionEngine) {
		int priorQty = getCurrentPosition(port);
		if(priorQty != 0){ //sell the losing shares at yesterday's close
			final LimitOrder order = new LimitOrder(symbol, priorQty, latestBar.close, false);
			executionEngine.executeOrder(order);
			while(! order.isConfirmed())
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
	}
}
