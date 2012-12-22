package com.davehoag.ib.dataTypes;

import flanagan.analysis.Stat;

public class BarCache {
	Bar[] localCache = new Bar[5 * (60 / 5) * 60 * 8];
	int lastIdx = 0;
	boolean wrapped = false;

	public BarCache() {
	}

	public void pushLatest(final Bar aBar) {
		if (lastIdx == localCache.length) {
			lastIdx = 0;
			wrapped = true;
		}
		localCache[lastIdx++] = aBar;
	}

	public Bar[] getBars(final int periods) {
		if ((periods > localCache.length) || (periods > lastIdx && !wrapped)) {
			throw new IllegalStateException("Not enough data to fullfill request");
		}
		Bar[] result = new Bar[periods];
		int count = 0;
		for (int i = lastIdx - 1; i >= Math.max(lastIdx - periods, 0); i--)
			result[count++] = localCache[i];
		if (periods > lastIdx) {
			final int end = localCache.length - count;
			for (int i = localCache.length - 1; i > end; i--)
				result[count++] = localCache[i];
		}
		return result;
	}

	public double[] getVwap(final int periods) {
		if ((periods > localCache.length) || (periods > lastIdx && !wrapped)) {
			throw new IllegalStateException("Not enough data to fullfill request");
		}
		double[] result = new double[periods];
		int count = 0;
		for (int i = lastIdx - 1; i > Math.max(lastIdx - periods, 0); i--)
			result[count++] = localCache[i].wap;
		if (periods > lastIdx) {
			final int end = localCache.length - count;
			for (int i = localCache.length - 1; i > end; i--)
				result[count++] = localCache[i].wap;
		}
		return result;
	}

	public double[] getVwapBands(int periods) {
		double[] vwapList = getVwap(periods);
		Stat stat = new Stat(vwapList);
		double stdDev = stat.standardDeviation();
		double vwap = vwapList[0];
		double[] result = new double[3];
		result[0] = vwap - stdDev;
		result[1] = vwap;
		result[2] = vwap + stdDev;
		return result;
	}
}