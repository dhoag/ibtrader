package com.davehoag.ib.dataTypes;

import java.util.HashMap;

import com.davehoag.ib.util.HistoricalDateManipulation;
import com.ib.client.Contract;

public class TradingDay {
	final HashMap<Contract, BarCache> dayBars = new HashMap<Contract, BarCache>();
	final HashMap<Contract, BarCache> fiveSecBars = new HashMap<Contract, BarCache>();
	
	/**
	 * Get a bar from which we can determine the prior close. Could be a 1 day bar
	 * or a 5 sec bar. 
	 * 
	 * @param aContract
	 * @param time
	 * @return
	 */
	public Bar getPriorClose(final Contract aContract, final long time){ 
		long open = HistoricalDateManipulation.getOpen(time);
		
		//first try the five sec bars
		final BarCache cache = fiveSecBars.get(aContract);
		int size = cache.size();
		for(int i = 0; i < size-1; i++){
			final Bar aBar = cache.get(i);
			if(aBar.originalTime == open) return cache.get(i+1);
		}
		//now try the 1 day bars
		final BarCache cache2 = dayBars.get(aContract);
		//return the most recent bar, should be yesterday. Not worried about validation
		//but may in the future care
		return cache2.get(0);
	}
	/**
	 * Walk backward looking for the open
	 * @return
	 */
	public Bar getOpen(final Contract aContract, final long time){
		long open = HistoricalDateManipulation.getOpen(time);
		final BarCache cache = fiveSecBars.get(aContract);
		int size = cache.size();
		for(int i = 0; i < size; i++){
			final Bar aBar = cache.get(i);
			if(aBar.originalTime == open) return aBar;
		}
		throw new IllegalStateException("No open for " + aContract + " date: " + HistoricalDateManipulation.getDateAsStr(time));
	}
	
	public void pushDayBar(final Contract aContract, final Bar aBar){
		assert aBar.barSize.equals("bar1day");
		assert aContract != null;
		
		final BarCache cache = getDayBarCache(aContract);
		cache.push(aBar);
	}
	public void push5SecBar(final Contract aContract, final Bar aBar){
		assert aBar.barSize.equals("bar5sec");
		assert aContract != null;
		
		final BarCache cache = get5SecBarCache(aContract);
		cache.push(aBar);
	}
	public BarCache getDayBarCache(final Contract aContract){
		return getBarCache(aContract, true);
	}
	public BarCache get5SecBarCache(final Contract aContract){
		return getBarCache(aContract, false);
	}
	public void putDayBarCache(final Contract aContract, final BarCache cache ){
		putBarCache(aContract, cache, true);
	}

	public void put5SecBarCache(final Contract aContract, final BarCache cache ){
		putBarCache(aContract, cache, true);
	}
	final synchronized BarCache getBarCache(final Contract aContract, final boolean dayBar ){
		assert aContract != null;
		final HashMap<Contract, BarCache> map = dayBar ? dayBars : fiveSecBars;
		BarCache cache = map.get(aContract);
		if(cache == null) {
			cache = new BarCache(500);
			map.put(aContract, cache);
		}
		return cache;
	}
	final synchronized void putBarCache(final Contract aContract, final BarCache cache, final boolean dayBar){
		assert aContract != null;
		final HashMap<Contract, BarCache> map = dayBar ? dayBars : fiveSecBars;
		map.put(aContract, cache);
	}
}
