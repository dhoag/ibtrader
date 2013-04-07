package com.davehoag.ib.dataTypes;

import java.util.HashMap;

import com.davehoag.ib.util.HistoricalDateManipulation;

public class TradingDay {
	final HashMap<String, BarCache> dayBars = new HashMap<String, BarCache>();
	final HashMap<String, BarCache> fiveSecBars = new HashMap<String, BarCache>();
	
	/**
	 * Get a bar from which we can determine the prior close. Could be a 1 day bar
	 * or a 5 sec bar. 
	 * 
	 * @param symbol
	 * @param time
	 * @return
	 */
	public Bar getPriorClose(final String symbol, final long time){ 
		long open = HistoricalDateManipulation.getOpen(time);
		
		//first try the five sec bars
		final BarCache cache = fiveSecBars.get(symbol);
		int size = cache.size();
		for(int i = 0; i < size-1; i++){
			final Bar aBar = cache.get(i);
			if(aBar.originalTime == open) return cache.get(i+1);
		}
		//now try the 1 day bars
		final BarCache cache2 = dayBars.get(symbol);
		//return the most recent bar, should be yesterday. Not worried about validation
		//but may in the future care
		return cache2.get(0);
	}
	/**
	 * Walk backward looking for the open
	 * @return
	 */
	public Bar getOpen(final String symbol, final long time){
		long open = HistoricalDateManipulation.getOpen(time);
		final BarCache cache = fiveSecBars.get(symbol);
		int size = cache.size();
		for(int i = 0; i < size; i++){
			final Bar aBar = cache.get(i);
			if(aBar.originalTime == open) return aBar;
		}
		throw new IllegalStateException("No open for " + symbol + " date: " + HistoricalDateManipulation.getDateAsStr(time));
	}
	
	public void pushDayBar(final String symbol, final Bar aBar){
		assert aBar.barSize.equals("bar1day");
		assert symbol != null;
		
		final BarCache cache = getDayBarCache(symbol);
		cache.push(aBar);
	}
	public void push5SecBar(final String symbol, final Bar aBar){
		assert aBar.barSize.equals("bar5sec");
		assert symbol != null;
		
		final BarCache cache = get5SecBarCache(symbol);
		cache.push(aBar);
	}
	public BarCache getDayBarCache(final String symbol){
		return getBarCache(symbol, true);
	}
	public BarCache get5SecBarCache(final String symbol){
		return getBarCache(symbol, false);
	}
	public void putDayBarCache(final String symbol, final BarCache cache ){
		putBarCache(symbol, cache, true);
	}

	public void put5SecBarCache(final String symbol, final BarCache cache ){
		putBarCache(symbol, cache, true);
	}
	final synchronized BarCache getBarCache(final String symbol, final boolean dayBar ){
		assert symbol != null;
		final HashMap<String, BarCache> map = dayBar ? dayBars : fiveSecBars;
		BarCache cache = map.get(symbol);
		if(cache == null) {
			cache = new BarCache(500);
			map.put(symbol, cache);
		}
		return cache;
	}
	final synchronized void putBarCache(final String symbol, final BarCache cache, final boolean dayBar){
		assert symbol != null;
		final HashMap<String, BarCache> map = dayBar ? dayBars : fiveSecBars;
		map.put(symbol, cache);
	}
}
