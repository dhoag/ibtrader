package com.davehoag.analytics;


public class Functions {

	/**
	 * @param start
	 * @param periods
	 * @param field
	 * @return
	 */
	public static double getMA(Iterable<DoubleReader> rdr) {
		double sum = 0;
		int count = 0;
		for(DoubleReader val : rdr){
			sum+=val.readDouble();
			count++;
		}
		return sum / count;
	}
	
	public static double getStdDev(Iterable<DoubleReader> rdr){
		double ma = getMA(rdr);
		double squaredSum = 0;
		int i = 0;
		int count = 0;
		for( DoubleReader aVal : rdr){
			squaredSum += Math.pow(aVal.readDouble() - ma, 2);
			count++;
		}
		double devMean = squaredSum / count;
		return Math.sqrt(devMean);
	}

}
