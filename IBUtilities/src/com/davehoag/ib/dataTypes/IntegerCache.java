package com.davehoag.ib.dataTypes;

import java.util.Iterator;

import org.apache.commons.math3.stat.regression.SimpleRegression;

public class IntegerCache {
	int idx = 0;
	final int [] intHist = new int[30];
	boolean direction = false;
	int countSinceChange = 0;
	int priorDifferentPrice;
	SimpleRegression regression = new SimpleRegression();
	
	/**
	 * Value from 1 to -1 as a measure of % change of a slope 
	 * @return
	 */
	public double getPercentageSlope(double tickSize){
		double slope = getSlope();
		return slope/tickSize;
	}
	//really only works once we have a full set of data
	Iterable iter = new Iterable<Integer>(){
		@Override
		public Iterator<Integer> iterator() {
			return  new Iterator<Integer>(){
				int j = idx == intHist.length - 1 ? 0 : idx +1;
				@Override
				public boolean hasNext() {
					return j != idx & intHist[j] != 0;
				}
				@Override
				public Integer next() {
					final int result  = intHist[j++];
					if(j == intHist.length) j = 0;
					return result;
				}
				@Override
				public void remove() {}
			};
		}
	};
	
	/**
	 * A thread unsafe iterable
	 * @return
	 */
	public Iterable<Integer> getPriceHistory(){
		return iter;
	}
	private boolean initRegression(){
		regression.clear();
		int i = 1;
		for( double d : getPriceHistory()){
			regression.addData(i++, d);
		}
		return (i != 1) ;
	}
	public double getRSquared(){
		if(initRegression())
			return regression.getRSquare();
		return 0;
	}
	public double getSlope(){
		if(initRegression())
			return regression.getSlope();
		return 0;	
	}
	final public void pushPrice(final int price){
		if(price != intHist[idx]) {
			direction = price > intHist[idx];
			countSinceChange = 0;
			priorDifferentPrice = intHist[idx];
		}
		else {
			countSinceChange++;
		}
		intHist[idx++] = price;
		if(idx == intHist.length) idx = 0;
	}
	public int getAge(){
		return countSinceChange;
	}
	final public int getPrice(){
		return intHist[idx];
	}
	final public int priorPrice(){
		//final int pIdx = idx == 0 ? priceHist.length-1 : idx -1;
		//return priceHist[pIdx];
		return priorDifferentPrice;
	}
}
