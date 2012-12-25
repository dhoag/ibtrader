package com.davehoag.ib.dataTypes;

/**
 * Wrapper class for price bands
 * 
 * @author David Hoag
 * 
 */
public class PriceBands {
	final double[] data;

	public PriceBands(final double[] d) {
		data = d;
	}

	public PriceBands(final Bar aBar) {
		data = new double[3];
		data[0] = aBar.low;
		data[1] = aBar.wap;
		data[2] = aBar.high;
	}

	public double getLow() {
		return data[0];
	}

	public double getMid() {
		return data[1];
	}

	public double getHigh() {
		return data[2];
	}

	double round2(double num) {
		double result = num * 10000;
		result = Math.round(result);
		result = result / 10000;
		return result;
	}

	@Override
	public String toString() {
		return "Bands[" + round2(getLow()) + "," + round2(getMid()) + "," + round2(getHigh()) + "]";
	}
}
