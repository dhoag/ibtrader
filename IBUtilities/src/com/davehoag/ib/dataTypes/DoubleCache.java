package com.davehoag.ib.dataTypes;

import java.util.Iterator;

import org.apache.commons.math3.stat.regression.SimpleRegression;

public class DoubleCache {
	int idx = 0;
	final double [] priceHist = new double[30];
	boolean direction = false;
	int countSinceChange = 0;
	double priorDifferentPrice;
	SimpleRegression regression = new SimpleRegression();
	
	//really only works once we have a full set of data
	Iterable iter = new Iterable<Double>(){
		@Override
		public Iterator<Double> iterator() {
			return  new Iterator<Double>(){
				int j = idx == priceHist.length - 1 ? 0 : idx +1;
				@Override
				public boolean hasNext() {
					return j != idx & priceHist[j] != 0;
				}
				@Override
				public Double next() {
					final double result  = priceHist[j++];
					if(j == priceHist.length) j = 0;
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
	public Iterable<Double> getPriceHistory(){
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
	final public void pushPrice(final double price){
		if(price != priceHist[idx]) {
			direction = price > priceHist[idx];
			countSinceChange = 0;
			priorDifferentPrice = priceHist[idx];
		}
		else {
			countSinceChange++;
		}
		priceHist[idx++] = price;
		if(idx == priceHist.length) idx = 0;
	}
	public int getAge(){
		return countSinceChange;
	}
	final public double getPrice(){
		return priceHist[idx];
	}
	final public double priorPrice(){
		//final int pIdx = idx == 0 ? priceHist.length-1 : idx -1;
		//return priceHist[pIdx];
		return priorDifferentPrice;
	}
}
