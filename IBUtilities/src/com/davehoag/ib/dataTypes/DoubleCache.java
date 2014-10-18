package com.davehoag.ib.dataTypes;

public class DoubleCache {
	int idx = 0;
	final double [] priceHist = new double[20];
	boolean direction = false;
	int countSinceChange = 0;
	double priorDifferentPrice;
	
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
