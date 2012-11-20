package com.davehoag.ib.dataTypes;

import java.util.Calendar;
import java.util.Iterator;

/**
 * 
 * @author dhoag
 *
 */
public class SimpleReturn implements Iterator<Bar>, Iterable<Bar>{
	Bar [] data;
	int last = 0;
	boolean initialized;
	public boolean isInitialized(){
		return initialized;
	}
	public SimpleReturn(int size){
		data = new Bar[size];
	}
	public String getSymbol(){
		if (!initialized)
			throw new IllegalStateException("Can't get symbol if not in valid state");
		return data[last].symbol;
	}
	public void newBar(final Bar aBar){
		data[last++] = aBar;
		if(last == data.length) last= 0;
		if(last == 0 && !initialized) // we wrapped
		{
			initialized = true;
		}
	}
	public Bar getMostRecent(){
		return last == 0? data[data.length - 1] : data[ last - 1];
	}
	public Bar getOldest(){
		return data[last];
	}
	/**
	 * Get the return for the oldest and most recent data in the ring buffer
	 * @return
	 */
	public double getReturn(){
		return getReturn(false, getOldest(), getMostRecent());
	}
	
	public double getDayOfMonthReturn(int dayOfMonth){
		Bar [] days = getDayOfMonthBars(dayOfMonth);
		reset();
		/*
		System.out.println(days[0] );
		System.out.println(days[1] );*/
		return getReturn(true, days[0], days[1]);
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
	/**
	 * 
	 * @param dayOfMonth
	 * @return
	 */
	public Bar [] getDayOfMonthBars(final int comparisonDay){
		final Bar [] result = new Bar[2];
		Bar firstDay = null;
		Bar lastDay =null; 
		boolean started = false;
		for(Bar aBar: this){
			final int actualTarget = getTargetDay(aBar, comparisonDay);
			Calendar c = Calendar.getInstance();
			c.setTime(aBar.getTime());
			int day = c.get(Calendar.DAY_OF_MONTH);
			if(day <= actualTarget) started = true;
			if(started && firstDay == null && day >= actualTarget){
				firstDay = aBar;
			}
			else{
				if(firstDay != null){
					Calendar c2 = Calendar.getInstance();
					c2.setTime(firstDay.getTime());
					if(c2.get(Calendar.MONTH) != c.get(Calendar.MONTH)){
						if(lastDay == null && day >= actualTarget){
							lastDay = aBar;
							break;
						}
					}
				}
			}
		}
		result[0] = firstDay;
		result[1] = lastDay;
		reset();
		return result;
	}
	/**
	 * 
	 * @param dayOfWeek
	 * @return
	 */
	public double getDayOfWeekReturn(int dayOfWeek){
		Bar [] days = getDayOfWeekBars(dayOfWeek);
		reset();
		return getReturn(true, days[0], days[1]);
	}
	/**
	 * 
	 * @param dayOfWeek
	 * @return
	 */
	protected Bar [] getDayOfWeekBars(int dayOfWeek){
		Bar [] result = new Bar[2];
		Bar firstDay = null;
		Bar lastDay =null;
		for(Bar aBar: this){
			Calendar c = Calendar.getInstance();
			c.setTime(aBar.getTime());
			int day = c.get(Calendar.DAY_OF_WEEK);
			if(day == dayOfWeek){
				if(firstDay == null) firstDay = aBar;
				else lastDay = aBar;
			}
		}		
		result[0] = firstDay;
		result[1] = lastDay;
		return result;
	}
	/**
	 * Get the return assuming a long trade  
	 * @param optimistic
	 * @return
	 */
	public double getReturn(boolean optimistic, final Bar oldest, final Bar recent){
		if(!initialized)throw new IllegalStateException("Not initialized");
		if(oldest == null || recent == null) throw new IllegalStateException("Two entries from the same period not found");
		double purchase = optimistic ? ((oldest.high + oldest.low)/2) : oldest.high;
		double sale = optimistic ? ((recent.high + recent.low)/2) : recent.close;
		return (sale - purchase)  / purchase;
	}
	
	/* Allow iterations over the ring buffer of Bars */
	int iteratorIndex = 0;
	@Override
	public Iterator<Bar> iterator() {
		return this;
	}
	@Override
	public boolean hasNext() {
		if(!initialized) throw new IllegalStateException("Array not fully populated!");
		
		return iteratorIndex < data.length;
	}
	@Override
	public Bar next() {
		iteratorIndex ++;
		int currentItemIdx = last + iteratorIndex;
		if(currentItemIdx >= data.length){
			currentItemIdx = currentItemIdx - data.length;
		}
		return data[currentItemIdx];
	}
	@Override
	public void remove() {
		// TODO Auto-generated method stub
		
	}
	public void reset(){
		iteratorIndex = -1;
	}
}
