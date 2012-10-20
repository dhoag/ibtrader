package com.davehoag.ib.dataTypes;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import me.prettyprint.hector.api.beans.HColumn;

import com.davehoag.ib.CassandraDao;


public class BarIterator implements Iterator<Bar>, Iterable<Bar> {
	final HashMap<String, List<HColumn<Long, Double>>> priceData;
	final HashMap<String, List<HColumn<Long, Long>>> volData;
	int count = 0;
	final String symbol;

	public BarIterator(final String sym, final HashMap<String, List<HColumn<Long, Double>>> price, 
			final HashMap<String, List<HColumn<Long, Long>>> vol) throws Exception {
		priceData = price;
		volData = vol;
		symbol = sym;
	}
	public void reset(){
		count = 0;
	}
	public BarIterator(String sym) {
		priceData = null;
		volData = null;
		symbol = sym;
	}

	public Iterator<Bar> iterator() {
		return this;
	}

	public boolean hasNext() {
		if (priceData == null)
			return false;
		return count < volData.get(symbol + ":vol").size();
	}

	public Bar next() {
		final Bar bar = new Bar();
		bar.symbol = symbol;
		bar.close = priceData.get(symbol + ":close").get(count).getValue().doubleValue();
		bar.open = priceData.get(symbol + ":open").get(count).getValue().doubleValue();
		bar.high = priceData.get(symbol + ":high").get(count).getValue().doubleValue();
		bar.low = priceData.get(symbol + ":low").get(count).getValue().doubleValue();
		bar.wap = priceData.get(symbol + ":wap").get(count).getValue().doubleValue();
		bar.tradeCount = volData.get(symbol + ":tradeCount").get(count).getValue().intValue();
		bar.volume = volData.get(symbol + ":vol").get(count).getValue().longValue();
		bar.originalTime = volData.get(symbol + ":vol").get(count).getName().longValue();
		count++;
		return bar;
	}

	public void remove() {
		// TODO Auto-generated method stub
	}
}